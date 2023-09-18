package ru.hse.mit.git.components.fs;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import ru.hse.mit.git.GitException;
import ru.hse.mit.git.components.utils.MiniGitUtils;

public class CommitFile extends AbstractEditableFile {
   private final String author;
   private final OffsetDateTime date;
   private final String message;
   private final String rootNodeHash;
   private final String parentCommitHash;

   public CommitFile(Path fullPath, @NotNull String rootNodeHash, @NotNull String parentCommitHash, @NotNull String author, @NotNull OffsetDateTime date, @NotNull String message) {
      String content = getCommitFileContent(rootNodeHash, parentCommitHash, author, date, message);

      this.filename = MiniGitUtils.getHashFromBytes(content.getBytes());
      this.fullPath = Path.of(fullPath.toString(), filename);
      this.rootNodeHash = rootNodeHash;
      this.parentCommitHash = parentCommitHash;
      this.date = date;
      this.message = message;
      this.author = author;
   }

   public CommitFile(String hash, Path fullPath, @NotNull String rootNodeHash, @NotNull String parentCommitHash, @NotNull String author, @NotNull OffsetDateTime date, @NotNull String message) {
      this.filename = hash;
      this.fullPath = Path.of(fullPath.toString(), filename);
      this.rootNodeHash = rootNodeHash;
      this.parentCommitHash = parentCommitHash;
      this.date = date;
      this.message = message;
      this.author = author;
   }

   public String getParentCommitHash() {
      return parentCommitHash;
   }

   public String getRootNodeHash() {
      return rootNodeHash;
   }

   public static CommitFile load(Path fullPath, String hash) throws GitException {
      try {
         List<String> lines = Files.readAllLines(Path.of(fullPath.toString(), hash));

         // root tree hash
         String rootNodeHash = lines.get(0).split(" ")[1];

         // parent commit hash
         String parentCommitHash = "";
         String[] parentCommitHashLine = lines.get(1).split(" ");
         if (parentCommitHashLine.length > 1) {
            parentCommitHash = parentCommitHashLine[1];
         }

         // author
         String author = lines.get(2).split(" ")[1];

         // date
         DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
         OffsetDateTime date = OffsetDateTime.parse(lines.get(3).split(" ")[1], formatter);

         // message
         String message = lines.get(4).replaceFirst("message ", "");

         return new CommitFile(hash, fullPath, rootNodeHash, parentCommitHash, author, date, message);
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }

   public void save() throws GitException {
      save(getCommitFileContent(rootNodeHash, parentCommitHash, author, date, message).getBytes());
   }

   public String getInfo() {
      return "Commit " + filename + System.lineSeparator()
          + "Author: " + author + System.lineSeparator()
          + "Date: " + date.toString() + System.lineSeparator()
          + System.lineSeparator() + message + System.lineSeparator();
   }

   private String getCommitFileContent(String rootNodeHash, String parentCommitHash, String author, OffsetDateTime date, String message) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
      String formattedDate = date.format(formatter);

      return
          "tree " + rootNodeHash + System.lineSeparator() +
          "parent " + parentCommitHash + System.lineSeparator() +
          "author " + author + System.lineSeparator() +
          "date " + formattedDate + System.lineSeparator() +
          "message " + message;
   }
}
