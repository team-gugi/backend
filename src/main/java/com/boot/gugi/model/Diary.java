package com.boot.gugi.model;

import com.boot.gugi.base.Enum.GameResultEnum;
import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "diary_posts")
public class Diary {

    @Id
    @GeneratedValue(generator = "diary_uuid")
    @GenericGenerator(name="diary_uuid", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID diaryId;

    @Column(nullable=false)
    private UUID userId;

    @Column(nullable=false)
    private LocalDate gameDate;

    @Column(nullable=false)
    private StadiumEnum gameStadium;

    @Column(nullable=false)
    private TeamEnum homeTeam;

    @Column(nullable=false)
    private TeamEnum awayTeam;

    @Column(nullable=false)
    private Integer homeScore;

    @Column(nullable=false)
    private Integer awayScore;

    @Column(nullable=false)
    private GameResultEnum gameResult;

    @Column(nullable=false)
    private String gameImg;

    @Column(nullable=false)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable=false)
    private LocalDateTime createdAt;

    public String getGameStadiumInKorean() {
        return gameStadium.toKorean();
    }
}