package com.example.ticktetapi.controller;

import com.example.ticktetapi.dto.EventoRequestDTO;
import com.example.ticktetapi.exception.RecursoNaoEncontradoException;
import com.example.ticktetapi.model.Evento;
import com.example.ticktetapi.service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventoController.class)
class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventoService eventoService;

    private EventoRequestDTO dto;

    private Evento salvo;

    @BeforeEach
    void setUp() {

        dto = new EventoRequestDTO();
        dto.setNome("Show de Rock");
        dto.setDataHora(LocalDateTime.now().plusDays(30));
        dto.setLocal("Arena de Pernambuco");
        dto.setPreco(new BigDecimal("150.00"));
        dto.setQuantidadeTotal(100);

        salvo = Evento.builder()
                .id(1L)
                .nome(dto.getNome())
                .dataHora(dto.getDataHora())
                .local(dto.getLocal())
                .preco(dto.getPreco())
                .quantidadeTotal(100)
                .quantidadeDisponivel(100)
                .build();
    }


    @Test
    void deveCriarEventoERetornar201() throws Exception {

        when(eventoService.criar(any())).thenReturn(salvo);

        mockMvc.perform(post("/api/eventos")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(100));
    }

    @Test
    void deveRetornar400QuandoNomeForVazio() throws Exception {

        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setNome("");
        dto.setDataHora(LocalDateTime.now().plusDays(30));
        dto.setLocal("Arena Recife");
        dto.setPreco(new BigDecimal("150.00"));
        dto.setQuantidadeTotal(100);

        mockMvc.perform(post("/api/eventos")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes").isArray());
    }

    @Test
    void deveRetornar404QuandoEventoNaoExistir() throws Exception {
        when(eventoService.buscarPorId(999L))
                .thenThrow( new RecursoNaoEncontradoException("Evento não encontrado com o id: 999"));

        mockMvc.perform(get("/api/eventos/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar200QuandoBuscarEventoJaCadastrado() throws Exception {
        when(eventoService.buscarPorId(1L))
                .thenReturn(salvo);

        mockMvc.perform(get("/api/eventos/{id}", 1L))
                .andExpect(status().isOk());
    }

}
