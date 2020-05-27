/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.wildfly.transaction.client;

import java.net.URI;
import java.util.Objects;

class XAOutflowedKey {
    private final URI location;
    private final String parentName;

    XAOutflowedKey(final URI location, final String parentName) {
        this.location = location;
        this.parentName = parentName;
    }

    public boolean equals(final Object obj) {
        return obj instanceof XAOutflowedKey && equals((XAOutflowedKey) obj);
    }

    private boolean equals(final XAOutflowedKey key) {
        return Objects.equals(location, key.location) && Objects.equals(parentName, key.parentName);
    }

    public int hashCode() {
        return Objects.hashCode(location) * 31 + Objects.hashCode(parentName);
    }
}