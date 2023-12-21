package com.pro.mybooklist.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.BacketInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.RawBookInfo;

@Service
public class BookService {
	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CommonService commonService;

	// Method to get books
	public List<Book> getBooks() {
		List<Book> books = (List<Book>) bookRepository.findAll();

		return books;
	}

	// Method to get a book by id
	public Optional<Book> getBookById(Long bookId) {
		Optional<Book> optionalBook = bookRepository.findById(bookId);

		return optionalBook;
	}

	// Method to get list of books by category:
	public List<Book> getBooksByCategory(Category category) {
		List<Book> booksInCategory = bookRepository.findByCategory(category);

		return booksInCategory;
	}

	// Method to get list of books that are top saled:
	public List<RawBookInfo> getTopSales() {
		List<RawBookInfo> booksTopSaled = bookRepository.findTopSales();

		return booksTopSaled;
	}

	// Method to get list of Ids of books by backet id
	public List<Long> getIdsOfBooksByBacketid(Long backetId) {
		commonService.findBacketAndCheckIsPrivate(backetId);

		List<Long> idsOfBooksInBacket = bookRepository.findIdsOfBooksByBacketid(backetId);

		return idsOfBooksInBacket;
	}

	// Method to get list of Books in Backet by backetId and backet password:
	public List<BookInCurrentCart> getBooksInBacketByIdAndPassword(BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();

		commonService.findBacketAndCheckIsPrivateAndCheckPassword(backetId, password);

		List<BookInCurrentCart> booksInBacket = bookRepository.findBooksInBacket(backetId);

		return booksInBacket;
	}

	// Method to get list of Books in order by orderId:
	public List<BookInCurrentCart> getBooksByOrderId(Long orderId) {
		commonService.findOrder(orderId);

		List<BookInCurrentCart> booksInOrder = bookRepository.findBooksInOrder(orderId);

		return booksInOrder;
	}
}
