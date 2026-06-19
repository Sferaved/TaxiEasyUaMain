package com.taxi.easy.ua.utils.phone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.utils.sanitizer.InputSanitizerHelper;

import org.junit.Test;

/**
 * Regression tests for phone submit flow in AccountFragment and MyPhoneDialogFragment:
 * normalize → sanitize → isValid → SQL pattern check.
 */
public class PhoneInputValidationChainTest {

    private static String normalizeAndSanitize(String rawPhone) {
        String normalized = PhoneNumberHelper.normalizeAndFormat(rawPhone);
        return InputSanitizerHelper.sanitize(normalized, InputSanitizerHelper.InputType.PHONE);
    }

    private static boolean isSubmittablePhone(String rawPhone) {
        String safePhone = normalizeAndSanitize(rawPhone);
        return PhoneNumberHelper.isValid(safePhone)
                && !InputSanitizerHelper.containsSqlInjectionPatterns(safePhone);
    }

    @Test
    public void chain_acceptsValidFormattedPhone() {
        assertTrue(isSubmittablePhone("+38 093 666 55 44"));
        assertEquals("+38 093 666 55 44", normalizeAndSanitize("+38 093 666 55 44"));
    }

    @Test
    public void chain_acceptsDigitsOnlyInput() {
        assertTrue(isSubmittablePhone("380936665544"));
        assertEquals("+38 093 666 55 44", normalizeAndSanitize("380936665544"));
    }

    @Test
    public void chain_stripsDisallowedCharactersBeforeValidation() {
        assertTrue(isSubmittablePhone("+38 (093) 666-55-44<script>"));
        assertEquals("+38 093 666 55 44", normalizeAndSanitize("+38 (093) 666-55-44<script>"));
    }

    @Test
    public void chain_rejectsDuplicate380Prefix() {
        assertFalse(isSubmittablePhone("+38 380 931 23 45"));
    }

    @Test
    public void chain_rejectsIncompleteNumber() {
        assertFalse(isSubmittablePhone("+38 093 666"));
        assertFalse(isSubmittablePhone(""));
    }

    @Test
    public void chain_rejectsSqlInjectionOnlyInput() {
        assertFalse(isSubmittablePhone("'; DROP TABLE users--"));
    }

    @Test
    public void chain_fixes380WhileTypingThenValidates() {
        String safe = normalizeAndSanitize("+38 380 939 54 65");
        assertEquals("+38 093 954 65", safe);
        assertFalse(PhoneNumberHelper.isValid(safe));
    }
}
