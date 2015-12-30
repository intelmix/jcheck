import java.io.IOException;
import java.util.Arrays;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.*;
import java.lang.*;
import java.util.*;
import java.util.jar.*;
import java.net.URI;
import java.io.*;
import javax.tools.JavaFileObject;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.JarURLConnection;
import java.net.URI;
import javax.tools.JavaFileObject;
import java.io.*; 
import java.net.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class JCompiler {
    private static JCompiler myObject = new JCompiler();
    private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    //It seems that re-using fileManager across multiple checking for different files will make using '-sourcepath' option effect-less
    //so we will cache last used source file and re-create fileManager when it is changed
    private static StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    private static String lastCheckedFile = null;

    public static void main(String args[]) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/check", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            URI requestedUri = t.getRequestURI();
            String fileToCheck = requestedUri.getRawQuery();

            long startTime = System.nanoTime();
            if ( lastCheckedFile == null || !lastCheckedFile.equals(fileToCheck) ) {
                 lastCheckedFile = fileToCheck;
                 fileManager.close();
                 fileManager = compiler.getStandardFileManager(null, null, null);
            }

            String response = myObject.compile(requestedUri.getRawQuery());
            long estimatedTime = System.nanoTime() - startTime;
            estimatedTime /= 1000;
            estimatedTime /= 1000;
            response += "\n(took "+String.valueOf(estimatedTime) + " ms)";
            
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private String getSourcePath(String filePath) {
        //go up in the file hierarchy until you find a pom.xml or else return null
        File file = new File(filePath);

        while ( file != null ) {
            String parentPath = file.getAbsoluteFile().getParent();

            if ( parentPath == null ) return null;
            file = new File(parentPath);

            if ( new File(parentPath, "pom.xml").exists() ) {
                return parentPath + "/src/main/java/";
            }
        }

        return null;
    }


    private String compile(String file) throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(file));

        String sourcePath = getSourcePath(file);

        List<String> optionList = new ArrayList<String>();
        if ( sourcePath != null ) {
            optionList.addAll(Arrays.asList("-sourcepath",sourcePath));
        }

        // set compiler's classpath to be same as the runtime's
        optionList.addAll(Arrays.asList("-classpath",System.getenv("CLASSPATH")));
        optionList.addAll(Arrays.asList("-d","/tmp"));

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList,
                null, compilationUnits);
        boolean success = task.call();
        fileManager.close();

        if (!success) {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                //d.getCode returns compiler.err.class.public.should.be.in.file 
                sb.append(String.format("At %d:%d ->  %s\n", diagnostic.getLineNumber(), diagnostic.getColumnNumber(), diagnostic.getMessage(null)));
            }

            return sb.toString();
        } else {
            return "OK!\n";
        }
    }
}
