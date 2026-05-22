package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM product WHERE status = 1 ORDER BY sales_count DESC LIMIT #{limit}")
    List<Product> selectTopProducts(@Param("limit") Integer limit);

    @Update("UPDATE product SET status = #{status} WHERE id = #{id}")
    int updateStatusDirectly(@Param("id") Long id, @Param("status") Integer status);

    @Select("SELECT * FROM product ORDER BY created_at DESC")
    List<Product> selectAllIgnoreLogic();

    @Select("SELECT * FROM product WHERE id = #{id}")
    Product selectByIdIgnoreLogic(@Param("id") Long id);
}