package com.example.lms.controller;

import com.example.lms.model.Book;
import com.example.lms.model.BorrowedBook;

import com.example.lms.repo.bookRepo;
import com.example.lms.repo.borrowedBookRepo;
import org.hibernate.annotations.processing.Find;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;

@RestController
@RequestMapping("/borrowedBooks")
@CrossOrigin(origins = "*")
public class borrowedBookController {

    @Autowired
    private borrowedBookRepo borrowedBookRepo;


    @Autowired
    private bookRepo bookRepo;

    public borrowedBookController(borrowedBookRepo borrowedBookRepo, bookRepo bookRepo) {
        this.borrowedBookRepo = borrowedBookRepo;
        this.bookRepo = bookRepo;

    }

    @GetMapping("/get")
    public List<BorrowedBook> getAllBooks() {
        return borrowedBookRepo.findByStatusNotOrderByQueueNumberAsc("Returned");
    }

    @GetMapping("/queue/{isbn}")
    public List<BorrowedBook> getQueueByIsbn(@PathVariable String isbn) {
        return borrowedBookRepo.findByIsbnAndStatusNotOrderByQueueNumberAsc(isbn, "Returned");
    }

    @GetMapping("/dueToday")
    public List<BorrowedBook> getDueToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();       // 2025-11-12T00:00:00
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);  // 2025-11-12T23:59:59.999999999

        return borrowedBookRepo.findByDueDateBetweenOrderByDueDateAsc(startOfDay, endOfDay);
    }

    @GetMapping("/claimExpiry")
    public List<BorrowedBook> getClaimExpiry() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();       // 2025-11-12T00:00:00
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);  // 2025-11-12T23:59:59.999999999
// go fort push
        return borrowedBookRepo.findByStatusAndClaimExpiryDateBetweenOrderByClaimExpiryDateAsc("Reserved – Pick Up",startOfDay, endOfDay);
    }


    @PostMapping("/add")
    public ResponseEntity<BorrowedBook> addBook(@RequestBody BorrowedBook borrowedBook) {
        // 1️⃣ Find the book by ISBN
        Book book = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);

        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 2️⃣ If available, mark as borrowed
        if ("true".equalsIgnoreCase(book.getIsAvailable())) {
            book.setIsAvailable("false");
            book.setCurrentBorrowerId(borrowedBook.getTransactionId());
            bookRepo.save(book);

            borrowedBook.setStatus("Borrowed");
            borrowedBook.setQueueNumber(1);
            LocalDateTime dateTime = LocalDateTime.now();
            borrowedBook.setBorrowDate(dateTime);
            borrowedBook.setDueDate(dateTime.plusDays(parseInt(borrowedBook.getDuration())));
        }
        // 3️⃣ If not available, put in waiting queue
        else {
            List<BorrowedBook> sameBooks = borrowedBookRepo.findByIsbn(borrowedBook.getIsbn());

            // Get the highest queue number so far
            int maxQueue = sameBooks.stream()
                    .mapToInt(b -> b.getQueueNumber() == null ? 0 : b.getQueueNumber())
                    .max()
                    .orElse(0);

            borrowedBook.setQueueNumber(maxQueue + 1);
            borrowedBook.setStatus("Waiting");
        }

        // 4️⃣ Save new borrowed record
        BorrowedBook saved = borrowedBookRepo.save(borrowedBook);

        return ResponseEntity.ok(saved);
    }




    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        Optional<BorrowedBook> optionalBook = borrowedBookRepo.findById(id);

        if (optionalBook.isPresent()) {
            borrowedBookRepo.delete(optionalBook.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }



    @PutMapping("/returnBook")
    public ResponseEntity<String> returnBook(@RequestBody BorrowedBook borrowedBook) {
        // 1️⃣ Find all borrow records with the same ISBN
        List<BorrowedBook> sameBooks = borrowedBookRepo.findByIsbn(borrowedBook.getIsbn());

        if (sameBooks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 2️⃣ Find the book being returned (the one with status = "Borrowed")
        BorrowedBook bookToReturn = sameBooks.stream()
                .filter(b -> "Borrowed".equalsIgnoreCase(b.getStatus()))
                .findFirst()
                .orElse(null);

        if (bookToReturn == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No borrowed book found for that ISBN to return.");
        }

        // 3️⃣ Mark it as returned
        bookToReturn.setStatus("Returned");
        LocalDateTime dateTime = LocalDateTime.now();
        bookToReturn.setReturnDate(dateTime);
        bookToReturn.setQueueNumber(0);
        borrowedBookRepo.save(bookToReturn);




        // 4️⃣ Find the next borrower in queue (lowest queueNumber + "Waiting" status)
        BorrowedBook nextBorrower = sameBooks.stream()
                .filter(b -> "Waiting".equalsIgnoreCase(b.getStatus()))
                .sorted(Comparator.comparingInt(BorrowedBook::getQueueNumber))
                .findFirst()
                .orElse(null);

        if (nextBorrower != null) {
            // 5️⃣ Update next borrower to For pickup
            nextBorrower.setStatus("Reserved – Pick Up");
            LocalDateTime expTime = LocalDateTime.now();
            nextBorrower.setClaimExpiryDate(expTime.plusDays(2));
            borrowedBookRepo.save(nextBorrower);

            // 6️⃣ Update the Book entity
            Book bookEntity = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);
            if (bookEntity != null) {
                bookEntity.setIsAvailable("false");
                bookEntity.setCurrentBorrowerId(nextBorrower.getTransactionId());
                bookEntity.setBorrowedTimes(bookEntity.getBorrowedTimes() + 1);
                bookRepo.save(bookEntity);
            }

            return ResponseEntity.ok("Book returned successfully. Next borrower is now set.");
        } else {
            // 7️⃣ No one waiting → make book available again
            Book bookEntity = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);
            if (bookEntity != null) {
                bookEntity.setIsAvailable("true");
                bookEntity.setCurrentBorrowerId(null);
                bookEntity.setBorrowedTimes(bookEntity.getBorrowedTimes() + 1);
                bookRepo.save(bookEntity);
            }

            return ResponseEntity.ok("Book returned successfully. No one in queue, book now available.");
        }
    }

    @PutMapping("/pickUp/{transactionId}")
    public ResponseEntity<String> pickUpBook(@PathVariable String transactionId) {
        LocalDateTime dateTime = LocalDateTime.now();
        BorrowedBook book = borrowedBookRepo.findByTransactionId(transactionId).orElse(null);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            int duration = parseInt(book.getDuration());
            book.setStatus("Borrowed");
            book.setBorrowDate(dateTime);
            book.setDueDate(dateTime.plusDays(duration));
            borrowedBookRepo.save(book);
        }

        return ResponseEntity.ok("Book Pickup successfully.");
    }

    @GetMapping("/getReturned")
    public List<BorrowedBook> getReturnedBooks() {
        return borrowedBookRepo.findByStatusOrderByReturnDateDesc("Returned");
    }



}
