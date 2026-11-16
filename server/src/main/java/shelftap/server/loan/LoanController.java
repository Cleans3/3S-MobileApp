package shelftap.server.loan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shelftap.server.security.JwtService.Caller;

import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
class LoanController {
    record CheckoutRequest(@NotBlank @Size(max = 16) String tagId) {
    }

    private final LoanService loans;
    private final LoanViews views;

    LoanController(LoanService loans, LoanViews views) {
        this.loans = loans;
        this.views = views;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LoanViews.LoanDto checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
        UUID loanId = loans.checkout(Caller.from(jwt).id(), request.tagId());
        return views.loan(loanId).orElseThrow();
    }

    @PostMapping("/{loanId}/renew")
    LoanViews.LoanDto renew(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID loanId) {
        return views.loan(loans.renew(Caller.from(jwt).id(), loanId)).orElseThrow();
    }
}
