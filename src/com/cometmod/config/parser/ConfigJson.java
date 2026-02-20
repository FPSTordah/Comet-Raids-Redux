package com.cometmod.config.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared lightweight JSON helpers used by config parsing.
 * <p>
 * This is intentionally minimal (no external dependencies) and is designed for
 * the current config schema shape used by Comet mod files.
 */
public final class ConfigJson {

    private ConfigJson() {
    }

    public static String extractJsonObject(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int braceIndex = json.indexOf("{", colonIndex);
            if (braceIndex == -1) {
                return null;
            }

            return extractObjectFromPosition(json, braceIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractJsonArray(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int bracketIndex = json.indexOf("[", colonIndex);
            if (bracketIndex == -1) {
                return null;
            }

            return extractArrayFromPosition(json, bracketIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractObjectFromPosition(String json, int startPos) {
        if (startPos < 0 || startPos >= json.length() || json.charAt(startPos) != '{') {
            return null;
        }

        int depth = 0;
        int endPos = startPos;
        boolean inString = false;

        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        endPos = i + 1;
                        break;
                    }
                }
            }
        }

        return endPos > startPos ? json.substring(startPos, endPos) : null;
    }

    public static String extractArrayFromPosition(String json, int startPos) {
        if (startPos < 0 || startPos >= json.length() || json.charAt(startPos) != '[') {
            return null;
        }

        int depth = 0;
        int endPos = startPos;
        boolean inString = false;

        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        endPos = i + 1;
                        break;
                    }
                }
            }
        }

        return endPos > startPos ? json.substring(startPos, endPos) : null;
    }

    public static List<String> extractArrayObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        if (arrayJson == null || arrayJson.length() < 2) {
            return objects;
        }

        int i = 1; // skip opening bracket
        while (i < arrayJson.length()) {
            char c = arrayJson.charAt(i);
            if (c == '{') {
                String obj = extractObjectFromPosition(arrayJson, i);
                if (obj != null) {
                    objects.add(obj);
                    i += obj.length();
                    continue;
                }
            }
            i++;
        }

        return objects;
    }

    public static List<String> extractStringArray(String arrayJson) {
        List<String> strings = new ArrayList<>();
        if (arrayJson == null) {
            return strings;
        }

        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(arrayJson);
        while (matcher.find()) {
            strings.add(matcher.group(1));
        }
        return strings;
    }

    public static List<Integer> extractIntArray(String json, String key) {
        List<Integer> ints = new ArrayList<>();
        String arrayJson = extractJsonArray(json, key);
        if (arrayJson == null) {
            return ints;
        }

        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(arrayJson);
        while (matcher.find()) {
            try {
                ints.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        return ints;
    }

    public static String extractStringValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startQuote = json.indexOf("\"", colonIndex + 1);
            if (startQuote == -1) {
                return null;
            }

            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) {
                return null;
            }

            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer extractIntValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startIndex = colonIndex + 1;
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            StringBuilder num = new StringBuilder();
            while (startIndex < json.length()) {
                char c = json.charAt(startIndex);
                if (Character.isDigit(c) || c == '-') {
                    num.append(c);
                    startIndex++;
                } else {
                    break;
                }
            }

            return num.length() > 0 ? Integer.parseInt(num.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Double extractDoubleValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startIndex = colonIndex + 1;
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            StringBuilder num = new StringBuilder();
            while (startIndex < json.length()) {
                char c = json.charAt(startIndex);
                if (Character.isDigit(c) || c == '.' || c == '-') {
                    num.append(c);
                    startIndex++;
                } else {
                    break;
                }
            }

            return num.length() > 0 ? Double.parseDouble(num.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean extractBooleanValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            String afterColon = json.substring(colonIndex + 1).trim();
            if (afterColon.startsWith("true")) {
                return true;
            }
            if (afterColon.startsWith("false")) {
                return false;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractPrimitiveValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startIndex = colonIndex + 1;
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            int endIndex = startIndex;
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                    break;
                }
                endIndex++;
            }

            return json.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
