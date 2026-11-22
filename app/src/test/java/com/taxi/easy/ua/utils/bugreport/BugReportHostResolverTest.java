package com.taxi.easy.ua.utils.bugreport;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BugReportHostResolverTest {

    private static final class HostA {
    }

    private static final class HostB {
    }

    @Test
    public void resolve_prefersPrimaryWhenMatching() {
        HostA primary = new HostA();
        HostA secondary = new HostA();

        assertSame(primary, BugReportHostResolver.resolve(primary, secondary, HostA.class));
    }

    @Test
    public void resolve_fallsBackToSecondary() {
        HostA secondary = new HostA();

        assertSame(secondary, BugReportHostResolver.resolve(new HostB(), secondary, HostA.class));
        assertSame(secondary, BugReportHostResolver.resolve(null, secondary, HostA.class));
    }

    @Test
    public void resolve_returnsNullWhenNeitherMatches() {
        assertNull(BugReportHostResolver.resolve(new HostB(), new HostB(), HostA.class));
        assertNull(BugReportHostResolver.resolve(null, null, HostA.class));
        assertNull(BugReportHostResolver.resolve(new HostA(), new HostA(), null));
    }
}
