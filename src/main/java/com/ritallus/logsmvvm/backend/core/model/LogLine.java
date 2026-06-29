package com.ritallus.logsmvvm.backend.core.model;

import java.time.LocalDateTime;

public record LogLine(
        String id,
        LocalDateTime timestamp,
        String level,
        String thread,
        String logger,
        String messageId,
        String message
) {}
