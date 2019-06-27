package com.lansent.udp_demo;

public class NetWorkPacket {
    public long bucketTime;

    public long upPacket;
    public long downPacket;
    public long packets ;
    @Override
    public String toString() {
        return "NetWorkPackets{" +
                "bucketTime=" + bucketTime +
                ", packets=" + packets / 1024 +
                " KB, upPacket=" + upPacket / 1024 +
                " KB, downPacket=" + downPacket / 1024 +
                " KB}";
    }
}
