# deal-scraper

> A data ingestion layer for the Deal Detector system.
`deal-scraper` is a marketplace scraper that collects and structures listing data used for automated deal detection.

The scraper gathers listings, normalises marketplace metadata, and prepares structured data that can be processed by the analysis engine in **deal-detector**.

---

## use case

This project is the **data ingestion layer** in the Deal Detector pipeline.

raw listings
↓
deal-scraper (collect + structure listings)
↓
deal-detector (analyse pricing signals)
↓
identify deals

---

## responsibilities

- scrape marketplace listings
- extract structured listing metadata
- normalise titles, prices, and listing attributes
- prepare the data for processing to the deal-detection system

---

## tech
- Java (Spring Boot)

---

## related project
https://github.com/psilde/deal-detector
