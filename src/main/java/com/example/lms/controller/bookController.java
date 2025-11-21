package com.example.lms.controller;

import com.example.lms.model.Book;
import com.example.lms.repo.bookRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*")
public class bookController {
    
    @Autowired
    private bookRepo bookRepo;

    public bookController(bookRepo bookRepo) {
        this.bookRepo = bookRepo;
    }

    @GetMapping("/get")
    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }

    @PostMapping("/add")
    public Book addBook(@RequestBody Book book) {
        book.setBorrowedTimes(0);
        book.setAvailableBooks(5);
        return bookRepo.save(book);
    }

    @GetMapping("/getbook/{isbn}")
    public ResponseEntity<Book> getBookByIsbn(@PathVariable String isbn) {
        return bookRepo.findByIsbn(isbn)
                .map(book -> ResponseEntity.ok(book))
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/update/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book book) {
        Book existingBook = bookRepo.findById(id).orElse(null);
        if (existingBook != null) {
            existingBook.setTitle(book.getTitle());
            existingBook.setAuthor(book.getAuthor());
            existingBook.setYear(book.getYear());
            existingBook.setIsAvailable(book.getIsAvailable());
            existingBook.setLocation(book.getLocation());
            existingBook.setDescription(book.getDescription());
            existingBook.setIsbn(book.getIsbn());
            existingBook.setGenre(book.getGenre());
            existingBook.setPages(book.getPages());
            return bookRepo.save(existingBook);
        }
        return null;
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteBorrowedBook(@PathVariable Long id) {
        Optional<Book> optionalBook = bookRepo.findById(id);

        if (optionalBook.isPresent()) {
            bookRepo.delete(optionalBook.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

}
