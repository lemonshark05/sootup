package org.example;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;


public class PTATest {
    public static final String className = "org.example.Demo3";

    public static Map<Value, Set<Integer>> workList = new LinkedHashMap<>();

    public static void main(String[] args) {
        SootConfig.setupSoot(className);
        SootClass sootClass = Scene.v().getSootClass(className);
        //print Jimple
        printJimple(sootClass);

        SootMethod entryMethod = sootClass.getMethodByName("main");
        Solve(entryMethod);
        for (Map.Entry<Value, Set<Integer>> entry : workList.entrySet()) {
            Value key = entry.getKey();
            Set<Integer> value = entry.getValue();
            for (Integer allocId : value) {
                System.out.println(key.getType().toString()+" "+allocId);
            }
        }

        PointsToAnalysis pointsToAnalysis = Scene.v().getPointsToAnalysis();
        for (Local local : entryMethod.retrieveActiveBody().getLocals()) {
            System.out.println(local.toString() + " | ");
            PointsToSet pointsToSet = pointsToAnalysis.reachingObjects(local);
            if(!pointsToSet.isEmpty()){
                for (Type possibleType : pointsToSet.possibleTypes()) {
                    System.out.println(possibleType);
                }
            }
        }

    }

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

    private static void Solve(SootMethod entryMethod) {

        Body body = entryMethod.retrieveActiveBody();
        UnitGraph graph = new BriefUnitGraph(body);
        Unit headUnit = graph.getHeads().iterator().next();
        for (Unit unit : body.getUnits()) {
            solveUnit(unit);
        }

        solveBlock(headUnit, graph);
    }

    private static void solveBlock(Unit startUnit, UnitGraph graph) {
        Queue<Unit> queue = new LinkedList<>();
        Set<Unit> visited = new HashSet<>();
        queue.add(startUnit);

        while (!queue.isEmpty()) {
            Unit currentUnit = queue.poll();
            if (!visited.contains(currentUnit)) {
                visited.add(currentUnit);
                solveUnit(currentUnit, graph);
                queue.addAll(graph.getSuccsOf(currentUnit));
            }
        }
    }

    public static void solveUnit(Unit unit, UnitGraph graph) {
        try {
            unit.apply(new AbstractStmtSwitch() {
                @Override
                public void caseAssignStmt(AssignStmt stmt) {
                    AssignStmt assignStmt = stmt;
                    Value lop = assignStmt.getLeftOp();
                    Value rop = assignStmt.getRightOp();

                    // Example usage of graph
                    List<Unit> successors = graph.getSuccsOf(stmt);
                    System.out.println("Successors of " + stmt + ": " + successors);

                    if (rop instanceof AnyNewExpr) {
                        int allocId = stmt.getJavaSourceStartLineNumber();
                        Set<Integer> allocIdSet = workList.getOrDefault(lop, new LinkedHashSet<>());
                        allocIdSet.add(allocId);
                        workList.put(lop, allocIdSet);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void solveUnit(Unit unit){
        try{
            unit.apply(new AbstractStmtSwitch() {
                @Override
                public void caseAssignStmt(AssignStmt stmt) {
                    AssignStmt assignStmt = stmt;
                    Value lop = assignStmt.getLeftOp();
                    Value rop = assignStmt.getRightOp();
                    if(rop instanceof AnyNewExpr){
                        // add to Map
                        int allodId = stmt.getJavaSourceStartLineNumber();
                        Set<Integer> allocIdSet = workList.getOrDefault(lop, new LinkedHashSet<>());
                        allocIdSet.add(allodId);
                        workList.put(lop, allocIdSet);
                    }
                }
            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }



}