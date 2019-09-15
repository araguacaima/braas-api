package com.araguacaima.braas.core.drools;

import com.araguacaima.commons.utils.ReflectionUtils;

import java.lang.reflect.Field;

public class Commons {

    public static final ReflectionUtils reflectionUtils = new ReflectionUtils(null);

    public static void setProperty(Object object, String fieldName, Object value) {
        Class clazz = object.getClass();

        try {
            Field field = reflectionUtils.getFieldInclusiveOnParents(clazz, fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
