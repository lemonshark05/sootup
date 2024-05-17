package org.example;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.HashPointsToSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class Taints {

    static Map<SootMethod, FlowSet> methodEntryMap = new TreeMap<>(new Comparator<SootMethod>() {
        @Override
        public int compare(SootMethod m1, SootMethod m2) {
            return m1.getSignature().compareTo(m2.getSignature());
        }
    });

    static Map<SootMethod, FlowSet> methodExitMap = new TreeMap<>(new Comparator<SootMethod>() {
        @Override
        public int compare(SootMethod m1, SootMethod m2) {
            return m1.getSignature().compareTo(m2.getSignature());
        }
    });

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
        String inputRoute = "org.example.Demo5";
        SootConfig.setupSoot(inputRoute);
        SootClass sootClass = Scene.v().loadClassAndSupport(inputRoute);
        sootClass.setApplicationClass();
        Scene.v().setMainClass(sootClass);
        printJimple(sootClass);

        PackManager.v().runPacks();

        CallGraph callGraph = Scene.v().getCallGraph();
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                analyzeMethod(method, callGraph);
            }
        }
    }
    private static void analyzeMethod(SootMethod method, CallGraph callGraph) {
        if (methodExitMap.containsKey(method)) {
            // Already analysis this method
            return;
        }

        System.out.println("Analyzing method: " + method.getSignature());
        Body body = method.retrieveActiveBody();
        MyTaintAnalysis analysis = new MyTaintAnalysis(new ExceptionalUnitGraph(body));
        FlowSet entryFlow = analysis.entryInitialFlow();
        FlowSet exitFlow = new ArraySparseSet();
        analysis.merge(entryFlow, methodEntryMap.getOrDefault(method, new ArraySparseSet()), exitFlow);
        methodEntryMap.put(method, entryFlow);
        methodExitMap.put(method, exitFlow);
        analysis.printResults();

        // Iterator CallGraph to get all functions
        Iterator<Edge> edges = callGraph.edgesOutOf(method);
        while (edges.hasNext()) {
            SootMethod targetMethod = edges.next().tgt();
            if (targetMethod.isConcrete() && !methodExitMap.containsKey(targetMethod)) {
                analyzeMethod(targetMethod, callGraph);
            }
        }
    }

    private static class MyTaintAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {
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
//            System.out.println("Processing unit: " + unit);

            if (unit instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) unit;
                Value rightOp = stmt.getRightOp();
                Value leftOp = stmt.getLeftOp();

//                System.out.println("Processing unit: " + unit);
//                System.out.println("Input taint set: " + in);
//                System.out.println("Left Operand: " + leftOp + " of type " + leftOp.getType() + "Is left Operand a Local? " + (leftOp instanceof Local));
//                System.out.println("Right Operand: " + rightOp + " of type " + rightOp.getType() + "Is Right Operand a Local? " + (rightOp instanceof FieldRef));

                // Handle assignment involving FieldRefs
                if (leftOp instanceof FieldRef || rightOp instanceof FieldRef) {
                    handleFieldAssignment(leftOp, rightOp, in, out);
                }

                if (leftOp instanceof Local && rightOp instanceof Local) {
                    PointsToSet rightPts = pta.reachingObjects((Local) rightOp);
                    PointsToSet leftPts = pta.reachingObjects((Local) leftOp);
                    if (!rightPts.isEmpty() && rightPts.hasNonEmptyIntersection(leftPts)) {
                        out.add(leftOp);
                        analyzedValues.add(stmt);
                        System.out.println("Alias detected between " + leftOp + " and " + rightOp);
                    }
                }

                // Propagation logic for static fields and constructors
                if (rightOp instanceof StaticFieldRef || rightOp instanceof NewExpr) {
                    // Handle static fields or new expressions as sources
                    if (taintSource(rightOp)) {
                        out.add(leftOp);
                        analyzedValues.add(stmt);
                        System.out.println("New source of taint due to " + rightOp);
                    }
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
                    System.out.println("Source detected: " + rightOp);
                    out.add(leftOp);
                    analyzedValues.add(stmt);
                    System.out.println("Source identified and tainted: " + leftOp);
                }
            } else if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                handleMethodInvocation(invokeExpr, in, out);
            } else if (unit instanceof ReturnStmt) {

            }

            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    handleInvocation(stmt, in, out);
                }
            }
        }

        private void handleFieldAssignment(Value leftOp, Value rightOp, FlowSet in, FlowSet out) {
            PointsToSet rightPts = null;
            PointsToSet leftPts = null;

            // Handle right operand
            if (rightOp instanceof FieldRef) {
                rightPts = getFieldPointsToSet((FieldRef) rightOp);
            } else if (rightOp instanceof Local) {
                rightPts = pta.reachingObjects((Local) rightOp);
            }

            // Handle left operand
            if (leftOp instanceof FieldRef) {
                leftPts = getFieldPointsToSet((FieldRef) leftOp);
            } else if (leftOp instanceof Local) {
                leftPts = pta.reachingObjects((Local) leftOp);
            }

            // Handle intersection and aliasing
            if (rightPts != null && leftPts != null && !rightPts.isEmpty() && rightPts.hasNonEmptyIntersection(leftPts)) {
                out.add(leftOp);
                System.out.println("Alias detected between " + leftOp + " and " + rightOp);
            }
        }

        private PointsToSet getFieldPointsToSet(FieldRef fieldRef) {
            if (!(pta instanceof PAG)) {
                throw new IllegalStateException("PointsToAnalysis is not PAG as expected.");
            }
            PAG pag = (PAG) pta;

            PointsToSet pts;
            if (fieldRef instanceof InstanceFieldRef) {
                InstanceFieldRef instanceFieldRef = (InstanceFieldRef) fieldRef;
                Value base = instanceFieldRef.getBase();

                // If base is a local, fetch its points-to set.
                if (base instanceof Local) {
                    pts = pag.reachingObjects((Local) base, fieldRef.getField());
                } else {
                    pts = new HashPointsToSet(fieldRef.getType(), pag);
                }
            } else if (fieldRef instanceof StaticFieldRef) {
                // Handle static fields differently if required
                pts = new HashPointsToSet(fieldRef.getType(), pag);
            } else {
                pts = new HashPointsToSet(fieldRef.getType(), pag);
            }

            return pts;
        }


        private boolean taintSource(Value val) {
            // Implement logic to determine if a value is a taint source

            return val instanceof NewExpr;
        }

        private void handleMethodInvocation(InvokeExpr invokeExpr, FlowSet in, FlowSet out) {
            SootMethod method = invokeExpr.getMethod();

            if (isResourceCloseMethod(method)) {
                return; //Check whether the method is a resource closing method, if so, do not process it
            }

            List<Value> args = invokeExpr.getArgs();

            System.out.println("Method call: " + method.getSignature() + " with arguments " + args);

            if (methodExitMap.containsKey(method)) {
                FlowSet methodExitSet = methodExitMap.get(method);
                out.union(methodExitSet, out); // Update the current taint set with the taint set of the called method
            }
            // Propagate taint from actual parameters to formal parameters
            boolean isArgTainted = false;
            for (int i = 0; i < args.size(); i++) {
                Value arg = args.get(i);
                if (in.contains(arg)) {
                    if (!methodEntryMap.containsKey(method)) {
                        methodEntryMap.put(method, new ArraySparseSet());
                    }
                    methodEntryMap.get(method).add(arg);
                    isArgTainted = true;
                    System.out.println("Argument " + arg + " is tainted.");
                }
            }

            if (isArgTainted) {
                System.out.println("Arguments tainted for method " + method.getSignature());
            }

            // If the method has been analyzed and we have exit information, propagate it back
            if (methodExitMap.containsKey(method)) {
                FlowSet methodExitSet = methodExitMap.get(method);
                if (invokeExpr instanceof DefinitionStmt) {
                    DefinitionStmt def = (DefinitionStmt) invokeExpr;
                    Value leftOp = def.getLeftOp();
                    if (!methodExitSet.isEmpty()) {
                        out.add(leftOp);
                        System.out.println("Taint propagated to " + leftOp);
                    }
                } else {
                    out.union(methodExitSet, out);
                }
            }

        }

        private boolean isResourceCloseMethod(SootMethod method) {
            String methodName = method.getName();
            return methodName.equals("close") && method.getDeclaringClass().getType().toString().matches("java.sql.Connection|java.util.Scanner");
        }

        private void handleInvocation(Stmt stmt, FlowSet in, FlowSet out) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (isSink(stmt)) {
                System.out.println("Sink detected: " + stmt);
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
