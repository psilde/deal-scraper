package com.scraper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketplace")
public class MarketplaceProperties {

    private boolean enabled = true;
    private int scrapeIntervalMinutes = 30;
    private int maxResultsPerSearch = 50;
    private Facebook facebook = new Facebook();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getScrapeIntervalMinutes() { return scrapeIntervalMinutes; }
    public void setScrapeIntervalMinutes(int v) { this.scrapeIntervalMinutes = v; }

    public int getMaxResultsPerSearch() { return maxResultsPerSearch; }
    public void setMaxResultsPerSearch(int v) { this.maxResultsPerSearch = v; }

    public Facebook getFacebook() { return facebook; }
    public void setFacebook(Facebook facebook) { this.facebook = facebook; }

    public static class Facebook {

        private boolean enabled = true;
        private boolean stubMode = true;
        private String apiBaseUrl = "https://marketplace.facebook.com";
        // login required to exceed ~48 results per search
        private String username = "";
        private String password = "";
        private int timeoutSeconds = 10;
        private int retryMaxAttempts = 3;
        private int retryBackoffMillis = 1000;
        private int maxScrollAttempts = 20;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public boolean isStubMode() { return stubMode; }
        public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }

        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }

        public int getRetryMaxAttempts() { return retryMaxAttempts; }
        public void setRetryMaxAttempts(int v) { this.retryMaxAttempts = v; }

        public int getRetryBackoffMillis() { return retryBackoffMillis; }
        public void setRetryBackoffMillis(int v) { this.retryBackoffMillis = v; }

        public int getMaxScrollAttempts() { return maxScrollAttempts; }
        public void setMaxScrollAttempts(int v) { this.maxScrollAttempts = v; }
    }
}
