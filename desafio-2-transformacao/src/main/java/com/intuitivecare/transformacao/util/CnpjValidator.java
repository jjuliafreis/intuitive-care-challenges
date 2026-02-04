package com.intuitivecare.transformacao.util;

/**
 * Utilitário para validação de CNPJ
 * Implementa validação completa com dígitos verificadores
 */
public final class CnpjValidator {

    // Pesos para cálculo do primeiro dígito verificador
    private static final int[] PESOS_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    
    // Pesos para cálculo do segundo dígito verificador
    private static final int[] PESOS_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
        // Construtor privado para classe utilitária
    } // CnpjValidator

    /**
     * Valida um CNPJ completo (formato e dígitos verificadores)
     * 
     * @param cnpj CNPJ a validar (pode conter pontuação)
     * @return true se válido, false caso contrário
     */
    public static boolean isValid(String cnpj) {
        if (cnpj == null) {
            return false;
        } // if

        // Remover caracteres não numéricos
        String cnpjNumerico = cnpj.replaceAll("[^0-9]", "");

        // Verificar tamanho
        if (cnpjNumerico.length() != 14) {
            return false;
        } // if

        // Verificar se todos os dígitos são iguais (CNPJs inválidos conhecidos)
        if (todosDigitosIguais(cnpjNumerico)) {
            return false;
        } // if

        // Calcular e verificar dígitos verificadores
        return verificarDigitos(cnpjNumerico);
    } // isValid

    /**
     * Verifica se o CNPJ tem formato válido (independente dos dígitos verificadores)
     */
    public static boolean hasValidFormat(String cnpj) {
        if (cnpj == null) {
            return false;
        } // if

        String cnpjNumerico = cnpj.replaceAll("[^0-9]", "");
        return cnpjNumerico.length() == 14;
    } // hasValidFormat

    /**
     * Formata CNPJ para exibição (XX.XXX.XXX/XXXX-XX)
     */
    public static String format(String cnpj) {
        if (cnpj == null) {
            return "";
        } // if

        String cnpjNumerico = cnpj.replaceAll("[^0-9]", "");
        
        if (cnpjNumerico.length() != 14) {
            return cnpj; // Retorna original se não puder formatar
        } // if

        return String.format("%s.%s.%s/%s-%s",
            cnpjNumerico.substring(0, 2),
            cnpjNumerico.substring(2, 5),
            cnpjNumerico.substring(5, 8),
            cnpjNumerico.substring(8, 12),
            cnpjNumerico.substring(12, 14));
    } // format

    /**
     * Normaliza CNPJ (apenas números, com padding de zeros à esquerda)
     */
    public static String normalize(String cnpj) {
        if (cnpj == null) {
            return "";
        } // if

        String cnpjNumerico = cnpj.replaceAll("[^0-9]", "");
        
        // Adicionar zeros à esquerda se necessário
        while (cnpjNumerico.length() < 14) {
            cnpjNumerico = "0" + cnpjNumerico;
        } // while

        return cnpjNumerico;
    } // normalize

    /**
     * Verifica se todos os dígitos são iguais
     */
    private static boolean todosDigitosIguais(String cnpj) {
        char primeiro = cnpj.charAt(0);
        for (int i = 1; i < cnpj.length(); i++) {
            if (cnpj.charAt(i) != primeiro) {
                return false;
            } // if
        } // for
        return true;
    } // todosDigitosIguais

    /**
     * Verifica os dígitos verificadores do CNPJ
     */
    private static boolean verificarDigitos(String cnpj) {
        try {
            // Calcular primeiro dígito verificador
            int soma1 = 0;
            for (int i = 0; i < 12; i++) {
                soma1 += Character.getNumericValue(cnpj.charAt(i)) * PESOS_PRIMEIRO[i];
            } // for
            
            int resto1 = soma1 % 11;
            int digito1 = (resto1 < 2) ? 0 : (11 - resto1);
            
            // Verificar primeiro dígito
            if (Character.getNumericValue(cnpj.charAt(12)) != digito1) {
                return false;
            } // if

            // Calcular segundo dígito verificador
            int soma2 = 0;
            for (int i = 0; i < 13; i++) {
                soma2 += Character.getNumericValue(cnpj.charAt(i)) * PESOS_SEGUNDO[i];
            } // for
            
            int resto2 = soma2 % 11;
            int digito2 = (resto2 < 2) ? 0 : (11 - resto2);
            
            // Verificar segundo dígito
            return Character.getNumericValue(cnpj.charAt(13)) == digito2;
            
        } catch (Exception e) {
            return false;
        } // try-catch
    } // verificarDigitos
} // CnpjValidator
