package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer status;
    private String statusText;
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private String shippingAddress;
    private String remark;
    private LocalDateTime receivedTime;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getReceivedTime() { return receivedTime; }
    public void setReceivedTime(LocalDateTime receivedTime) { this.receivedTime = receivedTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id; private String orderNo; private Long userId;
        private BigDecimal totalAmount; private BigDecimal discountAmount; private BigDecimal payAmount;
        private Integer status; private String statusText; private String paymentMethod;
        private LocalDateTime paymentTime; private String shippingAddress; private String remark;
        private LocalDateTime receivedTime; private LocalDateTime createdAt; private List<OrderItemDTO> items;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
        public Builder payAmount(BigDecimal payAmount) { this.payAmount = payAmount; return this; }
        public Builder status(Integer status) { this.status = status; return this; }
        public Builder statusText(String statusText) { this.statusText = statusText; return this; }
        public Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder paymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; return this; }
        public Builder shippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder receivedTime(LocalDateTime receivedTime) { this.receivedTime = receivedTime; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder items(List<OrderItemDTO> items) { this.items = items; return this; }
        public OrderDTO build() {
            OrderDTO r = new OrderDTO();
            r.id = id; r.orderNo = orderNo; r.userId = userId;
            r.totalAmount = totalAmount; r.discountAmount = discountAmount; r.payAmount = payAmount;
            r.status = status; r.statusText = statusText; r.paymentMethod = paymentMethod;
            r.paymentTime = paymentTime; r.shippingAddress = shippingAddress; r.remark = remark;
            r.receivedTime = receivedTime; r.createdAt = createdAt; r.items = items;
            return r;
        }
    }
}