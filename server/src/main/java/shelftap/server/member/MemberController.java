package shelftap.server.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Staff-only (role check is in SecurityConfig). */
@RestController
@RequestMapping("/api/members")
class MemberController {
    record RegisterRequest(@NotBlank @Size(max = 100) String fullName,
                           @NotBlank @Email @Size(max = 254) String email,
                           @NotNull @Past LocalDate dateOfBirth,
                           @NotBlank @Pattern(regexp = "\\d{12}") String cccd,
                           @NotBlank @Pattern(regexp = "\\+?\\d{8,15}") String phone,
                           @NotNull Member.Type memberType,
                           @NotBlank @Size(min = 10, max = 128) String initialPassword) {
    }

    record Registered(UUID memberId) {
    }

    private final MemberService members;

    MemberController(MemberService members) {
        this.members = members;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Registered register(@Valid @RequestBody RegisterRequest r) {
        return new Registered(members.register(r.fullName(), r.email(), r.dateOfBirth(), r.cccd(), r.phone(),
                r.memberType(), r.initialPassword()));
    }
}
