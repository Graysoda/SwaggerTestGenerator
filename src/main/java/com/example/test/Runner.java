package com.example.test;

import javafx.util.Pair;
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

        Map<String, List<Pair<String, String>>> pojosFromSwagger = generatePojos(root.getJSONObject("definitions"));

        for (String s : pojosFromSwagger.keySet()){
            System.out.print(s + "=[");
            for (Pair<String, String> pair : pojosFromSwagger.get(s)){
                System.out.print("<" + pair.getKey() + "," + pair.getValue() + ">,");
            }
            System.out.print("], ");
            System.out.println();
        }

//        JSONObject info = (JSONObject) root.get("info");
//
//        String workingDirectory = System.getProperty("user.home") + File.separator +
//                "Desktop" + File.separator +
//                info.getString("title").replace(" ", "_").trim() + File.separator;
//
//        File file = new File(workingDirectory);
//
//        JSONObject paths = root.getJSONObject("paths");
//
//        JSONArray endpoints = paths.names();
//
//        for (Object endpoint : endpoints) {
//            if (endpoint instanceof String){
//                JSONObject path = paths.getJSONObject(endpoint.toString());
//
//                JSONArray operations = path.names();
//
//                for (Object op : operations){
//                    String operationsDirectory = workingDirectory + File.separator +
//                            ((String) endpoint).replaceFirst("/","").replaceAll("/", "_") + File.separator +
//                            op.toString();
//
//                    if ("post".equals(op)) {
//                        TestCreator.makePostTests(path.getJSONObject(op.toString()), operationsDirectory);
//                    } else if ("get".equals(op)){
//                        TestCreator.makeGetTests(path.getJSONObject(op.toString()), operationsDirectory);
//                    } else if ("put".equals(op)){
//
//                    } else if ("delete".equals(op)){
//
//                    }
//                }
//            }
//        }
    }

    private static Map<String, List<Pair<String, String>>> generatePojos(JSONObject definitions) {
        Map<String, List<Pair<String, String >>> pojos = new HashMap<String, List<Pair<String, String>>>();
        for (Object name : definitions.names()) {
            if (name instanceof String){
                JSONObject jsonProperties = definitions.getJSONObject((String) name).getJSONObject("properties");
                List<Pair<String, String>> fields = new ArrayList<Pair<String, String>>();

                for (Object o : jsonProperties.names()) {
                    if (o instanceof String){
                        JSONObject jsonPropertyType = jsonProperties.getJSONObject((String) o);
//                        System.out.println(o);
                        fields.add(new Pair<String, String>(o.toString(), extractDataType(jsonPropertyType)));

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
//            System.out.println(jsonPropertyType.toString());
            String[] ref = jsonPropertyType.getString("$ref").split("/");
            dataType = ref[ref.length-1];
        }

//        System.out.println(dataType);

        return dataType;
    }
}
