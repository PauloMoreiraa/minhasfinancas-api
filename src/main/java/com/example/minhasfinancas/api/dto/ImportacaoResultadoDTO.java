package com.example.minhasfinancas.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportacaoResultadoDTO {
    private int lancamentosImportados;
    private int erros;
    private List<String> mensagensErros;

    public ImportacaoResultadoDTO(int lancamentosImportados, int erros, List<String> mensagensErros) {
        this.lancamentosImportados = lancamentosImportados;
        this.erros = erros;
        this.mensagensErros = mensagensErros;
    }
}
