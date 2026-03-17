package com.scraper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MarketplaceProperties.class)
public class MarketplaceConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marketplace Scraper API")
                        .version("1.0")
                        .description("""
                                Facebook Marketplace ingestion service.
                                Scrapes listings, normalises them, deduplicates,
                                and exposes results for consumption by FindADeal.
                                """)
                );
    }
}
