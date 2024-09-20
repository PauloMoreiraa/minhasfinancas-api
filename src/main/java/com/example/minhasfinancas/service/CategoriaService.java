package com.example.minhasfinancas.service;

import com.example.minhasfinancas.model.entity.Categoria;

import java.util.Optional;

public interface CategoriaService {

    Categoria salvar(Categoria categoria);

    Optional<Categoria> obterPorId(Long id);

    Optional<Categoria> obterPorDescricao(String descricao);
}
