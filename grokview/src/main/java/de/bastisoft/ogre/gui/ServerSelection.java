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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

class ServerSelection {

    static interface ServerSelectionListener {
        void serverSelected(Server server);
    }
    
    private Collection<ServerSelectionListener> listeners = new CopyOnWriteArrayList<>();
    private Server selected;
    
    Server getSelected() {
        return selected;
    }
    
    void setSelected(Server server) {
        if (selected == server)
            return;
        selected = server;
        
        for (ServerSelectionListener l : listeners)
            l.serverSelected(selected);
    }
    
    void addListener(ServerSelectionListener listener) {
        listeners.add(listener);
    }
    
}
