package ru.hse.mit.git.components.graph;

import java.util.Optional;

public class Node {
   public enum NodeType {
      TREE_NODE,
      BLOB_NODE
   }

   protected Optional<String> hash = Optional.empty();
   protected String nodeName;
   protected NodeType type;

   public Node(String nodeName, NodeType type) {
      this.nodeName = nodeName;
      this.type = type;
   }

   public String getName() {
      return nodeName;
   }

   public Optional<String> getHash() {
      return hash;
   }

   public NodeType getType() {
      return type;
   }
}
