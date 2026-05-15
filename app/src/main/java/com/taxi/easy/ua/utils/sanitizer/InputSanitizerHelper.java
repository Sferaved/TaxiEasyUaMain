package com.taxi.easy.ua.utils.sanitizer;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.regex.Pattern;

/**
 * Универсальный хелпер для санитизации пользовательского ввода
 * Предотвращает инъекции и нежелательные спецсимволы
 */
public class InputSanitizerHelper {

    // Предопределенные типы валидации
    public enum InputType {
        USERNAME,      // Только буквы, цифры, пробелы, дефис, апостроф, точка
        PHONE,         // Только +, цифры, пробелы, скобки, дефис
        EMAIL,         // Email формат с базовой санитизацией
        ADDRESS,       // Адрес: буквы, цифры, пробелы, знаки препинания, дефис, слеш
        COMMENT,       // Комментарий: разрешены буквы, цифры, пробелы, базовая пунктуация
        NUMERIC,       // Только цифры
        ALPHANUMERIC,  // Только буквы и цифры (без пробелов)
        TEXT_WITH_SPECIAL, // Полный текст с ограниченными спецсимволами
        LATIN_CYRILLIC // Буквы латиницы и кириллицы, пробелы, дефис
    }

    /**
     * Основной метод санитизации строки
     * @param input Входная строка
     * @param type Тип валидации
     * @return Очищенная строка
     */
    public static String sanitize(String input, InputType type) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        switch (type) {
            case USERNAME:
                // Имя пользователя: буквы (любые языки), цифры, пробелы, точка, дефис, апостроф
                return input.replaceAll("[^\\p{L}\\p{N}\\s\\.\\-']", "").trim();

            case PHONE:
                // Телефон: +, цифры, пробелы, скобки, дефис
                return input.replaceAll("[^+\\d\\s\\(\\)\\-]", "").trim();

            case EMAIL:
                // Email: разрешены буквы, цифры, точки, дефис, подчеркивание, плюс, @
                String cleaned = input.replaceAll("[^a-zA-Z0-9.@_\\-+]", "");
                // Дополнительная проверка на multiple @
                if (cleaned.chars().filter(ch -> ch == '@').count() > 1) {
                    cleaned = cleaned.replaceAll("@.*", "@");
                }
                return cleaned.toLowerCase().trim();

            case ADDRESS:
                // Адрес: буквы, цифры, пробелы, точка, запятая, дефис, слеш, скобки, номер дома
                return input.replaceAll("[^\\p{L}\\p{N}\\s\\.,\\-/№#\\(\\)]", "").trim();

            case COMMENT:
                // Комментарий: буквы, цифры, пробелы, базовая пунктуация
                return input.replaceAll("[^\\p{L}\\p{N}\\s\\.,!?\\-:;\"'()]", "").trim();

            case NUMERIC:
                // Только цифры
                return input.replaceAll("[^\\d]", "");

            case ALPHANUMERIC:
                // Только буквы и цифры (без пробелов)
                return input.replaceAll("[^\\p{L}\\p{N}]", "");

            case TEXT_WITH_SPECIAL:
                // Расширенный текст: буквы, цифры, пробелы, базовая пунктуация, @, #, $, %, &, *, +, =, <, >
                return input.replaceAll("[^\\p{L}\\p{N}\\s\\.,!?@#$%&*+=<>\\-:;\"'()/]", "").trim();

            case LATIN_CYRILLIC:
                // Только латиница, кириллица, пробелы, дефис
                return input.replaceAll("[^a-zA-Zа-яА-ЯёЁ\\s\\-]", "").trim();

            default:
                return input.replaceAll("[^\\p{L}\\p{N}\\s]", "").trim();
        }
    }

    /**
     * Проверка на наличие потенциально опасных SQL-инъекций
     * @param input Входная строка
     * @return true если обнаружены подозрительные паттерны
     */
    public static boolean containsSqlInjectionPatterns(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        // Паттерны SQL-инъекций
        String[] sqlPatterns = {
                "select.*from", "insert.*into", "update.*set", "delete.*from",
                "drop.*table", "create.*table", "alter.*table", "union.*select",
                "--", ";.*--", "'.*or.*1=1", "\".*or.*1=1", "'.*or.*'1'='1",
                "'.*or.*'1'='1'", "\".*or.*\"1\"=\"1", "exec.*xp_", "execute.*",
                "script", "javascript:", "onclick=", "onload=", "alert\\("
        };

        for (String pattern : sqlPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(lowerInput).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Экранирование спецсимволов для SQL запросов
     * @param input Входная строка
     * @return Экранированная строка
     */
    public static String escapeSql(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "''")
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Санитизация HTML тегов
     * @param input Входная строка
     * @return Строка без HTML тегов
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Безопасная обрезка строки с указанием максимальной длины
     * @param input Входная строка
     * @param maxLength Максимальная длина
     * @return Обрезанная строка
     */
    public static String truncate(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength).trim();
    }

    /**
     * Создает TextWatcher для автоматической санитизации EditText
     * @param editText Поле ввода
     * @param inputType Тип валидации
     * @return TextWatcher
     */
    public static TextWatcher createSanitizingTextWatcher(EditText editText, InputType inputType) {
        return new TextWatcher() {
            private String lastSanitized = "";
            private boolean isSanitizing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isSanitizing) return;

                String original = s.toString();
                String sanitized = sanitize(original, inputType);

                // Проверка на SQL-инъекции
                if (containsSqlInjectionPatterns(original)) {
                    sanitized = "";
                    if (editText.getContext() != null) {
                        android.widget.Toast.makeText(editText.getContext(),
                                "Обнаружен недопустимый ввод", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }

                if (!sanitized.equals(original)) {
                    isSanitizing = true;
                    editText.setText(sanitized);
                    editText.setSelection(Math.min(sanitized.length(), editText.getSelectionStart()));
                    isSanitizing = false;
                }
                lastSanitized = sanitized;
            }
        };
    }

    /**
     * Валидация email адреса
     * @param email Email для проверки
     * @return true если email валидный
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    /**
     * Валидация имени пользователя
     * @param username Имя для проверки
     * @return true если имя валидное
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        // Имя должно содержать от 2 до 50 символов, только буквы, цифры, пробелы, дефис, апостроф
        String usernameRegex = "^[\\p{L}\\p{N}\\s\\-']{2,50}$";
        return Pattern.compile(usernameRegex).matcher(username).matches();
    }

    /**
     * Полная обработка строки перед сохранением в БД
     * @param input Входная строка
     * @param inputType Тип валидации
     * @param maxLength Максимальная длина
     * @return Полностью обработанная строка
     */
    public static String prepareForDatabase(String input, InputType inputType, int maxLength) {
        if (input == null) {
            return "";
        }

        String result = sanitize(input, inputType);
        result = stripHtml(result);
        result = truncate(result, maxLength);
        result = escapeSql(result);

        return result.trim();
    }

    /**
     * Комбинированная проверка: санитизация + валидация
     * @param input Входная строка
     * @param inputType Тип валидации
     * @param maxLength Максимальная длина
     * @return Result объект с результатом проверки
     */
    public static ValidationResult validateAndSanitize(String input, InputType inputType, int maxLength) {
        String sanitized = sanitize(input, inputType);
        sanitized = stripHtml(sanitized);

        ValidationResult result = new ValidationResult();
        result.original = input;
        result.sanitized = truncate(sanitized, maxLength);

        // Проверки
        if (input == null || input.isEmpty()) {
            result.isValid = false;
            result.errorMessage = "Поле не может быть пустым";
            return result;
        }

        if (containsSqlInjectionPatterns(input)) {
            result.isValid = false;
            result.errorMessage = "Обнаружены подозрительные символы";
            return result;
        }

        switch (inputType) {
            case USERNAME:
                result.isValid = isValidUsername(result.sanitized);
                if (!result.isValid) result.errorMessage = "Имя должно содержать 2-50 символов (буквы, цифры, пробел, дефис)";
                break;
            case EMAIL:
                result.isValid = isValidEmail(result.sanitized);
                if (!result.isValid) result.errorMessage = "Неверный формат email";
                break;
            default:
                result.isValid = !result.sanitized.isEmpty();
                if (!result.isValid) result.errorMessage = "Некорректный ввод";
                break;
        }

        return result;
    }

    /**
     * Класс результата валидации
     */
    public static class ValidationResult {
        public String original;
        public String sanitized;
        public boolean isValid;
        public String errorMessage;

        public ValidationResult() {
            this.isValid = true;
            this.errorMessage = "";
        }
    }
}