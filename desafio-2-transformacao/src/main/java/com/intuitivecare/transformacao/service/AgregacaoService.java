package com.intuitivecare.transformacao.service;

import com.intuitivecare.transformacao.config.ApplicationConfig;
import com.intuitivecare.transformacao.model.DespesaAgregada;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Serviço de Agregação de Dados
 * 
 * Trade-off técnico implementado: ORDENAÇÃO EM MEMÓRIA COM COLLECTIONS.SORT
 * 
 * Estratégia: Ordenar usando TimSort (algoritmo padrão do Java)
 * 
 * Justificativa:
 * - PRÓS:
 *   * TimSort é O(n log n) e altamente otimizado
 *   * Estável (mantém ordem relativa de elementos iguais)
 *   * Excelente performance para dados parcialmente ordenados
 *   * Funciona bem para o volume esperado (~1000-10000 agregações)
 * 
 * - CONTRAS:
 *   * Requer todos os dados em memória
 *   * Para bilhões de registros, precisaria de external sort
 * 
 * - ALTERNATIVAS CONSIDERADAS:
 *   * External Merge Sort: Overkill para este volume
 *   * Banco de dados com ORDER BY: Overhead de infraestrutura
 *   * Parallel Sort: Possível melhoria para datasets maiores
 */
public class AgregacaoService {

    private static final Logger logger = LoggerFactory.getLogger(AgregacaoService.class);

    private final ApplicationConfig config;

    public AgregacaoService(ApplicationConfig config) {
        this.config = config;
    } // AgregacaoService

    /**
     * Agrega dados por RazaoSocial e UF
     * 
     * @param csvEnriquecido Caminho do CSV enriquecido
     * @return Caminho do CSV agregado
     */
    public Path agregarDados(Path csvEnriquecido) throws IOException {
        logger.info("Iniciando agregação de dados");
        
        // Mapa para agregação: chave = "RazaoSocial|UF"
        Map<String, List<BigDecimal>> agregacoes = new LinkedHashMap<>();
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
        
        try (Reader reader = Files.newBufferedReader(csvEnriquecido, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            for (CSVRecord record : parser) {
                String razaoSocial = record.get("RazaoSocial");
                String uf = record.get("UF");
                String valorStr = record.get("ValorDespesas");
                
                // Usar "N/A" para UF vazia
                if (uf == null || uf.isBlank()) {
                    uf = "N/A";
                } // if
                
                String chave = razaoSocial + "|" + uf;
                
                BigDecimal valor;
                try {
                    valor = new BigDecimal(valorStr);
                } catch (Exception e) {
                    valor = BigDecimal.ZERO;
                } // try-catch
                
                agregacoes.computeIfAbsent(chave, k -> new ArrayList<>()).add(valor);
            } // for
        } // try
        
        // Calcular estatísticas para cada grupo
        List<DespesaAgregada> resultados = calcularEstatisticas(agregacoes);
        
        // Ordenar por valor total (maior para menor)
        resultados.sort((a, b) -> b.getTotalDespesas().compareTo(a.getTotalDespesas()));
        
        // Salvar resultado
        Path csvOutput = config.getDiretorioOutput().resolve("despesas_agregadas.csv");
        escreverCsvAgregado(resultados, csvOutput);
        
        logger.info("Agregação concluída: {} grupos", resultados.size());
        return csvOutput;
    } // agregarDados

    /**
     * Calcula estatísticas para cada grupo de agregação
     */
    private List<DespesaAgregada> calcularEstatisticas(Map<String, List<BigDecimal>> agregacoes) {
        List<DespesaAgregada> resultados = new ArrayList<>();
        
        for (Map.Entry<String, List<BigDecimal>> entry : agregacoes.entrySet()) {
            String[] partes = entry.getKey().split("\\|");
            String razaoSocial = partes[0];
            String uf = partes.length > 1 ? partes[1] : "N/A";
            
            List<BigDecimal> valores = entry.getValue();
            
            DespesaAgregada agregada = new DespesaAgregada();
            agregada.setRazaoSocial(razaoSocial);
            agregada.setUf(uf);
            agregada.setQuantidadeTrimestres(valores.size());
            
            // Calcular total
            BigDecimal total = valores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            agregada.setTotalDespesas(total);
            
            // Calcular média
            BigDecimal media = total.divide(
                BigDecimal.valueOf(valores.size()), 
                2, 
                RoundingMode.HALF_UP
            );
            agregada.setMediaPorTrimestre(media);
            
            // Calcular desvio padrão usando Apache Commons Math
            BigDecimal desvioPadrao = calcularDesvioPadrao(valores);
            agregada.setDesvioPadrao(desvioPadrao);
            
            resultados.add(agregada);
        } // for
        
        return resultados;
    } // calcularEstatisticas

    /**
     * Calcula desvio padrão dos valores
     */
    private BigDecimal calcularDesvioPadrao(List<BigDecimal> valores) {
        if (valores.size() < 2) {
            return BigDecimal.ZERO;
        } // if
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (BigDecimal valor : valores) {
            stats.addValue(valor.doubleValue());
        } // for
        
        double dp = stats.getStandardDeviation();
        return BigDecimal.valueOf(dp).setScale(2, RoundingMode.HALF_UP);
    } // calcularDesvioPadrao

    /**
     * Escreve o CSV agregado
     */
    private void escreverCsvAgregado(List<DespesaAgregada> agregados, Path arquivo) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("RazaoSocial", "UF", "TotalDespesas", "MediaPorTrimestre", 
                       "DesvioPadrao", "QuantidadeTrimestres")
            .setDelimiter(';')
            .build();
        
        try (Writer writer = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (DespesaAgregada a : agregados) {
                printer.printRecord(
                    a.getRazaoSocial(),
                    a.getUf(),
                    a.getTotalDespesas().toPlainString(),
                    a.getMediaPorTrimestre().toPlainString(),
                    a.getDesvioPadrao().toPlainString(),
                    a.getQuantidadeTrimestres()
                );
            } // for
        } // try
        
        logger.info("CSV agregado gerado: {} ({} registros)", arquivo, agregados.size());
    } // escreverCsvAgregado

    /**
     * Compacta o resultado final
     */
    public Path compactarResultado(Path csvAgregado) throws IOException {
        Path zipFile = config.getDiretorioOutput().resolve("Teste_Candidato.zip");
        
        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // Adicionar CSV agregado
            adicionarAoZip(zos, csvAgregado, "despesas_agregadas.csv");
            
            // Adicionar outros arquivos relevantes se existirem
            Path csvEnriquecido = config.getDiretorioOutput().resolve("consolidado_enriquecido.csv");
            if (Files.exists(csvEnriquecido)) {
                adicionarAoZip(zos, csvEnriquecido, "consolidado_enriquecido.csv");
            } // if
            
            Path csvValidado = config.getDiretorioOutput().resolve("consolidado_validado.csv");
            if (Files.exists(csvValidado)) {
                adicionarAoZip(zos, csvValidado, "consolidado_validado.csv");
            } // if
        } // try
        
        logger.info("ZIP gerado: {}", zipFile);
        return zipFile;
    } // compactarResultado

    private void adicionarAoZip(ZipOutputStream zos, Path arquivo, String nomeNoZip) throws IOException {
        ZipEntry zipEntry = new ZipEntry(nomeNoZip);
        zos.putNextEntry(zipEntry);
        Files.copy(arquivo, zos);
        zos.closeEntry();
    } // adicionarAoZip
} // AgregacaoService
