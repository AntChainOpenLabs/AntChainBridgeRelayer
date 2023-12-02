/*
 * Copyright 2023 Ant Group
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

package com.alipay.antchain.bridge.relayer.core.types.network.provider;

import java.security.AccessController;
import java.security.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

import sun.security.action.PutAllAction;

public class MyProvider extends Provider {

    private static final long serialVersionUID = 1675722449913627123L;

    /**
     * Constructs a provider with the specified name, version number,
     * and information.
     */
    public MyProvider() {
        super("MyProvider", 1.0, "test");

        // if there is no security manager installed, put directly into
        // the provider. Otherwise, create a temporary map and use a
        // doPrivileged() call at the end to transfer the contents
        if (System.getSecurityManager() == null) {
            MyEntries.putEntries(this);
        } else {
            // use LinkedHashMap to preserve the order of the PRNGs
            Map<Object, Object> map = new LinkedHashMap<>();
            MyEntries.putEntries(map);
            AccessController.doPrivileged(new PutAllAction(this, map));
        }
    }
}
