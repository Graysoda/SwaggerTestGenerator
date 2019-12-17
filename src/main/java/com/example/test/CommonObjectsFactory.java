package com.example.test;

import javafx.util.Pair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.*;

public class CommonObjectsFactory {
    private String company = "example";

    public CommonObjectsFactory(){}

    public CommonObjectsFactory(String company){
        this.company = company;
    }

    public ArrayList<String> generateCommonObjects(Map<String, List<Pair<String, String>>> objectData, String serviceName){
        ArrayList<String> commonObjectsInStringForm = new ArrayList<>();

        for (String name : objectData.keySet()){
            List<Pair<String, String>> fields = objectData.get(name);
            StringBuilder stringBuilder = new StringBuilder();

            // extracts the enum values
            if (name.contains("Enum")){
                stringBuilder.append("public enum ").append(name).append(" {\n");

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
                // import statements
                for (Pair<String, String> field : fields){
                    if (field.getValue().contains("<")){
                        if (!stringBuilder.toString().contains("import java.util.List;")){
                            stringBuilder.append("import java.util.List;\n");
                        }
                        if (isNotStandardType(getListType(field.getValue()))){
                            stringBuilder.append("import com.").append(company).append(".api.restServices.").append(serviceName).append(".commonObjects.").append(getListType(field.getValue())).append(";\n");
                        }
                    } else if (isNotStandardType(field.getValue())){
                        stringBuilder.append("import com.").append(company).append(".api.restServices.").append(serviceName).append(".commonObjects.").append(field.getValue()).append(";\n");
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
            }
            commonObjectsInStringForm.add(stringBuilder.toString());
        }

        return commonObjectsInStringForm;
    }

    public Map<String, List<Pair<String, String>>> extractDefinitionData(JSONObject definitions) {
        Map<String, List<Pair<String, String >>> commonObjects = new HashMap<>();

        for (String name : definitions.keySet()) {
            if (name.contains("Iterable")){

            } else {
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
                            enumFields.add(new Pair<>(value.toUpperCase().replace(" ", "_"), value));
                        }

                        commonObjects.put(enumName, enumFields);

                        fields.add(new Pair<>(propertyName,enumName));
                    } else {
                        fields.add(new Pair<>(propertyName, type));
                    }
                }

                commonObjects.put(name, fields);
            }
        }
        return commonObjects;
    }


}
