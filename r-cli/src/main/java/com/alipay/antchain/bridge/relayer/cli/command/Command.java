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

/**
 * 命令类
 *
 * @author honglin.qhl
 * @version $Id: Command.java, v 0.1 2017-06-17 下午4:33 honglin.qhl Exp $$
 */
public class Command {

    /**
     * 命令名称
     */
    private String name;

    /**
     * 命令参数描述
     */
    private List<Arg> args = new ArrayList<>();

    /**
     * 使用命令名称构造命令
     *
     * @param name
     */
    public Command(String name) {
        this.name = name;
    }

    /**
     * 添加参数
     *
     * @param argName    参数名称
     * @param contraints 参数取值约束
     */
    public void addArgs(String argName, String type, List<String> contraints) {

        Arg item = new Arg();
        item.name = argName;
        item.type = type;
        item.contraints = contraints;

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
     * 参数描述类
     */
    public class Arg {
        private String name;
        private String type;
        private List<String> contraints;

        /**
         * Getter method for property <tt>name.</tt>.
         *
         * @return property value of name.
         */
        public String getName() {
            return name;
        }

        /**
         * Getter method for property <tt>contraints.</tt>.
         *
         * @return property value of contraints.
         */
        public List<String> getContraints() {
            return contraints;
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
