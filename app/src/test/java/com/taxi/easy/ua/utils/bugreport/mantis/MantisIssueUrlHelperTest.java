package com.taxi.easy.ua.utils.bugreport.mantis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MantisIssueUrlHelperTest {

    @Test
    public void buildIssueViewUrl_trimsTrailingSlash() {
        String url = MantisIssueUrlHelper.buildIssueViewUrl("https://bugs.example.com/", 42);
        assertEquals("https://bugs.example.com/view.php?id=42", url);
    }

    @Test
    public void buildIssueSummary_truncatesLongDescription() {
        String summary = MantisIssueUrlHelper.buildIssueSummary(
                "PAS_4",
                "4.1107",
                "A".repeat(200)
        );
        assertTrue(summary.length() <= 128);
        assertTrue(summary.startsWith("[PAS_4 4.1107] "));
        assertTrue(summary.endsWith("..."));
    }

    @Test
    public void buildTaxiBugReportBaseUrl_usesTaxiHostNotMantisRest() {
        assertEquals(
                "https://t.easy-order-taxi.site/",
                MantisIssueUrlHelper.buildTaxiBugReportBaseUrl("https://t.easy-order-taxi.site/")
        );
        assertEquals(
                "https://m.easy-order-taxi.site/",
                MantisIssueUrlHelper.buildTaxiBugReportBaseUrl("https://m.easy-order-taxi.site")
        );
        assertEquals("", MantisIssueUrlHelper.buildTaxiBugReportBaseUrl("  "));
        assertEquals("api/mantis/issues", MantisIssueUrlHelper.TAXI_CREATE_ISSUE_PATH);
    }
}
