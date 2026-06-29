package com.ritallus.logsmvvm.configs.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

@Configuration
@RequiredArgsConstructor
public class AsyncConfig {

    private final TaskDecorator mdcTaskDecorator;

    @Bean(name = "backendLogExecutor")
    public ExecutorService backendLogExecutor() {
        // Usamos un SingleThreadExecutor estándar con Virtual Threads de Java 21
        // usando la nueva API de hilos virtuales de la plataforma.
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name("backend-vt-", 0)
                        .factory()
        );
    }
}
