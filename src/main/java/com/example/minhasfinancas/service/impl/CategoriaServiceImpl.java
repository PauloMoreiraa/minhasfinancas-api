package com.example.minhasfinancas.service.impl;

import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.model.entity.Lancamento;
import com.example.minhasfinancas.model.enums.StatusLancamento;
import com.example.minhasfinancas.model.repository.CategoriaRepository;
import com.example.minhasfinancas.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private CategoriaRepository repository;

    public CategoriaServiceImpl(CategoriaRepository repository) {
        this.repository = repository;
    }

//    @Override
//    @Transactional
//    public Lancamento salvar(Lancamento lancamento) {
//        validar(lancamento);
//        lancamento.setStatus(StatusLancamento.PENDENTE);
//        return repository.save(lancamento);
//    }
    @Override
    @Transactional
    public Categoria salvar(Categoria categoria) {
        // Valida a categoria antes de salvar

        if (categoria == null || categoria.getDescricao() == null || categoria.getDescricao().isEmpty()) {
            throw new IllegalArgumentException("A categoria não pode ser nula e deve ter uma descrição válida.");
        }

        // Salva a categoria no banco de dados
        return repository.save(categoria);
    }
}
