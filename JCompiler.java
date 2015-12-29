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
    private static StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

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
            long startTime = System.nanoTime();
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

    private String method1() { return "method1"; }
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


        //StandardJavaFileManager std = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(file));

        //JavaFileManager fileManager = new CustomClassloaderJavaFileManager(Thread.currentThread().getContextClassLoader(), standardJavaFileManager);
        //FileManagerImpl fileManager = new FileManagerImpl(std);
        //ForwardingJavaFileManager<StandardJavaFileManager> fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(std);

        String sourcePath = getSourcePath(file);

        List<String> optionList = new ArrayList<String>();
        // set compiler's classpath to be same as the runtime's
        optionList.addAll(Arrays.asList("-classpath",System.getenv("CLASSPATH")));

        if ( sourcePath != null ) {
            optionList.addAll(Arrays.asList("-sourcepath","/srv/newzrobot/server/src/main/java/"));
        }

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

    /*private final class FileManagerImpl extends ForwardingJavaFileManager<StandardJavaFileManager> {

      public FileManagerImpl(StandardJavaFileManager fileManager) {
      super(fileManager);
      }

      @Override
      public ClassLoader getClassLoader(JavaFileManager.Location location) {
      System.out.println("Creating classloader");
      return new JarClassLoader();
      }
    }

    private final class JarClassLoader extends ClassLoader {
    private String jarFile = ""; //Path to the jar file

        public JarClassLoader() {
            super(JarClassLoader.class.getClassLoader()); //calls the parent class loader's constructor
        }

        public Class loadClass(String className) throws ClassNotFoundException {
            return findClass(className);
        }

        public Class findClass(String className) {
            byte classByte[];
            Class result = null;
            System.out.println("Loading " + className);

            try {
                return findSystemClass(className);
            } catch (Exception e) {
            }

            try {
                JarFile jar = new JarFile(jarFile);
                JarEntry entry = jar.getJarEntry(className + ".class");
                InputStream is = jar.getInputStream(entry);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                int nextValue = is.read();
                while (-1 != nextValue) {
                    byteStream.write(nextValue);
                    nextValue = is.read();
                }

                classByte = byteStream.toByteArray();
                result = defineClass(className, classByte, 0, classByte.length, null);
                return result;
            } catch (Exception e) {
                return null;
            }
        }

    }*/

}
