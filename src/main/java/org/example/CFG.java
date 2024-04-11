package org.example;

import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.signatures.MethodSignature;
import sootup.core.util.DotExporter;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

public class CFG {
    public static void main(String[] args) {
        AnalysisInputLocation inputLocation =
                new JavaSourcePathAnalysisInputLocation("src/test/resources/simple/");

        JavaView view = new JavaView(inputLocation);
        JavaClassType classType = view.getIdentifierFactory().getClassType("EvenOdd");
        Optional<JavaSootClass> classOptional = view.getClass(classType);
        if(!classOptional.isPresent()) {
            System.out.println("Class not founc");
            return;
        }

        JavaSootClass sootClass = classOptional.get();
        MethodSignature methodSignature = view.getIdentifierFactory()
                .getMethodSignature(classType, "foo",
                        "void", Collections.singletonList("int"));
        Optional<JavaSootMethod> method = view.getMethod(methodSignature);
        if (!method.isPresent()){
            System.out.println("Method not found");
            return;
        }

        JavaSootMethod sootMethod = method.get();
        System.out.println(sootMethod.getSignature());
        System.out.println(sootMethod.getBody());

//        Control Flow Graph
        StmtGraph<?> stmtGraph = sootMethod.getBody().getStmtGraph();
        String dotStr = DotExporter.buildGraph(stmtGraph, false, null, methodSignature);

        try{
            Files.write(Paths.get("src/test/resources/simple/EvenOdd.dot"), dotStr.getBytes());
            System.out.println("Generate the Dot file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
