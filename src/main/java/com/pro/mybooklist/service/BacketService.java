package com.pro.mybooklist.service;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.BacketInfo;
import com.pro.mybooklist.httpforms.BookQuantityInfo;
import com.pro.mybooklist.httpforms.QuantityInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookKey;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.sqlforms.QuantityOfBacket;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

@Service
public class BacketService {
	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@Autowired
	private CommonService commonService;

	// Method to get the total price of the backet by backetId and backet password:
	public TotalOfBacket getTotalByBacketId(BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();
		commonService.findBacketAndCheckIsPrivateAndCheckPassword(backetId, password);

		TotalOfBacket totalOfBacket = backetRepository.findTotalOfBacket(backetId);
		return totalOfBacket;
	}

	// Method to get the total of current backet of user by authentication:
	public TotalOfBacket getCurrentCartTotal(Authentication authentication) {
		User user = commonService.checkAuthentication(authentication);
		Long userId = user.getId();
		commonService.findCurrentBacketOfUser(user);

		TotalOfBacket totalOfCurrentBacket = backetRepository.findTotalOfCurrentCart(userId);
		return totalOfCurrentBacket;
	}

	// Method to get the total amount of books in the current backet of the user
	// (returns interface with backetid and items fields):
	public QuantityOfBacket getCurrentCartQuantity(Authentication authentication) {
		User user = commonService.checkAuthentication(authentication);
		Long userId = user.getId();
		commonService.findCurrentBacketOfUser(user);

		QuantityOfBacket quantityOfCurrentBacket = backetRepository.findQuantityInCurrent(userId);
		return quantityOfCurrentBacket;
	}

	// Method to create Backet with password and no user. The method returns the
	// backet Id and its password
	public BacketInfo createBacketNoAuthentication() {
		String password = RandomStringUtils.randomAlphanumeric(15);
		String hashedPassword = commonService.encodePassword(password);
		Long backetId = this.createBacket(hashedPassword);

		BacketInfo createdBacketInfo = new BacketInfo(backetId, password);
		return createdBacketInfo;
	}

	private Long createBacket(String hashedPassword) {
		Backet backet = new Backet(hashedPassword);
		backetRepository.save(backet);
		Long backetId = backet.getBacketid();

		return backetId;
	}

	// Method to add the certain quantity of the book to the backet by backetId and
	// backet password:
	public ResponseEntity<?> addBookToCartNoAuthentication(Long backetId,
			BookQuantityInfo bookQuantityAndBacketPassword) {
		Long bookId = bookQuantityAndBacketPassword.getBookid();
		int additionalQuantity = bookQuantityAndBacketPassword.getQuantity();
		String password = bookQuantityAndBacketPassword.getPassword();

		Backet backet = commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId, password);

		return this.addQuantityOfBookToTheBacket(backet, bookId, additionalQuantity);
	}

	// Method to add the certain quantity of the book to the current backet of the
	// user:
	public ResponseEntity<?> addBookToCurrentBacket(Long bookId, QuantityInfo quantityInfo,
			Authentication authentication) {
		int additionalQuantity = quantityInfo.getQuantity();

		User user = commonService.checkAuthentication(authentication);
		Backet currentBacket = commonService.findCurrentBacketOfUser(user);

		return this.addQuantityOfBookToTheBacket(currentBacket, bookId, additionalQuantity);
	}

	private ResponseEntity<?> addQuantityOfBookToTheBacket(Backet backet, Long bookId, int additionalQuantity) {
		Long backetId = backet.getBacketid();
		Book book = commonService.findBook(bookId);
		Optional<BacketBook> optionalBacketBook = this.getOptionalBacketBook(backetId, bookId);

		if (optionalBacketBook.isPresent()) {
			BacketBook backetBook = optionalBacketBook.get();
			this.addQuantityToBacketBook(backetBook, additionalQuantity);
		} else {
			this.createBacketBook(additionalQuantity, backet, book);
		}

		return new ResponseEntity<>("Book was added to cart successfully", HttpStatus.OK);
	}

	private void addQuantityToBacketBook(BacketBook backetBook, int additionalQuantity) {
		int currentQuantity = backetBook.getQuantity();
		int newQuantity = currentQuantity + additionalQuantity;
		this.setBookQuantityInCart(newQuantity, backetBook);
	}

	private void createBacketBook(int quantity, Backet backet, Book book) {
		BacketBook backetBook = new BacketBook(quantity, backet, book);
		backetBookRepository.save(backetBook);
	}

	// Method to reduce the amount of book by bookid and backetInfo
	public ResponseEntity<?> reduceBookNoAuthentication(Long bookId, BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();

		Backet backet = commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId, password);

		return this.reduceQuantityOfBookInBacket(backet, bookId);
	}

	// Method to reduce the quantity of the book in the current backet of the
	// authenticated user:
	public ResponseEntity<?> reduceBookAuthenticated(Long bookId, Authentication authentication) {
		User user = commonService.checkAuthentication(authentication);
		Backet currentBacket = commonService.findCurrentBacketOfUser(user);

		return this.reduceQuantityOfBookInBacket(currentBacket, bookId);
	}

	private ResponseEntity<?> reduceQuantityOfBookInBacket(Backet backet, Long bookId) {
		Long backetId = backet.getBacketid();
		commonService.findBook(bookId);

		BacketBook backetBook = this.findBacketBook(bookId, backetId);
		int quantity = backetBook.getQuantity();
		quantity = quantity - 1;

		return this.reduceQuantityOfBookInBacket(quantity, backetBook);
	}

	private ResponseEntity<?> reduceQuantityOfBookInBacket(int quantity, BacketBook backetBook) {
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

		Backet backet = commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId, password);

		return this.deleteBookFromBacket(backet, bookId);
	}

	// Method to delete the book from the current backet of the authenticated user:
	public ResponseEntity<?> deleteBookFromCurrentBacket(Long bookId, Authentication authentication) {
		User user = commonService.checkAuthentication(authentication);
		Backet currentBacket = commonService.findCurrentBacketOfUser(user);

		return this.deleteBookFromBacket(currentBacket, bookId);
	}

	private ResponseEntity<?> deleteBookFromBacket(Backet backet, Long bookId) {
		Long backetId = backet.getBacketid();
		commonService.findBook(bookId);
		BacketBook backetBook = this.findBacketBook(bookId, backetId);

		return this.deleteBookFromCart(backetBook);
	}

	// Method to clear current backet of the authenticated user:
	public ResponseEntity<?> clearCurrentBacket(Long userId, Authentication authentication) {
		User user = commonService.checkAuthenticationAndAuthorize(authentication, userId);
		Backet currentBacket = commonService.findCurrentBacketOfUser(user);

		long deleted = backetBookRepository.deleteByBacket(currentBacket);
		return new ResponseEntity<>(deleted + " records were deleted from current cart", HttpStatus.OK);
	}

	// Method to find BacketBook:
	private BacketBook findBacketBook(Long bookId, Long backetId) {
		Optional<BacketBook> optionalBacketBook = this.getOptionalBacketBook(backetId, bookId);
		if (!optionalBacketBook.isPresent())
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The book is not in the backet");

		BacketBook backetBook = optionalBacketBook.get();
		return backetBook;
	}

	// Method to find optional backet book by backetId and bookId:
	private Optional<BacketBook> getOptionalBacketBook(Long backetId, Long bookId) {
		BacketBookKey backetBookKey = new BacketBookKey(backetId, bookId);

		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);
		return optionalBacketBook;
	}

	// Method to set new quantity for the book in the backet
	private void setBookQuantityInCart(int quantity, BacketBook backetBook) {
		backetBook.setQuantity(quantity);
		backetBookRepository.save(backetBook);
	}

	// Method to delete the book from the backet by deleting backetBook record:
	private ResponseEntity<?> deleteBookFromCart(BacketBook backetBook) {
		backetBookRepository.delete(backetBook);
		return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
	}
}
