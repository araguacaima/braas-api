package com.araguacaima.braas.core.drools;

/**
 * Created by Alejandro on 21/11/2014.
 */
public enum DataTypes {
    STRING("String"),
    ARRAY("Array"),
    INTEGER("Integer"),
    NUMBER("Number"),
    ENUM("Enum"),
    OBJECT("Object"),
    DATE("Date"),
    DATETIME("Datetime");

    String value;

    DataTypes(String value) {
        this.value = value.trim();
    }

    public static DataTypes findValue(String value)
            throws IllegalArgumentException {
        DataTypes result = null;
        try {
            result = DataTypes.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            for (DataTypes accionEnum : DataTypes.values()) {
                String accionEnumStr = accionEnum.value();
                if (accionEnumStr.equalsIgnoreCase(value)) {
                    return accionEnum;
                }
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("No enum const " + DataTypes.class.getName() + "." + value);
        }
        return result;
    }

    public String value() {
        return value;
    }
}
