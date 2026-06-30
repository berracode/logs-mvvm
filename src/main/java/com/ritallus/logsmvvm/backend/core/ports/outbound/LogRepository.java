package com.ritallus.logsmvvm.backend.core.ports.outbound;

import java.util.List;

import com.ritallus.logsmvvm.backend.core.model.LogLine;

public interface LogRepository {
    void save(LogLine log, String rawLine);

    List<LogLine> searchByMessageId(String messageId);

    List<LogLine> searchByContent(String query);
}
