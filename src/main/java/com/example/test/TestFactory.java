package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.*;

public class TestFactory {
    private String company = "example";
    private ImportFactory importFactory;

    public TestFactory(){
        importFactory = new ImportFactory();
    }

    public TestFactory(String company){
        this.company = company;
        importFactory = new ImportFactory(company);
    }

    public Map<String, List<String>> generateTestClasses(JSONObject paths, String serviceName, Map<String, List<Pair<String, String>>> objectData, Map<String, String> mapTagNameToNewName) {
        Map<String, List<String>> organizedTests = new HashMap<>();

        for (String endpointPath : paths.keySet())
        {
            JSONObject endpointRqTypes = paths.getJSONObject(endpointPath);

            for (String rqType : endpointRqTypes.keySet())
            {
                JSONObject rqSpecs = endpointRqTypes.getJSONObject(rqType);
                StringBuilder testClassBuilder = new StringBuilder();
                String operationId = capitalize(rqSpecs.getString("operationId"));
                String organizingTag = mapTagNameToNewName.get(rqSpecs.getJSONArray("tags").getString(0));

                if (!organizedTests.containsKey(organizingTag)){
                    organizedTests.put(organizingTag, new ArrayList<>());
                }

                if (rqSpecs.getJSONObject("responses").keySet().contains("200"))
                {
                    // import statements
                    testClassBuilder.append(importFactory.generateTestImportStatements(rqSpecs, serviceName, objectData));

                    // start of class declaration
                    testClassBuilder.append("public class Test").append(operationId).append("_Positive_Rest extends ").append(serviceName).append("BaseTest {\n");

                    // basic test method
                    testClassBuilder.append("\n\t@Test()\n");
                    testClassBuilder.append("\tpublic void test").append(operationId).append("_Positive_Rest() {\n");

                    if (rqSpecs.has("parameters"))
                    {
                        testClassBuilder.append(generateTestParameters(rqSpecs.getJSONArray("parameters"), objectData));
                    }

                    testClassBuilder.append("\n\t\tRestResponse response = ").append(serviceName).append("Rest.").append(uncapitalize(serviceName)).append("(environment).").append(uncapitalize(operationId))
                            .append("();\n");

                    // end of test method
                    testClassBuilder.append("\t}\n");

                    // end of class
                    testClassBuilder.append("}\n");

                    organizedTests.get(organizingTag).add(testClassBuilder.toString());

                    testClassBuilder = new StringBuilder();
                }
                // import statements
                testClassBuilder.append(importFactory.generateTestImportStatements(rqSpecs, serviceName, objectData));
                // start of class declaration
                testClassBuilder.append("public class Test").append(operationId).append("_Negative_Rest extends ").append(serviceName).append("BaseTest {\n");

                // basic test method
                testClassBuilder.append("\n\t@Test()\n");
                testClassBuilder.append("\tpublic void test").append(operationId).append("_Negative_Rest() {\n");

                // parameter variables for the test
                if (rqSpecs.has("parameters"))
                {
                    testClassBuilder.append(generateTestParameters(rqSpecs.getJSONArray("parameters"), objectData));
                }

                // skeleton of how to send the request
                testClassBuilder.append("\n\t\tRestResponse response = ").append(serviceName).append("Rest.")
                        .append(uncapitalize(serviceName)).append("(environment).").append(rqSpecs.getString("operationId"))
                        .append("();\n");

                // end of test method
                testClassBuilder.append("\t}\n");

                // end of class
                testClassBuilder.append("}\n");

                organizedTests.get(organizingTag).add(testClassBuilder.toString());
            }
        }

        return organizedTests;
    }

    private String generateTestParameters(JSONArray parameters, Map<String, List<Pair<String, String>>> objectData) {
        StringBuilder parameterBuilder = new StringBuilder();
        for (Object parameter : parameters)
        {
            if (parameter instanceof JSONObject)
            {
                if (((JSONObject) parameter).has("schema"))
                {
                    String type = extractDataType(((JSONObject) parameter).getJSONObject("schema"));
                    List<Pair<String, String>> fields;

                    // checks if the type is a List/Array
                    if (type.contains("<"))
                    {
                        fields = objectData.get(getListType(type));
                        parameterBuilder.append("\t\t").append(type).append(" ").append(getListType(type).toLowerCase()).append("s = new ArrayList<>();\n");
                    }
                    else
                    {
                        fields = objectData.get(type);
                        parameterBuilder.append("\t\t").append(type).append(" ").append(uncapitalize(type)).append(" = new ").append(type).append("();\n");
                    }

                    // adds the objects needed for the test
                    for (Pair<String, String> field : fields)
                    {
                        if (field.getValue().contains("<"))
                        {
                            parameterBuilder.append("\t\t").append(field.getValue()).append(" ").append(field.getKey()).append(" = new ArrayList<>();\n");
                        }
                        else
                        {
                            parameterBuilder.append("\t\t").append(field.getValue()).append(" ").append(field.getKey()).append(";\n");
                        }
                    }

                }
                else
                {
                    String type = extractDataType((JSONObject) parameter);

                    if (type.contains("{"))
                    {
                        // removes any enum stuff since they'll need to be converted to strings anyway
                        type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                    }

                    // checks if the type is a List/Array
                    if (type.contains("<"))
                    {
                        parameterBuilder.append("\t\t").append(type).append(" ").append(getListType(type).toLowerCase()).append("s = new ArrayList<>();\n");
                    }
                    else
                    {
                        parameterBuilder.append("\t\t").append(type).append(" ").append(((JSONObject) parameter).getString("name")).append(";\n");
                    }
                }
            }
        }

        return parameterBuilder.toString();
    }
}
