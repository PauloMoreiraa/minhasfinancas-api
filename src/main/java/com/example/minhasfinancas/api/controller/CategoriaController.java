package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.api.dto.CategoriaDTO;
import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    //Endpoint de salvar categoria
    @PostMapping
    public ResponseEntity salvar(@RequestBody final CategoriaDTO dto) {
        try {
            Categoria entidade = converter(dto);
            entidade = service.salvar(entidade);
            return new ResponseEntity(entidade, HttpStatus.CREATED);

        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listar() {
        List<CategoriaDTO> categorias = service.listar().stream()
                .map(this::converter)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categorias);
    }

    // Método para converter Categoria em CategoriaDTO
    private CategoriaDTO converter(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setDescricao(categoria.getDescricao());
        return dto;
    }

    //Endpoint para converter categoria
    private Categoria converter(CategoriaDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setId(dto.getId());
        categoria.setDescricao(dto.getDescricao());

        return categoria;
    }

}
