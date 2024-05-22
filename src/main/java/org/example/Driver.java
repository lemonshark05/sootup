package org.example;

import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Driver {

    public static void main(String[] args) {
        String[] sootArgs = new String[] {
                "-w", // Enable whole-program analysis
                "-process-dir", "src/test/resources/tests", // Directory with compiled classes
                "-main-class", "org.example.Demo1", // Specify the main class
                "-output-format", "J", // Output format (Jimple, for example)
                "-output-dir", "sootOutput" // Directory to store Soot output
        };

        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("wjtp.myTransform", "enabled:true");
        Options.v().set_verbose(true);

        String targetMethodName = "loginUsers";
        String targetPackageName = "org.example.Demo1";

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTransform",
                new TaintTransformer(targetPackageName, targetMethodName)));
        System.out.println("in driver");
        soot.Main.main(sootArgs);
    }
}
