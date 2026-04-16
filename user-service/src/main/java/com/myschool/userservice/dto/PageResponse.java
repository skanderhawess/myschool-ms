package com.myschool.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;       // data for this page
    private int page;              // current page number (0-based)
    private int size;              // page size requested
    private long totalElements;    // total items in database
    private int totalPages;        // total number of pages
    private boolean last;          // is this the last page?
}
