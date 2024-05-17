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
    }

    // TEMPORARILY only dealing with Local, would need to change going forwards.
    private static class MyTaintAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {
        private Body body;
        private Set<Unit> analyzedValues;

        // using unit as an identifier for both program point and src
        private HashMap<Unit, HashMap<Value, Set<Unit>>> store; // does flowset provide the same function

        // assign identifier to source and sink units, useful at all?
        private HashMap<Unit, String> srcs; // @TODO create addSrc method to like a hashcons
        private HashMap<Unit, String> snks; // @TODO create addSnk method to like a hashcons

        private PointsToAnalysis pta;

        public MyTaintAnalysis(UnitGraph graph) {
            super(graph);
            this.body = ((ExceptionalUnitGraph) graph).getBody();
            this.analyzedValues = new HashSet<>();
            this.store = new HashMap<>();
            this.pta = Scene.v().getPointsToAnalysis();
            doAnalysis();

            /*
            System.out.println();
            System.out.println("== THE STORE ==");
            System.out.println(store.toString());
            System.out.println();
            */

            System.out.println("graph check debug start+++++++++++++++++++++++++++++++++++");
            for (Map.Entry<Unit, FlowSet> i : this.unitToAfterFlow.entrySet()) {
                Stmt stmt = (Stmt) i.getKey();
                System.out.println(stmt.toString());
            }
            System.out.println("graph check debug end+++++++++++++++++++++++++++++++++++");
        }

        @Override
        protected void merge(FlowSet in1, FlowSet in2, FlowSet out) {
            in1.union(in2, out);
        }

        @Override
        protected void copy(FlowSet source, FlowSet dest) {
            source.copy(dest);
        }

        // start with no taints
        @Override
        protected FlowSet entryInitialFlow() {
            return new ArraySparseSet();
        }

        // new set with no taints
        @Override
        protected FlowSet newInitialFlow() {
            return new ArraySparseSet();
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
                    System.out.println("++++++++++++++++++++leftOp.toString():" + leftOp.toString());
                    storeSet(unit, leftOp);
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

        private void addSrc(Object value, Unit unit, FlowSet flowset) {
            if (value instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) value;
                SootMethod method = invokeExpr.getMethod();
                String classMethodString = method.getDeclaringClass().getName() + "." + method.getName();


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

        // store[x] = store[x] union {u}
        private void storeAdd(Unit u, Value v) {
            HashMap<Value, Set<Unit>> valueMap = store.computeIfAbsent(u, k -> new HashMap<>());
            Set<Unit> unitSet = valueMap.computeIfAbsent(v, k -> new HashSet<>());

            unitSet.add(u);
        }

        // store[x] = {u}
        private void storeSet(Unit u, Value v) {
            HashMap<Value, Set<Unit>> valueMap = store.computeIfAbsent(u, k -> new HashMap<>());
            Set<Unit> unitSet = new HashSet<>();
            unitSet.add(u);

            valueMap.put(v, unitSet);
        }

        // store[x] = {}
        private void storeClear(Unit u, Value v) {
            HashMap<Value, Set<Unit>> valueMap = store.computeIfAbsent(u, k -> new HashMap<>());
            Set<Unit> unitSet = new HashSet<>();

            valueMap.put(v, unitSet);
        }

        // SELF REMINDER IT'S A REFERENCE YOU CAN MODIFY IT
        private Set<Unit> storeGet(Unit u, Value v) {
            HashMap<Value, Set<Unit>> valueMap = store.get(u);
            if (valueMap == null) {
                throw new IllegalArgumentException("storeGet: Unit " + u + " not found in store.");
            }

            Set<Unit> unitSet = valueMap.get(v);
            if (unitSet == null) {
                throw new IllegalArgumentException("storeGet: Value " + v + " not found in store.");
            }

            return unitSet;
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
