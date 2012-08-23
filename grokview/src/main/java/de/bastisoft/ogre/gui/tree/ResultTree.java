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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.FileMatch.LineMatch;

/**
 * Tree component to display OpenGrok search results.
 * 
 * <p>The appearance of this component is closely modeled after the search result
 * display in Eclipse.
 * 
 * @author Sebastian Koppehel
 */
public class ResultTree extends JTree {

    private ResultTreeModel model;
    private boolean expanding;
    private LinkHandler linkHandler;
    
    public ResultTree() {
        super();
        
        setModel(model = new ResultTreeModel());
        model.addTreeModelListener(new TreeModelListener() {
            @Override public void treeNodesInserted(final TreeModelEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { handleModelEvent(e); }
                });
            }
            
            @Override public void treeStructureChanged(TreeModelEvent e) {}
            @Override public void treeNodesRemoved(TreeModelEvent e) {}
            @Override public void treeNodesChanged(TreeModelEvent e) {}
        });
        
        addTreeExpansionListener(new TreeExpansionListener() {
            @Override public void treeExpanded(final TreeExpansionEvent event) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { handleExpansionEvent(event); }
                });
            }
            
            @Override public void treeCollapsed(TreeExpansionEvent event) {}
        });
        
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleMousePressed(e); }
        });
        
        // Tweak appearance
        setRootVisible(false);
        setShowsRootHandles(true);
        setCellRenderer(new CellRenderer());
    }
    
    private void handleModelEvent(TreeModelEvent event) {
        if (expanding)
            for (Object o : event.getChildren())
                expandRecursive(event.getPath(), o, false);
    }
    
    private void expandRecursive(Object[] path, Object node, boolean firstOnly) {
        if (model.getChildCount(node) == 0) {
            
            if (path.length > 0) // path.length == 0 happens when the root node has no children
                expandPath(new TreePath(path));
            return;
        }
        
        Object[] newpath = new Object[path.length + 1];
        System.arraycopy(path, 0, newpath, 0, path.length);
        newpath[newpath.length - 1] = node;
        
        int count = firstOnly ? 1 : model.getChildCount(node);
        
        for (int i = 0; i < count; i++)
            expandRecursive(newpath, model.getChild(node, i), firstOnly);
    }
    
    private void handleMousePressed(MouseEvent event) {
        // Only interested in double-clicks
        if (event.getClickCount() != 2)
            return;
        
        TreePath selection = getSelectionPath();
        if (selection != null) {
            final Object o = selection.getLastPathComponent();
            // If the user clicked on a line match, and a request handler was installed, request the file.
            if (o instanceof LineMatch && linkHandler != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { linkHandler.requested((LineMatch) o); }
                });
            }
        }
    }
    
    private void handleExpansionEvent(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        Object expandedNode = path.getLastPathComponent();
        if (model.getChildCount(expandedNode) == 1) {
            Object child = model.getChild(expandedNode, 0);
            if (child instanceof DirNode) {
                TreePath childpath = path.pathByAddingChild(child);
                if (!isExpanded(childpath))
                    setExpandedState(childpath, true);
            }
        }
    }
    
//    private static String printTreePath(TreePath path) {
//        Object[] p = path.getPath();
//        StringBuilder sb = new StringBuilder();
//        sb.append("[");
//        for (int i = 0; i < p.length; i++) {
//            sb.append(p[i]);
//            if (i < p.length - 1)
//                sb.append(", ");
//        }
//        sb.append("]");
//        return sb.toString();
//    }
    
    /**
     * Installs a link handler that will be called when the user requests that
     * a line match be displayed, opened in a browser or whatever the case may be.
     * 
     * <p>The handler's {@code rquested} method will not be called in the event
     * dispatch thread.
     * 
     * @param handler a handler that will handle requests issued by the user 
     */
    public void setFileRequestHandler(LinkHandler handler) {
        linkHandler = handler;
    }
    
    /**
     * Tells the tree whether or not nodes should automatically be expanded as
     * they're being added to the tree.
     * 
     * @param expanding {@code true} if nodes should be expanded
     */
    public void setExpanding(boolean expanding) {
        this.expanding = expanding;
    }
    
    /**
     * Adds a number of file matches to the tree.
     * 
     * @param matches new file matches
     */
    public void addFileMatches(Collection<FileMatch> matches) {
        model.addFileMatches(matches);
    }
    
    /**
     * Informs the tree that a file match has changed (in practice, has received
     * more line matches) since it was added to the tree.
     * 
     * @param match the modified file match
     */
    public void updateFileMatch(FileMatch match) {
        model.amendFileMatch(match);
    }
    
    /**
     * Expands nodes down to the first file match in the tree.
     */
    public void expandFirst() {
        expandRecursive(new Object[0], model.getRoot(), true);
    }
    
    /**
     * Resets the tree. Removes all directories and file matches.
     */
    public void reset() {
        model.clear();
    }
    
}
