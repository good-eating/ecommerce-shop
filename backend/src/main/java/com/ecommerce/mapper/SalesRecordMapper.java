package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.SalesRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SalesRecordMapper extends BaseMapper<SalesRecord> {

    @Select("SELECT * FROM sales_record WHERE created_at >= #{startTime} ORDER BY created_at DESC")
    List<SalesRecord> selectRecentRecords(@Param("startTime") LocalDateTime startTime);
}