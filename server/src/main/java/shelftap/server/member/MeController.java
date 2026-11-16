package shelftap.server.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shelftap.server.auth.AuthService;
import shelftap.server.loan.Fine;
import shelftap.server.loan.FineRepository;
import shelftap.server.loan.LoanViews;
import shelftap.server.security.JwtService.Caller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Everything a signed-in member reads about themselves. The member is always the token's subject, never a path or body id. */
@RestController
@RequestMapping("/api/me")
class MeController {
    record Me(UUID memberId, String fullName, String email, String memberType, String status, boolean historyEnabled,
              boolean mustChangePassword) {
        static Me of(Member m) {
            return new Me(m.id, m.fullName, m.email, m.memberType.name(), m.status.name(), m.historyEnabled, m.mustChangePassword);
        }
    }

    record PasswordRequest(@NotBlank @Size(max = 128) String currentPassword, @NotBlank @Size(min = 10, max = 128) String newPassword) {
    }

    record PrivacyRequest(@NotNull Boolean historyEnabled) {
    }

    record FineDto(UUID fineId, UUID loanId, int amountVnd, Instant paidAt) {
    }

    private final MemberService members;
    private final AuthService auth;
    private final LoanViews views;
    private final FineRepository fines;

    MeController(MemberService members, AuthService auth, LoanViews views, FineRepository fines) {
        this.members = members;
        this.auth = auth;
        this.views = views;
        this.fines = fines;
    }

    @GetMapping
    Me me(@AuthenticationPrincipal Jwt jwt) {
        return Me.of(members.get(Caller.from(jwt).id()));
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordRequest request) {
        Caller caller = Caller.from(jwt);
        auth.changePassword(caller.id(), caller.familyId(), request.currentPassword(), request.newPassword());
    }

    @PutMapping("/privacy")
    Me privacy(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PrivacyRequest request) {
        return Me.of(members.setHistoryEnabled(Caller.from(jwt).requirePasswordLogin().id(), request.historyEnabled()));
    }

    @GetMapping("/loans")
    List<LoanViews.LoanDto> loans(@AuthenticationPrincipal Jwt jwt) {
        return views.current(Caller.from(jwt).id());
    }

    @GetMapping("/history")
    List<LoanViews.HistoryItem> history(@AuthenticationPrincipal Jwt jwt) {
        UUID memberId = Caller.from(jwt).requirePasswordLogin().id();
        return members.get(memberId).historyEnabled ? views.history(memberId) : List.of();
    }

    @GetMapping("/reservations")
    List<LoanViews.ReservationDto> reservations(@AuthenticationPrincipal Jwt jwt) {
        return views.reservations(Caller.from(jwt).id());
    }

    @GetMapping("/fines")
    List<FineDto> fines(@AuthenticationPrincipal Jwt jwt) {
        return fines.findByMemberIdOrderByAssessedAtDesc(Caller.from(jwt).id()).stream()
                .map((Fine f) -> new FineDto(f.id, f.loanId, f.amountVnd, f.paidAt)).toList();
    }
}
