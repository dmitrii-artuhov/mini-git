package ru.hse.mit.git.components.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import ru.hse.mit.git.GitException;

public class MiniGitUtils {
   public static String getHashFromBytes(byte[] bytes) {
      // set encryption algorithm
      MessageDigest md = null;
      try {
         md = MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }

      byte[] hashBytes = md.digest(bytes);

      // Convert the hash bytes to a hexadecimal string
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
         sb.append(String.format("%02x", b));
      }

      return sb.toString();
   }

   public static void checkFileExists(File file) throws GitException {
      if (!file.exists()) {
         throw new GitException("File '" + file.getName() + "' does not exists");
      }
   }

   public static void checkFilesExists(List<File> files) throws GitException {
      for (File file : files) {
         if (!file.exists()) {
            throw new GitException("File '" + file.getName() + "' does not exists");
         }
      }
   }

   public static byte[] getFileBytes(Path fullPath) throws GitException {
      File file = new File(fullPath.toString());
      checkFileExists(file);

      try {
         return FileUtils.readFileToByteArray(file);
      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }
   }
}
