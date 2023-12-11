/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.shell;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.alipay.mychain.oracle.anchor.cli.command.NamespaceManager;
import com.alipay.mychain.oracle.anchor.cli.glclient.GrpcClient;
import com.alipay.mychain.oracle.anchor.cli.util.JsonUtil;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 *  @author honglin.qhl
 *  @version $Id: Shell.java, v 0.1 2017-06-15 下午2:22 honglin.qhl Exp $$
 */
public class Shell {

    private static String PROMPT = "relayer>";

    private NamespaceManager namespaceManager;

    private Terminal terminal;

    private GlLineReader reader;

    private Map<String, ReservedWord> reservedWord = new HashMap<>();

    private AtomicBoolean loopRunning = new AtomicBoolean(false);

    private ReentrantLock shellLock = new ReentrantLock();

    private PromptCompleter completer;

    private ShellProvider shellProvider;

    public final static Runtime Runtime = new Runtime();

    public Shell(ShellProvider shellProvider, PromptCompleter completer, GrpcClient grpcClient,
                 NamespaceManager namespaceManager) {

        // 不可扩展参数初始化
        init();

        // 扩展初始化
        this.shellProvider = shellProvider;
        this.namespaceManager = namespaceManager;

        this.completer = completer;
        this.reservedWord.keySet().forEach(reservedWord -> this.completer.addReservedWord(reservedWord));

        reader.setCompleter(completer);

        // 运行时设置
        Runtime.setGrpcClient(grpcClient);
    }

    void init() {
        // init term
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("can't open system stream");
        }

        // set printer
        Runtime.setPrinter(terminal.writer());

        // init linereader
        reader = new GlLineReader(terminal, "mychain-gl", new HashMap<>());

        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("./clihistory.tmp"));
        reader.setHistory(new DefaultHistory(reader));

        reader.unsetOpt(Option.MENU_COMPLETE);
        reader.setOpt(Option.AUTO_LIST);
        reader.unsetOpt(Option.AUTO_MENU);


        // init shell commands
        initReservedWord();
    }

    public void start() {

        try {
            if (shellLock.tryLock()) {

                if (loopRunning.get()) {
                    return;
                }

                loopRunning.set(true);

                welcome();

                new Thread(() -> {
                    // start loop
                    while (loopRunning.get()) {
                        String cmd = reader.readLine(PROMPT);

                        if (null == cmd || cmd.isEmpty()) {
                            continue;
                        }

                        try {
                            if (reservedWord.containsKey(cmd.trim())) {

                                reservedWord.get(cmd.trim()).execute();
                                continue;
                            }

                            // 取消CLI的反斜杠替换处理，取消后，嵌套的转义字符串在CLI需要4个反斜杠
                            // cmd = cmd.replaceAll("\\\\","\\\\\\\\");


                            String result = this.shellProvider.execute(cmd);


                            if (null != result) {
                                if (result.startsWith("{") || result.startsWith("[")) {
                                    Runtime.getPrinter().println(JsonUtil.format(result));
                                } else {
                                    Runtime.getPrinter().println(result);
                                }

                            }
                        } catch (Exception e) {
                            Runtime.getPrinter().println("shell evaluate fail:" + e.getMessage());
                        }
                    }
                }, "shell_thread").start();

            }
        } finally {
            shellLock.unlock();
        }
    }

    public String execute(String cmd) {
        return this.shellProvider.execute(cmd);
    }

    public void stop() {

        loopRunning.set(false);
        try {
            if (null != Runtime.getGrpcClient()) {
                Runtime.getGrpcClient().shutdown();
            }
        } catch (InterruptedException e) {
            // not process
        }
    }

    protected void initReservedWord() {
        this.reservedWord.put("exit", this::exit);
        this.reservedWord.put("help", this::help);
    }

    protected void exit() {
        stop();
    }

    protected void help() {
        Runtime.getPrinter().print(namespaceManager.dump());
    }

    protected void welcome() {
        Runtime.printer.println("------------------------------------------------------------");
        Runtime.printer.println("------------------------------------------------------------");
        Runtime.printer.println("----                                                   -----");
        Runtime.printer.println("----                  RELAYER CONSOLE                  -----");
        Runtime.printer.println("----                                                   -----");
        Runtime.printer.println("------------------------------------------------------------");
        Runtime.printer.println("------------------------------------------------------------");
        Runtime.printer.println("type help to see more...");
    }

    public static class Runtime {

        private PrintWriter printer;

        private GrpcClient grpcClient;

        void setPrinter(PrintWriter printer) {

            this.printer = printer;
        }

        void setGrpcClient(GrpcClient grpcClient) {
            this.grpcClient = grpcClient;
        }

        public PrintWriter getPrinter() {
            return printer;
        }

        public GrpcClient getGrpcClient() {
            return grpcClient;
        }

    }
}
