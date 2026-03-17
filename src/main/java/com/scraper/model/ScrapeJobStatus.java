package com.scraper.model;

/** Lifecycle states for a single scrape job execution. */
public enum ScrapeJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
