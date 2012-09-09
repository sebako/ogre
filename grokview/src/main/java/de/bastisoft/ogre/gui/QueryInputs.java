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

package de.bastisoft.ogre.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class QueryInputs {

    static enum Input {
        
        QUERY   ("fulltext"),
        DEFS    ("definition"),
        REFS    ("symbol"),
        PATH    ("path"),
        HIST    ("history"),
        PROJECT ("project");
        
        final String resource;
        
        Input(String resource) {
            this.resource = resource;
        }
        
    }
    
	private Map<Input, String> values = new HashMap<>();
	private List<String> projects = new ArrayList<>();
    
	String getInput(Input input) {
        return values.get(input);
    }
    
    void setInput(Input input, String value) {
        values.put(input, value);
    }
    
    List<String> getProjects() {
        return new ArrayList<>(projects);
    }
    
    /**
     * Sets list of previously entered or retrieved projects.
     * 
     * @param projects project list
     */
    void setProjects(Collection<String> projects) {
        this.projects.clear();
        this.projects.addAll(projects);
    }
    
}
