package ru.hse.mit.git.components.fs;


import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import ru.hse.mit.git.GitException;
import ru.hse.mit.git.components.utils.MiniGitUtils;

public class TreeFile extends AbstractEditableFile {
   private final byte[] fileBytes;

   public TreeFile(Path fullPathToDir, byte @NotNull [] fileBytes) {
      this.filename = MiniGitUtils.getHashFromBytes(fileBytes);
      this.fullPath = Path.of(fullPathToDir.toString(), filename);
      this.fileBytes = fileBytes;
   }

   public void save() throws GitException {
      save(fileBytes);
   }
}
