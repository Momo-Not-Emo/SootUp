package de.upb.sse.sootup.java.core.views;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2018-2020 Linghui Luo, Jan Martin Persch, Christian Brüggemann and others
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

import de.upb.sse.sootup.core.Project;
import de.upb.sse.sootup.core.cache.Cache;
import de.upb.sse.sootup.core.cache.provider.CacheProvider;
import de.upb.sse.sootup.core.cache.provider.FullCacheProvider;
import de.upb.sse.sootup.core.frontend.AbstractClassSource;
import de.upb.sse.sootup.core.inputlocation.AnalysisInputLocation;
import de.upb.sse.sootup.core.inputlocation.ClassLoadingOptions;
import de.upb.sse.sootup.core.inputlocation.EmptyClassLoadingOptions;
import de.upb.sse.sootup.core.transform.BodyInterceptor;
import de.upb.sse.sootup.core.types.ClassType;
import de.upb.sse.sootup.core.views.AbstractView;
import de.upb.sse.sootup.java.core.AnnotationUsage;
import de.upb.sse.sootup.java.core.JavaAnnotationSootClass;
import de.upb.sse.sootup.java.core.JavaSootClass;
import de.upb.sse.sootup.java.core.types.AnnotationType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * The Class JavaView manages the Java classes of the application being analyzed.
 *
 * @author Linghui Luo created on 31.07.2018
 * @author Jan Martin Persch
 */
public class JavaView extends AbstractView<JavaSootClass> {

  @Nonnull protected final Cache<JavaSootClass> cache;

  protected volatile boolean isFullyResolved = false;

  @Nonnull
  protected Function<AnalysisInputLocation<? extends JavaSootClass>, ClassLoadingOptions>
      classLoadingOptionsSpecifier;

  @Nonnull
  public JavaView(@Nonnull Project<JavaSootClass, ? extends JavaView> project) {
    this(project, new FullCacheProvider<>());
  }

  /** Creates a new instance of the {@link JavaView} class. */
  @Nonnull
  public JavaView(
      @Nonnull Project<JavaSootClass, ? extends JavaView> project,
      @Nonnull CacheProvider<JavaSootClass> cacheProvider) {
    this(project, cacheProvider, analysisInputLocation -> EmptyClassLoadingOptions.Default);
  }

  /**
   * Creates a new instance of the {@link JavaView} class.
   *
   * @param classLoadingOptionsSpecifier To use the default {@link ClassLoadingOptions} for an
   *     {@link AnalysisInputLocation}, simply return <code>null</code>, otherwise the desired
   *     options.
   */
  public JavaView(
      @Nonnull Project<JavaSootClass, ? extends JavaView> project,
      @Nonnull
          Function<AnalysisInputLocation<? extends JavaSootClass>, ClassLoadingOptions>
              classLoadingOptionsSpecifier) {
    this(project, new FullCacheProvider<>(), classLoadingOptionsSpecifier);
  }

  public JavaView(
      @Nonnull Project<JavaSootClass, ? extends JavaView> project,
      @Nonnull CacheProvider<JavaSootClass> cacheProvider,
      @Nonnull
          Function<AnalysisInputLocation<? extends JavaSootClass>, ClassLoadingOptions>
              classLoadingOptionsSpecifier) {
    super(project);
    this.classLoadingOptionsSpecifier = classLoadingOptionsSpecifier;
    this.cache = cacheProvider.createCache();
  }

  @Nonnull
  @Override
  public List<BodyInterceptor> getBodyInterceptors(AnalysisInputLocation<JavaSootClass> clazz) {
    return this.classLoadingOptionsSpecifier.apply(clazz) != null
        ? this.classLoadingOptionsSpecifier.apply(clazz).getBodyInterceptors()
        : getBodyInterceptors();
  }

  @Nonnull
  @Override
  public List<BodyInterceptor> getBodyInterceptors() {
    // TODO add default interceptors from
    // de.upb.sse.sootup.java.bytecode.interceptors.BytecodeBodyInterceptors;
    return Collections.emptyList();
  }

  @Override
  @Nonnull
  public synchronized Collection<JavaSootClass> getClasses() {
    resolveAll();
    return cache.getClasses();
  }

  @Override
  @Nonnull
  public synchronized Optional<JavaSootClass> getClass(@Nonnull ClassType type) {
    JavaSootClass cachedClass = cache.getClass(type);
    if (cachedClass != null) {
      return Optional.of(cachedClass);
    }

    Optional<? extends AbstractClassSource<? extends JavaSootClass>> abstractClass =
        getAbstractClass(type);
    if (!abstractClass.isPresent()) {
      return Optional.empty();
    }

    return buildClassFrom(abstractClass.get());
  }

  @Nonnull
  protected Optional<? extends AbstractClassSource<? extends JavaSootClass>> getAbstractClass(
      @Nonnull ClassType type) {
    return getProject().getInputLocations().stream()
        .map(location -> location.getClassSource(type, this))
        .filter(Optional::isPresent)
        // like javas behaviour: if multiple matching Classes(ClassTypes) are found on the
        // classpath the first is returned (see splitpackage)
        .limit(1)
        .map(Optional::get)
        .findAny();
  }

  @Nonnull
  protected synchronized Optional<JavaSootClass> buildClassFrom(
      AbstractClassSource<? extends JavaSootClass> classSource) {

    ClassType classType = classSource.getClassType();
    JavaSootClass theClass;
    if (!cache.hasClass(classType)) {
      theClass =
          classSource.buildClass(getProject().getSourceTypeSpecifier().sourceTypeFor(classSource));
      cache.putClass(classType, theClass);
    } else {
      theClass = cache.getClass(classType);
    }

    if (theClass.getType() instanceof AnnotationType) {
      JavaAnnotationSootClass jasc = (JavaAnnotationSootClass) theClass;
      jasc.getAnnotations(Optional.of(this)).forEach(AnnotationUsage::getValuesWithDefaults);
    }

    return Optional.of(theClass);
  }

  protected synchronized void resolveAll() {
    if (isFullyResolved) {
      return;
    }

    getProject().getInputLocations().stream()
        .flatMap(location -> location.getClassSources(this).stream())
        .forEach(this::buildClassFrom);
    isFullyResolved = true;
  }
}
