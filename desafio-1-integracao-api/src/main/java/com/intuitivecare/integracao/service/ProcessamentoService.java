package com.intuitivecare.integracao.service;

import com.intuitivecare.integracao.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável por filtrar e identificar arquivos de despesas
 * 
 * Os arquivos de Despesas com Eventos/Sinistros são identificados por
 * padrões no nome do arquivo que indicam esse tipo de dado
 */
public class ProcessamentoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessamentoService.class);
    
    // Padrões que indicam arquivos de despesas
    private static final String[] PADROES_DESPESAS = {
        "despesa",
        "sinistro",
        "evento",
        "4T",  // Padrão comum em arquivos trimestrais
        "DIOPS",
        "assistencia"
    };
    
    // Extensões suportadas
    private static final String[] EXTENSOES_SUPORTADAS = {
        ".csv",
        ".txt",
        ".xlsx",
        ".xls"
    };

    private final ApplicationConfig config;

    public ProcessamentoService(ApplicationConfig config) {
        this.config = config;
    } // ProcessamentoService

    /**
     * Filtra arquivos que contêm dados de Despesas com Eventos/Sinistros
     * 
     * @param arquivos Lista de todos os arquivos extraídos
     * @return Lista de arquivos que contêm dados de despesas
     */
    public List<Path> filtrarArquivosDespesas(List<Path> arquivos) {
        logger.info("Filtrando {} arquivos para identificar despesas...", arquivos.size());
        
        List<Path> arquivosDespesas = arquivos.stream()
            .filter(this::isArquivoSuportado)
            .filter(this::contemDadosDespesas)
            .collect(Collectors.toList());
        
        logger.info("Identificados {} arquivos de despesas", arquivosDespesas.size());
        arquivosDespesas.forEach(a -> logger.debug("  - {}", a.getFileName()));
        
        return arquivosDespesas;
    } // filtrarArquivosDespesas

    /**
     * Verifica se o arquivo tem extensão suportada
     */
    private boolean isArquivoSuportado(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        for (String ext : EXTENSOES_SUPORTADAS) {
            if (nome.endsWith(ext)) {
                return true;
            } // if
        } // for
        return false;
    } // isArquivoSuportado

    /**
     * Verifica se o arquivo contém dados de despesas
     * Usa uma combinação de análise do nome e conteúdo
     */
    private boolean contemDadosDespesas(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        
        // Primeiro verifica pelo nome do arquivo
        for (String padrao : PADROES_DESPESAS) {
            if (nome.contains(padrao.toLowerCase())) {
                logger.debug("Arquivo {} identificado pelo padrão '{}'", nome, padrao);
                return true;
            } // if
        } // for
        
        // Se não encontrou pelo nome, tenta analisar o cabeçalho do arquivo
        return analisarCabecalhoArquivo(arquivo);
    } // contemDadosDespesas

    /**
     * Analisa o cabeçalho do arquivo para identificar colunas de despesas
     */
    private boolean analisarCabecalhoArquivo(Path arquivo) {
        try {
            String primeiraLinha = Files.lines(arquivo)
                .findFirst()
                .orElse("")
                .toLowerCase();
            
            // Padrões no cabeçalho que indicam dados de despesas
            return primeiraLinha.contains("despesa") ||
                   primeiraLinha.contains("sinistro") ||
                   primeiraLinha.contains("evento") ||
                   primeiraLinha.contains("vl_saldo_final") ||
                   primeiraLinha.contains("valor");
                   
        } catch (IOException e) {
            logger.debug("Erro ao ler cabeçalho de {}: {}", arquivo, e.getMessage());
            return false;
        } // try-catch
    } // analisarCabecalhoArquivo

    /**
     * Identifica o formato do arquivo (CSV, TXT, XLSX)
     */
    public FormatoArquivo identificarFormato(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        
        if (nome.endsWith(".csv")) {
            return FormatoArquivo.CSV;
        } else if (nome.endsWith(".txt")) {
            return FormatoArquivo.TXT;
        } else if (nome.endsWith(".xlsx")) {
            return FormatoArquivo.XLSX;
        } else if (nome.endsWith(".xls")) {
            return FormatoArquivo.XLS;
        } // if-else
        
        return FormatoArquivo.DESCONHECIDO;
    } // identificarFormato

    /**
     * Detecta o delimitador usado em arquivos CSV/TXT
     */
    public char detectarDelimitador(Path arquivo) throws IOException {
        String primeiraLinha = Files.lines(arquivo)
            .findFirst()
            .orElse("");
        
        // Contar ocorrências de cada possível delimitador
        int pontoVirgula = contarOcorrencias(primeiraLinha, ';');
        int virgula = contarOcorrencias(primeiraLinha, ',');
        int tab = contarOcorrencias(primeiraLinha, '\t');
        int pipe = contarOcorrencias(primeiraLinha, '|');
        
        // Retornar o delimitador mais frequente
        if (pontoVirgula >= virgula && pontoVirgula >= tab && pontoVirgula >= pipe) {
            return ';';
        } else if (virgula >= tab && virgula >= pipe) {
            return ',';
        } else if (tab >= pipe) {
            return '\t';
        } else {
            return '|';
        } // if-else
    } // detectarDelimitador

    private int contarOcorrencias(String texto, char caractere) {
        int count = 0;
        for (char c : texto.toCharArray()) {
            if (c == caractere) count++;
        } // for
        return count;
    } // contarOcorrencias

    /**
     * Enum para formatos de arquivo suportados
     */
    public enum FormatoArquivo {
        CSV,
        TXT,
        XLSX,
        XLS,
        DESCONHECIDO
    } // FormatoArquivo
} // ProcessamentoService
