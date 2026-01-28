package se.fastighet.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // t.ex. "VVS", "El", "Ventilation"

    private String description;

    private String icon; // För frontend-ikon, t.ex. "wrench", "bolt", "wind"
}
