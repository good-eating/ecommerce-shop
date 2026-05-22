package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.BehaviorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BehaviorLogMapper extends BaseMapper<BehaviorLog> {

    int batchInsert(@Param("logs") List<BehaviorLog> logs);
}