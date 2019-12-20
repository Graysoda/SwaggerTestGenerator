package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.*;

public class RequestFactory {
    private String company = "example";
    private ImportFactory importFactory;

    public RequestFactory(){
        importFactory = new ImportFactory();
    }

    public RequestFactory(String company){
        this.company = company;
        importFactory = new ImportFactory(company);
    }

    public ArrayList<String> generateRequestClasses(JSONObject paths, Map<String, List<Pair<String, String>>> objectData, String serviceName) throws Exception {
        ArrayList<String> requestClasses = new ArrayList<>();

        for (String s : paths.keySet()){
            JSONObject endpointRQType = paths.getJSONObject(s);

            for (String rqType : endpointRQType.keySet()){
                JSONObject rqSpecifications = endpointRQType.getJSONObject(rqType);
                StringBuilder classStructureStringBuilder = new StringBuilder();
                StringBuilder fieldAccessors = new StringBuilder();

                JSONArray parameters = rqSpecifications.has("parameters") ? rqSpecifications.getJSONArray("parameters") : new JSONArray();

                if (!parameters.toList().isEmpty()){
                    // import statements
                    classStructureStringBuilder.append(importFactory.generateRequestImportStatements(parameters, serviceName));

                    //first line of the class declaration
                    classStructureStringBuilder.append("public class ").append(capitalize(rqSpecifications.getString("operationId"))).append("Request {\n");
                }

                //extracting parameters for endpoint operation from "parameters" object in swagger json
                for (Object param : parameters){
                    if (param instanceof JSONObject){
                        String name = makeCamelCase(((JSONObject) param).getString("name"));
                        classStructureStringBuilder.append("\t").append("private ");

                        String type = "";
                        if (((JSONObject) param).has("schema")){
                            type = extractDataType(((JSONObject) param).getJSONObject("schema"));
                        } else {
                            type = extractDataType((JSONObject) param);
                        }

                        if (type.contains("{")){
                            type = type.substring(0, type.indexOf("{")) + ">";
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
}
