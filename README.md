# sootup
https://soot-oss.github.io/SootUp/v1_2_0

### Copy dependencies
https://soot-oss.github.io/SootUp/v1_2_0/installation/

<aside>
💡 Add the JitPack repository to your build file

https://jitpack.io/#soot-oss/SootUp/develop-SNAPSHOT

```markup
<repositories><repository><id>jitpack.io</id><url>https://jitpack.io</url></repository></repositories>
```

</aside>

### Description
This project is about using a tool called SootUp to check Java web applications for
security issues. We're focusing on Java because it's a common language for
building applications, and we'll be looking specifically at how the program runs
(Java bytecode).

### Taint Analysis for Spotting Security Weak Spots
 - Objective: We aim to build a tool that can follow where sensitive data goes
within a Java application. Our goal is to find risky spots where unsafe data
could get to important parts of the application without being checked or
cleaned up first.
 - Java Applications:
For the project, consider exploring simple, open-source Java projects on GitHub
or my assignments, not utilizing the Spring framework. We'll focus on identifying
straightforward applications to ensure accessibility and ease of analysis.

### Project Steps:
1. Getting Started with SootUp: Set up SootUp right.
2. Stick to Java 8: Only look at applications built with Java 8.
3. Building the Taint Analysis Tool:
    - Create a basic tool that can follow untrusted data to see if it reaches
   critical areas of the application without being safely handled.
    - Concentrate on finding one specific type of security risk in the application,
   using a list of known risky data entry and exit points
### Graphviz
Install graphviz to generate png of CFG.

brew install graphviz

dot -T png EvenOdd.dot -o EvenOdd_cfg.png

dot -T png PrintInt.dot -o PrintInt_cfg.png

### Papers
https://courses.cs.washington.edu/courses/cse501/01wi/project/sable-thesis.pdf
