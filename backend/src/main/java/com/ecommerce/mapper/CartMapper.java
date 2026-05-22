package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Select("SELECT * FROM cart WHERE user_id = #{userId}")
    Cart selectByUserId(@Param("userId") Long userId);
}