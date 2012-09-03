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

/**
 * Reports the progress of a query in terms of single HTTP requests. A query
 * may be completed in a single step, or it may take a substantial number of
 * resquests.
 * 
 * <p>The number of steps that is required to perform a query isn't known
 * beforehand, the reported number of overall steps may therefore grow (but
 * never shrink) over the course of the query.
 * 
 * TODO: Document the phases and under what circumstances the overall number
 * can increase.
 */
public interface ProgressListener {

    public enum Phase { FILES, LINES }
    
    /**
     * Called before an HTTP request is performed.
     * 
     * @param phase the phase to which the next step will belong
     * @param current the number of the next step, counting from zero
     * @param overall the overall number of requests that must be performed,
     *             as it has been determined at this point
     */
    void progress(Phase phase, int current, int pending);
    
    void currentCounts(int dirCount, int fileCount, int lineCount);
    
}
