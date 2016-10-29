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
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final ChannelGroup group;
    private static final Map<String, Channel> users = new HashMap<>();

    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        String chname = getRemoteAddress(ctx.channel());
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            users.put(chname, ctx.channel());
            group.writeAndFlush(new TextWebSocketFrame("Client " + chname + " joined"));
            group.add(ctx.channel());
            sendUsersToAll();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        users.remove(getRemoteAddress(ctx.channel()));
        sendUsersToAll();
    }

    private void sendUsersToAll() {
        for (Channel ch : group) {
            String users = "users," + TextWebSocketFrameHandler
                    .users
                    .keySet()
                    .stream()
                    .filter(name -> {
                        return !name.equals(getRemoteAddress(ch));
                    })
                    .collect(Collectors.joining(","));
            ch.writeAndFlush(new TextWebSocketFrame(users));
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        msg.retain();
        String text = msg.text();
        String[] split = text.split("\\|");
        Channel ch = users.get(split[0]);
        try {
            if (ch != null) {
                String chname = getRemoteAddress(ctx.channel());
                ch.writeAndFlush(new TextWebSocketFrame(split[1]));
            } else
                ctx.channel().writeAndFlush(new TextWebSocketFrame("probably client has been disconnected"));
        } catch (Exception e) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("exception happened:" + e.getMessage()));
        }
    }

    private String getRemoteAddress(Channel channel) {
        return channel.remoteAddress().toString().substring(1);
    }
}
