package com.intuitivecare.integracao;

import com.intuitivecare.integracao.config.ApplicationConfig;
import com.intuitivecare.integracao.service.AnsDataService;
import com.intuitivecare.integracao.service.ConsolidacaoService;
import com.intuitivecare.integracao.service.DownloadService;
import com.intuitivecare.integracao.service.ProcessamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Aplicação principal do Desafio 1 - Integração com API Pública ANS
 * 
 * Responsável por orquestrar o fluxo de:
 * 1. Identificação dos últimos 3 trimestres disponíveis
 * 2. Download dos arquivos ZIP
 * 3. Extração e processamento dos dados
 * 4. Consolidação em CSV final
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("=== Iniciando Desafio 1 - Integração com API Pública ANS ===");
        
        try {
            ApplicationConfig config = new ApplicationConfig();
            
            // Serviço para identificar trimestres disponíveis
            AnsDataService ansDataService = new AnsDataService(config);
            
            // Serviço para download de arquivos
            DownloadService downloadService = new DownloadService(config);
            
            // Serviço para processamento de arquivos
            ProcessamentoService processamentoService = new ProcessamentoService(config);
            
            // Serviço para consolidação final
            ConsolidacaoService consolidacaoService = new ConsolidacaoService(config);
            
            // 1.1 - Identificar últimos 3 trimestres disponíveis
            logger.info("Etapa 1.1: Identificando últimos 3 trimestres disponíveis...");
            List<String> trimestres = ansDataService.identificarUltimosTrimestres(3);
            logger.info("Trimestres identificados: {}", trimestres);
            
            // 1.2 - Download e extração dos arquivos
            logger.info("Etapa 1.2: Baixando arquivos ZIP dos trimestres...");
            List<Path> arquivosExtraidos = downloadService.baixarEExtrairTrimestres(trimestres);
            logger.info("Arquivos extraídos: {}", arquivosExtraidos.size());
            
            // 1.2.1 - Processamento dos arquivos de despesas
            logger.info("Etapa 1.2: Processando arquivos de Despesas com Eventos/Sinistros...");
            List<Path> arquivosDespesas = processamentoService.filtrarArquivosDespesas(arquivosExtraidos);
            logger.info("Arquivos de despesas encontrados: {}", arquivosDespesas.size());
            
            // 1.3 - Consolidação e análise
            logger.info("Etapa 1.3: Consolidando dados e gerando CSV final...");
            Path csvConsolidado = consolidacaoService.consolidarDados(arquivosDespesas);
            
            // Compactar CSV final
            Path zipFinal = consolidacaoService.compactarCsv(csvConsolidado);
            
            logger.info("=== Desafio 1 concluído com sucesso! ===");
            logger.info("Arquivo gerado: {}", zipFinal.toAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Erro durante a execução do Desafio 1: {}", e.getMessage(), e);
            System.exit(1);
        } // try-catch
    } // main
} // Application
