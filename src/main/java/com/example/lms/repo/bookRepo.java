package com.example.lms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.lms.model.Book;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface bookRepo extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
}
