/*
 * Copyright 2010 Valley Campus Japan
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
package org.squilla.util;

/**
 * Tweaked version of com.ajile.util.FifoQueue
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class FifoQueue {

    private int maxElementCount;
    private int head;
    private int tail;
    private int elementCount;
    private Object[] elementData;

    public FifoQueue(int i) {
        this.head = 0;
        this.tail = 0;
        this.elementCount = 0;
        this.maxElementCount = i;
        this.elementData = new Object[i];
    }

    public int getElementCount() {
        return this.elementCount;
    }

    public boolean isEmpty() {
        if (this.elementCount == 0) {
            return true;
        }
        return false;
    }

    public boolean isFull() {
        if (this.elementCount >= (this.maxElementCount - 1)) {
            return true;
        }
        return false;
    }

    public synchronized int enqueue(Object obj) {
        if (this.elementCount >= this.maxElementCount) {
            return -1;
        }
        this.elementData[this.tail] = obj;
        this.tail = (this.tail + 1) % this.maxElementCount;
        this.elementCount++;
        this.notify();
        return this.maxElementCount - this.elementCount;
    }

    public synchronized Object dequeue() {
        if (this.elementCount <= 0) {
            return null;
        }
        Object obj = this.elementData[this.head];
        this.elementData[this.head] = null;
        this.head = (this.head + 1) % this.maxElementCount;
        this.elementCount--;
        return obj;
    }

    public synchronized Object blockingDequeue() {
        return this.blockingDequeue(0);
    }

    public synchronized Object blockingDequeue(int i) {
        while (this.elementCount <= 0) {
            try {
                this.wait((long) (i));
            } catch (InterruptedException ex) {
            }
            if (i > 0 && this.elementCount <= 0) {
                return null;
            }
        }
        Object obj = this.elementData[this.head];
        this.elementData[this.head] = null;
        this.head = (this.head + 1) % this.maxElementCount;
        this.elementCount--;
        return obj;
    }

    /**
     * Add
     */
    public synchronized Object peek() {
        if (this.elementCount <= 0) {
            return null;
        }
        return this.elementData[this.head];
    }
}
