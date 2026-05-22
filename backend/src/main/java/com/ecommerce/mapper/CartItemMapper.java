package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("SELECT * FROM cart_item WHERE cart_id = #{cartId} AND product_id = #{productId}")
    CartItem selectByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}