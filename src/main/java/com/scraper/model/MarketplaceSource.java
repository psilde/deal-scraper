package com.scraper.model;

public enum MarketplaceSource {

    FACEBOOK_MARKETPLACE;

    public String displayName() {
        return switch (this) {
            case FACEBOOK_MARKETPLACE -> "Facebook Marketplace";
        };
    }
}
