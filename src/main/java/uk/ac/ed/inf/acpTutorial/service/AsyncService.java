package uk.ac.ed.inf.acpTutorial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import uk.ac.ed.inf.acpTutorial.dto.Drone;
import uk.ac.ed.inf.acpTutorial.entity.DroneEntity;
import uk.ac.ed.inf.acpTutorial.mapper.DroneMapper;
import uk.ac.ed.inf.acpTutorial.repository.DroneRepository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service

// the magic annotation to enable async
@EnableAsync

@RequiredArgsConstructor
public class AsyncService {

    @Async
    public CompletableFuture<String> asyncMethod(){
        log.info("Async method init at: " + LocalDateTime.now());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("Error sleeping", e);
        }
        log.info("Async method terminateda at: " + LocalDateTime.now());
        return CompletableFuture.completedFuture("Done for " + UUID.randomUUID());
    }
}
