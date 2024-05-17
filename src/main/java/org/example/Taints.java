package org.example;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Taints {

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
        System.out.println("END      JIMPLE========================================================================");
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

    public static void main(String[] args) {
        SootClass sootClass = setupSoot("org.example.Demo1");
        printJimple(sootClass);

        PackManager.v().runPacks();

        analyzeMethods(sootClass);
    }

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

        // Iterator CallGraph to get all functions
        Iterator<Edge> edges = callGraph.edgesOutOf(method);
        while (edges.hasNext()) {
            SootMethod targetMethod = edges.next().tgt();
            if (targetMethod.isConcrete()) {
                analyzeMethod(targetMethod, callGraph);
            }
        }
    }

    private static class MyTaintAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Local>> {
        private Body body;
        private Set<Unit> analyzedValues;

        private PointsToAnalysis pta;

        public MyTaintAnalysis(UnitGraph graph) {
            super(graph);
            this.body = ((ExceptionalUnitGraph) graph).getBody();
            this.analyzedValues = new HashSet<>();
            this.pta = Scene.v().getPointsToAnalysis();
            doAnalysis();
        }

        @Override
        protected FlowSet<Local> newInitialFlow() {
            return new ArraySparseSet<Local>();
        }

        @Override
        protected FlowSet<Local> entryInitialFlow() {
            return new ArraySparseSet<Local>();
        }

        @Override
        protected void flowThrough(FlowSet in, Unit unit, FlowSet out) {
            in.copy(out);

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
                        // Check if the base object is tainted
                        if (in.contains(instanceInvoke.getBase())) {
                            tainted = true;
                        }
                    }

                    // Check if any argument is tainted
                    for (Value arg : invokeExpr.getArgs()) {
                        if (in.contains(arg)) {
                            tainted = true;
                            break;
                        }
                    }

                    if (tainted) {
                        out.add(leftOp);
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
                    if (in.contains(returnValue)) {

                    }
                }

                // Check if the leftOp points to any object that rightOp points to (aliasing)
                if (rightOp instanceof Local && in.contains(rightOp)) {
                    out.add(leftOp);
                    analyzedValues.add(stmt);
                    System.out.println("Tainted " + leftOp + " due to tainted right operand: " + rightOp);
                }

                // Special handling for string concatenations
                if (rightOp instanceof BinopExpr && rightOp.getType() instanceof RefType &&
                        ((RefType)rightOp.getType()).toString().equals("java.lang.String")) {
                    BinopExpr expr = (BinopExpr) rightOp;
                    if (in.contains(expr.getOp1()) || in.contains(expr.getOp2())) {
                        out.add(leftOp);
                        analyzedValues.add(stmt);
                        System.out.println("Tainting due to string concatenation in: " + stmt);
                    }
                }

                // Directly propagate taint from right to left if right is tainted
                if (in.contains(rightOp)) {
                    out.add(leftOp);
                    analyzedValues.add(stmt);
                    System.out.println("Tainted " + leftOp + " due to tainted right operand: " + rightOp);
                }

                // Check and mark the sources
                if (isSource(rightOp)) {
                    out.add(leftOp);
                    analyzedValues.add(stmt);
                    System.out.println("Source identified and tainted: " + leftOp);
                }
            }

            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    handleInvocation(stmt, in, out);
                }
            }
        }

        private void handleMethodInvocation(InvokeExpr invokeExpr, FlowSet in, FlowSet out) {
            // Check for taint in arguments and propagate to method's context if required
            for (Value arg : invokeExpr.getArgs()) {
                if (in.contains(arg)) {
                    System.out.println("Argument tainted: " + arg);
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
            in1.union(in2, out); // union(FlowSet otherFlow, FlowSet destFlow); why is it like this?
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
