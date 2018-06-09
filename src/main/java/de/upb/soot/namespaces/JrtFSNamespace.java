package de.upb.soot.namespaces;

import de.upb.soot.Utils;
import de.upb.soot.namespaces.classprovider.ClassSource;
import de.upb.soot.namespaces.classprovider.IClassProvider;
import de.upb.soot.signatures.ClassSignature;
import de.upb.soot.signatures.ModulePackageSignature;
import de.upb.soot.signatures.ModuleSignatureFactory;
import de.upb.soot.signatures.SignatureFactory;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

/**
 * Base class for {@link INamespace}s that can be located by a {@link Path} object.
 *
 * @author Andreas Dann created on 06.06.18
 */
public class JrtFSNamespace extends AbstractNamespace {

  private FileSystem theFileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));

  protected JrtFSNamespace(IClassProvider classProvider) {
    super(classProvider);
  }

  @Override
  public Optional<ClassSource> getClassSource(ClassSignature signature) {
    if (signature.packageSignature instanceof ModulePackageSignature)
      return this.getClassSourceInternalForModule(signature);
    return this.getClassSourceInternalForClassPath(signature);
  }

  private Optional<ClassSource> getClassSourceInternalForClassPath(ClassSignature classSignature) {

    Path filepath = classSignature.toPath(classProvider.getHandledFileType(), theFileSystem);
    final Path moduleRoot = theFileSystem.getPath("modules");
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleRoot)) {
      {

        for (Path entry : stream) {
          // check each module folder for the class
          Path foundfile = entry.resolve(filepath);
          if (Files.isRegularFile(foundfile)) {
            return classProvider.getClass(this, foundfile, classSignature);

          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Optional.empty();

  }

  private Optional<ClassSource> getClassSourceInternalForModule(ClassSignature classSignature) {
    Preconditions.checkArgument(classSignature.packageSignature instanceof ModulePackageSignature);

    ModulePackageSignature modulePackageSignature = (ModulePackageSignature) classSignature.packageSignature;

    Path filepath = classSignature.toPath(classProvider.getHandledFileType(), theFileSystem);
    final Path module = theFileSystem.getPath("modules", modulePackageSignature.moduleSignature.moduleName);
    Path foundClass = module.resolve(filepath);

    // this does not improve the performance
    // String filename = classSignature.getFullyQualifiedName().replace('.', '/') + "." +
    // classProvider.getHandledFileType().getExtension();
    // Path foundClass = theFileSystem.getPath("modules",modulePackageSignature.moduleSignature.moduleName,filename);

    if (Files.isRegularFile(foundClass)) {
      return classProvider.getClass(this, foundClass, classSignature);

    } else {
      return Optional.empty();
    }

  }

  // get the factory, which I should use the create the correspond class signatures
  @Override
  protected Collection<ClassSource> getClassSources(SignatureFactory factory) {

    final Path archiveRoot = theFileSystem.getPath("modules");
    return walkDirectory(archiveRoot, factory);

  }

  protected Collection<ClassSource> walkDirectory(Path dirPath, SignatureFactory factory) {

    final FileType handledFileType = classProvider.getHandledFileType();
    try {
      return Files.walk(dirPath).filter(filePath -> PathUtils.hasExtension(filePath, handledFileType))
          .flatMap(
              p -> Utils.optionalToStream(classProvider.getClass(this, p, JrtFSNamespace.fromPath(p, dirPath, factory))))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }

  }

  // TODO: originally, I could create a ModuleSingatre in any case, however, then
  // every signature factory needs a method create from path
  // however, I cannot think of a general way for java 9 modules anyway....
  // how to create the module name if we have a jar file..., or a multi jar, or the jrt file system
  // nevertheless, one general method for all signatures seems reasonable
  public static ClassSignature fromPath(Path file, Path moduleDir, SignatureFactory fac) {

    // else use the module system and create fully class signature
    if (fac instanceof ModuleSignatureFactory) {
      String filename = FilenameUtils.removeExtension(file.toString()).replace('/', '.');
      int index = filename.lastIndexOf('.');
      // get the package
      String packagename = filename.substring(0, index);
      String classname = filename.substring(0, index);
      return ((ModuleSignatureFactory) fac).getClassSignature(classname, packagename, moduleDir.toString());
    }

    // if we are using the normal signature factory, than trim the module from the path
    if (fac instanceof SignatureFactory) {
      return fac.getClassSignature(FilenameUtils.removeExtension(file.toString()).replace('/', '.'));

    }
    return null;

  }

}
