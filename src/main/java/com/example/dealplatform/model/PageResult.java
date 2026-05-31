package com.example.dealplatform.model;

import java.util.List;

public class PageResult<T> {
    private final List<T> records;
    private final int page;
    private final int size;
    private final int total;
    private final int totalPages;

    public PageResult(List<T> records, int page, int size, int total) {
        this.records = records;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = Math.max(1, (int) Math.ceil(total * 1.0 / size));
    }

    public List<T> getRecords() { return records; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < totalPages; }
}
