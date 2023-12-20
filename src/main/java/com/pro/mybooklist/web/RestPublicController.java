package com.pro.mybooklist.web;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import com.pro.mybooklist.model.BacketBookKey;
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
import com.pro.mybooklist.service.AuthenticationService;
import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.RawBookInfo;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@CrossOrigin(origins = "*")
@RestController
public class RestPublicController {
	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private CategoryRepository categoryRepository;

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
	private AuthenticationService jwtService;

	@Autowired
	AuthenticationManager authenticationManager;

	@Value("${spring.mail.username}")
	private String springMailUsername;

	@GetMapping("/login")
	public ResponseEntity<?> getToken(@RequestBody AccountCredentials credentials) {
		String emailOrUsername = credentials.getUsername();

		Optional<User> optionalUser = userRepository.findByEmail(emailOrUsername);

		if (!optionalUser.isPresent()) {
			optionalUser = userRepository.findByUsername(emailOrUsername);
			if (!optionalUser.isPresent())
				return new ResponseEntity<>("Wrong username/email", HttpStatus.NOT_FOUND);
		}

		User user = optionalUser.get();

		if (!user.isAccountVerified()) {
			if (!this.springMailUsername.equals("default_value"))
				return new ResponseEntity<>("Email is not verified", HttpStatus.CONFLICT);
			user.setAccountVerified(true);
			user.setVerificationCode(null);

			Long userId = user.getId();
			List<Backet> currentBackets = backetRepository.findCurrentByUserid(userId);

			if (currentBackets.size() > 0)
				return new ResponseEntity<>("User shouldn't have had a backet before verification",
						HttpStatus.INTERNAL_SERVER_ERROR);

			backetRepository.save(new Backet(true, user));
		}

		String username = user.getUsername();
		String password = credentials.getPassword();

		UsernamePasswordAuthenticationToken authenticationCredentials = new UsernamePasswordAuthenticationToken(
				username, password);

		Authentication authenticationInstance = authenticationManager.authenticate(authenticationCredentials);

		String authenticatedUsername = authenticationInstance.getName();

		String jwts = jwtService.getToken(authenticatedUsername);

		Optional<User> optionalAuthenticatedUser = userRepository.findByUsername(authenticatedUsername);

		if (!optionalAuthenticatedUser.isPresent())
			return new ResponseEntity<>("Wrong password", HttpStatus.UNAUTHORIZED);

		User authenticatedUser = optionalAuthenticatedUser.get();

		return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts)
				.header(HttpHeaders.ALLOW, authenticatedUser.getRole())
				.header(HttpHeaders.HOST, authenticatedUser.getId().toString())
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization, Allow", "Host").build();
	}

	@PostMapping("/signup")
	public ResponseEntity<?> signUp(@RequestBody SignupCredentials creds)
			throws UnsupportedEncodingException, MessagingException {

		String username = creds.getUsername();
		String email = creds.getEmail();

		Optional<User> optionalUser = userRepository.findByEmail(email);
		if (optionalUser.isPresent())
			return new ResponseEntity<>("Email is already in use", HttpStatus.NOT_ACCEPTABLE);

		optionalUser = userRepository.findByUsername(username);
		if (optionalUser.isPresent())
			return new ResponseEntity<>("Username is already in use", HttpStatus.CONFLICT);

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(creds.getPassword());
		String randomCode = RandomStringUtils.random(64);

		User newUser = new User(creds.getFirstname(), creds.getLastname(), username, hashPwd, "USER", email, randomCode,
				false);
		userRepository.save(newUser);

		try {
			this.sendVerificationEmail(newUser);

			return new ResponseEntity<>("We sent verification link to your email address :)", HttpStatus.OK);
		} catch (MailAuthenticationException e) {
			newUser.setAccountVerified(true);
			newUser.setVerificationCode(null);
			userRepository.save(newUser);

			return new ResponseEntity<>("Registration went well, you can login now", HttpStatus.ACCEPTED);
		}
	}

	@PutMapping("/verify")
	public ResponseEntity<?> verifyUser(@RequestBody TokenInfo tokenInfo) {
		String token = tokenInfo.getToken();

		Optional<User> optionalUser = userRepository.findByVerificationCode(token);

		if (!optionalUser.isPresent())
			return new ResponseEntity<>("Verification code is incorrect", HttpStatus.NOT_FOUND);

		User user = optionalUser.get();

		if (user.isAccountVerified())
			return new ResponseEntity<>("User is already verified", HttpStatus.CONFLICT);

		Long userId = user.getId();
		List<Backet> currentBackets = backetRepository.findCurrentByUserid(userId);
		if (currentBackets.size() > 0)
			return new ResponseEntity<>("User shouldn't have had a backet before verification",
					HttpStatus.INTERNAL_SERVER_ERROR);

		user.setVerificationCode(null);
		user.setAccountVerified(true);
		userRepository.save(user);
		backetRepository.save(new Backet(true, user));

		return new ResponseEntity<>("Verification went well", HttpStatus.OK);
	}

	@PutMapping("/resetpassword")
	public ResponseEntity<?> resetPassword(@RequestBody EmailInfo emailInfo)
			throws UnsupportedEncodingException, MessagingException {
		String email = emailInfo.getEmail();
		Optional<User> optionalUser = userRepository.findByEmail(email);

		if (!optionalUser.isPresent())
			return new ResponseEntity<>("User with this email (" + emailInfo.getEmail() + ") doesn't exist",
					HttpStatus.NOT_FOUND);

		User user = optionalUser.get();

		if (!user.isAccountVerified()) {
			if (!this.springMailUsername.equals("default_value"))
				return new ResponseEntity<>("User with this email (" + emailInfo.getEmail() + ") is not verified",
						HttpStatus.CONFLICT);

			user.setAccountVerified(true);
			user.setVerificationCode(null);
			userRepository.save(user);

			Long userId = user.getId();
			List<Backet> currentBackets = backetRepository.findCurrentByUserid(userId);

			if (currentBackets.size() > 0)
				return new ResponseEntity<>("User shouldn't have had a backet before verification",
						HttpStatus.INTERNAL_SERVER_ERROR);

			backetRepository.save(new Backet(true, user));
		}

		String password = RandomStringUtils.random(15);

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);

		try {
			this.sendPasswordEmail(user, password);
			user.setPassword(hashPwd);
			userRepository.save(user);

			return new ResponseEntity<>("A temporary password was sent to your email address", HttpStatus.OK);

		} catch (MailAuthenticationException e) {
			return new ResponseEntity<>("The email service is not in use now", HttpStatus.NOT_IMPLEMENTED);
		}
	}

	@GetMapping("/books")
	public @ResponseBody List<Book> getBooks() {
		return (List<Book>) bookRepository.findAll();
	}

	@GetMapping("/categories")
	public @ResponseBody List<Category> getCategories() {
		return (List<Category>) categoryRepository.findAll();
	}

	@GetMapping("/books/{id}")
	public @ResponseBody Optional<Book> getBookById(@PathVariable("id") Long bookId) {
		return bookRepository.findById(bookId);
	}

	@GetMapping("/booksbycategory")
	public @ResponseBody List<Book> getBooksByCategory(@RequestBody Category category) {
		return bookRepository.findByCategory(category);
	}

	@GetMapping("/topsales")
	public @ResponseBody List<RawBookInfo> getTopSales() {
		return bookRepository.findTopSales();
	}

	@GetMapping("/orderbypassword")
	public @ResponseBody Order getOrderByIdAndPassword(@RequestBody OrderPasswordInfo orderInfo) {
		if (orderRepository.findById(orderInfo.getOrderid()).isPresent()) {
			Order order = orderRepository.findById(orderInfo.getOrderid()).get();

			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(orderInfo.getPassword(), order.getPassword())) {
				return order;
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong password");
			}
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The order wasn't found by id");
		}
	}

	@GetMapping("/booksids/{backetid}")
	public @ResponseBody List<Long> getIdsOfBooksByBacketid(@PathVariable("backetid") Long backetId) {
		Optional<Backet> optionalBacket = backetRepository.findById(backetId);

		if (!optionalBacket.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The backet wasn't found by id");

		Backet backet = optionalBacket.get();

		User user = backet.getUser();

		if (user != null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The backet has its user owner");

		return bookRepository.findIdsOfBooksByBacketid(backetId);
	}

	@GetMapping("/showcart")
	public @ResponseBody List<BookInCurrentCart> getBooksInBacketByIdAndPassword(@RequestBody BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();
		Optional<Backet> optionalBacket = backetRepository.findById(backetId);
		if (!optionalBacket.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The backet wasn't found by id");

		Backet backet = optionalBacket.get();
		User backetOwner = backet.getUser();

		if (backetOwner != null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The backet is private");

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		if (!bc.matches(backetInfo.getPassword(), backet.getPasswordHash()))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is wrong");

		return bookRepository.findBooksInBacket(backetInfo.getId());
	}

	@GetMapping("/booksinorder/{orderid}")
	public @ResponseBody List<BookInCurrentCart> getBooksByOrderId(@PathVariable("orderid") Long orderId) {
		Optional<Order> optionalOrder = orderRepository.findById(orderId);

		if (!optionalOrder.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The order wasn't found");

		return bookRepository.findBooksInOrder(orderId);
	}

	@GetMapping("/totalofbacket")
	public @ResponseBody TotalOfBacket getTotalByBacketId(@RequestBody BacketInfo backetInfo) {
		Long backetId = backetInfo.getId();

		Optional<Backet> optionalBacket = backetRepository.findById(backetId);

		if (!optionalBacket.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The backet wasn't found");

		Backet backet = optionalBacket.get();

		User backetOwner = backet.getUser();

		if (backetOwner != null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The backet is private");

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		if (!bc.matches(backetInfo.getPassword(), backet.getPasswordHash()))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is wrong");

		TotalOfBacket totalOfBacket = backetRepository.findTotalOfBacket(backetId);

		return totalOfBacket;
	}

	@GetMapping("/getordertotal/{orderid}")
	public @ResponseBody TotalOfBacket getTotalOfOrderByOrderId(@PathVariable("orderid") Long orderId) {
		Optional<Order> optionalOrder = orderRepository.findById(orderId);

		if (!optionalOrder.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The order wasn't found");

		TotalOfBacket totalOfOrder = backetRepository.findTotalOfOrder(orderId);
		return totalOfOrder;
	}

	@GetMapping("/checkordernumber")
	public ResponseEntity<?> checkOrderNumber(@RequestBody OrderPasswordInfo orderInfo) {
		Long orderId = orderInfo.getOrderid();
		Optional<Order> optionalOrder = orderRepository.findById(orderId);

		if (!optionalOrder.isPresent())
			return new ResponseEntity<>("The order with that number DOES NOT exist", HttpStatus.NOT_FOUND);

		Order order = optionalOrder.get();

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		if (!bc.matches(orderInfo.getPassword(), order.getPassword()))
			return new ResponseEntity<>("The order password is incorrect", HttpStatus.BAD_REQUEST);

		return new ResponseEntity<>("The order number and password are correct", HttpStatus.OK);

	}

	@PostMapping("/createbacket")
	public @ResponseBody BacketInfo createBacketNoAuthentication() {
		String password = RandomStringUtils.random(15);
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);

		Backet backet = new Backet(hashPwd);
		backetRepository.save(backet);

		return new BacketInfo(backet.getBacketid(), password);
	}

	@PostMapping("/addbook/{backetid}")
	public ResponseEntity<?> addBookToCartNoAuthentication(@PathVariable("backetid") Long backetId,
			@RequestBody BookQuantityInfo bookQuantity) {
		Optional<Backet> optionalBacket = backetRepository.findById(backetId);

		if (!optionalBacket.isPresent())
			return new ResponseEntity<>("There is no such backet", HttpStatus.NOT_FOUND);

		Backet backet = optionalBacket.get();

		if (backet.getUser() != null)
			return new ResponseEntity<>("This backet is private", HttpStatus.UNAUTHORIZED);

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

		if (!bc.matches(bookQuantity.getPassword(), backet.getPasswordHash()))
			return new ResponseEntity<>("The password is wrong", HttpStatus.BAD_REQUEST);

		if (!backet.isCurrent())
			return new ResponseEntity<>("You can't change not current backet", HttpStatus.CONFLICT);

		Optional<Book> optionalBook = bookRepository.findById(bookQuantity.getBookid());

		if (!optionalBook.isPresent())
			return new ResponseEntity<>("There is no such book", HttpStatus.NOT_FOUND);

		Book book = optionalBook.get();

		BacketBookKey backetBookKey = new BacketBookKey(backet.getBacketid(), book.getId());
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);
		BacketBook backetBook;

		if (optionalBacketBook.isPresent()) {
			backetBook = optionalBacketBook.get();

			int quantity = backetBook.getQuantity();
			backetBook.setQuantity(quantity + bookQuantity.getQuantity());
		} else {
			backetBook = new BacketBook(bookQuantity.getQuantity(), backet, book);
		}

		backetBookRepository.save(backetBook);

		return new ResponseEntity<>("Book was added to cart successfully", HttpStatus.OK);
	}

	@PostMapping("/makesale")
	public @ResponseBody OrderPasswordInfo makeSaleNoAuthentication(
			@RequestBody AddressInfoNoAuthentication addressInfo)
			throws MessagingException, UnsupportedEncodingException {
		Optional<Backet> optionalBacket = backetRepository.findById(addressInfo.getBacketid());

		if (!optionalBacket.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The backet wasn't found by id");

		Backet backet = optionalBacket.get();

		List<BacketBook> backetBooksInBacket = backetBookRepository.findByBacket(backet);
		if (backetBooksInBacket.size() == 0)
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "The backet is empty");

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		if (!bc.matches(addressInfo.getPassword(), backet.getPasswordHash()))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong Password");

		if (!backet.isCurrent())
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You can't change not current Backet");

		backet.setCurrent(false);
		String passwordRandom = RandomStringUtils.random(15);
		String hashPwd = bc.encode(passwordRandom);

		Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(), addressInfo.getCountry(),
				addressInfo.getCity(), addressInfo.getStreet(), addressInfo.getPostcode(), addressInfo.getEmail(),
				backet, addressInfo.getNote(), hashPwd);
		orderRepository.save(order);
		backetRepository.save(backet);

		OrderPasswordInfo orderPassword = new OrderPasswordInfo(order.getOrderid(), passwordRandom);

		try {
			this.sendOrderInfoEmail(addressInfo.getFirstname() + " " + addressInfo.getLastname(),
					addressInfo.getEmail(), order.getOrderid(), passwordRandom);
			return orderPassword;
		} catch (MailAuthenticationException e) {
			return orderPassword;
		}

	}

	@PutMapping("/reduceitemnoauth/{backetid}")
	@Transactional
	public ResponseEntity<?> reduceItemNoAuthentication(@PathVariable("backetid") Long backetId,
			@RequestBody BacketInfo backetInfoWithBookId) {
		Optional<Backet> optionalBacket = backetRepository.findById(backetId);

		if (!optionalBacket.isPresent())
			return new ResponseEntity<>("Backet wasn't found by id", HttpStatus.NOT_FOUND);

		Backet backet = optionalBacket.get();

		User backetOwner = backet.getUser();

		if (backetOwner != null)
			return new ResponseEntity<>("The backet is private", HttpStatus.UNAUTHORIZED);

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

		if (!bc.matches(backetInfoWithBookId.getPassword(), backet.getPasswordHash()))
			return new ResponseEntity<>("The password is wrong", HttpStatus.BAD_REQUEST);

		if (!backet.isCurrent())
			return new ResponseEntity<>("You can't change not current backet", HttpStatus.CONFLICT);

		// In this case the backet info has id of the book that needs to be reduced and
		// the password of the backet
		Long bookId = backetInfoWithBookId.getId();
		Optional<Book> optionalBook = bookRepository.findById(bookId);

		if (!optionalBook.isPresent())
			return new ResponseEntity<>("There is no such book", HttpStatus.NOT_FOUND);

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

	@DeleteMapping("/deletebook/{backetid}")
	@Transactional
	public ResponseEntity<?> deleteBookNoAuthentication(@PathVariable("backetid") Long backetId,
			@RequestBody BacketInfo backetInfoWithBookId) {
		Optional<Backet> optionalBacket = backetRepository.findById(backetId);

		if (!optionalBacket.isPresent())
			return new ResponseEntity<>("The backet wasn't found", HttpStatus.NOT_FOUND);

		Backet backet = optionalBacket.get();
		User backetOwner = backet.getUser();

		if (backetOwner != null)
			return new ResponseEntity<>("The backet is private", HttpStatus.UNAUTHORIZED);

		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

		if (!bc.matches(backetInfoWithBookId.getPassword(), backet.getPasswordHash()))
			return new ResponseEntity<>("The password is wrong", HttpStatus.BAD_REQUEST);

		if (!backet.isCurrent())
			return new ResponseEntity<>("You can't change not current backet", HttpStatus.CONFLICT);

		Long bookId = backetInfoWithBookId.getId();

		Optional<Book> optionalBook = bookRepository.findById(bookId);

		if (!optionalBook.isPresent())
			return new ResponseEntity<>("The book wasn't found", HttpStatus.NOT_FOUND);

		BacketBookKey backetBookKey = new BacketBookKey(backetId, bookId);
		Optional<BacketBook> optionalBacketBook = backetBookRepository.findById(backetBookKey);

		if (!optionalBacketBook.isPresent())
			return new ResponseEntity<>("The book is not in the backet", HttpStatus.CONFLICT);

		BacketBook backetBook = optionalBacketBook.get();

		backetBookRepository.delete(backetBook);
		return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
	}

	private void sendVerificationEmail(User user) throws MessagingException, UnsupportedEncodingException {
		String toAddress = user.getEmail();
		String fromAddress = "aleksei.application.noreply@gmail.com";
		String senderName = "No reply";
		String subject = "Bookstore verification link";
		String content = "Dear [[name]],<br><br>"
				+ "This is the automatically generated message, please don't reply. To verify your bookstore account click the link below:<br><br>"
				+ "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY BOOKSTORE ACCOUNT</a></h3>" + "Thank you,<br>"
				+ "AXOS inc.";
		String endpoint = "/?token=";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[name]]", user.getUsername());
		String mainURL = "https://bookstore-axos.netlify.app" + endpoint + user.getVerificationCode();

		content = content.replace("[[URL]]", mainURL);

		helper.setText(content, true);

		mailSender.send(message);
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
	
	private void sendPasswordEmail(User user, String password) throws MessagingException, UnsupportedEncodingException {
		String toAddress = user.getEmail();
		String fromAddress = "aleksei.application.noreply@gmail.com";
		String senderName = "No reply";
		String subject = "Reset password";
		String content = "Dear [[name]],<br><br>" + "Here is your new password for your bookstore account:<br><br>"
				+ "<h3>[[PASSWORD]]</h3>" + "Thank you,<br>" + "AXOS inc.";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[name]]", user.getUsername());

		content = content.replace("[[PASSWORD]]", password);

		helper.setText(content, true);

		mailSender.send(message);
	}

}
