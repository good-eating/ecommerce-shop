package com.ecommerce.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.CartItemMapper;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.mapper.ProductMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public CartServiceImpl(CartMapper cartMapper, CartItemMapper cartItemMapper, ProductMapper productMapper, RedisTemplate<String, Object> redisTemplate) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CartDTO getCart(Long userId) {
        // 先从Redis获取
        String cartKey = "cart:user:" + userId;
        Map<Object, Object> redisCart = redisTemplate.opsForHash().entries(cartKey);

        if (!redisCart.isEmpty()) {
            return buildCartFromRedis(userId, redisCart);
        }

        // 从数据库获取
        return getCartFromDB(userId);
    }

    @Override
    @Transactional
    public void addToCart(Long userId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }

        if (product.getStock() < quantity) {
            throw new BusinessException("商品库存不足");
        }

        // 同步写入数据库（先获取自增ID）
        Cart cart = cartMapper.selectByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setTotalItems(0);
            cart.setTotalAmount(BigDecimal.ZERO);
            cartMapper.insert(cart);
        }

        CartItem existingItem = cartItemMapper.selectByCartIdAndProductId(cart.getId(), productId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemMapper.updateById(existingItem);
        } else {
            existingItem = new CartItem();
            existingItem.setCartId(cart.getId());
            existingItem.setProductId(productId);
            existingItem.setQuantity(quantity);
            existingItem.setPrice(product.getPrice());
            cartItemMapper.insert(existingItem);
        }

        // 同步写入Redis（带上数据库ID）
        String cartKey = "cart:user:" + userId;
        String itemKey = "product:" + productId;

        CartItemRedisDTO redisItem = new CartItemRedisDTO();
        redisItem.setId(existingItem.getId());
        redisItem.setProductId(productId);
        redisItem.setQuantity(existingItem.getQuantity());
        redisItem.setPrice(product.getPrice());
        redisItem.setAddedAt(System.currentTimeMillis());

        redisTemplate.opsForHash().put(cartKey, itemKey, JSON.toJSONString(redisItem));
        redisTemplate.expire(cartKey, 30, TimeUnit.DAYS);
    }

    @Override
    @Transactional
    public void updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null) {
            throw new BusinessException("购物车项不存在");
        }

        Cart cart = cartMapper.selectById(cartItem.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }

        if (quantity <= 0) {
            removeCartItem(userId, cartItemId);
            return;
        }

        // 更新数据库
        cartItem.setQuantity(quantity);
        cartItemMapper.updateById(cartItem);

        // 更新Redis
        String cartKey = "cart:user:" + userId;
        String itemKey = "product:" + cartItem.getProductId();

        CartItemRedisDTO redisItem = new CartItemRedisDTO();
        redisItem.setId(cartItem.getId());
        redisItem.setProductId(cartItem.getProductId());
        redisItem.setQuantity(quantity);
        redisItem.setPrice(cartItem.getPrice());
        redisItem.setAddedAt(System.currentTimeMillis());

        redisTemplate.opsForHash().put(cartKey, itemKey, JSON.toJSONString(redisItem));
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null) {
            return;
        }

        Cart cart = cartMapper.selectById(cartItem.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }

        // 删除数据库记录
        cartItemMapper.deleteById(cartItemId);

        // 删除Redis记录
        String cartKey = "cart:user:" + userId;
        String itemKey = "product:" + cartItem.getProductId();
        redisTemplate.opsForHash().delete(cartKey, itemKey);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartMapper.selectByUserId(userId);
        if (cart != null) {
            // 删除数据库记录
            cartItemMapper.delete(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getCartId, cart.getId())
            );
        }

        // 删除Redis记录
        String cartKey = "cart:user:" + userId;
        redisTemplate.delete(cartKey);
    }

    @Override
    @Transactional
    public void mergeCartOnLogin(Long userId, List<CartItemDTO> sessionItems) {
        if (sessionItems == null || sessionItems.isEmpty()) {
            return;
        }

        String cartKey = "cart:user:" + userId;

        for (CartItemDTO sessionItem : sessionItems) {
            // 合并策略：数量相加，取最新价格
            String itemKey = "product:" + sessionItem.getProductId();
            String existingItemJson = (String) redisTemplate.opsForHash().get(cartKey, itemKey);

            // 同步到数据库
            Cart cart = cartMapper.selectByUserId(userId);
            if (cart == null) {
                cart = new Cart();
                cart.setUserId(userId);
                cart.setTotalItems(0);
                cart.setTotalAmount(BigDecimal.ZERO);
                cartMapper.insert(cart);
            }

            CartItem cartItem = cartItemMapper.selectByCartIdAndProductId(cart.getId(), sessionItem.getProductId());
            if (cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() + sessionItem.getQuantity());
                cartItemMapper.updateById(cartItem);
            } else {
                cartItem = new CartItem();
                cartItem.setCartId(cart.getId());
                cartItem.setProductId(sessionItem.getProductId());
                cartItem.setQuantity(sessionItem.getQuantity());
                cartItem.setPrice(sessionItem.getPrice());
                cartItemMapper.insert(cartItem);
            }

            if (existingItemJson != null) {
                CartItemRedisDTO existingItem = JSON.parseObject(existingItemJson, CartItemRedisDTO.class);
                existingItem.setId(cartItem.getId());
                existingItem.setQuantity(existingItem.getQuantity() + sessionItem.getQuantity());
                existingItem.setAddedAt(System.currentTimeMillis());
                redisTemplate.opsForHash().put(cartKey, itemKey, JSON.toJSONString(existingItem));
            } else {
                CartItemRedisDTO newItem = new CartItemRedisDTO();
                newItem.setId(cartItem.getId());
                newItem.setProductId(sessionItem.getProductId());
                newItem.setQuantity(sessionItem.getQuantity());
                newItem.setPrice(sessionItem.getPrice());
                newItem.setAddedAt(System.currentTimeMillis());
                redisTemplate.opsForHash().put(cartKey, itemKey, JSON.toJSONString(newItem));
            }
        }

        redisTemplate.expire(cartKey, 30, TimeUnit.DAYS);
    }

    private void syncToDB(Long userId, Long productId, Integer quantity, BigDecimal price) {
        // 获取或创建购物车
        Cart cart = cartMapper.selectByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setTotalItems(0);
            cart.setTotalAmount(BigDecimal.ZERO);
            cartMapper.insert(cart);
        }

        // 查询购物车项
        CartItem cartItem = cartItemMapper.selectByCartIdAndProductId(cart.getId(), productId);

        if (cartItem != null) {
            // 更新数量
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItemMapper.updateById(cartItem);
        } else {
            // 新增购物车项
            cartItem = new CartItem();
            cartItem.setCartId(cart.getId());
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setPrice(price);
            cartItemMapper.insert(cartItem);
        }
    }

    private CartDTO getCartFromDB(Long userId) {
        Cart cart = cartMapper.selectByUserId(userId);
        if (cart == null) {
            return CartDTO.builder()
                    .totalItems(0)
                    .totalAmount(BigDecimal.ZERO)
                    .items(new ArrayList<>())
                    .build();
        }

        List<CartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getCartId, cart.getId())
        );

        List<CartItemDTO> items = cartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return CartDTO.builder()
                .id(cart.getId())
                .totalItems(items.size())
                .totalAmount(calculateTotal(items))
                .items(items)
                .build();
    }

    private CartDTO buildCartFromRedis(Long userId, Map<Object, Object> redisCart) {
        List<CartItemDTO> items = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : redisCart.entrySet()) {
            String itemJson = (String) entry.getValue();
            CartItemRedisDTO redisItem = JSON.parseObject(itemJson, CartItemRedisDTO.class);

            Product product = productMapper.selectById(redisItem.getProductId());
            if (product != null && product.getStatus() == 1) {
                CartItemDTO item = CartItemDTO.builder()
                        .id(redisItem.getId())
                        .productId(redisItem.getProductId())
                        .productName(product.getName())
                        .productImage(product.getImage())
                        .quantity(redisItem.getQuantity())
                        .price(redisItem.getPrice())
                        .subtotal(redisItem.getPrice().multiply(BigDecimal.valueOf(redisItem.getQuantity())))
                        .build();
                items.add(item);
            }
        }

        return CartDTO.builder()
                .totalItems(items.size())
                .totalAmount(calculateTotal(items))
                .items(items)
                .build();
    }

    private CartItemDTO convertToDTO(CartItem cartItem) {
        Product product = productMapper.selectById(cartItem.getProductId());

        return CartItemDTO.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProductId())
                .productName(product != null ? product.getName() : "未知商品")
                .productImage(product != null ? product.getImage() : null)
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .subtotal(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .build();
    }

    private BigDecimal calculateTotal(List<CartItemDTO> items) {
        return items.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Redis购物车项数据结构
    private static class CartItemRedisDTO {
        private Long id;
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
        private Long addedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Long getAddedAt() { return addedAt; }
        public void setAddedAt(Long addedAt) { this.addedAt = addedAt; }
    }
}