package com.pro.mybooklist.web;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.UserRepository;

@CrossOrigin(origins = "*")
@RestController
public class RestPublicController {
	@Autowired
	private BookRepository repository;

	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private UserRepository urepository;

	@Autowired
	private BacketRepository barepository;

	@Autowired
	private BacketBookRepository bbrepository;
	
	@Autowired
	private OrderRepository orepository;
	
	@Autowired
	private JavaMailSender mailSender;
	
	@GetMapping("/books")
	public @ResponseBody List<Book> getBooks() {
		return (List<Book>) repository.findAll();
	}
	
	@GetMapping("/categories")
	public @ResponseBody List<Category> getCategories() {
		return (List<Category>) crepository.findAll();
	}
	
	@GetMapping("/books/{id}")
	public @ResponseBody Optional<Book> getBookById(@PathVariable("id") Long bookId) {
		return repository.findById(bookId);
	}
	
	@GetMapping("/booksbycategory")
	public @ResponseBody List<Book> getBooksByCategory(@RequestBody Category category) {
		return repository.findByCategory(category);
	}
}
