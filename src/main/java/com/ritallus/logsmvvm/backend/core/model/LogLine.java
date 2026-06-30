package com.ritallus.logsmvvm.backend.core.model;

import java.time.LocalDateTime;

public record LogLine(
        String id,
        LocalDateTime timestamp,          // Extraído del log string
        LocalDateTime internalTimestamp,  // Puesto por ti en la ingesta
        String level,
        String thread,
        String logger,
        String messageId,
        String message
) {
}