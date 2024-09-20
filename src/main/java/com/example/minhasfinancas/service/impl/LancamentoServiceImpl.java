package com.example.minhasfinancas.service.impl;

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

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    // *** Função para importar lançamentos a partir de um CSV ***
    @Transactional
    public void importarLancamentosCSV(MultipartFile file, Long usuarioId) throws IOException, CsvValidationException {
        List<Lancamento> lancamentos = new ArrayList<>();

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV está vazio!");
        }

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] values;
            csvReader.readNext(); // Ignorar o cabeçalho

            while ((values = csvReader.readNext()) != null) {
                Lancamento lancamento = new Lancamento();
                lancamento.setDescricao(values[0]);
                lancamento.setMes(Integer.parseInt(values[1]));
                lancamento.setAno(Integer.parseInt(values[2]));
                lancamento.setValor(new BigDecimal(values[3]));
                lancamento.setTipo(TipoLancamento.valueOf(values[4].toUpperCase()));

                // Buscar a categoria correspondente
                Optional<Categoria> categoria = categoriaServiceImpl.obterPorDescricao(values[5]);

                if (categoria.isPresent()) {
                    lancamento.setCategoria(categoria.get());
                } else {
                    throw new IllegalArgumentException("Categoria não encontrada: " + values[5]);
                }

                lancamento.setStatus(StatusLancamento.PENDENTE);

                Usuario usuario = usuarioServiceImpl.obterPorId(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
                lancamento.setUsuario(usuario);

                lancamentos.add(lancamento);
            }
        }

        repository.saveAll(lancamentos);
    }
}
