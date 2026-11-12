package com.earningslens.api;

import com.earningslens.api.dto.KeyFiguresDto;
import com.earningslens.figures.KeyFiguresService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** JSON shape of GET /api/v1/key-figures (the service is a mock). */
@WebMvcTest(KeyFiguresController.class)
class KeyFiguresControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean KeyFiguresService service;

    @Test
    void returnsFiguresPerCompany() throws Exception {
        when(service.keyFigures()).thenReturn(List.of(new KeyFiguresDto("MSFT", "Microsoft Corporation", true,
                "https://data.sec.gov/api/xbrl/companyfacts/CIK0000789019.json", Instant.parse("2026-11-05T12:00:00Z"),
                List.of(new KeyFiguresDto.Quarter(2026, 4, LocalDate.of(2026, 6, 30), new BigDecimal("90007000000"),
                        new BigDecimal("35766000000"), null, true, "10-K", LocalDate.of(2026, 7, 29),
                        "https://www.sec.gov/Archives/edgar/data/789019/000095017026000004/")))));

        mvc.perform(get("/api/v1/key-figures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("MSFT"))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].quarters[0].fiscalQuarter").value(4))
                .andExpect(jsonPath("$[0].quarters[0].revenue").value(90007000000L))
                .andExpect(jsonPath("$[0].quarters[0].epsDiluted").doesNotExist())
                .andExpect(jsonPath("$[0].quarters[0].derived").value(true))
                .andExpect(jsonPath("$[0].quarters[0].periodEnd").value("2026-06-30"));
    }
}
