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
import com.pro.mybooklist.model.BacketBookKey;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;

import jakarta.transaction.Transactional;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class BacketBookRepositoryTest {
	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private UserRepository urepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@BeforeAll
	public void resetUserRepo() {
		crepository.deleteAll();
		urepository.deleteAll();
		backetRepository.deleteAll();
		bookRepository.deleteAll();
		backetBookRepository.deleteAll();
	}

	// CRUD tests for the backetbook repository
	// Create functionality
	@Test
	@Rollback
	public void testCreateBacketBookDefaultQuantity() {
		// Testing backetBook creating with backet with no user;
		BacketBook newBacketBook1 = this.createBacketBookDefaultQuantityNoUser("Little Women", "Other");
		assertThat(newBacketBook1.getId()).isNotNull();

		this.createBacketBookDefaultQuantityNoUser("Little Women 2", "Other");
		List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).hasSize(2);

		// Testing backetBook creating with backet with user;
		BacketBook newBacketBookUser1 = this.createBacketBookDefaultQuantityUser("user1", "Little Women 3", "Other");
		assertThat(newBacketBookUser1.getId()).isNotNull();

		this.createBacketBookDefaultQuantityUser("user2", "Little Women 4", "Other");
		backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).hasSize(4);
	}

	@Test
	@Rollback
	public void testCreateBacketBookCustomQuantity() {
		// Testing backetBook creating with backet with no user;
		BacketBook newBacketBook1 = this.createBacketBookCustomQuantityNoUser(2, "Little Women", "Other");
		assertThat(newBacketBook1.getId()).isNotNull();

		this.createBacketBookCustomQuantityNoUser(3, "Little Women 2", "Other");
		List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).hasSize(2);

		// Testing backetBook creating with backet with user;
		BacketBook newBacketBookUser1 = this.createBacketBookCustomQuantityUser(2, "user1", "Little Women 3", "Other");
		assertThat(newBacketBookUser1.getId()).isNotNull();

		this.createBacketBookCustomQuantityUser(4, "user2", "Little Women 4", "Other");
		backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).hasSize(4);
	}

	@Test
	@Rollback
	public void testFindAllAndFindById() {
		List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).isEmpty();

		BacketBookKey wrongKey = new BacketBookKey(Long.valueOf(2), Long.valueOf(2));
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(wrongKey);
		assertThat(optionalBacketBook).isNotPresent();

		BacketBook newBacketBook = this.createBacketBookDefaultQuantityNoUser("Little Women", "Other");
		BacketBookKey goodKey = newBacketBook.getId();

		optionalBacketBook = backetBookRepository.findById(goodKey);
		assertThat(optionalBacketBook).isPresent();

		this.createBacketBookDefaultQuantityNoUser("Little Women 2", "Other");

		backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).hasSize(2);
	}
	
	@Test
	@Rollback
	public void testFindByBacket() {
		Backet emptyBacket = this.createBacketNoUser();
		List<BacketBook> backetBooks = backetBookRepository.findByBacket(emptyBacket);
		assertThat(backetBooks).isEmpty();
		
		BacketBook newBacketBook = this.createBacketBookDefaultQuantityNoUser("Little Women", "Other");
		Backet backet = newBacketBook.getBacket();
		
		backetBooks = backetBookRepository.findByBacket(backet);
		assertThat(backetBooks).hasSize(1);
		
		this.createBacketBookDefaultQuantityUser("user1", "Little Women", "Other");
		this.createBacketBookDefaultQuantityUser("user1", "Little Women 2", "Other");
		BacketBook backetBook3User1 = this.createBacketBookCustomQuantityUser(2, "user1", "Fight Club", "Thriller");
		Backet backetOfUser1 = backetBook3User1.getBacket();
		
		backetBooks = backetBookRepository.findByBacket(backetOfUser1);
		assertThat(backetBooks).hasSize(3);
	}

	@Test
	@Rollback
	public void testUpdate() {
		BacketBook backetBook = this.createBacketBookDefaultQuantityNoUser("Little Women", "Other");
		backetBook.setQuantity(3);
		backetBookRepository.save(backetBook);
		
		assertThat(backetBook.getQuantity()).isEqualTo(3);
	}
	
	@Test
	@Rollback
	public void testDeleteByIdAndDeleteAll() {
		BacketBook backetBookToDelete = this.createBacketBookCustomQuantityNoUser(2, "Little Women", "Other");
		BacketBookKey key = backetBookToDelete.getId();
		backetBookRepository.deleteById(key);
		
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(key);
		assertThat(optionalBacketBook).isNotPresent();
		
		this.createBacketBookCustomQuantityNoUser(2, "Little Women", "Other");
		this.createBacketBookCustomQuantityNoUser(2, "Little Women 2", "Other");
		backetBookRepository.deleteAll();
		
		List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
		assertThat(backetBooks).isEmpty();
	}
	
	@Test
	@Rollback
	public void testDeleteByBacket() {
		BacketBook newBacketBook = this.createBacketBookDefaultQuantityNoUser("Little Women", "Other");
		Backet backet = newBacketBook.getBacket();

		long quantityOfDeletedBacketBooks = backetBookRepository.deleteByBacket(backet);
		assertThat(quantityOfDeletedBacketBooks).isEqualTo(1);

		this.createBacketBookDefaultQuantityUser("user1", "Little Women", "Other");
		this.createBacketBookDefaultQuantityUser("user1", "Little Women 2", "Other");
		BacketBook backetBook3User1 = this.createBacketBookCustomQuantityUser(2, "user1", "Fight Club", "Thriller");
		Backet backetOfUser1 = backetBook3User1.getBacket();

		quantityOfDeletedBacketBooks = backetBookRepository.deleteByBacket(backetOfUser1);
		assertThat(quantityOfDeletedBacketBooks).isEqualTo(3);
	}

	private BacketBook createBacketBookCustomQuantityNoUser(int quantity, String title, String categoryName) {
		Backet backet = this.createBacketNoUser();
		Book book = this.createBook(title, categoryName);

		BacketBook newBacketBook = new BacketBook(quantity, backet, book);
		backetBookRepository.save(newBacketBook);

		return newBacketBook;
	}

	private BacketBook createBacketBookCustomQuantityUser(int quantity, String username, String title,
			String categoryName) {
		User user = this.createUser(username);

		Backet backet = this.createBacketWithUser(user);
		Book book = this.createBook(title, categoryName);

		BacketBook newBacketBook = new BacketBook(quantity, backet, book);
		backetBookRepository.save(newBacketBook);

		return newBacketBook;
	}

	private BacketBook createBacketBookDefaultQuantityNoUser(String title, String categoryName) {
		Backet backet = this.createBacketNoUser();
		Book book = this.createBook(title, categoryName);

		BacketBook newBacketBook = new BacketBook(backet, book);
		backetBookRepository.save(newBacketBook);

		return newBacketBook;
	}

	private BacketBook createBacketBookDefaultQuantityUser(String username, String title, String categoryName) {
		User user = this.createUser(username);

		Backet backet = this.createBacketWithUser(user);
		Book book = this.createBook(title, categoryName);

		BacketBook newBacketBook = new BacketBook(backet, book);
		backetBookRepository.save(newBacketBook);

		return newBacketBook;
	}

	private User createUser(String username) {
		Optional<User> optionalUser = urepository.findByUsername(username);
		if (optionalUser.isPresent())
			return optionalUser.get();

		User newUser = new User(username, username, username, username, "USER", username, true);
		urepository.save(newUser);

		return newUser;
	}

	private Backet createBacketWithUser(User user) {
		List<Backet> currentBackets = backetRepository.findCurrentByUserid(user.getId());
		if (currentBackets.size() != 0)
			return currentBackets.get(0);

		Backet newBacket = new Backet(true, user);
		backetRepository.save(newBacket);

		return newBacket;
	}

	private Backet createBacketNoUser() {
		Backet newBacket = new Backet(true);
		backetRepository.save(newBacket);

		return newBacket;
	}

	private Book createBook(String title, String categoryName) {
		String isbn = title + "isbn";
		Optional<Book> optionalBook = bookRepository.findByIsbn(isbn);

		if (optionalBook.isPresent())
			return optionalBook.get();

		Category category = this.createCategory(categoryName);

		Book newBook = new Book(title, "Chuck Palahniuk", isbn, 1940, 11.2, category, "some_url");

		return newBook;
	}

	private Category createCategory(String categoryName) {
		Optional<Category> optionalCategory = crepository.findByName(categoryName);

		if (optionalCategory.isPresent())
			return optionalCategory.get();

		Category newCategory = new Category(categoryName);
		crepository.save(newCategory);

		return newCategory;
	}
}
