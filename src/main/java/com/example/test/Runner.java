package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.restrictedNames;

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
        CommonObjectsFactory commonObjectsFactory = new CommonObjectsFactory(company);
        OperationsFactory operationsFactory = new OperationsFactory(company);
        ResponseFactory responseFactory = new ResponseFactory(company);
        RequestFactory requestFactory = new RequestFactory(company);
        TestFactory testFactory = new TestFactory(company);

        JSONObject root = FileHelper.readJsonFileIntoObject(Runner.class.getClassLoader().getResource("other.json").toURI().getPath());

        Map<String, List<Pair<String, String>>> objectData = commonObjectsFactory.extractDefinitionData(root.getJSONObject("definitions"));

        System.out.println(restrictedNames);

        JSONObject info = (JSONObject) root.get("info");
        JSONArray tags = root.getJSONArray("tags");
        JSONObject paths = root.getJSONObject("paths");
        String serviceName = info.getString("title").replace(" ", "");

        ArrayList<String> operationClasses = operationsFactory.generateOperationClasses(serviceName, root.getString("basePath"), root.getString("host"), paths, tags);
        Map<String, List<String>> testClasses = testFactory.generateTestClasses(paths,serviceName, objectData, operationsFactory.processTags(tags));
        ArrayList<String> commonObjects = commonObjectsFactory.generateCommonObjects(objectData, serviceName);
        ArrayList<String> requestClasses = requestFactory.generateRequestClasses(paths, objectData, serviceName);
        ArrayList<String> responseClasses = responseFactory.generateResponseClasses(paths, objectData, serviceName);

        //service.forEach((tag, opIds) -> System.out.println(tag + " = " + opIds.toString()));

//        objectData.forEach( (variableName, fields) -> System.out.println(variableName + " = " + fields.toString()));
//        System.out.println(commonObjects.toString());
//        System.out.println(requestClasses.toString());
//        System.out.println(responseClasses.toString());
//        System.out.println(operationClasses.toString());
//        System.out.println(testClasses.toString());

        FileHelper.writeToFiles(operationClasses, testClasses, commonObjects, requestClasses, responseClasses);
    }
}
