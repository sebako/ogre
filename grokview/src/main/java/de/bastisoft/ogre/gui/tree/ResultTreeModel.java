/*
 * Copyright 2012 Sebastian Koppehel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bastisoft.ogre.gui.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.FileMatch.LineMatch;

public class ResultTreeModel implements TreeModel {

    private DirNode root = new DirNode(Collections.<DirNode>emptyList(), "/");
    
    private Collection<TreeModelListener> listeners = new CopyOnWriteArrayList<>();
    
    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof ResultTreeNode)
            return ((ResultTreeNode) parent).childAt(index);
        
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof ResultTreeNode)
            return ((ResultTreeNode) parent).numChildren();
        
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof LineMatch
                || node instanceof FileNode
                    && ((FileNode) node).fileMatch.getLines().size() == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // tree cannot be edited by the user
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof ResultTreeNode)
            return ((ResultTreeNode) parent).indexOf(child);
        
        return -1;
    }
    
    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
    
    
    /* So much for the TreeModel interface. Now some methods to update the results. */
    
    /**
     * Add a number of new file matches. If a file's directory is already represented in the
     * tree, the match will be added to the existing directory node. Other than that, the
     * order of the file matches passed to this method is preserved in the tree.
     * 
     * @param matches file matches to be added
     */
    public void addFileMatches(Collection<FileMatch> matches) {
        // Directory and file match nodes for firing insertion events later.
        Map<DirNode, List<ResultTreeNode>> added = new HashMap<>();
        
        for (FileMatch match : matches) {
            String dirname = match.getDirectory();
            DirNode dir = navigateToDir(dirname, added);
            FileNode node = dir.add(match);
            
            if (old(dir, added)) {
                /* The directory was already there, so listeners will not receive an insertion
                 * notification for the directory. We must notify them of the new file match node. */
                
                List<ResultTreeNode> list = added.get(dir);
                if (list == null)
                    added.put(dir, list = new ArrayList<>());
                list.add(node);
            }
        }
        
        for (DirNode dir : added.keySet())
            fireAddedToDir(dir, added.get(dir));
    }
        
    
    private DirNode navigateToDir(String path, Map<DirNode, List<ResultTreeNode>> added) {
        List<String> parts = pathComponents(path);
        
        DirNode dir = root;
        boolean create = false;
        
        for (String part : parts) {
            if (create)
                dir = dir.mkdir(part);
            else {
                DirNode subdir = dir.subdir(part);
                if (subdir == null) {
                    subdir = dir.mkdir(part);
                    
                    List<ResultTreeNode> newChildren = added.get(dir);
                    if (newChildren == null)
                        added.put(dir, newChildren = new ArrayList<>());
                    newChildren.add(subdir);
                    
                    create = true;
                }
                dir = subdir;
            }
        }
        
        return dir;
    }
    
    private boolean old(DirNode dir, Map<DirNode, List<ResultTreeNode>> added) {
        Set<DirNode> allnew = new HashSet<>();
        for (DirNode d : added.keySet())
            for (ResultTreeNode node : added.get(d))
                if (node instanceof DirNode)
                    allnew.add((DirNode) node);
        
        for (DirNode parent : dir.parents())
            if (allnew.contains(parent))
                return false;
        
        return !allnew.contains(dir);
    }
    
    private List<String> pathComponents(String dir) {
        List<String> l = new ArrayList<>();
        int q = 0;
        int p = dir.indexOf('/');
        while (p > -1) {
            if (p > q) l.add(dir.substring(q, p));
            q = p + 1;
            p = dir.indexOf('/', q);
        }
        if (q < dir.length())
            l.add(dir.substring(q));
        return l;
    }
    
    private void fireAddedToDir(final DirNode dir, List<ResultTreeNode> children) {
        Collections.sort(children, new Comparator<ResultTreeNode>() {
            @Override
            public int compare(ResultTreeNode o1, ResultTreeNode o2) {
                return dir.indexOf(o1) - dir.indexOf(o2);
            }
        });
        int[] childIndices = new int[children.size()];
        for (int i = 0; i < children.size(); i++) {
            childIndices[i] = dir.indexOf(children.get(i));
//            if (childIndices[i] == -1) {
//                ResultTreeNode child = children.get(i);
//                System.out.print(dir.name + " ");
//                if (child instanceof Directory)
//                    System.out.println("D_" + ((Directory) child).name);
//                else if (child instanceof FileNode)
//                    System.out.println("F_" + ((FileNode) child).fileMatch.getFilename());
//                else
//                    System.out.println(child);
//            }
        }
        TreeModelEvent event = new TreeModelEvent(this, dir.treePath(), childIndices, children.toArray());
        for (TreeModelListener l : listeners)
            l.treeNodesInserted(event);
    }
    
    /**
     * Reload the line matches for a file match that's already in the tree.
     * 
     * @param match file match object that was amended
     */
    public void amendFileMatch(FileMatch match) {
        FileNode found = null;
        
        DirNode dir = navigateToDir(match.getDirectory());
        if (dir != null)
            for (FileNode node : dir.files())
                if (node.fileMatch.getFullName().equals(match.getFullName())) {
                    found = node;
                    break;
                }
        
        if (found == null)
            addFileMatches(Collections.singletonList(match));
        
        /* It would be very hard to even make it possible to tell which lines are new,
         * so let's just report a subtree structure change. */
        
        TreeModelEvent event = new TreeModelEvent(this, found.treePath());
        for (TreeModelListener l : listeners)
            l.treeStructureChanged(event);
    }
    
    /**
     * Finds a directory that already exists in the tree. If it does not exist, it is not
     * created, and {@code null} is returned.
     * 
     * @param path path of the directory as a slash-separated string
     * @return the directory that was specified by the path
     */
    private DirNode navigateToDir(String path) {
        List<String> parts = pathComponents(path);
        
        DirNode dir = root;
        for (String part : parts) {
            dir = dir.subdir(part);
            if (dir == null)
                return null;
        }
        
        return dir;
    }
    
    public void clear() {
        root = new DirNode(Collections.<DirNode>emptyList(), "/");
        TreeModelEvent event = new TreeModelEvent(this, new TreePath(root));
        for (TreeModelListener l : listeners)
            l.treeStructureChanged(event);
    }

}
