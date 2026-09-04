package com.tejas.aaas.web;
import com.tejas.aaas.identity.Triplet;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/** OIDC onboarding controller. ACE remains the only protected-access decision point. */
@Controller @Profile("live")
public class AccountController {
    private final AceClient ace;
    public AccountController(AceClient ace) { this.ace = ace; }
    @GetMapping("/account")
    public String account(@RegisteredOAuth2AuthorizedClient("cognito") OAuth2AuthorizedClient client, Model model) {
        try { model.addAttribute("status", ace.status(token(client)).get("status")); }
        catch (RestClientException e) { model.addAttribute("status", "unavailable"); }
        return "account";
    }
    @PostMapping("/registration")
    public String register(@RegisteredOAuth2AuthorizedClient("cognito") OAuth2AuthorizedClient client,
        @RequestParam String identityType, @RequestParam String identityValue,
        @RequestParam String dateType, @RequestParam String dateValue, @RequestParam String zip,
        RedirectAttributes redirect) {
        try {
            var triplet = new Triplet(identityType, identityValue, dateType, dateValue, zip);
            triplet.validate(); ace.register(token(client), triplet);
            redirect.addFlashAttribute("message", "Identity verification completed. You can request access below.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("message", e.getMessage());
        } catch (RestClientResponseException e) {
            redirect.addFlashAttribute("message", switch(e.getStatusCode().value()) {
                case 422 -> "We could not verify a unique identity. The new registration was rejected; sign out and contact support.";
                case 409 -> "This account needs additional review or MFA. Access remains restricted.";
                case 504 -> "Verification timed out. Please retry; this is not an identity rejection.";
                default -> "Verification is unavailable. Please retry later or sign in again.";
            });
        } catch (RestClientException e) { redirect.addFlashAttribute("message", "Verification is temporarily unavailable. Please try again."); }
        return "redirect:/account";
    }
    @GetMapping("/workspace")
    public String workspace(@RegisteredOAuth2AuthorizedClient("cognito") OAuth2AuthorizedClient client, Model model) {
        try { ace.authorize(token(client)); return "workspace"; }
        catch (RestClientException e) {
            model.addAttribute("status", "access_denied");
            model.addAttribute("message", "Access could not be confirmed. Complete verification or try again later.");
            return "account";
        }
    }
    private String token(OAuth2AuthorizedClient client) { return client.getAccessToken().getTokenValue(); }
}
