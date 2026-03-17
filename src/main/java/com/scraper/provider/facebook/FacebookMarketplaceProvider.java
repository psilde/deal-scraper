package com.scraper.provider.facebook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.scraper.config.MarketplaceProperties;
import com.scraper.dto.MarketplaceSearchQuery;
import com.scraper.dto.RawMarketplaceListing;
import com.scraper.model.MarketplaceSource;
import com.scraper.model.ScanType;
import com.scraper.provider.MarketplaceProvider;
import com.scraper.provider.PlaywrightBrowserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FacebookMarketplaceProvider implements MarketplaceProvider {

    private static final Logger log = LoggerFactory.getLogger(FacebookMarketplaceProvider.class);

    private static final String LISTING_SELECTOR = "a[href*='/marketplace/item/']";
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("/marketplace/item/(\\d+)");
    // accepts: "$450", "A$1,200", "AU$300", "£200", "€300", "Free", "1200"
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("^(Free|[A-Z]{0,2}[$£€¥₹]\\s*[\\d,]+|[$£€¥₹][\\d,]+|[\\d,]+)$",
                    Pattern.CASE_INSENSITIVE);

    private final MarketplaceProperties properties;
    private final PlaywrightBrowserManager browserManager;
    private final ObjectMapper objectMapper;

    public FacebookMarketplaceProvider(
            MarketplaceProperties properties,
            PlaywrightBrowserManager browserManager,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.browserManager = browserManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MarketplaceSource source) {
        return source == MarketplaceSource.FACEBOOK_MARKETPLACE;
    }

    @Override
    public List<RawMarketplaceListing> search(MarketplaceSearchQuery query) {
        MarketplaceProperties.Facebook fbConfig = properties.getFacebook();

        if (!fbConfig.isEnabled()) {
            log.debug("fb.provider.disabled keyword={}", query.keyword());
            return List.of();
        }

        if (fbConfig.isStubMode() || !browserManager.isAvailable()) {
            if (!browserManager.isAvailable()) {
                log.warn("fb.provider.stub reason=playwright_unavailable keyword={}", query.keyword());
            }
            return searchStub(query);
        }

        return searchReal(query);
    }

    // real scraping

    private List<RawMarketplaceListing> searchReal(MarketplaceSearchQuery query) {
        String url = resolveNavigationUrl(query);
        String label = resolveLabel(query);
        int timeoutMs = properties.getFacebook().getTimeoutSeconds() * 1000;
        int maxScrollAttempts = properties.getFacebook().getMaxScrollAttempts();

        log.info("fb.scrape.start label=\"{}\" url={}", label, url);

        try (Page page = browserManager.newPage()) {
            navigateTo(page, url, timeoutMs);
            if (!validatePageContext(page, query)) {
                log.warn("fb.scrape.abort label=\"{}\" reason=context_mismatch", label);
                return List.of();
            }
            if (waitForListings(page, timeoutMs)) {
                return scrapeWithPage(page, query, label, maxScrollAttempts);
            }
        } catch (Exception e) {
            log.error("fb.scrape.error label=\"{}\" error={}", label, e.getMessage(), e);
            return List.of();
        }

        // no listings — session may have expired
        log.warn("fb.scrape.no_listings label=\"{}\" — refreshing session", label);
        boolean refreshed = browserManager.refreshSession();
        if (!refreshed) {
            log.warn("fb.scrape.session_refresh_failed label=\"{}\"", label);
            return List.of();
        }

        try (Page page = browserManager.newPage()) {
            navigateTo(page, url, timeoutMs);
            if (!validatePageContext(page, query)) {
                log.warn("fb.scrape.abort label=\"{}\" reason=context_mismatch_after_refresh", label);
                return List.of();
            }
            if (!waitForListings(page, timeoutMs)) {
                log.warn("fb.scrape.no_listings_after_refresh label=\"{}\" url={}", label, url);
                return List.of();
            }
            return scrapeWithPage(page, query, label, maxScrollAttempts);
        } catch (Exception e) {
            log.error("fb.scrape.error label=\"{}\" error={}", label, e.getMessage(), e);
            return List.of();
        }
    }

    private List<RawMarketplaceListing> scrapeWithPage(Page page, MarketplaceSearchQuery query,
                                                        String label, int maxScrollAttempts) {
        dismissOverlays(page);

        // facebook virtualises the list — accumulate by externalId across scrolls to avoid losing off-screen items
        Map<String, RawMarketplaceListing> accumulated = new LinkedHashMap<>();
        int stagnantCycles = 0;

        for (int i = 0; i < maxScrollAttempts; i++) {
            int prevSize = accumulated.size();

            List<RawMarketplaceListing> batch = extractListings(page, query);
            for (RawMarketplaceListing raw : batch) {
                accumulated.putIfAbsent(raw.externalId(), raw);
            }

            int newThisCycle = accumulated.size() - prevSize;

            log.info("fb.cycle label=\"{}\" i={} seen={} extracted={} newUnique={} totalAccumulated={} stagnantCycles={}",
                    label, i + 1, countAnchors(page), batch.size(), newThisCycle, accumulated.size(), stagnantCycles);

            if (accumulated.size() >= query.maxResults()) {
                log.info("fb.scroll.stop label=\"{}\" reason=max_results accumulated={}", label, accumulated.size());
                break;
            }

            if (newThisCycle == 0) {
                stagnantCycles++;
                if (stagnantCycles >= 5) {
                    log.info("fb.scroll.stop label=\"{}\" reason=stagnant stagnantCycles={} accumulated={}",
                            label, stagnantCycles, accumulated.size());
                    break;
                }
            } else {
                stagnantCycles = 0;
            }

            scrollToBottom(page);
            page.waitForTimeout(3000);
        }

        log.info("fb.scrape.done label=\"{}\" totalUnique={}", label, accumulated.size());
        return new ArrayList<>(accumulated.values());
    }

    // navigation and context validation

    private String resolveNavigationUrl(MarketplaceSearchQuery query) {
        if (query.scanType() == ScanType.CATEGORY) {
            if (isConcreteCategoryUrl(query.baseUrl(), query.sourceCity(), query.sourceCategory())) {
                return query.baseUrl();
            }
            if (query.sourceCity() != null && query.sourceCategory() != null) {
                String url = buildCategoryUrl(query.sourceCity(), query.sourceCategory());
                log.info("fb.url.constructed city={} category={} url={}", query.sourceCity(), query.sourceCategory(), url);
                return url;
            }
            log.warn("fb.url.missing_context sourceCity={} sourceCategory={} — falling back to base",
                    query.sourceCity(), query.sourceCategory());
            return properties.getFacebook().getApiBaseUrl();
        }
        String base = (query.baseUrl() != null && !query.baseUrl().isBlank())
                ? query.baseUrl()
                : properties.getFacebook().getApiBaseUrl();
        return buildSearchUrl(base, query.keyword());
    }

    // confirm final url contains expected city and category slugs; skip for keyword scans
    private boolean validatePageContext(Page page, MarketplaceSearchQuery query) {
        if (query.scanType() != ScanType.CATEGORY) return true;

        String finalUrl = page.url();
        String finalUrlLower = finalUrl.toLowerCase(Locale.ROOT);
        String expectedCity = toSlug(query.sourceCity());
        String expectedCategory = toSlug(query.sourceCategory());

        boolean cityPresent = !expectedCity.isEmpty() && finalUrlLower.contains(expectedCity);
        boolean categoryPresent = !expectedCategory.isEmpty() && finalUrlLower.contains(expectedCategory);

        log.info("fb.context.check expectedCity={} cityPresent={} expectedCategory={} categoryPresent={} finalUrl={}",
                expectedCity, cityPresent, expectedCategory, categoryPresent, finalUrl);

        if (!cityPresent || !categoryPresent) {
            log.warn("fb.context.mismatch expectedCity={} expectedCategory={} finalUrl={}",
                    expectedCity, expectedCategory, finalUrl);
            return false;
        }
        return true;
    }

    private boolean isConcreteCategoryUrl(String url, String city, String category) {
        if (url == null || url.isBlank()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return city != null && lower.contains(toSlug(city))
                && category != null && lower.contains(toSlug(category));
    }

    private String buildCategoryUrl(String city, String category) {
        return "https://www.facebook.com/marketplace/" + toSlug(city) + "/" + toSlug(category);
    }

    private String toSlug(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).trim().replace(" ", "-");
    }

    private String resolveLabel(MarketplaceSearchQuery query) {
        if (query.scanType() == ScanType.CATEGORY
                && query.sourceCategory() != null && query.sourceCity() != null) {
            return query.sourceCategory() + " @ " + query.sourceCity();
        }
        return query.keyword();
    }

    private void navigateTo(Page page, String url, int timeoutMs) {
        page.navigate(url, new Page.NavigateOptions()
                .setTimeout(timeoutMs)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    private boolean waitForListings(Page page, int timeoutMs) {
        try {
            page.waitForSelector(LISTING_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    private void dismissOverlays(Page page) {
        try { page.keyboard().press("Escape"); } catch (Exception ignored) {}
        for (String sel : List.of(
                "[aria-label='Close']", "[aria-label='close']",
                "[data-testid='close-button']", "div[role='dialog'] [aria-label='Close']")) {
            try {
                ElementHandle btn = page.querySelector(sel);
                if (btn != null) { btn.click(); page.waitForTimeout(500); }
            } catch (Exception ignored) {}
        }
    }

    // trigger lazy-load for next batch of listings
    private void scrollToBottom(Page page) {
        try {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        } catch (PlaywrightException ignored) {}
    }

    private int countAnchors(Page page) {
        try {
            return page.querySelectorAll(LISTING_SELECTOR).size();
        } catch (PlaywrightException e) {
            return 0;
        }
    }

    // extraction

    private List<RawMarketplaceListing> extractListings(Page page, MarketplaceSearchQuery query) {
        List<ElementHandle> anchors = page.querySelectorAll(LISTING_SELECTOR);
        List<RawMarketplaceListing> results = new ArrayList<>();
        int failures = 0;
        for (ElementHandle anchor : anchors) {
            try {
                RawMarketplaceListing listing = extractListing(anchor, query);
                if (listing != null) results.add(listing);
            } catch (Exception e) {
                failures++;
                log.debug("fb.extract.error error={}", e.getMessage());
            }
        }
        if (failures > 0) {
            log.debug("fb.extract.failures count={}", failures);
        }
        return results;
    }

    private RawMarketplaceListing extractListing(ElementHandle anchor, MarketplaceSearchQuery query) {
        String href = anchor.getAttribute("href");
        if (href == null || href.isBlank()) return null;

        String listingUrl = href.startsWith("http") ? href : "https://www.facebook.com" + href;
        String externalId = extractItemId(href);
        if (externalId == null) return null;

        ElementHandle img = anchor.querySelector("img");
        String imageUrl = img != null ? img.getAttribute("src") : null;

        // nested spans may duplicate text — extract price, title, location in order
        List<ElementHandle> spans = anchor.querySelectorAll("span");
        String priceText = null;
        String title     = null;
        String location  = null;

        for (ElementHandle span : spans) {
            String text = span.innerText().trim();
            if (text.isEmpty()) continue;

            if (priceText == null && looksLikePrice(text)) {
                priceText = text;
            } else if (priceText != null && title == null && !looksLikePrice(text)) {
                title = text;
            } else if (title != null && location == null && !text.equals(title) && !looksLikePrice(text)) {
                location = text;
                break;
            }
        }

        if (title == null || title.isBlank()) {
            log.debug("fb.skip reason=no_title externalId={}", externalId);
            return null;
        }

        BigDecimal price = parsePrice(priceText);
        if (price == null) {
            log.debug("fb.skip reason=unparseable_price externalId={} priceText={}", externalId, priceText);
            return null;
        }

        String rawPayload = buildRawPayload(externalId, title, priceText, location, listingUrl,
                query.sourceCategory(), query.sourceCity());

        log.debug("fb.listing externalId={} title=\"{}\" price={} location={}", externalId, title, price, location);

        return new RawMarketplaceListing(
                MarketplaceSource.FACEBOOK_MARKETPLACE,
                externalId,
                title,
                null,
                price,
                "AUD",
                location,
                null,
                listingUrl,
                imageUrl,
                query.keyword(),
                null, null, null,
                rawPayload,
                query.sourceCategory(),
                query.sourceCity()
        );
    }

    // helpers

    private String buildSearchUrl(String baseUrl, String keyword) {
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        return base + "search?query=" + encoded;
    }

    private String extractItemId(String href) {
        Matcher m = ITEM_ID_PATTERN.matcher(href);
        return m.find() ? m.group(1) : null;
    }

    private boolean looksLikePrice(String text) {
        if (text.equalsIgnoreCase("Free")) return true;
        return text.matches(".*\\d.*") && text.length() <= 20
                && PRICE_PATTERN.matcher(text.replace(",", "")).find();
    }

    private BigDecimal parsePrice(String priceText) {
        if (priceText == null) return null;
        if (priceText.equalsIgnoreCase("Free")) return BigDecimal.ZERO;

        String digits = priceText.replaceAll("[^\\d.]", "");
        if (digits.isEmpty()) return null;

        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildRawPayload(String externalId, String title, String price,
                                    String location, String url,
                                    String sourceCategory, String sourceCity) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "id", externalId,
                    "title", title,
                    "price", price != null ? price : "",
                    "location", location != null ? location : "",
                    "url", url,
                    "sourceCategory", sourceCategory != null ? sourceCategory : "",
                    "sourceCity", sourceCity != null ? sourceCity : "",
                    "scrapedAt", OffsetDateTime.now().toString()
            ));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    // stub mode

    private List<RawMarketplaceListing> searchStub(MarketplaceSearchQuery query) {
        log.info("fb.provider.stub keyword=\"{}\"", query.keyword());

        String keyword = query.keyword();
        int count = Math.min(query.maxResults(), 6);
        java.util.Random rng = new java.util.Random(keyword.hashCode());
        double base = 200 + rng.nextInt(600);

        List<RawMarketplaceListing> results = new ArrayList<>(count);
        String[] locations = {"Perth, Western Australia", "Fremantle WA", "Joondalup WA", "Rockingham WA"};

        for (int i = 0; i < count; i++) {
            double priceFactor = 0.5 + rng.nextDouble();
            BigDecimal price = BigDecimal.valueOf(Math.round(base * priceFactor));
            String externalId = "stub_" + Math.abs((keyword + i).hashCode());
            String title = capitalise(keyword) + (i > 0 ? " - Variant " + (char)('A' + i) : "");
            String location = locations[rng.nextInt(locations.length)];

            results.add(new RawMarketplaceListing(
                    MarketplaceSource.FACEBOOK_MARKETPLACE,
                    externalId, title, null, price, "AUD", location,
                    null,
                    "https://www.facebook.com/marketplace/item/" + externalId,
                    null, keyword, null, null, null,
                    "{\"stub\":true,\"id\":\"" + externalId + "\"}",
                    query.sourceCategory(),
                    query.sourceCity()
            ));
        }

        return results;
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return java.util.Arrays.stream(s.split("\\s+"))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
