package com.example.test;

import org.json.JSONObject;

public class TypeHelper {
    public static String getListType(String type) {
        return type.substring(type.indexOf("<") + 1, type.indexOf(">"));
    }

    public static String uncapitalize(String replace) {
        return replace.substring(0,1).toLowerCase() + replace.substring(1);
    }

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static boolean isNotStandardType(String field) {
        return !(field.equals("String")
                || field.equals("Integer")
                || field.equals("Boolean")
                || field.equals("Long")
                || field.equals("File"));
    }

    public static String generateAccessors(String type, String name) {
        return "\tpublic " + type + " get" + capitalize(name) + "{\n"
                + "\t\treturn " + name + ";\n"
                + "\t}\n"
                + "\tpublic void set" + capitalize(name) + "(" + type + " " + name + ") " + "{\n"
                + "\t\tthis." + name + " = " + name + "\n"
                + "\t}\n";
    }

    public static String extractDataType(JSONObject jsonPropertyType) {
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
            // which can be represented as a Pair<String(Name), additionalPropertyType>
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
}
