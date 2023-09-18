package ru.hse.mit.git.components.graph;

import java.util.Optional;

public class BlobNode extends Node {

   public BlobNode(String nodeName, String hash) {
      super(nodeName, NodeType.BLOB_NODE);
      this.hash = Optional.of(hash);
   }
}