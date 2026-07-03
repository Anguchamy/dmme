package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "trigger_keyword")
public class TriggerKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "automation_id", nullable = false)
    private Long automationId;

    @Column(nullable = false)
    private String keyword;

    /** EXACT | CONTAINS */
    @Column(nullable = false)
    private String matchType = "CONTAINS";
}
