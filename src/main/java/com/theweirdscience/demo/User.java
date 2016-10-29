package com.theweirdscience.demo;

import java.net.SocketAddress;

public class User {
    public String name;
    public SocketAddress remoteAddress;

    public User(String name, SocketAddress remoteAddress) {
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

        User user = (User) o;

        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        return remoteAddress != null ? remoteAddress.equals(user.remoteAddress) : user.remoteAddress == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (remoteAddress != null ? remoteAddress.hashCode() : 0);
        return result;
    }
}
