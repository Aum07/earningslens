package com.earningslens.api;

import com.earningslens.api.dto.KeyFiguresDto;
import com.earningslens.figures.KeyFiguresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/v1/key-figures: official quarterly revenue, net income and EPS from SEC EDGAR (added 2026-11-05). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Corpus")
public class KeyFiguresController {

    private final KeyFiguresService keyFigures;

    public KeyFiguresController(KeyFiguresService keyFigures) {
        this.keyFigures = keyFigures;
    }

    @GetMapping("/key-figures")
    @Operation(summary = "Get official key figures for the companies in the corpus",
            description = "Quarterly revenue, net income and diluted EPS from the companies' own XBRL data on SEC "
                    + "EDGAR (data.sec.gov company facts), for the quarters in the corpus. Fourth quarters are "
                    + "derived as full year minus nine months (derived=true, no EPS). A company whose SEC data "
                    + "cannot be loaded right now has available=false.")
    public List<KeyFiguresDto> keyFigures() {
        return keyFigures.keyFigures();
    }
}
