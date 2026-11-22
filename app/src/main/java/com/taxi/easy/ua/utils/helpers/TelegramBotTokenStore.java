package com.taxi.easy.ua.utils.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Копия токена info-бота. Экран «Упс» работает в отдельном процессе
 * и не открывает Firestore: тот уже занят основным процессом.
 */
public final class TelegramBotTokenStore {

    static final String FILE_NAME = "telegram_bot_token.cfg";

    private TelegramBotTokenStore() {
    }

    public static void write(@Nullable File filesDir, @NonNull String token) throws IOException {
        if (filesDir == null) {
            throw new IOException("files dir is null");
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            throw new IOException("telegram bot token is empty");
        }
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IOException("files dir was not created");
        }
        File target = new File(filesDir, FILE_NAME);
        File tmp = new File(filesDir, FILE_NAME + ".tmp");
        byte[] bytes = ("1\n" + trimmed + "\n").getBytes(StandardCharsets.UTF_8);
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
            throw new IOException("old telegram bot token was not replaced");
        }
        if (!tmp.renameTo(target)) {
            throw new IOException("telegram bot token was not saved");
        }
    }

    @Nullable
    public static String read(@Nullable File filesDir) {
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

    @Nullable
    static String decode(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String[] lines = text.split("\n", -1);
        if (lines.length < 2 || !"1".equals(lines[0])) {
            return null;
        }
        String token = lines[1].trim();
        return token.isEmpty() ? null : token;
    }
}
