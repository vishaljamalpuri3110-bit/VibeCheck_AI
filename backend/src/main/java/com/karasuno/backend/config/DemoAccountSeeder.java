package com.karasuno.backend.config;

import com.karasuno.backend.entity.User;
import com.karasuno.backend.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoAccountSeeder {

    private static final Logger log =
            LoggerFactory.getLogger(DemoAccountSeeder.class);

    @Bean
    @ConditionalOnProperty(
        prefix = "app.demo-accounts",
        name = "enabled",
        havingValue = "true"
    )
    public CommandLineRunner seedDemoAccounts(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            @Value("${app.demo-accounts.creator-password:Creator@123}")
            String creatorPassword,

            @Value("${app.demo-accounts.manager-password:Manager@123}")
            String managerPassword,

            @Value("${app.demo-accounts.admin-password:Admin@123}")
            String adminPassword
    ) {
        return args -> {
            createAccount(
                userRepository,
                passwordEncoder,
                "creator",
                "creator@vibecheck.ai",
                creatorPassword,
                "ROLE_CONTENT_CREATOR"
            );

            createAccount(
                userRepository,
                passwordEncoder,
                "manager",
                "manager@vibecheck.ai",
                managerPassword,
                "ROLE_MANAGER"
            );

            createAccount(
                userRepository,
                passwordEncoder,
                "admin",
                "admin@vibecheck.ai",
                adminPassword,
                "ROLE_ADMIN"
            );

            log.info("Demo account initialization completed.");
        };
    }

    private void createAccount(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String email,
            String rawPassword,
            String role
    ) {

        // Never overwrite an existing username or its credentials.
        if (userRepository.existsByUsername(username)) {
            log.info("Demo account '{}' already exists. Skipping.", username);
            return;
        }

        // Avoid unique-email conflicts with an unrelated existing account.
        if (userRepository.existsByEmail(email)) {
            log.warn(
                "Cannot create demo account '{}': email '{}' is already in use.",
                username,
                email
            );
            return;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roles(role)
                .build();

        userRepository.save(user);

        log.info("Created demo account '{}' with role '{}'.", username, role);
    }
}