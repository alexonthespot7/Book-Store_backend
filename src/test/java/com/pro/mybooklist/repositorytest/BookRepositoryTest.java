package com.pro.mybooklist.repositorytest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
public class BookRepositoryTest {
	@Autowired
	private UserRepository urepository;

	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private BookRepository bookrepository;

	@BeforeAll
	public void resetUserRepo() {
		urepository.deleteAll();
		crepository.deleteAll();
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

	private User createVerifiedUser(String username, String email) {
		User newUser = new User("Test", "test", username, "Some_Pwd_Hash", "USER", email, true);
		urepository.save(newUser);

		return newUser;
	}

	private User createUnverifiedUser(String username, String email, String verificationCode) {
		User newUser = new User("Test", "test", username, "Some_Pwd_Hash", "USER", email, verificationCode, false);
		urepository.save(newUser);

		return newUser;
	}

	private void verifyUser(String code) {
		Optional<User> optionalUser = urepository.findByVerificationCode(code);
		assertThat(optionalUser).isPresent();

		User user = optionalUser.get();
		user.setAccountVerified(true);
		user.setVerificationCode(null);
		urepository.save(user);
	}
}
