package com.earningslens.retrieval;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Creates the retrieval beans that need a little setup: the company directory from companies.yaml. */
@Configuration
public class RetrievalConfig {

    @Bean
    public CompanyDirectory companyDirectory() {
        try (InputStream in = new ClassPathResource("companies.yaml").getInputStream()) {
            return CompanyDirectory.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read companies.yaml", e);
        }
    }

    @Bean
    public QueryParser queryParser(CompanyDirectory companies) {
        return new QueryParser(companies);
    }

    @Bean
    public PeriodResolver periodResolver(CompanyDirectory companies, PeriodCatalog catalog) {
        return new PeriodResolver(companies, catalog);
    }
}
