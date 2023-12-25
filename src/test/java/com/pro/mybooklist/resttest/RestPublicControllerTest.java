package com.pro.mybooklist.resttest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro.mybooklist.httpforms.AccountCredentials;
import com.pro.mybooklist.httpforms.AddressInfoNoAuthentication;
import com.pro.mybooklist.httpforms.BacketInfo;
import com.pro.mybooklist.httpforms.BookQuantityInfo;
import com.pro.mybooklist.httpforms.EmailInfo;
import com.pro.mybooklist.httpforms.OrderPasswordInfo;
import com.pro.mybooklist.httpforms.SignupCredentials;
import com.pro.mybooklist.httpforms.TokenInfo;
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
public class RestPublicControllerTest {
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
	private static final String EMAIL = "user1@mail.com";

	private static final String DEFAULT_PASSWORD = "test";
	private static final String WRONG_PWD = "wrong_pwd";

	@Value("${spring.mail.username}")
	private String springMailUsername;

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

	@BeforeAll
	public void setUp() throws Exception {
		crepository.deleteAll();
		urepository.deleteAll();
		backetRepository.deleteAll();
		bookRepository.deleteAll();
		backetBookRepository.deleteAll();
		orepository.deleteAll();
	}

	@Test
	@Rollback
	public void testGetBooksAllCases() throws Exception {
		String requestURI = "/books";

		mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		this.createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
		this.createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
		this.createBook(BOOK_TITLE + " 3", ROMANCE_CATEGORY, DEFAULT_PRICE);

		mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(3));
	}

	@Test
	@Rollback
	public void testGetCategoriesAllCases() throws Exception {
		String requestURI = "/categories";

		mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		this.createCategory(OTHER_CATEGORY);
		this.createCategory(ROMANCE_CATEGORY);

		mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
	}

	@Test
	@Rollback
	public void testGetBookByIdAllCases() throws Exception {
		String requestURI = "/books/";

		// No book was find case:
		String requestURINotFound = requestURI + Long.valueOf(2);
		MvcResult result = mockMvc.perform(get(requestURINotFound)).andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isEqualTo("null");

		// Good case
		// Arrange
		Book book = this.createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
		Long bookId = book.getId();
		String requestURIGood = requestURI + bookId;

		// Act
		result = mockMvc.perform(get(requestURIGood)).andExpect(status().isOk()).andReturn();
		String bookAsString = result.getResponse().getContentAsString();
		TypeReference<Book> typeReference = new TypeReference<Book>() {
		};
		Book bookResponse = objectMapper.readValue(bookAsString, typeReference);

		// Assert
		assertThat(bookResponse).isNotNull();
		assertThat(bookResponse.getTitle()).isEqualTo(BOOK_TITLE);
		assertThat(bookResponse.getPrice()).isEqualTo(DEFAULT_PRICE);
		assertThat(bookResponse.getCategory().getName()).isEqualTo(OTHER_CATEGORY);
	}

	@Test
	@Rollback
	public void testGetBooksByCategoryAllCases() throws Exception {
		String requestURI = "/booksbycategory";
		// Category not found case:
		String requestBodyCategoryNotFound = "{\"categoryid\":1,\"name\":\"Other\"}";
		mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyCategoryNotFound))
				.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		// No books in the category case;
		Category otherCategory = this.createCategory(OTHER_CATEGORY);
		String requestBodyEmptyCategory = objectMapper.writeValueAsString(otherCategory);
		mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyEmptyCategory))
				.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		// Good case:
		this.createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
		this.createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
		mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyEmptyCategory))
				.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
	}

	@Nested
	class testGetOrderByIdAndPassword {
		@Test
		@Rollback
		public void testGetOrderByIdAndPasswordNotFoundCase() throws Exception {
			String requestURI = "/orderbypassword";
			OrderPasswordInfo orderInfoNotFound = new OrderPasswordInfo(Long.valueOf(2), DEFAULT_PASSWORD);
			String requestBodyNotFound = objectMapper.writeValueAsString(orderInfoNotFound);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyNotFound))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetOrderByIdAndPasswordWrongPasswordCase() throws Exception {
			String requestURI = "/orderbypassword";

			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			Order order = createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);

			OrderPasswordInfo OrderInfoWrongPwd = new OrderPasswordInfo(order.getOrderid(), "wrong_pwd");
			String requestBodyWrongPwd = objectMapper.writeValueAsString(OrderInfoWrongPwd);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongPwd))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testGetOrderByIdAndPasswordGoodCase() throws Exception {
			String requestURI = "/orderbypassword";

			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			Order order = createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);

			OrderPasswordInfo orderInfo = new OrderPasswordInfo(order.getOrderid(), DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(orderInfo);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Created"));
		}
	}

	@Test
	@Rollback
	public void testCreateBacketNoAuthenticationAllCases() throws Exception {
		String requestURI = "/createbacket";

		mockMvc.perform(post(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.password").exists());

		List<Backet> backets = (List<Backet>) backetRepository.findAll();
		assertThat(backets).hasSize(1);
	}

	@Nested
	class testAddBookToCartNoAuthentication {
		@Test
		@Rollback
		public void testAddBookToCartNoAuthenticationBacketNotFoundCase() throws Exception {
			String requestURI = "/addbook/";

			BookQuantityInfo bookQuantityInfo = new BookQuantityInfo(2, Long.valueOf(2), WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(bookQuantityInfo);
			String requestURIBacketNotFound = requestURI + Long.valueOf(2);
			MvcResult result = mockMvc
					.perform(
							post(requestURIBacketNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotFound()).andReturn();
			String message = result.getResponse().getErrorMessage();
			assertThat(message).isEqualTo("The backet wasn't found by id");
		}

		@Test
		@Rollback
		public void testAddBookToCartNoAuthenticationBacketIsPrivateCase() throws Exception {
			String requestURI = "/addbook/";

			Backet newBacket = createBacketWithUser(true, USERNAME);
			Long privateBacketId = newBacket.getBacketid();

			BookQuantityInfo bookQuantityInfo = new BookQuantityInfo(2, Long.valueOf(2), WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(bookQuantityInfo);
			String requestURIBacketIsPrivate = requestURI + privateBacketId;
			mockMvc.perform(
					post(requestURIBacketIsPrivate).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testAddBookToCartNoAuthenticationWrongPasswordCase() throws Exception {
			String requestURI = "/addbook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			String goodRequestURI = requestURI + backetId;

			BookQuantityInfo bookQuantityInfoWrongPwd = new BookQuantityInfo(2, Long.valueOf(2), WRONG_PWD);
			String requestBodyWrongPwd = objectMapper.writeValueAsString(bookQuantityInfoWrongPwd);
			mockMvc.perform(post(goodRequestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongPwd))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testAddBookToCartNoAuthenticationBacketIsNotCurrentCase() throws Exception {
			String requestURI = "/addbook/";

			Backet backet = createBacketNoUser(false);
			Long backetId = backet.getBacketid();
			String requestURINotCurrentBacket = requestURI + backetId;

			BookQuantityInfo bookQuantityInfo = new BookQuantityInfo(2, Long.valueOf(2), DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(bookQuantityInfo);
			mockMvc.perform(
					post(requestURINotCurrentBacket).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testAddBookToCartNoAuthenticationBookNotFoundCase() throws Exception {
			String requestURI = "/addbook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();
			String requestURIGood = requestURI + backetId;

			BookQuantityInfo bookQuantityInfo = new BookQuantityInfo(2, Long.valueOf(2), DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(bookQuantityInfo);
			MvcResult result = mockMvc
					.perform(post(requestURIGood).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotFound()).andReturn();

			String message = result.getResponse().getErrorMessage();
			assertThat(message).isEqualTo("The book wasn't found by id");
		}

		@Test
		@Rollback
		public void testAddBookToCartNoAuthenticationGoodCases() throws Exception {
			String requestURI = "/addbook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();
			String requestURIGood = requestURI + backetId;

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			// Adding the new backetBook record case
			BookQuantityInfo bookQuantityInfo = new BookQuantityInfo(2, bookId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(bookQuantityInfo);
			mockMvc.perform(post(requestURIGood).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());

			List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
			assertThat(backetBooks).hasSize(1);
			assertThat(backetBooks.get(0).getQuantity()).isEqualTo(2);

			// Adding books quantity to the existing backetBook case:
			mockMvc.perform(post(requestURIGood).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());
			backetBooks = (List<BacketBook>) backetBookRepository.findAll();
			assertThat(backetBooks).hasSize(1);
			assertThat(backetBooks.get(0).getQuantity()).isEqualTo(4);
		}
	}

	@Nested
	class testGetIdsOfBooksByBacketid {
		@Test
		@Rollback
		public void testGetIdsOfBooksByBacketidBacketNotFoundCase() throws Exception {
			String requestURI = "/booksids/";

			String requestURIBacketNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(get(requestURIBacketNotFound)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetIdsOfBooksByBacketidBacketHasUserCase() throws Exception {
			String requestURI = "/booksids/";

			Backet backet = createBacketWithUser(true, USERNAME);
			Long backetId = backet.getBacketid();

			String requestURIBacketHasUser = requestURI + backetId;

			mockMvc.perform(get(requestURIBacketHasUser)).andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testGetIdsOfBooksByBacketidGoodCases() throws Exception {
			String requestURI = "/booksids/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			String requestURIGood = requestURI + backetId;
			// Empty lost case
			mockMvc.perform(get(requestURIGood)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			createBacketBookCustomQuantity(2, book1, backet);
			createBacketBookCustomQuantity(1, book2, backet);
			mockMvc.perform(get(requestURIGood)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
		}
	}

	@Nested
	class testGetBooksInBacketByIdAndPassword {
		@Test
		@Rollback
		public void testGetBooksInBacketByIdAndPasswordBacketNotFoundCase() throws Exception {
			String requestURI = "/showcart";

			BacketInfo backetInfoBacketNotFound = new BacketInfo(Long.valueOf(2), WRONG_PWD);
			String requestBodyNotFound = objectMapper.writeValueAsString(backetInfoBacketNotFound);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyNotFound))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetBooksInBacketByIdAndPasswordBacketHasOwnerCase() throws Exception {
			String requestURI = "/showcart";

			Backet backetWithOwner = createBacketWithUser(false, USERNAME);
			Long backetId = backetWithOwner.getBacketid();

			BacketInfo backetInfo = new BacketInfo(backetId, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testGetBooksInBacketByIdAndPasswordWrongPasswordCase() throws Exception {
			String requestURI = "/showcart";

			Backet backet = createBacketNoUser(false);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfoWrongPwd = new BacketInfo(backetId, WRONG_PWD);
			String requestBodyWrongPwd = objectMapper.writeValueAsString(backetInfoWrongPwd);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongPwd))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testGetBooksInBacketByIdAndPasswordGoodCases() throws Exception {
			String requestURI = "/showcart";

			Backet backet = createBacketNoUser(false);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			// Empty backet case;
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", ROMANCE_CATEGORY, DEFAULT_PRICE);
			createBacketBookCustomQuantity(1, book1, backet);
			createBacketBookCustomQuantity(1, book2, backet);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
		}
	}

	@Nested
	class testGetBooksByOrderId {
		@Test
		@Rollback
		public void testGetBooksByOrderIdOrderNotFoundCase() throws Exception {
			String requestURI = "/booksinorder/";
			String requestURIOrderNotFound = requestURI + Long.valueOf(2);
			mockMvc.perform(get(requestURIOrderNotFound)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetBooksByOrderIdGoodCase() throws Exception {
			String requestURI = "/booksinorder/";
			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", ROMANCE_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			Order order = createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);
			Long orderId = order.getOrderid();
			String requestURIGood = requestURI + orderId;
			mockMvc.perform(get(requestURIGood)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
		}
	}

	@Nested
	class testGetTotalByBacketId {
		@Test
		@Rollback
		public void testGetTotalByBacketIdBacketNotFoundCase() throws Exception {
			String requestURI = "/totalofbacket";

			BacketInfo backetInfoNotFound = new BacketInfo(Long.valueOf(2), DEFAULT_PASSWORD);
			String requestBodyNotFound = objectMapper.writeValueAsString(backetInfoNotFound);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyNotFound))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetTotalByBacketIdBacketIsPrivateCase() throws Exception {
			String requestURI = "/totalofbacket";

			Backet newBacket = createBacketWithUser(true, USERNAME);
			Long backetId = newBacket.getBacketid();

			BacketInfo backetInfoPrivateBacket = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBodyPrivateBacket = objectMapper.writeValueAsString(backetInfoPrivateBacket);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyPrivateBacket))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testGetTotalByBacketIdWrongPasswordCase() throws Exception {
			String requestURI = "/totalofbacket";

			Backet newBacket = createBacketNoUser(true);
			Long backetId = newBacket.getBacketid();

			BacketInfo backetInfoWrongPwd = new BacketInfo(backetId, WRONG_PWD);
			String requestBodyWrongPwd = objectMapper.writeValueAsString(backetInfoWrongPwd);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongPwd))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testGetTotalByBacketIdGoodCases() throws Exception {
			String requestURI = "/totalofbacket";

			Backet newBacket = createBacketNoUser(true);
			Long backetId = newBacket.getBacketid();

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);

			// Empty backet case
			MvcResult result = mockMvc
					.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk()).andReturn();
			String resultAsString = result.getResponse().getContentAsString();
			assertThat(resultAsString).isEqualTo("");

			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", ROMANCE_CATEGORY, DEFAULT_PRICE);
			createBacketBookCustomQuantity(1, book1, newBacket);
			createBacketBookCustomQuantity(2, book2, newBacket);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk()).andExpect(jsonPath("$.total").value(DEFAULT_PRICE * 3));
		}
	}

	@Nested
	class testGetTotalOfOrderByOrderId {
		@Test
		@Rollback
		public void testGetTotalOfOrderByOrderIdOrderNotFoundCase() throws Exception {
			String requestURIOrderNotFound = "/getordertotal/" + Long.valueOf(2);

			mockMvc.perform(get(requestURIOrderNotFound)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetTotalOfOrderByOrderIdGoodCase() throws Exception {
			String requestURI = "/getordertotal/";

			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			Order order = createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);
			Long orderId = order.getOrderid();

			String requestURIGood = requestURI + orderId;
			mockMvc.perform(get(requestURIGood)).andExpect(status().isOk())
					.andExpect(jsonPath("$.total").value(DEFAULT_PRICE * 4));
		}
	}

	@Nested
	class testReduceItemNoAuthentication {
		@Test
		@Rollback
		public void testReduceItemNoAuthenticationBacketNotFoundCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			BacketInfo backetInfoNotFound = new BacketInfo(Long.valueOf(2), WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfoNotFound);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			MvcResult result = mockMvc
					.perform(put(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotFound()).andReturn();

			String message = result.getResponse().getErrorMessage();
			assertThat(message).isEqualTo("The backet wasn't found by id");
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationBacketIsPrivateCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backetWithOwner = createBacketWithUser(true, USERNAME);
			Long backetId = backetWithOwner.getBacketid();

			BacketInfo backetInfoPrivateBacket = new BacketInfo(backetId, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfoPrivateBacket);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(put(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationWrongPasswordCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfoWrongPwd = new BacketInfo(backetId, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfoWrongPwd);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(put(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationBacketIsNotCurrentCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backet = createBacketNoUser(false);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfoNotCurrent = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfoNotCurrent);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(put(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationBookNotFoundCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfoGood = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBodyGood = objectMapper.writeValueAsString(backetInfoGood);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			MvcResult result = mockMvc.perform(
					put(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBodyGood))
					.andExpect(status().isNotFound()).andReturn();

			String message = result.getResponse().getErrorMessage();
			assertThat(message).isEqualTo("The book wasn't found by id");
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationBookIsNotInBacketCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			String requestURIBookNotinBacket = requestURI + bookId;

			mockMvc.perform(put(requestURIBookNotinBacket).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationQuantityIsReducedGoodCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			createBacketBookCustomQuantity(2, book, backet);

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			String requestURIOk = requestURI + bookId;

			mockMvc.perform(put(requestURIOk).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());

			List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
			assertThat(backetBooks).hasSize(1);
			assertThat(backetBooks.get(0).getQuantity()).isEqualTo(1);
		}

		@Test
		@Rollback
		public void testReduceItemNoAuthenticationBookIsRemovedFromBacketGoodCase() throws Exception {
			String requestURI = "/reduceitemnoauth/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			createBacketBookCustomQuantity(1, book, backet);

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			String requestURIOk = requestURI + bookId;

			mockMvc.perform(put(requestURIOk).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());

			List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
			assertThat(backetBooks).hasSize(0);
		}
	}

	@Nested
	class testDeleteBookNoAuthentication {
		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBacketNotFoundCase() throws Exception {
			String requestURI = "/deletebook/";

			BacketInfo backetInfoNotFound = new BacketInfo(Long.valueOf(2), WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfoNotFound);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			MvcResult result = mockMvc
					.perform(
							delete(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotFound()).andReturn();

			String message = result.getResponse().getErrorMessage();
			assertThat(message).isEqualTo("The backet wasn't found by id");
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBacketIsPrivateCase() throws Exception {
			String requestURI = "/deletebook/";

			Backet backetWithOwner = createBacketWithUser(true, USERNAME);
			Long backetId = backetWithOwner.getBacketid();

			BacketInfo backetInfoPrivatBacket = new BacketInfo(backetId, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfoPrivatBacket);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(delete(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationWrongPasswordCase() throws Exception {
			String requestURI = "/deletebook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfoWrongPwd = new BacketInfo(backetId, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(backetInfoWrongPwd);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(delete(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBacketIsNotCurrentCase() throws Exception {
			String requestURI = "/deletebook/";

			Backet backet = createBacketNoUser(false);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfoNotCurrent = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfoNotCurrent);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(delete(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBookNotFoundCase() throws Exception {
			String requestURI = "/deletebook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBodyBookNotFound = objectMapper.writeValueAsString(backetInfo);
			String requestURIBookNotFound = requestURI + Long.valueOf(2);

			mockMvc.perform(delete(requestURIBookNotFound).contentType(MediaType.APPLICATION_JSON)
					.content(requestBodyBookNotFound)).andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBookIsNotInBacketCase() throws Exception {
			String requestURI = "/deletebook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			String requestURIBookNotInBacket = requestURI + bookId;

			mockMvc.perform(
					delete(requestURIBookNotInBacket).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBookWithQuantityMoreThan1IsRemovedFromBacketGoodCase()
				throws Exception {
			String requestURI = "/deletebook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			createBacketBookCustomQuantity(3, book, backet);

			BacketInfo backetInfoWithBookId = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfoWithBookId);
			String requestURIOk = requestURI + bookId;

			mockMvc.perform(delete(requestURIOk).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());

			List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
			assertThat(backetBooks).hasSize(0);
		}

		@Test
		@Rollback
		public void testDeleteBookNoAuthenticationBookWithQuantity1IsRemovedFromBacketGoodCase() throws Exception {
			String requestURI = "/deletebook/";

			Backet backet = createBacketNoUser(true);
			Long backetId = backet.getBacketid();

			Book book = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Long bookId = book.getId();

			createBacketBookCustomQuantity(1, book, backet);

			BacketInfo backetInfo = new BacketInfo(backetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(backetInfo);
			String requestURIOk = requestURI + bookId;

			mockMvc.perform(delete(requestURIOk).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());

			List<BacketBook> backetBooks = (List<BacketBook>) backetBookRepository.findAll();
			assertThat(backetBooks).hasSize(0);
		}
	}

	@Nested
	class testMakeSaleNoAuthentication {
		@Test
		@Rollback
		public void testMakeSaleNoAuthenticationBacketNotFoundCase() throws Exception {
			String requestURI = "/makesale";

			AddressInfoNoAuthentication addressInfoNotFound = new AddressInfoNoAuthentication(FIRSTNAME, LASTNAME,
					COUNTRY, CITY, STREET, POSTCODE, EMAIL, NOTE, Long.valueOf(2), WRONG_PWD);
			String requestBodyNotFound = objectMapper.writeValueAsString(addressInfoNotFound);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyNotFound))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testMakeSaleNoAuthenticationEmptyBacketCase() throws Exception {
			String requestURI = "/makesale";

			Backet emptyBacket = createBacketNoUser(true);
			Long bakcetId = emptyBacket.getBacketid();

			AddressInfoNoAuthentication addressInfoEmptyBacket = new AddressInfoNoAuthentication(FIRSTNAME, LASTNAME,
					COUNTRY, CITY, STREET, POSTCODE, EMAIL, NOTE, bakcetId, DEFAULT_PASSWORD);
			String requestBodyEmptyBacket = objectMapper.writeValueAsString(addressInfoEmptyBacket);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyEmptyBacket))
					.andExpect(status().isNotAcceptable());
		}

		@Test
		@Rollback
		public void testMakeSaleNoAuthenticationWrongPasswordCase() throws Exception {
			String requestURI = "/makesale";

			Backet backet = createBacketNoUser(false);
			Long bakcetId = backet.getBacketid();

			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			createBacketBookCustomQuantity(1, book1, backet);

			AddressInfoNoAuthentication addressInfoWrongPwd = new AddressInfoNoAuthentication(FIRSTNAME, LASTNAME,
					COUNTRY, CITY, STREET, POSTCODE, EMAIL, NOTE, bakcetId, WRONG_PWD);
			String requestBodyWrongPwd = objectMapper.writeValueAsString(addressInfoWrongPwd);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongPwd))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testMakeSaleNoAuthenticationBacketIsNotCurrentCase() throws Exception {
			String requestURI = "/makesale";

			Backet backetNotCurrent = createBacketNoUser(false);
			Long bakcetId = backetNotCurrent.getBacketid();

			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			createBacketBookCustomQuantity(1, book1, backetNotCurrent);

			AddressInfoNoAuthentication addressInfo = new AddressInfoNoAuthentication(FIRSTNAME, LASTNAME, COUNTRY,
					CITY, STREET, POSTCODE, EMAIL, NOTE, bakcetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(addressInfo);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testMakeSaleNoAuthenticationGoodCase() throws Exception {
			String requestURI = "/makesale";

			Backet backet = createBacketNoUser(true);
			Long bakcetId = backet.getBacketid();

			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			createBacketBookCustomQuantity(1, book1, backet);

			AddressInfoNoAuthentication addressInfo = new AddressInfoNoAuthentication(FIRSTNAME, LASTNAME, COUNTRY,
					CITY, STREET, POSTCODE, EMAIL, NOTE, bakcetId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(addressInfo);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk()).andExpect(jsonPath("$.orderid").exists())
					.andExpect(jsonPath("$.password").exists());

			assertThat(backet.isCurrent()).isFalse();
		}
	}

	@Nested
	class testGetTopSales {
		@Test
		@Rollback
		public void testGetTopSalesWithSalesGoodCase() throws Exception {
			String requestURI = "/topsales";

			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);
			createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);

			mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
		}

		@Test
		@Rollback
		public void testGetTopSalesNoSalesCase() throws Exception {
			String requestURI = "/topsales";

			mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));
		}
	}

	@Nested
	class testCheckOrderNumber {
		@Test
		@Rollback
		public void testCheckOrderNumberOrderNotFoundCase() throws Exception {
			String requestURI = "/checkordernumber";

			OrderPasswordInfo orderInfoNotFound = new OrderPasswordInfo(Long.valueOf(2), DEFAULT_PASSWORD);
			String requestBodyNotFound = objectMapper.writeValueAsString(orderInfoNotFound);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyNotFound))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testCheckOrderNumberWrongPasswordCase() throws Exception {
			String requestURI = "/checkordernumber";

			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			Order order = createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);
			Long orderId = order.getOrderid();

			OrderPasswordInfo orderInfoWrongPwd = new OrderPasswordInfo(orderId, WRONG_PWD);
			String requestBodyWrongPwd = objectMapper.writeValueAsString(orderInfoWrongPwd);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongPwd))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Rollback
		public void testCheckOrderNumberGoodCase() throws Exception {
			String requestURI = "/checkordernumber";

			List<Book> booksInOrder = new ArrayList<Book>();
			Book book1 = createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
			Book book2 = createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
			booksInOrder.add(book1);
			booksInOrder.add(book2);

			Order order = createOrderWithDefaultStatusNoUser(2, booksInOrder, DEFAULT_PASSWORD);
			Long orderId = order.getOrderid();

			OrderPasswordInfo orderInfo = new OrderPasswordInfo(orderId, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(orderInfo);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class testGetToken {
		@Test
		@Rollback
		public void testGetTokenUserNotFoundByEmailCase() throws Exception {
			String requestURI = "/login";

			createUser(USERNAME);

			AccountCredentials credentials = new AccountCredentials("Wrong_Email@mail.com", DEFAULT_PASSWORD);
			String requestBodyWrongEmail = objectMapper.writeValueAsString(credentials);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongEmail))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetTokenUserNotFoundByUsernameCase() throws Exception {
			String requestURI = "/login";

			createUser(USERNAME);

			AccountCredentials credentials = new AccountCredentials("Wrong_Username", DEFAULT_PASSWORD);
			String requestBodyWrongUsername = objectMapper.writeValueAsString(credentials);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyWrongUsername))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testGetTokenUnverifiedUserByEmailCase() throws Exception {
			String requestURI = "/login";

			User user = createUser(USERNAME);

			AccountCredentials credentials = new AccountCredentials(EMAIL, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(credentials);

			// email service is working case:
			if (!springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isConflict());

				// Email service is not working case:
			} else {
				// Wrong Password case
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isUnauthorized());
				assertThat(user.isAccountVerified()).isTrue();
			}
		}

		@Test
		@Rollback
		public void testGetTokenUnverifiedUserByUsernameCase() throws Exception {
			String requestURI = "/login";

			User user = createUser(USERNAME);

			AccountCredentials credentials = new AccountCredentials(USERNAME, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(credentials);

			// email service is working case:
			if (!springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isConflict());

				// Email service is not working case:
			} else {
				// Wrong Password case
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isUnauthorized());
				assertThat(user.isAccountVerified()).isTrue();
			}
		}

		@Test
		@Rollback
		public void testGetTokenWrongPasswordCases() throws Exception {
			String requestURI = "/login";

			User user = createUser(USERNAME);
			user.setAccountVerified(true);
			urepository.save(user);

			// By username case:
			AccountCredentials credentials = new AccountCredentials(USERNAME, WRONG_PWD);
			String requestBody = objectMapper.writeValueAsString(credentials);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isUnauthorized());

			// By email case:
			credentials = new AccountCredentials(EMAIL, WRONG_PWD);
			requestBody = objectMapper.writeValueAsString(credentials);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Rollback
		public void testGetTokenUnverifiedUserAlreadyHasBacketAndEmailIsNotWorkingCases() throws Exception {
			String requestURI = "/login";

			User user = createUser(USERNAME);
			createBacketWithUser(true, USERNAME);

			// By username case:
			AccountCredentials credentials = new AccountCredentials(USERNAME, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(credentials);

			if (springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk());

				assertThat(user.isAccountVerified()).isTrue();
				List<Backet> currentBackets = backetRepository.findCurrentByUserid(user.getId());
				assertThat(currentBackets).hasSize(1);
			}

			user.setAccountVerified(false);
			urepository.save(user);
			createBacketWithUser(true, USERNAME);

			// By email case:
			credentials = new AccountCredentials(EMAIL, DEFAULT_PASSWORD);
			requestBody = objectMapper.writeValueAsString(credentials);

			if (springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk());

				assertThat(user.isAccountVerified()).isTrue();
				List<Backet> currentBackets = backetRepository.findCurrentByUserid(user.getId());
				assertThat(currentBackets).hasSize(1);
			}
		}

		@Test
		@Rollback
		public void testGetTokenUnverifiedUserAndEmailIsNotWorkingGoodCases() throws Exception {
			String requestURI = "/login";

			User user = createUser(USERNAME);

			// By username case:
			AccountCredentials credentials = new AccountCredentials(USERNAME, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(credentials);
			List<Backet> backets = (List<Backet>) backetRepository.findAll();
			assertThat(backets).isEmpty();

			if (springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk())
						.andExpect(header().string("Authorization", Matchers.containsString("Bearer")))
						.andExpect(header().string("Allow", "USER"));

				assertThat(user.isAccountVerified()).isTrue();
				backets = (List<Backet>) backetRepository.findAll();
				assertThat(backets).hasSize(1);
				assertThat(backets.get(0).getUser()).isEqualTo(user);
			}

			backetRepository.deleteAll();
			user.setAccountVerified(false);
			urepository.save(user);

			// By email case:
			credentials = new AccountCredentials(EMAIL, DEFAULT_PASSWORD);
			requestBody = objectMapper.writeValueAsString(credentials);

			if (springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk())
						.andExpect(header().string("Authorization", Matchers.containsString("Bearer")))
						.andExpect(header().string("Allow", "USER"));

				assertThat(user.isAccountVerified()).isTrue();
				backets = (List<Backet>) backetRepository.findAll();
				assertThat(backets).hasSize(1);
				assertThat(backets.get(0).getUser()).isEqualTo(user);
			}
		}

		@Test
		@Rollback
		public void testGetTokenVerifiedGoodCases() throws Exception {
			String requestURI = "/login";

			User user = createUser(USERNAME);
			user.setAccountVerified(true);
			urepository.save(user);

			// By username case:
			AccountCredentials credentials = new AccountCredentials(USERNAME, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(credentials);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk())
					.andExpect(header().string("Authorization", Matchers.containsString("Bearer")))
					.andExpect(header().string("Allow", "USER"));

			// By email case:
			credentials = new AccountCredentials(EMAIL, DEFAULT_PASSWORD);
			requestBody = objectMapper.writeValueAsString(credentials);

			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk())
					.andExpect(header().string("Authorization", Matchers.containsString("Bearer")))
					.andExpect(header().string("Allow", "USER"));
		}
	}

	@Nested
	class testSignUp {
		@Test
		@Rollback
		public void testSignUpEmailInUseCase() throws Exception {
			String requestURI = "/signup";

			createUser(USERNAME + "2");

			SignupCredentials creds = new SignupCredentials(FIRSTNAME, LASTNAME, USERNAME, EMAIL, DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(creds);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotAcceptable());
		}

		@Test
		@Rollback
		public void testSignUpUsernameInUseCase() throws Exception {
			String requestURI = "/signup";

			createUser(USERNAME);

			SignupCredentials creds = new SignupCredentials(FIRSTNAME, LASTNAME, USERNAME, "new" + EMAIL,
					DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(creds);
			mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testSignUpGoodCases() throws Exception {
			String requestURI = "/signup";

			SignupCredentials creds = new SignupCredentials(FIRSTNAME, LASTNAME, USERNAME, "new" + EMAIL,
					DEFAULT_PASSWORD);
			String requestBody = objectMapper.writeValueAsString(creds);
			Optional<User> optionalUser;
			User user;

			if (!springMailUsername.equals("default_value")) {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk());

				optionalUser = urepository.findByUsername(USERNAME);
				assertThat(optionalUser.isPresent());
				user = optionalUser.get();
				assertThat(user.isAccountVerified()).isFalse();
			} else {
				mockMvc.perform(post(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isAccepted());

				optionalUser = urepository.findByUsername(USERNAME);
				assertThat(optionalUser.isPresent());
				user = optionalUser.get();
				assertThat(user.isAccountVerified()).isTrue();
			}
		}
	}

	@Nested
	class testVerifyUser {
		@Test
		@Rollback
		public void testVerifyUserVerificationCodeIsIncorrectCase() throws Exception {
			String requestURI = "/verify";

			TokenInfo tokenInfo = new TokenInfo("Wrong_Token");
			String requestBody = objectMapper.writeValueAsString(tokenInfo);
			mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testVerifyUserAlreadyVerifiedCase() throws Exception {
			String requestURI = "/verify";

			User user = createUser(USERNAME);
			user.setAccountVerified(true);
			urepository.save(user);

			String token = user.getVerificationCode();

			TokenInfo tokenInfo = new TokenInfo(token);
			String requestBody = objectMapper.writeValueAsString(tokenInfo);
			mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isConflict());
		}

		@Test
		@Rollback
		public void testVerifyUserAlreadyHasCurrentBacketCase() throws Exception {
			String requestURI = "/verify";

			User user = createUser(USERNAME);

			String token = user.getVerificationCode();

			createBacketWithUser(true, USERNAME);

			TokenInfo tokenInfo = new TokenInfo(token);
			String requestBody = objectMapper.writeValueAsString(tokenInfo);
			mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());
		}

		@Test
		@Rollback
		public void testVerifyUserGoodCase() throws Exception {
			String requestURI = "/verify";

			User user = createUser(USERNAME);

			String token = user.getVerificationCode();

			TokenInfo tokenInfo = new TokenInfo(token);
			String requestBody = objectMapper.writeValueAsString(tokenInfo);
			mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isOk());

			User verifiedUser = urepository.findByUsername(USERNAME).get();

			assertThat(verifiedUser.isAccountVerified()).isTrue();
			assertThat(verifiedUser.getVerificationCode()).isNull();

			List<Backet> backets = (List<Backet>) backetRepository.findAll();
			assertThat(backets).hasSize(1);
			assertThat(backets.get(0).getUser()).isEqualTo(verifiedUser);
			assertThat(backets.get(0).isCurrent()).isTrue();
		}
	}

	@Nested
	class testResetPassword {
		@Test
		@Rollback
		public void testResetPasswordUserNotFoundCase() throws Exception {
			String requestURI = "/resetpassword";

			EmailInfo emailInfo = new EmailInfo("WrongMail");
			String requestBody = objectMapper.writeValueAsString(emailInfo);

			mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotFound());
		}

		@Test
		@Rollback
		public void testResetPasswordAccountIsNotVerifiedCase() throws Exception {
			String requestURI = "/resetpassword";

			User user = createUser(USERNAME);

			EmailInfo emailInfo = new EmailInfo(EMAIL);
			String requestBody = objectMapper.writeValueAsString(emailInfo);

			if (!springMailUsername.equals("default_value")) {
				mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isConflict());
				return;
			}

			// User doesn't have backet yet case:
			mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
					.andExpect(status().isNotImplemented());

			assertThat(user.isAccountVerified()).isTrue();
			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
			assertThat(bc.matches(DEFAULT_PASSWORD, user.getPassword())).isTrue();

			List<Backet> backets = (List<Backet>) backetRepository.findAll();
			assertThat(backets).hasSize(1);
			assertThat(backets.get(0).isCurrent()).isTrue();

			// User already has backet case:
			user.setAccountVerified(false);
			user.setVerificationCode("SomeCode");
			urepository.save(user);

			if (!springMailUsername.equals("default_value")) {
				mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk());
				assertThat(user.isAccountVerified()).isTrue();
				backets = (List<Backet>) backetRepository.findAll();
				assertThat(backets).hasSize(1);
			} else {
				mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isNotImplemented());
				assertThat(user.isAccountVerified()).isTrue();
				backets = (List<Backet>) backetRepository.findAll();
				assertThat(backets).hasSize(1);
			}
		}

		@Test
		@Rollback
		public void testResetPasswordVerifiedUserGoodCases() throws Exception {
			String requestURI = "/resetpassword";

			User user = createUser(USERNAME);
			user.setAccountVerified(true);
			user.setVerificationCode(null);
			urepository.save(user);

			EmailInfo emailInfo = new EmailInfo(EMAIL);
			String requestBody = objectMapper.writeValueAsString(emailInfo);

			if (!springMailUsername.equals("default_value")) {
				mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isOk());
				BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
				assertThat(bc.matches(DEFAULT_PASSWORD, user.getPassword())).isFalse();
			} else {
				mockMvc.perform(put(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBody))
						.andExpect(status().isNotImplemented());
			}
		}
	}

	private Order createOrderWithDefaultStatusNoUser(int quantity, List<Book> books, String password) {
		Backet backet = this.createBacketNoUser(false);

		for (Book book : books) {
			this.createBacketBookCustomQuantity(quantity, book, backet);
		}

		String stringField = "field";
		String hashPwd = this.encodePassword(password);

		Order newOrder = new Order(stringField, stringField, stringField, stringField, stringField, stringField,
				stringField, backet, stringField, hashPwd);
		orepository.save(newOrder);

		return newOrder;
	}

	private Backet createBacketWithUser(boolean current, String username) {
		User user = this.createUser(username);

		List<Backet> currentBackets = backetRepository.findCurrentByUserid(user.getId());
		if (currentBackets.size() != 0 && current)
			return currentBackets.get(0);

		Backet newBacket = new Backet(current, user);
		backetRepository.save(newBacket);

		return newBacket;
	}

	private Backet createBacketNoUser(boolean current) {
		Backet newBacket = new Backet(current);
		backetRepository.save(newBacket);

		return newBacket;
	}

	private User createUser(String username) {
		Optional<User> optionalUser = urepository.findByUsername(username);

		if (optionalUser.isPresent())
			return optionalUser.get();

		String hashPwd = this.encodePassword(DEFAULT_PASSWORD);
		User user = new User(FIRSTNAME, LASTNAME, username, hashPwd, "USER", EMAIL, false);
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
}
