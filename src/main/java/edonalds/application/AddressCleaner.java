package edonalds.application;

import java.util.regex.Pattern;

public final class AddressCleaner {

    private AddressCleaner() {
    }

    private static final Pattern UNIT_NOTE = Pattern.compile(
            "(^|\\s+)(lgh|lokal|bst|stuga|hus|klipphus|kvarter|tomt)\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSTAL_CODE = Pattern.compile("(AX-?)?\\b\\d{5}\\b");
    private static final Pattern TRAILING_UNIT_LETTER = Pattern.compile("(\\d)\\s*[A-Za-z]\\s*\\d*\\s*$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String address = raw.trim();
        if (address.regionMatches(true, 0, "Del av ", 0, 7)) {
            return "";
        }

        // Anything after "/" is an alternative address and anything after " i " is a free-text location
        address = cutAt(address, "/");
        address = cutAt(address, " i ");

        StringBuilder result = new StringBuilder();
        for (String part : address.split(",")) {
            String cleaned = UNIT_NOTE.matcher(part).replaceAll("");
            cleaned = POSTAL_CODE.matcher(cleaned).replaceAll("");
            cleaned = TRAILING_UNIT_LETTER.matcher(cleaned.trim()).replaceAll("$1");
            cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
            if (cleaned.endsWith(".")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
            }
            if (!cleaned.isEmpty()) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(cleaned);
            }
        }
        if (result.length() == 0) {
            return "";
        }
        return result.append(" Åland").toString();
    }

    private static String cutAt(String value, String separator) {
        int index = value.indexOf(separator);
        return index < 0 ? value : value.substring(0, index);
    }
}
