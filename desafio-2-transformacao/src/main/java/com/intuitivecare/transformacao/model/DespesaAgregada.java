package com.intuitivecare.transformacao.model;

import java.math.BigDecimal;

/**
 * Modelo para dados agregados por operadora/UF
 */
public class DespesaAgregada {

    private String razaoSocial;
    private String uf;
    private BigDecimal totalDespesas;
    private BigDecimal mediaPorTrimestre;
    private BigDecimal desvioPadrao;
    private int quantidadeTrimestres;

    // Getters e Setters
    public String getRazaoSocial() {
        return razaoSocial;
    } // getRazaoSocial

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    } // setRazaoSocial

    public String getUf() {
        return uf;
    } // getUf

    public void setUf(String uf) {
        this.uf = uf;
    } // setUf

    public BigDecimal getTotalDespesas() {
        return totalDespesas;
    } // getTotalDespesas

    public void setTotalDespesas(BigDecimal totalDespesas) {
        this.totalDespesas = totalDespesas;
    } // setTotalDespesas

    public BigDecimal getMediaPorTrimestre() {
        return mediaPorTrimestre;
    } // getMediaPorTrimestre

    public void setMediaPorTrimestre(BigDecimal mediaPorTrimestre) {
        this.mediaPorTrimestre = mediaPorTrimestre;
    } // setMediaPorTrimestre

    public BigDecimal getDesvioPadrao() {
        return desvioPadrao;
    } // getDesvioPadrao

    public void setDesvioPadrao(BigDecimal desvioPadrao) {
        this.desvioPadrao = desvioPadrao;
    } // setDesvioPadrao

    public int getQuantidadeTrimestres() {
        return quantidadeTrimestres;
    } // getQuantidadeTrimestres

    public void setQuantidadeTrimestres(int quantidadeTrimestres) {
        this.quantidadeTrimestres = quantidadeTrimestres;
    } // setQuantidadeTrimestres
} // DespesaAgregada
