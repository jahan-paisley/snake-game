package com.theweirdscience.demo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.util.HashMap;
import java.util.Map;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final ChannelGroup group;
    private static final Map<String, Channel> users = new HashMap<>();

    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            users.put("user" + ctx.channel().remoteAddress(), ctx.channel());
            group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined"));
            group.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        msg.retain();
        String text = msg.text();
        String[] split = text.split("\\|");
        Channel ch = users.get("user/" + split[0]);
        try {
            if (ch != null)
                ch.writeAndFlush(new TextWebSocketFrame("From:" + ctx.channel().remoteAddress().toString().substring(1) + ":" + split[1]));
            else
                ctx.channel().writeAndFlush(new TextWebSocketFrame("probably client has been disconnected"));
        } catch (Exception e) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("exception happened:" + e.getMessage()));
        }
    }
}
