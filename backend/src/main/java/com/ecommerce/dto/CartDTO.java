package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartDTO {
    private Long id;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private List<CartItemDTO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id; private Integer totalItems;
        private BigDecimal totalAmount; private List<CartItemDTO> items;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder totalItems(Integer totalItems) { this.totalItems = totalItems; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder items(List<CartItemDTO> items) { this.items = items; return this; }
        public CartDTO build() {
            CartDTO r = new CartDTO();
            r.id = id; r.totalItems = totalItems;
            r.totalAmount = totalAmount; r.items = items;
            return r;
        }
    }
}