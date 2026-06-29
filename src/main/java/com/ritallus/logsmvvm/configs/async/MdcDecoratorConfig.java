package com.ritallus.logsmvvm.configs.async;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * Configuración centralizada para la propagación del contexto de diagnóstico mapeado (MDC).
 *
 * <p>En aplicaciones concurrentes de Spring Boot, los identificadores de rastreo (como {@code correlationId})
 * se almacenan en el {@link MDC} utilizando {@link ThreadLocal}. Al delegar tareas a hilos secundarios,
 * ya sea mediante la ejecución de métodos asíncronos ({@link org.springframework.scheduling.annotation.Async})
 * o procesamiento en hilos virtuales, dicho contexto se pierde por defecto.
 * </p>
 *
 * <p>Esta clase expone un {@link TaskDecorator} reutilizable que intercepta
 * la creación de tareas en los ejecutores ({@link org.springframework.core.task.AsyncTaskExecutor}), clonando
 * el mapa de contexto del hilo padre (HTTP thread) e inyectándolo de manera segura en el hilo hijo
 * antes de su ejecución. Al finalizar la tarea, garantiza la limpieza del contexto en el hilo secundario
 * para evitar fugas de memoria o contaminación cruzada de logs en hilos reutilizables.
 * </p>
 *
 * @see TaskDecorator
 * @see MDC
 * @since 1.0.0
 */
@Configuration
public class MdcDecoratorConfig {

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
