package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.api.dto.CategoriaDTO;
import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    //Endpoint para converter categoria
    private Categoria converter(CategoriaDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setId(dto.getId());
        categoria.setDescricao(dto.getDescricao());

        return categoria;
    }

}