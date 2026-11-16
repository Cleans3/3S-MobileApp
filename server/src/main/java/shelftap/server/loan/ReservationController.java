package shelftap.server.loan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shelftap.server.security.JwtService.Caller;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
class ReservationController {
    record ReserveRequest(@NotNull UUID titleId) {
    }

    private final ReservationService reservations;
    private final LoanViews views;

    ReservationController(ReservationService reservations, LoanViews views) {
        this.reservations = reservations;
        this.views = views;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LoanViews.ReservationDto reserve(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReserveRequest request) {
        return views.reservation(reservations.reserve(Caller.from(jwt).id(), request.titleId())).orElseThrow();
    }

    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reservationId) {
        reservations.cancel(Caller.from(jwt).id(), reservationId);
    }
}
