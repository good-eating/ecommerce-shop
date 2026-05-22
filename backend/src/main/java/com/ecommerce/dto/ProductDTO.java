package com.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String sku;
    private String description;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer salesCount;
    private String attributesJson;
    private String image;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id; private String name; private String sku; private String description;
        private Long categoryId; private BigDecimal price; private BigDecimal originalPrice;
        private Integer stock; private Integer salesCount; private String attributesJson;
        private String image;
        private Integer status; private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder sku(String sku) { this.sku = sku; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder originalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; return this; }
        public Builder stock(Integer stock) { this.stock = stock; return this; }
        public Builder salesCount(Integer salesCount) { this.salesCount = salesCount; return this; }
        public Builder attributesJson(String attributesJson) { this.attributesJson = attributesJson; return this; }
        public Builder image(String image) { this.image = image; return this; }
        public Builder status(Integer status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProductDTO build() {
            ProductDTO r = new ProductDTO();
            r.id = id; r.name = name; r.sku = sku; r.description = description;
            r.categoryId = categoryId; r.price = price; r.originalPrice = originalPrice;
            r.stock = stock; r.salesCount = salesCount; r.attributesJson = attributesJson;
            r.image = image; r.status = status; r.createdAt = createdAt;
            return r;
        }
    }
}