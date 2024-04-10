package org.example;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import sootup.core.Language;
import sootup.core.Project;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.signatures.MethodSignature;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.util.Collections;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation("src/test/resources/simple/");

        JavaView view = new JavaView((Project<JavaSootClass, ? extends JavaView>) inputLocation);

        JavaClassType classType = (JavaClassType) view.getIdentifierFactory().getClassType("HelloWorld");
        Optional<JavaSootClass> classOptional = view.getClass(classType);
        if(!classOptional.isPresent()) {
            System.out.println("Class not found");
            return;
        }

        JavaSootClass sootClass = classOptional.get();
        MethodSignature methodSignature = view.getIdentifierFactory()
                .getMethodSignature(classType,"main",
                        "void", Collections.singletonList("java.lang.String[]"));
        Optional<JavaSootMethod> method = (Optional<JavaSootMethod>) view.getMethod(methodSignature);
        if (!method.isPresent()){
            System.out.println("Method not found");
        }

        JavaSootMethod sootMethod
    }
}