package ru.hse.mit.git.components.fs;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import ru.hse.mit.git.GitException;
import ru.hse.mit.git.components.graph.TreeNode;

public class HeadFile extends AbstractEditableFile {
   private final Path branchesDir;
   private final Path commitsDir;
   private final Path treesDir;

   public HeadFile(String filename, Path fullPath, Path branchesPath, Path commitsPath, Path treesPath) {
      this.filename = filename;
      this.fullPath = fullPath;
      this.branchesDir = branchesPath;
      this.commitsDir = commitsPath;
      this.treesDir = treesPath;
   }

   public String getCurrentBranch() throws GitException {
      if (isDetached()) {
         return getCurrentCommitHash();
      }
      else {
         String line = loadFileFromDisk().get(0);
         return line.split(" ")[1];
      }
   }

   public void setCurrentBranch(String branchName) throws GitException {
      if (!branchExists(branchName)) {
         throw new GitException("Branch '" + branchName + "' does not exist");
      }

      String content = "ref " + branchName;
      setContentImmediately(content.getBytes());
   }

   public String getCurrentCommitHash() throws GitException {
      if (isDetached()) {
         try {
            return Files.readString(fullPath);
         } catch (IOException e) {
            throw new GitException(e);
         }
      }
      else {
         try {
            File branch = getBranchFile();
            return Files.readString(branch.toPath());
         } catch (IOException e) {
            throw new GitException(e.getMessage(), e.getCause());
         }
      }
   }

   public void setCurrentCommitAsDetached(String commitHash) throws GitException {
      setContentImmediately(commitHash.getBytes());
   }

   public void setCurrentCommit(String commitHash) throws GitException {
      if (!commitExists(commitHash)) {
         throw new GitException("Commit '" + commitHash + "' does not exist");
      }

      if (isDetached()) {
         setContentImmediately(commitHash.getBytes());
      }
      else {
         try {
            File branch = getBranchFile();
            Files.write(branch.toPath(), commitHash.getBytes());
         } catch (IOException e) {
            throw new GitException(e);
         }
      }
   }

   public String getShiftedCommitHash(int shift) throws GitException {
      String currentCommitHash = getCurrentCommitHash();

      int n = shift;
      while (n > 0) {
         if (currentCommitHash.isEmpty()) {
            break;
         }

         CommitFile commit = CommitFile.load(commitsDir, currentCommitHash);
         currentCommitHash = commit.getParentCommitHash();

         n--;
      }

      if (currentCommitHash.isEmpty()) {
         throw new GitException("No commit found associated with HEAD~" + shift);
      }

      return currentCommitHash;
   }

   public TreeNode loadTree() throws GitException {
      if (getCurrentCommitHash().isEmpty()) {
         return TreeNode.createRoot();
      }

      CommitFile currentCommit = CommitFile.load(commitsDir, getCurrentCommitHash());

      return TreeNode.loadTree(
          treesDir,
          currentCommit.getRootNodeHash()
      );
   }

   public boolean branchExists(String branchName) {
      return Files.exists(Path.of(branchesDir.toString(), branchName));
   }

   public boolean commitExists(String commitHash) {
      return Files.exists(Path.of(commitsDir.toString(), commitHash));
   }

   public boolean isDetached() throws GitException {
      List<String> lines = loadFileFromDisk();
      List<String> data = List.of(lines.get(0).split(" "));

      if (data.size() == 1) {
         return true;
      }

      return false;
   }

   private File getBranchFile() throws GitException {
      List<String> lines = loadFileFromDisk();
      List<String> data = List.of(lines.get(0).split(" "));

      if (data.size() != 2) {
         throw new GitException("HEAD is not on branch: " + lines.get(0));
      }

      String branchName = data.get(1);
      File branchFile = Path.of(branchesDir.toString(), branchName).toFile();
      if (!branchFile.exists()) {
         try {
            Files.createFile(branchFile.toPath());
         } catch (IOException e) {
            throw new GitException(e.getMessage(), e.getCause());
         }
      }

      return branchFile;
   }
}
