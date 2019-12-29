package com.example.test;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static com.example.test.TypeHelper.*;

public class OperationsFactory {
    private String company = "example";
    private ImportFactory importFactory;

    public OperationsFactory(){
        importFactory = new ImportFactory();
    }

    public OperationsFactory(String company){
        this.company = company;
        importFactory = new ImportFactory(company);
    }

    public ArrayList<String> generateOperationClasses(String serviceName, String basePath, String host, JSONObject paths, JSONArray tags) {
        ArrayList<String> operationClasses = new ArrayList<>();

        operationClasses.add(generatePropertiesFile(serviceName, host));
        operationClasses.add(generateBaseTestClass(serviceName));
        operationClasses.add(generateBaseRestClass(serviceName));
        operationClasses.add(generateServiceClass(serviceName, basePath));
        operationClasses.addAll(generateServiceClasses(tags, paths, serviceName, basePath));

        return operationClasses;
    }

    private String generateServerInterfaceClass(String title, JSONObject paths, String commonPath) {
        StringBuilder classBuilder = new StringBuilder();
        String serviceName = capitalize(title.replace(" ", ""));
        classBuilder.append(importFactory.generateInterfaceImportStatements(paths, serviceName));

        String commonPartOfMethodDeclaration = "\n\tpublic RestResponse ";

        // start class declaration
        classBuilder.append("public class ").append(serviceName).append(" {\n");

        // global variable declaration
        classBuilder.append("\tprivate RestService restService;\n");
        classBuilder.append("\tprivate String resource");

        if (!commonPath.isEmpty()){
            classBuilder.append(" = \"").append(commonPath).append("\";\n");
        } else {
            classBuilder.append(";\n");
        }

        // constructor declaration
        classBuilder.append("\n\tpublic ").append(serviceName).append("(RestService restService, String resource){\n");
        classBuilder.append("\t\tthis.restService = restService;\n");
        classBuilder.append("\t\tthis.resource = resource + this.resource;\n");
        classBuilder.append("\t}\n");

        for (String endpointPath : paths.keySet())
        {
            JSONObject endpointRqType = paths.getJSONObject(endpointPath);

            for (String rqType : endpointRqType.keySet())
            {
                JSONObject endpointOpSpec = endpointRqType.getJSONObject(rqType);
                String opId = endpointOpSpec.getString("operationId");
                StringBuilder methodCode = new StringBuilder();

                // start method signature for endpoint operation
                classBuilder.append(commonPartOfMethodDeclaration).append(opId).append("(Map<String, String> headers, ");

                // for loop to add any custom headers passed
                methodCode.append("\t\tfor(String header : headers.keySet()) {\n");
                methodCode.append("\t\t\trestServices.addCustomHeader(header, headers.get(header));\n");
                methodCode.append("\t\t}\n");

                if (endpointOpSpec.has("parameters"))
                {
                    for (Object parameterSpec : endpointOpSpec.getJSONArray("parameters"))
                    {
                        if (parameterSpec instanceof JSONObject)
                        {
                            String in = ((JSONObject) parameterSpec).getString("in");

                            if (in.equals("body"))
                            {
                                classBuilder.append(capitalize(opId)).append("Request request, ");

                                methodCode.append("\t\tString json = restService.getJsonFromObject(request);\n");
                            }
                            else if (in.equals("path") || in.equals("query"))
                            {
                                char delimiter = ';';
//                            if (((JSONObject) parameterSpec).has("style")){
//                                // TODO handle the style (it changes the delimiting character)
//                            }
                                String type = ((JSONObject) parameterSpec).has("schema") ? extractDataType(((JSONObject) parameterSpec).getJSONObject("schema")) : extractDataType((JSONObject) parameterSpec);

                                if (type.contains("{"))
                                {
                                    // removes any enum stuff since they'll need to be converted to strings anyway
                                    type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                                }

                                classBuilder.append(type).append(" ").append(makeCamelCase(((JSONObject) parameterSpec).getString("name"))).append(", ");

                                if (type.contains("<"))
                                {
                                    // gets the type of object the list contains
                                    String listType = type.substring(type.indexOf("<")+1, type.indexOf(">"));

                                    if (!methodCode.toString().contains("\t\tString path = \"\";\n"))
                                    {
                                        methodCode.append("\t\tString path = \"\";\n");
                                    }

                                    // for loop to make a string containing the values of the variables to be appended to the path
                                    methodCode.append("\t\tfor(").append(listType).append(" ").append(listType.toLowerCase()).append(" : ").append(((JSONObject) parameterSpec).getString("name")).append(") {\n");
                                    methodCode.append("\t\t\tpath = path + ").append(listType.toLowerCase()).append(" + \"").append(delimiter).append("\";\n");
                                    methodCode.append("\t\t}\n");
                                }
                            }
                        }
                        else
                        {
                            throw new RuntimeException("parameter not a JsonObject");
                        }
                    }
                }
                // remove last comma from parameters in method signature
                classBuilder.deleteCharAt(classBuilder.lastIndexOf(", "));
                // end method signature
                classBuilder.append(") {\n");

                // method logic
                classBuilder.append(methodCode.toString());

                // start of the return line
                classBuilder.append("\t\treturn restService.send").append(capitalize(rqType)).append("Request(resource");

                // removes the common part defined by the resource variable
                String endpointWithoutCommonPart = endpointPath.replace(commonPath, "");

                // logic for parsing the variable name out of the path
                if (endpointWithoutCommonPart.contains("{"))
                {
                    if (endpointWithoutCommonPart.indexOf("}") == endpointWithoutCommonPart.length()-1)
                    {
                        classBuilder.append(" + \"").append(endpointWithoutCommonPart.replace("{", "\" + ").replace("}", ""));
                    }
                    else
                    {
                        classBuilder.append(" + \"").append(endpointWithoutCommonPart.replace("{", "\" + ").replace("}", " + \"")).append("\"");
                    }
                }
                else
                {
                    classBuilder.append(" + \"").append(endpointWithoutCommonPart.replace(commonPath, "")).append("\"");
                }

                // logic for query variables
                if (endpointOpSpec.has("parameters"))
                {
                    boolean firstQueryParam = true;
                    for (Object paramSpec : endpointOpSpec.getJSONArray("parameters"))
                    {
                        if (paramSpec instanceof JSONObject && ((JSONObject) paramSpec).getString("in").equals("query"))
                        {
                            String name = makeCamelCase(((JSONObject) paramSpec).getString("name"));
                            if (firstQueryParam)
                            {
                                classBuilder.insert(classBuilder.lastIndexOf("\""),"?" + name + "=\" + " + name + " + ").deleteCharAt(classBuilder.lastIndexOf("\""));
                                firstQueryParam = false;
                            }
                            else
                            {
                                classBuilder.append("\"&").append(name).append("=\" + ").append(name).append(" + ");
                            }
                        }
                    }
                }

                // logic for how the return line should end
                if (methodCode.toString().contains("\t\tString path = \"\";\n") && methodCode.toString().contains("\t\tString json = restService.getJsonFromObject(request);\n"))
                {
                    classBuilder.append(" + path, HeaderType.NONE, json);\n");
                }
                else if (methodCode.toString().contains("\t\tString json = restService.getJsonFromObject(request);\n"))
                {
                    classBuilder.append(", HeaderType.NONE, json);\n");
                }
                else if (methodCode.toString().contains("\t\tString path = \"\";\n"))
                {
                    classBuilder.append(" + path, HeaderType.NONE, null);\n");
                }
                else
                {
                    classBuilder.append(", HeaderType.NONE, null);\n");
                }

                classBuilder.append("\t}\n");
            }
        }

        // end class declaration
        classBuilder.append("}\n");

        return classBuilder.toString();
    }

    private String generateBaseRestClass(String serviceName) {
        serviceName = serviceName.replace(" ", "");
        return "import com." + company + ".api.restServices.core.RestService;\n" +
                "import com." + company + ".api.restServices." + uncapitalize(serviceName) + "." + serviceName + "Service;\n\n" +
                "public class " + serviceName + "Rest {\n" +
                "\tpublic static " + serviceName + "Service " + uncapitalize(serviceName) + "Service(String environment) {\n" +
                "\t\tRestService = new RestService(environment);\n" +
                "\t\treturn new " + serviceName + "Service(restService);\n" +
                "\t}\n";
    }

    private String generateServiceClass(String serviceName, String basePath) {
        serviceName = serviceName.replace(" ", "");
        return "import com." + company + ".api.restServices.core.RestService;\n" +
                "import com." + company + ".api.restServices." + uncapitalize(serviceName) + "." + serviceName + ";\n\n" +
                "public class " + serviceName + "Service {\n" +
                // global variables
                "\tprivate RestService restService;\n" +
                "\tprivate String resource = \"" + basePath+ "\";\n" +
                "\tprivate final static String strProperties = \"properties/" + makePropertyFileName(serviceName) + "\";\n" +
                "\n" +
                // constructor
                "\tpublic " + serviceName + "(RestService restService) {\n" +
                "\t\tthis.restService = restService;\n" +
                "\t\tthis.restService.setMainResource(\"REST_" + serviceName + "Service\");\n" +
                "}\n" +
                "\n" +
                // method to get the server interface class
                "\tpublic " + serviceName + " " + uncapitalize(serviceName) + "() {\n" +
                "\t\treturn new " + serviceName + "(restService, resource);\n" +
                "\t}\n" +
                "}";
    }

    private String generateBaseTestClass(String serviceName) {
        return "import com." + company + ".api.restServices.BaseRestTest;\n\n" +
                "public class " + serviceName.replace(" ", "") + "BaseTest extends BaseRestTest {\n" +
                "\n}";
    }

    private String makePropertyFileName(String serviceName) {
        return serviceName.toLowerCase().replace(" ", "-") + ".properties";
    }

    private String generatePropertiesFile(String serviceName, String host) {
        return "REST_" + serviceName.replace(" ", "") + ".latest.endpoint=http://" + host;
    }

    public List<String> generateServiceClasses(JSONArray tags, JSONObject paths, String serviceName, String basePath) {
        serviceName = serviceName.replace(" ", "");
        List<String> subServices = new ArrayList<>();
        Map<String, List<Pair<String, String>>> tagsWithOpIdsAndEndpointPaths = new HashMap<>();

        // initializing the map of tags to paths
        for (Object o : tags)
        {
            if (o instanceof JSONObject)
            {
                 tagsWithOpIdsAndEndpointPaths.put(((JSONObject) o).getString("name"), new ArrayList<>());
            }
        }

        // associating operatingId's with tags
        for (String pathName : paths.keySet())
        {
            JSONObject pathSpec = paths.getJSONObject(pathName);
            String tagName = null;

            for (String rqType : pathSpec.keySet()){
                JSONObject rqSpec = pathSpec.getJSONObject(rqType);

                if (rqSpec.has("tags"))
                {
                    if (tagName == null)
                    {
                        tagName = rqSpec.getJSONArray("tags").getString(0);
                    }
                    else if (!tagName.equals(rqSpec.getJSONArray("tags").getString(0)))
                    {
                        System.out.println(pathName + "has different tags");
                    }

                    if (rqSpec.getJSONArray("tags").toList().size() == 1)
                    {
                        tagsWithOpIdsAndEndpointPaths
                                .get(rqSpec.getJSONArray("tags").getString(0))
                                .add(new Pair<>(rqSpec.getString("operationId"), pathName));
                    }
                    else
                    {
                        System.out.println(pathName + "[" + rqType + "] has more than 1 tag");
                    }
                }
                else
                {
                    System.out.println(pathName + "[" + rqType + "] has no tags");
                }
            }
        }

        // extracting the shared part of the path for each tag
        Map<String, String> tagsSharedPath = new HashMap<>();

        for (String tag : tagsWithOpIdsAndEndpointPaths.keySet())
        {
            String sharedPath = "";
            for (Pair<String, String> pair : tagsWithOpIdsAndEndpointPaths.get(tag))
            {
                if (sharedPath.isEmpty())
                {
                    sharedPath = pair.getValue();
                }
                else
                {
                    for (String s : sharedPath.split("/"))
                    {
                        if (!s.isEmpty()
                                && !Arrays.asList(pair.getValue().split("/")).contains(s)
                                && !Arrays.asList(pair.getValue().split("/")).contains(s + "s"))
                        {
                            sharedPath = sharedPath.replace(("/" + s), "");
                        }
                    }
                }
            }
            tagsSharedPath.put(tag, sharedPath);
        }

        // removing matching parts of the tag names
        Map<String, String> tagOldAndNewNamePairs = new HashMap<>();

        for (String tag : tagsWithOpIdsAndEndpointPaths.keySet())
        {
            ArrayList<String> comparators = new ArrayList<>(tagsWithOpIdsAndEndpointPaths.keySet());
            comparators.remove(tag);

            for (String compare : comparators)
            {
                if (compare.contains("-") && tag.contains("-"))
                {
                    String nameDifferences = tag;

                    for (String t : tag.split("-"))
                    {
                        if (Arrays.asList(compare.split("-")).contains(t))
                        {
                            nameDifferences = nameDifferences.replace("-" + t, "");

                            if (restrictedNames.contains(nameDifferences))
                            {
                                nameDifferences = nameDifferences + "-" + t;
                            }
                        }
                    }

                    if (!tagOldAndNewNamePairs.containsKey(tag))
                    {
                        tagOldAndNewNamePairs.put(tag, nameDifferences);
                    }
                }
            }
        }

        // makes the "sub service" classes (small subsections of server endpoints described in the swagger)
        for (String originalTag : tagsWithOpIdsAndEndpointPaths.keySet())
        {
            JSONObject pathSubsetByTag = new JSONObject();

            // populate the above JSONObject
            for (String path : paths.keySet())
            {
                for (String rqType : paths.getJSONObject(path).keySet())
                {
                    String tag = (String) paths.getJSONObject(path).getJSONObject(rqType).getJSONArray("tags").toList().get(0);
                    if (tag.equals(originalTag))
                    {
                        pathSubsetByTag.put(path, new JSONObject(paths.get(path).toString()));
                    }
                }
            }

            subServices.add(generateServerInterfaceClass(makeCamelCase(tagOldAndNewNamePairs.get(originalTag)), pathSubsetByTag, tagsSharedPath.get(originalTag)));
        }

        subServices.add(generateBaseServiceClass(tagOldAndNewNamePairs, serviceName, basePath));

        return subServices;
    }

    private String generateBaseServiceClass(Map<String, String> tagOldAndNewNamePairs, String serviceName, String basePath) {
        StringBuilder stringBuilder = new StringBuilder("import com." + company + ".api.restServices.core.RestService;\n" +
                "import com." + company + ".api.restServices." + uncapitalize(serviceName) + "." + serviceName + ";\n\n" +
                "public class " + serviceName + "Service {\n" +
                // global variables
                "\tprivate RestService restService;\n" +
                "\tprivate String resource = \"" + basePath + "\";\n" +
                "\tprivate final static String strProperties = \"properties/" + makePropertyFileName(serviceName) + "\";\n" +
                "\n" +
                // constructor
                "\tpublic " + serviceName + "(RestService restService) {\n" +
                "\t\tthis.restService = restService;\n" +
                "\t\tthis.restService.setMainResource(\"REST_" + serviceName + "Service\");\n" +
                "\t}\n");

        for (String newTag : tagOldAndNewNamePairs.values()){
            // method to get the server interface class
            stringBuilder.append("\n\tpublic ").append(makeCamelCase(capitalize(newTag))).append(" ").append(makeCamelCase(uncapitalize(newTag))).append("() {\n")
                    .append("\t\treturn new ").append(makeCamelCase(capitalize(newTag))).append("(restService, resource);\n")
                    .append("\t}\n");
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
