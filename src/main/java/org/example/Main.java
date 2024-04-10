package org.example;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation;

import java.util.Collections;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        AnalysisInputLocation inputLocation =
                new JavaSourcePathAnalysisInputLocation("src/test/resources/simple/");

        JavaView view = new JavaView(inputLocation);
        JavaClassType classType = view.getIdentifierFactory().getClassType("HelloWorld");
        Optional<JavaSootClass> classOptional = view.getClass(classType);
        if(!classOptional.isPresent()) {
            System.out.println("Class not founc");
            return;
        }

        JavaSootClass sootClass = classOptional.get();
        MethodSignature methodSignature = view.getIdentifierFactory()
                .getMethodSignature(classType, "main",
                        "void", Collections.singletonList("java.lang.String[]"));
        Optional<JavaSootMethod> method = view.getMethod(methodSignature);
        if (!method.isPresent()){
            System.out.println("Method not found");
            return;
        }

        JavaSootMethod sootMethod = method.get();
        System.out.println(sootMethod.getSignature());
        System.out.println(sootMethod.getBody());
    }
}