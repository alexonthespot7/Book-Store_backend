package com.pro.mybooklist.web;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.httpforms.AddressInfo;
import com.pro.mybooklist.httpforms.OrderPasswordInfo;
import com.pro.mybooklist.httpforms.PasswordInfo;
import com.pro.mybooklist.httpforms.QuantityInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookKey;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;
import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.QuantityOfBacket;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@CrossOrigin(origins = "*")
@RestController
@PreAuthorize("isAuthenticated()")
public class RestAuthenticatedController {
	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	AuthenticationManager authenticationManager;

	@Value("${spring.mail.username}")
	private String springMailUsername;

	@PutMapping("/updateuser/{userid}")
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public ResponseEntity<?> updateUser(@PathVariable("userid") Long userId, @RequestBody User user,
			Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User userToUpdate = optionalUser.get();

		if (userToUpdate.getId() != userId || userId != user.getId())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		userToUpdate.setLastname(user.getLastname());
		userToUpdate.setFirstname(user.getFirstname());
		userToUpdate.setCountry(user.getCountry());
		userToUpdate.setCity(user.getCity());
		userToUpdate.setStreet(user.getStreet());
		userToUpdate.setPostcode(user.getPostcode());

		userRepository.save(userToUpdate);

		return new ResponseEntity<>("User info was updated successfully", HttpStatus.OK);
	}

	@PutMapping("/changepassword")
	public ResponseEntity<?> changePassword(@RequestBody PasswordInfo passwordInfo, Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();

		if (!username.equals(passwordInfo.getUsername()))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

		if (!bc.matches(passwordInfo.getOldPassword(), user.getPassword()))
			return new ResponseEntity<>("The old password is incorrect", HttpStatus.CONFLICT);

		String hashPwd = bc.encode(passwordInfo.getNewPassword());

		user.setPassword(hashPwd);
		userRepository.save(user);

		return new ResponseEntity<>("The password was changed", HttpStatus.OK);
	}

	@GetMapping("/users/{userid}/orders")
	public @ResponseBody List<Order> getOrdersByUserId(@PathVariable("userid") Long userId,
			Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();

		if (user.getId() != userId)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		return orderRepository.findByUserid(userId);
	}

	@GetMapping(value = "/users/{id}")
	public @ResponseBody User getUserById(@PathVariable("id") Long userId, Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();

		if (user.getId() != userId)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		return user;
	}

	@GetMapping("/booksids")
	public @ResponseBody List<Long> getIdsOfBooksInCurrentCart(Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();
		Long userId = user.getId();

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		return bookRepository.findIdsOfBooksInCurrentCart(userId);
	}

	@GetMapping("/showcart/{userid}")
	public @ResponseBody List<BookInCurrentCart> getCurrentCartByUserId(@PathVariable("userid") Long userId,
			Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();

		if (user.getId() != userId)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		return (List<BookInCurrentCart>) bookRepository.findBooksInCurrentBacketByUserid(userId);
	}

	@GetMapping("/getcurrenttotal")
	public @ResponseBody TotalOfBacket getCurrentCartTotal(Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();
		Long userId = user.getId();

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		return backetRepository.findTotalOfCurrentCart(userId);
	}

	@GetMapping("/currentbacketquantity")
	public @ResponseBody QuantityOfBacket getCurrentCartQuantity(Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();
		Long userId = user.getId();

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		return backetRepository.findQuantityInCurrent(userId);
	}

	@PostMapping("/additem/{bookid}")
	public ResponseEntity<?> addBookToCurrentBacket(@PathVariable("bookid") Long bookId,
			@RequestBody QuantityInfo quantityInfo, Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();
		Long userId = user.getId();

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			return new ResponseEntity<>("There should be exactly 1 current backet for user. Actual quantity: "
					+ currentBacketsOfUser.size(), HttpStatus.INTERNAL_SERVER_ERROR);

		Backet currentBacket = currentBacketsOfUser.get(0);
		Long backetId = currentBacket.getBacketid();

		Optional<Book> optionalBook = bookRepository.findById(bookId);

		if (!optionalBook.isPresent())
			return new ResponseEntity<>("Book wasn't found by id", HttpStatus.NOT_FOUND);

		Book book = optionalBook.get();
		BacketBookKey backetBookKey = new BacketBookKey(backetId, bookId);
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);

		BacketBook backetBook;
		if (optionalBacketBook.isPresent()) {
			backetBook = optionalBacketBook.get();

			int quantity = backetBook.getQuantity();
			backetBook.setQuantity(quantity + quantityInfo.getQuantity());
		} else {
			backetBook = new BacketBook(quantityInfo.getQuantity(), currentBacket, book);
		}

		backetBookRepository.save(backetBook);

		return new ResponseEntity<>("Book was added to cart successfully", HttpStatus.OK);
	}

	@PostMapping("/makesale/{userid}")
	public @ResponseBody OrderPasswordInfo makeSaleByUserId(@PathVariable("userid") Long userId,
			@RequestBody AddressInfo addressInfo, Authentication authentication)
			throws MessagingException, UnsupportedEncodingException {

		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();

		if (user.getId() != userId)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		Backet currentBacket = currentBacketsOfUser.get(0);

		List<BacketBook> booksInBacket = backetBookRepository.findByBacket(currentBacket);

		if (booksInBacket.size() == 0)
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "The backet is empty");

		String passwordRandom = RandomStringUtils.random(15);
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwdOrder = bc.encode(passwordRandom);

		currentBacket.setCurrent(false);
		Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(), addressInfo.getCountry(),
				addressInfo.getCity(), addressInfo.getStreet(), addressInfo.getPostcode(), addressInfo.getEmail(),
				currentBacket, addressInfo.getNote(), hashPwdOrder);
		orderRepository.save(order);
		backetRepository.save(currentBacket);

		try {
			this.sendOrderInfoEmail(user.getUsername(), user.getEmail(), order.getOrderid(), passwordRandom);
		} catch (MailAuthenticationException e) {
		}
		if (!addressInfo.getEmail().equals(user.getEmail())) {
			try {
				this.sendOrderInfoEmail(user.getUsername(), addressInfo.getEmail(), order.getOrderid(), passwordRandom);
			} catch (MailAuthenticationException e) {
			}
		}

		backetRepository.save(new Backet(true, user));

		OrderPasswordInfo orderPassword = new OrderPasswordInfo(order.getOrderid(), passwordRandom);

		return orderPassword;
	}

	@PutMapping("/reduceitem/{bookid}")
	@Transactional
	public ResponseEntity<?> reduceItemAuthenticated(@PathVariable("bookid") Long bookId,
			Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();
		Long userId = user.getId();

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		Backet backet = currentBacketsOfUser.get(0);
		Long backetId = backet.getBacketid();

		Optional<Book> optionalBook = bookRepository.findById(bookId);

		if (!optionalBook.isPresent())
			return new ResponseEntity<>("Book cannot be found", HttpStatus.NOT_FOUND);

		BacketBookKey backetBookKey = new BacketBookKey(backetId, bookId);
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);

		if (!optionalBacketBook.isPresent())
			return new ResponseEntity<>("The book is not in the backet", HttpStatus.CONFLICT);

		BacketBook backetBook = optionalBacketBook.get();

		int quantity = backetBook.getQuantity();
		quantity = quantity - 1;

		if (quantity > 0) {
			backetBook.setQuantity(quantity);
			backetBookRepository.save(backetBook);

			return new ResponseEntity<>("The quantity of the book in the cart was reduced by one", HttpStatus.OK);
		} else {
			backetBookRepository.delete(backetBook);
			return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
		}
	}

	@DeleteMapping("/deleteitem/{bookid}")
	@Transactional
	public ResponseEntity<?> deleteItemFromCurrentBacket(@PathVariable("bookid") Long bookId,
			Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();
		Long userId = user.getId();

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		Backet backet = currentBacketsOfUser.get(0);
		Long backetId = backet.getBacketid();

		Optional<Book> optionalBook = bookRepository.findById(bookId);

		if (!optionalBook.isPresent())
			return new ResponseEntity<>("Book cannot be found", HttpStatus.NOT_FOUND);

		BacketBookKey backetBookKey = new BacketBookKey(backetId, bookId);
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);

		if (!optionalBacketBook.isPresent())
			return new ResponseEntity<>("The book is not in the backet", HttpStatus.CONFLICT);

		BacketBook backetBook = optionalBacketBook.get();

		backetBookRepository.delete(backetBook);
		return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
	}

	@DeleteMapping("/clearbacket/{userid}")
	@Transactional
	public ResponseEntity<?> clearCurrentBacket(@PathVariable("userid") Long userId, Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalUser = userRepository.findByUsername(username);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User user = optionalUser.get();

		if (user.getId() != userId)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"You are not allowed to get someone else's info");

		List<Backet> currentBacketsOfUser = backetRepository.findCurrentByUserid(userId);

		if (currentBacketsOfUser.size() != 1)
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"There should be exactly 1 current backet for user. Actual quantity: "
							+ currentBacketsOfUser.size());

		Backet backet = currentBacketsOfUser.get(0);

		long deleted = backetBookRepository.deleteByBacket(backet);

		return new ResponseEntity<>(deleted + " records were deleted from current cart", HttpStatus.OK);
	}

	private void sendOrderInfoEmail(String username, String emailTo, Long orderId, String password)
			throws MessagingException, UnsupportedEncodingException {
		String toAddress = emailTo;
		String fromAddress = springMailUsername;
		String senderName = "No reply";
		String subject = "Your order information";
		String content = "Dear [[name]],<br><br>"
				+ "You have just made an order in our bookstore!<br> Using the following order number and password you can get your order information on the website:<br>"
				+ "<h4>ORDER NUMBER: [[ORDERID]]</h4>" + "<h4>ORDER PASSWORD: [[ORDERPASS]]</h4>"
				+ "If you have any questions or you want to change order information please contact us through the email spotted on the website footer<br><br>"
				+ "Thank you for choosing us,<br>" + "AXOS inc.";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[name]]", username);

		content = content.replace("[[ORDERID]]", orderId.toString());

		content = content.replace("[[ORDERPASS]]", password);

		helper.setText(content, true);

		mailSender.send(message);
	}
}
