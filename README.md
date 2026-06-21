<div align="center">
  <img src=".github/header.svg" alt="deal-scraper" width="100%"/>
</div>

<div align="center">

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Playwright](https://img.shields.io/badge/Playwright-2EAD33?style=for-the-badge&logo=playwright&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

</div>

<br/>

Facebook Marketplace listings load dynamically — a standard HTTP scraper can't see them. deal-scraper uses Playwright, a headless browser engine, to render pages and extract structured listing data. It feeds a PostgreSQL store that deal-detector reads to surface underpriced listings.

Part of a two-service system — see also [deal-detector](https://github.com/psilde/deal-detector).

<br/>

<img src=".github/section-architecture.svg" alt="System Architecture" width="100%"/>

```mermaid
flowchart LR
    A[Facebook Marketplace] -->|Playwright headless browser| B[deal-scraper]
    B -->|normalised listings| C[(PostgreSQL)]
    C -->|consumed by| D[deal-detector]
```

<br/>

<img src=".github/section-features.svg" alt="Core Features" width="100%"/>

- **Headless browser scraping** — Playwright renders JavaScript-heavy marketplace pages that standard HTTP scrapers cannot access
- **Async scrape jobs** — jobs run asynchronously and expose a full lifecycle: `PENDING` → `RUNNING` → `COMPLETED` / `FAILED`
- **Keyword and category scanning** — trigger scrapes by search keyword or navigate directly to a category URL
- **Deduplication** — listings are fingerprinted on ingest; duplicates are counted and skipped, not re-inserted
- **Normalisation** — titles, prices, and listing attributes are cleaned and structured before storage
- **Scrape metrics** — each job records listings fetched, inserted, and duplicates skipped

<br/>

<img src=".github/section-techstack.svg" alt="Tech Stack" width="100%"/>

| Technology | Purpose |
|---|---|
| Java 21 + Spring Boot | Core application framework |
| Playwright | Headless browser for scraping JavaScript-rendered pages |
| PostgreSQL | Production data store for scraped listings and job history |
| H2 | In-memory database for local development and tests |
| Flyway | Version-controlled schema migrations |
| Spring Data JPA | Repository layer for listings and scrape jobs |

<br/>

<img src=".github/section-setup.svg" alt="Getting Started" width="100%"/>

```bash
# 1. Clone
git clone https://github.com/psilde/deal-scraper.git
cd deal-scraper

# 2. Run
./mvnw spring-boot:run
```

H2 is bundled for local development — no database setup required out of the box. For production, configure a PostgreSQL datasource in `application.properties`. Flyway runs migrations automatically on startup.

<br/>

<img src=".github/section-api.svg" alt="API Overview" width="100%"/>

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/marketplace/scrape` | Trigger a scrape job (returns `202 Accepted`) |
| `GET` | `/marketplace/listings` | Browse scraped listings, filter by keyword / category / city |
| `GET` | `/scrape-jobs` | List all scrape jobs, filter by source |
| `GET` | `/scrape-jobs/{id}` | Get a specific scrape job and its metrics |

**Trigger scrape request body:**
```json
{
  "marketplaceSource": "FACEBOOK_MARKETPLACE",
  "keyword": "iphone 15",
  "scanType": "KEYWORD",
  "sourceCity": "sydney"
}
```

<br/>

<img src=".github/section-companion.svg" alt="deal-detector" width="100%"/>

Analysis engine that reads from this store and surfaces underpriced listings: https://github.com/psilde/deal-detector
