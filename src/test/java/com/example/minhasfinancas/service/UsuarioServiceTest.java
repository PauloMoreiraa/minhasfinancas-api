package com.example.minhasfinancas.service;

import com.example.minhasfinancas.MinhasfinancasApplication;
import com.example.minhasfinancas.exception.ErroAutenticacao;
import com.example.minhasfinancas.exception.RegraNegocioException;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.model.repository.UsuarioRepository;
import com.example.minhasfinancas.service.impl.UsuarioServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = MinhasfinancasApplication.class)
@AutoConfigureTestEntityManager
@Transactional
public class UsuarioServiceTest {

    @SpyBean
    UsuarioServiceImpl service;

    @MockBean
    UsuarioRepository repository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    public void deveSalvarUmUsuario() {
        // cenário
        Mockito.doNothing().when(service).validarEmail(Mockito.anyString());
        String senha = "senha";
        String senhaCriptografada = "senhaCriptografada";
        Usuario usuario = Usuario.builder().id(1L).nome("nome").email("email@email.com").senha(senhaCriptografada).build();

        Mockito.when(repository.save(Mockito.any(Usuario.class))).thenReturn(usuario);
        Mockito.when(passwordEncoder.encode(senha)).thenReturn(senhaCriptografada);

        // ação
        Usuario usuarioSalvo = service.salvarUsuario(Usuario.builder().nome("nome").email("email@email.com").senha(senha).build());

        // verificação
        Assertions.assertThat(usuarioSalvo).isNotNull();
        Assertions.assertThat(usuarioSalvo.getId()).isEqualTo(1L);
        Assertions.assertThat(usuarioSalvo.getNome()).isEqualTo("nome");
        Assertions.assertThat(usuarioSalvo.getEmail()).isEqualTo("email@email.com");
        Assertions.assertThat(usuarioSalvo.getSenha()).isEqualTo(senhaCriptografada);
    }

    @Test(expected = RegraNegocioException.class)
    public void naoDeveSalvarUmUsuarioComEmailJaCadastrado() {
        // cenário
        String email = "email@email.com";
        Usuario usuario = Usuario.builder().email(email).build();
        Mockito.doThrow(RegraNegocioException.class).when(service).validarEmail(email);

        // ação
        service.salvarUsuario(usuario);

        // verificação
        Mockito.verify(repository, Mockito.never()).save(usuario);
    }

    @Test
    public void deveAutenticarUmUsuarioComSucesso() {
        // cenário
        String email = "email@email.com";
        String senha = "senha";
        String senhaCriptografada = "senhaCriptografada";

        Usuario usuario = Usuario.builder().email(email).senha(senhaCriptografada).id(1L).build();
        Mockito.when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));
        Mockito.when(passwordEncoder.matches(senha, senhaCriptografada)).thenReturn(true);

        // ação
        Usuario result = service.autenticar(email, senha);

        // verificação
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    public void deveLancarErroQuandoNaoEncontrarUsuarioCadastradoComOEmailInformado() {
        // cenário
        Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        // ação
        Throwable exception = Assertions.catchThrowable(() -> service.autenticar("email@email.com", "senha"));

        // verificação
        Assertions.assertThat(exception).isInstanceOf(ErroAutenticacao.class)
                .hasMessage("Usuário não encontrado para o email informado.");
    }

    @Test
    public void deveLancarErroQuandoSenhaNaoBater() {
        // cenário
        String senha = "senha";
        String senhaCriptografada = "senhaCriptografada";
        Usuario usuario = Usuario.builder().email("email@email.com").senha(senhaCriptografada).build();
        Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.of(usuario));
        Mockito.when(passwordEncoder.matches(Mockito.anyString(), eq(senhaCriptografada))).thenReturn(false);

        // ação
        Throwable exception = Assertions.catchThrowable(() -> service.autenticar("email@email.com", senha));

        // verificação
        Assertions.assertThat(exception).isInstanceOf(ErroAutenticacao.class)
                .hasMessage("Senha inválida.");
    }

    @Test
    public void deveValidarEmail() {
        // cenário
        Mockito.when(repository.existsByEmail(Mockito.anyString())).thenReturn(false);

        // ação
        service.validarEmail("email@email.com");
    }

    @Test(expected = RegraNegocioException.class)
    public void deveLancarErroQuandoExistirEmailCadastrado() {
        // cenário
        Mockito.when(repository.existsByEmail(Mockito.anyString())).thenReturn(true);

        // ação
        service.validarEmail("email@email.com");
    }
}

