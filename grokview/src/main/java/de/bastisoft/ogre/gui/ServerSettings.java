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

class ServerSettings {

    final String name;
    final String baseURL;
    final String proxyHost;
    final int proxyPort;
    final boolean fetchLines;
    final boolean fetchLinesLast;
    
    ServerSettings(String name) {
        this.name = name;
        baseURL = "";
        proxyHost = "";
        proxyPort = 8080;
        fetchLines = true;
        fetchLinesLast = true;
    }
    
    ServerSettings(
            String name,
            String baseURL,
            String proxyHost,
            int proxyPort,
            boolean fetchLines,
            boolean fetchLinesLast) {
        
        this.name = name;
        this.baseURL = baseURL;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.fetchLines = fetchLines;
        this.fetchLinesLast = fetchLinesLast;
    }
    
}
