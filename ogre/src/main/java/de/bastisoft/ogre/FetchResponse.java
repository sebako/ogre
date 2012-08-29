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

package de.bastisoft.ogre;

import java.net.URL;

import org.w3c.dom.Document;

/**
 * Records the response to an HTTP request.
 *
 * @author Sebastian Koppehel
 */
class FetchResponse {
    
    /**
     * The parsed document that was fetched from the server.
     */
    final Document document;
    
    /**
     * The URL of the page that was retrieved; this may differ from the originally requested
     * URL because of HTTP redirection. We're generally interested in the URL that the server
     * deems correct, because only that can serve as a reliable base URL for the relative URLs
     * found in the document, so we record this.
     * 
     * (If some site administrator smuggles an HTML "base" tag into the response and then
     * subsequent links rely on that, well, we have a problem then.)
     */
    final URL url;
    
    FetchResponse(Document document, URL url) {
        this.document = document;
        this.url = url;
    }
    
}
