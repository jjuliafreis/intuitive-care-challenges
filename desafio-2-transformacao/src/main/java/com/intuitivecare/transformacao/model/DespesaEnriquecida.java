package com.intuitivecare.transformacao.model;

import java.math.BigDecimal;

/**
 * Modelo de despesa validada e enriquecida
 */
public class DespesaEnriquecida {

    private String cnpj;
    private String razaoSocial;
    private String trimestre;
    private Integer ano;
    private BigDecimal valorDespesas;
    
    // Campos do enriquecimento
    private String registroANS;
    private String modalidade;
    private String uf;
    
    // Flags de validação
    private boolean cnpjValido;
    private boolean valorValido;
    private boolean matchCadastro;

    // Getters e Setters
    public String getCnpj() {
        return cnpj;
    } // getCnpj

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    } // setCnpj

    public String getRazaoSocial() {
        return razaoSocial;
    } // getRazaoSocial

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    } // setRazaoSocial

    public String getTrimestre() {
        return trimestre;
    } // getTrimestre

    public void setTrimestre(String trimestre) {
        this.trimestre = trimestre;
    } // setTrimestre

    public Integer getAno() {
        return ano;
    } // getAno

    public void setAno(Integer ano) {
        this.ano = ano;
    } // setAno

    public BigDecimal getValorDespesas() {
        return valorDespesas;
    } // getValorDespesas

    public void setValorDespesas(BigDecimal valorDespesas) {
        this.valorDespesas = valorDespesas;
    } // setValorDespesas

    public String getRegistroANS() {
        return registroANS;
    } // getRegistroANS

    public void setRegistroANS(String registroANS) {
        this.registroANS = registroANS;
    } // setRegistroANS

    public String getModalidade() {
        return modalidade;
    } // getModalidade

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    } // setModalidade

    public String getUf() {
        return uf;
    } // getUf

    public void setUf(String uf) {
        this.uf = uf;
    } // setUf

    public boolean isCnpjValido() {
        return cnpjValido;
    } // isCnpjValido

    public void setCnpjValido(boolean cnpjValido) {
        this.cnpjValido = cnpjValido;
    } // setCnpjValido

    public boolean isValorValido() {
        return valorValido;
    } // isValorValido

    public void setValorValido(boolean valorValido) {
        this.valorValido = valorValido;
    } // setValorValido

    public boolean isMatchCadastro() {
        return matchCadastro;
    } // isMatchCadastro

    public void setMatchCadastro(boolean matchCadastro) {
        this.matchCadastro = matchCadastro;
    } // setMatchCadastro

    /**
     * Chave composta para agregação
     */
    public String getChaveAgregacao() {
        return razaoSocial + "|" + (uf != null ? uf : "N/A");
    } // getChaveAgregacao
} // DespesaEnriquecida
