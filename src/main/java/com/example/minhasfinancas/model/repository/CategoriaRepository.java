package com.example.minhasfinancas.model.repository;

import com.example.minhasfinancas.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository <Categoria, Long> {
    Optional<Categoria> findByDescricao(String descricao);
}
