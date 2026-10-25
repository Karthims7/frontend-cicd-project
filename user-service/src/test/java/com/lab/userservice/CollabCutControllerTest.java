package com.lab.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollabCutBlueprintController.class)
class CollabCutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetRepository assetRepository;

    @MockitoBean
    private CommentRepository commentRepository;

    @MockitoBean
    private EventLogRepository eventLogRepository;

    @Test
    void getAllAssetsReturnsList() throws Exception {
        Asset asset = new Asset("Scene 14B", 3, "Roll A-04", "Alley Confrontation", "s3://raw/14b.mp4", "Look at the horizon.");
        asset.setId(1L);

        when(assetRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Alley Confrontation"))
                .andExpect(jsonPath("$[0].sceneId").value("Scene 14B"));
    }
}
