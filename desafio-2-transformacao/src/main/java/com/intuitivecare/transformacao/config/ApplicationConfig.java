package com.intuitivecare.transformacao.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configurações da aplicação de transformação
 */
public class ApplicationConfig {

    // URLs
    private static final String OPERADORAS_ATIVAS_URL = 
        "https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/";

    // Diretórios
    private final Path diretorioInput;
    private final Path diretorioOutput;
    private final Path diretorioTemp;

    // Arquivo CSV consolidado (do Desafio 1)
    private final Path csvConsolidado;

    public ApplicationConfig() {
        this.diretorioInput = Paths.get("input");
        this.diretorioOutput = Paths.get("output");
        this.diretorioTemp = Paths.get("temp");
        
        // Caminho padrão para o CSV do Desafio 1
        this.csvConsolidado = Paths.get("../desafio-1-integracao-api/output/consolidado_despesas.csv");
    } // ApplicationConfig

    public String getOperadorasAtivasUrl() {
        return OPERADORAS_ATIVAS_URL;
    } // getOperadorasAtivasUrl

    public Path getDiretorioInput() {
        return diretorioInput;
    } // getDiretorioInput

    public Path getDiretorioOutput() {
        return diretorioOutput;
    } // getDiretorioOutput

    public Path getDiretorioTemp() {
        return diretorioTemp;
    } // getDiretorioTemp

    public Path getCsvConsolidado() {
        return csvConsolidado;
    } // getCsvConsolidado
} // ApplicationConfig
