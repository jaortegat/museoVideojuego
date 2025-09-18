package com.example.museovideojuego.controller;

import com.example.museovideojuego.config.ConsoleConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

    @Value("${max.votes}")
    private int maxVotes;

    @Value("${videogames:}")
    private String videogamesProp;

    public VoteController(ConsoleConfig consoleConfig) {
        this.consoleConfig = consoleConfig;
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
        model.addAttribute("consoles", consoles);
        model.addAttribute("maxVotes", maxVotes); // Pass maxVotes to the frontend
        // Parse videogames CSV from properties and add to model
        List<String> videogames = List.of();
        if (videogamesProp != null && !videogamesProp.trim().isEmpty()) {
            videogames = List.of(videogamesProp.split("\s*,\s*"));
        }
        model.addAttribute("videogames", videogames);
        return "vote";
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        // Parse videogames CSV from properties and add to model so we can pair names with scores
        List<String> videogames = List.of();
        if (videogamesProp != null && !videogamesProp.trim().isEmpty()) {
            videogames = List.of(videogamesProp.split("\s*,\s*"));
        }
        model.addAttribute("videogames", videogames);
        return "results";
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

        // For now, store a flash attribute to show on results page (simple example)
        redirectAttributes.addFlashAttribute("playerName", playerName);
        redirectAttributes.addFlashAttribute("playerScores", scores);

        return "redirect:/results";
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
