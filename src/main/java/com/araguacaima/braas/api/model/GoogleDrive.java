package com.araguacaima.braas.api.model;

import java.util.Map;

public class GoogleDrive {

    private Rules rules;
    private Map<String, String> credential;

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    public Map<String, String> getCredential() {
        return credential;
    }

    public void setCredential(Map<String, String> credential) {
        this.credential = credential;
    }

    public static class Rules {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
