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
import java.util.Collections;
import java.util.List;

import javax.swing.tree.TreePath;

import de.bastisoft.ogre.FileMatch;

class DirNode implements ResultTreeNode, Comparable<DirNode> {
	
	final String name;
	
	private List<DirNode> parents;
	private List<FileNode> files = new ArrayList<>();
	private List<DirNode> subdirs = new ArrayList<>();
	
	DirNode(List<DirNode> parents, String name) {
		this.parents = Collections.unmodifiableList(parents);
		this.name = name;
	}
	
	public TreePath treePath() {
		DirNode[] path = new DirNode[parents.size() + 1];
		for (int i = 0; i < parents.size(); i++)
			path[i] = parents.get(i);
		path[path.length - 1] = this;
		return new TreePath(path);
	}
	
	public int numChildren() {
		return subdirs.size() + files.size();
	}
	
	public Object childAt(int index) {
		if (index < subdirs.size())
			return subdirs.get(index);
		return files.get(index - subdirs.size());
	}
	
	public int indexOf(Object child) {
		if (child instanceof DirNode)
			return subdirs.indexOf(child);
		
		if (child instanceof FileNode)
			return subdirs.size() + files.indexOf(child);
		
		return -1;
	}
	
	DirNode mkdir(String name) {
		List<DirNode> newParents = new ArrayList<>(parents.size() + 1);
		newParents.addAll(parents);
		newParents.add(this);
		DirNode subdir = new DirNode(newParents, name);
		subdirs.add(subdir);
		Collections.sort(subdirs);
		return subdir;
	}
	
	DirNode subdir(String name) {
		for (DirNode d : subdirs)
			if (name.equals(d.name))
				return d;
		return null;
	}
	
	FileNode add(FileMatch file) {
		FileNode node = new FileNode(this, file);
		files.add(node);
		Collections.sort(files);
		return node;
	}
	
	List<DirNode> parents() {
		return parents;
	}
	
	DirNode parent() {
		return parents.get(parents.size() - 1);
	}
	
	List<FileNode> files() {
		return files;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DirNode && ((DirNode) other).name.equals(name);
	}

	@Override
	public int compareTo(DirNode o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return "D(" + name + ")";
	}
	
}