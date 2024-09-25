package com.example.minhasfinancas.service;

import com.example.minhasfinancas.MinhasfinancasApplication;
import com.example.minhasfinancas.model.entity.Categoria;
import com.example.minhasfinancas.model.repository.CategoriaRepository;
import com.example.minhasfinancas.service.impl.CategoriaServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = MinhasfinancasApplication.class)
@AutoConfigureTestEntityManager
@Transactional
public class CategoriaServiceTest {

    @SpyBean
    CategoriaServiceImpl service;

    @MockBean
    CategoriaRepository repository;

    @Test
    public void deveSalvarUmaCategoria() {
        // Cenario
        Categoria categoriaASalvar = new Categoria();
        categoriaASalvar.setDescricao("Alimentação");

        Mockito.when(repository.save(categoriaASalvar)).thenReturn(categoriaASalvar);

        // Execução
        Categoria categoria = service.salvar(categoriaASalvar);

        // Verificação
        Assertions.assertThat(categoria.getDescricao()).isEqualTo("Alimentação");
    }

    @Test
    public void naoDeveSalvarUmaCategoriaQuandoDescricaoEstiverVazia() {
        // Cenario
        Categoria categoriaASalvar = new Categoria();
        categoriaASalvar.setDescricao("");

        // Execução e Verificação
        Assertions.catchThrowableOfType(() -> service.salvar(categoriaASalvar), IllegalArgumentException.class);
        Mockito.verify(repository, Mockito.never()).save(categoriaASalvar);
    }

    @Test
    public void naoDeveSalvarUmaCategoriaQuandoDescricaoJaExistir() {
        // Cenario
        Categoria categoriaASalvar = new Categoria();
        categoriaASalvar.setDescricao("Alimentação");

        Mockito.when(repository.findByDescricao("Alimentação")).thenReturn(Optional.of(new Categoria()));

        // Execução e Verificação
        Assertions.catchThrowableOfType(() -> service.salvar(categoriaASalvar), IllegalArgumentException.class);
        Mockito.verify(repository, Mockito.never()).save(categoriaASalvar);
    }

    @Test
    public void deveAtualizarUmaCategoria() {
        // Cenario
        Categoria categoriaExistente = new Categoria();
        categoriaExistente.setId(1L);
        categoriaExistente.setDescricao("Alimentação");

        Mockito.when(repository.save(categoriaExistente)).thenReturn(categoriaExistente);

        // Execução
        service.salvar(categoriaExistente);

        // Verificação
        Mockito.verify(repository, Mockito.times(1)).save(categoriaExistente);
    }

    @Test
    public void deveLancarErroAoTentarAtualizarUmaCategoriaQueNaoFoiSalva() {
        // Cenario
        Categoria categoria = new Categoria();

        // Execução e Verificação
        Assertions.catchThrowableOfType(() -> service.salvar(categoria), IllegalArgumentException.class);
        Mockito.verify(repository, Mockito.never()).save(categoria);
    }
}
