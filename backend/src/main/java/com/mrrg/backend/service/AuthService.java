package com.mrrg.backend.service;

import com.mrrg.backend.dto.LoginRequest;
import com.mrrg.backend.dto.LoginResponse;
import com.mrrg.backend.dto.RegisterRequest;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            ActivationService activationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.activationService = activationService;
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

        // Check if account is activated
        if (!user.getEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not activated");
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
}
