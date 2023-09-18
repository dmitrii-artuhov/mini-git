package ru.hse.mit.git.components.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import ru.hse.mit.git.GitException;
import ru.hse.mit.git.components.fs.TreeFile;
import ru.hse.mit.git.components.utils.MiniGitUtils;

public class TreeNode extends Node {
   private final Map<String, Node> children = new HashMap<>();
   private String content = "";

   public static TreeNode createRoot() {
      return new TreeNode("");
   }

   public TreeNode(String nodeName) {
      super(nodeName, NodeType.TREE_NODE);
   }

   /**
    *
    * @return blobs entries: { filename in working directory, hash }
    */
   public Map<String, String> getBlobs() {
      return getBlobs("");
   }


   private Map<String, String> getBlobs(String namePrefix) {
      Map<String, String> result = new HashMap<>();

      for (var childEntry : children.entrySet()) {
         String childName = childEntry.getKey();
         Node childNode = childEntry.getValue();

         switch (childNode.getType()) {
            case TREE_NODE -> result.putAll(((TreeNode)childNode).getBlobs(namePrefix + childName + "/"));
            case BLOB_NODE -> result.put(namePrefix + childName, childNode.getHash().get());
         }
      }

      return result;
   }

   public static TreeNode loadTree(Path pathToTreesDir, String hash) throws GitException {
      return loadTree(pathToTreesDir, hash, "");
   }

   private static TreeNode loadTree(Path pathToTreesDir, String hash, String name) throws GitException {
      TreeNode node = new TreeNode(name);
      node.hash = Optional.of(hash);

      try(Stream<String> stream = Files.lines(Path.of(pathToTreesDir.toString(), hash))) {
         List<String> lines = stream.toList();

         for (String line : lines) {
            String[] data = line.split(" ");
            String childType = data[0];
            String childHash = data[1];
            String childName = data[2];

            Node child;

            if (childType.equals("tree")) {
               child = loadTree(pathToTreesDir, childHash, childName);
            }
            else {
               child = new BlobNode(childName, childHash);
            }

            node.children.put(childName, child);
         }

      } catch (IOException e) {
         throw new GitException(e.getMessage(), e.getCause());
      }

      return node;
   }

   public void addChildren(int index, List<String> names, String blobHash) {
      if (index == names.size() - 1) {
         addBlob(names.get(index), blobHash);
         return;
      }

      String treeNodeName = names.get(index);
      if (!children.containsKey(treeNodeName)) {
         children.put(treeNodeName, new TreeNode(treeNodeName));
      }

      ((TreeNode)children.get(treeNodeName)).addChildren(index + 1, names, blobHash);
   }

   public void addBlob(String name, String hash) {
      if (!children.containsKey(name)) {
         children.put(name, new BlobNode(name, hash));
      }
   }

   public void buildGraph() {
      StringBuilder content = new StringBuilder();

      for (var childEntry : children.entrySet()) {
         String childName = childEntry.getKey();
         Node childNode = childEntry.getValue();

         switch (childNode.type) {
            case TREE_NODE -> {
               TreeNode treeNode = (TreeNode) childNode;
               treeNode.buildGraph();
               String hash = treeNode.getHash().get();

               content
                   .append("tree ")
                   .append(hash).append(" ")
                   .append(childName)
                   .append(System.lineSeparator());
            }
            case BLOB_NODE -> {
               content
                   .append("blob ")
                   .append(childNode.getHash().get()).append(" ")
                   .append(childName)
                   .append(System.lineSeparator());
            }
         }
      }

      String currentNodeHash = MiniGitUtils.getHashFromBytes(content.toString().getBytes());
      this.hash = Optional.of(currentNodeHash);
      this.content = content.toString();
   }

   public void saveGraph(Path fullPath) throws GitException {
      for (var childEntry : children.entrySet()) {
         Node childNode = childEntry.getValue();

         // only save tree-nodes, because blob-nodes are already saved
         if (childNode.type == NodeType.TREE_NODE) {
            TreeNode treeNode = (TreeNode)childNode;
            treeNode.saveGraph(fullPath);
         }
      }

      TreeFile treeFile = new TreeFile(fullPath, content.getBytes());
      treeFile.save();
   }
}







