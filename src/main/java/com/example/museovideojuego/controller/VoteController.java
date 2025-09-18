package com.example.museovideojuego.controller;

import com.example.museovideojuego.config.ConsoleConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class VoteController {

    private final ConsoleConfig consoleConfig;
    private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

    @Value("${max.votes}")
    private int maxVotes;

    public VoteController(ConsoleConfig consoleConfig) {
        this.consoleConfig = consoleConfig;
    }

    @GetMapping("/vote")
    public String showVotePage(Model model) {
        logger.info("Accessing /vote endpoint"); // Log to confirm method execution
        List<Console> consoles = consoleConfig.getConsoles().stream()
                .map(c -> new Console(c.getName(), c.getImage())) // Correctly map image to imageUrl
                .collect(Collectors.toList());
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
        return "vote";
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
