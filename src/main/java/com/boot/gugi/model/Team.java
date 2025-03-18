package com.boot.gugi.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "teams_info")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String teamCode;

    @Column(nullable=false)
    private String teamLogo;

    @Column(nullable=false)
    private String teamName;

    @Column(nullable=false)
    private String description;

    @Column(nullable=false)
    private String instagram;

    @Column(nullable=false)
    private String youtube;

    @Column(nullable=false)
    private String ticketShop;

    @Column(nullable=false)
    private String mdShop;
}