/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
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
package net.oneandone.maven.summon.extension;

import java.util.ArrayList;
import java.util.List;

public class Restriction {
    /** null: allow all; empty: deny all; otherwise: allow keys */
    private List<String> keys;

    /** default is to deny all */
    public Restriction() {
        this.keys = new ArrayList<>();
    }

    public boolean isAllowed(String key) {
        return keys == null || keys.contains(key);
    }

    public Restriction allowProperty(String propertyName) {
        String property = System.getProperty(propertyName);
        if (property != null) {
            for (String entry : property.split(",")) {
                if (!entry.isBlank()) {
                    keys.add(entry.trim());
                }
            }
        }
        return this;
    }


    public void allow(String key) {
        if (keys == null) {
            keys = new ArrayList<>();
        }
        keys.add(key);
    }

    public void allowAll() {
        this.keys = null;
    }
    public void denyAll() {
        this.keys = new ArrayList<>();
    }

    public void add(Restriction other) {
        if (other.keys == null) {
            this.keys = null;
        } else {
            for (String key : other.keys) {
                allow(key);
            }
        }
    }

    public String toString() {
        return keys == null ? "[]" : keys.toString();
    }
}
