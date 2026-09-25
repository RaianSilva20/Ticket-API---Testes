package com.example.ticktetapi.service;

import com.example.ticktetapi.dto.CompraIngressoRequestDTO;
import com.example.ticktetapi.exception.RecursoNaoEncontradoException;
import com.example.ticktetapi.exception.RegraNegocioException;
import com.example.ticktetapi.model.Cliente;
import com.example.ticktetapi.model.Evento;
import com.example.ticktetapi.model.Ingresso;
import com.example.ticktetapi.model.StatusIngresso;
import com.example.ticktetapi.repository.ClienteRepository;
import com.example.ticktetapi.repository.EventoRepository;
import com.example.ticktetapi.repository.IngressoRepository;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class IngressoServiceTest {

	private IngressoRepository ingressoRepository;
	private EventoRepository eventoRepository;
	private ClienteRepository clienteRepository;
	private IngressoService ingressoService;

	@BeforeEach
	void configurar() {
		ingressoRepository = mock(IngressoRepository.class);
		eventoRepository = mock(EventoRepository.class);
		clienteRepository = mock(ClienteRepository.class);
		ingressoService = new IngressoService(ingressoRepository, eventoRepository, clienteRepository);
	}

	@Test
	void deveComprarQuantidadeSolicitadaEAtualizarDisponibilidade() {
		Evento evento = evento(10, 10);
		Cliente cliente = Cliente.builder().id(2L).nome("Cliente").build();
		CompraIngressoRequestDTO dto = compraDto(1L, 2L, 3);
		when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
		when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
		when(ingressoRepository.save(any(Ingresso.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		List<Ingresso> resultado = ingressoService.comprar(dto);

		assertEquals(3, resultado.size());
		assertEquals(7, evento.getQuantidadeDisponivel());
		resultado.forEach(ingresso -> {
			assertSame(evento, ingresso.getEvento());
			assertSame(cliente, ingresso.getCliente());
			assertEquals(StatusIngresso.ATIVO, ingresso.getStatus());
			assertEquals(evento.getPreco(), ingresso.getValorPago());
		});
		verify(ingressoRepository, org.mockito.Mockito.times(3)).save(any(Ingresso.class));
		verify(eventoRepository).save(evento);
	}

	@Test
	void deveUsarUmaUnidadeQuandoQuantidadeNaoForInformada() {
		Evento evento = evento(10, 10);
		Cliente cliente = Cliente.builder().id(2L).build();
		CompraIngressoRequestDTO dto = compraDto(1L, 2L, null);
		when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
		when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
		when(ingressoRepository.save(any(Ingresso.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		List<Ingresso> resultado = ingressoService.comprar(dto);

		assertEquals(1, resultado.size());
		assertEquals(9, evento.getQuantidadeDisponivel());
	}

	@Test
	void naoDeveComprarQuandoEventoJaOcorreu() {
		Evento evento = evento(10, 10);
		evento.setDataHora(LocalDateTime.now().minusMinutes(1));
		when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
		when(clienteRepository.findById(2L)).thenReturn(Optional.of(Cliente.builder().id(2L).build()));

		assertThrows(RegraNegocioException.class, () -> ingressoService.comprar(compraDto(1L, 2L, 1)));

		verify(ingressoRepository, never()).save(any(Ingresso.class));
		verify(eventoRepository, never()).save(any(Evento.class));
	}

	@Test
	void naoDeveComprarQuandoNaoHouverIngressosSuficientes() {
		Evento evento = evento(2, 2);
		when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
		when(clienteRepository.findById(2L)).thenReturn(Optional.of(Cliente.builder().id(2L).build()));

		RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
				() -> ingressoService.comprar(compraDto(1L, 2L, 3)));

		assertEquals("Ingressos esgotados. Disponível: 2, solicitado: 3", excecao.getMessage());
		verify(ingressoRepository, never()).save(any(Ingresso.class));
	}

	@Test
	void deveLancarExcecaoQuandoEventoNaoExistirNaCompra() {
		when(eventoRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> ingressoService.comprar(compraDto(1L, 2L, 1)));

		verify(clienteRepository, never()).findById(any(Long.class));
	}

	@Test
	void deveLancarExcecaoQuandoClienteNaoExistirNaCompra() {
		when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(10, 10)));
		when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> ingressoService.comprar(compraDto(1L, 2L, 1)));

		verify(ingressoRepository, never()).save(any(Ingresso.class));
	}

	@Test
	void deveListarIngressosPorEventoECliente() {
		List<Ingresso> ingressos = List.of(Ingresso.builder().id(1L).build());
		when(ingressoRepository.findByEventoId(3L)).thenReturn(ingressos);
		when(ingressoRepository.findByClienteId(4L)).thenReturn(ingressos);

		assertSame(ingressos, ingressoService.listarPorEvento(3L));
		assertSame(ingressos, ingressoService.listarPorCliente(4L));

		verify(ingressoRepository).findByEventoId(3L);
		verify(ingressoRepository).findByClienteId(4L);
	}

	@Test
	void deveListarTodosEBuscarPorId() {
		Ingresso ingresso = Ingresso.builder().id(1L).build();
		when(ingressoRepository.findAll()).thenReturn(List.of(ingresso));
		when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));

		assertEquals(List.of(ingresso), ingressoService.listarTodos());
		assertSame(ingresso, ingressoService.buscarPorId(1L));
	}

	@Test
	void deveLancarExcecaoAoBuscarIngressoInexistente() {
		when(ingressoRepository.findById(1L)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException excecao = assertThrows(RecursoNaoEncontradoException.class,
				() -> ingressoService.buscarPorId(1L));

		assertEquals("Ingresso não encontrado com id: 1", excecao.getMessage());
	}

	@Test
	void deveCancelarIngressoEDevolverDisponibilidade() {
		Evento evento = evento(10, 6);
		Ingresso ingresso = ingresso(StatusIngresso.ATIVO, evento);
		when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));
		when(ingressoRepository.save(ingresso)).thenReturn(ingresso);

		assertSame(ingresso, ingressoService.cancelar(1L));

		assertEquals(StatusIngresso.CANCELADO, ingresso.getStatus());
		assertEquals(7, evento.getQuantidadeDisponivel());
		verify(ingressoRepository).save(ingresso);
		verify(eventoRepository).save(evento);
	}

	@Test
	void naoDeveCancelarIngressoCanceladoOuUtilizado() {
		Ingresso cancelado = ingresso(StatusIngresso.CANCELADO, evento(10, 6));
		when(ingressoRepository.findById(1L)).thenReturn(Optional.of(cancelado));
		assertThrows(RegraNegocioException.class, () -> ingressoService.cancelar(1L));

		Ingresso utilizado = ingresso(StatusIngresso.UTILIZADO, evento(10, 6));
		when(ingressoRepository.findById(2L)).thenReturn(Optional.of(utilizado));
		assertThrows(RegraNegocioException.class, () -> ingressoService.cancelar(2L));

		verify(ingressoRepository, never()).save(any(Ingresso.class));
	}

	@Test
	void deveMarcarIngressoAtivoComoUtilizado() {
		Ingresso ingresso = ingresso(StatusIngresso.ATIVO, evento(10, 6));
		when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));
		when(ingressoRepository.save(ingresso)).thenReturn(ingresso);

		assertSame(ingresso, ingressoService.marcarComoUtilizado(1L));

		assertEquals(StatusIngresso.UTILIZADO, ingresso.getStatus());
		verify(ingressoRepository).save(ingresso);
	}

	@Test
	void naoDeveMarcarIngressoNaoAtivoComoUtilizado() {
		Ingresso ingresso = ingresso(StatusIngresso.CANCELADO, evento(10, 6));
		when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));

		assertThrows(RegraNegocioException.class, () -> ingressoService.marcarComoUtilizado(1L));

		verify(ingressoRepository, never()).save(any(Ingresso.class));
	}

	private CompraIngressoRequestDTO compraDto(Long eventoId, Long clienteId, Integer quantidade) {
		CompraIngressoRequestDTO dto = new CompraIngressoRequestDTO();
		dto.setEventoId(eventoId);
		dto.setClienteId(clienteId);
		dto.setQuantidade(quantidade);
		return dto;
	}

	private Evento evento(int quantidadeTotal, int quantidadeDisponivel) {
		return Evento.builder()
				.dataHora(LocalDateTime.now().plusDays(1))
				.preco(BigDecimal.TEN)
				.quantidadeTotal(quantidadeTotal)
				.quantidadeDisponivel(quantidadeDisponivel)
				.build();
	}

	private Ingresso ingresso(StatusIngresso status, Evento evento) {
		return Ingresso.builder().status(status).evento(evento).build();
	}
}
