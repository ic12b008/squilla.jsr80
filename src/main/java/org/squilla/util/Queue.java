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
 * Java ME compatible version of "java.util.Queue".
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public interface Queue extends Collection {

    public boolean add(Object e);

    public Object element();

    public boolean offer(Object e);

    public Object peek();

    public Object poll();

    public Object remove();
}
