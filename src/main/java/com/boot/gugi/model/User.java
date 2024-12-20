package com.boot.gugi.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(generator = "users_uuid")
    @GenericGenerator(name="users_uuid", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID userId;

    @Column(nullable=false)
    private String provider;

    @Column(nullable=false)
    private String providerId;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private String email;

    @Column(nullable=false)
    private Integer gender;

    @Column(nullable=false)
    private Integer age;

    @Builder.Default
    private BigDecimal winRate = BigDecimal.ZERO;

    @Builder.Default
    private Integer totalDiaryCount = 0;

    @Builder.Default
    private Integer totalWins = 0;
}