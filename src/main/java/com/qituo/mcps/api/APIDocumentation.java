package com.qituo.mcps.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.qituo.mcps.core.MCPSMod;

public class APIDocumentation {
    private static APIDocumentation instance;
    private List<APIClass> apiClasses;
    private String documentationPath;
    
    private APIDocumentation() {
        this.apiClasses = new ArrayList<>();
        this.documentationPath = System.getProperty("user.dir") + File.separator + "docs" + File.separator + "api";
        initializeDocumentationDirectory();
    }
    
    public static APIDocumentation getInstance() {
        if (instance == null) {
            instance = new APIDocumentation();
        }
        return instance;
    }
    
    private void initializeDocumentationDirectory() {
        File dir = new File(documentationPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void registerAPIClass(Class<?> clazz, String description) {
        APIClass apiClass = new APIClass(clazz, description);
        apiClasses.add(apiClass);
        MCPSMod.LOGGER.info("Registered API class: " + clazz.getName());
    }
    
    public void generateDocumentation() {
        try {
            // 生成HTML文档
            generateHTMLDocumentation();
            
            // 生成Markdown文档
            generateMarkdownDocumentation();
            
            MCPSMod.LOGGER.info("API documentation generated successfully");
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error generating API documentation: " + e.getMessage());
        }
    }
    
    private void generateHTMLDocumentation() throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<title>MCPS API Documentation</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #333; }");
        html.append(".class-section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }");
        html.append(".method-section { margin: 10px 0; padding: 10px; border-left: 3px solid #4CAF50; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<h1>MCPS API Documentation</h1>");
        
        for (APIClass apiClass : apiClasses) {
            html.append("<div class='class-section'>");
            html.append("<h2>" + apiClass.getClassName() + "</h2>");
            html.append("<p>" + apiClass.getDescription() + "</p>");
            
            for (APIMethod method : apiClass.getMethods()) {
                html.append("<div class='method-section'>");
                html.append("<h3>" + method.getMethodName() + "</h3>");
                html.append("<p>" + method.getDescription() + "</p>");
                html.append("<p><strong>Parameters:</strong> " + method.getParameters() + "</p>");
                html.append("<p><strong>Returns:</strong> " + method.getReturnType() + "</p>");
                html.append("</div>");
            }
            
            html.append("</div>");
        }
        
        html.append("</body>");
        html.append("</html>");
        
        Path path = Paths.get(documentationPath, "index.html");
        Files.write(path, html.toString().getBytes());
    }
    
    private void generateMarkdownDocumentation() throws IOException {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# MCPS API Documentation\n\n");
        
        for (APIClass apiClass : apiClasses) {
            markdown.append("## " + apiClass.getClassName() + "\n\n");
            markdown.append(apiClass.getDescription() + "\n\n");
            
            for (APIMethod method : apiClass.getMethods()) {
                markdown.append("### " + method.getMethodName() + "\n\n");
                markdown.append(method.getDescription() + "\n\n");
                markdown.append("**Parameters:** " + method.getParameters() + "\n\n");
                markdown.append("**Returns:** " + method.getReturnType() + "\n\n");
            }
        }
        
        Path path = Paths.get(documentationPath, "README.md");
        Files.write(path, markdown.toString().getBytes());
    }
    
    public List<APIClass> getAPIClasses() {
        return apiClasses;
    }
    
    public String getDocumentationPath() {
        return documentationPath;
    }
    
    public static class APIClass {
        private Class<?> clazz;
        private String description;
        private List<APIMethod> methods;
        
        public APIClass(Class<?> clazz, String description) {
            this.clazz = clazz;
            this.description = description;
            this.methods = new ArrayList<>();
            scanMethods();
        }
        
        private void scanMethods() {
            // 扫描类的方法，这里简化实现
        }
        
        public String getClassName() {
            return clazz.getName();
        }
        
        public String getDescription() {
            return description;
        }
        
        public List<APIMethod> getMethods() {
            return methods;
        }
    }
    
    private static class APIMethod {
        private String methodName;
        private String description;
        private String parameters;
        private String returnType;
        
        public APIMethod(String methodName, String description, String parameters, String returnType) {
            this.methodName = methodName;
            this.description = description;
            this.parameters = parameters;
            this.returnType = returnType;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getParameters() {
            return parameters;
        }
        
        public String getReturnType() {
            return returnType;
        }
    }
}