package com.earningslens.document;

import com.earningslens.ingest.DocType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row of the documents table (V1__init.sql). Spec section 4: JPA for documents,
 * JdbcTemplate for chunks (JPA does not handle the vector type well).
 *
 * The column types must match the SQL exactly, because Hibernate checks them at startup
 * (spring.jpa.hibernate.ddl-auto: validate). That is why fiscal_quarter is a short
 * (SMALLINT) and content_sha256 is marked as CHAR.
 *
 * JPA CONCEPT: an entity needs a no-argument constructor (Hibernate uses it to create
 * objects from rows); "protected" keeps our own code from calling it by accident.
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "fiscal_quarter", nullable = false)
    private short fiscalQuarter;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private DocType docType;

    @Column(name = "source_url", nullable = false, columnDefinition = "text")
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_sha256", nullable = false, length = 64, unique = true)
    private String contentSha256;

    // Filled by the database default now(), so Hibernate never writes it.
    @Column(name = "ingested_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "timestamptz")
    private OffsetDateTime ingestedAt;

    protected DocumentEntity() {
    }

    public DocumentEntity(UUID id, String ticker, String companyName, int fiscalYear, int fiscalQuarter,
                          DocType docType, String sourceUrl, String contentSha256) {
        this.id = id;
        this.ticker = ticker;
        this.companyName = companyName;
        this.fiscalYear = fiscalYear;
        this.fiscalQuarter = (short) fiscalQuarter;
        this.docType = docType;
        this.sourceUrl = sourceUrl;
        this.contentSha256 = contentSha256;
    }

    public UUID getId() { return id; }
    public String getTicker() { return ticker; }
    public String getCompanyName() { return companyName; }
    public int getFiscalYear() { return fiscalYear; }
    public int getFiscalQuarter() { return fiscalQuarter; }
    public DocType getDocType() { return docType; }
    public String getSourceUrl() { return sourceUrl; }
    public String getContentSha256() { return contentSha256; }
    public OffsetDateTime getIngestedAt() { return ingestedAt; }
}
