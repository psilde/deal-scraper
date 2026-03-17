package com.scraper.provider;

import com.microsoft.playwright.*;
import com.microsoft.playwright.ElementHandle;
import com.scraper.config.MarketplaceProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class PlaywrightBrowserManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightBrowserManager.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36";

    private static final Path SESSION_FILE = Path.of("data/fb-session.json");

    private final MarketplaceProperties properties;

    private Playwright playwright;
    private Browser browser;
    private volatile boolean available = false;
    private volatile boolean sessionLoaded = false;

    public PlaywrightBrowserManager(MarketplaceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(List.of(
                                    "--disable-blink-features=AutomationControlled",
                                    "--no-sandbox",
                                    "--disable-dev-shm-usage",
                                    "--disable-gpu"
                            ))
            );
            available = true;
            log.info("playwright.browser.ready engine=chromium");

            MarketplaceProperties.Facebook fb = properties.getFacebook();
            String username = fb.getUsername();
            String password = fb.getPassword();

            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                login(username, password);
            } else {
                log.info("playwright.session.no_credentials — scraping unauthenticated (max ~48 results per search)");
            }

        } catch (Exception e) {
            log.warn("playwright.browser.unavailable — Facebook scraping disabled. " +
                    "Install browsers with: mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI " +
                    "-D exec.args=\"install chromium\" | error={}", e.getMessage());
        }
    }

    private void login(String username, String password) {
        // reuse saved session if available
        if (SESSION_FILE.toFile().exists()) {
            log.info("playwright.session.reusing_saved file={}", SESSION_FILE);
            sessionLoaded = true;
            return;
        }

        log.info("playwright.session.logging_in user={}", username);
        try (BrowserContext ctx = browser.newContext(baseContextOptions())) {
            Page page = ctx.newPage();
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            page.navigate("https://www.facebook.com/login", new Page.NavigateOptions().setTimeout(30_000));
            page.waitForTimeout(2000);

            // dismiss cookie banner if present
            for (String sel : List.of(
                    "[data-cookiebanner='accept_button']",
                    "[data-testid='cookie-policy-manage-dialog-accept-button']",
                    "button[title='Allow all cookies']",
                    "button[title='Accept all']",
                    "[aria-label='Allow all cookies']")) {
                try {
                    ElementHandle btn = page.querySelector(sel);
                    if (btn != null) { btn.click(); page.waitForTimeout(1000); break; }
                } catch (Exception ignored) {}
            }

            String emailSel = null;
            for (String sel : List.of("#email", "input[name='email']", "input[type='email']")) {
                try {
                    page.waitForSelector(sel, new Page.WaitForSelectorOptions().setTimeout(10_000));
                    emailSel = sel;
                    break;
                } catch (Exception ignored) {}
            }

            if (emailSel == null) {
                // save screenshot for debugging
                java.nio.file.Files.write(Path.of("debug-login.png"), page.screenshot());
                log.warn("playwright.session.login_failed error=could_not_find_email_field url={} " +
                         "— screenshot saved to debug-login.png", page.url());
                return;
            }

            page.fill(emailSel, username);

            String passSel = null;
            for (String sel : List.of("input[name='pass']", "#pass", "input[type='password']")) {
                try {
                    ElementHandle el = page.querySelector(sel);
                    if (el != null) { passSel = sel; break; }
                } catch (Exception ignored) {}
            }
            if (passSel == null) {
                log.warn("playwright.session.login_failed error=could_not_find_password_field");
                return;
            }
            page.fill(passSel, password);
            page.keyboard().press("Enter");

            try {
                page.waitForURL(url -> !url.contains("facebook.com/login"),
                        new Page.WaitForURLOptions().setTimeout(25_000));
            } catch (Exception e) {
                java.nio.file.Files.write(Path.of("debug-login.png"), page.screenshot());
                log.warn("playwright.session.login_no_redirect url={} — screenshot saved to debug-login.png", page.url());
                return;
            }

            String currentUrl = page.url();
            log.debug("playwright.session.post_login_url url={}", currentUrl);

            if (currentUrl.contains("checkpoint") || currentUrl.contains("two_step") || currentUrl.contains("2fa")) {
                java.nio.file.Files.write(Path.of("debug-login.png"), page.screenshot());
                log.warn("playwright.session.login_checkpoint url={} — 2FA or security check required; " +
                         "complete it manually then restart. Screenshot saved to debug-login.png", currentUrl);
                return;
            }

            // save session so future pages are authenticated
            SESSION_FILE.toFile().getParentFile().mkdirs();
            ctx.storageState(new BrowserContext.StorageStateOptions().setPath(SESSION_FILE));
            sessionLoaded = true;
            log.info("playwright.session.login_ok session_saved={}", SESSION_FILE);

        } catch (Exception e) {
            log.warn("playwright.session.login_failed error={} — continuing without session", e.getMessage());
        }
    }

    public Page newPage() {
        if (!available) {
            throw new IllegalStateException(
                    "Playwright browser not available. " +
                    "Run: mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install chromium\"");
        }

        Browser.NewContextOptions opts = baseContextOptions();
        if (sessionLoaded && SESSION_FILE.toFile().exists()) {
            opts.setStorageStatePath(SESSION_FILE);
        }

        BrowserContext context = browser.newContext(opts);
        Page page = context.newPage();
        page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        return page;
    }

    public boolean refreshSession() {
        log.info("playwright.session.refreshing — deleting stale session and re-logging in");
        sessionLoaded = false;
        SESSION_FILE.toFile().delete();

        MarketplaceProperties.Facebook fb = properties.getFacebook();
        String username = fb.getUsername();
        String password = fb.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("playwright.session.refresh_skipped — no credentials configured");
            return false;
        }

        login(username, password);
        return sessionLoaded;
    }

    public boolean isAvailable() {
        return available;
    }

    private Browser.NewContextOptions baseContextOptions() {
        return new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setViewportSize(1280, 900)
                .setLocale("en-AU");
    }

    @Override
    public void destroy() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
        }
        log.info("playwright.browser.closed");
    }
}
