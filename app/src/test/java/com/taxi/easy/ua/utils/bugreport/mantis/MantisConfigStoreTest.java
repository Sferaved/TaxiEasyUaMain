package com.taxi.easy.ua.utils.bugreport.mantis;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MantisConfigStoreTest {

    @Test
    public void writeThenRead_roundTrip() throws Exception {
        File dir = Files.createTempDirectory("mantis-config").toFile();
        MantisConfig original = new MantisConfig("token-1", "https://mantis.example/api/", 7, 3);

        MantisConfigStore.write(dir, original);
        MantisConfig loaded = MantisConfigStore.read(dir);

        assertEquals(original.apiToken, loaded.apiToken);
        assertEquals(original.baseUrl, loaded.baseUrl);
        assertEquals(original.projectId, loaded.projectId);
        assertEquals(original.categoryId, loaded.categoryId);
    }

    @Test
    public void write_replacesPreviousFile() throws Exception {
        File dir = Files.createTempDirectory("mantis-config").toFile();
        MantisConfigStore.write(dir, new MantisConfig("old-token", "https://old.example/", 1, 1));
        MantisConfigStore.write(dir, new MantisConfig("new-token", "https://new.example/", 2, 4));

        MantisConfig loaded = MantisConfigStore.read(dir);

        assertEquals("new-token", loaded.apiToken);
        assertEquals("https://new.example/", loaded.baseUrl);
        assertEquals(2, loaded.projectId);
        assertEquals(4, loaded.categoryId);
    }

    @Test
    public void read_missingOrBrokenFile_returnsNull() throws Exception {
        File dir = Files.createTempDirectory("mantis-config").toFile();
        assertNull(MantisConfigStore.read(dir));
        assertNull(MantisConfigStore.read(null));

        File broken = new File(dir, MantisConfigStore.FILE_NAME);
        Files.write(broken.toPath(), "not-a-config".getBytes());
        assertNull(MantisConfigStore.read(dir));
    }

    @Test
    public void encode_keepsTokenWithNewlineOnOneRecord() {
        MantisConfig config = new MantisConfig("ab\ncd", "https://mantis.example/", 9, 8);

        MantisConfig decoded = MantisConfigStore.decode(MantisConfigStore.encode(config));

        assertEquals("ab\ncd", decoded.apiToken);
        assertEquals(9, decoded.projectId);
        assertEquals(8, decoded.categoryId);
    }
}
