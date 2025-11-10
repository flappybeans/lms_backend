package com.example.lms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.lms.model.BorrowedBook;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface borrowedBookRepo extends JpaRepository<BorrowedBook, Long> {
}
