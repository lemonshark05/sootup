package org.example;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.language.JavaJimple;
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

        boolean found = sootMethod.getBody().getStmts().stream().anyMatch(
                stmt -> stmt instanceof JIdentityStmt
                && stmt.getInvokeExpr() instanceof JVirtualInvokeExpr
                && stmt.getInvokeExpr().getMethodSignature().getName().equals("println")
                && stmt.getInvokeExpr().getArg(0).equivTo(JavaJimple.getInstance().newStringConstant("Hello World!"))
        );

        if(found){
            System.out.println("found println");
        }else{
            System.out.println("No found println");
        }
    }
}