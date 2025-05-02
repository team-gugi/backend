package com.boot.gugi.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "stadiums")
public class Stadium {

    @Id
    private Integer stadiumCode;

    @Column(nullable=false)
    private String stadiumName;

    @Column(nullable=false)
    private String stadiumLocation;

    @Column(nullable=false)
    private String teamName;

    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("stadium-food")
    private final Set<StadiumFood> stadiumFoods = new HashSet<>();

    public Set<Food> getFoods() {
        return stadiumFoods.stream()
                .map(StadiumFood::getFood)
                .collect(Collectors.toSet());
    }
}