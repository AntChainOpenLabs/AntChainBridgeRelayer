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

package com.alipay.antchain.bridge.relayer.cli.command;

import java.util.ArrayList;
import java.util.List;

public class Command {

    /**
     * command name
     */
    private String name;

    /**
     * command args description
     */
    private List<Arg> args = new ArrayList<>();

    /**
     * create command with command name
     *
     * @param name
     */
    public Command(String name) {
        this.name = name;
    }

    /**
     * add args
     *
     * @param argName
     * @param constraints: parameter value constraint
     */
    public void addArgs(String argName, String type, List<String> constraints) {

        Arg item = new Arg();
        item.name = argName;
        item.type = type;
        item.constraints = constraints;

        this.args.add(item);
    }

    /**
     * Getter method for property <tt>name.</tt>.
     *
     * @return property value of name.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter method for property <tt>args.</tt>.
     *
     * @return property value of args.
     */
    public List<Arg> getArgs() {
        return args;
    }

    /**
     * Parameter description class
     */
    public class Arg {
        private String name;
        private String type;
        private List<String> constraints;

        /**
         * Getter method for property <tt>name.</tt>.
         *
         * @return property value of name.
         */
        public String getName() {
            return name;
        }

        /**
         * Getter method for property <tt>constraints.</tt>.
         *
         * @return property value of constraints.
         */
        public List<String> getConstraints() {
            return constraints;
        }

        /**
         * Getter method for property <tt>type.</tt>.
         *
         * @return property value of type.
         */
        public String getType() {
            return type;
        }
    }
}
