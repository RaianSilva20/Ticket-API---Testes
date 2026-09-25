package com.example.ticktetapi.service;

import com.example.ticktetapi.dto.EventoRequestDTO;
import com.example.ticktetapi.exception.RecursoNaoEncontradoException;
import com.example.ticktetapi.exception.RegraNegocioException;
import com.example.ticktetapi.model.Evento;
import com.example.ticktetapi.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class EventoServiceTest {

	private EventoRepository eventoRepository;
	private EventoService eventoService;

	@BeforeEach
	void configurar() {
		eventoRepository = mock(EventoRepository.class);
		eventoService = new EventoService(eventoRepository);
	}

	@Test
	void deveCriarEventoComQuantidadeDisponivelIgualATotal() {
		EventoRequestDTO dto = eventoDto(100);
		when(eventoRepository.save(any(Evento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		Evento resultado = eventoService.criar(dto);

		assertEquals(dto.getNome(), resultado.getNome());
		assertEquals(dto.getDescricao(), resultado.getDescricao());
		assertEquals(dto.getDataHora(), resultado.getDataHora());
		assertEquals(dto.getLocal(), resultado.getLocal());
		assertEquals(dto.getPreco(), resultado.getPreco());
		assertEquals(100, resultado.getQuantidadeTotal());
		assertEquals(100, resultado.getQuantidadeDisponivel());
		verify(eventoRepository).save(resultado);
	}

	@Test
	void deveListarTodosOsEventos() {
		List<Evento> eventos = List.of(evento(1L, 10, 10));
		when(eventoRepository.findAll()).thenReturn(eventos);

		assertSame(eventos, eventoService.listarTodos());

		verify(eventoRepository).findAll();
	}

	@Test
	void deveBuscarEventoPorId() {
		Evento evento = evento(7L, 10, 10);
		when(eventoRepository.findById(7L)).thenReturn(Optional.of(evento));

		assertSame(evento, eventoService.buscarPorId(7L));

		verify(eventoRepository).findById(7L);
	}

	@Test
	void deveLancarExcecaoAoBuscarEventoInexistente() {
		when(eventoRepository.findById(7L)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException excecao = assertThrows(RecursoNaoEncontradoException.class,
				() -> eventoService.buscarPorId(7L));

		assertEquals("Evento não encontrado com id: 7", excecao.getMessage());
	}

	@Test
	void deveAtualizarEventoPreservandoIngressosVendidos() {
		Evento evento = evento(7L, 100, 70);
		EventoRequestDTO dto = eventoDto(120);
		dto.setNome("Novo evento");
		when(eventoRepository.findById(7L)).thenReturn(Optional.of(evento));
		when(eventoRepository.save(evento)).thenReturn(evento);

		Evento resultado = eventoService.atualizar(7L, dto);

		assertSame(evento, resultado);
		assertEquals("Novo evento", evento.getNome());
		assertEquals(90, evento.getQuantidadeDisponivel());
		assertEquals(120, evento.getQuantidadeTotal());
		verify(eventoRepository).save(evento);
	}

	@Test
	void naoDeveAtualizarEventoQuandoNovaQuantidadeForMenorQueIngressosVendidos() {
		Evento evento = evento(7L, 100, 70);
		EventoRequestDTO dto = eventoDto(29);
		when(eventoRepository.findById(7L)).thenReturn(Optional.of(evento));

		RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
				() -> eventoService.atualizar(7L, dto));

		assertEquals("A nova quantidade total não pode ser menor que a quantidade já vendida (30)", excecao.getMessage());
		verify(eventoRepository, never()).save(any(Evento.class));
	}

	@Test
	void deveDeletarEventoEncontrado() {
		Evento evento = evento(7L, 10, 10);
		when(eventoRepository.findById(7L)).thenReturn(Optional.of(evento));

		eventoService.deletar(7L);

		verify(eventoRepository).findById(7L);
		verify(eventoRepository).delete(evento);
		verifyNoMoreInteractions(eventoRepository);
	}

	@Test
	void naoDeveDeletarEventoInexistente() {
		when(eventoRepository.findById(7L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> eventoService.deletar(7L));

		verify(eventoRepository, never()).delete(any(Evento.class));
	}

	private EventoRequestDTO eventoDto(int quantidadeTotal) {
		EventoRequestDTO dto = new EventoRequestDTO();
		dto.setNome("Evento de teste");
		dto.setDescricao("Descrição");
		dto.setDataHora(LocalDateTime.now().plusDays(1));
		dto.setLocal("Auditório");
		dto.setPreco(BigDecimal.TEN);
		dto.setQuantidadeTotal(quantidadeTotal);
		return dto;
	}

	private Evento evento(Long id, int quantidadeTotal, int quantidadeDisponivel) {
		return Evento.builder()
				.id(id)
				.nome("Evento")
				.dataHora(LocalDateTime.now().plusDays(1))
				.local("Auditório")
				.preco(BigDecimal.TEN)
				.quantidadeTotal(quantidadeTotal)
				.quantidadeDisponivel(quantidadeDisponivel)
				.build();
	}
}
