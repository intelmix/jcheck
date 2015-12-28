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

public class JCompiler {

    public static void main(String args[]) throws IOException {
        long startTime = System.nanoTime();
        String result = new JCompiler().compile(args[0]);
        long estimatedTime = System.nanoTime() - startTime;

        System.out.print(result);
        estimatedTime /= 1000;
        estimatedTime /= 1000;

        System.out.println("Elapsed time (ms): " + (String.valueOf(estimatedTime)));
    }

    private String compile(String file) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        //StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        StandardJavaFileManager std = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = std.getJavaFileObjectsFromStrings(Arrays.asList(file));

        //JavaFileManager fileManager = new CustomClassloaderJavaFileManager(Thread.currentThread().getContextClassLoader(), standardJavaFileManager);
        FileManagerImpl fileManager = new FileManagerImpl(std);
        //ForwardingJavaFileManager<StandardJavaFileManager> fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(std);

        List<String> optionList = new ArrayList<String>();
        // set compiler's classpath to be same as the runtime's
        optionList.addAll(Arrays.asList("-classpath",System.getenv("CLASSPATH")));
        optionList.addAll(Arrays.asList("-sourcepath","/srv/newzrobot/server/src/main/java/"));

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList,
                null, compilationUnits);
        boolean success = task.call();
        std.close();

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

    private final class FileManagerImpl extends ForwardingJavaFileManager<StandardJavaFileManager> {

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

    }

}
