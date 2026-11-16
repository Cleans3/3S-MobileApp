package shelftap.server.card;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shelftap.server.auth.TokenPair;
import shelftap.server.security.JwtService.Caller;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
class CardController {
    record Nonce(String nonce) {
    }

    record AuthRequest(@NotBlank @Size(max = 64) String nonce, @NotBlank @Size(max = 400) String signature, @NotNull UUID deviceId) {
    }

    record IssueRequest(@NotNull UUID memberId) {
    }

    record KeyRequest(@NotBlank @Size(max = 400) String modulus, @NotBlank @Size(max = 16) String exponent) {
    }

    record StatusRequest(@NotNull Card.Status status) {
    }

    private final CardService cards;

    CardController(CardService cards) {
        this.cards = cards;
    }

    @PostMapping("/{cardId}/challenge")
    Nonce challenge(@PathVariable String cardId) {
        return new Nonce(cards.challenge(cardId));
    }

    @PostMapping("/{cardId}/auth")
    TokenPair auth(@PathVariable String cardId, @Valid @RequestBody AuthRequest request) {
        return cards.authenticate(cardId, request.nonce(), request.signature(), request.deviceId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CardService.CardIssue issue(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IssueRequest request) {
        return cards.issue(Caller.from(jwt).id(), request.memberId());
    }

    @PutMapping("/{cardId}/public-key")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void registerKey(@AuthenticationPrincipal Jwt jwt, @PathVariable String cardId, @Valid @RequestBody KeyRequest request) {
        cards.registerKey(Caller.from(jwt).id(), cardId, request.modulus(), request.exponent());
    }

    @PutMapping("/{cardId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable String cardId, @Valid @RequestBody StatusRequest request) {
        cards.setStatus(Caller.from(jwt).id(), cardId, request.status());
    }
}
