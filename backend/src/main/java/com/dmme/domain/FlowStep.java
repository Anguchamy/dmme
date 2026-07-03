package com.dmme.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** One ordered message in an automation's DM flow. */
@Getter
@Setter
@Entity
@Table(name = "flow_step")
public class FlowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "automation_id", nullable = false)
    private Long automationId;

    @Column(nullable = false)
    private int stepOrder;

    /** TEXT | BUTTONS | QUESTION */
    @Column(nullable = false)
    private String stepType = "TEXT";

    @Column(length = 2000)
    private String body;

    /** For BUTTONS: [{ "title": "...", "url": "..." }] */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, String>> buttons;

    /** For QUESTION: EMAIL | PHONE | NAME | CUSTOM */
    private String collectField;

    /** For QUESTION with collectField = CUSTOM */
    private String customKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
