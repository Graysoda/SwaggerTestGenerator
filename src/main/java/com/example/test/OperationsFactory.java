package com.example.test;

import org.json.JSONObject;

import java.util.ArrayList;

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

    public ArrayList<String> generateOperationClasses(String serviceName, String basePath, String host, JSONObject paths) {
        ArrayList<String> operationClasses = new ArrayList<>();

        operationClasses.add(generatePropertiesFile(serviceName, host));
        operationClasses.add(generateBaseTestClass(serviceName));
        operationClasses.add(generateBaseRestClass(serviceName));
        operationClasses.add(generateServiceClass(serviceName, basePath));
        operationClasses.add(generateServerInterfaceClass(serviceName, paths));

        return operationClasses;
    }

    private String generateServerInterfaceClass(String title, JSONObject paths) {
        StringBuilder classBuilder = new StringBuilder();

        String serviceName = title.replace(" ", "");
        String commonPartOfMethodDeclaration = "\n\tpublic RestResponse ";

        // start class declaration
        classBuilder.append("public class ").append(serviceName).append(" extends ").append(serviceName).append("BaseTest {\n");

        // global variable declaration
        classBuilder.append("\tprivate RestService restService;\n");
        classBuilder.append("\tprivate String resource;\n");

        // constructor declaration
        classBuilder.append("\n\tpublic ").append(serviceName).append("(RestService restService, String resource){\n");
        classBuilder.append("\t\tthis.restService = restService;\n");
        classBuilder.append("\t\tthis.resource = resource;\n");
        classBuilder.append("\t}\n");

        for (String endpointPath : paths.keySet()){
            JSONObject endpointRqType = paths.getJSONObject(endpointPath);

            for (String rqType : endpointRqType.keySet()){
                JSONObject endpointOpSpec = endpointRqType.getJSONObject(rqType);
                String opId = endpointOpSpec.getString("operationId");
                StringBuilder methodCode = new StringBuilder();

                // start method signature for endpoint operation
                classBuilder.append(commonPartOfMethodDeclaration).append(opId).append("(Map<String, String> headers, ");

                // for loop to add any custom headers passed
                methodCode.append("\t\tfor(String header : headers.keySet()) {\n");
                methodCode.append("\t\t\trestServices.addCustomHeader(header, headers.get(header));\n");
                methodCode.append("\t\t}\n");

                if (endpointOpSpec.has("parameters")){
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

                                if (type.contains("{")){
                                    // removes any enum stuff since they'll need to be converted to strings anyway
                                    type = type.replace(type.substring(type.indexOf("{"), type.indexOf("}")+1), "");
                                }

                                classBuilder.append(type).append(" ").append(makeCamelCase(((JSONObject) parameterSpec).getString("name"))).append(", ");

                                if (type.contains("<"))
                                {
                                    // gets the type of object the list contains
                                    String listType = type.substring(type.indexOf("<")+1, type.indexOf(">"));

                                    if (!methodCode.toString().contains("\t\tString path = \"\";\n")){
                                        methodCode.append("\t\tString path = \"\";\n");
                                    }

                                    // for loop to make a string containing the values of the variables to be appended to the path
                                    methodCode.append("\t\tfor(").append(listType).append(" ").append(listType.toLowerCase()).append(" : ").append(((JSONObject) parameterSpec).getString("name")).append(") {\n");
                                    methodCode.append("\t\t\tpath = path + ").append(listType.toLowerCase()).append(" + \"").append(delimiter).append("\";\n");
                                    methodCode.append("\t\t}\n");
                                }
                            }
                        } else {
                            throw new RuntimeException("parameter not a JsonObject");
                        }
                    }
                }
                // remove last comma from parameters in method signature
                int i = classBuilder.lastIndexOf(", ");
                classBuilder.deleteCharAt(i);
                // end method signature
                classBuilder.append(") {\n");

                // method logic
                classBuilder.append(methodCode.toString());

                // return line
                classBuilder.append("\t\treturn restService.send").append(capitalize(rqType)).append("Request(resource")
                        .append(" + \"")
                        // logic for parsing the variable name out of the path
                        .append(endpointPath.contains("{") ? endpointPath.replace("{", "\" + ").replace("}", "")
                                : endpointPath + "\"");

                if (methodCode.toString().contains("\t\tString path = \"\";\n") && methodCode.toString().contains("\t\tString json = restService.getJsonFromObject(request);\n")){
                    classBuilder.append(" + path, HeaderType.NONE, json);\n");
                } else if (methodCode.toString().contains("\t\tString json = restService.getJsonFromObject(request);\n")) {
                    classBuilder.append(", HeaderType.NONE, json);\n");
                } else if (methodCode.toString().contains("\t\tString path = \"\";\n")){
                    classBuilder.append(" + path, HeaderType.NONE, null);\n");
                } else {
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
}
