package de.upb.sse.sootup.java.core;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2019-2020 Markus Schmidt, Linghui Luo and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import com.google.common.base.Preconditions;
import de.upb.sse.sootup.core.Project;
import de.upb.sse.sootup.core.SourceTypeSpecifier;
import de.upb.sse.sootup.core.cache.provider.CacheProvider;
import de.upb.sse.sootup.core.inputlocation.AnalysisInputLocation;
import de.upb.sse.sootup.core.inputlocation.ClassLoadingOptions;
import de.upb.sse.sootup.core.inputlocation.DefaultSourceTypeSpecifier;
import de.upb.sse.sootup.java.core.language.JavaLanguage;
import de.upb.sse.sootup.java.core.views.JavaView;
import de.upb.sse.sootup.java.core.views.MutableJavaView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Project Implementation for analyzing Java.
 *
 * @author Markus Schmidt
 * @author Linghui Luo
 */
public class JavaProject extends Project<JavaSootClass, JavaView> {

  public JavaProject(
      JavaLanguage language,
      @Nonnull List<AnalysisInputLocation<? extends JavaSootClass>> inputLocations,
      @Nonnull SourceTypeSpecifier sourceTypeSpecifier) {
    super(language, inputLocations, JavaIdentifierFactory.getInstance(), sourceTypeSpecifier);
  }

  @Nonnull
  public MutableJavaView createMutableView() {
    return new MutableJavaView(this);
  }

  @Nonnull
  public JavaView createView(
      @Nonnull
          Function<AnalysisInputLocation<? extends JavaSootClass>, ClassLoadingOptions>
              classLoadingOptionsSpecifier) {
    return new JavaView(this, classLoadingOptionsSpecifier);
  }

  @Nonnull
  @Override
  public JavaView createView(
      @Nonnull CacheProvider<JavaSootClass> cacheProvider,
      @Nonnull
          Function<AnalysisInputLocation<? extends JavaSootClass>, ClassLoadingOptions>
              classLoadingOptionsSpecifier) {
    return new JavaView(this, cacheProvider, classLoadingOptionsSpecifier);
  }

  @Nonnull
  public JavaView createView(@Nonnull CacheProvider<JavaSootClass> cacheProvider) {
    return new JavaView(this, cacheProvider);
  }

  @Nonnull
  public JavaView createView() {
    return new JavaView(this);
  }

  /**
   * Creates a {@link JavaProject} builder.
   *
   * @return A {@link JavaProjectBuilder}.
   */
  @Nonnull
  public static JavaProjectBuilder builder(JavaLanguage language) {
    return new JavaProjectBuilder(language);
  }

  public static class JavaProjectBuilder {
    private final List<AnalysisInputLocation<? extends JavaSootClass>> analysisInputLocations =
        new ArrayList<>();
    private final List<ModuleInfoAnalysisInputLocation> moduleAnalysisInputLocations =
        new ArrayList<>();

    private SourceTypeSpecifier sourceTypeSpecifier = DefaultSourceTypeSpecifier.getInstance();
    private final JavaLanguage language;
    private boolean useModules = false;

    public JavaProjectBuilder(JavaLanguage language) {
      this.language = language;
    }

    @Nonnull
    public JavaProjectBuilder setSourceTypeSpecifier(SourceTypeSpecifier sourceTypeSpecifier) {
      this.sourceTypeSpecifier = sourceTypeSpecifier;
      return this;
    }

    @Nonnull
    public JavaProjectBuilder addInputLocation(
        AnalysisInputLocation<JavaSootClass> analysisInputLocation) {
      this.analysisInputLocations.add(analysisInputLocation);
      return this;
    }

    @Nonnull
    public JavaProjectBuilder addInputLocation(
        ModuleInfoAnalysisInputLocation analysisInputLocation) {
      Preconditions.checkArgument(language.getVersion() > 8);
      useModules = true;
      this.moduleAnalysisInputLocations.add(analysisInputLocation);
      return this;
    }

    @Nonnull
    /**
     * if no ModuleAnalysisInputLocation is given but the analysis should use JavaModules for
     * resolving anyway
     */
    public JavaProjectBuilder enableModules() {
      useModules = true;
      return this;
    }

    @Nonnull
    public JavaProject build() {
      if (useModules) {
        return new JavaModuleProject(
            language, analysisInputLocations, moduleAnalysisInputLocations, sourceTypeSpecifier);
      } else {
        return new JavaProject(language, analysisInputLocations, sourceTypeSpecifier);
      }
    }
  }
}
