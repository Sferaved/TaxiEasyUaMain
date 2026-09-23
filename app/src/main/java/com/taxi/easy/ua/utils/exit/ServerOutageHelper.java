package com.taxi.easy.ua.utils.exit;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Чистая проверка: процесс экрана сбоя и ошибка «сервер недоступен».
 */
public final class ServerOutageHelper {

    public static final String ANR_PROCESS_SUFFIX = ":anr";
    public static final int CONNECT_TIMEOUT_MS = 2500;
    public static final int READ_TIMEOUT_MS = 2500;
    public static final int WAIT_SECONDS = 4;

    private static final int MAX_CAUSE_DEPTH = 8;

    private ServerOutageHelper() {
    }

    public static boolean isAnrProcessName(String processName) {
        return processName != null && processName.endsWith(ANR_PROCESS_SUFFIX);
    }

    public static boolean isServerUnreachable(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current instanceof UnknownHostException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof NoRouteToHostException
                    || current instanceof PortUnreachableException) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
}
