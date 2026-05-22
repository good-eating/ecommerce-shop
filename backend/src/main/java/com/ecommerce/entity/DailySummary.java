package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("daily_summary")
public class DailySummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate summaryDate;
    private String summaryType;
    private Integer totalUsers;
    private Integer newUsers;
    private Integer totalOrders;
    private BigDecimal totalSales;
    private String topProductsJson;
    private String userDistributionJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public String getSummaryType() { return summaryType; }
    public void setSummaryType(String summaryType) { this.summaryType = summaryType; }
    public Integer getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }
    public Integer getNewUsers() { return newUsers; }
    public void setNewUsers(Integer newUsers) { this.newUsers = newUsers; }
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
    public String getTopProductsJson() { return topProductsJson; }
    public void setTopProductsJson(String topProductsJson) { this.topProductsJson = topProductsJson; }
    public String getUserDistributionJson() { return userDistributionJson; }
    public void setUserDistributionJson(String userDistributionJson) { this.userDistributionJson = userDistributionJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}