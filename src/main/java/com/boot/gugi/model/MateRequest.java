package com.boot.gugi.model;

import com.boot.gugi.base.Enum.ApplicationStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "mate_request")
public class MateRequest {

    @Id
    @GeneratedValue(generator = "request_uuid")
    @GenericGenerator(name="request_uuid", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID requestId;

    @ManyToOne
    @JoinColumn(name = "mate_id")
    private MatePost matePost;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Enumerated(EnumType.STRING)
    private ApplicationStatusEnum status;

    @CreatedDate
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    public Integer getDaysSinceWritten() {
        return (int) ChronoUnit.DAYS.between(appliedAt.toLocalDate(), LocalDateTime.now().toLocalDate());
    }
}