package com.boot.gugi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "stadium_foods")
public class StadiumFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "food_id", nullable = false, unique = true)
    @JsonBackReference
    private Food food;

    @ManyToOne
    @JoinColumn(name = "stadium_code", nullable = false)
    @JsonBackReference
    private Stadium stadium;
}