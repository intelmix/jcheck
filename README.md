# jcheck
A fast syntax checker for java source code files

This is a small utility which creates an in-memory java compiler which can be invoked through HTTP commands to check a Java source file in a Maven-based project 
and get compiler errors. 


In order to use this utility you will need to compile and run it first.

```
javac JCompiler.java
java -cp . JCompiler &
```

You need to append `&` to the command so that the utility will be running in background.

To invoke the checker you need to call it via `curl` (If you dont't have `curl` installed, for god's sake please install it).

```
curl http://localhost:8000/check?/srv/newzrobot/server/src/main/java/File1.java
```

It will return list of errors + time taken to compile.

As this utility will be caching compiler and file manager it will be much faster that running `javac` from command line. Useful for cases during development
that you need instant feedback.

NOTE: This utility relies on a valid `CLASSPATH` defined in bash environment.
