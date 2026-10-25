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

@WebMvcTest(CollabCutController.class)
class CollabCutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClipRepository clipRepository;

    @MockitoBean
    private TimecodedNoteRepository noteRepository;

    @Test
    void getAllClipsReturnsList() throws Exception {
        Clip clip = new Clip("Alley Confrontation", "Scene 14B", 3, "Roll A-04", "Look at the horizon.");
        clip.setId(1L);

        when(clipRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(clip));

        mockMvc.perform(get("/api/clips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Alley Confrontation"))
                .andExpect(jsonPath("$[0].scene").value("Scene 14B"));
    }
}
