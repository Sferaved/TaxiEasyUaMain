package com.taxi.easy.ua.utils.bugreport.mantis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Копия ключа Mantis в файле приложения. Экран ошибки работает в отдельном процессе
 * и не открывает Firestore: тот уже занят основным процессом.
 */
public final class MantisConfigStore {

    static final String FILE_NAME = "mantis_config.cfg";

    private MantisConfigStore() {
    }

    public static void write(@Nullable File filesDir, @NonNull MantisConfig config) throws IOException {
        if (filesDir == null) {
            throw new IOException("files dir is null");
        }
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IOException("files dir was not created");
        }
        File target = new File(filesDir, FILE_NAME);
        File tmp = new File(filesDir, FILE_NAME + ".tmp");
        byte[] bytes = encode(config).getBytes(StandardCharsets.UTF_8);
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(bytes);
            out.flush();
            try {
                out.getFD().sync();
            } catch (IOException ignored) {
            }
        } finally {
            out.close();
        }
        if (target.exists() && !target.delete()) {
            throw new IOException("old mantis config was not replaced");
        }
        if (!tmp.renameTo(target)) {
            throw new IOException("mantis config was not saved");
        }
    }

    @Nullable
    public static MantisConfig read(@Nullable File filesDir) {
        if (filesDir == null) {
            return null;
        }
        File target = new File(filesDir, FILE_NAME);
        if (!target.isFile() || target.length() == 0) {
            return null;
        }
        try {
            byte[] bytes = new byte[(int) target.length()];
            FileInputStream in = new FileInputStream(target);
            try {
                int read = in.read(bytes);
                if (read != bytes.length) {
                    return null;
                }
            } finally {
                in.close();
            }
            return decode(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    static String encode(@NonNull MantisConfig config) {
        return "1\n"
                + config.projectId + "\n"
                + config.categoryId + "\n"
                + escape(config.baseUrl) + "\n"
                + escape(config.apiToken) + "\n";
    }

    @Nullable
    static MantisConfig decode(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String[] lines = text.split("\n", -1);
        if (lines.length < 5 || !"1".equals(lines[0])) {
            return null;
        }
        try {
            int projectId = Integer.parseInt(lines[1].trim());
            int categoryId = Integer.parseInt(lines[2].trim());
            String baseUrl = unescape(lines[3]);
            String apiToken = unescape(lines[4]);
            if (baseUrl.isEmpty() || apiToken.isEmpty()) {
                return null;
            }
            return new MantisConfig(apiToken, baseUrl, projectId, categoryId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @NonNull
    private static String escape(@NonNull String value) {
        return value.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    @NonNull
    private static String unescape(@NonNull String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                if (next == '\\') {
                    out.append('\\');
                    i++;
                } else if (next == 'n') {
                    out.append('\n');
                    i++;
                } else if (next == 'r') {
                    out.append('\r');
                    i++;
                } else {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
