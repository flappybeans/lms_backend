package com.example.lms.repo;

import com.example.lms.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.lms.model.BorrowedBook;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface borrowedBookRepo extends JpaRepository<BorrowedBook, Long> {
    List<BorrowedBook> findByIsbn(String isbn);
    List<BorrowedBook> findByStatusIn(List<String> statuses);
    List<BorrowedBook> findByStatusNotInOrderByQueueNumberAsc(List<String> statuses);


    Optional<BorrowedBook> findByTransactionId(String transactionId);
    List<BorrowedBook> findByStatusInOrderByReturnDateDesc(List<String> statuses);

    List<BorrowedBook> findByStatusNotInAndDueDateBetweenOrderByDueDateAsc(List<String> statuses, LocalDateTime start, LocalDateTime end);

    List<BorrowedBook> findByIsbnAndStatusNotInOrderByQueueNumberAsc(String isbn, List<String> statuses);

    List<BorrowedBook> findByStatusAndClaimExpiryDateBetweenOrderByClaimExpiryDateAsc(String status,LocalDateTime start, LocalDateTime end);


}
