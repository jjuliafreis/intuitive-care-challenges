package com.intuitivecare.integracao.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Modelo representando um registro de despesa consolidado
 * Imutável para garantir thread-safety e integridade dos dados
 */
public class DespesaConsolidada {

    private final String cnpj;
    private final String razaoSocial;
    private final String trimestre;
    private final Integer ano;
    private final BigDecimal valorDespesas;
    private final String arquivoOrigem;
    private final boolean valorSuspeito;
    private final String motivoSuspeita;

    private DespesaConsolidada(Builder builder) {
        this.cnpj = builder.cnpj;
        this.razaoSocial = builder.razaoSocial;
        this.trimestre = builder.trimestre;
        this.ano = builder.ano;
        this.valorDespesas = builder.valorDespesas;
        this.arquivoOrigem = builder.arquivoOrigem;
        this.valorSuspeito = builder.valorSuspeito;
        this.motivoSuspeita = builder.motivoSuspeita;
    } // DespesaConsolidada

    // Getters
    public String getCnpj() {
        return cnpj;
    } // getCnpj

    public String getRazaoSocial() {
        return razaoSocial;
    } // getRazaoSocial

    public String getTrimestre() {
        return trimestre;
    } // getTrimestre

    public Integer getAno() {
        return ano;
    } // getAno

    public BigDecimal getValorDespesas() {
        return valorDespesas;
    } // getValorDespesas

    public String getArquivoOrigem() {
        return arquivoOrigem;
    } // getArquivoOrigem

    public boolean isValorSuspeito() {
        return valorSuspeito;
    } // isValorSuspeito

    public String getMotivoSuspeita() {
        return motivoSuspeita;
    } // getMotivoSuspeita

    /**
     * Retorna CNPJ formatado apenas com números
     */
    public String getCnpjNumerico() {
        if (cnpj == null) return "";
        return cnpj.replaceAll("[^0-9]", "");
    } // getCnpjNumerico

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DespesaConsolidada that = (DespesaConsolidada) o;
        return Objects.equals(getCnpjNumerico(), that.getCnpjNumerico()) &&
               Objects.equals(trimestre, that.trimestre) &&
               Objects.equals(ano, that.ano);
    } // equals

    @Override
    public int hashCode() {
        return Objects.hash(getCnpjNumerico(), trimestre, ano);
    } // hashCode

    @Override
    public String toString() {
        return String.format("DespesaConsolidada{cnpj='%s', razaoSocial='%s', trimestre='%s', ano=%d, valor=%s}",
                cnpj, razaoSocial, trimestre, ano, valorDespesas);
    } // toString

    /**
     * Builder Pattern para construção flexível
     */
    public static class Builder {
        private String cnpj;
        private String razaoSocial;
        private String trimestre;
        private Integer ano;
        private BigDecimal valorDespesas;
        private String arquivoOrigem;
        private boolean valorSuspeito = false;
        private String motivoSuspeita;

        public Builder cnpj(String cnpj) {
            this.cnpj = cnpj;
            return this;
        } // cnpj

        public Builder razaoSocial(String razaoSocial) {
            this.razaoSocial = razaoSocial;
            return this;
        } // razaoSocial

        public Builder trimestre(String trimestre) {
            this.trimestre = trimestre;
            return this;
        } // trimestre

        public Builder ano(Integer ano) {
            this.ano = ano;
            return this;
        } // ano

        public Builder valorDespesas(BigDecimal valorDespesas) {
            this.valorDespesas = valorDespesas;
            return this;
        } // valorDespesas

        public Builder arquivoOrigem(String arquivoOrigem) {
            this.arquivoOrigem = arquivoOrigem;
            return this;
        } // arquivoOrigem

        public Builder valorSuspeito(boolean valorSuspeito) {
            this.valorSuspeito = valorSuspeito;
            return this;
        } // valorSuspeito

        public Builder motivoSuspeita(String motivoSuspeita) {
            this.motivoSuspeita = motivoSuspeita;
            return this;
        } // motivoSuspeita

        public DespesaConsolidada build() {
            return new DespesaConsolidada(this);
        } // build
    } // Builder

    public static Builder builder() {
        return new Builder();
    } // builder
} // DespesaConsolidada
