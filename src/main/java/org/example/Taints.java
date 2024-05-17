package org.example;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class Taints {

    // Debug variables
    private static final String TARGET_METHOD_NAME = "loginUsers";
    private static final String TARGET_PACKAGE_NAME = "org.example.Demo1";

    // @TODO case sensitivity?
    private static final String[] TAINT_SRCS = {
            "java.util.Scanner.nextLine"
    };
    private static final String[] TAINT_SNKS = {
            "java.sql.Statement.executeQuery",
            "java.sql.Statement.executeUpdate",
            "java.sql.Statement.execute"
    };

    public static void main(String[] args) {
        SootClass sootClass = setupSoot(TARGET_PACKAGE_NAME);
        printJimple(sootClass);

        PackManager.v().runPacks();

        analyzeMethodByName(sootClass, TARGET_METHOD_NAME); // intra-method


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

    @SuppressWarnings("SameParameterValue")
    private static SootClass setupSoot(String mainClass) {
        SootConfig.setupSoot(mainClass);
        SootClass sootClass = Scene.v().loadClassAndSupport(mainClass);
        sootClass.setApplicationClass(); // as opposed to library class
        Scene.v().setMainClass(sootClass);

        return sootClass;
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

    // DOES NOT WORK FOR NOW
    private static void analyzeMethods(SootClass sootClass) {
        CallGraph callGraph = Scene.v().getCallGraph();
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                analyzeMethod(method, callGraph);
            }
        }
    }

    private static void analyzeMethod(SootMethod method, CallGraph callGraph) {
        System.out.println("Analyzing method: " + method.getSignature());
        Body body = method.retrieveActiveBody();
        MyTaintAnalysis analysis = new MyTaintAnalysis(new ExceptionalUnitGraph(body));
        analysis.printResults();
        System.out.println();
        System.out.println("=========TAINT INFORMATION===========");
        System.out.println(analysis.getTaintInfoString());
    }

    private static class MyTaintAnalysis extends ForwardFlowAnalysis<Unit, TaintStore<Value, Unit>> {
        private Body body;
        private Set<Unit> analyzedValues;

        // using unit as an identifier for both program point and src
        // @TODO this will have to go become FlowSet for flowThrough eventually
        private Map<Unit, Set<Unit>> sinkToSourceMap;

        private PointsToAnalysis pta;

        public MyTaintAnalysis(UnitGraph graph) {
            super(graph);
            this.body = ((ExceptionalUnitGraph) graph).getBody();
            this.analyzedValues = new HashSet<>();
            this.sinkToSourceMap = new LinkedHashMap<>();
            this.pta = Scene.v().getPointsToAnalysis();
            doAnalysis();

            /*
            System.out.println();
            System.out.println("== THE STORE ==");
            System.out.println(store.toString());
            System.out.println();


            System.out.println("graph check debug start+++++++++++++++++++++++++++++++++++");
            for (Map.Entry<Unit, FlowSet> i : this.unitToAfterFlow.entrySet()) {
                Stmt stmt = (Stmt) i.getKey();
                System.out.println(stmt.toString());
            }
            System.out.println("graph check debug end+++++++++++++++++++++++++++++++++++");
            */
        }

        @Override
        protected void merge(TaintStore<Value, Unit> in1, TaintStore<Value, Unit> in2, TaintStore<Value, Unit> out) {
            in1.union(in2, out);
        }

        @Override
        protected void copy(TaintStore<Value, Unit> source, TaintStore<Value, Unit> dest) {
            source.copy(dest);
        }

        // start with no taints
        @Override
        protected TaintStore<Value, Unit> entryInitialFlow() {
            return new TaintStore<Value, Unit>();
        }

        // new set with no taints
        @Override
        protected TaintStore<Value, Unit> newInitialFlow() {
            return new TaintStore<Value, Unit>();
        }

        @Override
        protected void flowThrough(TaintStore<Value, Unit> in, Unit unit, TaintStore<Value, Unit> out) {

            in.copy(out); // set current set of tainted vars to previous instruction's set

            if (unit instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) unit;
                Value rightOp = stmt.getRightOp();
                Value leftOp = stmt.getLeftOp();

//                System.out.println("Processing unit: " + unit);
//                System.out.println("Input taint set: " + in);


                // Check and handle rightOp if it's an instance of StaticFieldRef
                if (rightOp instanceof StaticFieldRef) {
                    // Handle static field reference specifically
                    // Maybe check if it's a source of taint or something similar
                }

                // Propagate taint if the right side is already tainted
                if (rightOp instanceof InvokeExpr) {
                    InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                    boolean tainted = false;

                    // Check for instance method calls
                    if (invokeExpr instanceof InstanceInvokeExpr) {
                        InstanceInvokeExpr instanceInvoke = (InstanceInvokeExpr) invokeExpr;

                        Set<Unit> taintedBy = in.getTaints(instanceInvoke.getBase());
                        for (Unit taint : taintedBy) {

                        }

                        // Check if the base object is tainted
                        if (in.isTainted(instanceInvoke.getBase())) {

                            tainted = true;
                        }
                    }

                    // Check if any argument is tainted
                    for (Value arg : invokeExpr.getArgs()) {
                        if (in.isTainted(arg)) {
                            tainted = true;
                            break;
                        }
                    }

                    if (tainted) {
                        out.addTaint(leftOp, unit);
                        analyzedValues.add(stmt);
                        System.out.println("Tainted " + leftOp + " due to tainted argument or base in: " + rightOp);
                    }
                }

                if (unit instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) unit;
                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                    handleMethodInvocation(invokeExpr, in, out);
                }

                if (unit instanceof ReturnStmt) {
                    ReturnStmt returnStmt = (ReturnStmt) unit;
                    Value returnValue = returnStmt.getOp();
                    if (in.isTainted(returnValue)) {

                    }
                }

                // Check if the leftOp points to any object that rightOp points to (aliasing)
                if (rightOp instanceof Local && in.isTainted(rightOp)) {
                    out.addTaint(leftOp, unit);
                    analyzedValues.add(stmt);
                    System.out.println("Tainted " + leftOp + " due to tainted right operand: " + rightOp);
                }

                // Special handling for string concatenations
                if (rightOp instanceof BinopExpr && rightOp.getType() instanceof RefType &&
                        ((RefType)rightOp.getType()).toString().equals("java.lang.String")) {
                    BinopExpr expr = (BinopExpr) rightOp;
                    if (in.isTainted(expr.getOp1()) || in.isTainted(expr.getOp2())) {
                        out.addTaint(leftOp, unit);
                        analyzedValues.add(stmt);
                        System.out.println("Tainting due to string concatenation in: " + stmt);
                    }
                }

                // Directly propagate taint from right to left if right is tainted
                if (in.isTainted(rightOp)) {
                    out.addTaint(leftOp, unit);
                    analyzedValues.add(stmt);
                    System.out.println("Tainted " + leftOp + " due to tainted right operand: " + rightOp);
                }

                // Check and mark the sources
                if (isSource(rightOp)) {
                    out.setTaint(leftOp, unit);
                    analyzedValues.add(stmt);
                    System.out.println("Source identified and tainted: " + leftOp);
                }
            }

            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    checkStmtForSink(stmt, in, out);
                }
            }
        }

        private void checkStmtForSink(Stmt stmt, TaintStore<Value, Unit> in, TaintStore<Value, Unit> out) {
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                if (isSink(stmt)) {
                    for (Value arg : invokeExpr.getArgs()) {
                        // @TODO when should we sink it? what happens if we sink and then the taints get reset?
                        // right it adds in every source that it encounters
                        Set<Unit> sourceSet = sinkToSourceMap.computeIfAbsent(stmt, k -> new LinkedHashSet<>());
                        sourceSet.addAll(in.getTaints(arg));
                        if (in.isTainted(arg)) {
                            System.out.println("Detected potential SQL Injection: " + stmt);
                            analyzedValues.add(stmt);
                        }
                    }
                }
            }
        }

        private String getTaintInfoString() {
            StringBuilder output = new StringBuilder();
            for (Map.Entry<Unit, Set<Unit>> snksrcentry : sinkToSourceMap.entrySet()) {
                Unit snk = snksrcentry.getKey();
                for (Unit src : snksrcentry.getValue()) {
                    output.append("{").append(src.toString()).append("}");
                    output.append(" --> ");
                    output.append("{").append(snk.toString()).append("}");
                    output.append('\n');
                }
            }
            return output.toString();
        }

        private void handleMethodInvocation(InvokeExpr invokeExpr, TaintStore<Value, Unit> in, TaintStore<Value, Unit> out) {
            // Check for taint in arguments and propagate to method's context if required
            for (Value arg : invokeExpr.getArgs()) {
                if (in.isTainted(arg)) {
                    System.out.println("Argument tainted: " + arg);
                }
            }

        }

        private void handleInvocation(Stmt stmt, TaintStore<Value, Unit> in, TaintStore<Value, Unit> out) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (isSink(stmt)) {
                for (Value arg : invokeExpr.getArgs()) {
                    if (in.isTainted(arg)) {
                        System.out.println("Detected potential SQL Injection: " + stmt);
                        analyzedValues.add(stmt);
                    }
                }
            }
        }

        private boolean isSource(Object value) {
            if (value instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) value;
                SootMethod method = invokeExpr.getMethod();
                String classMethodString = method.getDeclaringClass().getName() + "." + method.getName();
                for (String src : TAINT_SRCS) {
                    if (classMethodString.equals(src)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isSink(Stmt stmt) {
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                SootMethod method = invokeExpr.getMethod();
                String classMethodString = method.getDeclaringClass().getName() + "." + method.getName();
                for (String snk : TAINT_SNKS) {
                    if (classMethodString.equals(snk)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void printResults() {
            System.out.println("Taint Analysis Results:");
            for (Unit u : body.getUnits()) {
                if (analyzedValues.contains(u)) {
                    System.out.println("Tainted unit: " + u);
                }
            }
        }
    }
}
