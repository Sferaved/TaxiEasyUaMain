package com.taxi.easy.ua.utils.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.taxi.easy.ua.utils.log.Logger;

import java.io.File;

public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "my_prefs";
    private static final String TAG = "SharedPreferencesHelper";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveValueAsync(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply(); // Асинхронное сохранение
    }
    // Метод для сохранения значения
    public void saveValue(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (value == null ? "null" : value.getClass().getSimpleName()) {
            case "String":
                editor.putString(key, (String) value);
                break;
            case "Integer":
                editor.putInt(key, (Integer) value);
                break;
            case "Boolean":
                editor.putBoolean(key, (Boolean) value);
                break;
            case "Float":
                editor.putFloat(key, (Float) value);
                break;
            case "Long":
                editor.putLong(key, (Long) value);
                break;
            case "null":
            default:
                throw new IllegalArgumentException("Unsupported value type");
        }


        editor.apply();
    }

    // Метод для получения значения
    public Object getValue(String key, Object defaultValue) {
        String typeName = (defaultValue == null) ? "null" : defaultValue.getClass().getSimpleName();
        switch (typeName) {
            case "String":
                return sharedPreferences.getString(key, (String) defaultValue);
            case "Integer":
                return sharedPreferences.getInt(key, (Integer) defaultValue);
            case "Boolean":
                return sharedPreferences.getBoolean(key, (Boolean) defaultValue);
            case "Float":
                return sharedPreferences.getFloat(key, (Float) defaultValue);
            case "Long":
                return sharedPreferences.getLong(key, (Long) defaultValue);
            case "null":
            default:
                throw new IllegalArgumentException("Unsupported default value type");
        }
    }


    // Метод для удаления значения
    public void removeValue(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    // Метод для проверки существования значения
    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    // Метод для очистки всех значений
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }


    // Clears all application data and restarts the app
    public static void clearApplication(Context context) {
        Logger.d(context, TAG, "Starting clearApplication");
        clearAllSharedPreferences(context);
        clearAllDatabases(context);
        clearAllCache(context);
        clearAllExternalCache(context);

        // Restart the application
        try {
            if (context instanceof Activity) {
                Logger.d(context, TAG, "Initiating application restart");
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    System.exit(0); // Terminate the current process
                } else {
                    Logger.d(context, TAG, "Could not find launch intent for package: " + context.getPackageName());
                }
            } else {
                Logger.d(context, TAG, "Context is not an Activity, cannot restart application");
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error during application restart: " + e.toString());
        }
        Logger.d(context, TAG, "Completed clearApplication");
    }

    // Clears all SharedPreferences files for the app
    public static void clearAllSharedPreferences(Context context) {
        Logger.d(context, TAG, "Starting clearAllSharedPreferences");
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        String prefsDir = context.getApplicationInfo().dataDir + "/shared_prefs";
        File dir = new File(prefsDir);

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".xml")) {
                        String prefName = file.substring(0, file.length() - 4);
                        Logger.d(context, TAG, "Clearing SharedPreferences: " + prefName);
                        try {
                            SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();
                            Logger.d(context, TAG, "Cleared SharedPreferences: " + prefName);
                        } catch (Exception e) {
                            Logger.e(context, TAG, "Error clearing SharedPreferences " + prefName + ": " + e.toString());
                        }
                    }
                }
            } else {
                Logger.d(context, TAG, "No SharedPreferences files found or unable to list files in: " + prefsDir);
            }
        } else {
            Logger.d(context, TAG, "SharedPreferences directory does not exist or is not a directory: " + prefsDir);
        }
        Logger.d(context, TAG, "Completed clearAllSharedPreferences");
    }

    // Clears all database files for the app
    public static void clearAllDatabases(Context context) {
        if (context == null) {
            Logger.e(context, TAG, "Context is null in clearAllDatabases");
            throw new IllegalArgumentException("Context cannot be null");
        }
        Logger.d(context, TAG, "Starting clearAllDatabases");
        String dbDir = context.getApplicationInfo().dataDir + "/databases";
        File dir = new File(dbDir);

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".db") || file.endsWith(".sqlite")) {
                        Logger.d(context, TAG, "Deleting database: " + file);
                        try {
                            if (context.deleteDatabase(file)) {
                                Logger.d(context, TAG, "Deleted database: " + file);
                            } else {
                                Logger.d(context, TAG, "Failed to delete database: " + file);
                            }
                        } catch (Exception e) {
                            Logger.e(context, TAG, "Error deleting database " + file + ": " + e.toString());
                        }
                    }
                }
            } else {
                Logger.d(context, TAG, "No database files found or unable to list files in: " + dbDir);
            }
        } else {
            Logger.d(context, TAG, "Database directory does not exist or is not a directory: " + dbDir);
        }
        Logger.d(context, TAG, "Completed clearAllDatabases");
    }

    // Clears all cache files for the app
    public static void clearAllCache(Context context) {
        if (context == null) {
            Logger.e(context, TAG, "Context is null in clearAllCache");
            throw new IllegalArgumentException("Context cannot be null");
        }
        Logger.d(context, TAG, "Starting clearAllCache");
        File cacheDir = context.getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            if (deleteRecursive(cacheDir, context)) {
                Logger.d(context, TAG, "Cleared internal cache directory: " + cacheDir.getAbsolutePath());
            } else {
                Logger.d(context, TAG, "Failed to clear internal cache directory: " + cacheDir.getAbsolutePath());
            }
        } else {
            Logger.d(context, TAG, "Internal cache directory is null or not a directory");
        }
        Logger.d(context, TAG, "Completed clearAllCache");
    }

    // Clears all external cache files for the app
    public static void clearAllExternalCache(Context context) {
        if (context == null) {
            Logger.e(context, TAG, "Context is null in clearAllExternalCache");
            throw new IllegalArgumentException("Context cannot be null");
        }
        Logger.d(context, TAG, "Starting clearAllExternalCache");
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null && externalCacheDir.isDirectory()) {
            if (deleteRecursive(externalCacheDir, context)) {
                Logger.d(context, TAG, "Cleared external cache directory: " + externalCacheDir.getAbsolutePath());
            } else {
                Logger.d(context, TAG, "Failed to clear external cache directory: " + externalCacheDir.getAbsolutePath());
            }
        } else {
            Logger.d(context, TAG, "External cache directory is null or not a directory");
        }
        Logger.d(context, TAG, "Completed clearAllExternalCache");
    }

    // Recursively deletes files and directories, returns true if successful
    private static boolean deleteRecursive(File fileOrDir, Context context) {
        if (fileOrDir == null) {
            Logger.d(context, TAG, "File or directory is null in deleteRecursive");
            return false;
        }
        boolean success = true;
        try {
            if (fileOrDir.isDirectory()) {
                File[] files = fileOrDir.listFiles();
                if (files != null) {
                    for (File child : files) {
                        Logger.d(context, TAG, "Attempting to delete: " + child.getAbsolutePath());
                        success &= deleteRecursive(child, context);
                    }
                } else {
                    Logger.d(context, TAG, "Unable to list files in directory: " + fileOrDir.getAbsolutePath());
                }
            }
            if (fileOrDir.delete()) {
                Logger.d(context, TAG, "Deleted: " + fileOrDir.getAbsolutePath());
            } else {
                Logger.d(context, TAG, "Failed to delete: " + fileOrDir.getAbsolutePath());
                success = false;
            }
        } catch (SecurityException e) {
            Logger.e(context, TAG, "SecurityException while deleting: " + fileOrDir.getAbsolutePath() + " " + e.toString());
            success = false;
        } catch (Exception e) {
            Logger.e(context, TAG, "Unexpected error while deleting: " + fileOrDir.getAbsolutePath() + " " + e.toString());
            success = false;
        }
        return success;
    }

}

