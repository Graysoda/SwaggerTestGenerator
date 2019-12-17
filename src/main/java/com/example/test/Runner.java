package com.example.test;

import javafx.util.Pair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Runner {
    private static String company = "example";
    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0){
            company = args[0];
        }
        CommonObjectsFactory commonObjectsFactory = new CommonObjectsFactory(company);
        OperationsFactory operationsFactory = new OperationsFactory(company);
        ResponseFactory responseFactory = new ResponseFactory(company);
        RequestFactory requestFactory = new RequestFactory(company);
        TestFactory testFactory = new TestFactory(company);


        JSONObject root = FileHelper.readJsonFileIntoObject(Runner.class.getClassLoader().getResource("swagger.json").toURI().getPath());

        Map<String, List<Pair<String, String>>> objectData = commonObjectsFactory.extractDefinitionData(root.getJSONObject("definitions"));

        JSONObject info = (JSONObject) root.get("info");

        JSONObject paths = root.getJSONObject("paths");

        ArrayList<String> operationClasses = operationsFactory.generateOperationClasses(info.getString("title"), root.getString("basePath"), root.getString("host"), paths);
        ArrayList<String> testClasses = testFactory.generateTestClasses(paths,info.getString("title").replace(" ", ""), objectData);
        ArrayList<String> commonObjects = commonObjectsFactory.generateCommonObjects(objectData, info.getString("title").replace(" ", ""));
        ArrayList<String> requestClasses = requestFactory.generateRequestClasses(paths, objectData, info.getString("title").replace(" ", ""));
        ArrayList<String> responseClasses = responseFactory.generateResponseClasses(paths, objectData, info.getString("title").replace(" ", ""));

//        objectData.forEach( (variableName, fields) -> System.out.println(variableName + " = " + fields.toString()));
        System.out.println(commonObjects.toString());
        System.out.println(requestClasses.toString());
        System.out.println(responseClasses.toString());
        System.out.println(operationClasses.toString());
        System.out.println(testClasses.toString());

        FileHelper.writeToFiles(operationClasses, testClasses, commonObjects, requestClasses, responseClasses);
    }
}
