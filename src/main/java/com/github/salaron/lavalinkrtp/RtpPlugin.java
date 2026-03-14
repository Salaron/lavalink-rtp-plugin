package com.github.salaron.lavalinkrtp;

import dev.arbjerg.lavalink.api.IPlayer;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RestController
public class RtpPlugin extends PluginEventHandler {
    private final ConcurrentHashMap<StreamKey, RtpStreamer> streamers = new ConcurrentHashMap<>();

    @Override
    public void onNewPlayer(@NotNull ISocketContext context, @NotNull IPlayer player) {
        super.onNewPlayer(context, player);

        var key = new StreamKey(context.getSessionId(), player.getGuildId());
        var streamer = new RtpStreamer(player);
        streamers.put(key, streamer);
    }

    @Override
    public void onDestroyPlayer(@NotNull ISocketContext context, @NotNull IPlayer player) {
        super.onDestroyPlayer(context, player);

        var key = new StreamKey(context.getSessionId(), player.getGuildId());
        var streamer = streamers.get(key);
        if (streamer != null) {
            streamer.stop();
            streamers.remove(key);
        }
    }

    @PatchMapping("/v4/sessions/{sessionId}/players/{guildId}/rtp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRtpConnection(@PathVariable("sessionId") String sessionId, @PathVariable("guildId") Long guildId, @RequestBody RtpOptions options) {
        var key = new StreamKey(sessionId, guildId);
        var streamer = streamers.get(key);
        if (streamer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player not found");
        }

        if (options.equals(streamer.getOptions())) {
            return;
        }

        try {
            streamer.stop();
            streamer.start(options);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
