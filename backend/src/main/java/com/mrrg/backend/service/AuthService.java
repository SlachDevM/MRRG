package com.mrrg.backend.service;

import com.mrrg.backend.dto.LoginRequest;
import com.mrrg.backend.dto.LoginResponse;
import com.mrrg.backend.dto.RegisterRequest;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.model.UserStatus;
import com.mrrg.backend.repository.AccountActivationTokenRepository;
import com.mrrg.backend.repository.UserRepository;
import com.mrrg.backend.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ActivationService activationService;
    private final AccountActivationTokenRepository tokenRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            ActivationService activationService,
            AccountActivationTokenRepository tokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.activationService = activationService;
        this.tokenRepository = tokenRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Check user status and provide appropriate error messages
        if (!user.getEnabled()) {
            // Determine if user is pending activation or disabled
            UserStatus status = computeUserStatus(user);
            if (status == UserStatus.PENDING_ACTIVATION) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not activated");
            } else if (status == UserStatus.DISABLED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
            }
        }

        return buildLoginResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.EMPLOYEE);
        // Direct registration via public endpoint is now restricted to development profiles only
        user.setEnabled(false); // Must activate via email token in production

        return buildLoginResponse(userRepository.save(user));
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                token
        );
    }

    public LoginResponse activateAccount(String token, String password) {
        User activatedUser = activationService.activateAccount(token, password);
        return buildLoginResponse(activatedUser);
    }

    /**
     * Computes the user's status based on enabled flag and activation tokens.
     * Used to provide more detailed error messages during login.
     *
     * @param user the user to compute status for
     * @return UserStatus
     */
    private UserStatus computeUserStatus(User user) {
        if (user.getEnabled()) {
            return UserStatus.ACTIVE;
        }

        // Check if user has a valid (unused and not expired) activation token
        boolean hasPendingToken = tokenRepository.hasValidTokenByUserId(user.getId());

        return hasPendingToken ? UserStatus.PENDING_ACTIVATION : UserStatus.DISABLED;
    }
}
