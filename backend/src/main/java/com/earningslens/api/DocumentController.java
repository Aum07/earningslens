package com.earningslens.api;

import com.earningslens.api.dto.CompanyDto;
import com.earningslens.api.dto.DocumentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/v1/documents and GET /api/v1/companies (spec 10, roadmap E4.6). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Corpus")
public class DocumentController {

    private final CorpusService corpus;

    public DocumentController(CorpusService corpus) {
        this.corpus = corpus;
    }

    /** Corpus page: every document with its chunk count. */
    @GetMapping("/documents")
    @Operation(summary = "List the documents in the corpus",
            description = "One item per press release, earnings call transcript, 10-Q or 10-K, with its source URL "
                    + "and how many chunks it was split into. Sorted by ticker, quarter (oldest first) and type.")
    public List<DocumentDto> documents() {
        return corpus.documents();
    }

    /** Filter dropdowns: companies and the quarters held for each. */
    @GetMapping("/companies")
    @Operation(summary = "List the companies and their fiscal quarters",
            description = "For the filter dropdowns. Quarters are the companies' own fiscal quarters: Microsoft's "
                    + "fiscal year ends in June (fiscalYearEndMonth 6), Alphabet's and Meta's in December.")
    public List<CompanyDto> companies() {
        return corpus.companies();
    }
}
