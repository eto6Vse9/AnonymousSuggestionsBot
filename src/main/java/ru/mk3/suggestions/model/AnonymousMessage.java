package ru.mk3.suggestions.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class AnonymousMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private boolean published = false;

}
