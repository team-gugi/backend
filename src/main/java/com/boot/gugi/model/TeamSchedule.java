package com.boot.gugi.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "teams_schedule")
public class TeamSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String date;

    @Column(nullable=false)
    private String specificDate;

    @Column(nullable=false)
    private String homeTeam;

    @Column(nullable=false)
    private String awayTeam;

    @Column(nullable=false)
    private String homeImg;

    @Column(nullable=false)
    private String awayImg;

    @Column(nullable=false)
    private Integer homeScore;

    @Column(nullable=false)
    private Integer awayScore;

    @Column(nullable=false)
    private String gameTime;

    @Column(nullable=false)
    private String stadium;

    @Column
    private String cancellationReason;
}
