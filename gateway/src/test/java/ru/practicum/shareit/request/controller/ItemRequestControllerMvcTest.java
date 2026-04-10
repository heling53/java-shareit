package ru.practicum.shareit.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.ShareItGateway;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemRequestController.class)
@ContextConfiguration(classes = ShareItGateway.class)
class ItemRequestControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ru.practicum.shareit.request.ItemRequestClient itemRequestClient;

    private static final String HEADER_USER_ID = "X-Sharer-User-Id";

    @Test
    void addRequest_ShouldReturnOk() throws Exception {
        ItemRequestDto input = new ItemRequestDto();
        input.setDescription("Test description");

        when(itemRequestClient.addRequest(anyLong(), any(ItemRequestDto.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        mockMvc.perform(post("/requests")
                        .header(HEADER_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserRequests_ShouldReturnOk() throws Exception {
        when(itemRequestClient.getUserRequests(anyLong()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        mockMvc.perform(get("/requests")
                        .header(HEADER_USER_ID, 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRequests_ShouldReturnOk() throws Exception {
        when(itemRequestClient.getAllRequests(anyLong(), anyInt(), anyInt()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        mockMvc.perform(get("/requests/all")
                        .header(HEADER_USER_ID, 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getRequestById_ShouldReturnOk() throws Exception {
        when(itemRequestClient.getRequestById(anyLong(), anyLong()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        mockMvc.perform(get("/requests/1")
                        .header(HEADER_USER_ID, 1L))
                .andExpect(status().isOk());
    }
}