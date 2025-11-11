package com.example.lms.model;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String author;
    private String year;
    private String pages;
    private String description;
    private String genre;
    private String isAvailable;
    private String coverImage;
    private String isbn;
    private String borrowedTimes;
    private String location;
    private String currentBorrowerId;
}