package org.example;

import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.Map;

public class TaintTransformer extends SceneTransformer {

    private final String targetPackageName;
    private final String targetMethodName;

    public TaintTransformer(String targetPackageName, String targetMethodName) {
        this.targetPackageName = targetPackageName;
        this.targetMethodName = targetMethodName;
    }

    @Override
    protected void internalTransform(String s, Map<String, String> map) {
        System.out.println("====internal transform");
        SootClass sootClass = setupSoot(targetPackageName);
        printJimple(sootClass);
        analyzeMethodByName(sootClass, targetMethodName);
    }

    @SuppressWarnings("SameParameterValue")
    private static SootClass setupSoot(String mainClass) {
        SootConfig.setupSoot(mainClass);
        SootClass sootClass = Scene.v().loadClassAndSupport(mainClass);
        sootClass.setApplicationClass(); // as opposed to library class
        Scene.v().setMainClass(sootClass);

        return sootClass;
    }

    private static void printJimple(SootClass sootClass) {
        System.out.println("PRINTING JIMPLE========================================================================");
        System.out.println("public class " + sootClass.getName() + " extends " + sootClass.getSuperclass().getName());
        System.out.println("{");

        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                System.out.println("  " + method.getSubSignature() + " {");
                printMethodJimple(method);
                System.out.println("  }\n");
            }
        }
        System.out.println("}");
        System.out.println("END JIMPLE=============================================================================");
    }

    private static void printMethodJimple(SootMethod method) {
        Body body = method.retrieveActiveBody();
        for (Unit unit : body.getUnits()) {
            String unitStr = unit.toString();
            System.out.println("    " + unitStr.replace(";", ";\n    ")); // Indent and format statements
        }
    }

    // intra-method
    @SuppressWarnings("SameParameterValue")
    private static void analyzeMethodByName(SootClass sootClass, String targetMethodName) {
        CallGraph callGraph = Scene.v().getCallGraph();
        for (SootMethod method : sootClass.getMethods()) {
            System.out.println("analyzeMethodByName | checking methodSignature: " + method.getName());
            if (method.isConcrete() && method.getName().equals(targetMethodName)) {
                System.out.println("analyzeMethodByName | target method found: " + method.getName());
                analyzeMethod(method, callGraph);
            }
        }
    }

    private static void analyzeMethod(SootMethod method, CallGraph callGraph) {
        System.out.println("Analyzing method: " + method.getSignature());
        Body body = method.retrieveActiveBody();
        Taints.MyTaintAnalysis analysis = new Taints.MyTaintAnalysis(new ExceptionalUnitGraph(body));
        System.out.println();
        System.out.println("=========TAINT INFORMATION===========");
        System.out.println(analysis.getTaintInfoString());
    }
}
