package com.example.test;

import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileHelper {

    public static JSONObject readJsonFileIntoObject(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();

        while (line != null){
            sb.append(line);
            line = reader.readLine();
        }

        reader.close();

        return new JSONObject(sb.toString());
    }

    public static void writeToFiles(ArrayList<String> operationClasses, Map<String, List<String>> testClasses,
                                    ArrayList<String> commonObjects, ArrayList<String> requestClasses,
                                    ArrayList<String> responseClasses) throws IOException {
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "testOutput" + File.separator;

        File file = new File(path);

        if (file.mkdir() || file.exists())
        {
            String operationPath = path + "operationClasses" + File.separator;
            file = new File(operationPath);

            if (file.mkdir() || file.exists())
            {
                String propertyFile = operationClasses.get(0);
                operationClasses.remove(0);

                file = new File(operationPath + propertyFile.substring(propertyFile.indexOf("_") + 1, propertyFile.indexOf(".")) + ".properties");

                if (file.createNewFile())
                {
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile()));
                    outputStream.write(propertyFile.getBytes());
                    outputStream.flush();
                    outputStream.close();
                }

                writeArrayToFiles(operationClasses, operationPath);
            }

            String testClassesPath = path + "testClasses" + File.separator;
            file = new File(testClassesPath);

            if (file.mkdir() || file.exists())
            {
                writeMapToFiles(testClasses, testClassesPath);
            }

            String commonObjectsPath = path + "commonObjects" + File.separator;
            file = new File(commonObjectsPath);

            if (file.mkdir() || file.exists())
            {
                writeArrayToFiles(commonObjects, commonObjectsPath);
            }

            String requestClassPath = path + "requestClasses" + File.separator;
            file = new File(requestClassPath);

            if (file.mkdir() || file.exists())
            {
                writeArrayToFiles(requestClasses, requestClassPath);
            }

            String responseClassPath = path + "responseClasses" + File.separator;
            file = new File(responseClassPath);

            if (file.mkdir() || file.exists())
            {
                writeArrayToFiles(responseClasses, responseClassPath);
            }
        }
    }

    private static void writeMapToFiles(Map<String, List<String>> testClasses, String testClassesPath) {
        for (String s : testClasses.keySet()) {
            String path = TypeHelper.makeCamelCase(testClassesPath + s + File.separator);
            if (new File(path).exists() || new File(path).mkdir()){
                writeArrayToFiles(testClasses.get(s), path);
            }
        }
    }

    private static void writeArrayToFiles(List<String> classes, String path) {
        for (String s : classes){
            String fileName;

            if (s.split("\n")[0].contains(" enum ")){
                fileName = path + s.split(" ")[2] + ".java";
            } else {
                fileName = path + s.split("public class ")[1].split(" ")[0] + ".java";
            }

            File file = new File(fileName);

            try {
                if (file.createNewFile()){
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    outputStream.write(s.getBytes());
                    outputStream.flush();
                    outputStream.close();
                } else if (file.delete() && file.createNewFile()){
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    outputStream.write(s.getBytes());
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
