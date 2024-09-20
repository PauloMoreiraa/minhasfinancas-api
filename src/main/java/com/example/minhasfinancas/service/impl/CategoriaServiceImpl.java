package com.example.minhasfinancas.service.impl;

import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.model.repository.CategoriaRepository;
import com.example.minhasfinancas.model.repository.LancamentoRepository;
import com.example.minhasfinancas.service.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final LancamentoRepository lancamentoRepository;
    private CategoriaRepository repository;

    public CategoriaServiceImpl(CategoriaRepository repository, LancamentoRepository lancamentoRepository) {
        this.repository = repository;
        this.lancamentoRepository = lancamentoRepository;
    }

    @Override
    @Transactional
    public Categoria salvar(Categoria categoria) {
        // Valida a categoria antes de salvar
        if (categoria == null || categoria.getDescricao() == null || categoria.getDescricao().isEmpty()) {
            throw new IllegalArgumentException("A categoria não pode ser nula e deve ter uma descrição válida.");
        }

        // Verifica se a categoria com a mesma descrição já existe
        Optional<Categoria> categoriaExistente = repository.findByDescricao(categoria.getDescricao());
        if (categoriaExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe uma categoria com a descrição: " + categoria.getDescricao());
        }

        // Salva a categoria no banco de dados
        return repository.save(categoria);
    }

    public Optional<Categoria> obterPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Categoria> obterPorDescricao(String descricao) {
        return repository.findByDescricao(descricao);
    }
}
