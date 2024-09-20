package com.example.minhasfinancas.api.resource;

import com.example.minhasfinancas.api.dto.AtualizaStatusDTO;
import com.example.minhasfinancas.api.dto.LancamentoDTO;
import com.example.minhasfinancas.exception.RegraNegocioException;
import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.model.entity.Lancamento;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.model.enums.StatusLancamento;
import com.example.minhasfinancas.model.enums.TipoLancamento;
import com.example.minhasfinancas.service.CategoriaService;
import com.example.minhasfinancas.service.LancamentoService;
import com.example.minhasfinancas.service.UsuarioService;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoResource {

    private final LancamentoService service;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;


    @GetMapping
    public ResponseEntity buscar(
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam("usuario") Long idUsuario
    ) {
        Lancamento lancamentoFiltro = new Lancamento();
        lancamentoFiltro.setDescricao(descricao);
        lancamentoFiltro.setMes(mes);
        lancamentoFiltro.setAno(ano);

        Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
        if(!usuario.isPresent()) {
            return ResponseEntity.badRequest().body("Não foi possível realizar a consulta. Usuário não encontrado para o Id informado.");
        }else{
            lancamentoFiltro.setUsuario(usuario.get());
        }
        List<Lancamento> lancamentos = service.buscar(lancamentoFiltro);
        return ResponseEntity.ok(lancamentos);
    }

    @GetMapping("{id}")
    public ResponseEntity obterLancamento (@PathVariable("id") Long id) {
        return service.obterPorId(id)
                .map(lancamento -> new ResponseEntity(converter(lancamento), HttpStatus.OK))
                .orElseGet(()-> new ResponseEntity(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity salvar(@RequestBody LancamentoDTO dto) {
        try {
            // Verifica se o categoriaId é nulo
            if (dto.getCategoriaId() == null) {
                return ResponseEntity.badRequest().body("A categoria não pode ser nula.");
            }

            // Verifica se a categoria existe
            if (!categoriaService.obterPorId(dto.getCategoriaId()).isPresent()) {
                return ResponseEntity.badRequest().body("Categoria não encontrada, crie ou altere a categoria desejada.");
            }

            Lancamento entidade = converter(dto);
            entidade = service.salvar(entidade);
            return new ResponseEntity(entidade, HttpStatus.CREATED);
        } catch (RegraNegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("{id}")
    public ResponseEntity atualizar(@PathVariable("id") Long id, @RequestBody LancamentoDTO dto) {
        // Tenta obter o lançamento pelo ID fornecido
        return service.obterPorId(id).map(entity -> {
            // Verifica se o lançamento está efetivado ou cancelado
            if (entity.getStatus() == StatusLancamento.EFETIVADO || entity.getStatus() == StatusLancamento.CANCELADO) {
                return ResponseEntity.badRequest().body("Não é possível atualizar um lançamento que já foi efetivado ou cancelado.");
            }

            try {
                Lancamento lancamento = converter(dto);
                lancamento.setId(entity.getId());
                service.atualizar(lancamento);
                return ResponseEntity.ok(lancamento);
            } catch (RegraNegocioException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet(() ->
                new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST));
    }


    @PutMapping("{id}/atualiza-status")
    public ResponseEntity atualizarStatus(@PathVariable("id") Long id, @RequestBody AtualizaStatusDTO dto){
        // Obtém o lançamento pelo ID fornecido
        return service.obterPorId(id).map(entity -> {
            // Tenta converter o status recebido no DTO para um enum StatusLancamento
            StatusLancamento statusSelecionado = StatusLancamento.valueOf(dto.getStatus());

            // Se o status fornecido não for válido, retorna um erro de solicitação
            if(statusSelecionado == null){
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            // Verifica se a data do lançamento é futura
            if (isDataFutura(entity)) {
                return ResponseEntity.badRequest().body("O lançamento não pode ser efetivado ou cancelado com data futura.");
            }
            try{
                // Atualiza o status do lançamento
                entity.setStatus(statusSelecionado);
                // Persiste a atualização no banco de dados
                service.atualizar(entity);
                // Retorna a entidade atualizada com um status de sucesso
                return ResponseEntity.ok(entity);
            }catch (RegraNegocioException e){
                // Se ocorrer um erro de negócio, retorna um erro de solicitação ruim com a mensagem de erro
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet(() ->
                new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST));
    }

    private boolean isDataFutura(Lancamento lancamento) {
        // Obtém a data atual
        LocalDate hoje = LocalDate.now();
        int anoAtual = hoje.getYear();
        int mesAtual = hoje.getMonthValue();

        // Obtém o ano e o mês do lançamento
        Integer anoLancamento = lancamento.getAno();
        Integer mesLancamento = lancamento.getMes();

        // Se o ano ou o mês do lançamento não estiverem informados, assume que não é uma data futura
        if (anoLancamento == null || mesLancamento == null) {
            return false; // Considera que um lançamento sem ano e mês não é futuro
        }

        // Verifica se o ano do lançamento é maior que o ano atual ou, se for o mesmo ano, se o mês é maior que o mês atual
        return anoLancamento > anoAtual || (anoLancamento == anoAtual && mesLancamento > mesAtual);
    }

    @DeleteMapping("{id}")
    public ResponseEntity deletar(@PathVariable("id") Long id) {
        return service.obterPorId(id).map(entidade ->{
            service.deletar(entidade);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }).orElseGet(()->
                new ResponseEntity("Lancamento não encontrado na base de Dados.", HttpStatus.BAD_REQUEST));
    }



    private LancamentoDTO converter(Lancamento lancamento){
        return LancamentoDTO.builder()
                .id(lancamento.getId())
                .descricao(lancamento.getDescricao())
                .valor(lancamento.getValor())
                .mes(lancamento.getMes())
                .ano(lancamento.getAno())
                .status(lancamento.getStatus().name())
                .tipo(lancamento.getTipo().name())
                .usuario(lancamento.getUsuario().getId())
                .categoriaId(lancamento.getCategoria().getId())
                .build();
    }

    private Lancamento converter(LancamentoDTO dto) {
        Lancamento lancamento = new Lancamento();
        lancamento.setId(dto.getId());
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setMes(dto.getMes());
        lancamento.setAno(dto.getAno());
        lancamento.setValor(dto.getValor());

        // Verifica se o usuário existe
        Usuario usuario = usuarioService
                .obterPorId(dto.getUsuario())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado para o Id informado."));
        lancamento.setUsuario(usuario);

        // Verifica se a categoria é válida
        if (dto.getCategoriaId() == null || !categoriaService.obterPorId(dto.getCategoriaId()).isPresent()) {
            throw new RegraNegocioException("A categoria não pode ser nula ou inválida.");
        }

        // Se a categoria é válida, busca e define no lançamento
        Categoria categoria = categoriaService.obterPorId(dto.getCategoriaId()).orElseThrow(
                () -> new RegraNegocioException("Categoria não encontrada para o Id informado.")
        );
        lancamento.setCategoria(categoria);

        if (dto.getTipo() != null) {
            lancamento.setTipo(TipoLancamento.valueOf(dto.getTipo()));
        }
        if (dto.getStatus() != null) {
            lancamento.setStatus(StatusLancamento.valueOf(dto.getStatus()));
        }

        return lancamento;
    }

    @PostMapping("{id}/importar")
    public ResponseEntity<?> importarLancamentosCSV(@RequestParam("file") MultipartFile file, @PathVariable("id") Long usuario) {
        try {
            service.importarLancamentosCSV(file, usuario);
            return ResponseEntity.ok("Lançamentos importados com sucesso!");
        } catch (IOException | CsvValidationException e) {
            return ResponseEntity.badRequest().body("Erro ao importar lançamentos: " + e.getMessage());
        }
    }


}