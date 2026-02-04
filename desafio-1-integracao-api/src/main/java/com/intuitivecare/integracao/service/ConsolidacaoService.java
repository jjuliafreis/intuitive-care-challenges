package com.intuitivecare.integracao.service;

import com.intuitivecare.integracao.config.ApplicationConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Serviço responsável pela consolidação dos dados de despesas da ANS
 * 
 * Formato dos arquivos da ANS:
 * - DATA: Data do registro (ex: 2025-01-01)
 * - REG_ANS: Registro da operadora na ANS
 * - CD_CONTA_CONTABIL: Código da conta contábil
 * - DESCRICAO: Descrição da conta
 * - VL_SALDO_INICIAL: Valor inicial
 * - VL_SALDO_FINAL: Valor final (usado para despesas)
 * 
 * Filtramos apenas contas que começam com "41" (Eventos Indenizáveis / Sinistros)
 */
public class ConsolidacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ConsolidacaoService.class);
    
    // Padrão para extrair trimestre e ano do nome do arquivo (ex: 1T2025.csv)
    private static final Pattern ARQUIVO_PATTERN = Pattern.compile("([1-4])T(\\d{4})");
    
    // Código da conta de Eventos Indenizáveis (nível principal)
    private static final String CONTA_EVENTOS = "41";

    private final ApplicationConfig config;
    
    // Estatísticas
    private int registrosProcessados = 0;
    private int registrosDescartados = 0;
    private int operadorasUnicas = 0;

    public ConsolidacaoService(ApplicationConfig config) {
        this.config = config;
    } // ConsolidacaoService

    /**
     * Consolida dados de múltiplos arquivos em um único CSV
     * 
     * @param arquivos Lista de arquivos a processar
     * @return Caminho do CSV consolidado
     */
    public Path consolidarDados(List<Path> arquivos) throws Exception {
        logger.info("Iniciando consolidação de {} arquivos", arquivos.size());
        
        // Map para agregar despesas por operadora e trimestre
        // Chave: REG_ANS + "-" + trimestre + "-" + ano
        Map<String, BigDecimal> despesasPorOperadora = new HashMap<>();
        Set<String> operadoras = new HashSet<>();
        
        for (Path arquivo : arquivos) {
            try {
                processarArquivo(arquivo, despesasPorOperadora, operadoras);
            } catch (Exception e) {
                logger.error("Erro ao processar arquivo {}: {}", arquivo.getFileName(), e.getMessage());
            } // try-catch
        } // for
        
        operadorasUnicas = operadoras.size();
        
        // Criar diretório de output
        Files.createDirectories(config.getDiretorioOutput());
        
        // Gerar CSV consolidado
        Path csvOutput = config.getDiretorioOutput().resolve("consolidado_despesas.csv");
        escreverCsv(despesasPorOperadora, csvOutput);
        
        // Log estatísticas
        logger.info("=== Relatório de Consolidação ===");
        logger.info("Registros processados: {}", registrosProcessados);
        logger.info("Registros descartados: {}", registrosDescartados);
        logger.info("Operadoras únicas: {}", operadorasUnicas);
        logger.info("=================================");
        
        return csvOutput;
    } // consolidarDados

    /**
     * Processa um arquivo CSV da ANS
     */
    private void processarArquivo(Path arquivo, Map<String, BigDecimal> despesas, Set<String> operadoras) throws Exception {
        logger.info("Processando: {}", arquivo.getFileName());
        
        // Extrair trimestre e ano do nome do arquivo
        String nomeArquivo = arquivo.getFileName().toString();
        Matcher matcher = ARQUIVO_PATTERN.matcher(nomeArquivo);
        
        String trimestre = "Q1";
        int ano = 2025;
        
        if (matcher.find()) {
            trimestre = "Q" + matcher.group(1);
            ano = Integer.parseInt(matcher.group(2));
        } else {
            logger.warn("Não foi possível extrair trimestre/ano de: {}", nomeArquivo);
        }
        
        // Ler CSV com separador ; (padrão ANS)
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
        
        int registrosArquivo = 0;
        
        try (Reader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            for (CSVRecord record : parser) {
                try {
                    String regAns = record.get("REG_ANS");
                    String contaContabil = record.get("CD_CONTA_CONTABIL");
                    String valorStr = record.get("VL_SALDO_FINAL");
                    
                    // Filtrar apenas conta 41 (nível principal - Eventos Indenizáveis)
                    // Usamos exatamente "41" para não duplicar com subcontas
                    if (!CONTA_EVENTOS.equals(contaContabil)) {
                        continue;
                    }
                    
                    if (regAns == null || regAns.isBlank()) {
                        registrosDescartados++;
                        continue;
                    }
                    
                    // Parsear valor (formato brasileiro: vírgula como decimal)
                    BigDecimal valor = parseValor(valorStr);
                    
                    // Chave única: REG_ANS-trimestre-ano
                    String chave = regAns + "-" + trimestre + "-" + ano;
                    
                    // Agregar valor
                    despesas.merge(chave, valor, BigDecimal::add);
                    operadoras.add(regAns);
                    
                    registrosProcessados++;
                    registrosArquivo++;
                    
                } catch (Exception e) {
                    logger.debug("Erro ao processar linha {}: {}", record.getRecordNumber(), e.getMessage());
                    registrosDescartados++;
                } // try-catch
            } // for
        } // try
        
        logger.info("Processados {} registros de {}", registrosArquivo, nomeArquivo);
    } // processarArquivo

    /**
     * Parseia valor no formato brasileiro (vírgula como decimal)
     */
    private BigDecimal parseValor(String valorStr) {
        if (valorStr == null || valorStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        
        // Remover aspas se houver
        valorStr = valorStr.replace("\"", "").trim();
        
        // Formato brasileiro: 1.234,56 -> 1234.56
        valorStr = valorStr.replace(".", "").replace(",", ".");
        
        try {
            return new BigDecimal(valorStr);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    } // parseValor

    /**
     * Escreve o CSV consolidado
     */
    private void escreverCsv(Map<String, BigDecimal> despesas, Path outputPath) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader("RegistroANS", "Trimestre", "Ano", "ValorDespesas")
            .build();
        
        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (Map.Entry<String, BigDecimal> entry : despesas.entrySet()) {
                String[] partes = entry.getKey().split("-");
                if (partes.length >= 3) {
                    String regAns = partes[0];
                    String trimestre = partes[1];
                    String ano = partes[2];
                    BigDecimal valor = entry.getValue();
                    
                    printer.printRecord(regAns, trimestre, ano, valor);
                }
            }
        }
        
        logger.info("CSV gerado: {} ({} registros)", outputPath, despesas.size());
    } // escreverCsv

    /**
     * Compacta o CSV em um arquivo ZIP
     */
    public Path compactarCsv(Path csvPath) throws IOException {
        Path zipPath = csvPath.resolveSibling(
            csvPath.getFileName().toString().replace(".csv", ".zip")
        );
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            ZipEntry entry = new ZipEntry(csvPath.getFileName().toString());
            zos.putNextEntry(entry);
            
            Files.copy(csvPath, zos);
            
            zos.closeEntry();
        }
        
        logger.info("ZIP gerado: {}", zipPath);
        return zipPath;
    } // compactarCsv
} // ConsolidacaoService
