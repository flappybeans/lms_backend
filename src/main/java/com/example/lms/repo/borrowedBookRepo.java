package com.example.lms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.lms.model.BorrowedBook;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface borrowedBookRepo extends JpaRepository<BorrowedBook, Long> {
    List<BorrowedBook> findByIsbn(String isbn);
    List<BorrowedBook> findByStatus(String status);
    List<BorrowedBook> findByStatusNotOrderByQueueNumberAsc(String status);

    Optional<BorrowedBook> findByTransactionId(String transactionId);
    List<BorrowedBook> findByStatusOrderByReturnDateDesc(String status);

    List<BorrowedBook> findByDueDateBetweenOrderByDueDateAsc(LocalDateTime start, LocalDateTime end);

    List<BorrowedBook> findByIsbnAndStatusNotOrderByQueueNumberAsc(String isbn, String status);
    List<BorrowedBook> findByStatusAndClaimExpiryDateBetweenOrderByClaimExpiryDateAsc(String status,LocalDateTime start, LocalDateTime end);


}
