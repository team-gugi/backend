package com.boot.gugi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "teams_rank")
public class TeamRank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String team;

    @Column(nullable=false)
    private Integer teamRank;

    @Column(nullable=false)
    private Integer game;

    @Column(nullable=false)
    private Integer win;

    @Column(nullable=false)
    private Integer lose;

    @Column(nullable=false)
    private Integer draw;

    @Column(nullable=false, precision = 4, scale = 3)
    private BigDecimal winningRate;

    @Column(nullable=false, precision = 4, scale = 1)
    private BigDecimal difference;

    @Column(unique = true)
    private String rankKey;
}