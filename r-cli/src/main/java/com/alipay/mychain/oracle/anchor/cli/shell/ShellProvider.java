
package com.alipay.mychain.oracle.anchor.cli.shell;

public interface ShellProvider {

    String execute(String cmd);

    void shutdown();
}
