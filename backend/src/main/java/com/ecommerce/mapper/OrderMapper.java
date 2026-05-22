package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM `order` WHERE id = #{id}")
    Order selectByIdIgnoreLogic(@Param("id") Long id);

    @Update("UPDATE `order` SET status = #{status}, payment_method = #{paymentMethod}, payment_time = #{paymentTime} WHERE id = #{id}")
    int updatePaymentStatus(@Param("id") Long id, @Param("status") Integer status, @Param("paymentMethod") String paymentMethod, @Param("paymentTime") LocalDateTime paymentTime);

    @Update("UPDATE `order` SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE `order` SET received_time = #{receivedTime} WHERE id = #{id}")
    int updateReceivedTime(@Param("id") Long id, @Param("receivedTime") LocalDateTime receivedTime);

    @Select("SELECT * FROM `order` WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `order` WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Order> selectPageByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("size") Integer size);

    @Select("SELECT COUNT(*) FROM `order` WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `order` WHERE status = 1 ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Order> selectPaidOrders(@Param("offset") Integer offset, @Param("size") Integer size);

    @Select("SELECT COUNT(*) FROM `order` WHERE status = 1")
    Long countPaidOrders();

    @Select("SELECT * FROM `order` WHERE status IN (2, 3) ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Order> selectShippedOrders(@Param("offset") Integer offset, @Param("size") Integer size);

    @Select("SELECT COUNT(*) FROM `order` WHERE status IN (2, 3)")
    Long countShippedOrders();

    @Select("SELECT * FROM `order` WHERE status = 0 AND created_at < #{expireTime}")
    List<Order> selectExpiredOrders(@Param("expireTime") LocalDateTime expireTime);

    @Select("SELECT COUNT(*) FROM `order` WHERE created_at >= #{startTime}")
    Long countTodayOrders(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` WHERE created_at >= #{startTime} AND status >= 1")
    BigDecimal sumTodaySales(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT COUNT(*) FROM `order` WHERE created_at >= #{startTime} AND status >= 1")
    Long countTodayPaidOrders(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT * FROM `order` ORDER BY created_at DESC")
    List<Order> selectAllIgnoreLogic();

    @Select("SELECT COUNT(*) FROM `order` WHERE status = #{status}")
    Long countByStatus(@Param("status") Integer status);

    @Select("SELECT HOUR(created_at) as `hour`, COUNT(*) as order_count, COALESCE(SUM(total_amount), 0) as sales_amount FROM `order` WHERE created_at >= #{startTime} GROUP BY HOUR(created_at) ORDER BY `hour`")
    List<Map<String, Object>> selectHourlyStats(@Param("startTime") LocalDateTime startTime);
}