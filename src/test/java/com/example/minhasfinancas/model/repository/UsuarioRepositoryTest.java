package com.example.minhasfinancas.model.repository;

import com.example.minhasfinancas.MinhasfinancasApplication;
import com.example.minhasfinancas.model.entity.Usuario;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
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
public class UsuarioRepositoryTest {

    @Autowired
    UsuarioRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    public void deveVerificarAExistenciaDeUmEmail(){
        //cenario
        Usuario usuario = criarUsuario();
        entityManager.persist(usuario);

        //acao
        boolean result = repository.existsByEmail("usuario@email.com");

        //verificacao
        Assertions.assertThat(result).isTrue();
    }
    @Test
    public void deveRetornarFalsoQuandoNaoHouverUsuarioCadastrdoComOEmail(){
        //acao
        boolean result = repository.existsByEmail("usuario@email.com");

        //verificacao
        Assertions.assertThat(result).isFalse();

    }

    @Test
    public void devePersistirUmUsuarioNaBaseDeDados(){
        Usuario usuario = criarUsuario();
        //AÇÃO
        Usuario usuarioSalvo = repository.save(usuario);

        //Verificação
        Assertions.assertThat(usuarioSalvo.getId()).isNotNull();
    }

    @Test
    public void deveBuscarUmUsuarioPorEmail(){
        //cenario
        Usuario usuario = criarUsuario();
        entityManager.persist(usuario);

        //verificacao
        Optional<Usuario> result = repository.findByEmail("usuario@email.com");

        Assertions.assertThat(result.isPresent()).isTrue();

    }

    @Test
    public void deveRetornarVazioUsuarioPorEmailQuandoNaoExisteNaBase(){

        //verificacao
        Optional<Usuario> result = repository.findByEmail("usuario@email.com");

        Assertions.assertThat(result.isPresent()).isFalse();

    }

    public static Usuario criarUsuario(){
        return Usuario.builder().nome("usuario").email("usuario@email.com").build();
    }

}
