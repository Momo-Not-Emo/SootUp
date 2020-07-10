package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.java6;


import de.upb.swt.soot.core.model.SootClass;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.MinimalSourceTestSuiteBase;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class StaticImportTest extends MinimalSourceTestSuiteBase {

  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        "mathFunctions", getDeclaredClassSignature(), "void", Collections.emptyList());
  }

  @Test
  public void test() {
    SootMethod method = loadMethod(getMethodSignature());
    assertJimpleStmts(method, expectedBodyStmts());
    SootClass sootClass = loadClass(getDeclaredClassSignature());
  }

  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "r0 := @this: StaticImport",
            "$r1 = <java.lang.System: java.io.PrintStream out>",
            "$d0 = staticinvoke <java.lang.Math: double sqrt(double)>(4)",
            "virtualinvoke $r1.<java.io.PrintStream: void println(double)>($d0)",
            "$r2 = <java.lang.System: java.io.PrintStream out>",
            "$d1 = staticinvoke <java.lang.Math: double pow(double,double)>(2, 5)",
            "virtualinvoke $r2.<java.io.PrintStream: void println(double)>($d1)",
            "$r3 = <java.lang.System: java.io.PrintStream out>",
            "$d2 = staticinvoke <java.lang.Math: double ceil(double)>(5.6)",
            "virtualinvoke $r3.<java.io.PrintStream: void println(double)>($d2)",
            "$r4 = <java.lang.System: java.io.PrintStream out>",
            "virtualinvoke $r4.<java.io.PrintStream: void println(java.lang.String)>(\"Static import for System.out\")",
            "return")
        .collect(Collectors.toList());
  }
}
