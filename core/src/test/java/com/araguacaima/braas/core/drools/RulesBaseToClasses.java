package com.araguacaima.braas.core.drools;

import com.araguacaima.commons.utils.ClassLoaderUtils;
import com.araguacaima.commons.utils.JsonUtils;
import javassist.*;
import javassist.bytecode.DuplicateMemberException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class RulesBaseToClasses {

    private static final Logger log = LoggerFactory.getLogger(RulesBaseToClasses.class);

    private com.araguacaima.commons.utils.ReflectionUtils reflectionUtils = new com.araguacaima.commons.utils.ReflectionUtils(null);


    private ClassLoaderUtils classLoaderUtils = new ClassLoaderUtils(null);


    private CommonUtils commonUtils = new CommonUtils();


    private List<CtClass> createdClasses = new ArrayList<>();


    private JsonUtils jsonUtils = new JsonUtils();

    private Map<String, File> files = new HashMap<>();
    public static String PACKAGE_PREFIX = "com.bbva.functional.document";
    private String directoryOutputPath;
    private ClassPool pool = ClassPool.getDefault();
    private String enumConstructorBody = "{\n\tthis.value = value;\n\tthis.description = description;\n}";

/*    public Map<String, File> generateClassFromObject(RestAnnotationProcessorConfig config)
            throws Exception {
        clear();
        Collection<Object> Objects = config.getObjectCollection();
        String fullClassName = config.getFullClassName();
        directoryOutputPath = config.getOutputPath();
        try {
            classLoaderUtils.removeClass(fullClassName);
            pool.get(fullClassName).detach();
        } catch (Throwable ignored) {
        }
        process(Objects, fullClassName);
        storeClasses();
        return files;
    }*/

    private void storeClasses() {
        createdClasses.forEach((value) -> {
            try {
                storeClass(value);
            } catch (IOException | CannotCompileException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public void clear()
            throws CannotCompileException {
        files.clear();
        createdClasses.clear();
        directoryOutputPath = null;
        pool.makePackage(pool.getClassLoader(), PACKAGE_PREFIX);
    }

/*    private void process(Collection<Object> Objects, String classname)
            throws Exception {
        CtClass cc;
        try {
            cc = pool.get(classname);
            pool = new ClassPool(null);
            pool.appendSystemPath();
            pool.makePackage(pool.getClassLoader(), PACKAGE_PREFIX);
            cc.detach();
        } catch (NotFoundException ignored) {
        }
        processClass(Objects, classname);
    }*/

    public void storeClass(CtClass cc)
            throws IOException, CannotCompileException, ClassNotFoundException {
        cc.defrost();
        String classname = cc.getName();
        String shortClassname = cc.getSimpleName();
        try {
            CtClass superClass = cc.getSuperclass();
            if (superClass != null) {
                String superClassName = superClass.getName();
                if (!Object.class.getName().equals(superClassName) && reflectionUtils.getSimpleJavaTypeOrNull(
                        superClassName,
                        true) == null && !ReflectionUtils.isCollectionImplementation(superClassName)) {
                    String superClassname = superClass.getSimpleName();
                    if (files.get(superClassname) == null) {
                        storeClass(superClass);
                    }
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        File directory = new File(directoryOutputPath + "/" + PACKAGE_PREFIX.replaceAll("\\.", "/"));
        FileUtils.forceMkdir(directory);

        File file = new File(directory.getCanonicalPath() + "/" + StringUtils.capitalize(shortClassname) + ".class");

        FileUtils.writeByteArrayToFile(file, cc.toBytecode());
        try {
            cc.toClass(classLoaderUtils.getClassLoader(), null);
        } catch (CannotCompileException ignored) {
        }
        files.put(classname, file);
    }

/*    private void processClass(Collection<Object> Objects, String classname)
            throws Exception {
        CtClass cc = createClass(classname);
        processRows(cc, Objects);
    }*/

    public CtClass createClass(String className)
            throws CannotCompileException {
        return createClass(className, null);
    }

    public CtClass createClass(String className, String superClass)
            throws CannotCompileException {

        String fullyQualifiedClassname;
        if (className.contains(".")) {
            fullyQualifiedClassname = className;
        } else {
            fullyQualifiedClassname = PACKAGE_PREFIX + "." + StringUtils.capitalize(className);
        }
        CtClass cc = null;
        try {
            cc = pool.get(fullyQualifiedClassname);
            if (StringUtils.isNotEmpty(superClass)) {
                CtClass superClass_;
                try {
                    superClass_ = pool.get(PACKAGE_PREFIX + "." + StringUtils.capitalize(superClass));
                } catch (NotFoundException ignored) {
                    superClass_ = createClass(superClass);
                }
                cc.defrost();
                cc.setSuperclass(superClass_);
            }
        } catch (NotFoundException ignored) {
        }
        if (cc == null) {
            String fullyQualifiedJavaType = reflectionUtils.getFullyQualifiedJavaTypeOrNull(className, false);
            if (fullyQualifiedJavaType != null) {
                try {
                    cc = pool.get(fullyQualifiedJavaType);
                } catch (NotFoundException ignored) {
                }
            }
        }
        if (cc == null) {
            cc = pool.makeClass(fullyQualifiedClassname);
            if (StringUtils.isNotEmpty(superClass)) {
                CtClass superClass_;
                try {
                    superClass_ = pool.get(PACKAGE_PREFIX + "." + StringUtils.capitalize(superClass));
                } catch (NotFoundException ignored) {
                    superClass_ = createClass(superClass);
                }
                cc.defrost();
                cc.setSuperclass(superClass_);
            }
        }
        if (reflectionUtils.getSimpleJavaTypeOrNull(cc.getSimpleName()) == null) {
            createdClasses.add(cc);
        }
        return cc;
    }

    private CtClass createCollectionClass(String s)
            throws CannotCompileException, NotFoundException {
        CtClass cc = createClass(s);
        cc.defrost();
        cc.setSuperclass(cc.getClassPool().get(LinkedHashSet.class.getName()));
        return cc;
    }

    public void addField(CtClass cc, String fieldName, Object Object, String dataType)
            throws Exception {
        CtClass fieldClass;
        String field_ = fieldName.replaceAll("\\[.*?\\]", "").replaceAll("<.*?>", "");
        DataTypes dataTypes = DataTypes.OBJECT;
        try {
            dataTypes = DataTypes.findValue(dataType);
        } catch (IllegalArgumentException ignored) {
        }
        switch (dataTypes) {
            case ARRAY:
                CtClass collectionType = createCollectionClass(commonUtils.pluralizeTypeName(field_));
                dataType = collectionType.getName();
                pool.importPackage(Collection.class.getPackage().getName());
                break;
            case OBJECT:
                dataType = StringUtils.capitalize(fieldName);
                break;
            case DATE:
                dataType = Date.class.getName();
                pool.importPackage(Date.class.getPackage().getName());
                break;
            case DATETIME:
                dataType = DateTime.class.getName();
                pool.importPackage(DateTime.class.getPackage().getName());
                break;
            case INTEGER:
                dataType = Integer.class.getName();
                break;
            case STRING:
                dataType = String.class.getName();
                break;
            case NUMBER:
                dataType = Number.class.getName();
                break;
            case ENUM:
                break;
            default:
                if (dataType.contains(".")) {
                    String[] dataTypeSplitted = dataType.split("\\.");
                    dataType = dataTypeSplitted[dataTypeSplitted.length - 1];
                } else {
                    dataType = StringUtils.capitalize(dataType);
                }
                break;
        }
        String javaTypeOrNull = reflectionUtils.getFullyQualifiedJavaTypeOrNull(dataType, false);
        fieldClass = javaTypeOrNull == null ? createClass(dataType, null) : pool.get(javaTypeOrNull);

        CtField field1 = null;
        try {
            field1 = cc.getField(field_);
        } catch (NotFoundException ignored) {
        }
        if (field1 == null) {
            CtField field = new CtField(fieldClass, field_, cc);
            try {
                cc.addField(field);
                cc.addMethod(ReflectionUtils.generateGetter(cc, field_, fieldClass));
                cc.addMethod(ReflectionUtils.generateSetter(cc, field_, fieldClass));
            } catch (DuplicateMemberException ignored) {
            }
        }
    }


/*
    private void appendClassAnnotations(CtClass cc, String en_description, String es_description) {
        cc.defrost();
        ClassFile cfile = cc.getClassFile();
        ConstPool cpool = cfile.getConstPool();
        String entity = StringUtils.uncapitalize(cc.getSimpleName());

        AnnotationsAttribute attr = new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);

        Map<String, String> xmlRootElementParams = new HashMap<>();
        xmlRootElementParams.put("name", entity);
        xmlRootElementParams.put("namespace", "http://bbva.com/corporate/model/" + entity);
        Annotation xmlRootElementAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                XmlRootElement.class.getName(),
                xmlRootElementParams);

        Map<String, String> xmlTypeParams = new HashMap<>();
        xmlTypeParams.put("name", entity);
        xmlTypeParams.put("namespace", "http://bbva.com/corporate/model/" + entity);
        Annotation xmlTypeAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                XmlType.class.getName(),
                xmlTypeParams);

        Map<String, Enum> xmlAccessorTypeParams = new HashMap<>();
        xmlAccessorTypeParams.put("value", XmlAccessType.FIELD);
        Annotation xmlAccessorTypeAnnot = ReflectionUtils.createEnumValuesAnnotation(cpool,
                XmlAccessorType.class.getName(),
                xmlAccessorTypeParams);


        List<Annotation> annotations = new ArrayList<>();

        annotations.add(xmlRootElementAnnot);
        annotations.add(xmlTypeAnnot);
        annotations.add(xmlAccessorTypeAnnot);
        attr.setAnnotations(annotations.toArray(new Annotation[annotations.size()]));
        cfile.addAttribute(attr);
    }

    private CtClass createEnumClass(String className, String[] enumValues)
            throws NotFoundException, CannotCompileException, ClassNotFoundException, IOException {

        String fullyQualifiedClassname;
        if (className.contains(".")) {
            fullyQualifiedClassname = className;
        } else {
            fullyQualifiedClassname = PACKAGE_PREFIX + "." + StringUtils.capitalize(className);
        }
        CtClass cc = null;
        try {
            cc = pool.get(fullyQualifiedClassname);
        } catch (NotFoundException ignored) {
        }
        if (cc == null) {
            String fullyQualifiedJavaType = reflectionUtils.getFullyQualifiedJavaTypeOrNull(className, false);
            if (fullyQualifiedJavaType != null) {
                try {
                    cc = pool.get(fullyQualifiedJavaType);
                } catch (NotFoundException ignored) {
                }
            }
        }
        if (cc == null) {
            CtClass enumClass = pool.get("java.lang.Enum");
            CtClass stringCtClass = pool.get("java.lang.String");
            cc = pool.makeClass(fullyQualifiedClassname, enumClass);
            CtField valueField = CtField.make("public String value;", cc);
            cc.addField(valueField);
            cc.addMethod(ReflectionUtils.generateGetter(cc, "value", stringCtClass));
            cc.addMethod(ReflectionUtils.generateSetter(cc, "value", stringCtClass));
            CtField descriptionField = CtField.make("public String description;", cc);
            cc.addField(descriptionField);
            cc.addMethod(ReflectionUtils.generateGetter(cc, "description", stringCtClass));
            cc.addMethod(ReflectionUtils.generateSetter(cc, "description", stringCtClass));
            cc.setModifiers(Modifier.ENUM);
            Class enumClass_;
            try {
                classLoaderUtils.removeClass(Class.forName(fullyQualifiedClassname));
            } catch (ClassNotFoundException ignored) {
            }
            try {
                enumClass_ = cc.toClass();
            } catch (CannotCompileException ignored) {
                enumClass_ = Class.forName(cc.getName());
            }
            for (String enumValue : enumValues) {
                String name;
                String value;
                String description = null;
                enumValue = enumValue.replaceAll(":", "=");
                String[] splitted = enumValue.split("=");
                name = splitted[0].trim().toUpperCase();
                value = name;
                try {
                    description = splitted[1].trim();
                } catch (Throwable ignored) {
                }
                ReflectionUtils.addEnum(enumClass_, name, value, description);
            }
            cc.defrost();
            cc = pool.makeClass(fullyQualifiedClassname);
        }
        return cc;
    }

    private void processRows(CtClass cc, Collection<Object> Objects)
            throws Exception {

        for (final Object Object : Objects) {
            String field = Object.toString();
            if (StringUtils.isNotBlank(field)) {
                String[] splittedField = StringUtils.split(field, ".");
                String[] enumValues = null;
                String enumValuesStr = Object.toString();
                if (StringUtils.isNotBlank(enumValuesStr)) {
                    enumValues = enumValuesStr.split("\n");
                }
                if (splittedField.length > 1) {
                    String fieldToken = splittedField[0];
                    processRow(null, splittedField, Object, enumValues);
                } else {
                    processField(cc, field, Object, enumValues);
                }
            }
        }
    }

    private void processRow(CtClass cc,
                            String[] fullyQualifiedField,
                            Object Object,
                            String[] enumValues)
            throws Exception {
        if (ArrayUtils.isNotEmpty(fullyQualifiedField)) {
            String firstToken = fullyQualifiedField[0];
            String field;
            if (firstToken.contains("<") && firstToken.contains(">")) {
                Pattern p = Pattern.compile("(<(.*?)>)");
                Matcher m = p.matcher(firstToken);
                if (m.find()) {
                    field = m.group(m.groupCount());
                    String s_ = StringUtils.replace(firstToken, field, StringUtils.EMPTY);
                    String[] splittedField = StringUtils.split(s_, ".");
                    String superClass = splittedField[splittedField.length - 1];
                    superClass = superClass.replaceAll("\\[.*?\\]", StringUtils.EMPTY).replaceAll("<.*?>",
                            StringUtils.EMPTY);
                    createClass(field, superClass);
                } else {
                    field = firstToken.replaceAll("\\[.*?\\]", StringUtils.EMPTY).replaceAll("<.*?>",
                            StringUtils.EMPTY);
                }
            } else {
                field = firstToken.replaceAll("\\[.*?\\]", StringUtils.EMPTY);
            }
            if (fullyQualifiedField.length > 1) {
                String[] remainingFields = Arrays.copyOfRange(fullyQualifiedField, 1, fullyQualifiedField.length);
                String description_en = Object.getDescription_en().getValue();
                String description_es = Object.getDescription_es().getValue();
                if (cc == null) {
                    processRow(createClass(field, description_en, description_es, null),
                            remainingFields,
                            Object,
                            enumValues);
                } else {
                    CtClass cc_ = createClass(field, description_en, description_es, null);
                    processRow(cc_, remainingFields, Object, enumValues);
                    Cell newType = new Cell();
                    newType.setValue(cc_.getName());
                    Object.setDataType(newType);
                    processField(cc, field, Object, enumValues);
                }
            } else {
                processField(cc, field, Object, enumValues);
            }
        }

    }


    private void appendFieldAnnotations(CtField field, Object Object)
            throws NotFoundException {
        ClassFile cfile = field.getDeclaringClass().getClassFile();
        ConstPool cpool = cfile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
        String en_description = StringUtils.defaultIfEmpty(Object.getDescription_en().getValue(),
                StringUtils.EMPTY);
        String es_description = StringUtils.defaultIfEmpty(Object.getDescription_es().getValue(),
                StringUtils.EMPTY);
        String entity = field.getName();
        CtClass type = field.getType();
        String dataType = type.getName();

        Annotation elementDescriptionsAnnot = reflectionUtils.createElementDescriptionsAnnotation(en_description,
                es_description,
                cpool);

        Annotation bindingDefinitionsAnnot = MethodWrapper.toAnnotations(cpool, Object.getMethod());

        Map<String, String> apiModelPropertyParams = new HashMap<>();
        apiModelPropertyParams.put("name", entity);
        apiModelPropertyParams.put("value", en_description);
        apiModelPropertyParams.put("dataType", dataType);
        Annotation apiModelPropertyAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                ApiModelProperty.class.getName(),
                apiModelPropertyParams);

        Map<String, Enum> attributeTypeParams = new HashMap<>();

        attributeTypeParams.put("value",
                (type.isEnum() || type.isPrimitive()) ? AttributeType.Include.SIMPLE : AttributeType.Include.COMPLEX);
        Annotation attributeTypeAnnot = ReflectionUtils.createEnumValuesAnnotation(cpool,
                AttributeType.class.getName(),
                attributeTypeParams);

        Map<String, String> params = new HashMap<>();
        List<Annotation> countrySpecificList = new ArrayList<>();
        List<Annotation> countryExceptionList = new ArrayList<>();

        String countrySpecific_es = Object.getCountrySpecific_es().getValue();
        if (StringUtils.isNotBlank(countrySpecific_es)) {
            params.put("country", "ES");
            params.put("description", countrySpecific_es);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_mx = Object.getCountrySpecific_mx().getValue();

        if (StringUtils.isNotBlank(countrySpecific_mx)) {
            params.put("country", "MX");
            params.put("description", countrySpecific_mx);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_co = Object.getCountrySpecific_co().getValue();

        if (StringUtils.isNotBlank(countrySpecific_co)) {
            params.put("country", "CO");
            params.put("description", countrySpecific_co);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_cl = Object.getCountrySpecific_cl().getValue();

        if (StringUtils.isNotBlank(countrySpecific_cl)) {
            params.put("country", "CL");
            params.put("description", countrySpecific_cl);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_pe = Object.getCountrySpecific_pe().getValue();

        if (StringUtils.isNotBlank(countrySpecific_pe)) {
            params.put("country", "PE");
            params.put("description", countrySpecific_pe);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_ar = Object.getCountrySpecific_ar().getValue();

        if (StringUtils.isNotBlank(countrySpecific_ar)) {
            params.put("country", "AR");
            params.put("description", countrySpecific_ar);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_ve = Object.getCountrySpecific_ve().getValue();

        if (StringUtils.isNotBlank(countrySpecific_ve)) {
            params.put("country", "VE");
            params.put("description", countrySpecific_ve);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }
        String countrySpecific_usa = Object.getCountrySpecific_usa().getValue();

        if (StringUtils.isNotBlank(countrySpecific_usa)) {
            params.put("country", "US");
            params.put("description", countrySpecific_usa);
            Annotation countrySpecificAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountrySpecific.class.getName(),
                    params);
            countrySpecificList.add(countrySpecificAnnot);
        }

        String countryException_es = Object.getCountryException_es().getValue();

        if (StringUtils.isNotBlank(countryException_es)) {
            params.put("country", "ES");
            params.put("description", countryException_es);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_mx = Object.getCountryException_mx().getValue();

        if (StringUtils.isNotBlank(countryException_mx)) {
            params.put("country", "MX");
            params.put("description", countryException_mx);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_co = Object.getCountryException_co().getValue();

        if (StringUtils.isNotBlank(countryException_co)) {
            params.put("country", "CO");
            params.put("description", countryException_co);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_cl = Object.getCountryException_cl().getValue();

        if (StringUtils.isNotBlank(countryException_cl)) {
            params.put("country", "CL");
            params.put("description", countryException_cl);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_pe = Object.getCountryException_pe().getValue();

        if (StringUtils.isNotBlank(countryException_pe)) {
            params.put("country", "PE");
            params.put("description", countryException_pe);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_ar = Object.getCountryException_ar().getValue();

        if (StringUtils.isNotBlank(countryException_ar)) {
            params.put("country", "AR");
            params.put("description", countryException_ar);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_ve = Object.getCountryException_ve().getValue();

        if (StringUtils.isNotBlank(countryException_ve)) {
            params.put("country", "VE");
            params.put("description", countryException_ve);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        String countryException_usa = Object.getCountryException_usa().getValue();

        if (StringUtils.isNotBlank(countryException_usa)) {
            params.put("country", "US");
            params.put("description", countryException_usa);
            Annotation countryExceptionAnnot = ReflectionUtils.createStringValuesAnnotation(cpool,
                    CountryException.class.getName(),
                    params);
            countryExceptionList.add(countryExceptionAnnot);
        }

        Annotation countrySpecificsAnnot = ReflectionUtils.createAnnotationsAnnotation(countrySpecificList,
                CountrySpecifics.class.getSimpleName(),
                cpool);
        Annotation countryExceptionsAnnot = ReflectionUtils.createAnnotationsAnnotation(countryExceptionList,
                CountryExceptions.class.getSimpleName(),
                cpool);

        List<Annotation> annotations = new ArrayList<>();
        if (elementDescriptionsAnnot != null) {
            annotations.add(elementDescriptionsAnnot);
        }
        annotations.add(apiModelPropertyAnnot);
        annotations.add(attributeTypeAnnot);
        if (bindingDefinitionsAnnot != null) {
            annotations.add(bindingDefinitionsAnnot);
        }
        if (countrySpecificsAnnot != null) {
            annotations.add(countrySpecificsAnnot);
        }
        if (countryExceptionsAnnot != null) {
            annotations.add(countryExceptionsAnnot);
        }
        attr.setAnnotations(annotations.toArray(new Annotation[annotations.size()]));

        field.getFieldInfo().addAttribute(attr);
    }

*/


    public String createPackage(String packageName)
            throws CannotCompileException {
        pool.makePackage(pool.getClassLoader(), packageName);
        return packageName;
    }

}
