package com.earningslens.api;

import com.earningslens.api.dto.CompanyDto;
import com.earningslens.api.dto.DocumentDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roadmap E4.6: JSON shape of GET /api/v1/documents and GET /api/v1/companies (CorpusService is a mock). */
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    private static final UUID DOC = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Autowired MockMvc mvc;

    @MockitoBean CorpusService corpus;

    @Test
    void documentsListsEachDocumentWithItsChunkCount() throws Exception {
        when(corpus.documents()).thenReturn(List.of(new DocumentDto(DOC, "MSFT", "Microsoft Corporation", 2026, 2,
                DocType.PRESS_RELEASE, "https://www.sec.gov/example.htm", 41)));

        mvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(DOC.toString()))
                .andExpect(jsonPath("$[0].ticker").value("MSFT"))
                .andExpect(jsonPath("$[0].companyName").value("Microsoft Corporation"))
                .andExpect(jsonPath("$[0].fiscalYear").value(2026))
                .andExpect(jsonPath("$[0].fiscalQuarter").value(2))
                .andExpect(jsonPath("$[0].docType").value("PRESS_RELEASE"))
                .andExpect(jsonPath("$[0].sourceUrl").value("https://www.sec.gov/example.htm"))
                .andExpect(jsonPath("$[0].chunkCount").value(41));
    }

    @Test
    void companiesListsTickersWithTheirQuarters() throws Exception {
        when(corpus.companies()).thenReturn(List.of(new CompanyDto("MSFT", "Microsoft Corporation", 6,
                List.of(new FiscalPeriodDto(2026, 2), new FiscalPeriodDto(2026, 3)))));

        mvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("MSFT"))
                .andExpect(jsonPath("$[0].name").value("Microsoft Corporation"))
                .andExpect(jsonPath("$[0].fiscalYearEndMonth").value(6))
                .andExpect(jsonPath("$[0].periods", hasSize(2)))
                .andExpect(jsonPath("$[0].periods[0].year").value(2026))
                .andExpect(jsonPath("$[0].periods[0].quarter").value(2));
    }

    @Test
    void emptyCorpusGivesEmptyLists() throws Exception {
        when(corpus.documents()).thenReturn(List.of());
        when(corpus.companies()).thenReturn(List.of());

        mvc.perform(get("/api/v1/documents")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/companies")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }
}
