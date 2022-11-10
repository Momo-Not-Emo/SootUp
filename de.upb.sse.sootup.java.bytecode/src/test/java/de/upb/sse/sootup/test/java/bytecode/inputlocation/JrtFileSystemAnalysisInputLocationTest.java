package de.upb.sse.sootup.test.java.bytecode.inputlocation;

import static org.junit.Assert.*;

import categories.Java9Test;
import de.upb.sse.sootup.core.Project;
import de.upb.sse.sootup.core.frontend.AbstractClassSource;
import de.upb.sse.sootup.core.inputlocation.DefaultSourceTypeSpecifier;
import de.upb.sse.sootup.core.types.ClassType;
import de.upb.sse.sootup.java.bytecode.inputlocation.JrtFileSystemAnalysisInputLocation;
import de.upb.sse.sootup.java.core.JavaModuleIdentifierFactory;
import de.upb.sse.sootup.java.core.JavaModuleProject;
import de.upb.sse.sootup.java.core.JavaSootClass;
import de.upb.sse.sootup.java.core.language.JavaLanguage;
import de.upb.sse.sootup.java.core.signatures.ModuleSignature;
import de.upb.sse.sootup.java.core.views.JavaView;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** @author Andreas Dann, Markus Schmidt */
@Category(Java9Test.class)
public class JrtFileSystemAnalysisInputLocationTest {

  @Test
  public void getClassSource() {
    JrtFileSystemAnalysisInputLocation inputLocation = new JrtFileSystemAnalysisInputLocation();
    Project<JavaSootClass, JavaView> project =
        new JavaModuleProject(
            new JavaLanguage(9),
            Collections.emptyList(),
            Collections.singletonList(inputLocation),
            DefaultSourceTypeSpecifier.getInstance());
    final ClassType sig =
        JavaModuleIdentifierFactory.getInstance().getClassType("String", "java.lang", "java.base");

    final Optional<? extends AbstractClassSource<JavaSootClass>> clazz =
        inputLocation.getClassSource(sig, project.createView());
    assertTrue(clazz.isPresent());
    assertEquals(sig, clazz.get().getClassType());
  }

  @Test
  public void getClassSources() {
    // hint: quite expensive as it loads **all** Runtime modules!
    JrtFileSystemAnalysisInputLocation inputLocation = new JrtFileSystemAnalysisInputLocation();
    Project<JavaSootClass, JavaView> project =
        new JavaModuleProject(
            new JavaLanguage(9),
            Collections.emptyList(),
            Collections.singletonList(inputLocation),
            DefaultSourceTypeSpecifier.getInstance());
    final ClassType sig1 =
        JavaModuleIdentifierFactory.getInstance().getClassType("String", "java.lang", "java.base");
    final ClassType sig2 =
        JavaModuleIdentifierFactory.getInstance().getClassType("System", "java.lang", "java.base");

    final Collection<? extends AbstractClassSource<?>> classSources =
        inputLocation.getClassSources(project.createView());
    assertTrue(classSources.size() > 26000);
    assertTrue(classSources.stream().anyMatch(cs -> cs.getClassType().equals(sig1)));
    assertTrue(classSources.stream().anyMatch(cs -> cs.getClassType().equals(sig2)));
  }

  @Test
  public void discoverModules() {
    JrtFileSystemAnalysisInputLocation inputLocation = new JrtFileSystemAnalysisInputLocation();
    Collection<ModuleSignature> modules = inputLocation.discoverModules();
    assertTrue(modules.size() > 65);
    System.out.println(modules);
    assertTrue(modules.contains(JavaModuleIdentifierFactory.getModuleSignature("java.base")));
    assertTrue(modules.contains(JavaModuleIdentifierFactory.getModuleSignature("java.se")));
    assertTrue(modules.contains(JavaModuleIdentifierFactory.getModuleSignature("jdk.javadoc")));
    assertTrue(modules.contains(JavaModuleIdentifierFactory.getModuleSignature("jdk.charsets")));
  }
}
