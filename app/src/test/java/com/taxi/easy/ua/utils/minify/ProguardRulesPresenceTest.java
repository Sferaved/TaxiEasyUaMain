package com.taxi.easy.ua.utils.minify;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Guards release R8 keep-rules needed for Play DEX obfuscation &gt;= 25%.
 */
public class ProguardRulesPresenceTest {

    @Test
    public void releaseRules_enableOptimizationAndKeepReflectionApis() throws IOException {
        String text = readRules();

        assertFalse("Play rejects -dontobfuscate for DEX obfuscation score",
                containsDirective(text, "-dontobfuscate"));
        assertFalse("Play rejects -dontoptimize for DEX optimization score",
                containsDirective(text, "-dontoptimize"));
        assertFalse("Play rejects -dontshrink for DEX shrinking score",
                containsDirective(text, "-dontshrink"));

        assertTrue(text.contains("-keepattributes Signature"));
        assertTrue(text.contains("SourceFile,LineNumberTable"));
        assertTrue(text.contains("@com.google.gson.annotations.SerializedName"));
        assertTrue(text.contains("retrofit2.http.*"));
        assertTrue(text.contains("-keep,allowobfuscation,allowshrinking class retrofit2.Response"));
        assertTrue(text.contains("-keep,allowobfuscation class com.taxi.easy.ua.**"));
        assertTrue("Gson needs original field names (not allowobfuscation on <fields>)",
                text.contains("-keepclassmembers class com.taxi.easy.ua.**"));
        assertTrue(text.contains("org.greenrobot.eventbus.Subscribe"));
        assertTrue(text.contains("@androidx.room.Entity"));
        assertTrue(text.contains("androidx.work.ListenableWorker"));
        assertTrue(text.contains("com.bumptech.glide"));
        assertTrue(text.contains("im.crisp"));
        assertTrue(text.contains("com.uxcam"));
        assertFalse("Do not keep all of Play Services — that tanks obfuscation %",
                text.contains("-keep class com.google.android.gms."));
    }

    private static boolean containsDirective(String text, String directive) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.equals(directive) || trimmed.startsWith(directive + " ")) {
                return true;
            }
        }
        return false;
    }

    private static String readRules() throws IOException {
        File[] candidates = {
                new File("proguard-rules.pro"),
                new File("app/proguard-rules.pro"),
                new File("../proguard-rules.pro")
        };
        for (File file : candidates) {
            if (file.isFile()) {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
        }
        fail("proguard-rules.pro not found; cwd=" + new File(".").getAbsolutePath());
        return "";
    }
}
