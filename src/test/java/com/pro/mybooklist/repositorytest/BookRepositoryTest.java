package com.pro.mybooklist.repositorytest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;
import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.RawBookInfo;

import jakarta.transaction.Transactional;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class BookRepositoryTest {
	@Autowired
	private UserRepository urepository;

	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private BacketRepository backetrepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@Autowired
	private OrderRepository orepository;

	@Autowired
	private BookRepository bookrepository;

	@BeforeAll
	public void resetUserRepo() {
		urepository.deleteAll();
		crepository.deleteAll();
		backetrepository.deleteAll();
		backetBookRepository.deleteAll();
		orepository.deleteAll();
		bookrepository.deleteAll();
	}

	// CRUD tests for the book repository
	// Create functionality
	@Test
	@Rollback
	public void testCreateBook() {
		Book newBook = this.createBook("Little Women", "Romance");
		assertThat(newBook.getId()).isNotNull();

		this.createBook("Little Women 2", "Thriller");
		List<Book> users = (List<Book>) bookrepository.findAll();
		assertThat(users).hasSize(2);
	}

	// Read functionalities tests
	@Test
	@Rollback
	public void testFindAllAndFindById() {
		List<Book> books = (List<Book>) bookrepository.findAll();
		assertThat(books).isEmpty();

		Optional<Book> optionalBook = bookrepository.findById(Long.valueOf(2));
		assertThat(optionalBook).isNotPresent();

		Book newBook1 = this.createBook("Little Women", "Romance");
		Long newBook1Id = newBook1.getId();
		this.createBook("Little Women 2", "Thriller");

		books = (List<Book>) bookrepository.findAll();
		assertThat(books).hasSize(2);

		optionalBook = bookrepository.findById(newBook1Id);
		assertThat(optionalBook).isPresent();
	}

	@Test
	@Rollback
	public void testFindByCategory() {
		Category categoryOther = this.createCategory("Other");
		List<Book> books = bookrepository.findByCategory(categoryOther);
		assertThat(books).isEmpty();

		this.createBook("Little Women", "Other");
		this.createBook("Little Women 2", "Other");

		books = bookrepository.findByCategory(categoryOther);
		assertThat(books).hasSize(2);
	}

	@Test
	@Rollback
	public void testFindByIsbn() {
		String title = "Little Women";
		String isbn = title + "isbn";
		Optional<Book> optionalBook = bookrepository.findByIsbn(isbn);
		assertThat(optionalBook).isNotPresent();

		this.createBook("Little Women", "Other");

		optionalBook = bookrepository.findByIsbn(isbn);
		assertThat(optionalBook).isPresent();
	}

	@Test
	@Rollback
	public void testFindTopSales() {
		List<RawBookInfo> topSalesBooks = bookrepository.findTopSales();
		assertThat(topSalesBooks).isEmpty();

		Book book1 = this.createBook("Little Women", "Other");
		this.createSale(2, book1);

		topSalesBooks = bookrepository.findTopSales();
		assertThat(topSalesBooks).hasSize(1);

		String topSaleBookTitle = "Little Women2";
		Book book2 = this.createBook(topSaleBookTitle, "Other");
		this.createSale(4, book2);

		topSalesBooks = bookrepository.findTopSales();
		assertThat(topSalesBooks).hasSize(2);
		assertThat(topSalesBooks.get(0).getTitle()).isEqualTo(topSaleBookTitle);
	}

	@Test
	@Rollback
	public void testFindBooksInBacket() {
		List<BookInCurrentCart> booksInBacket = bookrepository.findBooksInBacket(Long.valueOf(1));
		assertThat(booksInBacket).isEmpty();

		Backet backet = this.createBacketNoUser(true);
		Long backetId = backet.getBacketid();
		Book book1 = this.createBook("Little Women", "Other");
		this.createBacketBookCustomQuantity(2, book1, backet);

		booksInBacket = bookrepository.findBooksInBacket(backetId);
		assertThat(booksInBacket).hasSize(1);

		Book book2 = this.createBook("Little Women 2", "Other");
		this.createBacketBookCustomQuantity(2, book2, backet);

		booksInBacket = bookrepository.findBooksInBacket(backetId);
		assertThat(booksInBacket).hasSize(2);
	}

	@Test
	@Rollback
	public void testFindBooksInOrder() {
		List<BookInCurrentCart> booksInOrder = bookrepository.findBooksInOrder(Long.valueOf(1));
		assertThat(booksInOrder).isEmpty();

		String bookTitle = "Little Women";
		Book book1 = this.createBook(bookTitle, "Other");
		Order newOrder = this.createSale(2, book1);
		Long orderId = newOrder.getOrderid();

		booksInOrder = bookrepository.findBooksInOrder(orderId);
		assertThat(booksInOrder).hasSize(1);
		assertThat(booksInOrder.get(0).getTitle()).isEqualTo(bookTitle);
	}

	@Test
	@Rollback
	public void testFindIdsOfBooksByBacketid() {
		List<Long> idsOfBooks = bookrepository.findIdsOfBooksByBacketid(Long.valueOf(2));
		assertThat(idsOfBooks).isEmpty();

		Backet backet = this.createBacketNoUser(true);
		Long backetId = backet.getBacketid();
		Book book1 = this.createBook("Little Women", "Other");
		this.createBacketBookCustomQuantity(2, book1, backet);

		idsOfBooks = bookrepository.findIdsOfBooksByBacketid(backetId);
		assertThat(idsOfBooks).hasSize(1);

		Book book2 = this.createBook("Little Women 2", "Other");
		this.createBacketBookCustomQuantity(2, book2, backet);

		idsOfBooks = bookrepository.findIdsOfBooksByBacketid(backetId);
		assertThat(idsOfBooks).hasSize(2);
	}

	@Test
	@Rollback
	public void testFindBooksInCurrentBacketByUserid() {
		List<BookInCurrentCart> booksInBacket = bookrepository.findBooksInCurrentBacketByUserid(Long.valueOf(2));
		assertThat(booksInBacket).isEmpty();

		String username = "user1";
		User user = this.createVerifiedUser(username, username + "@gmail.com");
		Long userId = user.getId();

		Backet backet = this.createCurrentBacketWithUser(user);
		Book book1 = this.createBook("Little Women", "Other");
		this.createBacketBookCustomQuantity(2, book1, backet);

		booksInBacket = bookrepository.findBooksInCurrentBacketByUserid(userId);
		assertThat(booksInBacket).hasSize(1);

		Book book2 = this.createBook("Little Women 2", "Other");
		this.createBacketBookCustomQuantity(2, book2, backet);

		booksInBacket = bookrepository.findBooksInCurrentBacketByUserid(userId);
		assertThat(booksInBacket).hasSize(2);
	}

	@Test
	@Rollback
	public void testfindIdsOfBooksInCurrentCart() {
		List<Long> idsOfBooks = bookrepository.findIdsOfBooksInCurrentCart(Long.valueOf(2));
		assertThat(idsOfBooks).isEmpty();

		String username = "user1";
		User user = this.createVerifiedUser(username, username + "@gmail.com");
		Long userId = user.getId();

		Backet backet = this.createCurrentBacketWithUser(user);
		Book book1 = this.createBook("Little Women", "Other");
		this.createBacketBookCustomQuantity(2, book1, backet);

		idsOfBooks = bookrepository.findIdsOfBooksInCurrentCart(userId);
		assertThat(idsOfBooks).hasSize(1);

		Book book2 = this.createBook("Little Women 2", "Other");
		this.createBacketBookCustomQuantity(2, book2, backet);

		idsOfBooks = bookrepository.findIdsOfBooksInCurrentCart(userId);
		assertThat(idsOfBooks).hasSize(2);
	}

	@Test
	@Rollback
	public void testUpdateBook() {
		Book book1 = this.createBook("Little Women", "Other");

		Category updatedCategory = this.createCategory("Romance");
		String updatedTitle = "The oldman and the sea";
		String updatedAuthor = "Ernest Hemingway";
		int updatedBookYear = 2001;
		double updatedPrice = 11.24;
		String updatedUrl = "url.com";
		String updatedIsbn = "isbn222222";

		book1.setAuthor(updatedAuthor);
		book1.setBookYear(updatedBookYear);
		book1.setCategory(updatedCategory);
		book1.setIsbn(updatedIsbn);
		book1.setTitle(updatedTitle);
		book1.setUrl(updatedUrl);
		book1.setPrice(updatedPrice);
		bookrepository.save(book1);

		List<Book> romances = bookrepository.findByCategory(updatedCategory);
		assertThat(romances).hasSize(1);

		Book updatedBook = romances.get(0);
		assertThat(updatedBook.getAuthor()).isEqualTo(updatedAuthor);
		assertThat(updatedBook.getBookYear()).isEqualTo(updatedBookYear);
		assertThat(updatedBook.getCategory()).isEqualTo(updatedCategory);
		assertThat(updatedBook.getIsbn()).isEqualTo(updatedIsbn);
		assertThat(updatedBook.getTitle()).isEqualTo(updatedTitle);
		assertThat(updatedBook.getPrice()).isEqualTo(updatedPrice);
		assertThat(updatedBook.getUrl()).isEqualTo(updatedUrl);
	}
	
	@Test
	@Rollback
	public void testDeleteBook() {
		Book book1 = this.createBook("Little Women", "Other");
		Long book1Id = book1.getId();
		bookrepository.deleteById(book1Id);
		
		List<Book> books = (List<Book>) bookrepository.findAll();
		assertThat(books).isEmpty();
		
		this.createBook("Little Women", "Other");
		this.createBook("Little Women 2", "Other");
		
		bookrepository.deleteAll();
		books = (List<Book>) bookrepository.findAll();
		assertThat(books).isEmpty();
	}

	private Book createBook(String title, String categoryName) {
		Category category = this.createCategory(categoryName);
		Book newBook = new Book(title, "Chuck Palahniuk", title + "isbn", 1998, 10.2, category, "someurlToPicture");
		bookrepository.save(newBook);

		return newBook;
	}

	private Category createCategory(String categoryName) {
		Optional<Category> optionalCategory = crepository.findByName(categoryName);
		if (optionalCategory.isPresent())
			return optionalCategory.get();

		Category category = new Category(categoryName);
		crepository.save(category);

		return category;
	}

	private Order createSale(int quantity, Book book) {
		Backet backet = this.createBacketNoUser(false);
		this.createBacketBookCustomQuantity(quantity, book, backet);

		String stringField = "field";

		Order newOrder = new Order(stringField, stringField, stringField, stringField, stringField, stringField,
				stringField, backet, stringField, stringField);
		orepository.save(newOrder);

		return newOrder;
	}

	private BacketBook createBacketBookCustomQuantity(int quantity, Book book, Backet backet) {
		BacketBook newBacketBook = new BacketBook(quantity, backet, book);
		backetBookRepository.save(newBacketBook);

		return newBacketBook;
	}

	private Backet createCurrentBacketWithUser(User user) {
		List<Backet> currentBackets = backetrepository.findCurrentByUserid(user.getId());
		if (currentBackets.size() != 0)
			return currentBackets.get(0);

		Backet newBacket = new Backet(true, user);
		backetrepository.save(newBacket);

		return newBacket;
	}

	private Backet createBacketNoUser(boolean isCurrent) {
		Backet newBacket = new Backet(isCurrent);
		backetrepository.save(newBacket);

		return newBacket;
	}

	private User createVerifiedUser(String username, String email) {
		Optional<User> optionalUser = urepository.findByUsername(username);
		if (optionalUser.isPresent())
			return optionalUser.get();

		User newUser = new User("Test", "test", username, "Some_Pwd_Hash", "USER", email, true);
		urepository.save(newUser);

		return newUser;
	}
}
