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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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

        if (lancamento.getDescricao() == null || lancamento.getDescricao().trim().equals("")) {
            throw new RegraNegocioException("Informe uma Descrição válida.");
        }
        if (lancamento.getMes() == null || lancamento.getMes() < 1 || lancamento.getMes() > 12) {
            throw new RegraNegocioException("Informe um Mês válido.");
        }
        if (lancamento.getAno() == null || lancamento.getAno().toString().length() != 4) {
            throw new RegraNegocioException("Informe um Ano válido.");
        }
        if (lancamento.getUsuario() == null || lancamento.getUsuario().getId() == null) {
            throw new RegraNegocioException("Informe um Usuário.");
        }
        if (lancamento.getValor() == null || lancamento.getValor().compareTo(BigDecimal.ZERO) < 1) {
            throw new RegraNegocioException("Informe um Valor válido.");
        }
        if (lancamento.getTipo() == null) {
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

        if (receitas == null) {
            receitas = BigDecimal.ZERO;
        }
        if (despesas == null) {
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

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV está vazio!");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".csv")) {
            throw new IllegalArgumentException("O arquivo deve ter a extensão .csv!");
        }

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] values;
            int linhaAtual = 0;
            csvReader.readNext();

            while ((values = csvReader.readNext()) != null) {
                linhaAtual++;
                List<String> errosLinha = new ArrayList<>();
                boolean erroLeve = false;

                if (values.length != 8) {
                    mensagensErros.add("- Erro na linha " + linhaAtual + ": número incorreto de colunas (exigido: 8, encontrado: " + values.length + ").");
                    erros++;
                    continue;
                }

                String descricao = values[0];
                if (descricao == null || descricao.isEmpty() || descricao.length() > 100) {
                    errosLinha.add("Coluna de descrição: Descrição inválida (vazia ou com mais de 100 caracteres).");
                }

                try {
                    int mes = Integer.parseInt(values[1]);
                    if (mes < 1 || mes > 12) {
                        errosLinha.add("Coluna de mês: Mês inválido (valor: " + mes + ").");
                    }
                } catch (NumberFormatException e) {
                    errosLinha.add("Coluna de mês: Formato inválido.");
                }

                try {
                    int ano = Integer.parseInt(values[2]);
                    if (String.valueOf(ano).length() != 4) {
                        errosLinha.add("Coluna de ano: Ano inválido (deve ter 4 dígitos, valor: " + ano + ").");
                    }
                } catch (NumberFormatException e) {
                    errosLinha.add("Coluna de ano: Formato inválido.");
                }

                try {
                    BigDecimal valor = new BigDecimal(values[3]);
                    if (valor.compareTo(BigDecimal.ZERO) < 0) {
                        errosLinha.add("Coluna de valor: Valor não pode ser negativo (valor: " + valor + ").");
                    }
                } catch (NumberFormatException e) {
                    errosLinha.add("Coluna de valor: Formato inválido.");
                }

                String tipo = values[4].toUpperCase();
                if (!tipo.equals("RECEITA") && !tipo.equals("DESPESA")) {
                    errosLinha.add("Coluna de tipo: Tipo de lançamento inválido (deve ser 'RECEITA' ou 'DESPESA', valor: " + tipo + ").");
                }

                BigDecimal latitude = null;
                try {
                    latitude = new BigDecimal(values[5]);
                    if (latitude.scale() > 15 || latitude.precision() - latitude.scale() > 3) {
                        latitude = null;
                    }
                } catch (NumberFormatException e) {
                    latitude = null;
                }

                BigDecimal longitude = null;
                try {
                    longitude = new BigDecimal(values[6]);
                    if (longitude.scale() > 15 || longitude.precision() - longitude.scale() > 3) {
                        longitude = null;
                    }
                } catch (NumberFormatException e) {
                    longitude = null;
                }

                String categoriaStr = values[7];
                Categoria categoria = null;
                if (categoriaStr != null && !categoriaStr.trim().isEmpty()) {
                    Optional<Categoria> categoriaOptional = categoriaServiceImpl.obterPorDescricao(categoriaStr.trim());
                    if (categoriaOptional.isPresent()) {
                        categoria = categoriaOptional.get();
                    }
                }

                if (!errosLinha.isEmpty()) {
                    mensagensErros.add("- Erro(s) na linha " + linhaAtual + ":\n " + String.join("\n ", errosLinha));
                    erros++;
                    continue;
                }

                try {
                    Lancamento lancamento = new Lancamento();
                    lancamento.setDescricao(descricao);
                    lancamento.setMes(Integer.parseInt(values[1]));
                    lancamento.setAno(Integer.parseInt(values[2]));
                    lancamento.setValor(new BigDecimal(values[3]));
                    lancamento.setLatitude(latitude);
                    lancamento.setLongitude(longitude);
                    lancamento.setTipo(TipoLancamento.valueOf(tipo));
                    lancamento.setCategoria(categoria);
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

        if (!lancamentos.isEmpty()) {
            repository.saveAll(lancamentos);
        }

        List<String> lancamentosJson = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Lancamento lancamento : lancamentos) {
            try {
                lancamentosJson.add(objectMapper.writeValueAsString(lancamento));
            } catch (JsonProcessingException e) {
                mensagensErros.add("Erro ao converter lançamento para JSON: " + e.getMessage());
            }
        }

        return new ImportacaoResultadoDTO(lancamentosImportados, erros, mensagensErros, lancamentosJson);
    }


}
