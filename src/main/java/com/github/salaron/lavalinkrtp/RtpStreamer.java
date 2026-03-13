package com.github.salaron.lavalinkrtp;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import dev.arbjerg.lavalink.api.IPlayer;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

public final class RtpStreamer {
    private static final Logger log = LoggerFactory.getLogger(RtpStreamer.class);

    @Nullable
    private RtpOptions options;

    @Nullable
    private ScheduledExecutorService executor;

    @Nullable
    private ScheduledFuture<?> senderTask;

    private final ByteBuffer buffer;
    private final ByteBuffer frameBuffer;
    private final MutableAudioFrame frame;
    private final IPlayer player;

    private DatagramSocket socket;
    private DatagramPacket packet;
    private int seq;
    private int timestamp;

    public RtpStreamer(IPlayer player) {
        this.player = player;

        buffer = ByteBuffer.allocate(12 + StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize());
        frameBuffer = buffer.slice(12, StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize());

        frame = new MutableAudioFrame();
        frame.setBuffer(frameBuffer);
    }

    @Nullable
    public RtpOptions getOptions() {
        return options;
    }

    public synchronized void start(RtpOptions options) throws UnknownHostException, SocketException {
        log.info("Starting RTP streamer ({}:{}, {}, {})",
                options.getHost(), options.getPort(), options.getSsrc(), options.getPayloadType());

        this.options = options;

        var address = InetAddress.getByName(options.getHost());
        var port = options.getPort();
        var payloadType = options.getPayloadType();
        var ssrc = options.getSsrc();

        socket = new DatagramSocket();
        packet = new DatagramPacket(new byte[0], 0, address, port);

        seq = ThreadLocalRandom.current().nextInt() & 0xffff;
        timestamp = 0;

        executor = Executors.newSingleThreadScheduledExecutor(r ->
                new Thread(r, "rtp-streamer"));

        senderTask = executor.scheduleAtFixedRate(
                () -> sendFrame(payloadType, ssrc),
                0,
                20,
                TimeUnit.MILLISECONDS
        );
    }

    public synchronized void stop() {
        if (options == null) {
            return;
        }

        log.info("Stopping RTP streamer");

        options = null;

        if (senderTask != null) {
            senderTask.cancel(false);
        }

        if (executor != null) {
            executor.shutdown();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void sendFrame(byte payloadType, int ssrc) {
        try {
            var audioPlayer = player.getAudioPlayer();
            if (!audioPlayer.provide(frame)) {
                return;
            }

            buffer.clear();

            buffer.put((byte) 0x80);
            buffer.put((byte) (payloadType & 0x7f));
            buffer.putShort((short) seq);
            buffer.putInt(timestamp);
            buffer.putInt(ssrc);

            buffer.position(buffer.position() + frameBuffer.position());
            buffer.flip();

            packet.setData(buffer.array(), 0, buffer.limit());
            socket.send(packet);

            timestamp += 960;
            seq = (seq + 1) & 0xffff;
        } catch (Exception e) {
            log.error("RTP send failed", e);
        }
    }
}
