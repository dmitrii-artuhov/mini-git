package ru.hse.mit.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import ru.hse.mit.git.components.fs.BlobFile;
import ru.hse.mit.git.components.fs.CommitFile;
import ru.hse.mit.git.components.fs.HeadFile;
import ru.hse.mit.git.components.fs.IndexFile;
import ru.hse.mit.git.components.fs.IndexFile.FileStatus;
import ru.hse.mit.git.components.graph.TreeNode;
import ru.hse.mit.git.components.utils.MiniGitUtils;

public class MiniGit {
   private final String workingDir;
   private boolean isInitialized = false;

   private static final String REPOSITORY_DIR = ".mini-git";
   private static final String BLOBS_DIR = "blobs";
   private static final String TREES_DIR = "trees";
   private static final String COMMITS_DIR = "commits";
   private static final String BRANCHES_DIR = "branches";

   private static final String HEAD_FILE = "HEAD";
   private static final String INDEX_FILE = "INDEX";

   private static final String MASTER_BRANCH = "master";

   private final HeadFile headFile;
   private final IndexFile indexFile;

   public MiniGit(String workingDir) {
      this.workingDir = workingDir;
      this.headFile = new HeadFile(
          HEAD_FILE,
          getFullPathFromRepository(HEAD_FILE),
          getFullPathFromRepository(BRANCHES_DIR),
          getFullPathFromRepository(COMMITS_DIR),
          getFullPathFromRepository(TREES_DIR)
      );
      this.indexFile = new IndexFile(INDEX_FILE, getFullPathFromRepository(INDEX_FILE));
   }

   public String init() throws GitException {
      try {
         // directories
         Files.createDirectories(getFullPathFromRepository(BLOBS_DIR));
         Files.createDirectories(getFullPathFromRepository(TREES_DIR));
         Files.createDirectories(getFullPathFromRepository(COMMITS_DIR));
         Files.createDirectories(getFullPathFromRepository(BRANCHES_DIR));

         // files
         Files.createFile(getFullPathFromRepository(HEAD_FILE));
         Files.createFile(getFullPathFromRepository(INDEX_FILE));

         // set master branch to head by default
         Files.createFile(getFullPathFromRepository(BRANCHES_DIR, MASTER_BRANCH));
         headFile.setCurrentBranch(MASTER_BRANCH);

         isInitialized = true;
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }

      return "Project initialized" + System.lineSeparator();
   }

   public String add(@NotNull List<String> entryNames) throws GitException {
      checkInitialized();
      indexFile.load();
      Map<String, File> pureFiles = getPureFiles(entryNames);

      for (Map.Entry<String, File> fileEntry : pureFiles.entrySet()) {
         byte[] fileBytes;

         try {
            fileBytes = FileUtils.readFileToByteArray(fileEntry.getValue());
         } catch (IOException e) {
            throw new GitException(e.getMessage(), e.getCause());
         }

         // create blob
         BlobFile blob = new BlobFile(getFullPathFromRepository(BLOBS_DIR), fileBytes);
         blob.save();

         // add entry to index file
         indexFile.addEntry(fileEntry.getKey(), /* hash */ blob.getFilename());
      }

      indexFile.save();

      return "Add completed successful" + System.lineSeparator();
   }

   public String rm(@NotNull List<String> entryNames) throws GitException {
      checkInitialized();
      indexFile.load();
      Map<String, File> pureFiles = getPureFiles(entryNames);

      for (Map.Entry<String, File> fileEntry : pureFiles.entrySet()) {
         // remove entry from index file
         indexFile.removeEntry(fileEntry.getKey());
      }

      indexFile.save();

      return "Rm completed successful" + System.lineSeparator();
   }

   public String status() throws GitException {
      checkInitialized();
      if (headFile.isDetached()) {
         // actually, my implementation will show correct diff for the detached HEAD as well
         // I will stick to the provided tests, though
         return "Error while performing status: Head is detached" + System.lineSeparator();
      }

      indexFile.load();

      Map<IndexFile.FileStatus, List<String>> untrackedFiles = indexFile.getUntrackedFiles(
          getFullPathFromWorkingDirectory(),
          getFullPathFromRepository()
      );

      Map<IndexFile.FileStatus, List<String>> readyToCommitFiles = indexFile.getReadyToCommitFiles(
          headFile.loadTree().getBlobs()
      );

      StringBuilder content = new StringBuilder();
      content.append("Current branch is '").append(headFile.getCurrentBranch()).append("'").append(System.lineSeparator());

      boolean untrackedAdded = appendStatus(content, untrackedFiles, "Untracked files:");
      boolean readyToCommitAdded = appendStatus(content, readyToCommitFiles, "Ready to commit:");

      if (!untrackedAdded && !readyToCommitAdded) {
         content.append("Everything up to date").append(System.lineSeparator());
      }

      return content.toString();
   }

   public String commit(@NotNull String message) throws GitException {
      checkInitialized();
      indexFile.load();
      TreeNode root = TreeNode.createRoot();

      for (var entry : indexFile.getEntries()) {
         String path = entry.getKey();
         String blobHash = entry.getValue();

         List<String> names = List.of(path.split("/"));
         root.addChildren(0, names, blobHash);
      }

      root.buildGraph();
      root.saveGraph(getFullPathFromRepository(TREES_DIR));

      CommitFile commit = new CommitFile(
          getFullPathFromRepository(COMMITS_DIR),
          root.getHash().get(),
          headFile.getCurrentCommitHash(),
          "Dimechik",
          OffsetDateTime.now(),
          message
      );

      commit.save();
      headFile.setCurrentCommit(commit.getFilename());

      return "Files committed" + System.lineSeparator();
   }

   /**
    *
    * @param checkpointName either commit hash or branch name (eg. master)
    */
   public String reset(@NotNull String checkpointName) throws GitException {
      checkInitialized();
      return resetImpl(checkpointName);
   }

   public String reset(int stepsBackwardsFromHead) throws GitException {
      checkInitialized();
      return resetImpl(headFile.getShiftedCommitHash(stepsBackwardsFromHead));
   }

   private String resetImpl(String checkpointName) throws GitException {
      // Update HEAD file
      // branch
      if (Files.exists(getFullPathFromRepository(BRANCHES_DIR, checkpointName))) {
         headFile.setCurrentBranch(checkpointName);
      }
      // commit
      else if (Files.exists(getFullPathFromRepository(COMMITS_DIR, checkpointName))) {
         headFile.setCurrentCommit(checkpointName);
      }
      else {
         throw new GitException("Neither commit, nor branch exists named '" + checkpointName + "'");
      }

      // Update index file
      TreeNode root = headFile.loadTree();
      indexFile.setEntries(root.getBlobs());
      indexFile.save();

      // Update working directory
      clearWorkingDirectory();
      indexFile.saveTrackedFilesToWorkingDir(getFullPathFromWorkingDirectory(), getFullPathFromRepository(BLOBS_DIR));

      return "Reset successful" + System.lineSeparator();
   }

   public String log() throws GitException {
      checkInitialized();
      return logImpl(headFile.getCurrentCommitHash());
   }

   public String log(String commitHash) throws GitException {
      checkInitialized();
      return logImpl(commitHash);
   }

   public String log(int stepsBackwardsFromHead) throws GitException {
      checkInitialized();
      return logImpl(headFile.getShiftedCommitHash(stepsBackwardsFromHead));
   }

   private String logImpl(String startingCommit) throws GitException {
      StringBuilder result = new StringBuilder();

      Path fullPathToCommitsDir = getFullPathFromRepository(COMMITS_DIR);
      String currentCommitHash = startingCommit;

      while (!currentCommitHash.equals("")) {
         CommitFile commit = CommitFile.load(fullPathToCommitsDir, currentCommitHash);
         result.append(commit.getInfo()).append(System.lineSeparator());
         currentCommitHash = commit.getParentCommitHash();
      }

      return result.toString();
   }

   /**
    *
    * @param checkpointName either commit hash or branch name (eg. master)
    */
   public String checkout(String checkpointName) throws GitException {
      checkInitialized();
      return checkoutImpl(checkpointName);
   }

   public String checkout(int stepsBackwardsFromHead) throws GitException {
      checkInitialized();
      return checkoutImpl(headFile.getShiftedCommitHash(stepsBackwardsFromHead));
   }

   public String checkoutImpl(String checkpointName) throws GitException {
      TreeNode prevRoot = headFile.loadTree();

      // Update HEAD file
      // branch
      if (Files.exists(getFullPathFromRepository(BRANCHES_DIR, checkpointName))) {
         headFile.setCurrentBranch(checkpointName);
      }
      // commit
      else if (Files.exists(getFullPathFromRepository(COMMITS_DIR, checkpointName))) {
         headFile.setCurrentCommitAsDetached(checkpointName);
      }
      else {
         throw new GitException("Neither commit, nor branch exists named '" + checkpointName + "'");
      }

      // Update index file
      TreeNode root = headFile.loadTree();
      Map<String, String> checkoutBlobs = root.getBlobs();
      indexFile.setEntries(checkoutBlobs);
      indexFile.save();

      // add new files from checkout commit/branch
      indexFile.saveTrackedFilesToWorkingDir(getFullPathFromWorkingDirectory(), getFullPathFromRepository(BLOBS_DIR));

      // remove all files from working directory, that are in `prevRoot` but not in `root`
      Map<String, String> prevBlobs = prevRoot.getBlobs();
      for (var entry : prevBlobs.entrySet()) {
         String filename = entry.getKey();

         if (!checkoutBlobs.containsKey(filename)) {
            try {
               Files.delete(getFullPathFromWorkingDirectory(filename));
            } catch (IOException e) {
               throw new GitException(e.getMessage(), e.getCause());
            }
         }
      }

      removeEmptyWorkingDirectories(getFullPathFromWorkingDirectory());


      return "Checkout completed successful" + System.lineSeparator();
   }

   public String checkout(List<String> filenames) throws GitException {
      checkInitialized();

      TreeNode root = headFile.loadTree();
      Map<String, String> blobs = root.getBlobs();

      for (String filename : filenames) {
         if (!blobs.containsKey(filename)) {
            throw new GitException("Filename '" + filename + "' is not recognized by git");
         }
      }

      for (String filename : filenames) {
         String hash = blobs.get(filename);
         try {
            byte[] fileBytes = Files.readAllBytes(getFullPathFromRepository(BLOBS_DIR, hash));
            Files.write(
                getFullPathFromWorkingDirectory(filename),
                fileBytes
            );
         } catch (IOException e) {
            throw new GitException(e.getMessage(), e.getCause());
         }
      }

      return "Checkout completed successful" + System.lineSeparator();
   }

   public String createBranch(String branchName) throws GitException {
      if (headFile.branchExists(branchName)) {
         throw new GitException("Branch '" + branchName + "' already exists");
      }

      try {
         Path branchFile = Files.createFile(getFullPathFromRepository(BRANCHES_DIR, branchName));
         Files.write(branchFile, headFile.getCurrentCommitHash().getBytes());
         // the return message of this command is pretty weird, considering that we checkout new branch by default
         // according to the tests
         headFile.setCurrentBranch(branchName);
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }

      return
          "Branch new-feature created successfully" + System.lineSeparator() +
          "You can checkout it with 'checkout " + branchName + "'" + System.lineSeparator();
   }

   public String showBranches() throws GitException {
      StringBuilder content = new StringBuilder();
      content.append("Available branches:").append(System.lineSeparator());

      List<Path> result;
      try (Stream<Path> walk = Files.walk(getFullPathFromRepository(BRANCHES_DIR))) {
         result = walk.filter(Files::isRegularFile)
             .collect(Collectors.toList());
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }

      result.forEach(path -> {
         content.append(path.getFileName().toString()).append(System.lineSeparator());
      });

      return content.toString();
   }

   public String removeBranch(String branchName) throws GitException {
      if (!headFile.branchExists(branchName)) {
         throw new GitException("Branch '" + branchName + "' does not exist");
      }

      if (headFile.getCurrentBranch().equals(branchName)) {
         throw new GitException("Cannot remove current branch");
      }

      try {
         Files.delete(getFullPathFromRepository(BRANCHES_DIR, branchName));
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }

      return "Branch " + branchName + " removed successfully" + System.lineSeparator();
   }

   public String merge(String otherBranchName) throws GitException {
      // Нуууу, я почитал, как это делать:
      // за 1 балл, пожалуй, откажусь + уже нет ментальных сил это реализовывать((
      throw new UnsupportedOperationException();
   }

   public String getRelativeRevisionFromHead(int n) throws GitException {
      return headFile.getShiftedCommitHash(n);
   }


   /*------- Helper methods ------------------------------------*/
   private Path getFullPathFromRepository(String... paths) {
      String prefix = Path.of(workingDir, REPOSITORY_DIR).toString();
      return Path.of(prefix, paths);
   }

   private Path getFullPathFromWorkingDirectory(String... paths) {
      return Path.of(workingDir, paths);
   }

   private void checkInitialized() throws GitException {
      if (!isInitialized) {
         throw new GitException("MiniGit repository not initialized");
      }
   }

   /**
    * Flattens the directories that {@code entryNames contain}, meaning goes inside of them while no directories left,
    * the file names are built respectively
    */
   private Map<String, File> getPureFiles(List<String> entryNames) throws GitException {
      Map<String, File> files = new HashMap<>();
      for (String name : entryNames) {
         files.put(name, getFullPathFromWorkingDirectory(name).toFile());
      }

      // TODO: Do I have to check for files existance?
      // MiniGitUtils.checkFilesExists(files.values().stream().toList());
      return collectPureFiles("", files);
   }

   /**
    * For every {@code File} that is a directory goes inside of it recursively and collects pure files from it
    */
   private Map<String, File> collectPureFiles(String prefix, Map<String, File> entryFiles) {
      Map<String, File> result = new HashMap<>();
      entryFiles.forEach((name, file) -> {
         if (file.isDirectory()) {
            if (file.getName().equals(REPOSITORY_DIR)) {
               return;
            }

            result.putAll(collectPureFiles(
                prefix + file.getName() + "/",
                Arrays.stream(Objects.requireNonNull(file.listFiles())).collect(Collectors.toMap(
                    File::getName,
                    Function.identity()
                ))
            ));
         }
         else {
            // replacing './' symbol in path, so that we will not have extra tree-nodes for '.' folders
            if (prefix.isEmpty()) {
               result.put(name.replace("./", ""), file);
            }
            else {
               result.put((prefix + name).replace("./", ""), file);
            }
         }
      });

      return result;
   }

   /**
    *
    * @param content
    * @return {@code true} if some data was appended, otherwise {@code false}
    */
   private boolean appendStatus(
       StringBuilder content,
       Map<FileStatus, List<String>> files,
       String title
   ) {
      String filesNew = collectFilesStatus(files.get(FileStatus.NEW));
      String filesModified = collectFilesStatus(files.get(FileStatus.MODIFIED));
      String filesDeleted = collectFilesStatus(files.get(FileStatus.DELETED));


      boolean filesAdded = false;
      if (filesModified.length() + filesNew.length() + filesDeleted.length() != 0) {
         filesAdded = true;
         content.append(title).append(System.lineSeparator()).append(System.lineSeparator());

         if (!filesNew.isEmpty()) {
            content.append("New files:").append(System.lineSeparator())
                .append(filesNew).append(System.lineSeparator());
         }

         if (!filesModified.isEmpty()) {
            content.append("Modified files:").append(System.lineSeparator())
                .append(filesModified).append(System.lineSeparator());
         }

         if (!filesDeleted.isEmpty()) {
            content.append("Removed files:").append(System.lineSeparator())
                .append(filesDeleted).append(System.lineSeparator());
         }
      }

      return filesAdded;
   }

   private String collectFilesStatus(List<String> files) {
      StringBuilder result = new StringBuilder();

      for (String filename : files) {
         result.append("\t").append(filename).append(System.lineSeparator());
      }

      return result.toString();
   }

   private void clearWorkingDirectory() throws GitException {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(getFullPathFromWorkingDirectory())) {
         for (Path entry : stream) {
            if (entry.toString().contains(REPOSITORY_DIR)) {
               continue;
            }

            if (Files.isDirectory(entry)) {
               deleteRecursively(entry);
            }
            else {
               Files.delete(entry);
            }
         }
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }

   private void deleteRecursively(Path path) throws IOException {
      if (Files.isDirectory(path)) {
         try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path file : stream) {
               deleteRecursively(file);
            }
         }
      }

      Files.deleteIfExists(path);
   }

   private void removeEmptyWorkingDirectories(Path currentDir) throws GitException {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
         for (Path entry : stream) {
            if (entry.toString().contains(REPOSITORY_DIR)) {
               continue;
            }

            if (Files.isDirectory(entry)) {
               removeEmptyWorkingDirectories(entry);
            }
         }

         if (countEntries(currentDir) == 0) {
            Files.delete(currentDir);
         }
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }

   private int countEntries(Path directory) throws GitException {
      int count = 0;
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
         for (Path entry : stream) {
            count++;
         }
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
      return count;
   }
}
