package com.araguacaima.braas.core.drools;

import com.araguacaima.braas.core.*;
import com.araguacaima.commons.utils.JsonUtils;
import com.sun.codemodel.JCodeModel;
import javassist.CtClass;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class T {

    private RulesBaseToClasses rulesBaseToClasses = new RulesBaseToClasses();
    private List<String> defaultClasses = Arrays.asList(
            RuleLogging.class.getName(),
            RuleMessageInfo.class.getName(),
            RuleMessageWarning.class.getName(),
            RuleMessageError.class.getName(),
            RuleMessageSuccess.class.getName());
    private Map<String, CtClass> createdClasses = new LinkedHashMap<>();
    private static final JsonUtils jsonUtils = new JsonUtils();


 /*   @Test
    public void test(String pathname) throws IOException {
        createdClasses.clear();
        if (StringUtils.isBlank(pathname)) {
            pathname = "D:\\Alex\\Downloads\\braas-rules.xlsx";
        } else {
            File file = Commons.findFile(pathname);
            pathname = file.getCanonicalPath();
        }
        try {
            System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.CommonsLogger");
            FileInputStream file = new FileInputStream(new File(pathname));
            String prefix = "pkg-";
            String suffix = "-" + Calendar.getInstance().getTimeInMillis();
            //Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);

            //Get first/desired sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(0);
            //Iterate through each rows one by one
            rulesBaseToClasses.clear();
            Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String value = cell.getStringCellValue().trim();
                    if (StringUtils.isNotBlank(value)) {
                        if (value.startsWith("RuleSet")) {
                            processRuleSet(prefix, suffix, cellIterator.next());
                        } else if (value.startsWith("Import")) {
                            createClasses(cellIterator.next().getStringCellValue());
                        } else if (value.startsWith("RuleTable")) {
                            processRuleTable(rowIterator);
                        }
                    }
                }
            }
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRuleTable(Iterator<Row> rowIterator) throws Exception {
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            //For each row, iterate through all the columns
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String value = cell.getStringCellValue().trim();
                if (StringUtils.isNotBlank(value)) {
                    if (value.startsWith("RuleTable")) {
                        processRuleTable(rowIterator);
                    } else {
                        if (value.equalsIgnoreCase("Condition")) {
                            processCondition(rowIterator);
                        }
                    }
                }
            }
        }
    }

    private void processCondition(Iterator<Row> rowIterator) throws Exception {
        Row row = rowIterator.next();
        //For each row, iterate through all the columns
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String value = cell.getStringCellValue().trim();
            Set<String> classNames = createdClasses.keySet();
            for (String className : classNames) {
                String classNameShort = (new StringTokenizer(className)).
                        Pattern pattern = Pattern.compile("\\$(\\w*):" + className);
                Matcher matcher = pattern.matcher(value);
                if (matcher.matches()) {
                    processFields(createdClasses.get(className), rowIterator);
                }
            }
            ;
        }
    }

    private void processFields(CtClass ctClass, Iterator<Row> rowIterator) throws Exception {
        Row row = rowIterator.next();
        //For each row, iterate through all the columns
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String value = cell.getStringCellValue().trim();
            Pattern pattern = Pattern.compile("\\$(\\w*):");
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                rulesBaseToClasses.addField(ctClass, value, new Object(), null);
            }
        }
    }

    private void processRuleSet(String prefix, String suffix, Cell cell) throws CannotCompileException {
        String packageName;
        packageName = prefix + cell.getStringCellValue() + suffix;
        rulesBaseToClasses.createPackage(packageName);
    }

    private void createClasses(String value) throws
            CannotCompileException, IOException, ClassNotFoundException {
        String[] classes = StringUtils.split(value, ",");
        for (String class_ : classes) {
            String trimmedClass = class_.trim();
            if (!defaultClasses.contains(trimmedClass)) {
                CtClass createdClass = rulesBaseToClasses.createClass(trimmedClass);
                rulesBaseToClasses.storeClass(createdClass);
                createdClasses.put(createdClass.getName(), createdClass);
            }
        }
    }

*/

}
