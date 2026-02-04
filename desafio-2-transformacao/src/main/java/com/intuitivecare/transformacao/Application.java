package com.intuitivecare.transformacao;

import com.intuitivecare.transformacao.config.ApplicationConfig;
import com.intuitivecare.transformacao.service.AgregacaoService;
import com.intuitivecare.transformacao.service.EnriquecimentoService;
import com.intuitivecare.transformacao.service.ValidacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Aplicação principal do Desafio 2 - Transformação e Validação de Dados
 * 
 * Responsável por orquestrar:
 * 1. Validação de dados do CSV consolidado
 * 2. Enriquecimento com dados cadastrais
 * 3. Agregação e geração de estatísticas
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("=== Iniciando Desafio 2 - Transformação e Validação de Dados ===");
        
        try {
            ApplicationConfig config = new ApplicationConfig();
            
            // Serviços
            ValidacaoService validacaoService = new ValidacaoService(config);
            EnriquecimentoService enriquecimentoService = new EnriquecimentoService(config);
            AgregacaoService agregacaoService = new AgregacaoService(config);
            
            // 2.1 - Validação de Dados
            logger.info("Etapa 2.1: Validando dados do CSV consolidado...");
            Path csvValidado = validacaoService.validarDados(config.getCsvConsolidado());
            logger.info("Validação concluída: {}", csvValidado);
            
            // 2.2 - Enriquecimento com Dados Cadastrais
            logger.info("Etapa 2.2: Enriquecendo dados com cadastro de operadoras...");
            Path csvEnriquecido = enriquecimentoService.enriquecerDados(csvValidado);
            logger.info("Enriquecimento concluído: {}", csvEnriquecido);
            
            // 2.3 - Agregação e Estatísticas
            logger.info("Etapa 2.3: Agregando dados e calculando estatísticas...");
            Path csvAgregado = agregacaoService.agregarDados(csvEnriquecido);
            logger.info("Agregação concluída: {}", csvAgregado);
            
            // Compactar resultado final
            Path zipFinal = agregacaoService.compactarResultado(csvAgregado);
            
            logger.info("=== Desafio 2 concluído com sucesso! ===");
            logger.info("Arquivo gerado: {}", zipFinal.toAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Erro durante a execução do Desafio 2: {}", e.getMessage(), e);
            System.exit(1);
        } // try-catch
    } // main
} // Application
