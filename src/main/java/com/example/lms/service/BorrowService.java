package com.example.lms.service;

import com.example.lms.model.Book;
import com.example.lms.model.BorrowedBook;
import com.example.lms.repo.bookRepo;
import com.example.lms.repo.borrowedBookRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class BorrowService {

    @Autowired
    borrowedBookRepo borrowedBookRepo;

    @Autowired
    bookRepo bookRepo;

    /**
     * CANCEL ANY BORROW REQUEST
     * (Borrowed, Reserved – Pick Up, Waiting)
     * Updates queue & book availability automatically.
     */
    public String cancelBorrow(BorrowedBook bookToCancel) {

        String isbn = bookToCancel.getIsbn();

        // 1️⃣ Cancel this borrow entry
        bookToCancel.setStatus("Cancelled");
        bookToCancel.setQueueNumber(0);
        borrowedBookRepo.save(bookToCancel);

        // 2️⃣ Get all records of this book
        List<BorrowedBook> sameBooks = borrowedBookRepo.findByIsbn(isbn);

        // 3️⃣ Find next borrower waiting
        BorrowedBook next = sameBooks.stream()
                .filter(b -> "Waiting".equalsIgnoreCase(b.getStatus()))
                .sorted(Comparator.comparingInt(BorrowedBook::getQueueNumber))
                .findFirst()
                .orElse(null);

        Book bookEntity = bookRepo.findByIsbn(isbn).orElse(null);

        if (bookEntity == null) {
            return "Cancelled, but book entity not found.";
        }

        if (next != null) {
            next.setStatus("Reserved – Pick Up");
            next.setClaimExpiryDate(LocalDateTime.now().plusDays(2));
            borrowedBookRepo.save(next);

            bookEntity.setIsAvailable("false");
            bookEntity.setCurrentBorrowerId(next.getTransactionId());
            bookRepo.save(bookEntity);

            return "Cancelled. Next borrower moved to pickup.";
        }

// If no waiting, book must be available
        bookEntity.setIsAvailable("true");
        bookEntity.setAvailableBooks(bookEntity.getAvailableBooks() + 1);
        bookEntity.setCurrentBorrowerId(null);

        bookRepo.save(bookEntity);

        return "Cancelled. No queue left — book is now available.";
    }

    /**
     * Mark overdue and unclaimed automatically.
     */
    public String checkDueBooks() {
        LocalDateTime now = LocalDateTime.now();

        List<BorrowedBook> activeBooks = borrowedBookRepo.findByStatusIn(
                List.of("Borrowed", "Reserved – Pick Up")
        );

        int overdueCount = 0;
        int unclaimedCount = 0;

        for (BorrowedBook book : activeBooks) {
            // Borrowed → Overdue check
            if ("Borrowed".equalsIgnoreCase(book.getStatus())) {
                if (book.getDueDate() != null && book.getDueDate().isBefore(now)) {
                    book.setStatus("Overdue");
                    book.setPenalty("200");
                    overdueCount++;
                }
            }

            // Pick-up → Unclaimed check
            if ("Reserved – Pick Up".equalsIgnoreCase(book.getStatus())) {
                if (book.getClaimExpiryDate() != null && book.getClaimExpiryDate().isBefore(now)) {
                    cancelBorrow(book);
                    unclaimedCount++;

                }
            }
        }

        borrowedBookRepo.saveAll(activeBooks);

        return overdueCount + " overdue, " + unclaimedCount + " unclaimed.";
    }

}
