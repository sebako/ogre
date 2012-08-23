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

/**
 * Holds the position, size, and maximised state of a frame.
 */
class FrameState {

    final int x, y, width, height;
    final boolean maximizedHorizontal, maximizedVertical;
    final int splitPosition;
    
    FrameState(int x, int y, int width, int height,
            boolean maximizedHorizontal, boolean maximizedVertical,
            int splitPosition) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maximizedHorizontal = maximizedHorizontal;
        this.maximizedVertical = maximizedVertical;
        this.splitPosition = splitPosition;
    }
    
}
