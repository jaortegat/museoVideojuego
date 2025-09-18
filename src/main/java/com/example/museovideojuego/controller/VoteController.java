package com.example.museovideojuego.controller;

import com.example.museovideojuego.config.ConsoleConfig;
import com.example.museovideojuego.model.ConsoleVote;
import com.example.museovideojuego.model.PlayerScore;
import com.example.museovideojuego.repository.ConsoleVoteRepository;
import com.example.museovideojuego.repository.PlayerScoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
// ...existing code...
import java.util.ArrayList;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VoteController {

    private final ConsoleConfig consoleConfig;
    private final ConsoleVoteRepository consoleVoteRepository;
    private final PlayerScoreRepository playerScoreRepository;
    private final com.example.museovideojuego.service.VoteSseService voteSseService;
    private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

    @Value("${max.votes}")
    private int maxVotes;

    @Value("${videogames:}")
    private String videogamesProp;

    public VoteController(ConsoleConfig consoleConfig,
                          ConsoleVoteRepository consoleVoteRepository,
                          PlayerScoreRepository playerScoreRepository,
                          com.example.museovideojuego.service.VoteSseService voteSseService) {
        this.consoleConfig = consoleConfig;
        this.consoleVoteRepository = consoleVoteRepository;
        this.playerScoreRepository = playerScoreRepository;
        this.voteSseService = voteSseService;
    }

    @GetMapping("/vote")
    public String showVotePage(Model model) {
        logger.info("Accessing /vote endpoint"); // Log to confirm method execution
    List<Console> consoles = consoleConfig.getConsoles().stream()
        .map(c -> new Console(c.getName(), c.getImage())) // Correctly map image to imageUrl
        .toList();
        logger.debug("Loaded consoles: {}", consoles);
        logger.info("Consoles list content: {}", consoles); // Log the content of consoles
        if (consoles == null || consoles.isEmpty()) {
            logger.warn("No consoles available to display");
        } else {
            for (Console console : consoles) {
                logger.info("Console to display: name={}, imageUrl={}", console.getName(), console.getImageUrl());
            }
        }
        // Ensure DB has entries for each console and load current vote counts
        if (consoles != null) {
            for (Console c : consoles) {
            String name = c.getName();
            consoleVoteRepository.findByConsoleName(name).orElseGet(() -> {
                ConsoleVote cv = new ConsoleVote(name);
                return consoleVoteRepository.save(cv);
            });
            }
        }

        // Load votes from DB and attach counts to consoles via a simple map
        List<ConsoleVote> votes = consoleVoteRepository.findAll();
        // Map console name to votes for display (we keep Console objects simple)
        model.addAttribute("consoles", consoles);
        model.addAttribute("consoleVotes", votes);
        model.addAttribute("maxVotes", maxVotes); // Pass maxVotes to the frontend
        // Parse videogames CSV from properties and add to model
        List<String> videogames = List.of();
        if (videogamesProp != null && !videogamesProp.trim().isEmpty()) {
            videogames = List.of(videogamesProp.split("\s*,\s*"));
        }
        model.addAttribute("videogames", videogames);
        return "vote";
    }

    @PostMapping("/vote")
    public String handleVoteSubmission(@RequestParam(name = "selectedConsoles", required = false) String selected,
                                       RedirectAttributes redirectAttributes) {
        if (selected != null && !selected.isBlank()) {
            String[] parts = selected.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                consoleVoteRepository.findByConsoleName(trimmed).ifPresentOrElse(cv -> {
                    cv.increment();
                    ConsoleVote saved = consoleVoteRepository.save(cv);
                    logger.info("Vote recorded for console='{}' newCount={}", saved.getConsoleName(), saved.getVotes());
                    voteSseService.broadcastVote(saved.getConsoleName(), saved.getVotes());
                }, () -> {
                    ConsoleVote cv = new ConsoleVote(trimmed);
                    cv.increment();
                    ConsoleVote saved = consoleVoteRepository.save(cv);
                    logger.info("Vote recorded for new console='{}' newCount={}", saved.getConsoleName(), saved.getVotes());
                    voteSseService.broadcastVote(saved.getConsoleName(), saved.getVotes());
                });
            }
        }
        redirectAttributes.addFlashAttribute("message", "Votos guardados");
        return "redirect:/results";
    }

    // New JSON endpoint to submit votes without redirecting
    @PostMapping(path = "/api/vote", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> apiVote(@RequestBody java.util.Map<String,Object> body) {
        Object sel = body.get("selectedConsoles");
        if (sel instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> list = (java.util.List<String>) sel;
            for (String raw : list) {
                final String trimmed = raw == null ? "" : raw.trim();
                consoleVoteRepository.findByConsoleName(trimmed).ifPresentOrElse(cv -> {
                    cv.increment();
                    ConsoleVote saved = consoleVoteRepository.save(cv);
                    logger.info("Vote recorded for console='{}' newCount={}", saved.getConsoleName(), saved.getVotes());
                    voteSseService.broadcastVote(saved.getConsoleName(), saved.getVotes());
                }, () -> {
                    ConsoleVote cv = new ConsoleVote(trimmed);
                    cv.increment();
                    ConsoleVote saved = consoleVoteRepository.save(cv);
                    logger.info("Vote recorded for new console='{}' newCount={}", saved.getConsoleName(), saved.getVotes());
                    voteSseService.broadcastVote(saved.getConsoleName(), saved.getVotes());
                });
            }
        }
        return ResponseEntity.ok(java.util.Map.of("status","ok"));
    }

    @GetMapping(path = "/api/results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> apiResults() {
        java.util.List<ConsoleVote> votes = consoleVoteRepository.findAll();
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        for (ConsoleVote cv : votes) {
            if (cv.getVotes() > 0) { // only expose consoles with votes
                out.add(java.util.Map.of("console", cv.getConsoleName(), "votes", cv.getVotes()));
            }
        }
        return ResponseEntity.ok(java.util.Map.of("results", out));
    }

    @GetMapping("/results-stream")
    @ResponseBody
    public SseEmitter resultsStream() {
        return voteSseService.subscribe();
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        // Parse videogames CSV from properties and add to model so we can pair names with scores
        List<String> videogames = List.of();
        if (videogamesProp != null && !videogamesProp.trim().isEmpty()) {
            videogames = List.of(videogamesProp.split("\s*,\s*"));
        }
        model.addAttribute("videogames", videogames);

        // Aggregated console votes for chart
        List<ConsoleVote> votes = consoleVoteRepository.findAll();
        java.util.List<String> consoleNames = new java.util.ArrayList<>();
        java.util.List<Long> consoleCounts = new java.util.ArrayList<>();
        for (ConsoleVote cv : votes) {
            // Only include consoles that have at least one vote
            if (cv.getVotes() > 0) {
                consoleNames.add(cv.getConsoleName());
                consoleCounts.add(cv.getVotes());
            }
        }
        model.addAttribute("consoleNames", consoleNames);
        model.addAttribute("consoleCounts", consoleCounts);

        // Leaderboards per videogame
        java.util.Map<String, java.util.List<PlayerScore>> leaderboards = new java.util.LinkedHashMap<>();
        for (String game : videogames) {
            java.util.List<PlayerScore> top = playerScoreRepository.findByVideogameOrderByScoreDesc(game);
            // limit to top 10
            if (top.size() > 10) {
                top = top.subList(0, 10);
            }
            leaderboards.put(game, top);
        }
        model.addAttribute("leaderboards", leaderboards);
        return "results";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }

    @PostMapping("/side-action")
    public String handleSideAction(@RequestParam(name = "playerName", required = false) String playerName,
                                   @RequestParam MultiValueMap<String, String> paramMap,
                                   RedirectAttributes redirectAttributes) {
        // Collect scores submitted as scores[0], scores[1], etc.
        List<Integer> scores = new ArrayList<>();
        // Find keys that start with 'scores'
        List<String> keys = paramMap.keySet().stream().filter(k -> k.startsWith("scores")).sorted().toList();
        for (String key : keys) {
            List<String> values = paramMap.get(key);
            if (values != null && !values.isEmpty()) {
                try {
                    scores.add(Integer.parseInt(values.get(0)));
                } catch (NumberFormatException e) {
                    scores.add(null);
                }
            } else {
                scores.add(null);
            }
        }

        logger.info("Received side-action from player='{}' scores={}", playerName, scores);

        // Persist player scores
        List<String> videogames = List.of();
        if (videogamesProp != null && !videogamesProp.trim().isEmpty()) {
            videogames = List.of(videogamesProp.split("\\s*,\\s*"));
        }
        for (int i = 0; i < scores.size(); i++) {
            Integer s = scores.get(i);
            if (s != null && i < videogames.size()) {
                PlayerScore ps = new PlayerScore(playerName == null ? "" : playerName, videogames.get(i), s);
                playerScoreRepository.save(ps);
            }
        }

        // Flash attributes for immediate feedback
        redirectAttributes.addFlashAttribute("playerName", playerName);
        redirectAttributes.addFlashAttribute("playerScores", scores);

        return "redirect:/results";
    }

    // Admin endpoint to reset the database (clears votes and player scores)
    @PostMapping(path = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> resetAll() {
        logger.warn("/reset called - clearing ConsoleVote and PlayerScore tables");
        // delete all player scores
        playerScoreRepository.deleteAll();
        // find all consoles and delete votes
        java.util.List<ConsoleVote> existing = consoleVoteRepository.findAll();
        // delete all console vote rows
        consoleVoteRepository.deleteAll();
        // broadcast zero votes so SSE clients remove bars
        for (ConsoleVote cv : existing) {
            voteSseService.broadcastVote(cv.getConsoleName(), 0L);
        }
        return ResponseEntity.ok(java.util.Map.of("status", "reset"));
    }

    public static class Console {
        private String name;
        private String imageUrl;

        public Console(String name, String imageUrl) {
            this.name = name;
            this.imageUrl = imageUrl;
        }

        public String getName() {
            return name;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }
}
