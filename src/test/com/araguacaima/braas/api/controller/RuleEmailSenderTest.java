package com.araguacaima.braas.api.controller;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.araguacaima.braas.core.Constants.environment;

public class RuleEmailSenderTest {

    @Before
    public void init() {
        environment.put("MONGODB_URI", "mongodb://localhost:27017/braas_db");
    }

    @Test
    public void testsSend() {
        Map<String, Object> params = new HashMap<>();
        params.put("to", "araguacaima@gmail.com");
        RuleEmailSender.send("Test", params);
    }

}
