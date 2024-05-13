package org.example;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.HashSet;
import java.util.Set;

public class Taints {

    private static void printJimple(SootClass sootClass) {
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
    }

    private static void printMethodJimple(SootMethod method) {
        Body body = method.retrieveActiveBody();
        for (Unit unit : body.getUnits()) {
            String unitStr = unit.toString();
            System.out.println("    " + unitStr.replace(";", ";\n    ")); // Indent and format statements
        }
    }
    public static void main(String[] args) {
        SootConfig.setupSoot("org.example.Demo3");
        SootClass sootClass = Scene.v().loadClassAndSupport("org.example.Demo3");
        sootClass.setApplicationClass();
        Scene.v().setMainClass(sootClass);

        for (SootMethod method : sootClass.getMethods()) {
            if (method.getName().equals("main")) {
                Body b = method.retrieveActiveBody();
                MyTaintAnalysis analysis = new MyTaintAnalysis(new ExceptionalUnitGraph(b));
                analysis.printResults();
//                printJimple(sootClass);
            }
        }

//        // Register the analysis as a transformation that gets applied on all methods
//        PackManager.v().getPack("jtp").add(new Transform("jtp.myTaintAnalysis", new BodyTransformer() {
//            @Override
//            protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
//                // Perform the analysis on the method's body
//                MyTaintAnalysis analysis = new MyTaintAnalysis(new ExceptionalUnitGraph(b));
//                printJimple(b.getMethod().getDeclaringClass());
//                analysis.printResults();
//            }
//        }));
//
//        PackManager.v().runPacks();
    }

    private static class MyTaintAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {
        private Body body;
        private Set<Unit> analyzedValues;

        public MyTaintAnalysis(UnitGraph graph) {
            super(graph);
            this.body = ((ExceptionalUnitGraph) graph).getBody();
            this.analyzedValues = new HashSet<>();
            doAnalysis();
        }

        @Override
        protected FlowSet newInitialFlow() {
            return new ArraySparseSet();
        }

        @Override
        protected FlowSet entryInitialFlow() {
            return new ArraySparseSet();
        }

        @Override
        protected void flowThrough(FlowSet in, Unit unit, FlowSet out) {
            in.copy(out);

            if (unit instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) unit;
                Value rightOp = stmt.getRightOp();
                Value leftOp = stmt.getLeftOp();

                // If the right side is a source, add the left side to the out set as tainted
                if (isSource(rightOp)) {
                    out.add(leftOp);
                    analyzedValues.add(stmt);  // Mark this statement for review
                }

                // If the right side is tainted, add the left side to the out set as tainted
                if (in.contains(rightOp)) {
                    out.add(leftOp);
                    analyzedValues.add(stmt);
                }

                // Propagate taint for string operations
                if (rightOp instanceof BinopExpr && rightOp.getType() instanceof RefType &&
                        ((RefType)rightOp.getType()).toString().equals("java.lang.String")) {
                    BinopExpr expr = (BinopExpr) rightOp;
                    if (in.contains(expr.getOp1()) || in.contains(expr.getOp2())) {
                        out.add(leftOp);
                        analyzedValues.add(stmt);
                    }
                }
            }

            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    handleInvocation(stmt, in, out);
                }
            }
        }

        private void handleInvocation(Stmt stmt, FlowSet in, FlowSet out) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (isSink(stmt)) {
                for (Value arg : invokeExpr.getArgs()) {
                    if (in.contains(arg)) {
                        System.out.println("Detected potential SQL Injection: " + stmt);
                        analyzedValues.add(stmt);
                    }
                }
            }
        }

        @Override
        protected void merge(FlowSet in1, FlowSet in2, FlowSet out) {
            in1.union(in2, out);
        }

        @Override
        protected void copy(FlowSet source, FlowSet dest) {
            source.copy(dest);
        }

        private boolean isSource(Value value) {
            if (value instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) value;
                SootMethod method = invokeExpr.getMethod();
                return method.getDeclaringClass().getName().equals("java.util.Scanner") &&
                        method.getName().equals("nextLine");
            }
            return false;
        }

        private boolean isSink(Stmt stmt) {
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                SootMethod method = invokeExpr.getMethod();
                return method.getDeclaringClass().getName().equals("java.sql.Statement") &&
                        (method.getName().equals("executeQuery") || method.getName().equals("executeUpdate") || method.getName().equals("execute"));
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
