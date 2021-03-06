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
import java.util.List;

/**
 * Represents the information parsed out of a single OpenGrok result page.
 * Intended for preliminary internal use only.
 */
class ResultPage {

    /**
     * The URL of this result page.
     */
    URL url;
    
    /**
     * File matches found on this result page.
     */
    List<FileMatch> fileMatches;
    
    /**
     * Links to further result pages for the query that were indicated on this result page,
     * in the order in which they appeared.
     */
    List<WebLink> pageLinks;
    
}
