package com.dmme.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "plan")
public class Plan {

    @Id
    private String code;              // FREE | PRO | AGENCY

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int priceMinor;           // paise

    @Column(nullable = false)
    private String currency = "INR";

    private Integer dmLimit;          // null => unlimited
    private Integer contactLimit;     // null => unlimited
    private String razorpayPlanId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> features;

    @Column(nullable = false)
    private int sortOrder = 0;
}
