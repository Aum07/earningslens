package com.earningslens.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roadmap E4.6: GET /api/v1/eval/latest returns the report as written, or 404 before the first eval run. */
@WebMvcTest(EvalController.class)
class EvalControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean LatestEvalReport latest;

    @Test
    void returnsTheReportUnchanged() throws Exception {
        String report = """
                {"split": "test", "configs": [{"config": "C", "recallAtKAll": 0.5}]}""";
        when(latest.read()).thenReturn(Optional.of(JsonMapper.builder().build().readTree(report)));

        mvc.perform(get("/api/v1/eval/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.split").value("test"))
                .andExpect(jsonPath("$.configs[0].config").value("C"))
                .andExpect(jsonPath("$.configs[0].recallAtKAll").value(0.5));
    }

    @Test
    void noReportYetIs404() throws Exception {
        when(latest.read()).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/eval/latest")).andExpect(status().isNotFound());
    }
}
