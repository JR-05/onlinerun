package com.runcode.server.callback;

import com.github.dockerjava.api.model.Frame;
import com.runcode.entities.Result;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 接收到docker信息的回调
 *
 * @author JR
 */
@Slf4j
public class HttpCallback extends ResultCallback {

    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
    private static final AsciiString CONNECTION = new AsciiString("Connection");
    private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

    // http客户端的channel
    private ChannelHandlerContext ctx;
    // httpRequest
    private FullHttpRequest req;
    // 发送内容
    private String content;

    public HttpCallback(ChannelHandlerContext ctx, FullHttpRequest req) {
        this.ctx = ctx;
        this.req = req;
    }

    /**
     * 接收到一条Docker响应后，返回给Websocket客户端
     *
     * @param frame
     */
    @Override
    public void onNext(Frame frame) {
        log.info("收到docker响应");
        if (frame != null) {
            String msg = new String(frame.getPayload());
            log.info("接受的docker响应数据：{}", msg);

            switch (frame.getStreamType()) {
                case STDOUT:
                case RAW:
                case STDERR:
                    this.content = msg;
                    break;
            }
        }
    }

    /**
     * 若当前指令执行完成，则发送最后一条消息
     */
    @Override
    public void onComplete() {
        if (isFinal()) {
            Result result = Result.OK()
                    .data("content", this.content);

            this.response(ctx, req, result);
        }
        super.onComplete();
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
