package com.theweirdscience.demo.model;

import java.net.SocketAddress;

public class Player {
    public String name;
    public SocketAddress remoteAddress;

    public Player(String name, SocketAddress remoteAddress) {
        this.name = name;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String toString() {
        return name + "," + remoteAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;

        if (name != null ? !name.equals(player.name) : player.name != null) return false;
        return remoteAddress != null ? remoteAddress.equals(player.remoteAddress) : player.remoteAddress == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (remoteAddress != null ? remoteAddress.hashCode() : 0);
        return result;
    }
}
