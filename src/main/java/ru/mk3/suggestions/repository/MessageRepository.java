package ru.mk3.suggestions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mk3.suggestions.model.AnonymousMessage;

@Repository
public interface MessageRepository extends JpaRepository<AnonymousMessage, Integer> {
}