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
import java.math.BigDecimal;
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

    @GetMapping
    public ResponseEntity buscar(
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "tipo", required = false) String tipo,
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

    @PostMapping
    public ResponseEntity salvar(@RequestBody LancamentoDTO dto) {
        try {
            if (dto.getCategoriaId() != null && !categoriaService.obterPorId(dto.getCategoriaId()).isPresent()) {
                return ResponseEntity.badRequest().body("Categoria não encontrada. O lançamento será salvo sem categoria.");
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
        return service.obterPorId(id).map(entity -> {
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
    public ResponseEntity atualizarStatus(@PathVariable("id") Long id, @RequestBody AtualizaStatusDTO dto) {
        return service.obterPorId(id).map(entity -> {
            if (dto.getStatus() == null || dto.getStatus().isEmpty()) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            StatusLancamento statusSelecionado;

            try {
                statusSelecionado = StatusLancamento.valueOf(dto.getStatus().toUpperCase()); // Conversão segura
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            if (statusSelecionado != StatusLancamento.EFETIVADO && statusSelecionado != StatusLancamento.CANCELADO) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            if (statusSelecionado == StatusLancamento.EFETIVADO && isDataFutura(entity)) {
                return ResponseEntity.badRequest().body("O lançamento não pode ser efetivado com data futura.");
            }

            try {
                entity.setStatus(statusSelecionado);
                service.atualizar(entity);
                return ResponseEntity.ok(entity);
            } catch (RegraNegocioException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet(() ->
                new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST));
    }

    private boolean isDataFutura(Lancamento lancamento) {
        LocalDate hoje = LocalDate.now();
        int anoAtual = hoje.getYear();
        int mesAtual = hoje.getMonthValue();

        Integer anoLancamento = lancamento.getAno();
        Integer mesLancamento = lancamento.getMes();

        if (anoLancamento == null || mesLancamento == null) {
            return false;
        }

        return anoLancamento > anoAtual || (anoLancamento == anoAtual && mesLancamento > mesAtual);
    }

    @DeleteMapping("{id}")
    public ResponseEntity deletar(@PathVariable("id") Long id) {
        return service.obterPorId(id).map(entidade ->{
            service.deletar(entidade);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }).orElseGet(()->
                new ResponseEntity("Lançamento não encontrado na base de Dados.", HttpStatus.BAD_REQUEST));
    }

    private LancamentoDTO converter(Lancamento lancamento) {
        return LancamentoDTO.builder()
                .id(lancamento.getId())
                .descricao(lancamento.getDescricao())
                .valor(lancamento.getValor())
                .mes(lancamento.getMes())
                .ano(lancamento.getAno())
                .status(lancamento.getStatus().name())
                .tipo(lancamento.getTipo().name())
                .usuario(lancamento.getUsuario().getId())
                .latitude(lancamento.getLatitude())
                .longitude(lancamento.getLongitude())
                .categoriaId(lancamento.getCategoria() != null ? lancamento.getCategoria().getId() : null)
                .build();
    }

    private Lancamento converter(LancamentoDTO dto) {
        Lancamento lancamento = new Lancamento();
        lancamento.setId(dto.getId());
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setMes(dto.getMes());
        lancamento.setAno(dto.getAno());
        lancamento.setValor(dto.getValor());

        validarCoordenadas(dto.getLatitude(), dto.getLongitude());

        lancamento.setLatitude(dto.getLatitude());
        lancamento.setLongitude(dto.getLongitude());

        Usuario usuario = usuarioService
                .obterPorId(dto.getUsuario())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado para o Id informado."));
        lancamento.setUsuario(usuario);

        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaService.obterPorId(dto.getCategoriaId())
                    .orElseGet(() -> {
                        lancamento.setCategoria(null);
                        return null;
                    });

            if (categoria != null) {
                lancamento.setCategoria(categoria);
            }
        } else {
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

    private void validarCoordenadas(BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null) {
            if (latitude.scale() > 15 || latitude.precision() - latitude.scale() > 3) {
                throw new RegraNegocioException("Latitude fora do formato NUMERIC(9,6).");
            }
        }
        if (longitude != null) {
            if (longitude.scale() > 15 || longitude.precision() - longitude.scale() > 3) {
                throw new RegraNegocioException("Longitude fora do formato NUMERIC(9,6).");
            }
        }
    }


    @PostMapping("{id}/importar")
    public ResponseEntity<?> importarLancamentosCSV(@RequestParam("file") MultipartFile file, @PathVariable("id") Long usuario) {
        try {
            ImportacaoResultadoDTO resultado = service.importarLancamentosCSV(file, usuario);
            return ResponseEntity.ok(resultado);
        } catch (IOException | CsvValidationException e) {
            return ResponseEntity.badRequest().body("Erro ao importar lançamentos: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadLancamentos(
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "usuario", required = true) Long idUsuario
    ) {
        if (idUsuario == null || idUsuario <= 0) {
            return ResponseEntity.badRequest().body("ID de usuário é obrigatório e deve ser um valor positivo.");
        }

        if (mes != null) {
            if (mes < 1 || mes > 12) {
                return ResponseEntity.badRequest().body("Mês inválido.");
            }
        }

        if (ano != null) {
            if (ano < 1000 || ano > 3000) {
                return ResponseEntity.badRequest().body("Ano inválido.");
            }
        }

        Lancamento lancamentoFiltro = new Lancamento();
        lancamentoFiltro.setDescricao(descricao);
        lancamentoFiltro.setMes(mes);
        lancamentoFiltro.setAno(ano);

        Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
        if (!usuario.isPresent()) {
            return ResponseEntity.badRequest().body("Usuário não encontrado para o ID informado.");
        }
        lancamentoFiltro.setUsuario(usuario.get());

        if(categoriaId != null) {
            Categoria categoria = new Categoria();
            categoria.setId(categoriaId);
            lancamentoFiltro.setCategoria(categoria);
        }

        if(tipo != null){
            TipoLancamento tipoLancamento = TipoLancamento.valueOf(tipo);
            lancamentoFiltro.setTipo(tipoLancamento);
        }

        List<Lancamento> lancamentos;
        try {
            lancamentos = service.buscar(lancamentoFiltro);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao buscar lançamentos: " + e.getMessage());
        }

        if (lancamentos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

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