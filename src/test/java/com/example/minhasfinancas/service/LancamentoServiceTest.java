package com.example.minhasfinancas.service;

import com.example.minhasfinancas.MinhasfinancasApplication;
import com.example.minhasfinancas.api.dto.ImportacaoResultadoDTO;
import com.example.minhasfinancas.exception.RegraNegocioException;
import com.example.minhasfinancas.model.entity.Lancamento;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.model.enums.StatusLancamento;
import com.example.minhasfinancas.model.repository.LancamentoRepository;
import com.example.minhasfinancas.model.repository.LancamentoRepositoryTest;
import com.example.minhasfinancas.service.impl.CategoriaServiceImpl;
import com.example.minhasfinancas.service.impl.LancamentoServiceImpl;
import com.example.minhasfinancas.service.impl.UsuarioServiceImpl;
import com.opencsv.exceptions.CsvValidationException;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Example;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = MinhasfinancasApplication.class)
@AutoConfigureTestEntityManager
@Transactional
public class LancamentoServiceTest {

    @SpyBean
    LancamentoServiceImpl service;

    @MockBean
    LancamentoRepository repository;

    @Autowired
    private LancamentoServiceImpl lancamentoServiceImpl;

    @Autowired
    private UsuarioServiceImpl usuarioServiceImpl;

    @Autowired
    private CategoriaServiceImpl categoriaServiceImpl;
    @Autowired
    private LancamentoRepository lancamentoRepository;


    @Test
    public void deveSalvarUmLancamento() {
        //cenario
        Lancamento lancamentoASalvar = LancamentoRepositoryTest.criarLancamento();
        Mockito.doNothing().when(service).validar(lancamentoASalvar);

        Lancamento lancamentoSalvo = LancamentoRepositoryTest.criarLancamento();
        lancamentoSalvo.setId(1l);
        lancamentoSalvo.setStatus(StatusLancamento.PENDENTE);
        Mockito.when(repository.save(lancamentoASalvar)).thenReturn(lancamentoSalvo);

        //execucao
        Lancamento lancamento = service.salvar(lancamentoASalvar);

        //verificacao
        Assertions.assertThat(lancamento.getId()).isEqualTo(lancamentoSalvo.getId());
        Assertions.assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.PENDENTE);
    }

    @Test
    public void naoDeveSalvarUmLancamentoQuandoHouverErroDeValidacao() {
        //cenario
        Lancamento lancamentoASalvar = LancamentoRepositoryTest.criarLancamento();
        doThrow(RegraNegocioException.class).when(service).validar(lancamentoASalvar);

        //execucao e verificacao
        Assertions.catchThrowableOfType(()-> service.salvar(lancamentoASalvar), RegraNegocioException.class);

        Mockito.verify(repository, Mockito.never()).save(lancamentoASalvar);

    }

    @Test
    public void naoDeveAtualizarUmLancamentoSeUsuarioNaoForInformado() {
        // Cenario
        Lancamento lancamentoSemUsuario = LancamentoRepositoryTest.criarLancamento();
        lancamentoSemUsuario.setId(1L);
        lancamentoSemUsuario.setUsuario(null); // Define o usuário como nulo

        // Execucao e Verificacao
        try {
            service.atualizar(lancamentoSemUsuario);
            Assert.fail("Esperava uma exceção ao tentar atualizar um lançamento sem usuário.");
        } catch (RegraNegocioException e) {
            // Verifica se a exceção correta foi lançada
            assertEquals("Informe um Usuário.", e.getMessage());
        }

        // Verificacao
        Mockito.verify(repository, Mockito.never()).save(lancamentoSemUsuario);
    }



    @Test
    public void deveAtualizarUmLancamento() {
        //cenario
        Lancamento lancamentoSalvo = LancamentoRepositoryTest.criarLancamento();
        lancamentoSalvo.setId(1l);
        lancamentoSalvo.setStatus(StatusLancamento.PENDENTE);

        Mockito.doNothing().when(service).validar(lancamentoSalvo);

        Mockito.when(repository.save(lancamentoSalvo)).thenReturn(lancamentoSalvo);

        //execucao
        service.atualizar(lancamentoSalvo);

        //verificacao
        Mockito.verify(repository, Mockito.times(1)).save(lancamentoSalvo);
    }

    @Test(expected = RegraNegocioException.class)
    public void naoDeveAtualizarUmLancamentoSeStatusForCancelado() {
        // Cenário
        Lancamento lancamentoCancelado = LancamentoRepositoryTest.criarLancamento();
        lancamentoCancelado.setId(1L);
        lancamentoCancelado.setStatus(StatusLancamento.CANCELADO); // Define o status como CANCELADO

        // Execução
        service.atualizar(lancamentoCancelado);

        // Se a exceção for lançada, o teste passa.
        // Verificação
        Mockito.verify(repository, Mockito.never()).save(lancamentoCancelado);
    }

    @Test(expected = RegraNegocioException.class)
    public void naoDeveAtualizarUmLancamentoSeStatusForEfetivado() {
        // Cenário
        Lancamento lancamentoCancelado = LancamentoRepositoryTest.criarLancamento();
        lancamentoCancelado.setId(1L);
        lancamentoCancelado.setStatus(StatusLancamento.EFETIVADO); // Define o status como CANCELADO

        // Execução
        service.atualizar(lancamentoCancelado);

        // Se a exceção for lançada, o teste passa.
        // Verificação
        Mockito.verify(repository, Mockito.never()).save(lancamentoCancelado);
    }



    @Test
    public void deveLancarErroAoTentarAtualizarUmLancamentoQueAindaNaoFoiSalvo() {
        //cenario
        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();

        //execucao e verificacao
        Assertions.catchThrowableOfType(()-> service.atualizar(lancamento), NullPointerException.class);
        Mockito.verify(repository, Mockito.never()).save(lancamento);

    }

    @Test
    public void deveDeletarUmLancamento() {
        //cenario
        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();
        lancamento.setId(1l);

        //execucao
        service.deletar(lancamento);

        //verificacao
        Mockito.verify(repository).delete(lancamento);
    }

    @Test
    public void naoDeveDeletarUmLancamentoQueAindaNaoFoiSalvo() {
        //cenario
        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();

        //execucao
        Assertions.catchThrowableOfType(()-> service.deletar(lancamento), NullPointerException.class);

        //verificacao
        Mockito.verify(repository, Mockito.never()).delete(lancamento);
    }

    @Test
    public void deveFiltrarLancamentos() {
        //cenario
        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();
        lancamento.setId(1l);

        List<Lancamento> lista = Arrays.asList(lancamento);
        Mockito.when(repository.findAll(Mockito.any(Example.class))).thenReturn(lista);

        //execucao
        List<Lancamento> resultado = service.buscar(lancamento);

        //verificacoes
        Assertions.assertThat(resultado)
                .isNotEmpty()
                .hasSize(1)
                .contains(lancamento);
    }

    @Test
    public void deveAtualizarOStatusDeUmLancamento() {
        //cenario
        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();
        lancamento.setId(1l);
        lancamento.setStatus(StatusLancamento.PENDENTE);

        StatusLancamento novoStatus = StatusLancamento.EFETIVADO;
        Mockito.doReturn(lancamento).when(service).atualizar(lancamento);

        //execucao
        service.atualizarStatus(lancamento, novoStatus);

        //verificacoes
        Assertions.assertThat(lancamento.getStatus()).isEqualTo(novoStatus);
        Mockito.verify(service).atualizar(lancamento);
    }

    @Test
    public void deveObterUmLancamentoPorId(){
        //cenario
        Long id = 1l;

        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();
        lancamento.setId(1l);

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(lancamento));

        //execucao

        Optional<Lancamento> resultado = service.obterPorId(id);

        //verificacao
        Assertions.assertThat(resultado.isPresent()).isTrue();
    }

    @Test
    public void deveRetornarVazioQuandoOLancamentoNaoExiste(){
        //cenario
        Long id = 1l;
        Lancamento lancamento = LancamentoRepositoryTest.criarLancamento();
        lancamento.setId(1l);

        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());

        //execucao

        Optional<Lancamento> resultado = service.obterPorId(id);

        //verificacao
        Assertions.assertThat(resultado.isPresent()).isFalse();
    }

    @Test
    public void deveLancarErrosAoValidarUmLancamento(){
        Lancamento lancamento = new Lancamento();

        Throwable erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe uma Descrição válida.");

        lancamento.setDescricao("");

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe uma Descrição válida.");

        lancamento.setDescricao("Salario");

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Mês válido.");

        lancamento.setAno(0);

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Mês válido.");

        lancamento.setAno(13);

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Mês válido.");

        lancamento.setMes(1);

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Ano válido.");

        lancamento.setAno(202);

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Ano válido.");


        lancamento.setAno(2024);
        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Usuário.");

        lancamento.setUsuario(new Usuario());

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Usuário.");

        lancamento.getUsuario().setId(1l);

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Valor válido.");

        lancamento.setValor(BigDecimal.ZERO);

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um Valor válido.");

        lancamento.setValor(BigDecimal.valueOf(1));

        erro = Assertions.catchThrowable(()-> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um tipo de Lançamento.");
    }

    //testes CSV
    @Test(expected = IllegalArgumentException.class)
    public void deveLancarErroQuandoArquivoCSVEstiverVazio() throws CsvValidationException, IOException {
        // Cenario
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.isEmpty()).thenReturn(true);

        Long usuarioId = 1L;

        // Execução
        service.importarLancamentosCSV(file, usuarioId);
    }

    @Test
    public void deveLancarExcecaoQuandoArquivoCSVPossuiMenosDeSeisColunas() {
        // Cenario
        MultipartFile arquivoCSV = new MockMultipartFile("arquivo.csv", "linha1\nlinha2\n".getBytes());

        // Execucao e Verificacao
        Assertions.catchThrowableOfType(() -> service.importarLancamentosCSV(arquivoCSV, 1L), IllegalArgumentException.class);
    }


    @Test
    public void deveLancarErroAoImportarLancamentosComNumeroIncorretoDeColunas() throws IOException, CsvValidationException {
        // Cenário
        String conteudoCSV = "descricao,mês,ano,valor,tipo\n" + // Cabeçalho
                "Salario,5,2024,3000,RECEITA\n" + // Linha 1 (válida)
                "Despesa,6,2024,-1000,DESPESA\n"; // Linha 2 (inválida - 5 colunas)

        MultipartFile arquivoCSV = new MockMultipartFile("lancamentos.csv",
                "lancamentos.csv", "text/csv",
                conteudoCSV.getBytes(StandardCharsets.UTF_8) // Converte para bytes corretamente
        );

        // Execução
        ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(arquivoCSV, 1L);

        // Verificação
        Assertions.assertThat(resultado.getErros()).isGreaterThan(0);
        Assertions.assertThat(resultado.getMensagensErros()).contains("Erro na linha 2: número incorreto de colunas (exigido: 6, encontrado: 5).");
    }

    @Test
    public void deveLancarErroAoImportarLancamentosComDescricaoInvalida() throws IOException, CsvValidationException {
        // Cenário
        String conteudoCSV = "descricao,mês,ano,valor,tipo,categoria\n" + // Cabeçalho
                ",5,2024,3000,RECEITA,Alimentação\n"; // Linha 1 (inválida - descrição vazia)

        MultipartFile arquivoCSV = new MockMultipartFile("lancamentos.csv",
                "lancamentos.csv", "text/csv",
                conteudoCSV.getBytes(StandardCharsets.UTF_8) // Converte para bytes corretamente
        );

        // Execução
        ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(arquivoCSV, 1L);

        // Verificação
        Assertions.assertThat(resultado.getErros()).isGreaterThan(0);
        Assertions.assertThat(resultado.getMensagensErros())
                .contains("Erro na linha 1, coluna 0 (Descrição): Descrição inválida (vazia ou com mais de 100 caracteres).");
    }

    @Test
    public void deveLancarErroAoImportarLancamentosComMesInvalido() throws IOException, CsvValidationException {
        // Cenário
        String conteudoCSV = "descricao,mês,ano,valor,tipo,categoria\n" + // Cabeçalho
                "Salario,13,2024,3000,RECEITA,Alimentação\n"; // Linha 1 (inválida - mês fora do intervalo)

        MultipartFile arquivoCSV = new MockMultipartFile("lancamentos.csv",
                "lancamentos.csv", "text/csv",
                conteudoCSV.getBytes(StandardCharsets.UTF_8) // Converte para bytes corretamente
        );

        // Execução
        ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(arquivoCSV, 1L);

        // Verificação
        Assertions.assertThat(resultado.getErros()).isGreaterThan(0);
        Assertions.assertThat(resultado.getMensagensErros())
                .contains("Erro na linha 1, coluna 1 (Mês): Mês inválido (valor: 13).");
    }

    @Test
    public void deveLancarErroAoImportarLancamentosComValorNegativo() throws IOException, CsvValidationException {
        // Cenário
        String conteudoCSV = "descricao,mês,ano,valor,tipo,categoria\n" + // Cabeçalho
                "Salario,5,2024,-3000,RECEITA,Alimentação\n"; // Linha 1 (inválida - valor negativo)

        MultipartFile arquivoCSV = new MockMultipartFile("lancamentos.csv",
                "lancamentos.csv", "text/csv",
                conteudoCSV.getBytes(StandardCharsets.UTF_8) // Converte para bytes corretamente
        );

        // Execução
        ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(arquivoCSV, 1L);

        // Verificação
        Assertions.assertThat(resultado.getErros()).isGreaterThan(0);
        Assertions.assertThat(resultado.getMensagensErros())
                .contains("Erro na linha 1, coluna 3 (Valor): Valor não pode ser negativo (valor: -3000).");
    }

    @Test
    public void deveLancarErroAoImportarLancamentosComAnoInvalido() throws IOException, CsvValidationException {
        // Cenário
        String conteudoCSV = "descricao,mês,ano,valor,tipo,categoria\n" + // Cabeçalho
                "Salario,5,100,3000,RECEITA,\n"; // Linha 1 (inválida - ano fora do intervalo)

        MultipartFile arquivoCSV = new MockMultipartFile("lancamentos.csv",
                "lancamentos.csv", "text/csv",
                conteudoCSV.getBytes(StandardCharsets.UTF_8) // Converte para bytes corretamente
        );

        // Execução
        ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(arquivoCSV, 1L);

        // Verificação
        Assertions.assertThat(resultado.getErros()).isGreaterThan(0);
        Assertions.assertThat(resultado.getMensagensErros())
                .contains("Erro na linha 1, coluna 2 (Ano): Ano inválido (deve ter 4 dígitos, valor: 100).");
    }

    @Test
    public void deveLancarErroAoImportarLancamentosComTipoInvalido() throws IOException, CsvValidationException {
        // Cenário
        String conteudoCSV = "descricao,mês,ano,valor,tipo,categoria\n" +
                "Salario,5,2024,3000,RTA,\n"; // Linha 1 (inválida - ano fora do intervalo)

        MultipartFile arquivoCSV = new MockMultipartFile("lancamentos.csv",
                "lancamentos.csv", "text/csv",
                conteudoCSV.getBytes(StandardCharsets.UTF_8) // Converte para bytes corretamente
        );

        // Execução
        ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(arquivoCSV, 1L);

        // Verificação
        Assertions.assertThat(resultado.getErros()).isGreaterThan(0);
        Assertions.assertThat(resultado.getMensagensErros())
                .contains("Erro na linha 1, coluna 4 (Tipo): Tipo de lançamento inválido (deve ser 'RECEITA' ou 'DESPESA', valor: RTA).");
    }
}
