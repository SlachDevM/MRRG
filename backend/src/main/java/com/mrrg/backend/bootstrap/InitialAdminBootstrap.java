package com.mrrg.backend.bootstrap;

import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.bootstrap.initial-admin.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InitialAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public InitialAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.initial-admin.email}") String email,
            @Value("${app.bootstrap.initial-admin.password}") String password,
            @Value("${app.bootstrap.initial-admin.name}") String name
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.debug("Initial admin bootstrap skipped: an ADMIN user already exists");
            return;
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn(
                    "Initial admin bootstrap skipped: INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD must be configured"
            );
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Initial admin bootstrap skipped: user {} already exists", normalizedEmail);
            return;
        }

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setName(name != null && !name.isBlank() ? name.trim() : "Administrator");
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);
        log.info("Initial administrator account created for {}", normalizedEmail);
    }
}
