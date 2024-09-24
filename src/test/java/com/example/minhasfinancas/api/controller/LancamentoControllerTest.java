package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.MinhasfinancasApplication;
import com.example.minhasfinancas.api.dto.AtualizaStatusDTO;
import com.example.minhasfinancas.model.entity.Lancamento;
import com.example.minhasfinancas.model.enums.StatusLancamento;
import com.example.minhasfinancas.service.LancamentoService;
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

}

