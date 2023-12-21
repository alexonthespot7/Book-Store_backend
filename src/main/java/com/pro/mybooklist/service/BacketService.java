package com.pro.mybooklist.service;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.BacketInfo;
import com.pro.mybooklist.httpforms.BookQuantityInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookKey;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

@Service
public class BacketService {
	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private CommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(BacketService.class);

	// Method to get the total price of the backet by backetId and backet password:
	public TotalOfBacket getTotalByBacketId(BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();

		commonService.findBacketAndCheckIsPrivateAndCheckPassword(backetId, password);

		TotalOfBacket totalOfBacket = backetRepository.findTotalOfBacket(backetId);

		return totalOfBacket;
	}

	// Method to create Backet with password and no user. The method returns the
	// backet Id and its password
	public BacketInfo createBacketNoAuthentication() {
		String password = RandomStringUtils.random(15);
		String hashedPassword = commonService.encodePassword(password);

		Long backetId = this.createBacket(hashedPassword);

		BacketInfo createdBacketInfo = new BacketInfo(backetId, password);

		return createdBacketInfo;
	}

	// Method to add backet of certain quantity to the backet by backetId and backet
	// password:
	public ResponseEntity<?> addBookToCartNoAuthentication(Long backetId,
			BookQuantityInfo bookQuantityAndBacketPassword) {
		Long bookId = bookQuantityAndBacketPassword.getBookid();
		int additionalQuantity = bookQuantityAndBacketPassword.getQuantity();
		String password = bookQuantityAndBacketPassword.getPassword();

		Backet backet = commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId, password);

		Book book = this.findBook(bookId);

		Optional<BacketBook> optionalBacketBook = getOptionalBacketBook(backetId, bookId);

		if (optionalBacketBook.isPresent()) {
			BacketBook backetBook = optionalBacketBook.get();

			this.addQuantityToBacketBook(backetBook, additionalQuantity);
		} else {
			this.createBacketBook(additionalQuantity, backet, book);
		}

		return new ResponseEntity<>("Book was added to cart successfully", HttpStatus.OK);
	}

	// Method to reduce the amount of book by bookid and backetInfo
	public ResponseEntity<?> reduceItemNoAuthentication(Long bookId, BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();

		commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId, password);

		this.findBook(bookId);

		BacketBook backetBook = this.findBacketBook(bookId, backetId);

		int quantity = backetBook.getQuantity();
		quantity = quantity - 1;

		if (quantity > 0) {
			this.setBookQuantityInCart(quantity, backetBook);

			return new ResponseEntity<>("The quantity of the book in the cart was reduced by one", HttpStatus.OK);
		} else {
			return this.deleteBookFromCart(backetBook);
		}
	}

	// Method to delete book from backet By bookid and backetInfo
	public ResponseEntity<?> deleteBookNoAuthentication(Long bookId, BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();

		commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId, password);

		this.findBook(bookId);

		BacketBook backetBook = this.findBacketBook(bookId, backetId);

		return this.deleteBookFromCart(backetBook);
	}

	private Long createBacket(String hashedPassword) {
		Backet backet = new Backet(hashedPassword);
		backetRepository.save(backet);
		Long backetId = backet.getBacketid();

		return backetId;
	}

	private Book findBook(Long bookId) {
		Optional<Book> optionalBook = bookRepository.findById(bookId);

		if (!optionalBook.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The book wasn't found by id");

		Book book = optionalBook.get();

		return book;
	}

	private BacketBook findBacketBook(Long bookId, Long backetId) {
		Optional<BacketBook> optionalBacketBook = getOptionalBacketBook(backetId, bookId);

		if (!optionalBacketBook.isPresent())
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The book is not in the backet");

		BacketBook backetBook = optionalBacketBook.get();

		return backetBook;
	}

	private Optional<BacketBook> getOptionalBacketBook(Long backetId, Long bookId) {
		BacketBookKey backetBookKey = new BacketBookKey(backetId, bookId);
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);

		return optionalBacketBook;
	}

	private void addQuantityToBacketBook(BacketBook backetBook, int additionalQuantity) {
		int currentQuantity = backetBook.getQuantity();
		int newQuantity = currentQuantity + additionalQuantity;
		
		this.setBookQuantityInCart(newQuantity, backetBook);
	}
	
	private void setBookQuantityInCart(int quantity, BacketBook backetBook) {
		backetBook.setQuantity(quantity);
		backetBookRepository.save(backetBook);
	}

	private void createBacketBook(int quantity, Backet backet, Book book) {
		BacketBook backetBook = new BacketBook(quantity, backet, book);

		backetBookRepository.save(backetBook);
	}

	private ResponseEntity<?> deleteBookFromCart(BacketBook backetBook) {
		backetBookRepository.delete(backetBook);

		return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
	}
}
