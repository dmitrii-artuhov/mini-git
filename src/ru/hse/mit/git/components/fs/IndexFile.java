package ru.hse.mit.git.components.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import ru.hse.mit.git.GitException;
import ru.hse.mit.git.components.utils.MiniGitUtils;

public class IndexFile extends AbstractEditableFile {
   public enum FileStatus {
      MODIFIED,
      NEW,
      DELETED
   }
   private final Map<String, String> entries = new HashMap<>();

   public IndexFile(String filename, Path fullPath) {
      this.filename = filename;
      this.fullPath = fullPath;
   }

   public Set<Entry<String, String>> getEntries() {
      return entries.entrySet();
   }

   public void load() throws GitException {
      entries.clear();
      List<String> lines = loadFileFromDisk();

      for (String line : lines) {
         String[] keyVal = line.split(" ");
         entries.put(keyVal[0], keyVal[1]);
      }
   }

   public void save() throws GitException {
      List<String> lines = entries.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).toList();
      saveFileOnDisk(lines);
   }

   public void addEntry(String entryName, String entryHash) {
      entries.put(entryName, entryHash);
   }

   public void removeEntry(String entryName) {
      entries.remove(entryName);
   }

   public void setEntries(Map<String, String> newEtries) {
      entries.clear();
      entries.putAll(newEtries);
   }

   public void saveTrackedFilesToWorkingDir(Path workingDir, Path blobsDir) throws GitException {
      for (var entry : entries.entrySet()) {
         String filename = entry.getKey();
         String hash = entry.getValue();

         try {
            Path path = Path.of(workingDir.toString(), filename);
            if (!Files.exists(path)) {
               Files.createDirectories(path.getParent());
               Files.createFile(path);
            }
            Files.write(path, Files.readAllBytes(Path.of(blobsDir.toString(), hash)));
         } catch (IOException e) {
            throw new GitException(e.getMessage(), e.getCause());
         }
      }
   }

   public Map<FileStatus, List<String>> getUntrackedFiles(Path workingDir, Path exclude) throws GitException {
      Collection<String> indexFiles = entries.keySet();
      Collection<String> workingDirFiles = getFilesFromWorkingDirectory(workingDir, exclude);
      Map<FileStatus, List<String>> result = Map.of(
          FileStatus.MODIFIED, new ArrayList<>(),
          FileStatus.NEW, new ArrayList<>(),
          FileStatus.DELETED, new ArrayList<>()
      );

      Set<String> allFiles = new HashSet<>();
      allFiles.addAll(indexFiles);
      allFiles.addAll(workingDirFiles);

      for (String filename : allFiles) {
         boolean indexFileContains = indexFiles.contains(filename);
         boolean workingDirContains = workingDirFiles.contains(filename);

         if (indexFileContains && workingDirContains) {
            byte[] workingDirFileBytes = MiniGitUtils.getFileBytes(Path.of(workingDir.toString(), filename));
            String workingDirFileHash = MiniGitUtils.getHashFromBytes(workingDirFileBytes);

            if (!entries.get(filename).equals(workingDirFileHash)) {
               result.get(FileStatus.MODIFIED).add(filename);
            }
         }
         else if (!indexFileContains && workingDirContains) {
            result.get(FileStatus.NEW).add(filename);
         }
         else if (indexFileContains && !workingDirContains) {
            result.get(FileStatus.DELETED).add(filename);
         }
      }

      return result;
   }

   public Map<FileStatus, List<String>> getReadyToCommitFiles(Map<String, String> repoEntries) {
      Collection<String> indexFiles = entries.keySet();
      Collection<String> repoFiles = repoEntries.keySet();

      Map<FileStatus, List<String>> result = Map.of(
          FileStatus.MODIFIED, new ArrayList<>(),
          FileStatus.NEW, new ArrayList<>(),
          FileStatus.DELETED, new ArrayList<>()
      );

      Collection<String> allFiles = new HashSet<>();
      allFiles.addAll(indexFiles);
      allFiles.addAll(repoFiles);

      for (String filename : allFiles) {
         boolean indexContainsFile = indexFiles.contains(filename);
         boolean repoContainsFile = repoFiles.contains(filename);

         if (indexContainsFile && repoContainsFile) {
            String indexHash = entries.get(filename);
            String repoHash = repoEntries.get(filename);

            if (!indexHash.equals(repoHash)) {
               result.get(FileStatus.MODIFIED).add(filename);
            }
         }
         else if (indexContainsFile && !repoContainsFile) {
            result.get(FileStatus.NEW).add(filename);
         }
         else if (!indexContainsFile && repoContainsFile) {
            result.get(FileStatus.DELETED).add(filename);
         }
      }

      return result;
   }

   public Set<String> getFilesFromWorkingDirectory(Path workingDir, Path exclude) throws GitException {
      Set<String> result = new HashSet<>();

      try (Stream<Path> files = Files.walk(workingDir)) {
         files.forEach(path -> {
            if (path.toFile().isDirectory() || path.toString().startsWith(exclude.toString())) {
               return;
            }

            String trimmedPath = path.toString().replace(workingDir.toString(), "");
            if (trimmedPath.isEmpty()) {
               return;
            }

            trimmedPath = trimmedPath.substring(1).replace("\\", "/");
            result.add(trimmedPath);
         });

         return result;
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }
}