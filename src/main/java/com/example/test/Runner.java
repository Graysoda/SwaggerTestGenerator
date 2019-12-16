package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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

        ArrayList<String> operationClasses = generateOperationClasses(info.getString("title"), root.getString("basePath"), root.getString("host"), paths);
        ArrayList<String> testClasses = generateTestClasses(paths,info.getString("title").replace(" ", ""), objectData);
        ArrayList<String> commonObjects = generateCommonObjects(objectData);
        ArrayList<String> requestClasses = generateRequestClasses(paths);
        ArrayList<String> responseClasses = generateResponseClasses(paths);

//        objectData.forEach( (variableName, fields) -> System.out.println(variableName + " = " + fields.toString()));
//        System.out.println(commonObjects.toString());
//        System.out.println(requestClasses.toString());
//        System.out.println(responseClasses.toString());
//        System.out.println(operationClasses.toString());
//        System.out.println(testClasses.toString());

        writeToFiles(operationClasses, testClasses, commonObjects, requestClasses, responseClasses);
    }

    private static void writeToFiles(ArrayList<String> operationClasses, ArrayList<String> testClasses, ArrayList<String> commonObjects, ArrayList<String> requestClasses, ArrayList<String> responseClasses) throws IOException {
        String path = System.getProperty("user.home") + "/Desktop/testOutput/";

        File file = new File(path);

        if (file.mkdir() || file.exists()){
            String operationPath = path + "operationClasses/";
            file = new File(operationPath);

            if (file.mkdir() || file.exists()){

                String propertyFile = operationClasses.get(0);
                operationClasses.remove(0);

                file = new File(operationPath + propertyFile.substring(propertyFile.indexOf("_") + 1, propertyFile.indexOf(".")) + ".properties");

                if (file.createNewFile()){
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile()));
                    outputStream.write(propertyFile.getBytes());
                    outputStream.flush();
                    outputStream.close();
                }

                writeArrayToFiles(operationClasses, operationPath);
            }

            String testClassesPath = path + "testClasses/";
            file = new File(testClassesPath);

            if (file.mkdir() || file.exists()){
                writeArrayToFiles(testClasses, testClassesPath);
            }

            String commonObjectsPath = path + "commonObjects/";
            file = new File(commonObjectsPath);

            if (file.mkdir() || file.exists()){
                writeArrayToFiles(commonObjects, commonObjectsPath);
            }

            String requestClassPath = path + "requestClasses/";
            file = new File(requestClassPath);

            if (file.mkdir() || file.exists()){
                writeArrayToFiles(requestClasses, requestClassPath);
            }

            String responseClassPath = path + "responseClasses/";
            file = new File(responseClassPath);

            if (file.mkdir() || file.exists()){
                writeArrayToFiles(responseClasses, responseClassPath);
            }
        }
    }

    private static void writeArrayToFiles(ArrayList<String> classes, String path) throws IOException {
        for (String s : classes){
            String fileName = path + s.split(" ")[2] + ".java";;

            if (fileName.contains("Enum")){
                System.out.println("enum");
                String temp = s.split(" ")[2];
                fileName = path + temp.substring(0, temp.indexOf("{")) + ".java";
            }
            File file = new File(fileName);
            System.out.println(fileName);
            if (file.createNewFile()){
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
                outputStream.write(s.getBytes());
                outputStream.flush();
                outputStream.close();
            }
        }
    }

    private static ArrayList<String> generateTestClasses(JSONObject paths, String serviceName, Map<String, List<Pair<String, String>>> objectData) {
        ArrayList<String> testClasses = new ArrayList<>();

        for (String endpointPath : paths.keySet()){
            JSONObject endpointRqTypes = paths.getJSONObject(endpointPath);

            for (String rqType : endpointRqTypes.keySet()){
                JSONObject rqSpecs = endpointRqTypes.getJSONObject(rqType);
                StringBuilder testClassBuilder = new StringBuilder();

                if (rqSpecs.getJSONObject("responses").keySet().contains("200")){
                    // start of class declaration
                    testClassBuilder.append("public class Test").append(rqSpecs.getString("operationId")).append("_Positive_Rest extends ").append(serviceName).append("BaseTest {\n");
                    // annotations for setup method
                    testClassBuilder.append("\n\t@Override\n\t@BeforeMethod(alwaysRun = true)\n\t@Parameters(\"environment\")\n");
                    // basic setup method
                    testClassBuilder.append("\tpublic void setup(String environment) {\n");
                    testClassBuilder.append("\t\tsetEnvironment(environment);\n");
                    testClassBuilder.append("\t}\n");

                    // basic test method
                    testClassBuilder.append("\n\t@Test()\n");
                    testClassBuilder.append("\tpublic void test").append(rqSpecs.getString("operationId")).append("_Positive_Rest() {\n");

                    testClassBuilder.append(generateTestParameters(rqSpecs.getJSONArray("parameters"), objectData));

                    testClassBuilder.append("\n\t\tRestResponse response = ").append(serviceName).append("Rest.").append(uncapitalize(serviceName)).append("(environment).").append(rqSpecs.getString("operationId"))
                            .append("();\n");

                    // end of test method
                    testClassBuilder.append("\t}\n");

                    // end of class
                    testClassBuilder.append("}\n");

                    testClasses.add(testClassBuilder.toString());

                    testClassBuilder = new StringBuilder();
                }

                // start of class declaration
                testClassBuilder.append("public class Test").append(rqSpecs.getString("operationId")).append("_Negative_Rest extends ").append(serviceName).append("BaseTest {\n");
                // annotations for setup method
                testClassBuilder.append("\n\t@Override\n\t@BeforeMethod(alwaysRun = true)\n\t@Parameters(\"environment\")\n");
                // basic setup method
                testClassBuilder.append("\tpublic void setup(String environment) {\n");
                testClassBuilder.append("\t\tsetEnvironment(environment);\n");
                testClassBuilder.append("\t}\n");

                // basic test method
                testClassBuilder.append("\n\t@Test()\n");
                testClassBuilder.append("\tpublic void test").append(rqSpecs.getString("operationId")).append("_Negative_Rest() {\n");

                testClassBuilder.append(generateTestParameters(rqSpecs.getJSONArray("parameters"), objectData));

                testClassBuilder.append("\n\t\tRestResponse response = ").append(serviceName).append("Rest.").append(uncapitalize(serviceName)).append("(environment).").append(rqSpecs.getString("operationId"))
                        .append("();\n");

                // end of test method
                testClassBuilder.append("\t}\n");


                // end of class
                testClassBuilder.append("}\n");

                testClasses.add(testClassBuilder.toString());


                testClasses.add(testClassBuilder.toString());
            }
        }

        return testClasses;
    }

    private static String generateTestParameters(JSONArray parameters, Map<String, List<Pair<String, String>>> objectData){
        StringBuilder parameterBuilder = new StringBuilder();
        for (Object parameter : parameters){
            if (parameter instanceof JSONObject){
                if (((JSONObject) parameter).has("schema")){
                    String type = extractDataType(((JSONObject) parameter).getJSONObject("schema"));
                    List<Pair<String, String>> fields;

                    if (type.contains("<")){
                        fields = objectData.get(getListType(type));
                    } else {
                        fields = objectData.get(type);
                    }

                    if (type.contains("<")){
                        parameterBuilder.append("\t\t").append(type).append(" ").append(getListType(type).toLowerCase()).append("s = new ArrayList<>();\n");
                    } else {
                        parameterBuilder.append("\t\t").append(type).append(" ").append(type.toLowerCase()).append(" = new ").append(type).append("();\n");
                    }

                    for (Pair<String, String> field : fields){
                        if (field.getValue().contains("<")){
                            parameterBuilder.append("\t\t").append(field.getValue()).append(" ").append(field.getKey()).append(" = new ArrayList<>();\n");
                        } else {
                            parameterBuilder.append("\t\t").append(field.getValue()).append(" ").append(field.getKey()).append(";\n");
                        }
                    }

                } else {
                    String type = extractDataType((JSONObject) parameter);

                    if (type.contains("{")){
                        // removes any enum stuff since they'll need to be converted to strings anyway
                        type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                    }

                    if (type.contains("<")){
                        parameterBuilder.append("\t\t").append(type).append(" ").append(getListType(type).toLowerCase()).append("s = new ArrayList<>();\n");
                    } else {
                        parameterBuilder.append("\t\t").append(type).append(" ").append(type.toLowerCase()).append(";\n");
                    }
                }
            }
        }

        return parameterBuilder.toString();
    }

    private static String getListType(String type) {
        return type.substring(type.indexOf("<") + 1, type.indexOf(">"));
    }

    private static ArrayList<String> generateOperationClasses(String serviceName, String basePath, String host, JSONObject paths) throws Exception {
        ArrayList<String> operationClasses = new ArrayList<>();

        operationClasses.add(generatePropertiesFile(serviceName, host));
        operationClasses.add(generateBaseTestClass(serviceName));
        operationClasses.add(generateBaseRestClass(serviceName));
        operationClasses.add(generateServiceClass(serviceName, basePath));
        operationClasses.add(generateServerInterfaceClass(serviceName, paths));

        return operationClasses;
    }

    private static String generateBaseRestClass(String serviceName) {
        return "public class " + serviceName.replace(" ", "") + "Rest {\n" +
                "\tpublic static " + serviceName.replace(" ", "") + "Service " + uncapitalize(serviceName.replace(" ", "")) + "Service(String environment) {\n" +
                "\t\tRestService = new RestService(environment);\n" +
                "\t\treturn new " + serviceName.replace(" ", "") + "Service(restService);\n" +
                "\t}\n";
    }

    private static String generatePropertiesFile(String serviceName, String host) {
        return "REST_" + serviceName.replace(" ", "") + ".latest.endpoint=http://" + host;
    }

    private static String generateServiceClass(String serviceName, String basePath) {
        return "public class " + serviceName.replace(" ", "") + "Service {\n" +
                // global variables
                "\tprivate RestService restService;\n" +
                "\tprivate String resource = \"" + basePath+ "\";\n" +
                "\tprivate final static String strProperties = \"properties/" + makePropertyFileName(serviceName) + "\";\n" +
                "\n" +
                // constructor
                "\tpublic " + serviceName.replace(" ", "") + "(RestService restService) {\n" +
                "\t\tthis.restService = restService;\n" +
                "\t\tthis.restService.setMainResource(\"REST_" + serviceName.replace(" ", "") + "Service\");\n" +
                "}\n" +
                "\n" +
                // method to get the server interface class
                "\tpublic " + serviceName.replace(" ", "") + " " + uncapitalize(serviceName.replace(" ", "")) + "() {\n" +
                "\t\treturn new " + serviceName.replace(" ", "") + "(restService, resource);\n" +
                "\t}\n" +
                "}";
    }

    private static String uncapitalize(String replace) {
        return replace.substring(0,1).toLowerCase() + replace.substring(1);
    }

    private static String makePropertyFileName(String serviceName) {
        return serviceName.toLowerCase().replace(" ", "-") + ".properties";
    }

    private static String generateBaseTestClass(String serviceName) {
        return "public class " + serviceName.replace(" ", "") + "BaseTest extends BaseTest {\n" +
                "\n}";
    }

    public static String generateServerInterfaceClass(String title, JSONObject paths) throws Exception {
        StringBuilder classBuilder = new StringBuilder();
        String serviceName = title.replace(" ", "");
        String commonPartOfMethodDeclaration = "\n\tpublic RestResponse ";

        // start class declaration
        classBuilder.append("public class ").append(serviceName).append(" extends ").append(serviceName).append("BaseTest {\n");

        // global variable declaration
        classBuilder.append("\tprivate RestService restService;\n");
        classBuilder.append("\tprivate String resource;\n");

        // constructor declaration
        classBuilder.append("\n\tpublic ").append(serviceName).append("(RestService restService, String resource){\n");
        classBuilder.append("\t\tthis.restService = restService;\n");
        classBuilder.append("\t\tthis.resource = resource;\n");
        classBuilder.append("\t}\n");

        for (String endpointPath : paths.keySet()){
            JSONObject endpointRqType = paths.getJSONObject(endpointPath);

            for (String rqType : endpointRqType.keySet()){
                JSONObject endpointOpSpec = endpointRqType.getJSONObject(rqType);
                String opId = endpointOpSpec.getString("operationId");
                StringBuilder methodCode = new StringBuilder();

                // start method signature for endpoint operation
                classBuilder.append(commonPartOfMethodDeclaration).append(opId).append("(Map<String, String> headers, ");

                // for loop to add any custom headers passed
                methodCode.append("\t\tfor(String header : headers.keySet()) {\n");
                methodCode.append("\t\t\trestServices.addCustomHeader(header, headers.get(header));\n");
                methodCode.append("\t\t}\n");

                for (Object parameterSpec : endpointOpSpec.getJSONArray("parameters"))
                {
                    if (parameterSpec instanceof JSONObject)
                    {
                        String in = ((JSONObject) parameterSpec).getString("in");

                        if (in.equals("body"))
                        {
                            classBuilder.append(capitalize(opId)).append("Request request, ");

                            methodCode.append("\t\tString json = restService.getJsonFromObject(request);\n");
                        }
                        else if (in.equals("path") || in.equals("query"))
                        {
                            char delimiter = ';';
//                            if (((JSONObject) parameterSpec).has("style")){
//                                // TODO handle the style (it changes the delimiting character)
//                            }
                            String type = ((JSONObject) parameterSpec).has("schema") ? extractDataType(((JSONObject) parameterSpec).getJSONObject("schema")) : extractDataType((JSONObject) parameterSpec);

                            if (type.contains("{")){
                                // removes any enum stuff since they'll need to be converted to strings anyway
                                type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                            }

                            classBuilder.append(type).append(" ").append(((JSONObject) parameterSpec).getString("name")).append(", ");

                            if (type.contains("<"))
                            {
                                // gets the type of object the list contains
                                String listType = type.substring(type.indexOf("<")+1, type.indexOf(">"));

                                if (!methodCode.toString().contains("\t\tString path = \"\";\n")){
                                    methodCode.append("\t\tString path = \"\";\n");
                                }

                                // for loop to make a string containing the values of the variables to be appended to the path
                                methodCode.append("\t\tfor(").append(listType).append(" ").append(listType.toLowerCase()).append(" : ").append(((JSONObject) parameterSpec).getString("name")).append(") {\n");
                                methodCode.append("\t\t\tpath = path + ").append(listType.toLowerCase()).append(" + \"").append(delimiter).append("\";\n");
                                methodCode.append("\t\t}\n");
                            }
                        }
                    } else {
                        throw new RuntimeException("parameter not a JsonObject");
                    }
                }
                // remove last comma from parameters in method signature
                int i = classBuilder.lastIndexOf(", ");
                classBuilder.deleteCharAt(i);
                // end method signature
                classBuilder.append(") {\n");

                // method logic
                classBuilder.append(methodCode.toString());

                // return line
                classBuilder.append("\t\treturn restService.send").append(capitalize(rqType)).append("Request(resource")
                        .append(" + \"")
                        // logic for parsing the variable name out of the path
                        .append(endpointPath.contains("{") ? endpointPath.replace("{", "\" + ").replace("}", "")
                                : endpointPath + "\"");

                if (methodCode.toString().contains("\t\tString path = \"\";\n") && methodCode.toString().contains("\t\tString json = restService.getJsonFromObject(request);\n")){
                    classBuilder.append(" + path, HeaderType.NONE, json);\n");
                } else if (methodCode.toString().contains("\t\tString json = restService.getJsonFromObject(request);\n")) {
                    classBuilder.append(", HeaderType.NONE, json);\n");
                } else if (methodCode.toString().contains("\t\tString path = \"\";\n")){
                    classBuilder.append(" + path, HeaderType.NONE, null);\n");
                } else {
                    classBuilder.append(", HeaderType.NONE, null);\n");
                }

                classBuilder.append("\t}\n");
            }
        }

        // end class declaration
        classBuilder.append("}\n");

        return classBuilder.toString();
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

    private static ArrayList<String> generateCommonObjects(Map<String, List<Pair<String, String>>> objectData){
        ArrayList<String> commonObjectsInStringForm = new ArrayList<>();

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
