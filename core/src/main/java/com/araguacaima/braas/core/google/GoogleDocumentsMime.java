package com.araguacaima.braas.core.google;


import java.util.*;

public class GoogleDocumentsMime {

    public enum Types {

        FORM("application/vnd.google-apps.form"),
        DOCUMENT("application/vnd.google-apps.document"),
        DRAWING("application/vnd.google-apps.drawing"),
        SPREADSHEET("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        SCRIPT("application/vnd.google-apps.script"),
        PRESENTATION("application/vnd.google-apps.presentation"),

        AUDIO("application/vnd.google-apps.audio"),
        FILE("application/vnd.google-apps.file"),
        FOLDER("application/vnd.google-apps.folder"),
        FUSIONTABLE("application/vnd.google-apps.fusiontable"),
        MAP("application/vnd.google-apps.map"),
        PHOTO("application/vnd.google-apps.photo"),
        SITES("application/vnd.google-apps.sites"),
        UNKNOWN("application/vnd.google-apps.unknown"),
        VIDEO("application/vnd.google-apps.video"),
        DRIVE_SDK("application/vnd.google-apps.drive-sdk");

        private static List<Types> EXPORTABLE = new ArrayList<Types>() {{
            add(Types.FORM);
            add(Types.DOCUMENT);
            add(Types.DRAWING);
            add(Types.SPREADSHEET);
            add(Types.SCRIPT);
            add(Types.PRESENTATION);
        }};
        String value;

        Types(String value) {
            this.value = value.trim();
        }

        public static boolean isDocument(String mimeType) {
            try {
                return EXPORTABLE.contains(findValue(mimeType));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        public static Types findValue(String value)
                throws IllegalArgumentException {
            Types result = null;
            try {
                result = Types.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                for (Types accionEnum : Types.values()) {
                    String accionEnumStr = accionEnum.value();
                    if (accionEnumStr.equalsIgnoreCase(value)) {
                        return accionEnum;
                    }
                }
            }
            if (result == null) {
                throw new IllegalArgumentException("No enum const " + Types.class.getName() + "." + value);
            }
            return result;
        }

        public String value() {
            return value;
        }
    }

    public static class Conversions {

        public static Map<Types, LinkedHashSet<String>> EXPORT = new HashMap<>();
        public static Map<String, String> FILE_EXTENSIONS = new HashMap<>();

        static {
            EXPORT.put(Types.FORM, new LinkedHashSet<String>() {{
                add("application/zip");
            }});
            EXPORT.put(Types.DOCUMENT, new LinkedHashSet<String>() {{
                add("application/rtf");
                add("application/vnd.oasis.opendocument.text");
                add("text/html");
                add("application/pdf");
                add("application/epub+zip");
                add("application/zip");
                add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                add("text/plain");

            }});
            EXPORT.put(Types.DRAWING, new LinkedHashSet<String>() {{
                add("image/svg+xml");
                add("image/png");
                add("application/pdf");
                add("image/jpeg");
            }});
            EXPORT.put(Types.SPREADSHEET, new LinkedHashSet<String>() {{
                add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                add("application/vnd.oasis.opendocument.spreadsheet");
                add("application/x-vnd.oasis.opendocument.spreadsheet");
                add("text/tab-separated-values");
                add("application/pdf");
                add("text/csv");
                add("application/zip");
            }});
            EXPORT.put(Types.SCRIPT, new LinkedHashSet<String>() {{
                add("application/vnd.google-apps.script+json");
            }});
            EXPORT.put(Types.PRESENTATION, new LinkedHashSet<String>() {{
                add("application/vnd.oasis.opendocument.presentation");
                add("application/pdf");
                add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
                add("text/plain");
            }});
            FILE_EXTENSIONS.put("application/epub+zip", ".zip");
            FILE_EXTENSIONS.put("application/pdf", ".pdf");
            FILE_EXTENSIONS.put("application/rtf", ".rtf");
            FILE_EXTENSIONS.put("application/vnd.google-apps.script+json", ".json");
            FILE_EXTENSIONS.put("application/vnd.oasis.opendocument.presentation", ".ppt");
            FILE_EXTENSIONS.put("application/vnd.oasis.opendocument.spreadsheet", ".xlsx");
            FILE_EXTENSIONS.put("application/vnd.oasis.opendocument.text", ".txt");
            FILE_EXTENSIONS.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".ppt");
            FILE_EXTENSIONS.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
            FILE_EXTENSIONS.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
            FILE_EXTENSIONS.put("application/x-vnd.oasis.opendocument.spreadsheet", ".xlsx");
            FILE_EXTENSIONS.put("application/zip", ".zip");
            FILE_EXTENSIONS.put("image/jpeg", ".jpg");
            FILE_EXTENSIONS.put("image/png", ".png");
            FILE_EXTENSIONS.put("image/svg+xml", ".svg");
            FILE_EXTENSIONS.put("text/csv", ".csv");
            FILE_EXTENSIONS.put("text/html", ".html");
            FILE_EXTENSIONS.put("text/plain", ".txt");
            FILE_EXTENSIONS.put("text/tab-separated-values", ".csv");
            FILE_EXTENSIONS.put("application/vnd.google-apps.audio", ".mp3");
            FILE_EXTENSIONS.put("application/vnd.google-apps.document", ".docx");
            FILE_EXTENSIONS.put("application/vnd.google-apps.drawing", ".mp3");
            FILE_EXTENSIONS.put("application/vnd.google-apps.drive-sdk", ".drv");
            FILE_EXTENSIONS.put("application/vnd.google-apps.file", ".file");
            FILE_EXTENSIONS.put("application/vnd.google-apps.folder", ".folder");
            FILE_EXTENSIONS.put("application/vnd.google-apps.form", ".form");
            FILE_EXTENSIONS.put("application/vnd.google-apps.fusiontable", ".ftable");
            FILE_EXTENSIONS.put("application/vnd.google-apps.map", ".map");
            FILE_EXTENSIONS.put("application/vnd.google-apps.photo", ".png");
            FILE_EXTENSIONS.put("application/vnd.google-apps.presentation", ".ppt");
            FILE_EXTENSIONS.put("application/vnd.google-apps.script", ".js");
            FILE_EXTENSIONS.put("application/vnd.google-apps.sites", ".site");
            FILE_EXTENSIONS.put("application/vnd.google-apps.spreadsheet", ".xlsx");
            FILE_EXTENSIONS.put("application/vnd.google-apps.unknown", ".unk");
            FILE_EXTENSIONS.put("application/vnd.google-apps.video", ".mp4");
        }
    }
}
