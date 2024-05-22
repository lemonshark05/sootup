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
        System.out.println();
        System.out.println("=========TAINT INFORMATION===========");
        System.out.println(analysis.getTaintInfoString());
    }

    static class MyTaintAnalysis extends ForwardFlowAnalysis<Unit, TaintStore<Value, Unit>> {
        private Body body;
        private PointsToAnalysis pta;

        private final Map<Unit, Set<Unit>> sinkToSourceMap;

        public MyTaintAnalysis(UnitGraph graph) {
            super(graph);
            this.body = ((ExceptionalUnitGraph) graph).getBody();
            this.sinkToSourceMap = new LinkedHashMap<>();
            this.pta = Scene.v().getPointsToAnalysis(); // @TODO we haven't set one
            doAnalysis();
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

            // Handles all assign statements.
            if (unit instanceof JAssignStmt stmt) {
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
                if (rightOp instanceof InvokeExpr invokeExpr) {

                    // Check for instance method calls
                    if (invokeExpr instanceof InstanceInvokeExpr instanceInvoke) {

                        Set<Unit> taintedBy = in.getTaints(instanceInvoke.getBase());
                        for (Unit taint : taintedBy) {

                        }

                        out.propagateTaints(instanceInvoke.getBase(), leftOp);
                    }

                    // Check if any argument is tainted
                    for (Value arg : invokeExpr.getArgs()) {
                        out.propagateTaints(arg, leftOp);
                    }
                }

                if (unit instanceof InvokeStmt invokeStmt) {
                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                    handleMethodInvocation(invokeExpr, in, out);
                }

                if (unit instanceof ReturnStmt returnStmt) {
                    Value returnValue = returnStmt.getOp();
                    if (in.isTainted(returnValue)) {

                    }
                }

                // Check if the leftOp points to any object that rightOp points to (aliasing)
                // COPY INSTRUCTION, SET INSTEAD OF PROPAGATE
                if (rightOp instanceof Local) {
                    out.setTaints(leftOp, out.getTaints(rightOp));
                }

                // attempt at generalized binop
                // @TODO we should set instead of propagate here maybe? double check.
                if (rightOp instanceof BinopExpr binopexpr) {
                    out.propagateTaints(binopexpr.getOp1(), leftOp);
                    out.propagateTaints(binopexpr.getOp2(), leftOp);
                }

                // @TODO can we generalize this to be used with any BinopExpr
                /*
                // Special handling for string concatenations
                LEGACY
                if (rightOp instanceof BinopExpr && rightOp.getType() instanceof RefType &&
                        ((RefType)rightOp.getType()).toString().equals("java.lang.String")) {
                    BinopExpr expr = (BinopExpr) rightOp;
                    if (in.isTainted(expr.getOp1()) || in.isTainted(expr.getOp2())) {
                        out.addTaint(leftOp, unit);
                        analyzedValues.add(stmt);
                        System.out.println("Tainting due to string concatenation in: " + stmt);
                    }
                }
                 */

                // Directly propagate taint from right to left if right is tainted
                // @TODO is this a catch-all?
                out.propagateTaints(rightOp, leftOp);
                /*
                LEGACY
                if (in.isTainted(rightOp)) {
                    out.addTaint(leftOp, unit);
                    analyzedValues.add(stmt);
                    System.out.println("Tainted " + leftOp + " due to tainted right operand: " + rightOp);
                }
                 */

                // Check and mark the sources
                if (isSource(rightOp)) {
                    out.setTaint(leftOp, unit);
                    System.out.println("Source identified and tainted: " + leftOp);
                }
            } else {
                System.out.println(unit.getClass().getName());
            }

            // detached sink checking
            if (unit instanceof Stmt stmt) {
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

                        /*
                        LEGACY
                        if (in.isTainted(arg)) {
                            System.out.println("Detected potential SQL Injection: " + stmt);
                            analyzedValues.add(stmt);
                        }
                         */
                    }
                }
            }
        }

        public String getTaintInfoString() {
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

        private boolean isSource(Object value) {
            if (value instanceof InvokeExpr invokeExpr) {
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
    }
}
