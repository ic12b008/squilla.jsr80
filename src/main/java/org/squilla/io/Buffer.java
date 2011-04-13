/*
 * Copyright 2011 Shotaro Uchida
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
package org.squilla.io;

/**
 * 
 * @author Shotaro Uchida
 */
public abstract class Buffer {

    private int mark;
    private int position;
    private int limit;
    private int capacity;

    public Buffer(int capacity) {
        this.capacity = capacity;
        this.limit = capacity;
        this.mark = 0;
        this.position = 0;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position < mark || position > getLimit()) {
            throw new IllegalArgumentException("Out of range");
        }
        this.position = position;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit < getPosition() || limit > getCapacity()) {
            throw new IllegalArgumentException("Out of range");
        }
        this.limit = limit;
    }

    public void skip(int n) {
        position += n;
    }

    public void mark() {
        mark = getPosition();
    }
    
    public void reset() {
        setPosition(mark);
    }

    public void rewind() {
        mark = 0;
        setPosition(0);
    }

    public int getRemaining() {
        return getLimit() - getPosition();
    }

    public void flip() {
        setLimit(getPosition());
        rewind();
    }
}
