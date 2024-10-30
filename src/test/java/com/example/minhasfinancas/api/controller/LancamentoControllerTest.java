package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.MinhasfinancasApplication;
import com.example.minhasfinancas.api.dto.AtualizaStatusDTO;
import com.example.minhasfinancas.api.dto.LancamentoDTO;
import com.example.minhasfinancas.model.entity.Lancamento;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.model.enums.StatusLancamento;
import com.example.minhasfinancas.model.repository.LancamentoRepository;
import com.example.minhasfinancas.service.LancamentoService;
import com.example.minhasfinancas.service.impl.UsuarioServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = MinhasfinancasApplication.class)
@AutoConfigureTestEntityManager
@Transactional
public class LancamentoControllerTest {

    @Autowired
    private LancamentoController lancamentoController;

    @MockBean
    private LancamentoService service;

    @MockBean
    private UsuarioServiceImpl usuarioServiceImpl;
    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Test
    public void naoDeveEfetivarLancamentoComDataFutura() {
        // Cenário
        Lancamento lancamento = new Lancamento();
        lancamento.setId(1L);
        lancamento.setMes(LocalDate.now().getMonthValue() + 1);
        lancamento.setAno(LocalDate.now().getYear());
        lancamento.setStatus(StatusLancamento.PENDENTE);

        AtualizaStatusDTO atualizaStatusDTO = new AtualizaStatusDTO();
        atualizaStatusDTO.setStatus("EFETIVADO");

        Mockito.when(service.obterPorId(1L)).thenReturn(Optional.of(lancamento));

        // Ação
        ResponseEntity response = lancamentoController.atualizarStatus(1L, atualizaStatusDTO);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("O lançamento não pode ser efetivado com data futura.");
    }

    @Test
    public void deveEfetivarLancamentoComDataValida() {
        // Cenário
        Lancamento lancamento = new Lancamento();
        lancamento.setId(1L);
        lancamento.setMes(LocalDate.now().getMonthValue()); // Mês atual
        lancamento.setAno(LocalDate.now().getYear());
        lancamento.setStatus(StatusLancamento.PENDENTE);

        AtualizaStatusDTO atualizaStatusDTO = new AtualizaStatusDTO();
        atualizaStatusDTO.setStatus("EFETIVADO");

        Mockito.when(service.obterPorId(1L)).thenReturn(Optional.of(lancamento));

        // Ação
        ResponseEntity response = lancamentoController.atualizarStatus(1L, atualizaStatusDTO);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isEqualTo(lancamento); // Verifica se o corpo da resposta é o lançamento atualizado

        // Verifica se o status do lançamento foi atualizado para EFETIVADO
        Mockito.verify(service, Mockito.times(1)).atualizar(lancamento);
        Assertions.assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.EFETIVADO);
    }

    @Test
    public void naoDeveBaixarLancamentosComMesInvalido() {
        // Ação
        ResponseEntity response = lancamentoController.downloadLancamentos("Teste", 13, 2024, null, "DESPESA", 1L);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Mês inválido.");
    }

    @Test
    public void naoDeveBaixarLancamentosComAnoInvalido() {
        // Ação
        ResponseEntity response = lancamentoController.downloadLancamentos("Teste", 12, 024, null, "DESPESA", 1L);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            Assertions.assertThat(response.getBody()).isEqualTo("Ano inválido.");
    }

    @Test
    public void deveBaixarLancamentosComSucesso() {
        // Cenário: Usuário válido e lançamentos encontrados
        Usuario usuario = new Usuario();
        Lancamento lancamento = new Lancamento();
        lancamento.setDescricao("Lançamento Teste");

        // Simula a busca do usuário por ID
        Mockito.when(usuarioServiceImpl.obterPorId(1L)).thenReturn(Optional.of(usuario));

        // Simula a busca de lançamentos que retornam um item
        Mockito.when(service.buscar(Mockito.any())).thenReturn(Collections.singletonList(lancamento));

        // Ação: Realiza o download dos lançamentos
        ResponseEntity<?> response = lancamentoController.downloadLancamentos("Lançamento Teste", null, null, null, "DESPESA", 1L);

        // Verificação: O status da resposta deve ser OK (200)
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verifica o cabeçalho de Content-Disposition para o download de arquivo
        Assertions.assertThat(response.getHeaders().get("Content-Disposition"))
                .contains("attachment;filename=lancamentos.json");

        // Verifica se o corpo da resposta é um byte array (JSON convertido)
        Assertions.assertThat(response.getBody()).isInstanceOf(byte[].class);
    }

    @Test
    public void naoDeveBaixarLancamentosSemLancamentos() {
        // Cenário: Usuário válido e sem lançamentos encontrados
        Mockito.when(usuarioServiceImpl.obterPorId(1L)).thenReturn(Optional.of(new Usuario()));
        Mockito.when(service.buscar(Mockito.any())).thenReturn(Collections.emptyList());

        // Ação: Tentar baixar lançamentos
        ResponseEntity<?> response = lancamentoController.downloadLancamentos("Teste", null, null, null, "DESPESA", 1L);

        // Verificação: Status deve ser NO_CONTENT
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void deveRetornarErroAoBuscarLancamentosComUsuarioNaoEncontrado() {
        // Cenário
        Mockito.when(usuarioServiceImpl.obterPorId(1L)).thenReturn(Optional.empty()); // Simula usuário inexistente

        // Ação
        ResponseEntity<?> response = lancamentoController.buscar("Lançamento Teste", 5, 2024, null, "RECEITA", 1L);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody())
                .isEqualTo("Não foi possível realizar a consulta. Usuário não encontrado para o Id informado.");
    }

    @Test
    public void deveRetornarLancamentosComSucesso() {
        // Cenário: Usuário válido e lançamentos encontrados
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Lancamento lancamento = new Lancamento();
        lancamento.setDescricao("Lançamento Teste");

        // Simula a busca do usuário por ID
        Mockito.when(usuarioServiceImpl.obterPorId(1L)).thenReturn(Optional.of(usuario));

        // Simula a busca de lançamentos que retorna um item
        Mockito.when(service.buscar(Mockito.any(Lancamento.class)))
                .thenReturn(Collections.singletonList(lancamento));

        // Ação: Realiza a busca dos lançamentos
        ResponseEntity<?> response = lancamentoController.buscar("Lançamento Teste", 5, 2024, null, "RECEITA", 1L);

        // Verificação: O status da resposta deve ser OK (200)
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verifica se a lista de lançamentos contém o lançamento esperado
        Assertions.assertThat(response.getBody()).isInstanceOf(List.class);
        List<Lancamento> lancamentos = (List<Lancamento>) response.getBody();
        Assertions.assertThat(lancamentos).contains(lancamento);
    }

    @Test
    public void deveRetornarErroQuandoCategoriaNaoExistir() {
        // Cenário: Categoria não encontrada
        LancamentoDTO dto = new LancamentoDTO();
        dto.setCategoriaId(2L); // Id de uma categoria inexistente

        Mockito.when(service.obterPorId(2L)).thenReturn(Optional.empty());

        // Execução
        ResponseEntity<?> response = lancamentoController.salvar(dto);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Categoria não encontrada. O lançamento será salvo sem categoria.");
    }

    @Test
    public void naoDeveAtualizarLancamentoNaoExistente() {
        // Cenário
        LancamentoDTO lancamentoDTO = new LancamentoDTO();

        // Mock para não encontrar o lançamento
        Mockito.when(service.obterPorId(1L)).thenReturn(Optional.empty());

        // Ação
        ResponseEntity<?> response = lancamentoController.atualizar(1L, lancamentoDTO);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Lançamento não encontrado na base de dados.");
    }

    @Test
    public void naoDeveAtualizarLancamentoCancelado() {
        // Cenário
        Lancamento lancamentoExistente = new Lancamento();
        lancamentoExistente.setId(1L);
        lancamentoExistente.setStatus(StatusLancamento.CANCELADO); // Status inicial

        LancamentoDTO lancamentoDTO = new LancamentoDTO();

        // Mock para retornar o lançamento existente
        Mockito.when(service.obterPorId(1L)).thenReturn(Optional.of(lancamentoExistente));

        // Ação
        ResponseEntity<?> response = lancamentoController.atualizar(1L, lancamentoDTO);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Não é possível atualizar um lançamento que já foi efetivado ou cancelado.");
    }

    @Test
    public void naoDeveAtualizarLancamentoEfetivado() {
        // Cenário
        Lancamento lancamentoExistente = new Lancamento();
        lancamentoExistente.setId(1L);
        lancamentoExistente.setStatus(StatusLancamento.EFETIVADO); // Status inicial

        LancamentoDTO lancamentoDTO = new LancamentoDTO();

        // Mock para retornar o lançamento existente
        Mockito.when(service.obterPorId(1L)).thenReturn(Optional.of(lancamentoExistente));

        // Ação
        ResponseEntity<?> response = lancamentoController.atualizar(1L, lancamentoDTO);

        // Verificação
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Não é possível atualizar um lançamento que já foi efetivado ou cancelado.");
    }
}

