package com.example.museovideojuego.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class VoteSseService {
    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VoteSseService.class);

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // never timeout for dev
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcastVote(String consoleName, long votes) {
    String json = String.format("{\"console\":\"%s\",\"votes\":%d}", escapeJson(consoleName), votes);
    logger.info("Broadcasting vote event to {} emitters for console='{}' votes={}", emitters.size(), consoleName, votes);
    for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("vote")
                        .data(json, org.springframework.http.MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcastScore(String videogame, String playerName, int score) {
    String json = String.format("{\"videogame\":\"%s\",\"player\":\"%s\",\"score\":%d}",
        escapeJson(videogame), escapeJson(playerName), score);
    logger.info("Broadcasting score event to {} emitters for game='{}' player='{}' score={}", emitters.size(), videogame, playerName, score);
    for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("score")
                        .data(json, org.springframework.http.MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
