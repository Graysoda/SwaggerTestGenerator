package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Runner {
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File("C:\\Users\\David Grayson\\IdeaProjects\\swagger code gen\\src\\main\\resources\\swagger.json")));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();

        while (line != null){
            sb.append(line);
            line = reader.readLine();
        }

        reader.close();

        JSONObject root = new JSONObject(sb.toString());

        Map<String, List<Pair<String, String>>> objectData = extractDefinitionData(root.getJSONObject("definitions"));

        objectData.forEach( (variableName, fields) -> System.out.println(variableName + " = " + fields.toString()));

        List<String> classesInStringForm = generatePojos(objectData);

//        for (String s : pojosInStringForm){
//            System.out.println("*****************************************");
//            System.out.println(s);
//            System.out.println("*****************************************");
//        }

        JSONObject info = (JSONObject) root.get("info");

        String workingDirectory = System.getProperty("user.home") + File.separator +
                "Desktop" + File.separator +
                info.getString("title").replace(" ", "_").trim() + File.separator;

        File file = new File(workingDirectory);

        JSONObject paths = root.getJSONObject("paths");

        //TODO make request super class and pass name to test makers

        JSONArray endpoints = paths.names();

        for (Object endpoint : endpoints) {
            if (endpoint instanceof String){
                JSONObject path = paths.getJSONObject(endpoint.toString());

                JSONArray operations = path.names();

                for (Object op : operations){
                    String operationsDirectory = workingDirectory + File.separator +
                            ((String) endpoint).replaceFirst("/","").replaceAll("/", "_") + File.separator +
                            op.toString();

                    if ("post".equals(op)) {
                        JSONObject operation = path.getJSONObject(op.toString());
                        makePostTest(operation, "", getRequiredCommonObjects(operation, objectData));
                    } else if ("get".equals(op)){

                    } else if ("put".equals(op)){

                    } else if ("delete".equals(op)){

                    }
                }
            }
        }
    }

    private static void makePostTest(JSONObject op, String superClass, Map<String, List<Pair<String, String>>> commonObjectsRequiredForOperation) {
        StringBuilder stringBuilder = new StringBuilder();

        // import statements
        String testngAnnotationImport = "import org.testng.annotations.";
        stringBuilder.append(testngAnnotationImport).append("BeforeMethod;\n")
                .append(testngAnnotationImport).append("Parameters;\n")
                .append(testngAnnotationImport).append("Test;\n");

        // beginning of class declaration
        stringBuilder.append("public class Test").append(capitalize(op.getString("operationId"))).append("_Rest");

        // make class extend super class if one was passed
        if (superClass != null && !superClass.isEmpty()){
            stringBuilder.append(" extends ").append(superClass).append("{\n");
        } else {
            stringBuilder.append("{\n");
        }

        // annotations for the setup method
        stringBuilder.append("\t@Override\n\t@BeforeMethod(alwaysRun = true)\n\t@Parameters(\"environment\")\n");

        // basic setup method
        stringBuilder.append("\tpublic void setup(String environment) {\n\t\tsetEnvironment(environment);\n\t}\n\n");

        // beginning of test method
        stringBuilder.append("\t@Test()\n\tpublic void test").append(op.getString("operationId")).append("_Rest(){\n");

        // TODO variables for the test
        stringBuilder.append("\t\t");
    }

    private static Map<String, List<Pair<String, String>>> getRequiredCommonObjects(JSONObject operation, Map<String, List<Pair<String, String>>> objectData){
        Map<String, List<Pair<String, String>>> commonObjectsRequiredForOperation = new HashMap<>();

        for (Object o : operation.getJSONArray("parameters")){
            if (o instanceof JSONObject){
                String commonObjectName = extractDataType(((JSONObject) o).getJSONObject("schema"));
                commonObjectsRequiredForOperation.put(commonObjectName, objectData.get(commonObjectName));
            }
        }

        return commonObjectsRequiredForOperation;
    }

    private static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private static List<String> generatePojos(Map<String, List<Pair<String, String>>> objectData){
        List<String> pojosInStringForm = new ArrayList<>();

        for (String name : objectData.keySet()){
            List<Pair<String, String>> fields = objectData.get(name);
            StringBuilder stringBuilder = new StringBuilder();

            // extracts the enum values
            for (Pair<String, String> pair : fields){
                if (pair.getValue().contains("{")){
                    //grabs the comma delimited enum values specified in the swagger
                    String[] enumValues = pair.getValue().substring(pair.getValue().indexOf("{"), pair.getValue().indexOf("}")+1)
                            .replace("{","").replace("}","").split(",");
                    String enumName = name + capitalize(pair.getKey()); // Capitalizes the name of the variable the enum is named after
                    String type = pair.getValue().substring(0, pair.getValue().indexOf("{"));
                    String fieldName = pair.getKey();

                    stringBuilder.append("public enum ").append(enumName).append("{\n");

                    // write the enum values
                    for (String value : enumValues){
                        stringBuilder.append("\t").append(value.toUpperCase()).append("(\"").append(value).append("\"),\n");
                    }

                    int i = stringBuilder.lastIndexOf(",");

                    stringBuilder.deleteCharAt(i); // removes the last comma from the list of enum values
                    stringBuilder.insert(i, ";\n");

                    stringBuilder.append("\tprivate ").append(type)
                            .append(" ").append(fieldName).append(";\n\n"); //the string value for the enum

                    // enum constructor
                    stringBuilder.append("\t")
                            .append(enumName)
                            .append("(").append(type).append(" ").append(fieldName).append("){\n")
                            .append("\t\tthis.").append(fieldName).append(" = ").append(fieldName).append(";\n\t}\n}\n\n");

                    // replace the Key from the Pair to be the newly created Enum
                    fields.set(fields.indexOf(pair), new Pair<>(fieldName, enumName));

                    pojosInStringForm.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }
            }

            // start of pojo class
            stringBuilder.append("public class ").append(name).append(" {\n");

            // declaring global variables
            for (Pair<String, String> pair : fields){
                stringBuilder.append("\tprivate ").append(pair.getValue()).append(" ").append(pair.getKey()).append(";\n");
            }

            // empty constructor
            stringBuilder.append("\t").append(name).append("(){}").append("\n\n");

            // constructor with all fields
            stringBuilder.append("\t").append(name).append("(");

            // declaring the fields in the constructor method signature
            for (Pair<String, String> field : fields) {
                stringBuilder.append(field.getValue())
                        .append(" ")
                        .append(field.getKey())
                        .append(",");
            }

            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(",")) // deletes the last ", " from the above loop
                    .append("){\n");

            // setting the classes globals to the constructor values
            for (Pair<String, String> pair : fields){
                stringBuilder.append("\t\tthis.").append(pair.getKey()).append(" = ").append(pair.getKey()).append(";\n");
            }

            // end of the full constructor
            stringBuilder.append("\t}\n\n");

            // make getters
            for (Pair<String, String> pair : fields){
                String varName = capitalize(pair.getKey());
                stringBuilder.append("\tpublic ").append(pair.getValue()) // return type
                        .append(" get").append(varName).append("(){\n") // name of getter
                        .append("\t\treturn this.").append(pair.getKey()).append(";\n\t}\n\n"); // return statement
            }

            // make setters
            for (Pair<String, String> pair : fields){
                String varName = capitalize(pair.getKey());
                stringBuilder.append("\tpublic void set").append(varName) // name of setter
                        .append("(").append(pair.getValue()).append(" ").append(pair.getKey()).append("){\n") // method parameter
                        .append("\t\tthis.").append(pair.getKey()).append(" = ").append(pair.getKey()).append("\n\t}\n\n"); // actual setting of the field
            }

            stringBuilder.append("}"); // last curly brace

            pojosInStringForm.add(stringBuilder.toString());
        }

        return pojosInStringForm;
    }

    private static Map<String, List<Pair<String, String>>> extractDefinitionData(JSONObject definitions) {
        Map<String, List<Pair<String, String >>> pojos = new HashMap<>();
        for (Object name : definitions.names()) {
            if (name instanceof String){
                JSONObject jsonProperties = definitions.getJSONObject((String) name).getJSONObject("properties");
                List<Pair<String, String>> fields = new ArrayList<>();

                for (Object o : jsonProperties.names()) {
                    if (o instanceof String){
                        JSONObject jsonPropertyType = jsonProperties.getJSONObject((String) o);
                        fields.add(new Pair<>(o.toString(), extractDataType(jsonPropertyType)));
                    }
                }

                pojos.put(name.toString(), fields);
            }
        }
        return pojos;
    }

    private static String extractDataType(JSONObject jsonPropertyType) {
        String dataType = "";

        if (jsonPropertyType.has("type")){

            // Long and Integer cases
            if (jsonPropertyType.getString("type").equals("integer")){
                if (jsonPropertyType.getString("format").equals("int64")){
                    dataType = "Long";
                } else if (jsonPropertyType.getString("format").equals("int32")){
                    dataType = "Integer";
                }
            }

            // Boolean case
            if (jsonPropertyType.getString("type").equals("boolean")){
                dataType = "Boolean";
            }

            // String case
            if(jsonPropertyType.getString("type").equals("string")){
                dataType = "String";
            }

            // Array case
            if (jsonPropertyType.getString("type").equals("array")){
                dataType = "ArrayList<" + extractDataType(jsonPropertyType.getJSONObject("items")) + ">";
            }

            // Enum check
            if (jsonPropertyType.has("enum")){
                StringBuilder dataTypeBuilder = new StringBuilder(dataType + "{");

                for (Object s : jsonPropertyType.getJSONArray("enum")) {
                    if (s instanceof String){
                        dataTypeBuilder.append(s.toString()).append(",");
                    }
                }
                dataTypeBuilder.deleteCharAt(dataTypeBuilder.lastIndexOf(","));

                dataTypeBuilder.append("}");

                dataType = dataTypeBuilder.toString();
            }
        }

        // $ref case
        if (jsonPropertyType.has("$ref") && !jsonPropertyType.has("type")){
            String[] ref = jsonPropertyType.getString("$ref").split("/");
            dataType = ref[ref.length-1];
        }

        return dataType;
    }
}
