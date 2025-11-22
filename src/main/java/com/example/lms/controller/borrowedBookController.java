package com.example.lms.controller;

import com.example.lms.model.Book;
import com.example.lms.model.BorrowedBook;

import com.example.lms.repo.bookRepo;
import com.example.lms.repo.borrowedBookRepo;
import com.example.lms.service.BorrowService;
import org.hibernate.annotations.processing.Find;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
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
    
    @Autowired
    private BorrowService borrowService;

    public borrowedBookController(borrowedBookRepo borrowedBookRepo, bookRepo bookRepo, BorrowService borrowService) {
        this.borrowedBookRepo = borrowedBookRepo;
        this.bookRepo = bookRepo;
        this.borrowService = borrowService;
    }

    @GetMapping("/get")
    public List<BorrowedBook> getAllBooks() {
        return borrowedBookRepo.findByStatusNotInOrderByQueueNumberAsc(
                List.of("Returned", "Cancelled", "Unclaimed")
        );
    }

    @PatchMapping("/checkDue")
    public ResponseEntity<String> checkDue() {
        borrowService.checkDueBooks();
        return ResponseEntity.ok("Due books checked.");
    }




    @GetMapping("/queue/{isbn}")
    public List<BorrowedBook> getQueueByIsbn(@PathVariable String isbn) {
        return borrowedBookRepo.findByIsbnAndStatusNotInOrderByQueueNumberAsc(
                isbn,
                List.of("Returned", "Cancelled", "Unclaimed")
        );
    }

    @GetMapping("/dueToday")
    public List<BorrowedBook> getDueToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();      // 00:00:00
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX); // 23:59:59.999999999

        return borrowedBookRepo.findByStatusNotInAndDueDateBetweenOrderByDueDateAsc(
                List.of("Returned", "Cancelled", "Unclaimed"),
                startOfDay,
                endOfDay
        );
    }

    @GetMapping("/claimExpiry")
    public List<BorrowedBook> getClaimExpiry() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();       // 2025-11-12T00:00:00
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);  // 2025-11-12T23:59:59.999999999

        return borrowedBookRepo.findByStatusAndClaimExpiryDateBetweenOrderByClaimExpiryDateAsc("Reserved – Pick Up",startOfDay, endOfDay);
    }


    @PostMapping("/add/{count}")
    public ResponseEntity<BorrowedBook> addBook(@PathVariable int count, @RequestBody BorrowedBook borrowedBook) {
        // 1️⃣ Find the book by ISBN
        Book book = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);

        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 2️⃣ If available, mark as borrowed
        if (book.getAvailableBooks() > 0) {
            if(book.getAvailableBooks() == 1){
                book.setIsAvailable("false");
            }
            book.setAvailableBooks(book.getAvailableBooks() - count);
            book.setCurrentBorrowerId(borrowedBook.getTransactionId());
            bookRepo.save(book);

            borrowedBook.setStatus("Borrowed");
            borrowedBook.setCount(count);
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

    @PutMapping("/payPenalty/{transactionId}")
    public ResponseEntity<String> payPenalty(@PathVariable String transactionId) {
        BorrowedBook book = borrowedBookRepo.findByTransactionId(transactionId).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        book.setPenalty("Paid");
        borrowedBookRepo.save(book);

        return ResponseEntity.ok("Penalty paid successfully");
    }

    @PutMapping("/returnBook/{transactionId}")
    public ResponseEntity<String> returnBook(@PathVariable String transactionId) {
        // 1️⃣ Find the borrow record
        Optional<BorrowedBook> bookOptional = borrowedBookRepo.findByTransactionId(transactionId);

        if (bookOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Transaction ID not found.");
        }
        
        BorrowedBook bookToReturn = bookOptional.get();

        if (bookToReturn.getStatus().equalsIgnoreCase("Overdue")
                && !"Paid".equals(bookToReturn.getPenalty())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Book overdue. Penalty not paid.");
        }


        // 2️⃣ Mark as returned
        bookToReturn.setStatus("Returned");
        bookToReturn.setReturnDate(LocalDateTime.now());
        bookToReturn.setQueueNumber(0);
        borrowedBookRepo.save(bookToReturn);

        // 3️⃣ Update book stock (+1)
        Book bookEntity = bookRepo.findByIsbn(bookToReturn.getIsbn()).orElse(null);

        if (bookEntity == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Book record missing.");
        }


        System.out.println(bookEntity.getAvailableBooks() + " - " + bookToReturn.getCount());
        bookEntity.setAvailableBooks(bookEntity.getAvailableBooks() + bookToReturn.getCount());
        bookEntity.setIsAvailable("true");
        bookRepo.save(bookEntity);

        // 4️⃣ Find next borrower in queue
        List<BorrowedBook> sameBooks = borrowedBookRepo.findByIsbn(bookToReturn.getIsbn());

        BorrowedBook nextBorrower = sameBooks.stream()
                .filter(b -> "Waiting".equalsIgnoreCase(b.getStatus()))
                .sorted(Comparator.comparingInt(BorrowedBook::getQueueNumber))
                .findFirst()
                .orElse(null);

        // 5️⃣ Process next in queue ONLY IF availableBooks > 0
        if (nextBorrower != null && bookEntity.getAvailableBooks() > 0) {

            nextBorrower.setStatus("Reserved – Pick Up");
            nextBorrower.setClaimExpiryDate(LocalDateTime.now().plusDays(2));
            borrowedBookRepo.save(nextBorrower);

            // Book becomes NOT available since this reservation consumes a copy
            bookEntity.setAvailableBooks(bookEntity.getAvailableBooks() - nextBorrower.getCount());
            bookRepo.save(bookEntity);

            return ResponseEntity.ok("Book returned successfully!");
        }

        return ResponseEntity.ok("Book returned successfully!");
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
    public List<BorrowedBook> getReturnedAndCancelledBooks() {
        return borrowedBookRepo.findByStatusInOrderByReturnDateDesc(
                List.of("Returned", "Cancelled", "Unclaimed")
        );
    }


    @PutMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestBody BorrowedBook request) {
        BorrowedBook book = borrowedBookRepo.findByTransactionId(request.getTransactionId())
                .orElse(null);

        if (book == null) {
            return ResponseEntity.badRequest().body("Transaction ID not found.");
        }

        return ResponseEntity.ok(borrowService.cancelBorrow(book));
    }


}
