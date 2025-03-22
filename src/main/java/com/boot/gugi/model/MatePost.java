package com.boot.gugi.model;

import com.boot.gugi.base.Enum.AgeRangeEnum;
import com.boot.gugi.base.Enum.GenderEnum;
import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "mate_posts")
public class MatePost {

    @Id
    @GeneratedValue(generator = "mate_uuid")
    @GenericGenerator(name="mate_uuid", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID mateId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String title;

    @Column(nullable=false)
    private String content;

    @Column(nullable=false)
    private String contact;

    @Column(nullable=false)
    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    @Column(nullable=false)
    @Enumerated(EnumType.STRING)
    private AgeRangeEnum age;

    @Column(nullable=false)
    private LocalDate gameDate;

    @Column(nullable=false)
    private TeamEnum homeTeam;

    @Min(value = 2, message = "Member must be at least 2")
    @Max(value = 6, message = "Member must be at most 6")
    @Column(nullable = false)
    private Integer member;

    @Builder.Default
    private Integer confirmedMembers = 1;

    @Column(nullable=false)
    private StadiumEnum gameStadium;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable=false)
    private boolean expired = false;

    @OneToMany(mappedBy = "matePost", cascade = CascadeType.ALL)
    private List<MateRequest> mateRequestList;

    public Integer getDaysSinceWritten() {
        return (int) ChronoUnit.DAYS.between(updatedAt.toLocalDate(), LocalDateTime.now().toLocalDate());
    }

    public Integer getDaysUntilGame() {
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), gameDate);
    }
}