package com.pro.mybooklist.resttest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class RestPublicControllerTest {
	private static final Logger log = LoggerFactory.getLogger(RestPublicControllerTest.class);

	private static final String END_POINT_PATH = "";

	private static final String BOOK_TITLE = "Little Women";
	private static final String OTHER_CATEGORY = "Other";
	private static final String ROMANCE_CATEGORY = "Romance";

	private static final Double DEFAULT_PRICE = 10.5;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private BookRepository bookRepository;

	@BeforeAll
	public void setUp() throws Exception {
		crepository.deleteAll();
		bookRepository.deleteAll();
	}

	@Test
	@Rollback
	public void testGetBooks() throws Exception {
		String requestURI = "/books";

		mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		this.createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
		this.createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
		this.createBook(BOOK_TITLE + " 3", ROMANCE_CATEGORY, DEFAULT_PRICE);

		mockMvc.perform(get(requestURI)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(3));
	}

	@Test
	@Rollback
	public void testGetCategories() throws Exception {
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
		mockMvc.perform(get(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyCategoryNotFound))
				.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		// No books in the category case;
		Category otherCategory = this.createCategory(OTHER_CATEGORY);
		String requestBodyEmptyCategory = objectMapper.writeValueAsString(otherCategory);
		mockMvc.perform(get(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyEmptyCategory))
				.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(0));

		// Good case:
		this.createBook(BOOK_TITLE, OTHER_CATEGORY, DEFAULT_PRICE);
		this.createBook(BOOK_TITLE + " 2", OTHER_CATEGORY, DEFAULT_PRICE);
		mockMvc.perform(get(requestURI).contentType(MediaType.APPLICATION_JSON).content(requestBodyEmptyCategory))
				.andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2));
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
}
