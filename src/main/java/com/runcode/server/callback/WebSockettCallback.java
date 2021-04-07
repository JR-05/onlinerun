package com.runcode.server.callback;

import com.github.dockerjava.api.model.Frame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * 接收到docker信息的回调
 *
 * @author JR
 */
@Slf4j
public class WebSockettCallback extends ResultCallback {

    /**
     * websocket客户端的channel
     */
    private ChannelHandlerContext ctx;

    public WebSockettCallback(ChannelHandlerContext ctx) {
        this.ctx = ctx;
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
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(msg));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 若当前指令执行完成，则发送最后一条消息
     */
    @Override
    public void onComplete() {
        ctx.channel().writeAndFlush(new TextWebSocketFrame("程序运行结束，总耗费时间："));
        super.onComplete();
    }
}
