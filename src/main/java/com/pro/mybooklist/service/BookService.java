package com.pro.mybooklist.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.BacketInfo;
import com.pro.mybooklist.httpforms.BookUpdate;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.RawBookInfo;

@Service
public class BookService {
	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private CategoryRepository categoryRepository;

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

	// Method to get the list of IDs of the books in the current backet of the user
	// by user authentication:
	public List<Long> getIdsOfBooksInCurrentCart(Authentication authentication) {
		User user = commonService.checkAuthentication(authentication);
		Long userId = user.getId();
		commonService.findCurrentBacketOfUser(user);

		List<Long> idsOfBooksInCurrentCart = bookRepository.findIdsOfBooksInCurrentCart(userId);
		return idsOfBooksInCurrentCart;
	}

	// Method to get list of Books in Backet by backetId and backet password:
	public List<BookInCurrentCart> getBooksInBacketByIdAndPassword(BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		String password = backetInfo.getPassword();

		commonService.findBacketAndCheckIsPrivateAndCheckPassword(backetId, password);

		List<BookInCurrentCart> booksInBacket = bookRepository.findBooksInBacket(backetId);
		return booksInBacket;
	}

	// Method to get the list of books in current backet of the user by user id and
	// authentication
	public List<BookInCurrentCart> getCurrentCartByUserId(Long userId, Authentication authentication) {
		User user = commonService.checkAuthenticationAndAuthorize(authentication, userId);
		commonService.findCurrentBacketOfUser(user);

		List<BookInCurrentCart> booksInCurrentBacketOfUser = bookRepository.findBooksInCurrentBacketByUserid(userId);
		return booksInCurrentBacketOfUser;
	}

	// Method to get list of Books in order by orderId:
	public List<BookInCurrentCart> getBooksByOrderId(Long orderId) {
		commonService.findOrder(orderId);

		List<BookInCurrentCart> booksInOrder = bookRepository.findBooksInOrder(orderId);
		return booksInOrder;
	}

	// Method to update book:
	public ResponseEntity<?> updateBook(Long bookId, BookUpdate updatedBook) {
		Book book = commonService.findBook(bookId);
		String isbn = updatedBook.getIsbn();
		this.checkIsbn(isbn, book);
		this.updateBook(book, updatedBook);

		return new ResponseEntity<>("The book was updated successfully", HttpStatus.OK);
	}

	private void checkIsbn(String isbn, Book bookToUpdate) {
		Optional<Book> optionalBook = bookRepository.findByIsbn(isbn);
		if (optionalBook.isPresent()) {
			Book bookInDb = optionalBook.get();
			if (bookInDb.getId() != bookToUpdate.getId())
				throw new ResponseStatusException(HttpStatus.CONFLICT, "The duplicate ISBN value is not allowed");
		}

	}

	private void updateBook(Book bookToUpdate, BookUpdate updatedBook) {
		bookToUpdate.setTitle(updatedBook.getTitle());
		bookToUpdate.setAuthor(updatedBook.getAuthor());
		bookToUpdate.setIsbn(updatedBook.getIsbn());
		bookToUpdate.setBookYear(updatedBook.getBookYear());
		bookToUpdate.setPrice(updatedBook.getPrice());
		bookToUpdate.setUrl(updatedBook.getUrl());

		Category updatedCategory = this.findCategory(updatedBook.getCategoryId());
		bookToUpdate.setCategory(updatedCategory);
		bookRepository.save(bookToUpdate);
	}

	private Category findCategory(Long categoryId) {
		Optional<Category> optionalCategory = categoryRepository.findById(categoryId);

		if (!optionalCategory.isPresent())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The category wasn't found");

		Category category = optionalCategory.get();
		return category;
	}
}
