package com.example.studentdashboard.seed;

import com.example.studentdashboard.config.SeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Thin entry point — the actual seeding logic lives in DemoDataSeedService
 * so it runs inside a real @Transactional boundary (a CommandLineRunner
 * calling a @Transactional method on itself would bypass the Spring proxy
 * and silently run non-transactionally).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SeedProperties seedProperties;
    private final DemoDataSeedService demoDataSeedService;

    @Override
    public void run(String... args) {
        if (!seedProperties.enabled()) {
            log.info("Demo data seeding is disabled (app.seed.enabled=false).");
            return;
        }
        demoDataSeedService.seedIfEmpty();
    }
}
