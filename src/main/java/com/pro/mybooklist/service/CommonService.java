package com.pro.mybooklist.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;

@Service
public class CommonService {
	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BookRepository bookRepository;

	// Method to find the backet and check if it's private:
	public Backet findBacketAndCheckIsPrivate(Long backetId) {
		Backet backet = this.findBacket(backetId);
		this.checkIfBacketIsPrivate(backet);
		return backet;
	}

	// Method to find backet, check if it is private and check the provided
	// password:
	public Backet findBacketAndCheckIsPrivateAndCheckPassword(Long backetId, String password) {
		Backet backet = this.findBacketAndCheckIsPrivate(backetId);
		this.checkPassword(password, backet.getPasswordHash());
		return backet;
	}

	// Method to find backet, check if it's private, it's password and check if it's
	// current
	public Backet findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(Long backetId, String password) {
		Backet backet = this.findBacketAndCheckIsPrivateAndCheckPassword(backetId, password);
		this.checkIfBacketIsCurrent(backet);
		return backet;
	}

	// The method to find out if the user has exactly one current backet:
	public Backet findCurrentBacketOfUser(User user) {
		Long userId = user.getId();
		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1) {
			return this.handleUserHasMoreOrLessThanOneCurrentBacketCase(currentBacketsOfUser, user);
		}

		Backet currentBacket = currentBacketsOfUser.get(0);
		return currentBacket;
	}
	
	private Backet handleUserHasMoreOrLessThanOneCurrentBacketCase(List<Backet> currentBacketsOfUser, User user) {
		for (Backet currentBacketOfUser : currentBacketsOfUser) {
			backetRepository.delete(currentBacketOfUser);
		}
		Backet newCurrentBacketForUser = new Backet(true, user);
		backetRepository.save(newCurrentBacketForUser);
		return newCurrentBacketForUser;
	}

	// Method to add new current Backet for the user
	public void addCurrentBacketForUser(User user) {
		Long userId = user.getId();
		this.checkCurrentBacketsOfUser(userId);
		Backet newCurrentBacketForUser = new Backet(true, user);
		backetRepository.save(newCurrentBacketForUser);
	}

	private void checkCurrentBacketsOfUser(Long userId) {
		List<Backet> currentBackets = backetRepository.findCurrentByUserid(userId);

		if (currentBackets.size() > 0) {
			for (Backet currentBacketOfUser : currentBackets) {
				backetRepository.delete(currentBacketOfUser);
			}
		}
	}

	private Backet findBacket(Long backetId) {
		Optional<Backet> optionalBacket = backetRepository.findById(backetId);

		if (!optionalBacket.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The backet wasn't found by id");

		Backet backet = optionalBacket.get();
		return backet;
	}

	private void checkIfBacketIsPrivate(Backet backet) {
		User backetOwner = backet.getUser();

		if (backetOwner != null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The backet is private");
	}

	private void checkIfBacketIsCurrent(Backet backet) {
		if (!backet.isCurrent())
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You can't change not current backet");
	}

	// Method to encode password:
	public String encodePassword(String password) {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);

		return hashPwd;
	}

	// Method to compare password and hashedPassword with BCryp encoder
	public void checkPassword(String password, String passwordHash) {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		if (!bc.matches(password, passwordHash))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is wrong");
	}

	// Method to check if the order is in the db by orderId:
	public Order findOrder(Long orderId) {
		Optional<Order> optionalOrder = orderRepository.findById(orderId);
		if (!optionalOrder.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The order wasn't found by id");

		Order order = optionalOrder.get();
		return order;
	}

	// Method to find order and check its password
	public Order findOrderAndCheckPassword(Long orderId, String password) {
		Order order = this.findOrder(orderId);
		this.checkPassword(password, order.getPassword());

		return order;
	}

	// Method to check provided authentication and find user by auth
	public User checkAuthentication(Authentication authentication) {
		String username = this.getAuthenticatedUsername(authentication);
		User user = this.findUserByUsername(username);

		return user;
	}

	// Method to check authentication and then check if the authenticated user is
	// the target user by comparing IDs
	public User checkAuthenticationAndAuthorize(Authentication authentication, Long userId) {
		User user = this.checkAuthentication(authentication);
		this.checkAuthorization(user, userId);
		return user;
	}

	// Method to check if the user has rights by user instance and userId by
	// comparing IDs:
	public void checkAuthorization(User user, Long userId) {
		if (user.getId() != userId)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");
	}

	private String getAuthenticatedUsername(Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();

		return username;
	}

	// The method to find the user by username:
	public User findUserByUsername(String username) {
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with this username doesn't exist");

		User user = optionalUser.get();

		return user;
	}
	
	// Method to find the book by id:
		public Book findBook(Long bookId) {
			Optional<Book> optionalBook = bookRepository.findById(bookId);
			if (!optionalBook.isPresent())
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The book wasn't found by id");

			Book book = optionalBook.get();
			return book;
		}
}
