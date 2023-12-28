package com.pro.mybooklist.resttest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro.mybooklist.httpforms.AccountCredentials;
import com.pro.mybooklist.httpforms.BookUpdate;
import com.pro.mybooklist.httpforms.OrderInfo;
import com.pro.mybooklist.httpforms.RoleInfo;
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

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class RestAdminControllerTest {
	private static final String BOOK_TITLE = "Little Women";
	private static final String OTHER_CATEGORY = "Other";
	private static final String ROMANCE_CATEGORY = "Romance";
	private static final Double DEFAULT_PRICE = 10.5;

	private static final String FIRSTNAME = "John";
	private static final String LASTNAME = "Doe";
	private static final String COUNTRY = "Finland";
	private static final String CITY = "Helsinki";
	private static final String STREET = "Kitarakuja 3B";
	private static final String POSTCODE = "00410";
	private static final String NOTE = "Complete my order quickly";

	private static final String USERNAME = "user1";
	private static final String ADMIN_USERNAME = "admin";
	private static final String ADMIN_EMAIL = "admin@mail.com";
	private static final String EMAIL = "user1@mail.com";

	private static final String DEFAULT_PASSWORD = "test";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	@Autowired
	private OrderRepository orepository;

	private String jwt;
	private Long adminId;

	@BeforeAll
	public void setUp() throws Exception {
		this.resetRepos();
		this.getToken();
	}

	@Test
	@Rollback
	public void testGetUsersAllCases() throws Exception {
		String requestURI = "/users";

		// Only one user (admin) case:
		mockMvc.perform(get(requestURI).header("Authorization", jwt)).andExpect(status().isOk())
				.andExpect(jsonPath("$.size()").value(1));

		this.createUser(USERNAME, EMAIL);
		this.createUser(USERNAME + "2", "2" + EMAIL);

		// 3 users in db case:
		mockMvc.perform(get(requestURI).header("Authorization", jwt)).andExpect(status().isOk())
				.andExpect(jsonPath("$.size()").value(3));

		// Testing security:
		mockMvc.perform(get(requestURI)).andExpect(status().isUnauthorized());
	}

	@Test
	@Rollback
	public void testGetOrdersAllCases() throws Exception {
		String requestURI = "/orders";

		// No orders case:
		mockMvc.perform(get(requestURI).header("Authorization", jwt)).andExpect(status().isOk())
				.andExpect(jsonPath("$.size()").value(0));

		this.createOrderWithDefaultStatusNoUser();
		this.createOrderWithDefaultStatusNoUser();

		// Two orders case:
		mockMvc.perform(get(requestURI).header("Authorization", jwt)).andExpect(status().isOk())
				.andExpect(jsonPath("$.size()").value(2));

		// Testing security:
		mockMvc.perform(get(requestURI)).andExpect(status().isUnauthorized());
	}

	@Nested
	class testGetOrderById {
		@Test
		@Rollback
		public void testGetOrderByIdOrderNotFoundCase() throws Exception {
			String requestURIOrderNotFound = "/orders/22";

			mockMvc.perform(get(requestURIOrderNotFound).header("Authorization", jwt)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetOrderByIdGoodCase() throws Exception {
			String requestURI = "/orders/";

			Order order = createOrderWithDefaultStatusNoUser();
			Long orderId = order.getOrderid();

			String requestURIGood = requestURI + orderId;

			mockMvc.perform(get(requestURIGood).header("Authorization", jwt)).andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("Created"));
		}
	}

	@Nested
	class testUpdateOrder {
		@Test
		@Rollback
		public void testUpdateOrderNotFoundCase() throws Exception {
			String requestURI = "/updateorder/22";

			OrderInfo orderInfo = new OrderInfo("In progress", FIRSTNAME, LASTNAME, COUNTRY, CITY, STREET, POSTCODE,
					EMAIL);
			String requestBody = objectMapper.writeValueAsString(orderInfo);

			mockMvc.perform(put(requestURI).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testUpdateOrderGoodCase() throws Exception {
			String requestURI = "/updateorder/";

			Order order = createOrderWithDefaultStatusNoUser();
			Long orderId = order.getOrderid();
			String requestURIGood = requestURI + orderId;

			OrderInfo orderInfo = new OrderInfo("In progress", FIRSTNAME, LASTNAME, COUNTRY, CITY + "Update", STREET,
					POSTCODE, "New_mail");
			String requestBody = objectMapper.writeValueAsString(orderInfo);

			mockMvc.perform(put(requestURIGood).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)).andExpect(status().isOk());

			List<Order> orders = (List<Order>) orepository.findAll();
			assertThat(orders).hasSize(1);
			Order updatedOrder = orders.get(0);
			assertThat(updatedOrder.getCity()).isEqualTo(CITY + "Update");
			assertThat(updatedOrder.getStatus()).isEqualTo("In progress");
			assertThat(updatedOrder.getEmail()).isEqualTo("New_mail");
		}
	}

	@Nested
	class testChangeUserRole {
		@Test
		@Rollback
		public void testChangeUserRoleUserNotFoundCase() throws Exception {
			String requestURIUserNotFound = "/changerole/22";

			RoleInfo roleInfo = new RoleInfo("ADMIN");
			String requestBody = objectMapper.writeValueAsString(roleInfo);

			mockMvc.perform(put(requestURIUserNotFound).header("Authorization", jwt)
					.contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testChangeUserRoleTryingToChangeOwnRoleCase() throws Exception {
			String requestURI = "/changerole/";

			String requestURIOwnRole = requestURI + adminId;

			RoleInfo roleInfo = new RoleInfo("USER");
			String requestBody = objectMapper.writeValueAsString(roleInfo);

			mockMvc.perform(put(requestURIOwnRole).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)).andExpect(status().isNotAcceptable());
		}

		@Test
		@Rollback
		public void testChangeUserRoleGoodCase() throws Exception {
			String requestURI = "/changerole/";

			User user = createUser(USERNAME, EMAIL);
			Long userId = user.getId();

			String requestURIGood = requestURI + userId;

			RoleInfo roleInfo = new RoleInfo("ADMIN");
			String requestBody = objectMapper.writeValueAsString(roleInfo);

			mockMvc.perform(put(requestURIGood).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)).andExpect(status().isOk());

			List<User> users = (List<User>) urepository.findAll();
			for (User admin : users) {
				assertThat(admin.getRole()).isEqualTo("ADMIN");
			}
		}
	}

	@Nested
	class testUpdateBook {
		@Test
		@Rollback
		public void testUpdateBookNotFoundBookCase() throws Exception {
			String requestURIBookNotFound = "/books/2222";

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, 11);
			String requestBody = objectMapper.writeValueAsString(book);

			mockMvc.perform(put(requestURIBookNotFound).header("Authorization", jwt)
					.contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testUpdateBookCategoryNotFoundCase() throws Exception {
			String requestURI = "/books/";

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, 11);
			Long bookId = book.getId();

			BookUpdate bookUpdated = new BookUpdate();
			bookUpdated.setCategoryId(Long.valueOf(22));

			String requestURIGood = requestURI + bookId;
			String requestBodyCategoryNotFound = objectMapper.writeValueAsString(bookUpdated);

			mockMvc.perform(put(requestURIGood).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBodyCategoryNotFound)).andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testUpdateBookDuplicateIsbnCase() throws Exception {
			String requestURI = "/books/";

			Book newBook = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);

			Book bookToUpdate = createBook(BOOK_TITLE, OTHER_CATEGORY, 11);
			Long bookId = bookToUpdate.getId();

			BookUpdate bookUpdated = new BookUpdate();
			bookUpdated.setIsbn(newBook.getIsbn());

			String requestURIGood = requestURI + bookId;
			String requestBodyCategoryNotFound = objectMapper.writeValueAsString(bookUpdated);

			mockMvc.perform(put(requestURIGood).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBodyCategoryNotFound)).andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testUpdateBookGoodCase() throws Exception {
			String requestURI = "/books/";

			Book book = createBook("WRONG", OTHER_CATEGORY, 11);
			Category newCategory = createCategory(ROMANCE_CATEGORY);

			BookUpdate updatedBook = new BookUpdate();
			updatedBook.setAuthor(FIRSTNAME);
			updatedBook.setTitle(BOOK_TITLE);
			updatedBook.setPrice(DEFAULT_PRICE);
			updatedBook.setBookYear(2001);
			updatedBook.setCategoryId(newCategory.getCategoryid());
			updatedBook.setIsbn("1234567");
			updatedBook.setUrl("NewURL");

			Long bookId = book.getId();
			String requestURIGood = requestURI + bookId;
			String requestBody = objectMapper.writeValueAsString(updatedBook);

			mockMvc.perform(put(requestURIGood).header("Authorization", jwt).contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)).andExpect(status().isOk());

			Optional<Book> optionalUpdatedBook = bookRepository.findById(bookId);
			assertThat(optionalUpdatedBook).isPresent();
			Book editedBook = optionalUpdatedBook.get();

			assertThat(editedBook.getAuthor()).isEqualTo(FIRSTNAME);
			assertThat(editedBook.getTitle()).isEqualTo(BOOK_TITLE);
			assertThat(editedBook.getCategory()).isEqualTo(newCategory);
		}
	}

	private Backet createBacketWithUser(boolean current, String username, String email) {
		User user = this.createUser(username, email);

		List<Backet> currentBackets = backetRepository.findCurrentByUserid(user.getId());
		if (currentBackets.size() != 0 && current)
			return currentBackets.get(0);

		Backet newBacket = new Backet(current, user);
		backetRepository.save(newBacket);

		return newBacket;
	}

	private User createUser(String username, String email) {
		Optional<User> optionalUser = urepository.findByUsername(username);

		if (optionalUser.isPresent())
			return optionalUser.get();

		optionalUser = urepository.findByEmail(email);

		if (optionalUser.isPresent())
			return optionalUser.get();

		String hashPwd = this.encodePassword(DEFAULT_PASSWORD);
		User user = new User(FIRSTNAME, LASTNAME, username, hashPwd, "USER", email, false);
		urepository.save(user);

		return user;
	}

	private BacketBook createBacketBookCustomQuantity(int quantity, Book book, Backet backet) {
		BacketBook newBacketBook = new BacketBook(quantity, backet, book);
		backetBookRepository.save(newBacketBook);

		return newBacketBook;
	}

	private Book createBook(String title, String categoryName, double price) {
		Category category = this.createCategory(categoryName);

		Optional<Book> bookByIsbn = bookRepository.findByIsbn(title + "isbn");

		if (bookByIsbn.isPresent())
			return bookByIsbn.get();

		Book newBook = new Book(title, "Chuck Palahniuk", title + "isbn", 1998, price, category, "someurlToPicture");
		bookRepository.save(newBook);

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

	private String encodePassword(String password) {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);

		return hashPwd;
	}

	private void resetRepos() {
		crepository.deleteAll();
		urepository.deleteAll();
		backetRepository.deleteAll();
		bookRepository.deleteAll();
		backetBookRepository.deleteAll();
		orepository.deleteAll();
	}

	private void getToken() throws Exception {
		String requestURI = "/login";

		// Adding verified user to the database
		this.createAdmin();

		this.createBacketWithUser(true, ADMIN_USERNAME, ADMIN_EMAIL);

		AccountCredentials creds = new AccountCredentials(ADMIN_USERNAME, DEFAULT_PASSWORD);
		String requestBody = objectMapper.writeValueAsString(creds);
		MvcResult result = mockMvc
				.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isOk()).andReturn();

		jwt = result.getResponse().getHeader("Authorization");
		adminId = Long.valueOf(result.getResponse().getHeader("Host"));
	}

	private User createAdmin() {
		String hashPwd = this.encodePassword(DEFAULT_PASSWORD);

		User newAdmin = new User(FIRSTNAME, FIRSTNAME, ADMIN_USERNAME, hashPwd, "ADMIN", ADMIN_EMAIL, true);

		urepository.save(newAdmin);

		return newAdmin;
	}

	private Backet createBacketNoUser(boolean current) {
		Backet newBacket = new Backet(current);
		backetRepository.save(newBacket);

		return newBacket;
	}

	private Order createOrderWithDefaultStatusNoUser() {
		Backet backet = this.createBacketNoUser(false);

		List<Book> booksInOrder = new ArrayList<Book>();
		Book book1 = this.createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
		Book book2 = this.createBook(BOOK_TITLE + " 2", ROMANCE_CATEGORY, DEFAULT_PRICE);
		booksInOrder.add(book1);
		booksInOrder.add(book2);

		for (Book book : booksInOrder) {
			this.createBacketBookCustomQuantity(1, book, backet);
		}

		String hashPwd = this.encodePassword(DEFAULT_PASSWORD);

		Order newOrder = new Order(FIRSTNAME, LASTNAME, COUNTRY, CITY, STREET, POSTCODE, EMAIL, backet, NOTE, hashPwd);
		orepository.save(newOrder);

		return newOrder;
	}
}
