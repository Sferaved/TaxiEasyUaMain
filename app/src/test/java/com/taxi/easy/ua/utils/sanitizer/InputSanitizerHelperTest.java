package com.taxi.easy.ua.utils.sanitizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputSanitizerHelperTest {

    @Test
    public void sanitize_phone_keepsAllowedCharacters() {
        assertEquals(
                "+38 (050) 123-45-67",
                InputSanitizerHelper.sanitize("+38 (050) 123-45-67<script>", InputSanitizerHelper.InputType.PHONE)
        );
    }

    @Test
    public void sanitize_email_lowercasesAndStripsInvalid() {
        assertEquals(
                "user@example.com",
                InputSanitizerHelper.sanitize("User@Example.com!", InputSanitizerHelper.InputType.EMAIL)
        );
    }

    @Test
    public void containsSqlInjectionPatterns_detectsSuspiciousInput() {
        assertTrue(InputSanitizerHelper.containsSqlInjectionPatterns("'; DROP TABLE users--"));
        assertTrue(InputSanitizerHelper.containsSqlInjectionPatterns("1' OR '1'='1"));
        assertFalse(InputSanitizerHelper.containsSqlInjectionPatterns("вул. Хрещатик, 1"));
    }

    @Test
    public void isValidEmail_acceptsCommonFormats() {
        assertTrue(InputSanitizerHelper.isValidEmail("user@example.com"));
        assertFalse(InputSanitizerHelper.isValidEmail("not-an-email"));
        assertFalse(InputSanitizerHelper.isValidEmail(""));
    }

    @Test
    public void isValidUsername_enforcesLengthAndCharset() {
        assertTrue(InputSanitizerHelper.isValidUsername("Іван Петренко"));
        assertFalse(InputSanitizerHelper.isValidUsername("A"));
        assertFalse(InputSanitizerHelper.isValidUsername("name<script>"));
    }

    @Test
    public void escapeSql_doublesSingleQuotes() {
        assertEquals("O''Brien", InputSanitizerHelper.escapeSql("O'Brien"));
    }

    @Test
    public void stripHtml_removesTags() {
        assertEquals("Hello", InputSanitizerHelper.stripHtml("<b>Hello</b>"));
    }

    @Test
    public void truncate_limitsLength() {
        assertEquals("12345", InputSanitizerHelper.truncate("123456789", 5));
    }
}
