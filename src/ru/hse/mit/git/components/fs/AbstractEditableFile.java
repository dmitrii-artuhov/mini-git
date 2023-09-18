package ru.hse.mit.git.components.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import ru.hse.mit.git.GitException;

public class AbstractEditableFile {
   protected String filename;
   protected Path fullPath;

   public String getFilename() {
      return filename;
   }

   protected List<String> loadFileFromDisk() throws GitException {
      try (Stream<String> stream = Files.lines(fullPath)) {
         return stream.toList();
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }

   protected void saveFileOnDisk(List<String> lines) throws GitException {
      try {
         Files.write(fullPath, (Iterable<String>) lines.stream()::iterator);
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }

   protected void setContentImmediately(byte[] content) throws GitException {
      try {
         Files.write(fullPath, content);
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }

   /**
    * If file did not exist, then it stores new file in the filesystem
    * @param fileBytes
    * @throws GitException
    */
   protected void save(byte[] fileBytes) throws GitException {
      if (!Files.exists(fullPath)) {
         try {
            Files.createFile(fullPath);
            setContentImmediately(fileBytes);
         } catch (IOException e) {
            throw new GitException(e.getMessage(), e.getCause());
         }
      }
   }
}


