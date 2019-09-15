package com.araguacaima.braas.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class Constants {
    public static final String SIMPLE = "Simple";
    public static final String SHARED = "Shared";
    public static final String BASE_API_NAMESPACE = "https://www.bbvaapis.com/";
    public static final String BASE_API_NAMESPACE_PATTERN = BASE_API_NAMESPACE + "${serviceName}/${version}";
    public static final String API_BASE_NAME = "api.raml";
    public static final String COMPLETE_TEXT = "$$$COMPLETE$$$";
    public static final String APIS_COMMONS_COMMONS = "glapi-global-apis-commons-commons";
    public static Locale LOCALE_EN = new Locale("en", "us");
    public static Locale LOCALE_ES = new Locale("es", "es");
    public static Collection<Locale> LOCALES = new ArrayList<Locale>() {
        {
            add(LOCALE_EN);
            add(LOCALE_ES);
        }
    };

    public enum URL_RESOURCE_STRATEGIES {
        WORKBENCH,
        MAVEN,
        ABSOLUTE_DECISION_TABLE_PATH,
        GOOGLE_DRIVE_DECISION_TABLE_PATH,
        ABSOLUTE_DRL_PATH
    }

    public enum RULES_SESSION_TYPE {
        STATEFUL,
        STATELESS
    }

    public enum RULES_REPOSITORY_STRATEGIES {
        DRL,
        DECISION_TABLE
    }

    public enum CREDENTIALS_ORIGIN_STRATEGIES {
        SERVER,
        LOCAL,
        DB
    }

}
