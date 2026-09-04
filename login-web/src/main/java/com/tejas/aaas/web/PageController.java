package com.tejas.aaas.web;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
/** Renders the public login screen without exposing provider configuration. */
@Controller
public class PageController {
    private final Environment environment;
    public PageController(Environment environment) { this.environment = environment; }
    @GetMapping({"/", "/login"})
    public String login(Model model) {
        model.addAttribute("live", environment.matchesProfiles("live"));
        return "login";
    }
}
