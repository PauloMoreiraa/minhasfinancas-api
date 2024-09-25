package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.api.dto.UsuarioDTO;
import com.example.minhasfinancas.exception.ErroAutenticacao;
import com.example.minhasfinancas.exception.RegraNegocioException;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.service.JwtService;
import com.example.minhasfinancas.service.LancamentoService;
import com.example.minhasfinancas.service.UsuarioService;
import com.example.minhasfinancas.service.impl.SecurityUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles
@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc


public class UsuarioControllerTest {
    static final String API = "/api/usuarios";
    static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @MockBean
    UsuarioService service;

    @MockBean
    LancamentoService lancamentoService;

    @MockBean
    JwtService jwtService;

    @MockBean
    SecurityUserDetailsService securityUserDetailsService;

    @Test
    public void deveAutenticarUmUsuario() throws Exception {
        //cenario
        String email = "usuario@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();

        Usuario usuario = Usuario.builder().id(1l).email(email).senha(senha).build();

        Mockito.when(service.autenticar(email, senha)).thenReturn(usuario);

        String json = new ObjectMapper().writeValueAsString(dto);

        //execucao e verificacao
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(API.concat("/autenticar"))
                .accept(JSON)
                .contentType(JSON)
                .content(json);

        mvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void deveRetornarBadRequestAoObterErroDeAutenticaco() throws Exception {
        //cenario
        String email = "usuario@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Mockito.when(service.autenticar(email, senha)).thenThrow(ErroAutenticacao.class);

        String json = new ObjectMapper().writeValueAsString(dto);

        //execucao e verificacao
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(API.concat("/autenticar"))
                .accept(JSON)
                .contentType(JSON)
                .content(json);

        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deveCriarUmNovoUsuario() throws Exception {
        //cenario
        String email = "usuario@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email("usuario@email.com").senha("123").build();
        Usuario usuario = Usuario.builder().id(1l).email(email).senha(senha).build();

        Mockito.when(service.salvarUsuario(Mockito.any(Usuario.class))).thenReturn(usuario);
        String json = new ObjectMapper().writeValueAsString(dto);

        //execucao e verificacao
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(API)
                .accept(JSON)
                .contentType(JSON)
                .content(json);

        mvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(usuario.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("nome").value(usuario.getNome()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(usuario.getEmail()));
    }

    @Test
    public void deveRetornarBadRequestAoTentarCriarUmUsuarioInvalido() throws Exception {
        //cenario
        String email = "usuario@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email("usuario@email.com").senha("123").build();


        Mockito.when(service.salvarUsuario(Mockito.any(Usuario.class))).thenThrow(RegraNegocioException.class);
        String json = new ObjectMapper().writeValueAsString(dto);

        //execucao e verificacao
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(API)
                .accept(JSON)
                .contentType(JSON)
                .content(json);

        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

}
