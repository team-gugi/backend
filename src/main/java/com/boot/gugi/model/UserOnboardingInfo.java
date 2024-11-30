package com.boot.gugi.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users_onboarding_infos")
public class UserOnboardingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", foreignKey = @ForeignKey(name = "users_onboarding_infos_fk_users_id"))
    private User user;

    @Column(nullable=false)
    private String nickName;

    @Column
    private String profileImg;

    @Column(nullable=false)
    private String introduction;

    @Column(nullable=false)
    private String team;
}
