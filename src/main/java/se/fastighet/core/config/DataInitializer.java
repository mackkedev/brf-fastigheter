package se.fastighet.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.fastighet.core.entity.User;
import se.fastighet.core.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createTestUsers();
        }
    }

    private void createTestUsers() {
        User admin = User.builder()
                .email("admin@test.se")
                .name("Test Admin")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Created test admin user: admin@test.se");

        User resident = User.builder()
                .email("user@test.se")
                .name("Test User")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.RESIDENT)
                .build();
        userRepository.save(resident);
        log.info("Created test resident user: user@test.se");
    }
}
