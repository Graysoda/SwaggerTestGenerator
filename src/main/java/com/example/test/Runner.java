package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Runner {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File(Runner.class.getClassLoader().getResource("swagger.json").toURI())));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();

        while (line != null){
            sb.append(line);
            line = reader.readLine();
        }

        reader.close();

        JSONObject root = new JSONObject(sb.toString());

        Map<String, List<Pair<String, String>>> objectData = extractDefinitionData(root.getJSONObject("definitions"));

        JSONObject info = (JSONObject) root.get("info");

        JSONObject paths = root.getJSONObject("paths");

        //TODO make operation super class

        List<String> commonObjects = generateCommonObjects(objectData);
        ArrayList<String> requestClasses = generateRequestClasses(paths);
        ArrayList<String> responseClasses = generateResponseClasses(paths);

        objectData.forEach( (variableName, fields) -> System.out.println(variableName + " = " + fields.toString()));
        System.out.println(requestClasses.toString());
        System.out.println(responseClasses.toString());
    }

    private static ArrayList<String> generateResponseClasses(JSONObject paths) throws Exception {
        ArrayList<String> responseClasses = new ArrayList<>();

        for (String s : paths.keySet()){
            JSONObject endpointRQType = paths.getJSONObject(s);

            for (String rqType : endpointRQType.keySet()){
                JSONObject rqSpecifications = endpointRQType.getJSONObject(rqType);

                if (rqSpecifications.has("responses")){
                    JSONObject responses = rqSpecifications.getJSONObject("responses");

                    for (String httpResponseCode : responses.keySet()){
                        JSONObject response = responses.getJSONObject(httpResponseCode);

                        if (response.has("schema")){
                            String type = extractDataType(response.getJSONObject("schema"));
                            StringBuilder classStructureStringBuilder = new StringBuilder();
                            StringBuilder fieldAccessors = new StringBuilder();

                            //first line of the class declaration
                            classStructureStringBuilder.append("public class ").append(capitalize(rqSpecifications.getString("operationId"))).append("Response {\n");

                            classStructureStringBuilder.append("\t").append("private ").append(type).append(" ");

                            if (type.contains("<")){
                                String name = type.replace("<", "").replace(">", "").replace(", ", "");
                                classStructureStringBuilder.append(name).append(";\n");
                                fieldAccessors.append(generateAccessors(type, name));
                            } else {
                                classStructureStringBuilder.append(type.toLowerCase()).append(";\n");
                                fieldAccessors.append(generateAccessors(type, type.toLowerCase()));
                            }

                            classStructureStringBuilder.append(fieldAccessors.toString());
                            classStructureStringBuilder.append("}");
                            responseClasses.add(classStructureStringBuilder.toString());
                        }
                    }

                } else {
                    throw new Exception("No response defined for " + s + " " + rqType);
                }
            }
        }

        return responseClasses;
    }

    private static ArrayList<String> generateRequestClasses(JSONObject paths) {
        ArrayList<String> requestClasses = new ArrayList<>();

        for (String s : paths.keySet()){
            JSONObject endpointRQType = paths.getJSONObject(s);

            for (String rqType : endpointRQType.keySet()){
                JSONObject rqSpecifications = endpointRQType.getJSONObject(rqType);
                StringBuilder classStructureStringBuilder = new StringBuilder();
                StringBuilder fieldAccessors = new StringBuilder();

                JSONArray parameters = rqSpecifications.getJSONArray("parameters");

                if (!parameters.toList().isEmpty()){
                    //first line of the class declaration
                    classStructureStringBuilder.append("public class ").append(capitalize(rqSpecifications.getString("operationId"))).append("Request {\n");
                }

                //extracting parameters for endpoint operation from "parameters" object in swagger json
                for (Object param : parameters){
                    if (param instanceof JSONObject){
                        String name = ((JSONObject) param).getString("name");
                        classStructureStringBuilder.append("\t").append("private ");

                        String type = "";
                        if (((JSONObject) param).has("schema")){
                            type = extractDataType(((JSONObject) param).getJSONObject("schema"));
                        } else {
                            type = extractDataType((JSONObject) param);
                        }

                        if (type.contains("{")){
                            type = type.substring(0, type.indexOf("{"));
                        }

                        classStructureStringBuilder.append(type).append(" ");

                        classStructureStringBuilder.append(name).append(";\n");
                        fieldAccessors.append(generateAccessors(type, name));
                    } else {
                        throw new JSONException("Invalid format in parameter array of " + s + " " + rqType);
                    }
                }

                if (!parameters.toList().isEmpty()){
                    classStructureStringBuilder.append(fieldAccessors.toString());
                    classStructureStringBuilder.append("}");
                    requestClasses.add(classStructureStringBuilder.toString());
                }
            }
        }
        return requestClasses;
    }

    private static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private static List<String> generateCommonObjects(Map<String, List<Pair<String, String>>> objectData){
        List<String> commonObjectsInStringForm = new ArrayList<>();

        for (String name : objectData.keySet()){
            List<Pair<String, String>> fields = objectData.get(name);
            StringBuilder stringBuilder = new StringBuilder();

            // extracts the enum values
            if (name.contains("Enum")){
                stringBuilder.append("public enum ").append(name).append("{\n");

                for (Pair<String, String> pair : fields){
                    stringBuilder.append(pair.getKey()).append("(\"").append(pair.getValue()).append("\"),\n");
                }

                int i = stringBuilder.lastIndexOf(",");

                stringBuilder.deleteCharAt(i);
                stringBuilder.insert(i, ";\n");

                //internal string variable for enum
                stringBuilder.append("\tprivate String s;\n");

                // enum constructor
                    stringBuilder.append("\t")
                            .append(name)
                            .append("(String s){\n")
                            .append("\t\tthis.s = s;\n\t}\n}\n\n");

            } else {
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
            }
            commonObjectsInStringForm.add(stringBuilder.toString());
        }

        return commonObjectsInStringForm;
    }

    private static Map<String, List<Pair<String, String>>> extractDefinitionData(JSONObject definitions) {
        Map<String, List<Pair<String, String >>> commonObjects = new HashMap<>();

        for (String name : definitions.keySet()) {
            JSONObject jsonProperties = definitions.getJSONObject(name).getJSONObject("properties");
            List<Pair<String, String>> fields = new ArrayList<>();

            for (String propertyName : jsonProperties.keySet()) {
                JSONObject jsonPropertyType = jsonProperties.getJSONObject(propertyName);
                String type = extractDataType(jsonPropertyType);

                // check if it's an enum value
                if (type.contains("{")){
                    String[] enumValues = type.substring(type.indexOf("{"), type.indexOf("}")+1)
                            .replace("{","").replace("}","").split(",");
                    String enumName = name + capitalize(propertyName) + "Enum";
                    List<Pair<String, String>> enumFields = new ArrayList<>();

                    for (String value : enumValues){
                        enumFields.add(new Pair<>(value.toUpperCase(), value));
                    }

                    commonObjects.put(enumName, enumFields);

                    fields.add(new Pair<>(propertyName,enumName));
                } else {
                    fields.add(new Pair<>(propertyName, type));
                }
            }
            commonObjects.put(name, fields);
        }
        return commonObjects;
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

            // Object check
            // I'm assuming that all objects will be a container around a list of their "additionalProperties"
            // which can be represented as a Pair<String, additionalPropertyType>
            if (jsonPropertyType.getString("type").equals("object")){
                dataType = "ArrayList<Pair<String, " + extractDataType(jsonPropertyType.getJSONObject("additionalProperties")) + ">";
            }

            // File check
            if (jsonPropertyType.getString("type").equals("file")){
                dataType = "File";
            }
        }

        // $ref case
        if (jsonPropertyType.has("$ref") && !jsonPropertyType.has("type")){
            String[] ref = jsonPropertyType.getString("$ref").split("/");
            dataType = ref[ref.length-1];
        }

        return dataType;
    }

    private static String generateAccessors(String type, String name) {
        return "\tpublic " + type + " get" + capitalize(name) + "{\n"
                + "\t\treturn " + name + ";\n"
                + "\t}\n"
                + "\tpublic void set" + capitalize(name) + "(" + type + " " + name + ") " + "{\n"
                + "\t\tthis." + name + " = " + name + "\n"
                + "\t}\n";
    }
}
