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

import java.util.List;

import javax.swing.tree.TreePath;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.LineMatch;

class FileNode implements ResultTreeNode, Comparable<FileNode> {
	
	final DirNode dir;
	final FileMatch fileMatch;
	
	private TreePath treePath;
	
	FileNode(DirNode dir, FileMatch fileMatch) {
		this.dir = dir;
		this.fileMatch = fileMatch;
	}
	
	@Override
	public TreePath treePath() {
		if (treePath == null) {
			List<DirNode> dirParents = dir.parents();
			Object[] path = new Object[dirParents.size() + 2];
			for (int i = 0; i < dirParents.size(); i++)
				path[i] = dirParents.get(i);
			path[path.length - 2] = dir;
			path[path.length - 1] = this;
			treePath = new TreePath(path);
		}
		return treePath;
	}
	
	@Override
	public int numChildren() {
		return fileMatch.getLines().size();
	}
	
	@Override
	public Object childAt(int index) {
		return fileMatch.getLines().get(index);
	}
	
	@Override
	public int indexOf(Object child) {
		if (child instanceof LineMatch)
			return fileMatch.getLines().indexOf(child);
		
		return -1;
	}
	
	@Override
	public int hashCode() {
		return fileMatch.getFilename().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof FileNode && ((FileNode) obj).fileMatch.getFilename().equals(fileMatch.getFilename());
	}
	
	@Override
	public int compareTo(FileNode o) {
		return fileMatch.getFilename().compareTo(o.fileMatch.getFilename());
	}
	
	@Override
	public String toString() {
		return "F(" + fileMatch.getFilename() + ")";
	}
	
}