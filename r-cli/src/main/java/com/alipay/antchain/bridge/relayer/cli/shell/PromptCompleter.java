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

package com.alipay.antchain.bridge.relayer.cli.shell;

import java.util.ArrayList;
import java.util.List;

import com.alipay.antchain.bridge.relayer.cli.command.CommandNamespace;
import com.alipay.antchain.bridge.relayer.cli.command.NamespaceManager;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class PromptCompleter implements Completer {

    private List<CommandNamespace> namespaces = new ArrayList<>();

    private List<String> reservedWords = new ArrayList<>();

    /**
     * add namespace
     *
     * @param namespaceManager
     */
    public void addNamespace(NamespaceManager namespaceManager) {

        namespaceManager.getCommandNamespaces().forEach(namespace -> namespaces.add(namespace));
    }

    /**
     * add reserved word
     *
     * @param reservedWord
     */
    public void addReservedWord(String reservedWord) {
        this.reservedWords.add(reservedWord);
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine commandLine, List<Candidate> candidates) {
        assert commandLine != null;
        assert candidates != null;

        String buffer = commandLine.line().substring(0, commandLine.cursor());

        // If you do not enter `.` Symbol, the reserved word and command are completed.
        if (!buffer.contains(".")) {

            // Complete reserved word.
            reservedWords.forEach(reservedWord -> {
                if (!buffer.isEmpty() && !reservedWord.startsWith(buffer)) {
                    return;
                }

                candidates.add(new Candidate(reservedWord, reservedWord, null, null, null, null, true));
            });

            // Complete command.
            namespaces.forEach(namespace -> {

                if (!buffer.isEmpty() && !namespace.name().startsWith(buffer)) {
                    return;
                }

                StringBuilder sb = new StringBuilder(namespace.name());
                namespace.getCommands().forEach((cmdName, cmd) -> {

                    sb.append("\n\t.").append(cmdName);
                    if (!cmd.getArgs().isEmpty()) {
                        sb.append("(");
                        cmd.getArgs().forEach(arg -> {
                            sb.append("String " + arg.getName()).append(",");
                        });
                        sb.deleteCharAt(sb.length() - 1);
                        sb.append(")");

                    } else {
                        sb.append("()");
                    }
                });

                candidates.add(new Candidate(namespace.name() + ".", namespace.name(), null, null, null, null, true));
            });
        } else if (buffer.contains("(")) {
            // If a complete command has been entered (check whether the `(` symbol has been entered), complete the detailed parameter fields.

            String[] buf = buffer.split("\\.");

            namespaces.forEach(namespace -> {

                if (!namespace.name().equals(buf[0])) {
                    return;
                }
                namespace.getCommands().forEach((cmdName, cmd) -> {

                    String command = buf[1].split("\\(")[0];

                    if (cmdName.equals(command)) {

                        StringBuilder sb = new StringBuilder(cmdName);
                        if (!cmd.getArgs().isEmpty()) {
                            sb.append("(");
                            cmd.getArgs().forEach(arg -> {
                                sb.append("\n    " + arg.getType() + " " + arg.getName()).append(" ");

                                if (!arg.getConstraints().isEmpty()) {
                                    sb.append(" //");
                                    arg.getConstraints().forEach(contraint -> {
                                        sb.append(contraint).append(",");
                                    });
                                    sb.deleteCharAt(sb.length() - 1);
                                }
                            });
                            sb.append("\n)");

                            candidates.add(
                                new Candidate(buffer, sb.toString(), null, null, null,
                                    null, true));
                        } else {
                            sb.append("()");
                            candidates.add(
                                new Candidate(namespace.name() + "." + cmdName + "()", sb.toString(), null, null, null,
                                    null, true));
                        }
                    }
                });
            });
        } else {
            // If the `.` symbol is entered, the matching command is completed.
            String[] buf = buffer.split("\\.");

            namespaces.forEach(namespace -> {

                if (!namespace.name().equals(buf[0])) {
                    return;
                }

                long matchCount = namespace.getCommands().keySet().stream().filter(
                    cmdName -> cmdName.startsWith(buf.length <= 1 ? "" : buf[1])).count();

                namespace.getCommands().forEach((cmdName, cmd) -> {

                    if (cmdName.startsWith(buf.length <= 1 ? "" : buf[1])) {

                        StringBuilder sb = new StringBuilder(cmdName);
                        if (cmd.getArgs().isEmpty()) {
                            sb.append("()");
                            candidates.add(
                                new Candidate(namespace.name() + "." + cmdName + "()", sb.toString(), null, null, null,
                                    null, true));

                        } else if (matchCount == 1) {
                            sb.append("(");
                            cmd.getArgs().forEach(arg -> {
                                sb.append("\n    " + arg.getType() + " " + arg.getName()).append(" ");

                                if (!arg.getConstraints().isEmpty()) {
                                    sb.append(" //");
                                    arg.getConstraints().forEach(contraint -> {
                                        sb.append(contraint).append(",");
                                    });
                                    sb.deleteCharAt(sb.length() - 1);
                                }
                            });
                            sb.append("\n)");

                            candidates.add(
                                new Candidate(namespace.name() + "." + cmdName + "(", sb.toString(), null, null, null,
                                    null, true));
                        } else {
                            sb.append("(");
                            cmd.getArgs().forEach(arg -> {
                                sb.append(arg.getType() + " " + arg.getName()).append(",");
                            });
                            sb.deleteCharAt(sb.length() - 1);
                            sb.append(")");

                            candidates.add(
                                new Candidate(namespace.name() + "." + cmdName + "(", sb.toString(), null, null, null,
                                    null, true));
                        }
                    }
                });
            });
        }
    }
}
