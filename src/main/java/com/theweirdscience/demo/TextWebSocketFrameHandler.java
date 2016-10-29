package com.theweirdscience.demo;

import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.kohsuke.randname.RandomNameGenerator;

import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final ChannelGroup group;
    private static final Map<User, Channel> users = Maps.newHashMap();
    private static final RandomNameGenerator rnd = new RandomNameGenerator(0);

    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ;

        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            users.put(new User(rnd.next(), ctx.channel().remoteAddress()), ctx.channel());
            group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel().remoteAddress() + " joined"));
            group.add(ctx.channel());
            sendUsersToAll();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Optional.ofNullable(findUser(ctx.channel().remoteAddress())).ifPresent(x -> users.remove(x));
        sendUsersToAll();
    }

    private void sendUsersToAll() {
        for (Channel ch : group) {
            String users1 = "users," + users.entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().remoteAddress.equals(ch.remoteAddress()))
                    .map(entry -> String.format("{\"%S\": \"%S\"}", entry.getKey().name, entry.getKey().remoteAddress))
                    .collect(Collectors.joining(",", "[", "]"));
            ch.writeAndFlush(new TextWebSocketFrame(users1));
        }
    }

    public Channel findChannelByRemoteAddress(String address) {
        return users.entrySet()
                .stream()
                .filter(x -> x.getKey().remoteAddress.toString().equals(address))
                .map(x -> x.getValue())
                .findFirst().get();
    }

    public String findName(SocketAddress remoteAddress) {
        return users.entrySet()
                .stream()
                .filter(x -> x.getKey().remoteAddress.equals(remoteAddress))
                .map(z -> z.getKey().name).findFirst().get();
    }

    public User findUser(SocketAddress remoteAddress) {
        Optional<User> first = users.entrySet()
                .stream()
                .filter(x -> x.getKey().remoteAddress.equals(remoteAddress))
                .map(z -> z.getKey())
                .findFirst();
        return first.isPresent() ? first.get() : null;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        msg.retain();
        String text = msg.text();
        String[] split = text.split("\\|");
        Channel ch = findChannelByRemoteAddress(split[0]);
        try {
            if (ch != null) {
                String sender = findName(ctx.channel().remoteAddress());
                ch.writeAndFlush(new TextWebSocketFrame(sender + ":" + split[1]));
            } else
                ctx.channel().writeAndFlush(new TextWebSocketFrame("probably client has been disconnected"));
        } catch (Exception e) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("exception happened:" + e.getMessage()));
        }
    }
}
