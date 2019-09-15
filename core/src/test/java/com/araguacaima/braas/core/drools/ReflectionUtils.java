package com.araguacaima.braas.core.drools;

import com.google.common.collect.ObjectArrays;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import sun.reflect.ConstructorAccessor;
import sun.reflect.FieldAccessor;
import sun.reflect.ReflectionFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by Alejandro on 09/09/2015.
 */
@Component
public class ReflectionUtils {

    public static Collection<String> COMMONS_JAVA_TYPES_EXCLUSIONS = new ArrayList<String>() {
        {
            add("java.util.Currency");
            add("java.util.Calendar");
            add("org.joda.time.Period");
        }
    };
    public static Collection<String> COMMONS_TYPES_PREFIXES = new ArrayList<String>() {
        {
            add("java.lang");
            add("java.util");
            add("java.math");
            add("java.io");
            add("java.sql");
            add("java.text");
            add("java.net");
            add("org.joda.time");
        }
    };
    private static Collection<Class> COMMONS_COLLECTIONS_IMPLEMENTATIONS = new ArrayList<Class>() {
        {
            add(ArrayList.class);
            add(TreeSet.class);
            add(HashSet.class);
            add(LinkedHashSet.class);
            add(LinkedList.class);
        }
    };
    private static ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();

    /**
     * Add an enum instance to the enum class given as argument
     *
     * @param <T>         the type of the enum (implicit)
     * @param type        the class of the enum to be modified
     * @param name        the name of the new enum instance to be added to the class.
     * @param value       the value for the name of the new enum instance.
     * @param description the full description of the new enum instance.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<?>> void addEnum(Class<T> type, String name, String value, String description) {

        // 0. Sanity checks
        if (!Enum.class.isAssignableFrom(type)) {
            throw new RuntimeException("class " + type + " is not an instance of Enum");
        }

        // 1. Lookup "$VALUES" holder in enum class and get previous enum instances
        Field valuesField = null;
        Field[] fields = type.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("enumConstants")) {
                valuesField = field;
                AccessibleObject.setAccessible(new Field[]{valuesField}, true);
                break;
            }
        }

        try {
            // 2. Copy it
            T[] previousValues;
            List<T> values = new ArrayList<T>();
            if (valuesField != null) {
                previousValues = (T[]) valuesField.get(type);
                if (previousValues != null) {
                    values = new ArrayList<T>(Arrays.asList(previousValues));
                }
            }
            // 3. build new enum
            T newValue; // could be used to pass values to the enum constuctor if needed

            newValue = (T) makeEnum(type, // The target enum class
                    name, value, description,// THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
                    values.size(), new Class<?>[]{}, // could be used to pass values to the enum constuctor if needed
                    new Object[]{});

            // 4. add new value
            values.add(newValue);

            // 5. Set new values field
            setFailsafeFieldValue(valuesField, type, values.toArray((T[]) Array.newInstance(type, 0)));

            // 6. Clean enum cache
            //            cleanEnumCache(type);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static Object makeEnum(Class<?> enumClass,
                                   String name,
                                   String value,
                                   String description,
                                   int ordinal,
                                   Class<?>[] additionalTypes,
                                   Object[] additionalValues)
            throws Exception {
        Object[] parms = new Object[additionalValues.length + 2];
        parms[0] = name;
        parms[1] = ordinal;
        System.arraycopy(additionalValues, 0, parms, 2, additionalValues.length);
        Object o = enumClass.cast(getConstructorAccessor(enumClass, additionalTypes).newInstance(parms));
        try {
            Method mValue = o.getClass().getMethod("setValue", String.class);
            mValue.setAccessible(true);
            mValue.invoke(o, value);
            Method mDescription = o.getClass().getMethod("setDescription", String.class);
            mDescription.setAccessible(true);
            mDescription.invoke(o, description);
        } catch (Throwable ignored) {
        }
        return o;
    }

    private static ConstructorAccessor getConstructorAccessor(Class<?> enumClass, Class<?>[] additionalParameterTypes)
            throws NoSuchMethodException {
        Class<?>[] parameterTypes = new Class[additionalParameterTypes.length + 2];
        parameterTypes[0] = String.class;
        parameterTypes[1] = int.class;
        System.arraycopy(additionalParameterTypes, 0, parameterTypes, 2, additionalParameterTypes.length);
        return reflectionFactory.newConstructorAccessor(enumClass.getDeclaredConstructor(parameterTypes));
    }

    @SuppressWarnings("unchecked")
    public static Object changeAnnotationValue(Annotation annotation, String key, Object newValue) {
        Object handler = Proxy.getInvocationHandler(annotation);
        Field f;
        try {
            f = handler.getClass().getDeclaredField("memberValues");
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalStateException(e);
        }
        f.setAccessible(true);
        Map<String, Object> memberValues;
        try {
            memberValues = (Map<String, Object>) f.get(handler);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        Object oldValue = memberValues.get(key);
        if (oldValue == null || oldValue.getClass() != newValue.getClass()) {
            throw new IllegalArgumentException();
        }
        memberValues.put(key, newValue);
        return oldValue;
    }

    private static void cleanEnumCache(Class<?> enumClass)
            throws NoSuchFieldException, IllegalAccessException {
        blankField(enumClass, "enumConstantDirectory"); // Sun (Oracle?!?) JDK 1.5/6
        blankField(enumClass, "enumConstants"); // IBM JDK
    }

    private static void blankField(Class<?> enumClass, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        for (Field field : Class.class.getDeclaredFields()) {
            if (field.getName().contains(fieldName)) {
                AccessibleObject.setAccessible(new Field[]{field}, true);
                setFailsafeFieldValue(field, enumClass, null);
                break;
            }
        }
    }

    private static void setFailsafeFieldValue(Field field, Object target, Object value)
            throws NoSuchFieldException, IllegalAccessException {

        // let's make the field accessible
        field.setAccessible(true);

        // next we change the modifier in the Field instance to
        // not be final anymore, thus tricking reflection into
        // letting us modify the static final field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = modifiersField.getInt(field);

        // blank out the final bit in the modifiers int
        modifiers &= ~Modifier.FINAL;
        modifiersField.setInt(field, modifiers);

        FieldAccessor fa = reflectionFactory.newFieldAccessor(field, false);
        fa.set(target, value);
    }


    public static javassist.bytecode.annotation.Annotation createAnnotationsAnnotation(List<javassist.bytecode
            .annotation.Annotation> bindingDefinitionElements,
                                                                                       String annotationName,
                                                                                       ConstPool cpool) {

        javassist.bytecode.annotation.Annotation elementDescriptionsAnnot = null;
        if (CollectionUtils.isNotEmpty(bindingDefinitionElements)) {
            elementDescriptionsAnnot = ReflectionUtils.createAnnotationValuesAnnotation(cpool,
                    annotationName,
                    bindingDefinitionElements);
        }
        return elementDescriptionsAnnot;
    }

    public static javassist.bytecode.annotation.Annotation createBooleanValuesAnnotation(ConstPool cpool,
                                                                                         String annotationName,
                                                                                         Map<String, Boolean>
                                                                                                 annotationValues) {
        javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(annotationName,
                cpool);
        if (annotationValues != null) {
            for (Map.Entry<String, Boolean> entry : annotationValues.entrySet()) {
                String key = entry.getKey();
                Boolean value = entry.getValue();
                BooleanMemberValue memberValue = new BooleanMemberValue(cpool);
                memberValue.setValue(value);
                annot.addMemberValue(key, memberValue);
            }
        }
        return annot;
    }

    public static javassist.bytecode.annotation.Annotation createStringValuesAnnotation(ConstPool cpool,
                                                                                        String annotationName,
                                                                                        Map<String, String>
                                                                                                annotationValues) {

        javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(annotationName,
                cpool);
        if (annotationValues != null) {
            for (Map.Entry<String, String> entry : annotationValues.entrySet()) {
                String value = StringUtils.defaultIfEmpty(entry.getValue(), StringUtils.EMPTY);
                StringMemberValue memberValue = new StringMemberValue(value, cpool);
                annot.addMemberValue(entry.getKey(), memberValue);
            }
        }
        return annot;
    }

    public static javassist.bytecode.annotation.Annotation createAnnotationValuesAnnotation(ConstPool cpool,
                                                                                            String annotationName,
                                                                                            List<javassist.bytecode
                                                                                                    .annotation
                                                                                                    .Annotation>
                                                                                                    annotationValues) {
        javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(annotationName,
                cpool);

        if (annotationValues != null) {
            ArrayMemberValue arrayMemberValue = new ArrayMemberValue(new AnnotationMemberValue(cpool), cpool);
            List<AnnotationMemberValue> values = new ArrayList<>();
            for (javassist.bytecode.annotation.Annotation annotation : annotationValues) {
                AnnotationMemberValue memberValue = new AnnotationMemberValue(cpool);
                memberValue.setValue(annotation);
                values.add(memberValue);
            }
            AnnotationMemberValue[] annotationMemberValues = values.toArray(new AnnotationMemberValue[values.size()]);
            arrayMemberValue.setValue(annotationMemberValues);
            annot.addMemberValue("value", arrayMemberValue);
        }
        return annot;
    }

    public static javassist.bytecode.annotation.Annotation createEnumValuesAnnotation(ConstPool cpool,
                                                                                      String annotationName,
                                                                                      Map<String, ? extends Enum>
                                                                                              annotationValues) {
        javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(annotationName,
                cpool);
        if (annotationValues != null) {
            for (Map.Entry<String, ? extends Enum> entry : annotationValues.entrySet()) {
                String key = entry.getKey();
                Enum value = entry.getValue();
                EnumMemberValue memberValue = new EnumMemberValue(cpool);
                memberValue.setType(value.getClass().getName());
                memberValue.setValue(value.name());
                annot.addMemberValue(key, memberValue);
            }
        }
        return annot;
    }


    public static CtMethod generateGetter(CtClass declaringClass, String fieldName, CtClass fieldClass)
            throws CannotCompileException {

        Class clazz = fieldClass.getClass();
        String prefix = (clazz.getSimpleName().equals("Boolean") || clazz.getTypeName().equals("boolean")) ? "is" :
                "get";
        String getterName = prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        StringBuilder sb = new StringBuilder();
        sb.append("public ").append(fieldClass.getName()).append(" ").append(getterName).append("(){").append(
                "return this.").append(fieldName).append(";").append("}");
        return CtMethod.make(sb.toString(), declaringClass);
    }

    public static CtMethod generateSetter(CtClass declaringClass, String fieldName, CtClass fieldClass)
            throws CannotCompileException {

        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        StringBuilder sb = new StringBuilder();
        sb.append("public void ").append(setterName).append("(").append(fieldClass.getName()).append(" ").append(
                fieldName).append(")").append("{").append("this.").append(fieldName).append("=").append(fieldName)
                .append(
                ";").append("}");
        return CtMethod.make(sb.toString(), declaringClass);
    }

    public static Collection<Field> getAllFieldsIncludingParents(Object object) {
        return getAllFieldsIncludingParents(object.getClass());
    }

    public static String getExtractedGenerics(String s) {
        String s1 = s.trim();
        try {
            s1 = s1.split("List<")[1].replaceAll(">", StringUtils.EMPTY);
        } catch (Throwable ignored) {
        }
        return s1;
    }


    public static boolean isCollectionImplementation(String fullyQulifiedClassName) {
        try {
            Class clazz = Class.forName(fullyQulifiedClassName);
            return isCollectionImplementation(clazz);
        } catch (Throwable ignored) {
        }
        return false;

    }

    public static boolean isCollectionImplementation(Class clazz) {
        if (clazz == null) {
            return false;
        }
        return Collection.class.isAssignableFrom(clazz) || Object[].class.isAssignableFrom(clazz) || clazz.isArray();
    }

    public static boolean isList(String type) {
        try {
            return type.equals("List") || type.startsWith("List<") || type.startsWith("java.util.List<") || type.equals(
                    "Collection") || type.startsWith("Collection<") || type.startsWith("java.util.Collection<");
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T mergeObjects(T origin, T target)
            throws IllegalAccessException, InstantiationException {
        return mergeObjects(origin, target, false, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> T mergeObjects(T origin, T target, boolean override, boolean nullify)
            throws IllegalAccessException, InstantiationException {
        Class<?> targetClazz = target.getClass();
        Collection<Field> targetFields = getAllFieldsIncludingParents(targetClazz);
        Object returnValue = targetClazz.newInstance();
        for (Field field : targetFields) {
            try {
                field.setAccessible(true);
                Object value = field.get(origin);
                if (nullify || (value != null)) {
                    if (override) {
                        field.set(returnValue, value);
                    } else {
                        if (isMapImplementation(value.getClass())) {
                            if (((Map) value).size() == 0) {
                                field.set(returnValue, value);
                                continue;
                            }
                        }
                        if (isCollectionImplementation(value.getClass())) {
                            if (((Collection) value).size() == 0) {
                                field.set(returnValue, value);
                                continue;
                            }
                        }
                        final Object defaultValue = PrimitiveDefaults.getDefaultValue(value.getClass());
                        if (defaultValue != null && !defaultValue.equals(value)) {
                            field.set(returnValue, value);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return (T) returnValue;
    }

    public static Collection<Field> getAllFieldsIncludingParents(Class clazz) {
        return getAllFieldsIncludingParents(clazz,
                null,
                Modifier.STATIC | Modifier.VOLATILE | Modifier.NATIVE | Modifier.TRANSIENT);
    }

    public static boolean isMapImplementation(Class clazz) {
        return clazz != null && Map.class.isAssignableFrom(clazz);
    }

    public static Collection<Field> getAllFieldsIncludingParents(Class clazz,
                                                                 final Integer modifiersInclusion,
                                                                 final Integer modifiersExclusion) {
        final Collection<Field> fields_ = new ArrayList<>();
        org.springframework.util.ReflectionUtils.FieldFilter fieldFilterModifierInclusion = new org.springframework
                .util.ReflectionUtils.FieldFilter() {
            @Override
            public boolean matches(Field field) {
                return modifiersInclusion == null || (field.getModifiers() & modifiersInclusion) != 0;
            }
        };
        org.springframework.util.ReflectionUtils.FieldFilter fieldFilterModifierExclusion = new org.springframework
                .util.ReflectionUtils.FieldFilter() {
            @Override
            public boolean matches(Field field) {
                return modifiersExclusion == null || (field.getModifiers() & modifiersExclusion) == 0;
            }
        };
        org.springframework.util.ReflectionUtils.doWithFields(clazz,
                new org.springframework.util.ReflectionUtils.FieldCallback() {
                    @Override
                    public void doWith(Field field)
                            throws IllegalArgumentException, IllegalAccessException {
                        fields_.add(field);
                    }
                },
                modifiersInclusion == null ? modifiersExclusion == null ? null : fieldFilterModifierExclusion :
                        fieldFilterModifierInclusion);
        return fields_;
    }


    public static class PrimitiveDefaults {
        // These gets initialized to their default values
        private static final boolean DEFAULT_BOOLEAN = Boolean.FALSE;
        private static final Boolean DEFAULT_BOOLEAN_ = Boolean.FALSE;
        private static final byte DEFAULT_BYTE = (byte) 0;
        private static final Byte DEFAULT_BYTE_ = (byte) 0;
        private static final char DEFAULT_CHAR = '\0';
        private static final double DEFAULT_DOUBLE = 0d;
        private static final Double DEFAULT_DOUBLE_ = 0d;
        private static final float DEFAULT_FLOAT = 0f;
        private static final Float DEFAULT_FLOAT_ = 0f;
        private static final int DEFAULT_INT = 0;
        private static final Integer DEFAULT_INT_ = 0;
        private static final long DEFAULT_LONG = 0L;
        private static final Long DEFAULT_LONG_ = 0L;
        private static final short DEFAULT_SHORT = (short) 0;
        private static final Short DEFAULT_SHORT_ = (short) 0;
        private static final String DEFAULT_STRING_ = StringUtils.EMPTY;

        public static Object getDefaultValue(Class clazz) {
            if (clazz.equals(boolean.class)) {
                return DEFAULT_BOOLEAN;
            } else if (clazz.equals(Boolean.class)) {
                return DEFAULT_BOOLEAN_;
            } else if (clazz.equals(byte.class)) {
                return DEFAULT_BYTE;
            } else if (clazz.equals(Byte.class)) {
                return DEFAULT_BYTE_;
            } else if (clazz.equals(short.class)) {
                return DEFAULT_SHORT;
            } else if (clazz.equals(Short.class)) {
                return DEFAULT_SHORT_;
            } else if (clazz.equals(char.class)) {
                return DEFAULT_CHAR;
            } else if (clazz.equals(int.class)) {
                return DEFAULT_INT;
            } else if (clazz.equals(Integer.class)) {
                return DEFAULT_INT_;
            } else if (clazz.equals(long.class)) {
                return DEFAULT_LONG;
            } else if (clazz.equals(Long.class)) {
                return DEFAULT_LONG_;
            } else if (clazz.equals(float.class)) {
                return DEFAULT_FLOAT;
            } else if (clazz.equals(Float.class)) {
                return DEFAULT_FLOAT_;
            } else if (clazz.equals(double.class)) {
                return DEFAULT_DOUBLE;
            } else if (clazz.equals(Double.class)) {
                return DEFAULT_DOUBLE_;
            } else if (clazz.equals(String.class)) {
                return DEFAULT_STRING_;
            } else if (clazz.equals(Object.class)) {
                return null;
            } else {
                throw new IllegalArgumentException("Class type " + clazz + " not supported");
            }
        }
    }

}

