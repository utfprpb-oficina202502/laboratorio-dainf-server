package br.com.utfpr.gerenciamento.server.util;

import br.com.utfpr.gerenciamento.server.annotation.FullNameValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FullNameValidatorTest {
    private final FullNameValidator.FullNameValidatorImpl validator = new FullNameValidator.FullNameValidatorImpl();

    @Test
    void validFullNameShouldBeValid() {
        assertTrue(validator.isValid("João Silva", null));
        assertTrue(validator.isValid("Maria dos Santos", null));
        assertTrue(validator.isValid("José da Silva", null));
        assertTrue(validator.isValid("Ana Maria Braga", null));
        assertTrue(validator.isValid("Carlos Alberto de Nobrega", null));
        assertTrue(validator.isValid("João O'Conner", null));
        assertTrue(validator.isValid("Maria-José Silva", null));
        assertTrue(validator.isValid("Pedro Álvarez", null));
    }

    @Test
    void validFullNameWithPrefixAndSuffixShouldBeValid() {
        assertTrue(validator.isValid("João Silva Jr.", null));
        assertTrue(validator.isValid("Carlos Santos II", null));
        assertTrue(validator.isValid("Maria van der Berg", null));
        assertTrue(validator.isValid("Luiz Mac Donald", null));
    }

    @Test
    void invalidFullNameShouldFail() {
        assertFalse(validator.isValid("joão", null));
        assertFalse(validator.isValid("JOAO SILVA", null));
        assertFalse(validator.isValid("joão silva", null));
        assertFalse(validator.isValid("123 Silva", null));
        assertFalse(validator.isValid("João@Silva", null));
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("   ", null));
        assertFalse(validator.isValid("A", null));
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void validFullNameWithSpecialCharactersShouldBeValid() {
        assertTrue(validator.isValid("François D'Ávila", null));
        assertTrue(validator.isValid("Ñato López", null));
        assertTrue(validator.isValid("Érica Müller", null));
        assertTrue(validator.isValid("Ítalo Gonçalves", null));
    }
}
