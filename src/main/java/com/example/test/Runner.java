package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Runner {
    private static String company = "example";
    private static String pathToPom;

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0){
            company = args[0];
        }
        if (args != null && args.length > 1){
            pathToPom = args[1];
        }

        FileHelper fileHelper = new FileHelper(company, pathToPom);

        JSONObject root = fileHelper.readJsonFileIntoObject(Runner.class.getClassLoader().getResource("other.json").toURI().getPath());
        
        JSONObject info = (JSONObject) root.get("info");
        JSONArray tags = root.getJSONArray("tags");
        JSONObject paths = root.getJSONObject("paths");
        String serviceName = info.getString("title").replace(" ", "");

        CommonObjectsFactory commonObjectsFactory = new CommonObjectsFactory(company);
        OperationsFactory operationsFactory = new OperationsFactory(company, serviceName);
        ResponseFactory responseFactory = new ResponseFactory(company);
        RequestFactory requestFactory = new RequestFactory(company);
        TestFactory testFactory = new TestFactory(company);

        Map<String, List<Pair<String, String>>> objectData = commonObjectsFactory.extractDefinitionData(root.getJSONObject("definitions"));
        Map<String, String> tagMap = operationsFactory.processTags(tags);

        ArrayList<String> operationClasses = operationsFactory.generateOperationClasses(serviceName, root.getString("basePath"), root.getString("host"), paths, tags);
        Map<String, List<String>> testClasses = testFactory.generateTestClasses(paths,serviceName, objectData, tagMap);
        ArrayList<String> commonObjects = commonObjectsFactory.generateCommonObjects(objectData, serviceName);
        ArrayList<String> requestClasses = requestFactory.generateRequestClasses(paths, serviceName, tagMap);
        ArrayList<String> responseClasses = responseFactory.generateResponseClasses(paths, serviceName, tagMap);

        fileHelper.writeToFiles(operationClasses, testClasses, commonObjects, requestClasses, responseClasses, serviceName);
    }
}
