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

package de.bastisoft.ogre.event;

import java.util.Collection;

import de.bastisoft.ogre.FileMatch;

/**
 * Receives preliminary results during the course of an ongoing query.
 */
public interface ResultReceiver {

    /**
     * Called when new file matches have been retrieved. These may be abridged even
     * if the scraper instance was configured to fetch all line matches; in that case
     * the additional line matches will be reported later.
     * 
     * @param newMatches the new file matches
     */
    void newFileMatches(Collection<FileMatch> newMatches);
    
    /**
     * Called when a file match object has been amended with additional line matches.
     * The file match will have been reported before in a call to newFileMatches().
     * 
     * @param amendedMatch the file match object that has been amended
     */
    void newLineMatches(FileMatch amendedMatch);
    
}
