package com.windowtester.internal.finder.matchers.swing;

import abbot.finder.matchers.AbstractMatcher;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class TreePathMatcher extends AbstractMatcher {

  private final String treePath;

  public TreePathMatcher(String treePath) {
    this.treePath = treePath;
  }

  @Override
  public boolean matches(Component component) {
    if (component instanceof JTree tree) {
      var rootNode = tree.getModel().getRoot();
      if (rootNode instanceof DefaultMutableTreeNode treeNode) {
        var iter = Arrays.stream(treePath.split("/")).toList().iterator();
        return searchTreeNode(List.of(treeNode), iter);
      }
    }
    return false;
  }

  private boolean searchTreeNode(
      List<DefaultMutableTreeNode> treeNodes, Iterator<String> searchIterator) {
    var nodeName = searchIterator.next();
    for (DefaultMutableTreeNode treeNode : treeNodes) {
      if (treeNode.toString().equalsIgnoreCase(nodeName)) {
        if (!searchIterator.hasNext()) {
          return true;
        }

        var treeChildren = getTreeChild(treeNode);
        if (treeChildren.isEmpty()) {
          return !searchIterator.hasNext();
        }
        return searchTreeNode(treeChildren, searchIterator);
      }
    }
    return false;
  }

  private List<DefaultMutableTreeNode> getTreeChild(DefaultMutableTreeNode treeNode) {
    var children = new ArrayList<DefaultMutableTreeNode>();
    for (int i = 0; i < treeNode.getChildCount(); i++) {
      var child = treeNode.getChildAt(i);
      if (child instanceof DefaultMutableTreeNode childNode) {
        children.add(childNode);
      }
    }
    return children;
  }
}
