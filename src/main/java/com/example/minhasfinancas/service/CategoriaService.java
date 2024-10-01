package com.example.minhasfinancas.service;

import com.example.minhasfinancas.model.entity.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaService {

    Categoria salvar(Categoria categoria);

    List<Categoria> listar();

    Optional<Categoria> obterPorId(Long id);

    Optional<Categoria> obterPorDescricao(String descricao);
}
