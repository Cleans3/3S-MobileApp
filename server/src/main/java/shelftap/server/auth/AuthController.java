package shelftap.server.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
class AuthController {
    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 128) String password,
                        @NotNull UUID deviceId) {
    }

    record RefreshRequest(@NotBlank @Size(max = 100) String refreshToken, @NotNull UUID deviceId) {
    }

    record LogoutRequest(@NotBlank @Size(max = 100) String refreshToken) {
    }

    record StaffLoginRequest(@NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 128) String password,
                             @NotNull UUID deviceId) {
    }

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/login")
    TokenPair login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password(), request.deviceId());
    }

    @PostMapping("/auth/refresh")
    TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return auth.refresh(request.refreshToken(), request.deviceId());
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody LogoutRequest request) {
        auth.logout(request.refreshToken());
    }

    @PostMapping("/staff/login")
    TokenPair staffLogin(@Valid @RequestBody StaffLoginRequest request) {
        return auth.staffLogin(request.username(), request.password(), request.deviceId());
    }
}
