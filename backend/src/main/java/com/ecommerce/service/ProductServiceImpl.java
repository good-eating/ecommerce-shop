package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.ProductMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public PageResult<ProductDTO> getProducts(Integer page, Integer size, Long categoryId, String keyword, String sort) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);

        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }

        // 排序
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sales".equals(sort)) {
            wrapper.orderByDesc(Product::getSalesCount);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }

        Page<Product> productPage = productMapper.selectPage(new Page<>(page, size), wrapper);

        List<ProductDTO> items = productPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(productPage.getTotal(), items, (long) page, (long) size);
    }

    @Override
    public PageResult<ProductDTO> getAllProducts(Integer page, Integer size, Long categoryId, String keyword, Integer status) {
        List<Product> allProducts = productMapper.selectAllIgnoreLogic();

        if (categoryId != null) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getCategoryId() != null && p.getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getName() != null && p.getName().contains(keyword))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getStatus() != null && p.getStatus().equals(status))
                    .collect(Collectors.toList());
        }

        int total = allProducts.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<ProductDTO> items;
        if (fromIndex >= total) {
            items = List.of();
        } else {
            items = allProducts.subList(fromIndex, toIndex).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        return PageResult.of((long) total, items, (long) page, (long) size);
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在");
        }
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setSku(productDTO.getSku());
        product.setDescription(productDTO.getDescription());
        product.setCategoryId(productDTO.getCategoryId());
        product.setPrice(productDTO.getPrice());
        product.setOriginalPrice(productDTO.getOriginalPrice());
        product.setStock(productDTO.getStock());
        product.setSalesCount(0);
        product.setAttributesJson(productDTO.getAttributesJson());
        product.setImage(productDTO.getImage());
        product.setStatus(1);

        productMapper.insert(product);
        return convertToDTO(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        product.setName(productDTO.getName());
        product.setSku(productDTO.getSku());
        product.setDescription(productDTO.getDescription());
        product.setCategoryId(productDTO.getCategoryId());
        product.setPrice(productDTO.getPrice());
        product.setOriginalPrice(productDTO.getOriginalPrice());
        product.setStock(productDTO.getStock());
        product.setAttributesJson(productDTO.getAttributesJson());
        product.setImage(productDTO.getImage());

        productMapper.updateById(product);
        return convertToDTO(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        productMapper.updateStatusDirectly(id, 0);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectByIdIgnoreLogic(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("状态值无效");
        }
        int updated = productMapper.updateStatusDirectly(id, status);
        if (updated == 0) {
            throw new BusinessException("更新失败，请重试");
        }
    }

    @Override
    public List<ProductDTO> getTopProducts(Integer limit) {
        List<Product> products = productMapper.selectTopProducts(limit);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .salesCount(product.getSalesCount())
                .attributesJson(product.getAttributesJson())
                .image(product.getImage())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }
}