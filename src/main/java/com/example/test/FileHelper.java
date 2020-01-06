package com.example.test;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.example.test.TypeHelper.makeCamelCase;
import static com.example.test.TypeHelper.uncapitalize;

public class FileHelper {
    private String company;
    private String pomFile = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "\t\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "\t\txsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "\t<modelVersion>4.0.0</modelVersion>\n" +
            "\n" +
            "\t<groupId>com." + company + ".api</groupId>\n" +
            "\t<artifactId></artifactId>\n" +
            "\t<version>1.0-SNAPSHOT</version>\n\n" +
            "\t<properties>\n" +
            "\t</properties>\n\n" +
            "\t<dependencies>\n" +
            "\t</dependencies>\n" +
            "</project>";

    public FileHelper(String company, String pathToPom){
        this.company = company;
        if (StringUtils.isNotEmpty(pathToPom)){
            try {
                pomFile = readFileToString(pathToPom);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public JSONObject readJsonFileIntoObject(String filePath) throws IOException {
        return new JSONObject(readFileToString(filePath));
    }

    public String readFileToString(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();

        while (line != null){
            sb.append(line);
            line = reader.readLine();
        }

        reader.close();

        return sb.toString();
    }

    public void writeToFiles(ArrayList<String> operationClasses, Map<String, List<String>> testClasses,
                                    ArrayList<String> commonObjects, ArrayList<String> requestClasses,
                                    ArrayList<String> responseClasses, String serviceName) throws IOException {
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + serviceName + File.separator;

        File file = new File(path);

        if (file.mkdirs() || file.exists()){
            file = new File(path + "pom.xml");

            if (file.createNewFile()){
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile()));
                outputStream.write(pomFile.getBytes());
                outputStream.flush();
                outputStream.close();
            }

            path = path + "src" + File.separator;

            file = new File(path);

            if (file.mkdirs() || file.exists()){
                String main = path + "main" + File.separator;
                String test = path + "test" + File.separator;

                file = new File(main);

                // src/main file/folder structure
                if (file.mkdirs() || file.exists()){
                    // property file for project
                    String propertyFileLocation = main + "resources" + File.separator + "properties" + File.separator;
                    file = new File(propertyFileLocation);

                    if (file.mkdirs() || file.exists()){
                        String propertyFile = operationClasses.get(0);
                        operationClasses.remove(0);

                        writeStringToFile(propertyFile, propertyFileLocation + makeCamelCase(serviceName) + ".properties");
                    }
                    // end of property file

                    // src file structure
                    String srcFolder = main + "java" + File.separator
                            + "com" + File.separator
                            + company + File.separator
                            + "api" + File.separator
                            + "restServices" + File.separator;
                    file = new File(srcFolder);

                    // base test and base rest classes
                    if (file.mkdirs() || file.exists()){
                        writeArrayToFiles(operationClasses.subList(0,2), srcFolder);
                        operationClasses.remove(0);
                        operationClasses.remove(0);
                    }

                    String serviceClassPath = srcFolder + uncapitalize(makeCamelCase(serviceName)) + File.separator;

                    System.out.println(serviceClassPath);

                    file = new File(serviceClassPath);

                    // root service class
                    if (file.mkdirs() || file.exists()){
                        for (String s : operationClasses){
                            if (extractClassName(s).equals(serviceName + "Service")){
                                writeStringToFile(s, serviceClassPath + extractClassName(s) + ".java");
                                operationClasses.remove(s);
                                break;
                            }
                        }
                    }

                    // sub services
                    for (String opClass : operationClasses){
                        String subServicePath = serviceClassPath + uncapitalize(extractClassName(opClass)) + File.separator;
                        file = new File(subServicePath);

                        if (file.mkdirs() || file.exists()){
                            writeStringToFile(opClass, subServicePath + extractClassName(opClass) + ".java");

                            String requestsPath = subServicePath + "requests" + File.separator;
                            file = new File(requestsPath);
                            List<String> requests = new ArrayList<>();

                            if (file.mkdirs() || file.exists()){
                                List<String> rqNames = extractRequestsRequired(opClass);
                                for (String rqClass : requestClasses){
                                    String rqClassName = extractClassName(rqClass);
                                    if (rqNames.contains(rqClassName)){
                                        requests.add(rqClass);
                                        writeStringToFile(rqClass, requestsPath + rqClassName + ".java");
                                    }
                                }
                            }

                            String responsesPath = subServicePath + "responses" + File.separator;
                            file = new File(responsesPath);
                            List<String> responses = new ArrayList<>();

                            if (file.mkdirs() || file.exists()){
                                List<String> responseNames = extractResponsesRequired(opClass);
                                for (String responseClass : responseClasses){
                                    String responseClassName = makeCamelCase(extractClassName(responseClass));
                                    if (responseNames.contains(responseClassName)){
                                        responses.add(responseClass);
                                        writeStringToFile(responseClass, responsesPath + responseClassName + ".java");
                                    }
                                }
                            }

                            String commonObjectsPath = subServicePath + "commonObjects" + File.separator;
                            file = new File(commonObjectsPath);

                            if (file.mkdirs() || file.exists()){
                                List<String> commonObjectNames = extractObjectsFromImportStatements(requests);

                                extractObjectsFromImportStatements(responses).forEach(
                                        s -> {
                                            if (!commonObjectNames.contains(s)){
                                                commonObjectNames.add(s);
                                            }
                                        });

                                populateCommonObjects(commonObjectNames, commonObjects, commonObjectsPath);
                            }
                        }
                    }
                }

                file = new File(test);

                // src/test file/folder
                if (file.mkdirs() || file.exists()){
                    String testFolder = test + "java" + File.separator
                            + "com" + File.separator
                            + company + File.separator
                            + "composite" + File.separator
                            + "api" + File.separator
                            + uncapitalize(serviceName) + "Module" + File.separator
                            + "restServices" + File.separator
                            + uncapitalize(serviceName) + File.separator;

                    file = new File(testFolder);

                    if (file.mkdirs() || file.exists()){
                        for (String key : testClasses.keySet()){
                            String testSectionFolder = testFolder + uncapitalize(makeCamelCase(key)) + File.separator;
                            file = new File(testSectionFolder);

                            if (file.mkdirs() || file.exists()){
                                writeArrayToFiles(testClasses.get(key), testSectionFolder);
                            }
                        }
                    }
                }

                String testResources = test
                        + "resources" + File.separator
                        + "com" + File.separator
                        + company + File.separator
                        + "composite" + File.separator
                        + "api" + File.separator
                        + makeCamelCase(serviceName) + "Module" + File.separator
                        + "restServices" + File.separator;

                file = new File(testResources);

                if (file.mkdirs() || file.exists()){
                    for (String key  : testClasses.keySet()){
                        file = new File(testResources + key);

                        if (file.mkdirs() || file.exists()){

                        }
                    }
                }
            }
        }
    }

    private void populateCommonObjects(List<String> commonObjectNames, List<String> allCommonObjects, String filePath) {
        for (String s : allCommonObjects){
            String className = extractClassName(s);
            if (commonObjectNames.contains(className)){
                writeStringToFile(fixImports(s, filePath), filePath + className + ".java");
                List<String> commonObjectImports = extractObjectsFromImportStatements(Collections.singletonList(s));
                if (!commonObjectImports.isEmpty()){
                    populateCommonObjects(commonObjectImports, allCommonObjects, filePath);
                }
            }
        }
    }



    public static String extractClassName(String clazz) {
        if (clazz.split("\n")[0].contains(" enum ")) {
            return clazz.split(" ")[2];
        } else {
            return clazz.split("public class ")[1].split(" ")[0];
        }
    }

    private List<String> extractObjectsFromImportStatements(List<String> classes) {
        List<String> coNames = new ArrayList<>();

        for (String clazz : classes){
            for (String line : clazz.split("\n")){
                if (line.contains("import")){
                    String name = line.split("\\.")[line.split("\\.").length-1].replace(";", "");
                    if (!coNames.contains(name)){
                        coNames.add(name);
                    }
                }
            }
        }

        return coNames;
    }

    private List<String> extractResponsesRequired(String opClass) {
        ArrayList<String> responseClassNames = new ArrayList<>();
        for (String s : opClass.split("\\(Map<String, String> headers, ")){
            if (s.split("\\)")[0].contains("request")){
                responseClassNames.add(s.split("\\)")[0].split(" ")[0].replace("Request", "Response"));
            }
        }
        return responseClassNames;
    }

    private List<String> extractRequestsRequired(String opClass) {
        ArrayList<String> requestClassNames = new ArrayList<>();
        for (String s : opClass.split("\\(Map<String, String> headers, ")){
            if (s.split("\\)")[0].contains("request")){
                requestClassNames.add(s.split("\\)")[0].split(" ")[0]);
            }
        }
        return requestClassNames;
    }

    public void writeArrayToFiles(List<String> classes, String path) {
        for (String s : classes){
            writeStringToFile(s, path + extractClassName(s) + ".java");
        }
    }

    private String fixImports(String clazz, String filePath) {
        if (clazz.contains("public enum ")){
            return clazz;
        }
        ArrayList<String> importStatements = extractImportStatements(clazz);
        String importPath = filePath.substring(0, filePath.lastIndexOf(File.separator)).replace(File.separatorChar, '.');
        String missingPart = importPath.split("\\.")[importPath.split("\\.").length-2];

        for (String s : importStatements){
            clazz = clazz.replace(s, s.replace("commonObjects", missingPart + ".commonObjects"));
        }

        return clazz;
    }

    private ArrayList<String> extractImportStatements(String clazz) {
        ArrayList<String> importStatements = new ArrayList<>();

        for (String line : clazz.split("\n")){
            if (line.contains("import com." + company + ".")){
                importStatements.add(line);
            }
        }

        return importStatements;
    }

    private void writeStringToFile(String fileContent, String pathToFile){
        StringBuilder pkgStatement = new StringBuilder("package ");
        File file = new File(pathToFile);

        if (pathToFile.endsWith(".java")){
            boolean isAfterCom = false;
            String[] splitPath = pathToFile.replace(File.separatorChar, '.').split("\\.");
            for (int i = 0; i < splitPath.length; i++){
                if (splitPath[i].equals("com")){
                    isAfterCom = true;
                }
                if (isAfterCom){
                    pkgStatement.append(splitPath[i]).append(".");
                }
            }

            pkgStatement.deleteCharAt(pkgStatement.lastIndexOf("."));
            pkgStatement.delete(pkgStatement.lastIndexOf("."), pkgStatement.length());
            pkgStatement.delete(pkgStatement.lastIndexOf("."), pkgStatement.length());

            pkgStatement.append(";\n\n");
        }

        try {
            if (file.createNewFile()){
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(pathToFile));
                if (!pkgStatement.toString().isEmpty()){
                    outputStream.write(pkgStatement.toString().getBytes());
                }
                outputStream.write(fileContent.getBytes());
                outputStream.flush();
                outputStream.close();
            } else if (file.delete() && file.createNewFile()){
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(pathToFile));
                if (!pkgStatement.toString().isEmpty()){
                    outputStream.write(pkgStatement.toString().getBytes());
                }
                outputStream.write(fileContent.getBytes());
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            System.out.println(pathToFile);
            e.printStackTrace();
        }
    }
}
