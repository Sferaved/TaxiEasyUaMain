package com.taxi.easy.ua.utils.bugreport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrashScreenReportTextTest {

    @Test
    public void problemSummary_usesScreenMessage() {
        assertEquals("Упс... Щось пішло не так", CrashScreenReportText.problemSummary("  Упс... Щось пішло не так  "));
    }

    @Test
    public void problemSummary_blankBecomesDefault() {
        assertEquals("Crash screen", CrashScreenReportText.problemSummary("  "));
        assertEquals("Crash screen", CrashScreenReportText.problemSummary(null));
    }

    @Test
    public void fullDescription_includesDeviceReportAndLastCrash() {
        String description = CrashScreenReportText.fullDescription(
                "Щось пішло не так",
                "device block",
                "java.lang.IllegalStateException"
        );

        assertTrue(description.startsWith("Щось пішло не так"));
        assertTrue(description.contains("device block"));
        assertTrue(description.contains("Last crash:\njava.lang.IllegalStateException"));
    }

    @Test
    public void fullDescription_omitsBlankLastCrash() {
        String description = CrashScreenReportText.fullDescription("Щось пішло не так", "device block", "  ");

        assertTrue(description.contains("device block"));
        assertFalse(description.contains("Last crash:"));
    }
}
