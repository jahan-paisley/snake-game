package com.theweirdscience.demo;

import com.google.common.collect.Maps;
import com.theweirdscience.demo.model.Game;
import com.theweirdscience.demo.model.Player;
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
    private static final Map<Player, Channel> users = Maps.newConcurrentMap();
    private static final List<Game> Games = Collections.synchronizedList(new ArrayList<Game>());
    private static final RandomNameGenerator rnd = new RandomNameGenerator(0);

    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            users.put(new Player(rnd.next(), ctx.channel().remoteAddress()), ctx.channel());
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
            String users1 = "users_updated," + users.entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().remoteAddress.equals(ch.remoteAddress()))
                    .map(entry -> String.format("{\"%S\": \"%S\"}", entry.getKey().name, entry.getKey().remoteAddress))
                    .collect(Collectors.joining(",", "[", "]"));
            ch.writeAndFlush(new TextWebSocketFrame(users1));
        }
    }

    public Optional<Channel> findChannelByRemoteAddress(String address) {
        return users.entrySet()
                .stream()
                .filter(x -> x.getKey().remoteAddress.toString().equals(address))
                .map(x -> x.getValue())
                .findFirst();
    }

    public String findName(SocketAddress remoteAddress) {
        return users.entrySet()
                .stream()
                .filter(x -> x.getKey().remoteAddress.equals(remoteAddress))
                .map(z -> z.getKey().name).findFirst().get();
    }

    public Player findUser(SocketAddress remoteAddress) {
        Optional<Player> first = users.entrySet()
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
        Optional<Channel> ch = findChannelByRemoteAddress(split[0]);
        try {
            if (ch.isPresent()) {
                String sender = findName(ctx.channel().remoteAddress());
                ch.get().writeAndFlush(new TextWebSocketFrame(sender + ":" + split[1]));
            } else
                ctx.channel().writeAndFlush(new TextWebSocketFrame("probably client has been disconnected"));
        } catch (Exception e) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("exception happened:" + e.getMessage()));
        } finally {
            msg.release();
        }
    }
}
