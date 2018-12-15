package util;

import java.util.Objects;

/**
 * A pair of IP address and port number
 */
public class AddressPortPair {

    private final String ip;
    private final int port;

    public AddressPortPair(final String ip, final int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number");
        }
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressPortPair that = (AddressPortPair) o;
        return port == that.port &&
                Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
