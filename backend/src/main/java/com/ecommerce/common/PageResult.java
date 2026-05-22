package com.ecommerce.common;

import java.util.List;

public class PageResult<T> {
    private Long total;
    private List<T> items;
    private Long page;
    private Long size;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public Long getPage() { return page; }
    public void setPage(Long page) { this.page = page; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public static <T> PageResult<T> of(Long total, List<T> items, Long page, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setItems(items);
        result.setPage(page);
        result.setSize(size);
        return result;
    }
}