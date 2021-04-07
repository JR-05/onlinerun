package com.runcode.server.callback;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.github.dockerjava.core.command.ExecStartResultCallback;

/**
 * 接收到docker信息的回调
 *
 * @author JR
 */
public abstract class ResultCallback extends ResultCallbackTemplate<ExecStartResultCallback, Frame> {

    /**
     * 是否是程序执行的最后一条指令
     */
    private boolean isFinal;

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean isFinal() {
        return isFinal;
    }
}
