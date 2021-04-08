package com.runcode.server.http.controller;

import cn.hutool.json.JSONUtil;
import com.runcode.docker.DockerJavaClient;
import com.runcode.entities.CodeLang;
import com.runcode.entities.CodeWrapperDTO;
import com.runcode.server.callback.HttpCallback;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 处理前端Rest请求
 *
 * @author JR
 */
public class CodeRunController {

    DockerJavaClient dockerJavaClient = DockerJavaClient.getSingleton();

    /**
     * 处理前端请求
     *
     * @param request
     * @return
     */
    @Ignore
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        CodeWrapperDTO codeWrapper = JSONUtil.toBean(request.content().toString(CharsetUtil.UTF_8), CodeWrapperDTO.class);

        if (!Arrays.stream(CodeLang.values()).map(it -> it.name()).collect(Collectors.toList()).contains(codeWrapper.getLangType().toUpperCase())) {
            throw new RuntimeException("语言类型错误");
        }

        dockerJavaClient.exec(CodeLang.valueOf(codeWrapper.getLangType().toUpperCase()), codeWrapper.getContent(), new HttpCallback(ctx, request));
    }
}
