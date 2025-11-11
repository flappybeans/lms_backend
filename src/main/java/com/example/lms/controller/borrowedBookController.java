package com.example.lms.controller;

import com.example.lms.model.Book;
import com.example.lms.model.BorrowedBook;
import com.example.lms.repo.bookRepo;
import com.example.lms.repo.borrowedBookRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        return borrowedBookRepo.findAll();
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



    @GetMapping("/getborrowedBook/{id}")
    public BorrowedBook getBookById(@PathVariable Long id) {
        return borrowedBookRepo.findById(id).orElse(null);
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

    @PostMapping("/returnBook")
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
        bookToReturn.setQueueNumber(0);
        borrowedBookRepo.save(bookToReturn);

        // 4️⃣ Find the next borrower in queue (lowest queueNumber + "Waiting" status)
        BorrowedBook nextBorrower = sameBooks.stream()
                .filter(b -> "Waiting".equalsIgnoreCase(b.getStatus()))
                .sorted(Comparator.comparingInt(BorrowedBook::getQueueNumber))
                .findFirst()
                .orElse(null);

        if (nextBorrower != null) {
            // 5️⃣ Update next borrower to Borrowed
            nextBorrower.setStatus("Borrowed");
            borrowedBookRepo.save(nextBorrower);

            // 6️⃣ Update the Book entity
            Book bookEntity = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);
            if (bookEntity != null) {
                bookEntity.setIsAvailable("false");
                bookEntity.setCurrentBorrowerId(nextBorrower.getTransactionId());
                bookRepo.save(bookEntity);
            }

            return ResponseEntity.ok("Book returned successfully. Next borrower is now set.");
        } else {
            // 7️⃣ No one waiting → make book available again
            Book bookEntity = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);
            if (bookEntity != null) {
                bookEntity.setIsAvailable("true");
                bookEntity.setCurrentBorrowerId(null);
                bookRepo.save(bookEntity);
            }

            return ResponseEntity.ok("Book returned successfully. No one in queue, book now available.");
        }
    }




//    @PutMapping("/update/{id}")
//    public BorrowedBook updateBook(@PathVariable Long id, @RequestBody BorrowedBook borrowedBook) {
//        BorrowedBook existingBook = borrowedBookRepo.findById(id).orElse(null);
//        if (existingBook != null) {
//            existingBook.setTitle(borrowedBook.getTitle());
//            existingBook.setAuthor(borrowedBook.getAuthor());
//            existingBook.setYear(borrowedBook.getYear());
//            existingBook.setIsbn(borrowedBook.getIsbn());
//            existingBook.setStatus(borrowedBook.getStatus());
//            existingBook.setCoverImage(borrowedBook.getCoverImage());
//            existingBook.setBorrowerName(borrowedBook.getBorrowerName());
//            existingBook.setBorrowerAddress(borrowedBook.getBorrowerAddress());
//            existingBook.setBorrowerContact(borrowedBook.getBorrowerContact());
//            existingBook.setTransactionId(borrowedBook.getTransactionId());
//            existingBook.setClaimExpiryDate(borrowedBook.getClaimExpiryDate());
//            existingBook.setIsClaimed(borrowedBook.getIsClaimed());
//            existingBook.setRemarks(borrowedBook.getRemarks());
//            existingBook.setDueDate(borrowedBook.getDueDate());
//            existingBook.setBorrowDate(borrowedBook.getBorrowDate());
//            return borrowedBookRepo.save(existingBook);
//        }
//        return null;
//    }

}
