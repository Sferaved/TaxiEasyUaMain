package com.taxi.easy.ua.utils.exit;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerOutageHelperTest {

    @Test
    public void anrProcessSuffix_isAnrProcess() {
        assertTrue(ServerOutageHelper.isAnrProcessName("com.taxi.easy.ua:anr"));
        assertFalse(ServerOutageHelper.isAnrProcessName("com.taxi.easy.ua"));
        assertFalse(ServerOutageHelper.isAnrProcessName(null));
    }

    @Test
    public void connectionFailures_areUnreachable() {
        assertTrue(ServerOutageHelper.isServerUnreachable(new UnknownHostException("taxi")));
        assertTrue(ServerOutageHelper.isServerUnreachable(new ConnectException("Connection refused")));
        assertTrue(ServerOutageHelper.isServerUnreachable(new SocketTimeoutException("timeout")));
    }

    @Test
    public void nestedCause_isUnreachable() {
        IOException wrapped = new IOException("request failed", new ConnectException("refused"));
        assertTrue(ServerOutageHelper.isServerUnreachable(wrapped));
    }

    @Test
    public void ordinaryIoAndRuntime_areNotOutage() {
        assertFalse(ServerOutageHelper.isServerUnreachable(new IOException("unexpected end of stream")));
        assertFalse(ServerOutageHelper.isServerUnreachable(new RuntimeException("parse")));
        assertFalse(ServerOutageHelper.isServerUnreachable(null));
    }
}
