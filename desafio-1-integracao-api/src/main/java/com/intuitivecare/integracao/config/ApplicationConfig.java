package com.intuitivecare.integracao.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configurações da aplicação centralizadas
 * Facilita manutenção e testes
 */
public class ApplicationConfig {

    // URLs da API ANS
    private static final String ANS_BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/";
    private static final String DEMONSTRACOES_CONTABEIS_PATH = "demonstracoes_contabeis/";
    
    // Diretórios de trabalho
    private final Path diretorioDownload;
    private final Path diretorioExtracao;
    private final Path diretorioOutput;
    
    // Configurações de processamento
    private final int quantidadeTrimestres;
    private final int timeoutConexaoMs;
    private final int timeoutLeituraMs;
    
    public ApplicationConfig() {
        // Diretórios padrão
        this.diretorioDownload = Paths.get("downloads");
        this.diretorioExtracao = Paths.get("extraidos");
        this.diretorioOutput = Paths.get("output");
        
        // Configurações padrão
        this.quantidadeTrimestres = 3;
        this.timeoutConexaoMs = 30000;
        this.timeoutLeituraMs = 60000;
    } // ApplicationConfig
    
    public String getAnsBaseUrl() {
        return ANS_BASE_URL;
    } // getAnsBaseUrl
    
    public String getDemonstracoesContabeisUrl() {
        return ANS_BASE_URL + DEMONSTRACOES_CONTABEIS_PATH;
    } // getDemonstracoesContabeisUrl
    
    public Path getDiretorioDownload() {
        return diretorioDownload;
    } // getDiretorioDownload
    
    public Path getDiretorioExtracao() {
        return diretorioExtracao;
    } // getDiretorioExtracao
    
    public Path getDiretorioOutput() {
        return diretorioOutput;
    } // getDiretorioOutput
    
    public int getQuantidadeTrimestres() {
        return quantidadeTrimestres;
    } // getQuantidadeTrimestres
    
    public int getTimeoutConexaoMs() {
        return timeoutConexaoMs;
    } // getTimeoutConexaoMs
    
    public int getTimeoutLeituraMs() {
        return timeoutLeituraMs;
    } // getTimeoutLeituraMs
} // ApplicationConfig
