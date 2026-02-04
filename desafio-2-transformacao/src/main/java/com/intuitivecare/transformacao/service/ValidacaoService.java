package com.intuitivecare.transformacao.service;

import com.intuitivecare.transformacao.config.ApplicationConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de Validação de Dados
 * 
 * Valida os dados do CSV consolidado do Desafio 1:
 * - RegistroANS: Código de registro na ANS
 * - Trimestre: Q1, Q2, Q3 ou Q4
 * - Ano: Ano do registro
 * - ValorDespesas: Valor das despesas
 */
public class ValidacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ValidacaoService.class);

    private final ApplicationConfig config;
    
    // Estatísticas
    private int totalRegistros = 0;
    private int registrosValidos = 0;
    private int registrosInvalidos = 0;

    public ValidacaoService(ApplicationConfig config) {
        this.config = config;
    } // ValidacaoService

    /**
     * Valida os dados do CSV consolidado
     * 
     * @param csvInput Caminho do CSV de entrada
     * @return Caminho do CSV validado
     */
    public Path validarDados(Path csvInput) throws IOException {
        logger.info("Iniciando validação de dados: {}", csvInput);
        
        if (!Files.exists(csvInput)) {
            throw new IOException("Arquivo não encontrado: " + csvInput);
        }
        
        Files.createDirectories(config.getDiretorioOutput());
        Path csvOutput = config.getDiretorioOutput().resolve("consolidado_validado.csv");
        
        List<String[]> registrosValidados = new ArrayList<>();
        
        CSVFormat formatLeitura = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
        
        try (Reader reader = Files.newBufferedReader(csvInput, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, formatLeitura)) {
            
            for (CSVRecord record : parser) {
                totalRegistros++;
                
                String registroAns = getValorSeguro(record, "RegistroANS");
                String trimestre = getValorSeguro(record, "Trimestre");
                String anoStr = getValorSeguro(record, "Ano");
                String valorStr = getValorSeguro(record, "ValorDespesas");
                
                // Validar campos obrigatórios
                if (registroAns.isBlank() || trimestre.isBlank() || anoStr.isBlank()) {
                    registrosInvalidos++;
                    continue;
                }
                
                // Validar valor
                BigDecimal valor = parseValor(valorStr);
                if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                    registrosInvalidos++;
                    continue;
                }
                
                // Registro válido
                registrosValidos++;
                registrosValidados.add(new String[]{
                    registroAns, trimestre, anoStr, valor.toPlainString()
                });
            } // for
        } // try
        
        // Escrever CSV validado
        escreverCsv(registrosValidados, csvOutput);
        
        // Log estatísticas
        logger.info("=== Relatório de Validação ===");
        logger.info("Total de registros: {}", totalRegistros);
        logger.info("Registros válidos: {}", registrosValidos);
        logger.info("Registros inválidos: {}", registrosInvalidos);
        logger.info("==============================");
        
        return csvOutput;
    } // validarDados

    private void escreverCsv(List<String[]> registros, Path arquivo) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("RegistroANS", "Trimestre", "Ano", "ValorDespesas")
            .setDelimiter(';')
            .build();
        
        try (Writer writer = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (String[] r : registros) {
                printer.printRecord((Object[]) r);
            }
        }
        
        logger.info("CSV validado: {} ({} registros)", arquivo, registros.size());
    } // escreverCsv

    private String getValorSeguro(CSVRecord record, String coluna) {
        try {
            return record.get(coluna);
        } catch (Exception e) {
            return "";
        }
    } // getValorSeguro

    private BigDecimal parseValor(String valorStr) {
        if (valorStr == null || valorStr.isBlank()) {
            return null;
        }
        try {
            // Formato pode ter vírgula como decimal
            valorStr = valorStr.replace(",", ".");
            return new BigDecimal(valorStr);
        } catch (NumberFormatException e) {
            return null;
        }
    } // parseValor
} // ValidacaoService
