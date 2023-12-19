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

import java.util.Map;

/**
 * 命令命名空间
 *
 * @author honglin.qhl
 * @version $Id: CmdNamespace.java, v 0.1 2017-06-14 下午9:32 honglin.qhl Exp $$
 */
public interface CommandNamespace {

    /**
     * 命名空间名称
     *
     * @return
     */
    public String name();

    /**
     * 获取命令命名空间下所有命令
     *
     * @return
     */
    public Map<String, Command> getCommands();
}
