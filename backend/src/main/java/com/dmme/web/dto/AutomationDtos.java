package com.dmme.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request/response payloads for the automation builder. */
public final class AutomationDtos {

    private AutomationDtos() {}

    public record KeywordDto(String keyword, String matchType) {}

    public record StepDto(
            int stepOrder,
            String stepType,          // TEXT | BUTTONS | QUESTION
            String body,
            List<Map<String, String>> buttons,
            String collectField,      // EMAIL | PHONE | NAME | CUSTOM
            String customKey
    ) {}

    /** Full create/update payload for an automation and its flow. */
    public record AutomationUpsert(
            String name,
            String type,              // COMMENT | STORY_REPLY | LIVE | DM
            String status,            // ACTIVE | PAUSED | DRAFT
            Long igAccountId,
            String igMediaId,
            boolean matchAny,
            String publicReply,
            boolean askFollowEnabled,
            String askFollowMessage,
            List<KeywordDto> keywords,
            List<StepDto> steps
    ) {}

    public record AutomationView(
            Long id,
            String name,
            String type,
            String status,
            Long igAccountId,
            String igMediaId,
            boolean matchAny,
            String publicReply,
            boolean askFollowEnabled,
            String askFollowMessage,
            List<KeywordDto> keywords,
            List<StepDto> steps,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
