package org.example;

import soot.*;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SootConfig {

    public static final String sourceDirectory = System.getProperty("user.dir")+ File.separator + "target" + File.separator + "classes";

    public static List<String> excludeClassList;

    public static void setupSoot(String classname){
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_soot_classpath(sourceDirectory);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));
        Options.v().set_whole_program(true);
        Options.v().set_verbose(true);
        Options.v().setPhaseOption("jb","use-original-names:true");
//        Options.v().setPhaseOption("jb.dae","only-stack-locals:true");
//        Options.v().setPhaseOption("jb.cp", "enabled:false");
//        Options.v().setPhaseOption("jb.ls","enabled:false");
//        Options.v().setPhaseOption("jb.dae","enabled:false");
//        Options.v().setPhaseOption("jb.ulp","unsplit-original-locals:false");
//        Options.v().setPhaseOption("jb.a","enabled:false");
//        Options.v().setPhaseOption("jb.cp","enabled:false");

        Scene.v().loadNecessaryClasses();
        SootClass sootClass = Scene.v().loadClassAndSupport(classname);
        sootClass.setApplicationClass();

        // add to-exlude classes
        Options.v().set_exclude(addExcludeClasses());

        PackManager.v().runPacks();

        // Enable SPARK call-graph construction
        Options.v().setPhaseOption("cg.spark","on");
        Options.v().setPhaseOption("cg.spark","enabled:true");
        Options.v().setPhaseOption("cg.spark","verbose:true");
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");

//        PackManager.v().writeOutput();

    }

    public static void getBasicInfo(){
        //获取含有main方法的类
        SootClass mainClass = Scene.v().getMainClass();

        //获取main方法
        SootMethod mainMethod = Scene.v().getMainMethod();

        //获取运行时类 应用类 基础类 所有类
        Chain<SootClass> libraryClasses = Scene.v().getLibraryClasses();
        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
        Set<String> basicClasses = Scene.v().getBasicClasses();
        Chain<SootClass> classes = Scene.v().getClasses();

        //获取当前soot的分析路径：通常为classpath+app-path
        String sootClassPath = Scene.v().getSootClassPath();

        //获取默认JVMclasspath的路径
        String s = Scene.v().defaultClassPath();//rt.jar path
    }

    // 增加不需要的类
    public static List<String> addExcludeClasses(){
        if(excludeClassList == null){
            excludeClassList = new ArrayList<String>();
        }
        // 前缀名区别
        excludeClassList.add("java.");
        excludeClassList.add("javax.");
        excludeClassList.add("sun.");
        excludeClassList.add("sunw.");
        excludeClassList.add("com.sun.");
        excludeClassList.add("com.ibm.");

        return excludeClassList;

    }

}