package com.earningslens.config;

import com.earningslens.api.RateLimitInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS and rate limiting for the REST API (spec 13, roadmap E4.7).
 * CORS: only the frontend origin(s) from earningslens.api.cors-allowed-origins may call /api/** from a browser.
 * Rate limit: only POST /api/v1/chat (the call that costs money).
 *
 * @EnableConfigurationProperties: @WebMvcTest slices load this class but not the @ConfigurationPropertiesScan on
 * BackendApplication, so the two settings records are registered here too (the full app skips the duplicate).
 */
@Configuration
@EnableConfigurationProperties({ApiProperties.class, RateLimitProperties.class})
public class WebConfig implements WebMvcConfigurer {

    private final ApiProperties api;
    private final RateLimitInterceptor rateLimit;

    public WebConfig(ApiProperties api, RateLimitInterceptor rateLimit) {
        this.api = api;
        this.rateLimit = rateLimit;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(api.corsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimit).addPathPatterns("/api/v1/chat");
    }
}
