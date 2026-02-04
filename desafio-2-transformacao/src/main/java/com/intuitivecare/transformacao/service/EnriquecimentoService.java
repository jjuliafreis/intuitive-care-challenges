package com.intuitivecare.transformacao.service;

import com.intuitivecare.transformacao.config.ApplicationConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;

/**
 * Serviço de Enriquecimento de Dados
 * 
 * Faz join entre dados de despesas (por RegistroANS) e cadastro de operadoras
 * para obter CNPJ, Razão Social, Modalidade e UF.
 * 
 * Trade-off: JOIN EM MEMÓRIA COM HASHMAP
 * - Carrega cadastro em HashMap (chave: Registro_ANS)
 * - Lookup O(1) para cada registro de despesa
 */
public class EnriquecimentoService {

    private static final Logger logger = LoggerFactory.getLogger(EnriquecimentoService.class);

    private final ApplicationConfig config;
    private final HttpClient httpClient;
    
    // Estatísticas
    private int registrosComMatch = 0;
    private int registrosSemMatch = 0;

    public EnriquecimentoService(ApplicationConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    } // EnriquecimentoService

    /**
     * Enriquece dados do CSV validado com informações cadastrais
     */
    public Path enriquecerDados(Path csvValidado) throws Exception {
        logger.info("Iniciando enriquecimento de dados");
        
        // 1. Baixar e carregar dados cadastrais
        Map<String, OperadoraInfo> cadastroMap = carregarCadastroOperadoras();
        logger.info("Cadastro carregado: {} operadoras", cadastroMap.size());
        
        // 2. Processar CSV validado e fazer join
        List<DespesaEnriquecida> despesasEnriquecidas = processarJoin(csvValidado, cadastroMap);
        
        // 3. Salvar resultado
        Path csvOutput = config.getDiretorioOutput().resolve("consolidado_enriquecido.csv");
        escreverCsvEnriquecido(despesasEnriquecidas, csvOutput);
        
        // Log estatísticas
        logger.info("=== Relatório de Enriquecimento ===");
        logger.info("Registros com match: {}", registrosComMatch);
        logger.info("Registros sem match: {}", registrosSemMatch);
        logger.info("===================================");
        
        return csvOutput;
    } // enriquecerDados

    /**
     * Baixa e carrega dados cadastrais das operadoras
     */
    private Map<String, OperadoraInfo> carregarCadastroOperadoras() throws Exception {
        Path arquivoCadastro = baixarArquivoCadastro();
        
        Map<String, OperadoraInfo> mapa = new HashMap<>();
        
        // Tentar diferentes delimitadores
        char delimitador = detectarDelimitador(arquivoCadastro);
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimitador)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
        
        try (Reader reader = Files.newBufferedReader(arquivoCadastro, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            List<String> headers = parser.getHeaderNames();
            logger.debug("Colunas do cadastro: {}", headers);
            
            // Identificar colunas por nome (case-insensitive)
            Map<String, String> colunaMap = mapearColunas(headers);
            
            for (CSVRecord record : parser) {
                try {
                    String registroAns = getValor(record, colunaMap.get("REGISTRO_ANS"));
                    String cnpj = getValor(record, colunaMap.get("CNPJ"));
                    String razaoSocial = getValor(record, colunaMap.get("RAZAO_SOCIAL"));
                    String modalidade = getValor(record, colunaMap.get("MODALIDADE"));
                    String uf = getValor(record, colunaMap.get("UF"));
                    
                    if (registroAns != null && !registroAns.isBlank()) {
                        mapa.put(registroAns, new OperadoraInfo(cnpj, razaoSocial, modalidade, uf));
                    }
                } catch (Exception e) {
                    logger.debug("Erro ao processar registro: {}", e.getMessage());
                }
            }
        }
        
        return mapa;
    } // carregarCadastroOperadoras

    /**
     * Mapeia nomes de colunas do arquivo para nomes padronizados
     * Colunas do cadastro ANS:
     * - REGISTRO_OPERADORA: código do registro ANS (ex: 325465)
     * - CNPJ: CNPJ da operadora
     * - Razao_Social: razão social
     * - Modalidade: tipo de operadora
     * - UF: estado
     */
    private Map<String, String> mapearColunas(List<String> headers) {
        Map<String, String> mapa = new HashMap<>();
        
        for (String header : headers) {
            String h = header.toLowerCase();
            
            // REGISTRO_OPERADORA é o código ANS (ex: 325465), NÃO confundir com Data_Registro_ANS
            if (h.equals("registro_operadora") || h.equals("registro_ans") || h.equals("reg_ans")) {
                mapa.put("REGISTRO_ANS", header);
            } else if (h.equals("cnpj") || h.contains("cd_cnpj") || h.contains("nr_cnpj")) {
                mapa.put("CNPJ", header);
            } else if (h.contains("razao") || h.contains("razão")) {
                mapa.put("RAZAO_SOCIAL", header);
            } else if (h.contains("modalidade")) {
                mapa.put("MODALIDADE", header);
            } else if (h.equals("uf") || h.contains("estado") || h.contains("sigla_uf")) {
                mapa.put("UF", header);
            }
        }
        
        logger.debug("Mapeamento de colunas: {}", mapa);
        return mapa;
    } // mapearColunas

    private String getValor(CSVRecord record, String coluna) {
        if (coluna == null) return "";
        try {
            return record.get(coluna);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Baixa o arquivo de cadastro de operadoras
     */
    private Path baixarArquivoCadastro() throws Exception {
        Files.createDirectories(config.getDiretorioTemp());
        
        String urlArquivo = buscarUrlArquivoCadastro();
        logger.info("Baixando cadastro de: {}", urlArquivo);
        
        String nomeArquivo = urlArquivo.substring(urlArquivo.lastIndexOf('/') + 1);
        Path arquivoDestino = config.getDiretorioTemp().resolve(nomeArquivo);
        
        // Usar cache se existir
        if (Files.exists(arquivoDestino)) {
            logger.info("Usando cache: {}", arquivoDestino);
            return arquivoDestino;
        }
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlArquivo))
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build();
        
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao baixar cadastro: HTTP " + response.statusCode());
        }
        
        try (InputStream is = response.body()) {
            Files.copy(is, arquivoDestino, StandardCopyOption.REPLACE_EXISTING);
        }
        
        logger.info("Download concluído: {}", arquivoDestino);
        return arquivoDestino;
    } // baixarArquivoCadastro

    /**
     * Busca a URL do arquivo de cadastro na página da ANS
     */
    private String buscarUrlArquivoCadastro() throws IOException {
        Document doc = Jsoup.connect(config.getOperadorasAtivasUrl())
            .timeout(30000)
            .get();
        
        Elements links = doc.select("a[href$=.csv], a[href$=.CSV]");
        
        for (Element link : links) {
            String href = link.attr("href");
            if (href.toLowerCase().contains("relatorio") || href.toLowerCase().contains("cadop")) {
                if (!href.startsWith("http")) {
                    return config.getOperadorasAtivasUrl() + href;
                }
                return href;
            }
        }
        
        // Fallback: pegar o primeiro CSV
        if (!links.isEmpty()) {
            String href = links.first().attr("href");
            if (!href.startsWith("http")) {
                return config.getOperadorasAtivasUrl() + href;
            }
            return href;
        }
        
        throw new IOException("Não foi possível encontrar arquivo de cadastro");
    } // buscarUrlArquivoCadastro

    private char detectarDelimitador(Path arquivo) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            String linha = reader.readLine();
            if (linha != null) {
                if (linha.contains(";")) return ';';
                if (linha.contains("\t")) return '\t';
            }
        }
        return ',';
    }

    /**
     * Processa join entre despesas e cadastro
     */
    private List<DespesaEnriquecida> processarJoin(Path csvValidado, Map<String, OperadoraInfo> cadastro) throws IOException {
        List<DespesaEnriquecida> resultado = new ArrayList<>();
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
        
        try (Reader reader = Files.newBufferedReader(csvValidado, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            for (CSVRecord record : parser) {
                String registroAns = record.get("RegistroANS");
                String trimestre = record.get("Trimestre");
                String ano = record.get("Ano");
                String valorStr = record.get("ValorDespesas");
                
                OperadoraInfo info = cadastro.get(registroAns);
                
                DespesaEnriquecida despesa = new DespesaEnriquecida();
                despesa.registroAns = registroAns;
                despesa.trimestre = trimestre;
                despesa.ano = Integer.parseInt(ano);
                despesa.valorDespesas = new BigDecimal(valorStr);
                
                if (info != null) {
                    despesa.cnpj = info.cnpj;
                    despesa.razaoSocial = info.razaoSocial;
                    despesa.modalidade = info.modalidade;
                    despesa.uf = info.uf;
                    registrosComMatch++;
                } else {
                    despesa.cnpj = "";
                    despesa.razaoSocial = "OPERADORA NÃO ENCONTRADA";
                    despesa.modalidade = "";
                    despesa.uf = "";
                    registrosSemMatch++;
                }
                
                resultado.add(despesa);
            }
        }
        
        return resultado;
    } // processarJoin

    /**
     * Escreve CSV enriquecido
     */
    private void escreverCsvEnriquecido(List<DespesaEnriquecida> despesas, Path arquivo) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader("CNPJ", "RegistroANS", "RazaoSocial", "Modalidade", "UF", "Trimestre", "Ano", "ValorDespesas")
            .build();
        
        try (Writer writer = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            
            for (DespesaEnriquecida d : despesas) {
                printer.printRecord(
                    d.cnpj,
                    d.registroAns,
                    d.razaoSocial,
                    d.modalidade,
                    d.uf,
                    d.trimestre,
                    d.ano,
                    d.valorDespesas.toPlainString()
                );
            }
        }
        
        logger.info("CSV enriquecido: {} ({} registros)", arquivo, despesas.size());
    } // escreverCsvEnriquecido

    // Classes internas
    private static class OperadoraInfo {
        String cnpj;
        String razaoSocial;
        String modalidade;
        String uf;
        
        OperadoraInfo(String cnpj, String razaoSocial, String modalidade, String uf) {
            this.cnpj = cnpj != null ? cnpj : "";
            this.razaoSocial = razaoSocial != null ? razaoSocial : "";
            this.modalidade = modalidade != null ? modalidade : "";
            this.uf = uf != null ? uf : "";
        }
    }
    
    private static class DespesaEnriquecida {
        String cnpj;
        String registroAns;
        String razaoSocial;
        String modalidade;
        String uf;
        String trimestre;
        int ano;
        BigDecimal valorDespesas;
    }
} // EnriquecimentoService
