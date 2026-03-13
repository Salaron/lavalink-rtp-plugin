package com.github.salaron.lavalinkrtp;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RtpOptions {
    private String host;
    private int port;
    private int ssrc;
    private byte payloadType;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getSsrc() {
        return ssrc;
    }

    public void setSsrc(int ssrc) {
        this.ssrc = ssrc;
    }

    public byte getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(byte payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof RtpOptions other))
            return false;

        return Objects.equals(other.host, host) &&
                Objects.equals(other.port, port) &&
                Objects.equals(other.ssrc, ssrc) &&
                Objects.equals(other.payloadType, payloadType);
    }
}