package com.boot.gugi.model;

import com.boot.gugi.base.Enum.MatchStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "match_requests")
public class MatchRequest {

    @Id
    @GeneratedValue(generator = "request_uuid")
    @GenericGenerator(name="request_uuid", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID requestId;

    @ManyToOne
    @JoinColumn(name = "mate_id")
    private MatePost matePost;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    private MatchStatusEnum status;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}