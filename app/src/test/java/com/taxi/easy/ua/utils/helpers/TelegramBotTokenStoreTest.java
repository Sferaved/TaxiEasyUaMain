package com.taxi.easy.ua.utils.helpers;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TelegramBotTokenStoreTest {

    @Test
    public void writeThenRead_roundTrip() throws Exception {
        File dir = Files.createTempDirectory("tg-bot-token").toFile();

        TelegramBotTokenStore.write(dir, " 123:ABC ");
        assertEquals("123:ABC", TelegramBotTokenStore.read(dir));
    }

    @Test
    public void write_replacesPreviousFile() throws Exception {
        File dir = Files.createTempDirectory("tg-bot-token").toFile();
        TelegramBotTokenStore.write(dir, "old-token");
        TelegramBotTokenStore.write(dir, "new-token");

        assertEquals("new-token", TelegramBotTokenStore.read(dir));
    }

    @Test
    public void read_missingOrBrokenFile_returnsNull() throws Exception {
        File dir = Files.createTempDirectory("tg-bot-token").toFile();
        assertNull(TelegramBotTokenStore.read(dir));
        assertNull(TelegramBotTokenStore.read(null));

        File broken = new File(dir, TelegramBotTokenStore.FILE_NAME);
        Files.write(broken.toPath(), "not-a-token".getBytes());
        assertNull(TelegramBotTokenStore.read(dir));
    }
}
