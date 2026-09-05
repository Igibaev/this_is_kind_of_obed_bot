/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
  private ZipUtil() {}

  public static int zipDirectory(Path sourceDir, Path zipFilePath) throws IOException {
    if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
      return 0;
    }
    int count = 0;
    try (OutputStream fos = Files.newOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        Stream<Path> walk = Files.walk(sourceDir)) {
      for (Path file : walk.filter(Files::isRegularFile).toList()) {
        String entryName = sourceDir.relativize(file).toString().replace(File.separatorChar, '/');
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
        count++;
      }
    }
    return count;
  }
}
