package com.scraper.model;

public enum ScanType {
    // navigates to {baseUrl}/search?query={keyword}
    KEYWORD,
    // navigates to the category url directly
    CATEGORY
}
