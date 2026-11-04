package com.earningslens.api;

import com.earningslens.PgvectorContainerTest;
import com.earningslens.api.dto.CompanyDto;
import com.earningslens.api.dto.DocumentDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap E4.6: documents with chunk counts and companies with quarters, against the pgvector test container. */
class CorpusServiceTest extends PgvectorContainerTest {

    @Autowired CorpusService corpus;

    private UUID msftQ3PressRelease;

    @BeforeEach
    void setUp() throws SQLException {
        clearDatabase();
        // One document with two chunks: insert the first, then add a second chunk to the same document.
        UUID firstChunk = insertChunk("MSFT", new FiscalPeriod(2026, 3), DocType.PRESS_RELEASE, "Revenue grew.", vector(1));
        msftQ3PressRelease = jdbc.queryForObject("SELECT document_id FROM chunks WHERE id = ?", UUID.class, firstChunk);
        jdbc.update("""
                INSERT INTO chunks (id, document_id, chunk_index, ticker, fiscal_year, fiscal_quarter, doc_type,
                                    content, token_count, embedding)
                SELECT ?, document_id, 1, ticker, fiscal_year, fiscal_quarter, doc_type, 'Margins rose.', 10, embedding
                FROM chunks WHERE id = ?
                """, UUID.randomUUID(), firstChunk);

        insertChunk("MSFT", new FiscalPeriod(2026, 2), DocType.TRANSCRIPT, "Demand was strong.", vector(1));
        insertChunk("GOOGL", new FiscalPeriod(2026, 1), DocType.FORM_10Q, "Cloud grew.", vector(1));
    }

    @Test
    void documentsAreSortedAndCountTheirChunks() {
        List<DocumentDto> documents = corpus.documents();

        assertThat(documents).extracting(DocumentDto::ticker, DocumentDto::fiscalYear, DocumentDto::fiscalQuarter,
                        DocumentDto::docType, DocumentDto::chunkCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("GOOGL", 2026, 1, DocType.FORM_10Q, 1),
                        org.assertj.core.groups.Tuple.tuple("MSFT", 2026, 2, DocType.TRANSCRIPT, 1),
                        org.assertj.core.groups.Tuple.tuple("MSFT", 2026, 3, DocType.PRESS_RELEASE, 2));
        DocumentDto last = documents.get(2);
        assertThat(last.id()).isEqualTo(msftQ3PressRelease);
        assertThat(last.companyName()).isEqualTo("MSFT Inc.");
        assertThat(last.sourceUrl()).startsWith("https://example.com/");
    }

    @Test
    void companiesFollowCompaniesYamlAndSkipCompaniesWithoutDocuments() {
        List<CompanyDto> companies = corpus.companies();

        assertThat(companies).extracting(CompanyDto::ticker).containsExactly("MSFT", "GOOGL");   // no META documents
        CompanyDto msft = companies.get(0);
        assertThat(msft.name()).isEqualTo("Microsoft Corporation");
        assertThat(msft.fiscalYearEndMonth()).isEqualTo(6);
        assertThat(msft.periods()).containsExactly(new FiscalPeriodDto(2026, 2), new FiscalPeriodDto(2026, 3));
    }
}
