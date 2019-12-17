package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.*;

public class ImportFactory {
    public String company = "example";

    public ImportFactory(){}

    public ImportFactory(String company){
        this.company = company;
    }

    public String generateTestImportStatements(JSONObject rqSpecs, String serviceName, Map<String, List<Pair<String, String>>> objectData) {
        return "import org.testng.annotations.Test;\n" +
                "import com." + company + ".api.restServices." + serviceName + "BaseTest;\n" +
                generateImportStatements(rqSpecs.getJSONArray("parameters"), serviceName, objectData) +
                "\n";
    }

    public String generateImportStatements(JSONArray parameters, String serviceName, Map<String, List<Pair<String, String>>> objectData){
        StringBuilder stringBuilder = new StringBuilder();
        String commonObjectImport = "import com." + company + ".api.restService." + serviceName + ".commonObjects.";

        for (Object param : parameters)
        {
            if (param instanceof JSONObject)
            {
                String type;

                if (((JSONObject) param).has("schema"))
                {
                    type = extractDataType(((JSONObject) param).getJSONObject("schema"));
                    List<Pair<String, String>> fields;

                    if (type.contains("<"))
                    {
                        fields = objectData.get(getListType(type));
                        if (isNotStandardType(getListType(type)))
                        {
                            stringBuilder.append(commonObjectImport).append(getListType(type)).append(";\n");
                        }
                    }
                    else
                    {
                        fields = objectData.get(type);
                        if (isNotStandardType(type))
                        {
                            stringBuilder.append(commonObjectImport).append(type).append(";\n");
                        }
                    }

                    for (Pair<String, String> field : fields)
                    {
                        String fieldType = (field.getValue().contains("<")) ? getListType(field.getValue()) : field.getValue();

                        if (field.getValue().contains("Enum") || isNotStandardType(fieldType))
                        {
                            stringBuilder.append(commonObjectImport).append(fieldType).append(";\n");
                        }
                    }

                }
                else if (((JSONObject) param).has("type"))
                {
                    type = extractDataType((JSONObject) param);

                    if (type.contains("{"))
                    {
                        // removes any enum stuff since they'll need to be converted to strings anyway
                        type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                    }

                    if (((JSONObject) param).getString("type").equals("array") && !stringBuilder.toString().contains("import java.util.ArrayList;"))
                    {
                        stringBuilder.append("import java.util.ArrayList;\n");
                    }

                    if (type.contains("<"))
                    {
                        String listType = getListType(type);
                        if (isNotStandardType(listType))
                        {
                            stringBuilder.append(commonObjectImport).append(getListType(type)).append(";\n");
                        }
                    }
                    else if (isNotStandardType(type))
                    {
                        stringBuilder.append(commonObjectImport).append(type).append(";\n");
                    }
                }
            }
        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    public String generateResponseImportStatements(JSONObject responses, String serviceName, Map<String, List<Pair<String, String>>> objectData) {
        String commonObjectImport = "import com." + company + ".api.restService." + serviceName + ".commonObjects.";
        StringBuilder stringBuilder = new StringBuilder();

        for (String httpCode : responses.keySet())
        {
            JSONObject responseSpec = responses.getJSONObject(httpCode);
            String type;

            if (responseSpec.has("schema"))
            {
                type = extractDataType(responseSpec.getJSONObject("schema"));

                if (type.contains("<")){
                    // if type contains "Pair" then the type is "object" and contains a list of "additional properties" of unknown type
                    if (!stringBuilder.toString().contains("import java.util.ArrayList;"))
                    {
                        stringBuilder.append("import java.util.ArrayList;\n");
                    }

                    if (type.contains("Pair"))
                    {
                        if (!stringBuilder.toString().contains("import javafx.util.Pair;"))
                        {
                            stringBuilder.append("import javafx.util.Pair;\n");
                        }

                        String objectAdditionalPropertyType = type.substring(type.indexOf(",") + 1, type.indexOf(">")).trim();

                        if (isNotStandardType(objectAdditionalPropertyType))
                        {
                            stringBuilder.append(commonObjectImport).append(objectAdditionalPropertyType).append(";\n");
                        }
                    }
                    else if (isNotStandardType(getListType(type)))
                    {
                        stringBuilder.append(commonObjectImport).append(getListType(type)).append(";\n");
                    }
                }
                else if (isNotStandardType(type))
                {
                    stringBuilder.append(commonObjectImport).append(type).append(";\n");
                }
            }
            else if (responseSpec.has("type"))
            {
                type = extractDataType(responseSpec);

                if (type.contains("{"))
                {
                    // removes any enum stuff since they'll need to be converted to strings anyway
                    type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                }

                if (type.contains("<"))
                {
                    if (!stringBuilder.toString().contains("import java.util.ArrayList;"))
                    {
                        stringBuilder.append("import java.util.ArrayList;\n");
                    }

                    String listType = getListType(type);

                    if (isNotStandardType(listType))
                    {
                        stringBuilder.append(commonObjectImport).append(getListType(type)).append(";\n");
                    }
                }
                else if (isNotStandardType(type))
                {
                    stringBuilder.append(commonObjectImport).append(type).append(";\n");
                }
            }
        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    public String generateRequestImportStatements(JSONArray parameters, String serviceName, Map<String, List<Pair<String, String>>> objectData) {
        String commonObjectImport = "import com." + company + ".api.restService." + serviceName + ".commonObjects.";
        StringBuilder stringBuilder = new StringBuilder();

        for (Object param : parameters)
        {
            if (param instanceof JSONObject)
            {
                String type;

                if (((JSONObject) param).has("schema"))
                {
                    type = extractDataType(((JSONObject) param).getJSONObject("schema"));

                    if (type.contains("<"))
                    {
                        if (!stringBuilder.toString().contains("import java.util.ArrayList;"))
                        {
                            stringBuilder.append("import java.util.ArrayList;\n");
                        }

                        if (type.contains("Pair"))
                        {
                            if (!stringBuilder.toString().contains("import javafx.util.Pair;"))
                            {
                                stringBuilder.append("import javafx.util.Pair;\n");
                            }

                            String objectAdditionalPropertyType = type.substring(type.indexOf(",") + 1, type.indexOf(">")).trim();

                            if (isNotStandardType(objectAdditionalPropertyType))
                            {
                                stringBuilder.append(commonObjectImport).append(objectAdditionalPropertyType).append(";\n");
                            }
                        }
                        else if (isNotStandardType(getListType(type)))
                        {
                            stringBuilder.append(commonObjectImport).append(getListType(type)).append(";\n");
                        }
                    }
                    else if (isNotStandardType(type))
                    {
                        stringBuilder.append(commonObjectImport).append(type).append(";\n");
                    }
                }
                else if (((JSONObject) param).has("type"))
                {
                    type = extractDataType((JSONObject) param);

                    if (type.contains("{"))
                    {
                        // removes any enum stuff since they'll need to be converted to strings anyway
                        type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                    }

                    if (type.contains("<"))
                    {
                        if (!stringBuilder.toString().contains("import java.util.ArrayList;"))
                        {
                            stringBuilder.append("import java.util.ArrayList;\n");
                        }

                        String listType = getListType(type);

                        if (isNotStandardType(listType))
                        {
                            stringBuilder.append(commonObjectImport).append(getListType(type)).append(";\n");
                        }
                    }
                    else if (isNotStandardType(type))
                    {
                        stringBuilder.append(commonObjectImport).append(type).append(";\n");
                    }
                }
            }
        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
