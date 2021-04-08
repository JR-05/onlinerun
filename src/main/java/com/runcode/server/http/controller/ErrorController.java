package com.runcode.server.http.controller;

import cn.hutool.json.JSONUtil;
import com.runcode.docker.DockerJavaClient;
import com.runcode.entities.CodeLang;
import com.runcode.entities.CodeWrapperDTO;
import com.runcode.entities.Result;
import com.runcode.server.callback.HttpCallback;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import org.junit.Ignore;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 处理前端Rest请求
 *
 * @author JR
 */
public class ErrorController {
    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
    private static final AsciiString CONNECTION = new AsciiString("Connection");
    private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

    /**
     * 处理前端请求
     *
     * @param request
     * @return
     */
    @Ignore
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request, String errMessage) {
        this.response(ctx, request, Result.ERROR().msg(errMessage));
    }

    /**
     * 响应HTTP的请求
     *
     * @param ctx
     * @param req
     * @param result
     */
    private void response(ChannelHandlerContext ctx, FullHttpRequest req, Result result) {
        boolean keepAlive = HttpUtil.isKeepAlive(req);
        byte[] jsonByteByte = result.toJson().getBytes();

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(jsonByteByte));
        response.headers().set(CONTENT_TYPE, "application/json;charset=utf-8");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Access-Control-Allow-Headers", "*");

        /* HTTP/1.1 持久化相关 */
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }
}
