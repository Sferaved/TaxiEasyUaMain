package com.taxi.easy.ua.utils.orders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RequiredTimeParseHelperTest {

    @Test
    public void formatForStorage_nullOrEmpty_returnsEmpty() {
        assertEquals("", RequiredTimeParseHelper.formatForStorage(null));
        assertEquals("", RequiredTimeParseHelper.formatForStorage(""));
        assertEquals("", RequiredTimeParseHelper.formatForStorage("   "));
    }

    @Test
    public void formatForStorage_epochPlaceholder_returnsEmpty() {
        assertEquals("", RequiredTimeParseHelper.formatForStorage("01.01.1970 00:00"));
        assertEquals("", RequiredTimeParseHelper.formatForStorage("1970-01-01T00:00"));
    }

    @Test
    public void formatForStorage_validApiDateTime_returnsIsoLocalDateTime() {
        assertEquals("2026-07-07T21:47", RequiredTimeParseHelper.formatForStorage("07.07.2026 21:47"));
        assertEquals("2025-02-10T14:10", RequiredTimeParseHelper.formatForStorage("10.02.2025 14:10"));
    }

    @Test
    public void formatForStorage_textWithEmbeddedDateTime_extractsFirstMatch() {
        assertEquals(
                "2026-07-07T21:47",
                RequiredTimeParseHelper.formatForStorage(" замовлення на 07.07.2026 21:47")
        );
    }

    @Test
    public void formatForStorage_unrecognizedFormat_returnsEmpty() {
        assertEquals("", RequiredTimeParseHelper.formatForStorage("soon"));
        assertEquals("", RequiredTimeParseHelper.formatForStorage("2026-07-07 21:47"));
    }
}
