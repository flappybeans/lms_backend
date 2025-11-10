package com.example.lms.controller;

import com.example.lms.model.Book;
import com.example.lms.model.BorrowedBook;
import com.example.lms.repo.bookRepo;
import com.example.lms.repo.borrowedBookRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public BorrowedBook addBook(@RequestBody BorrowedBook borrowedBook) {
        Book book = bookRepo.findByIsbn(borrowedBook.getIsbn()).orElse(null);

        if(book != null && book.getIsAvailable().equals("true")){
            book.setIsAvailable("false");
            bookRepo.save(book);
        }

        return borrowedBookRepo.save(borrowedBook);
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
