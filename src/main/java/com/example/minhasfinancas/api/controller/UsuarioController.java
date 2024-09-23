package com.example.minhasfinancas.api.controller;

import com.example.minhasfinancas.api.dto.TokenDTO;
import com.example.minhasfinancas.api.dto.UsuarioDTO;
import com.example.minhasfinancas.exception.ErroAutenticacao;
import com.example.minhasfinancas.exception.RegraNegocioException;
import com.example.minhasfinancas.model.entity.Usuario;
import com.example.minhasfinancas.service.JwtService;
import com.example.minhasfinancas.service.LancamentoService;
import com.example.minhasfinancas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;
    private final LancamentoService lancamentoService;
    private final JwtService jwtService;


    //Endpoint de autenticar/login do usuário
    @PostMapping("/autenticar")
    public ResponseEntity<?> autenticar(@RequestBody UsuarioDTO dto){
        try {
            //Recebe o email e senha
            Usuario usuarioAutenticado = service.autenticar(dto.getEmail(), dto.getSenha());

            //Gera o token para acessar a API
            String token = jwtService.gerarToken(usuarioAutenticado);
            TokenDTO tokenDTO = new TokenDTO(usuarioAutenticado.getNome(), token);

            //Retorna o token de acesso
            return ResponseEntity.ok(tokenDTO);
        } catch (ErroAutenticacao e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //Endpoint de salvar usuário
    @PostMapping
    public ResponseEntity salvar(@RequestBody UsuarioDTO dto) {

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(dto.getSenha()).build();

        //tenta salvar o usuário
        try{
            Usuario usuarioSalvo = service.salvarUsuario(usuario);
            return new ResponseEntity(usuarioSalvo, HttpStatus.CREATED);

        //retorna o erro caso haja algum erro para salvar o usuário
        }catch (RegraNegocioException e){
            return  ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //Endpoint para obter saldo
    @GetMapping("{id}/saldo")
    public ResponseEntity obterSaldo(@PathVariable("id") Long id){
        Optional<Usuario> usuario = service.obterPorId(id);

        //verifica se o usuário NÃO foi informado na url
        if(!usuario.isPresent()){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        //variavel saldo recebe o saldo pelo ID do usuário
        BigDecimal saldo = lancamentoService.obterSaldoPorUsuario(id);
        return ResponseEntity.ok(saldo);
    }

}
