package com.intuitivecare.integracao.service;

import com.intuitivecare.integracao.config.ApplicationConfig;
import com.intuitivecare.integracao.model.TrimestreInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Serviço responsável por interagir com a API de Dados Abertos da ANS
 * Identifica e lista os trimestres disponíveis para download
 * 
 * Estrutura real da ANS:
 * - /demonstracoes_contabeis/2024/1T2024.zip
 * - /demonstracoes_contabeis/2024/2T2024.zip
 * - etc.
 */
public class AnsDataService {

    private static final Logger logger = LoggerFactory.getLogger(AnsDataService.class);
    
    // Padrão para identificar diretórios de ano (ex: 2024/)
    private static final Pattern ANO_PATTERN = Pattern.compile("(\\d{4})/");
    
    // Padrão para identificar arquivos ZIP de trimestre (ex: 1T2024.zip, 2T2024.zip)
    private static final Pattern ZIP_TRIMESTRE_PATTERN = Pattern.compile("([1-4])T(\\d{4})\\.zip", Pattern.CASE_INSENSITIVE);

    private final ApplicationConfig config;

    public AnsDataService(ApplicationConfig config) {
        this.config = config;
    } // AnsDataService

    /**
     * Identifica os últimos N trimestres disponíveis na API ANS
     * 
     * @param quantidade Número de trimestres a identificar
     * @return Lista de identificadores de trimestres (formato: "YYYY/XT" onde X é 1-4)
     * @throws IOException Se houver erro de conexão
     */
    public List<String> identificarUltimosTrimestres(int quantidade) throws IOException {
        logger.info("Buscando trimestres disponíveis em: {}", config.getDemonstracoesContabeisUrl());
        
        List<TrimestreInfo> todosTrimestres = new ArrayList<>();
        
        // Buscar anos disponíveis
        List<Integer> anos = buscarAnosDisponiveis();
        logger.info("Anos encontrados: {}", anos);
        
        // Para cada ano, buscar trimestres (arquivos ZIP disponíveis)
        for (Integer ano : anos) {
            List<TrimestreInfo> trimestresAno = buscarTrimestresDoAno(ano);
            todosTrimestres.addAll(trimestresAno);
        } // for
        
        // Ordenar por data (mais recente primeiro) e pegar os N últimos
        List<String> ultimosTrimestres = todosTrimestres.stream()
            .sorted(TrimestreInfo::compareToDesc)
            .limit(quantidade)
            .map(TrimestreInfo::getIdentificador)
            .collect(Collectors.toList());
        
        logger.info("Últimos {} trimestres identificados: {}", quantidade, ultimosTrimestres);
        return ultimosTrimestres;
    } // identificarUltimosTrimestres

    /**
     * Busca os anos disponíveis no diretório de demonstrações contábeis
     */
    private List<Integer> buscarAnosDisponiveis() throws IOException {
        Document doc = Jsoup.connect(config.getDemonstracoesContabeisUrl())
            .timeout(config.getTimeoutConexaoMs())
            .get();
        
        Elements links = doc.select("a[href]");
        List<Integer> anos = new ArrayList<>();
        
        for (Element link : links) {
            String href = link.attr("href");
            Matcher matcher = ANO_PATTERN.matcher(href);
            if (matcher.find()) {
                try {
                    int ano = Integer.parseInt(matcher.group(1));
                    if (ano >= 2000 && ano <= 2100) { // Validação básica
                        anos.add(ano);
                    } // if
                } catch (NumberFormatException e) {
                    logger.debug("Ignorando link não numérico: {}", href);
                } // try-catch
            } // if
        } // for
        
        return anos.stream().distinct().sorted().collect(Collectors.toList());
    } // buscarAnosDisponiveis

    /**
     * Busca os trimestres disponíveis para um ano específico
     * Identifica pelos arquivos ZIP no formato XTyyyy.zip
     */
    private List<TrimestreInfo> buscarTrimestresDoAno(int ano) throws IOException {
        String urlAno = config.getDemonstracoesContabeisUrl() + ano + "/";
        logger.debug("Buscando trimestres do ano {} em: {}", ano, urlAno);
        
        List<TrimestreInfo> trimestres = new ArrayList<>();
        
        try {
            Document doc = Jsoup.connect(urlAno)
                .timeout(config.getTimeoutConexaoMs())
                .get();
            
            Elements links = doc.select("a[href$=.zip], a[href$=.ZIP]");
            
            for (Element link : links) {
                String href = link.attr("href");
                Matcher matcher = ZIP_TRIMESTRE_PATTERN.matcher(href);
                if (matcher.find()) {
                    int numTrimestre = Integer.parseInt(matcher.group(1));
                    int anoArquivo = Integer.parseInt(matcher.group(2));
                    String urlZip = urlAno + href;
                    trimestres.add(new TrimestreInfo(anoArquivo, numTrimestre, urlZip));
                    logger.debug("Trimestre encontrado: {}/{}T - {}", anoArquivo, numTrimestre, href);
                } // if
            } // for
        } catch (IOException e) {
            logger.warn("Erro ao buscar trimestres do ano {}: {}", ano, e.getMessage());
        } // try-catch
        
        return trimestres;
    } // buscarTrimestresDoAno

    /**
     * Obtém URL do arquivo ZIP para um trimestre específico
     * @param identificadorTrimestre formato "YYYY/XT" ex: "2024/3T"
     */
    public List<String> buscarArquivosZipTrimestre(String identificadorTrimestre) throws IOException {
        // Identificador agora é no formato "2024/3T"
        // Precisamos construir a URL do ZIP: 2024/3T2024.zip
        String[] partes = identificadorTrimestre.split("/");
        if (partes.length != 2) {
            throw new IOException("Formato de identificador inválido: " + identificadorTrimestre);
        }
        
        String ano = partes[0];
        String trimestre = partes[1]; // ex: "3T"
        String nomeZip = trimestre + ano + ".zip";
        String urlZip = config.getDemonstracoesContabeisUrl() + ano + "/" + nomeZip;
        
        logger.debug("URL do ZIP: {}", urlZip);
        
        return List.of(urlZip);
    } // buscarArquivosZipTrimestre
} // AnsDataService
