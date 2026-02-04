package com.intuitivecare.integracao.service;

import com.intuitivecare.integracao.config.ApplicationConfig;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pelo download e extração de arquivos
 * 
 * Trade-off técnico implementado: PROCESSAMENTO INCREMENTAL
 * 
 * Justificativa:
 * - Optei por processar incrementalmente (arquivo por arquivo) em vez de carregar
 *   todos em memória de uma vez
 * - Prós: Menor consumo de memória, permite processar arquivos maiores, resiliente a falhas
 * - Contras: Potencialmente mais lento devido a múltiplas operações de I/O
 * - Considerando que os arquivos da ANS podem ser grandes (centenas de MB),
 *   o processamento incremental é mais seguro e escalável
 */
public class DownloadService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    
    private final ApplicationConfig config;
    private final HttpClient httpClient;
    private final AnsDataService ansDataService;

    public DownloadService(ApplicationConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.getTimeoutConexaoMs()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.ansDataService = new AnsDataService(config);
    } // DownloadService

    /**
     * Baixa e extrai arquivos de múltiplos trimestres
     * 
     * @param trimestres Lista de identificadores de trimestres
     * @return Lista de caminhos para arquivos extraídos
     */
    public List<Path> baixarEExtrairTrimestres(List<String> trimestres) throws Exception {
        // Criar diretórios de trabalho
        Files.createDirectories(config.getDiretorioDownload());
        Files.createDirectories(config.getDiretorioExtracao());
        
        List<Path> todosArquivosExtraidos = new ArrayList<>();
        
        for (String trimestre : trimestres) {
            logger.info("Processando trimestre: {}", trimestre);
            
            try {
                // Buscar URLs dos ZIPs do trimestre
                List<String> urlsZip = ansDataService.buscarArquivosZipTrimestre(trimestre);
                logger.info("Encontrados {} arquivos ZIP no trimestre {}", urlsZip.size(), trimestre);
                
                for (String urlZip : urlsZip) {
                    try {
                        // Download do ZIP
                        Path arquivoZip = baixarArquivo(urlZip, trimestre);
                        
                        // Extração
                        List<Path> extraidos = extrairZip(arquivoZip, trimestre);
                        todosArquivosExtraidos.addAll(extraidos);
                        
                        logger.info("Extraídos {} arquivos de {}", extraidos.size(), arquivoZip.getFileName());
                    } catch (Exception e) {
                        logger.error("Erro ao processar arquivo {}: {}", urlZip, e.getMessage());
                        // Continua com o próximo arquivo (resiliência)
                    } // try-catch
                } // for urlsZip
            } catch (Exception e) {
                logger.error("Erro ao processar trimestre {}: {}", trimestre, e.getMessage());
                // Continua com o próximo trimestre (resiliência)
            } // try-catch
        } // for trimestres
        
        return todosArquivosExtraidos;
    } // baixarEExtrairTrimestres

    /**
     * Baixa um arquivo da URL especificada
     */
    private Path baixarArquivo(String url, String trimestre) throws Exception {
        logger.debug("Baixando: {}", url);
        
        // Extrair nome do arquivo da URL
        String nomeArquivo = url.substring(url.lastIndexOf('/') + 1);
        
        // Criar subdiretório para o trimestre
        Path diretorioTrimestre = config.getDiretorioDownload().resolve(trimestre.replace("/", "_"));
        Files.createDirectories(diretorioTrimestre);
        
        Path arquivoDestino = diretorioTrimestre.resolve(nomeArquivo);
        
        // Se já existe, não baixa novamente (cache simples)
        if (Files.exists(arquivoDestino)) {
            logger.debug("Arquivo já existe, usando cache: {}", arquivoDestino);
            return arquivoDestino;
        } // if
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(config.getTimeoutLeituraMs()))
            .GET()
            .build();
        
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao baixar arquivo: HTTP " + response.statusCode());
        } // if
        
        try (InputStream is = response.body()) {
            Files.copy(is, arquivoDestino, StandardCopyOption.REPLACE_EXISTING);
        } // try
        
        logger.info("Download concluído: {}", arquivoDestino);
        return arquivoDestino;
    } // baixarArquivo

    /**
     * Extrai um arquivo ZIP
     * Trata diferentes encodings e estruturas de diretório
     */
    private List<Path> extrairZip(Path arquivoZip, String trimestre) throws IOException {
        List<Path> arquivosExtraidos = new ArrayList<>();
        
        Path diretorioDestino = config.getDiretorioExtracao().resolve(trimestre.replace("/", "_"));
        Files.createDirectories(diretorioDestino);
        
        try (InputStream fis = Files.newInputStream(arquivoZip);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipArchiveInputStream zis = new ZipArchiveInputStream(bis, "UTF-8", true, true)) {
            
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                } // if
                
                String nomeArquivo = entry.getName();
                // Remover estrutura de diretórios interna do ZIP
                if (nomeArquivo.contains("/")) {
                    nomeArquivo = nomeArquivo.substring(nomeArquivo.lastIndexOf('/') + 1);
                } // if
                if (nomeArquivo.contains("\\")) {
                    nomeArquivo = nomeArquivo.substring(nomeArquivo.lastIndexOf('\\') + 1);
                } // if
                
                Path arquivoDestino = diretorioDestino.resolve(nomeArquivo);
                
                // Evitar path traversal
                if (!arquivoDestino.normalize().startsWith(diretorioDestino.normalize())) {
                    logger.warn("Tentativa de path traversal detectada: {}", entry.getName());
                    continue;
                } // if
                
                Files.copy(zis, arquivoDestino, StandardCopyOption.REPLACE_EXISTING);
                arquivosExtraidos.add(arquivoDestino);
                logger.debug("Extraído: {}", arquivoDestino);
            } // while
        } // try
        
        return arquivosExtraidos;
    } // extrairZip
} // DownloadService
