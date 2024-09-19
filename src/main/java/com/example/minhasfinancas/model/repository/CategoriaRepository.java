package com.example.minhasfinancas.model.repository;

import com.example.minhasfinancas.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository <Categoria, Long> {
}
