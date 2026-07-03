package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "usage_counter")
@IdClass(UsageCounter.Key.class)
public class UsageCounter {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    private String period;        // 'YYYY-MM'

    @Column(nullable = false)
    private int dmsSent = 0;

    public static class Key implements Serializable {
        private UUID userId;
        private String period;

        public Key() {}
        public Key(UUID userId, String period) {
            this.userId = userId;
            this.period = period;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(period, key.period);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, period);
        }
    }
}
