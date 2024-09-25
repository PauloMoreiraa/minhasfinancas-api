package com.example.minhasfinancas.service.impl;

import com.example.minhasfinancas.api.dto.ImportacaoResultadoDTO;
import com.example.minhasfinancas.exception.RegraNegocioException;
import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.model.entity.Lancamento;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.model.enums.StatusLancamento;
import com.example.minhasfinancas.model.enums.TipoLancamento;
import com.example.minhasfinancas.model.repository.LancamentoRepository;
import com.example.minhasfinancas.service.LancamentoService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.Setter;
import net.bytebuddy.matcher.StringMatcher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;

@Service
public class LancamentoServiceImpl implements LancamentoService {

    private final UsuarioServiceImpl usuarioServiceImpl;
    private final CategoriaServiceImpl categoriaServiceImpl;
    private LancamentoRepository repository;

    public LancamentoServiceImpl(LancamentoRepository repository, UsuarioServiceImpl usuarioServiceImpl, CategoriaServiceImpl categoriaServiceImpl) {
        this.repository = repository;
        this.usuarioServiceImpl = usuarioServiceImpl;
        this.categoriaServiceImpl = categoriaServiceImpl;
    }

    @Override
    @Transactional
    public Lancamento salvar(Lancamento lancamento) {
        validar(lancamento);
        lancamento.setStatus(StatusLancamento.PENDENTE);
        return repository.save(lancamento);
    }

    @Override
    @Transactional
    public Lancamento atualizar(Lancamento lancamento) {
        Objects.requireNonNull(lancamento.getId());
        validar(lancamento);
        return repository.save(lancamento);
    }

    @Override
    @Transactional
    public void deletar(Lancamento lancamento) {
        Objects.requireNonNull(lancamento.getId());
        repository.delete(lancamento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lancamento> buscar(Lancamento lancamentoFiltro) {
        Example example = Example.of(lancamentoFiltro,
                ExampleMatcher.matching()
                        .withIgnoreCase()
                        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
        return repository.findAll(example);
    }

    @Override
    public void atualizarStatus(Lancamento lancamento, StatusLancamento status) {
        lancamento.setStatus(status);
        atualizar(lancamento);
    }

    @Override
    public void validar(Lancamento lancamento) {

        if(lancamento.getDescricao() == null || lancamento.getDescricao().trim().equals("")){
            throw new RegraNegocioException("Informe uma Descrição válida.");
        }
        if(lancamento.getMes() == null || lancamento.getMes() < 1 || lancamento.getMes() > 12){
            throw new RegraNegocioException("Informe um Mês válido.");
        }
        if(lancamento.getAno() == null || lancamento.getAno().toString().length() != 4){
            throw new RegraNegocioException("Informe um Ano válido.");
        }
        if(lancamento.getUsuario() == null || lancamento.getUsuario().getId() == null){
            throw new RegraNegocioException("Informe um Usuário.");
        }
        if (lancamento.getValor() == null || lancamento.getValor().compareTo(BigDecimal.ZERO) < 1){
            throw new RegraNegocioException("Informe um Valor válido.");
        }
        if (lancamento.getTipo() == null){
            throw new RegraNegocioException("Informe um tipo de Lançamento.");
        }
    }

    @Override
    public Optional<Lancamento> obterPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obterSaldoPorUsuario(Long id) {
        BigDecimal receitas = repository.obterSaldoPorTipoLancamentoEUsuarioEStatus(id, TipoLancamento.RECEITA, StatusLancamento.EFETIVADO);
        BigDecimal despesas = repository.obterSaldoPorTipoLancamentoEUsuarioEStatus(id, TipoLancamento.DESPESA, StatusLancamento.EFETIVADO);

        if(receitas == null){
            receitas = BigDecimal.ZERO;
        }
        if (despesas == null){
            despesas = BigDecimal.ZERO;
        }

        return receitas.subtract(despesas);
    }

    @Override
    @Transactional
    public ImportacaoResultadoDTO importarLancamentosCSV(MultipartFile file, Long usuarioId) throws IOException, CsvValidationException {
        List<Lancamento> lancamentos = new ArrayList<>();
        List<String> mensagensErros = new ArrayList<>();
        int lancamentosImportados = 0;
        int erros = 0;

        // Verifica se o arquivo está vazio
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV está vazio!");
        }

        // Verifica a extensão do arquivo
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".csv")) {
            throw new IllegalArgumentException("O arquivo deve ter a extensão .csv!");
        }

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] values;
            int linhaAtual = 0; // Contador para a linha atual no CSV (inicia após o cabeçalho)
            csvReader.readNext(); // Ignorar o cabeçalho

            while ((values = csvReader.readNext()) != null) {
                linhaAtual++; // Incrementa o número da linha a cada nova iteração

                try {
                    // Verifica se a linha tem exatamente 6 colunas
                    if (values.length != 6) {
                        mensagensErros.add("Erro na linha " + linhaAtual + ": número incorreto de colunas (exigido: 6, encontrado: " + values.length + ").");
                        erros++;
                        continue; // Pula para a próxima linha
                    }

                    // Validação da Descrição (Coluna 0)
                    String descricao = values[0];
                    if (descricao == null || descricao.isEmpty() || descricao.length() > 100) {
                        mensagensErros.add("Erro na linha " + linhaAtual + ", coluna 0 (Descrição): Descrição inválida (vazia ou com mais de 100 caracteres).");
                        erros++;
                        continue; // Pula para a próxima linha
                    }

                    // Validação do Mês (Coluna 1)
                    int mes = Integer.parseInt(values[1]);
                    if (mes < 1 || mes > 12) {
                        mensagensErros.add("Erro na linha " + linhaAtual + ", coluna 1 (Mês): Mês inválido (valor: " + mes + ").");
                        erros++;
                        continue; // Pula para a próxima linha
                    }

                    // Validação do Ano (Coluna 2)
                    int ano = Integer.parseInt(values[2]);
                    if (String.valueOf(ano).length() != 4) {
                        mensagensErros.add("Erro na linha " + linhaAtual + ", coluna 2 (Ano): Ano inválido (deve ter 4 dígitos, valor: " + ano + ").");
                        erros++;
                        continue; // Pula para a próxima linha
                    }

                    // Validação do Valor (Coluna 3)
                    BigDecimal valor = new BigDecimal(values[3]);
                    if (valor.compareTo(BigDecimal.ZERO) < 0) {
                        mensagensErros.add("Erro na linha " + linhaAtual + ", coluna 3 (Valor): Valor não pode ser negativo (valor: " + valor + ").");
                        erros++;
                        continue; // Pula para a próxima linha
                    }

                    // Validação do Tipo de Lançamento (Coluna 4)
                    String tipo = values[4].toUpperCase();
                    if (!tipo.equals("RECEITA") && !tipo.equals("DESPESA")) {
                        mensagensErros.add("Erro na linha " + linhaAtual + ", coluna 4 (Tipo): Tipo de lançamento inválido (deve ser 'RECEITA' ou 'DESPESA', valor: " + tipo + ").");
                        erros++;
                        continue; // Pula para a próxima linha
                    }

                    // Validação da Categoria (Coluna 5) - opcional
                    String categoriaStr = values[5];
                    Categoria categoria = null;

                    if (categoriaStr != null && !categoriaStr.trim().isEmpty()) {
                        Optional<Categoria> categoriaOptional = categoriaServiceImpl.obterPorDescricao(categoriaStr.trim());
                        if (categoriaOptional.isPresent()) {
                            categoria = categoriaOptional.get();
                        } else {
                            mensagensErros.add("Erro na linha " + linhaAtual + ", coluna 5 (Categoria): Categoria não encontrada (valor: " + categoriaStr + ").");
                            erros++;
                            continue; // Pula para a próxima linha
                        }
                    }

                    // Criação do lançamento
                    Lancamento lancamento = new Lancamento();
                    lancamento.setDescricao(descricao);
                    lancamento.setMes(mes);
                    lancamento.setAno(ano);
                    lancamento.setValor(valor);
                    lancamento.setTipo(TipoLancamento.valueOf(tipo));
                    lancamento.setCategoria(categoria); // Categoria pode ser null ou string vazia
                    lancamento.setStatus(StatusLancamento.PENDENTE);

                    Usuario usuario = usuarioServiceImpl.obterPorId(usuarioId)
                            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
                    lancamento.setUsuario(usuario);

                    lancamentos.add(lancamento);
                    lancamentosImportados++;
                } catch (Exception e) {
                    mensagensErros.add("Erro ao processar linha " + linhaAtual + ": " + Arrays.toString(values) + " - " + e.getMessage());
                    erros++;
                }
            }
        }

        // Salvando os lançamentos se houverem lançamentos válidos
        if (!lancamentos.isEmpty()) {
            repository.saveAll(lancamentos);
        }

        // Adiciona mensagem de sucesso caso não haja erros
        if (erros == 0) {
            mensagensErros.add("Importação realizada com sucesso! Todos os lançamentos foram importados sem erros.");
        }

        return new ImportacaoResultadoDTO(lancamentosImportados, erros, mensagensErros);
    }



}
