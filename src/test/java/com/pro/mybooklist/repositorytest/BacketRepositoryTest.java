package com.pro.mybooklist.repositorytest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.ArrayList;
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
import com.pro.mybooklist.sqlforms.QuantityOfBacket;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

import jakarta.transaction.Transactional;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class BacketRepositoryTest {
	@Autowired
	private UserRepository urepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@Autowired
	private BookRepository bookrepository;

	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private OrderRepository orepository;

	@Autowired
	private BacketRepository backetrepository;

	@BeforeAll
	public void resetUserRepo() {
		urepository.deleteAll();
		backetBookRepository.deleteAll();
		bookrepository.deleteAll();
		crepository.deleteAll();
		orepository.deleteAll();
		backetrepository.deleteAll();
	}

	// CRUD tests for the backet repository
	// Create functionality
	@Test
	@Rollback
	public void testCreateBacketWithUser() {
		Backet newBacket1 = this.createBacketWithUser(true, "user1");
		assertThat(newBacket1.getBacketid()).isNotNull();

		this.createBacketWithUser(true, "user2");
		List<Backet> backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).hasSize(2);
	}

	@Test
	@Rollback
	public void testCreateBacketNoUser() {
		Backet newBacket1 = this.createBacketNoUser(true);
		assertThat(newBacket1.getBacketid()).isNotNull();

		this.createBacketNoUser(true);
		List<Backet> backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).hasSize(2);
	}

	@Test
	@Rollback
	public void testFindAllAndFindById() {
		List<Backet> backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).isEmpty();

		Optional<Backet> optionalBacket = backetrepository.findById(Long.valueOf(2));
		assertThat(optionalBacket).isNotPresent();

		Backet backet = this.createBacketNoUser(true);
		Long backetId = backet.getBacketid();

		optionalBacket = backetrepository.findById(backetId);
		assertThat(optionalBacket).isPresent();

		this.createBacketNoUser(true);

		backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).hasSize(2);
	}

	@Test
	@Rollback
	public void testFindQuantityInCurrent() {
		QuantityOfBacket quantityOfBacket = backetrepository.findQuantityInCurrent(Long.valueOf(2));
		assertThat(quantityOfBacket).isNull();

		String username = "user1";
		User user = this.createUser(username);
		Long userId = user.getId();

		Backet backet = this.createBacketWithUser(true, username);
		Book book1 = this.createBook("Little Women", "Other", 10.2);
		this.createBacketBookCustomQuantity(2, book1, backet);

		quantityOfBacket = backetrepository.findQuantityInCurrent(userId);
		assertThat(quantityOfBacket).isNotNull();
		assertThat(quantityOfBacket.getItems()).isEqualTo(2);

		Book book2 = this.createBook("Little Women 2", "Other", 10.2);
		this.createBacketBookCustomQuantity(3, book2, backet);
		quantityOfBacket = backetrepository.findQuantityInCurrent(userId);
		assertThat(quantityOfBacket.getItems()).isEqualTo(5);
	}

	@Test
	@Rollback
	public void testFindTotalOfBacket() {
		TotalOfBacket totalOfBacket = backetrepository.findTotalOfBacket(Long.valueOf(2));
		assertThat(totalOfBacket).isNull();

		String username = "user1";
		Backet backet = this.createBacketWithUser(true, username);
		Long backetId = backet.getBacketid();

		double price1 = 10.2;
		Book book1 = this.createBook("Little Women", "Other", price1);
		this.createBacketBookCustomQuantity(2, book1, backet);

		totalOfBacket = backetrepository.findTotalOfBacket(backetId);
		assertThat(totalOfBacket).isNotNull();
		assertThat(totalOfBacket.getTotal()).isEqualTo(price1 * 2);

		double price2 = 8.2;
		Book book2 = this.createBook("Little Women 2", "Other", price2);
		this.createBacketBookCustomQuantity(3, book2, backet);
		totalOfBacket = backetrepository.findTotalOfBacket(backetId);
		assertThat(totalOfBacket).isNotNull();
		assertThat(totalOfBacket.getTotal()).isEqualTo(price1 * 2 + price2 * 3);

		Backet backetNoUser = this.createBacketNoUser(true);
		Long backet2Id = backetNoUser.getBacketid();

		totalOfBacket = backetrepository.findTotalOfBacket(backet2Id);
		assertThat(totalOfBacket).isNull();

		this.createBacketBookCustomQuantity(1, book2, backetNoUser);
		totalOfBacket = backetrepository.findTotalOfBacket(backet2Id);
		assertThat(totalOfBacket).isNotNull();
		assertThat(totalOfBacket.getTotal()).isEqualTo(price2);
	}

	@Test
	@Rollback
	public void testFindTotalOfOrder() {
		TotalOfBacket totalOfBacket = backetrepository.findTotalOfOrder(Long.valueOf(2));
		assertThat(totalOfBacket).isNull();

		int quantity = 2;
		double priceBook1 = 11.2;
		double priceBook2 = 4.6;

		List<Book> booksInOrder1 = new ArrayList<Book>();
		Book book1 = this.createBook("Little Women", "Other", priceBook1);
		Book book2 = this.createBook("Fight Club", "Thriller", priceBook2);
		booksInOrder1.add(book1);
		booksInOrder1.add(book2);

		Order order1 = this.createSale(quantity, booksInOrder1);
		Long order1Id = order1.getOrderid();

		totalOfBacket = backetrepository.findTotalOfOrder(order1Id);
		assertThat(totalOfBacket).isNotNull();
		assertThat(totalOfBacket.getTotal()).isCloseTo(quantity * (priceBook1 + priceBook2), offset(0.01));
	}

	@Test
	@Rollback
	public void testFindTotalOfCurrentCart() {
		TotalOfBacket totalOfCurrent = backetrepository.findTotalOfCurrentCart(Long.valueOf(2));
		assertThat(totalOfCurrent).isNull();

		String username = "user1";
		Backet backet = this.createBacketWithUser(true, username);

		double price1 = 10.2;
		Book book1 = this.createBook("Little Women", "Other", price1);
		this.createBacketBookCustomQuantity(2, book1, backet);

		Long user1Id = urepository.findByUsername(username).get().getId();

		totalOfCurrent = backetrepository.findTotalOfCurrentCart(user1Id);
		assertThat(totalOfCurrent).isNotNull();
		assertThat(totalOfCurrent.getTotal()).isEqualTo(price1 * 2);

		double price2 = 8.2;
		Book book2 = this.createBook("Little Women 2", "Other", price2);
		this.createBacketBookCustomQuantity(3, book2, backet);
		totalOfCurrent = backetrepository.findTotalOfCurrentCart(user1Id);
		assertThat(totalOfCurrent).isNotNull();
		assertThat(totalOfCurrent.getTotal()).isEqualTo(price1 * 2 + price2 * 3);
	}

	@Test
	@Rollback
	public void testFindNotCurrentByUserid() {
		List<Long> idsOfNotCurrentBackets = backetrepository.findNotCurrentByUserid(Long.valueOf(2));
		assertThat(idsOfNotCurrentBackets).isEmpty();

		String username = "user1";
		Backet backet = this.createBacketWithUser(false, username);
		this.createBacketWithUser(true, username);
		Long backetId = backet.getBacketid();

		Long user1Id = urepository.findByUsername(username).get().getId();

		idsOfNotCurrentBackets = backetrepository.findNotCurrentByUserid(user1Id);
		assertThat(idsOfNotCurrentBackets).hasSize(1);
		assertThat(idsOfNotCurrentBackets.get(0)).isEqualTo(backetId);
	}
	
	@Test
	@Rollback
	public void testFindCurrentByUserid() {
		List<Backet> currentBackets = backetrepository.findCurrentByUserid(Long.valueOf(2));
		assertThat(currentBackets).isEmpty();
		
		String username = "user1";
		this.createBacketWithUser(false, username);
		this.createBacketWithUser(true, username);
		
		Long user1Id = urepository.findByUsername(username).get().getId();

		currentBackets = backetrepository.findCurrentByUserid(user1Id);
		assertThat(currentBackets).hasSize(1);
	}
	
	@Test
	@Rollback
	public void testUpdateBacket() {
		boolean current = true;
		Backet newBacket = this.createBacketNoUser(current);
		
		String updatedPwd = "newHash";
		
		newBacket.setCurrent(!current);
		newBacket.setPasswordHash(updatedPwd);
		backetrepository.save(newBacket);
		
		assertThat(newBacket.isCurrent()).isEqualTo(!current);
		assertThat(newBacket.getPasswordHash()).isEqualTo(updatedPwd);
	}
	
	@Test
	@Rollback
	public void testDeleteBacket() {
		Backet backetToDelete = this.createBacketNoUser(false);
		Long backetId = backetToDelete.getBacketid();
		backetrepository.deleteById(backetId);
		
		Optional<Backet> optionalBacket = backetrepository.findById(backetId);
		assertThat(optionalBacket).isNotPresent();
		
		this.createBacketNoUser(false);
		this.createBacketNoUser(false);
		backetrepository.deleteAll();
		
		List<Backet> backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).isEmpty();
	}

	private Order createSale(int quantity, List<Book> books) {
		Backet backet = this.createBacketNoUser(false);

		for (Book book : books) {
			this.createBacketBookCustomQuantity(quantity, book, backet);
		}

		String stringField = "field";

		Order newOrder = new Order(stringField, stringField, stringField, stringField, stringField, stringField,
				stringField, backet, stringField, stringField);
		orepository.save(newOrder);

		return newOrder;
	}

	private Backet createBacketWithUser(boolean current, String username) {
		User user = this.createUser(username);

		List<Backet> currentBackets = backetrepository.findCurrentByUserid(user.getId());
		if (currentBackets.size() != 0)
			return currentBackets.get(0);

		Backet newBacket = new Backet(current, user);
		backetrepository.save(newBacket);

		return newBacket;
	}

	private Backet createBacketNoUser(boolean current) {
		Backet newBacket = new Backet(current);
		backetrepository.save(newBacket);

		return newBacket;
	}

	private User createUser(String username) {
		Optional<User> optionalUser = urepository.findByUsername(username);

		if (optionalUser.isPresent())
			return optionalUser.get();

		User user = new User("Firstname", "Lastname", username, "hash_pwd", "USER", username + "@mail.com", false);
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

}
