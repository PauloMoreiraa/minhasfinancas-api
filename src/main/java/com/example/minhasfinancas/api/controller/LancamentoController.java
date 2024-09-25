package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.api.dto.AtualizaStatusDTO;
import com.example.minhasfinancas.api.dto.ImportacaoResultadoDTO;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoController {

    private final LancamentoService service;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;


//    Endpoint para buscar lançamento

    @GetMapping
    public ResponseEntity buscar(
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "tipo", required = false) String tipo, // Pode ser "RECEITA" ou "DESPESA"
            @RequestParam("usuario") Long idUsuario
    ) {
        Lancamento lancamentoFiltro = new Lancamento();
        lancamentoFiltro.setDescricao(descricao);
        lancamentoFiltro.setMes(mes);
        lancamentoFiltro.setAno(ano);

        Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
        if (!usuario.isPresent()) {
            return ResponseEntity.badRequest().body("Não foi possível realizar a consulta. Usuário não encontrado para o Id informado.");
        } else {
            lancamentoFiltro.setUsuario(usuario.get());
        }

        // Adiciona a categoria e tipo de lançamento no filtro
        if (categoriaId != null) {
            Categoria categoria = new Categoria();
            categoria.setId(categoriaId);
            lancamentoFiltro.setCategoria(categoria);
        }

        if (tipo != null) {
            lancamentoFiltro.setTipo(TipoLancamento.valueOf(tipo));
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


    //Endpoint de salvar
    @PostMapping
    public ResponseEntity salvar(@RequestBody LancamentoDTO dto) {
        try {
            // Verifica se a categoria existe, se o Id não for nulo
            if (dto.getCategoriaId() != null && !categoriaService.obterPorId(dto.getCategoriaId()).isPresent()) {
                return ResponseEntity.badRequest().body("Categoria não encontrada, crie ou altere a categoria desejada.");
            }

            Lancamento entidade = converter(dto);
            entidade = service.salvar(entidade);
            return new ResponseEntity(entidade, HttpStatus.CREATED);
        } catch (RegraNegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    //Endpoint de atualizar/editar o lançamento
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


    //Endpoint para atualizar o status do lançamento
    @PutMapping("{id}/atualiza-status")
    public ResponseEntity atualizarStatus(@PathVariable("id") Long id, @RequestBody AtualizaStatusDTO dto) {
        // Obtém o lançamento pelo ID fornecido
        return service.obterPorId(id).map(entity -> {
            // Verifica se o status está nulo ou vazio
            if (dto.getStatus() == null || dto.getStatus().isEmpty()) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            // Tenta converter o status recebido no DTO para um enum StatusLancamento
            StatusLancamento statusSelecionado;

            try {
                statusSelecionado = StatusLancamento.valueOf(dto.getStatus().toUpperCase()); // Conversão segura
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            // Verifica se o status é válido
            if (statusSelecionado != StatusLancamento.EFETIVADO && statusSelecionado != StatusLancamento.CANCELADO) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            // Se o status é EFETIVADO, verifica se a data do lançamento é futura
            if (statusSelecionado == StatusLancamento.EFETIVADO && isDataFutura(entity)) {
                return ResponseEntity.badRequest().body("O lançamento não pode ser efetivado com data futura.");
            }

            try {
                // Atualiza o status do lançamento
                entity.setStatus(statusSelecionado);
                // Persiste a atualização no banco de dados
                service.atualizar(entity);
                // Retorna a entidade atualizada com um status de sucesso
                return ResponseEntity.ok(entity);
            } catch (RegraNegocioException e) {
                // Se ocorrer um erro de negócio, retorna um erro de solicitação ruim com a mensagem de erro
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet(() ->
                new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST));
    }

    // Verifica a data do lançamento
    private boolean isDataFutura(Lancamento lancamento) {
        LocalDate hoje = LocalDate.now();
        int anoAtual = hoje.getYear();
        int mesAtual = hoje.getMonthValue();

        Integer anoLancamento = lancamento.getAno();
        Integer mesLancamento = lancamento.getMes();

        // Se o ano ou o mês do lançamento não estiverem informados, assume que não é uma data futura
        if (anoLancamento == null || mesLancamento == null) {
            return false; // Considera que um lançamento sem ano e mês não é futuro
        }

        // Verifica se o ano do lançamento é maior que o ano atual ou, se for o mesmo ano, se o mês é maior que o mês atual
        return anoLancamento > anoAtual || (anoLancamento == anoAtual && mesLancamento > mesAtual);
    }


    //Endpoint de deletar lançamento
    @DeleteMapping("{id}")
    public ResponseEntity deletar(@PathVariable("id") Long id) {
        return service.obterPorId(id).map(entidade ->{
            service.deletar(entidade);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }).orElseGet(()->
                new ResponseEntity("Lançamento não encontrado na base de Dados.", HttpStatus.BAD_REQUEST));
    }

    private LancamentoDTO converter(Lancamento lancamento){
        //cria um objeto lançamentoDTO usando o padrão do projeto builder
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

        // Se o categoriaId for fornecido, verifica se a categoria existe
        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaService.obterPorId(dto.getCategoriaId())
                    .orElseThrow(() -> new RegraNegocioException("Categoria não encontrada para o Id informado."));
            lancamento.setCategoria(categoria);
        } else {
            // Se não houver categoriaId, define a categoria como null
            lancamento.setCategoria(null);
        }

        if (dto.getTipo() != null) {
            lancamento.setTipo(TipoLancamento.valueOf(dto.getTipo()));
        }
        if (dto.getStatus() != null) {
            lancamento.setStatus(StatusLancamento.valueOf(dto.getStatus()));
        }

        return lancamento;
    }



    //Endpoint de Upload de Arquivo CSV
    @PostMapping("{id}/importar")
    public ResponseEntity<?> importarLancamentosCSV(@RequestParam("file") MultipartFile file, @PathVariable("id") Long usuario) {
        try {
            ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(file, usuario);
            return ResponseEntity.ok(resultado);
        } catch (IOException | CsvValidationException e) {
            return ResponseEntity.badRequest().body("Erro ao importar lançamentos: " + e.getMessage());
        }
    }

    // Novo endpoint para download de lançamentos filtrados em JSON
    @GetMapping("/download")
    public ResponseEntity<?> downloadLancamentos(
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "usuario", required = true) Long idUsuario // Parâmetro 'usuario' agora é obrigatório
    ) {
        // Verificar se o ID do usuário foi passado e é válido
        if (idUsuario == null || idUsuario <= 0) {
            return ResponseEntity.badRequest().body("ID de usuário é obrigatório e deve ser um valor positivo.");
        }

        // Verificar se o mês é válido
        if (mes != null) {
            if (mes < 1 || mes > 12) {
                return ResponseEntity.badRequest().body("Mês inválido.");
            }
        }

        // Verificar se o ano é válido
        if (ano != null) {
            if (ano < 1000 || ano > 3000) {
                return ResponseEntity.badRequest().body("Ano inválido.");
            }
        }

        Lancamento lancamentoFiltro = new Lancamento();
        lancamentoFiltro.setDescricao(descricao);
        lancamentoFiltro.setMes(mes);
        lancamentoFiltro.setAno(ano);

        // Buscar usuário e verificar se existe
        Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
        if (!usuario.isPresent()) {
            return ResponseEntity.badRequest().body("Usuário não encontrado para o ID informado.");
        }
        lancamentoFiltro.setUsuario(usuario.get());

        // Buscar lançamentos com o filtro
        List<Lancamento> lancamentos;
        try {
            lancamentos = service.buscar(lancamentoFiltro);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao buscar lançamentos: " + e.getMessage());
        }

        // Verificar se não há lançamentos
        if (lancamentos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Converter os lançamentos para JSON e retornar
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            objectMapper.writeValue(outputStream, lancamentos);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=lancamentos.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(outputStream.toByteArray());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar o arquivo JSON: " + e.getMessage());
        }
    }


}