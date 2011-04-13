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
package org.squilla.util;

import java.util.Collection;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public interface BlockingQueue extends Queue {

    public boolean add(Object e);

    public boolean contains(Object o);

    public int drainTo(Collection c);

    public int drainTo(Collection c, int maxElements);

    public boolean offer(Object e);

    public boolean offer(Object e, long timeout) throws InterruptedException;

    public Object poll(long timeout) throws InterruptedException;

    public void put(Object e) throws InterruptedException;

    public int remainingCapacity();

    public boolean remove(Object o);

    public Object take() throws InterruptedException;
}
