package com.ritallus.logsmvvm.backend.core.dto;

import com.ritallus.logsmvvm.backend.core.model.LogLine;

public record LogRecordDto(LogLine logLine, String rawLine) {
}
