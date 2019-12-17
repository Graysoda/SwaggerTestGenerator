package com.example.test;

import javafx.util.Pair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.*;

public class ResponseFactory {
    private String company = "example";
    private ImportFactory importFactory;

    public ResponseFactory(){
        importFactory = new ImportFactory();
    }

    public ResponseFactory(String company){
        this.company = company;
        importFactory = new ImportFactory(company);
    }

    public ArrayList<String> generateResponseClasses(JSONObject paths, Map<String, List<Pair<String, String>>> objectData, String serviceName) throws Exception {
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

                            // import statements
                            classStructureStringBuilder.append(importFactory.generateResponseImportStatements(responses, serviceName, objectData));

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
}