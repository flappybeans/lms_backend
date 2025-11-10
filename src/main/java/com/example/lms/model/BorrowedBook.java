package com.example.lms.model;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "borrowed_books")
public class BorrowedBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String author;
    private String year;
    private String isbn;
    private String status;
    private String coverImage;

    private String borrowerName;
    private String borrowerAddress;
    private String borrowerContact;

    private String transactionId;
    private String claimExpiryDate;
    private String isClaimed;
    private String remarks;

    private String duration;
    private String dueDate;
    private String borrowDate;
}