package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT * FROM category WHERE status = #{status} ORDER BY sort_order ASC")
    List<Category> selectByStatus(@Param("status") Integer status);

    @Update("UPDATE category SET status = 0 WHERE id = #{id}")
    int disableCategory(@Param("id") Long id);
}
