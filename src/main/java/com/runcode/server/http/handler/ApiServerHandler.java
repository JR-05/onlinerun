package com.runcode.server.http.handler;

import com.runcode.entities.Result;
import com.runcode.server.http.controller.CodeRunController;
import com.runcode.server.http.controller.ErrorController;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;


/**
 * @author JR
 */
public class ApiServerHandler extends ChannelInboundHandlerAdapter {

    private CodeRunController codeRunController = new CodeRunController();
    private ErrorController errorController = new ErrorController();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            //客户端的请求对象
            FullHttpRequest req = (FullHttpRequest) msg;

            //获取客户端的URL
            String uri = req.uri();

            //根据不同的请求API做不同的处理(路由分发)
            try {
                switch (uri) {
                    case "/run":
                        codeRunController.handle(ctx, req);
                        break;
                    default:
                        errorController.handle(ctx, req, "请求路径出错！");
                }
            } catch (Exception e) {
                errorController.handle(ctx, req, e.getMessage());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
