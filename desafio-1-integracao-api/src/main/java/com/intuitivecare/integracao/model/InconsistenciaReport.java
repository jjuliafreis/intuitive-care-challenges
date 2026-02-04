package com.intuitivecare.integracao.model;

/**
 * Resultado do processamento de inconsistências
 * Usado para análise e documentação das decisões tomadas
 */
public class InconsistenciaReport {

    private int cnpjsDuplicadosRazoesDiferentes;
    private int valoresZerados;
    private int valoresNegativos;
    private int trimestresFormatoInconsistente;
    private int registrosProcessados;
    private int registrosDescartados;

    public InconsistenciaReport() {
        this.cnpjsDuplicadosRazoesDiferentes = 0;
        this.valoresZerados = 0;
        this.valoresNegativos = 0;
        this.trimestresFormatoInconsistente = 0;
        this.registrosProcessados = 0;
        this.registrosDescartados = 0;
    } // InconsistenciaReport

    public void incrementarCnpjsDuplicados() {
        this.cnpjsDuplicadosRazoesDiferentes++;
    } // incrementarCnpjsDuplicados

    public void incrementarValoresZerados() {
        this.valoresZerados++;
    } // incrementarValoresZerados

    public void incrementarValoresNegativos() {
        this.valoresNegativos++;
    } // incrementarValoresNegativos

    public void incrementarTrimestresInconsistentes() {
        this.trimestresFormatoInconsistente++;
    } // incrementarTrimestresInconsistentes

    public void incrementarProcessados() {
        this.registrosProcessados++;
    } // incrementarProcessados

    public void incrementarDescartados() {
        this.registrosDescartados++;
    } // incrementarDescartados

    // Getters
    public int getCnpjsDuplicadosRazoesDiferentes() {
        return cnpjsDuplicadosRazoesDiferentes;
    } // getCnpjsDuplicadosRazoesDiferentes

    public int getValoresZerados() {
        return valoresZerados;
    } // getValoresZerados

    public int getValoresNegativos() {
        return valoresNegativos;
    } // getValoresNegativos

    public int getTrimestresFormatoInconsistente() {
        return trimestresFormatoInconsistente;
    } // getTrimestresFormatoInconsistente

    public int getRegistrosProcessados() {
        return registrosProcessados;
    } // getRegistrosProcessados

    public int getRegistrosDescartados() {
        return registrosDescartados;
    } // getRegistrosDescartados

    @Override
    public String toString() {
        return String.format("""
            === Relatório de Inconsistências ===
            Registros processados: %d
            Registros descartados: %d
            CNPJs duplicados com razões diferentes: %d
            Valores zerados: %d
            Valores negativos: %d
            Trimestres com formato inconsistente: %d
            =====================================
            """,
            registrosProcessados,
            registrosDescartados,
            cnpjsDuplicadosRazoesDiferentes,
            valoresZerados,
            valoresNegativos,
            trimestresFormatoInconsistente);
    } // toString
} // InconsistenciaReport
