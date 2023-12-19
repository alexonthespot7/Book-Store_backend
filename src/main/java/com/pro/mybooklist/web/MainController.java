package com.pro.mybooklist.web;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.httpforms.AddressInfo;
import com.pro.mybooklist.httpforms.BookInfo;
import com.pro.mybooklist.httpforms.BookQuantityInfo;
import com.pro.mybooklist.httpforms.NotUserAddressInfo;
import com.pro.mybooklist.httpforms.OrderInfo;
import com.pro.mybooklist.httpforms.OrderPasswordInfo;
import com.pro.mybooklist.httpforms.QuantityInfo;
import com.pro.mybooklist.httpforms.SenderInfo;
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
import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.QuantityOfBacket;
import com.pro.mybooklist.sqlforms.RawBookInfo;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

@CrossOrigin(origins = "*")
@RestController
public class MainController {
	private static final Logger log = LoggerFactory.getLogger(MainController.class);

	@Autowired
	private BookRepository repository;

	@Autowired
	private CategoryRepository crepository;

	@Autowired
	private UserRepository urepository;

	@Autowired
	private BacketRepository barepository;

	@Autowired
	private BacketBookRepository bbrepository;

	@Autowired
	private OrderRepository orepository;

	@Autowired
	private JavaMailSender mailSender;

	@GetMapping("/ordersbypassword")
	public @ResponseBody Order getOrderByIdAndPassword(@RequestBody BookInfo orderInfo) {
		if (orepository.findById(orderInfo.getBookid()).isPresent()) {
			Order order = orepository.findById(orderInfo.getBookid()).get();

			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(orderInfo.getPassword(), order.getPassword())) {
				return order;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	// method of creating backet for users without profile
	@PostMapping("/createbacket")
	public @ResponseBody BookInfo createBacketNoAuthentication() {
		String password = RandomStringUtils.random(15);
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);

		Backet backet = new Backet(hashPwd);
		barepository.save(backet);

		return new BookInfo(backet.getBacketid(), password);
	}

	// the method of adding book to the backet for anonyms users
	@PostMapping("addbook/{backetid}")
	public ResponseEntity<?> addBookToNonLoggedCart(@PathVariable("backetid") Long backetId,
			@RequestBody BookQuantityInfo bookQuantity) {
		Optional<Backet> optBacket = barepository.findById(backetId);

		if (optBacket.isPresent() && optBacket.get().getUser() == null) {
			Backet backet = optBacket.get();

			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(bookQuantity.getPassword(), backet.getPasswordHash())) {
				Optional<Book> optBook = repository.findById(bookQuantity.getBookid());

				if (optBook.isPresent()) {
					Book book = optBook.get();

					BacketBookKey bbKey = new BacketBookKey(backet.getBacketid(), book.getId());
					Optional<BacketBook> optBB = bbrepository.findById(bbKey);
					BacketBook backetBook;

					if (optBB.isPresent()) {
						backetBook = optBB.get();

						int quantity = backetBook.getQuantity();
						backetBook.setQuantity(quantity + bookQuantity.getQuantity());
					} else {
						backetBook = new BacketBook(bookQuantity.getQuantity(), backet, book);
					}

					bbrepository.save(backetBook);

					return new ResponseEntity<>("Book was added to cart successfully", HttpStatus.OK);
				} else {
					return new ResponseEntity<>("There is no such book" + bookQuantity.getBookid(),
							HttpStatus.BAD_REQUEST);
				}
			} else {
				return new ResponseEntity<>("The password is wrong!", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("There is no such backet or this backet is private", HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/booksids/{backetid}")
	public @ResponseBody List<Long> showIdsOfBooksByBacketid(@PathVariable("backetid") Long backetId) {
		if (barepository.findById(backetId).isPresent() && barepository.findById(backetId).get().getUser() == null) {
			return repository.findIdsOfBooksByBacketid(backetId);
		} else {
			return null;
		}
	}

	@RequestMapping(value = "/showcart", method = RequestMethod.GET)
	public @ResponseBody List<BookInCurrentCart> getBooksInBacketByIdAndPassword(@RequestBody BookInfo bookInfo) {
		Optional<Backet> optBacket = barepository.findById(bookInfo.getBookid());
		if (optBacket.isPresent() && optBacket.get().getUser() == null) {
			Backet backet = optBacket.get();

			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
			if (bc.matches(bookInfo.getPassword(), backet.getPasswordHash())) {
				return repository.findBooksInBacket(bookInfo.getBookid());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	@GetMapping("/booksinbacket/{orderid}")
	public @ResponseBody List<BookInCurrentCart> getBooksByOrderId(@PathVariable("orderid") Long orderId) {
		Optional<Order> optOrder = orepository.findById(orderId);

		if (optOrder.isPresent()) {
			return repository.findBooksInOrder(orderId);
		} else {
			return null;
		}
	}

	@RequestMapping("/users")
	@PreAuthorize("hasAuthority('ADMIN')")
	public @ResponseBody List<User> getUsers() {
		return (List<User>) urepository.findAll();
	}

	@RequestMapping("/users/{userid}/orders")
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public @ResponseBody List<Order> getOrdersByUserId(@PathVariable("userid") Long userId) {
		Optional<User> optUser = urepository.findById(userId);
		if (optUser.isPresent()) {
			return orepository.findByUserid(userId);
		} else {
			return null;
		}
	}

	@RequestMapping("/orders")
	@PreAuthorize("hasAuthority('ADMIN')")
	public @ResponseBody List<Order> getOrders() {
		return (List<Order>) orepository.findAll();
	}

	@RequestMapping(value = "/orders/{orderid}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public @ResponseBody Order getOrderById(@PathVariable("orderid") Long orderId) {
		if (orepository.findById(orderId).isPresent()) {
			Order order = orepository.findById(orderId).get();
			return order;
		} else {
			return null;
		}
	}

	@RequestMapping(value = "/updateorder/{orderid}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<?> updateOrder(@PathVariable("orderid") Long orderId, @RequestBody OrderInfo orderInfo)
			throws MessagingException, UnsupportedEncodingException {
		Optional<Order> optOrder = orepository.findById(orderId);

		if (optOrder.isPresent()) {
			Order order = optOrder.get();

			order.setFirstname(orderInfo.getFirstname());
			order.setLastname(orderInfo.getLastname());
			order.setCountry(orderInfo.getCountry());
			order.setCity(orderInfo.getCity());
			order.setStreet(orderInfo.getStreet());
			order.setPostcode(orderInfo.getPostcode());
			if (!order.getEmail().equals(orderInfo.getEmail())) {
				sendOrderEmailChanged(orderInfo.getFirstname(), orderInfo.getLastname(), orderInfo.getEmail(), orderId);
				order.setEmail(orderInfo.getEmail());
			}
			if (!order.getStatus().equals(orderInfo.getStatus())) {
				Backet backet = order.getBacket();
				if (backet.getUser() != null) {
					sendStatusChangeEmail(orderInfo.getFirstname(), orderInfo.getLastname(),
							backet.getUser().getEmail(), orderId, orderInfo.getStatus());
				}
				sendStatusChangeEmail(orderInfo.getFirstname(), orderInfo.getLastname(), orderInfo.getEmail(), orderId,
						orderInfo.getStatus());
				order.setStatus(orderInfo.getStatus());
			}
			orepository.save(order);
			return new ResponseEntity<>("Order Info was updated successfully", HttpStatus.OK);
		} else {
			return new ResponseEntity<>("The order with that id is unknown", HttpStatus.BAD_REQUEST);
		}
	}

	// Rest method to load user by id - Backend - DONE, Frontend - DONE
	@RequestMapping(value = "/users/{id}")
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public @ResponseBody User restUserById(@PathVariable("id") Long userId) {
		Optional<User> optUser = urepository.findById(userId);

		if (optUser.isPresent()) {
			return optUser.get();
		} else {
			return null;
		}
	}

	// Rest method to add book to the current user current backet - Backend - DONE,
	// Frontend - waiting for implementation
	@RequestMapping(value = "/additem/{bookid}", method = RequestMethod.POST)
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> addItemToBacket(@PathVariable("bookid") Long bookId,
			@RequestBody QuantityInfo quantityInfo, Authentication authentication) {

		if (authentication.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) authentication.getPrincipal();

			Optional<User> optUser = urepository.findByUsername(myUser.getUsername());

			if (optUser.isPresent()) {
				User user = optUser.get();
				List<Backet> optBackets = barepository.findCurrentByUserid(user.getId());

				if (optBackets.size() == 1) {
					Backet currentBacket = optBackets.get(0);
					Optional<Book> optBook = repository.findById(bookId);

					if (optBook.isPresent()) {
						Book book = optBook.get();
						BacketBookKey bbKey = new BacketBookKey(currentBacket.getBacketid(), book.getId());
						Optional<BacketBook> optBB = bbrepository.findById(bbKey);
						BacketBook backetBook;

						if (optBB.isPresent()) {
							backetBook = optBB.get();

							int quantity = backetBook.getQuantity();
							backetBook.setQuantity(quantity + quantityInfo.getQuantity());
						} else {
							backetBook = new BacketBook(quantityInfo.getQuantity(), currentBacket, book);
						}

						bbrepository.save(backetBook);

						return new ResponseEntity<>("Book was added to cart successfully", HttpStatus.OK);
					} else {
						return new ResponseEntity<>("Book cannot be found", HttpStatus.CONFLICT);
					}
				} else {
					return new ResponseEntity<>(
							"There is more or less than 1 current backet for user, quantity: " + optBackets.size(),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				return new ResponseEntity<>("Incorrect username", HttpStatus.UNAUTHORIZED);
			}
		} else {
			return new ResponseEntity<>("Authentication problems", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("/booksids")
	@PreAuthorize("isAuthenticated()")
	public @ResponseBody List<Long> showIdsOfBooksInCurrentCart(Authentication auth) {
		if (auth.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) auth.getPrincipal();
			Optional<User> optUser = urepository.findByUsername(myUser.getUsername());

			if (optUser.isPresent()) {
				return repository.findIdsOfBooksInCurrentCart(optUser.get().getId());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	// rest method of fetching current backet by user id, MAY DELETE LATER NOT
	// USEFUL RN DIDNT CONFIGURE IN WEB SEC
	@RequestMapping("/users/{userid}/backet")
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public @ResponseBody Backet currBacketByUser(@PathVariable("userid") Long userId) {
		return barepository.findCurrentByUserid(userId).size() == 1 ? barepository.findCurrentByUserid(userId).get(0)
				: null;
	}

	// the method to show current cart by userId - Backend - DONE, Frontend -
	// WAiting for implementation
	@RequestMapping(value = "/showcart/{userid}")
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public @ResponseBody List<BookInCurrentCart> showCurrentCart(@PathVariable("userid") Long userId) {
		return (List<BookInCurrentCart>) repository.findBooksInCurrentBacketByUserid(userId);
	}

	

	@RequestMapping("/getcurrtotal")
	@PreAuthorize("isAuthenticated()")
	public @ResponseBody TotalOfBacket getCurrentCartTotal(Authentication auth) {
		if (auth.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) auth.getPrincipal();
			Optional<User> optUser = urepository.findByUsername(myUser.getUsername());

			if (optUser.isPresent()) {
				return barepository.findTotalOfCurrentCart(optUser.get().getId());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@RequestMapping(value = "/gettotal", method = RequestMethod.POST)
	public @ResponseBody TotalOfBacket getTotalByBacketid(@RequestBody BookInfo bookInfo) {
		Optional<Backet> optBacket = barepository.findById(bookInfo.getBookid());

		if (optBacket.isPresent()) {
			Backet backet = optBacket.get();

			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
			if (bc.matches(bookInfo.getPassword(), backet.getPasswordHash())) {
				return barepository.findTotalOfBacket(bookInfo.getBookid());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@RequestMapping("/getordertotal/{orderid}")
	public @ResponseBody TotalOfBacket getTotalOfOrder(@PathVariable("orderid") Long orderId) {
		Optional<Order> order = orepository.findById(orderId);

		if (order.isPresent()) {
			return barepository.findTotalOfOrder(orderId);
		} else {
			return null;
		}
	}

	// Rest method to clear current user backet - BACKEND - DONE, FRONTEND - waiting
	// for implementation
	@RequestMapping("/clearbacket/{userid}")
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	@Transactional
	public ResponseEntity<?> clearCurrentBacket(@PathVariable("userid") Long userId) {
		if (barepository.findCurrentByUserid(userId).size() == 1) {
			Backet backet = barepository.findCurrentByUserid(userId).get(0);

			long deleted = bbrepository.deleteByBacket(backet);

			return new ResponseEntity<>(deleted + ", records were deleted from current cart.", HttpStatus.OK);
		} else {
			return new ResponseEntity<>(
					"Cannot find current cart for this user. Or there is more than one current cart what is forbidden by database logical design",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/reduceitem/{backetid}", method = RequestMethod.POST)
	@Transactional
	public ResponseEntity<?> rediceItemNonLogged(@PathVariable("backetid") Long backetId,
			@RequestBody BookInfo bookInfo) {
		Optional<Backet> optBacket = barepository.findById(backetId);

		if (optBacket.isPresent() && optBacket.get().getUser() == null) {
			Backet backet = optBacket.get();
			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(bookInfo.getPassword(), backet.getPasswordHash())) {
				Optional<Book> optBook = repository.findById(bookInfo.getBookid());

				if (optBook.isPresent()) {
					Book book = optBook.get();

					BacketBookKey bbKey = new BacketBookKey(backet.getBacketid(), book.getId());
					Optional<BacketBook> optBB = bbrepository.findById(bbKey);
					BacketBook backetBook;

					if (optBB.isPresent()) {
						backetBook = optBB.get();

						int quantity = backetBook.getQuantity();
						quantity = quantity - 1;

						if (quantity > 0) {
							backetBook.setQuantity(quantity);
							bbrepository.save(backetBook);

							return new ResponseEntity<>("The quantity of the book in the cart was reduced by one",
									HttpStatus.OK);
						} else {
							bbrepository.delete(backetBook);
							return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
						}
					} else {
						return new ResponseEntity<>("Cannot find the record in backet_book table", HttpStatus.CONFLICT);
					}
				} else {
					return new ResponseEntity<>("There is no such book", HttpStatus.BAD_REQUEST);
				}
			} else {
				return new ResponseEntity<>("The password is wrong!", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("There is no such backet or the backet is private", HttpStatus.BAD_REQUEST);
		}
	}

	// Rest method for reducing book quantity in current cart BACKEND - DONE,
	// FRONTEND - waiting for impl
	@RequestMapping(value = "/reduceitem/{bookid}")
	@PreAuthorize("isAuthenticated()")
	@Transactional
	public ResponseEntity<?> reduceItem(@PathVariable("bookid") Long bookId, Authentication authentication) {
		if (authentication.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) authentication.getPrincipal();

			Optional<User> optUser = urepository.findByUsername(myUser.getUsername());

			if (optUser.isPresent()) {
				User user = optUser.get();
				List<Backet> optBackets = barepository.findCurrentByUserid(user.getId());

				if (optBackets.size() == 1) {
					Backet currentBacket = optBackets.get(0);
					Optional<Book> optBook = repository.findById(bookId);

					if (optBook.isPresent()) {
						Book book = optBook.get();
						BacketBookKey bbKey = new BacketBookKey(currentBacket.getBacketid(), book.getId());
						Optional<BacketBook> optBB = bbrepository.findById(bbKey);
						BacketBook backetBook;

						if (optBB.isPresent()) {
							backetBook = optBB.get();

							int quantity = backetBook.getQuantity();
							quantity = quantity - 1;

							if (quantity > 0) {
								backetBook.setQuantity(quantity);
								bbrepository.save(backetBook);

								return new ResponseEntity<>("The quantity of the book in the cart was reduced by one",
										HttpStatus.OK);
							} else {
								bbrepository.delete(backetBook);
								return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);
							}
						} else {
							return new ResponseEntity<>("Cannot find the record in backet_book table",
									HttpStatus.CONFLICT);
						}

					} else {
						return new ResponseEntity<>("Book cannot be found", HttpStatus.CONFLICT);
					}
				} else {
					return new ResponseEntity<>(
							"There is more or less than 1 current backet for user, quantity: " + optBackets.size(),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				return new ResponseEntity<>("Incorrect username", HttpStatus.UNAUTHORIZED);
			}
		} else {
			return new ResponseEntity<>("Authentication problems", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/deletebook/{backetid}", method = RequestMethod.DELETE)
	@Transactional
	public ResponseEntity<?> deleteBookNonLogged(@PathVariable("backetid") Long backetId,
			@RequestBody BookInfo bookInfo) {
		Optional<Backet> optBacket = barepository.findById(backetId);

		if (optBacket.isPresent() && optBacket.get().getUser() == null) {
			Backet backet = optBacket.get();
			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(bookInfo.getPassword(), backet.getPasswordHash())) {
				Optional<Book> optBook = repository.findById(bookInfo.getBookid());

				if (optBook.isPresent()) {
					Book book = optBook.get();

					BacketBookKey bbKey = new BacketBookKey(backet.getBacketid(), book.getId());
					Optional<BacketBook> optBB = bbrepository.findById(bbKey);
					BacketBook backetBook;

					if (optBB.isPresent()) {
						backetBook = optBB.get();

						bbrepository.delete(backetBook);
						return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);

					} else {
						return new ResponseEntity<>("Cannot find the record in backet_book table", HttpStatus.CONFLICT);
					}
				} else {
					return new ResponseEntity<>("There is no such book", HttpStatus.BAD_REQUEST);
				}
			} else {
				return new ResponseEntity<>("The password is wrong!", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("The backet does not exist or is prviate", HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/deleteitem/{bookid}")
	@PreAuthorize("isAuthenticated()")
	@Transactional
	public ResponseEntity<?> deleteItem(@PathVariable("bookid") Long bookId, Authentication authentication) {
		if (authentication.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) authentication.getPrincipal();

			Optional<User> optUser = urepository.findByUsername(myUser.getUsername());

			if (optUser.isPresent()) {
				User user = optUser.get();
				List<Backet> optBackets = barepository.findCurrentByUserid(user.getId());

				if (optBackets.size() == 1) {
					Backet currentBacket = optBackets.get(0);
					Optional<Book> optBook = repository.findById(bookId);

					if (optBook.isPresent()) {
						Book book = optBook.get();
						BacketBookKey bbKey = new BacketBookKey(currentBacket.getBacketid(), book.getId());
						Optional<BacketBook> optBB = bbrepository.findById(bbKey);
						BacketBook backetBook;

						if (optBB.isPresent()) {
							backetBook = optBB.get();

							bbrepository.delete(backetBook);
							return new ResponseEntity<>("The book was deleted from the cart", HttpStatus.OK);

						} else {
							return new ResponseEntity<>("Cannot find the record in backet_book table",
									HttpStatus.CONFLICT);
						}

					} else {
						return new ResponseEntity<>("Book cannot be found", HttpStatus.CONFLICT);
					}
				} else {
					return new ResponseEntity<>(
							"There is more or less than 1 current backet for user, quantity: " + optBackets.size(),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				return new ResponseEntity<>("Incorrect username", HttpStatus.UNAUTHORIZED);
			}
		} else {
			return new ResponseEntity<>("Authentication problems", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/makesale", method = RequestMethod.POST)
	public @ResponseBody OrderPasswordInfo makeSaleNotLogged(@RequestBody NotUserAddressInfo addressInfo)
			throws MessagingException, UnsupportedEncodingException {
		Optional<Backet> optBacket = barepository.findById(addressInfo.getBacketid());

		if (optBacket.isPresent()) {
			Backet backet = optBacket.get();
			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(addressInfo.getPassword(), backet.getPasswordHash())) {
				if (bbrepository.findByBacket(backet).size() > 0) {
					backet.setCurrent(false);
					String passwordRandom = RandomStringUtils.random(15);
					String hashPwd = bc.encode(passwordRandom);

					Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(),
							addressInfo.getCountry(), addressInfo.getCity(), addressInfo.getStreet(),
							addressInfo.getPostcode(), addressInfo.getEmail(), backet, addressInfo.getNote(), hashPwd);
					orepository.save(order);
					barepository.save(backet);
					sendOrderInfoEmail(addressInfo.getFirstname() + " " + addressInfo.getLastname(),
							addressInfo.getEmail(), order.getOrderid(), passwordRandom);

					OrderPasswordInfo orderPassword = new OrderPasswordInfo(order.getOrderid(), passwordRandom);

					return orderPassword;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	// Rest method to handle a sale (make cart from current to not current (saled))
	// BACK - DONE, FRONT - WAITNIG FOR IMPL
	@RequestMapping(value = "/makesale/{userid}", method = RequestMethod.POST)
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public @ResponseBody OrderPasswordInfo makeSale(@PathVariable("userid") Long userId,
			@RequestBody AddressInfo addressInfo) throws MessagingException, UnsupportedEncodingException {
		if (barepository.findCurrentByUserid(userId).size() == 1) {
			Backet backet = barepository.findCurrentByUserid(userId).get(0);
			Optional<User> optUser = urepository.findById(userId);

			if (optUser.isPresent()) {
				List<BacketBook> booksInBacket = bbrepository.findByBacket(backet);

				if (booksInBacket.size() > 0) {
					User user = optUser.get();

					String passwordRandom = RandomStringUtils.random(15);
					BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
					String hashPwdOrder = bc.encode(passwordRandom);

					backet.setCurrent(false);
					Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(),
							addressInfo.getCountry(), addressInfo.getCity(), addressInfo.getStreet(),
							addressInfo.getPostcode(), addressInfo.getEmail(), backet, addressInfo.getNote(),
							hashPwdOrder);
					orepository.save(order);
					barepository.save(backet);

					sendOrderInfoEmail(user.getUsername(), user.getEmail(), order.getOrderid(), passwordRandom);
					if (!addressInfo.getEmail().equals(user.getEmail())) {
						sendOrderInfoEmail(user.getUsername(), addressInfo.getEmail(), order.getOrderid(),
								passwordRandom);
					}

					barepository.save(new Backet(true, user));

					OrderPasswordInfo orderPassword = new OrderPasswordInfo(order.getOrderid(), passwordRandom);

					return orderPassword;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	// Rest method to get list of lists of past sales (carts that user bought)
	// BACKEND - DONE, FRONT - NOT YET, SEC - CONFIGured
	@RequestMapping("/pastsales")
	@PreAuthorize("isAuthenticated()")
	public @ResponseBody List<List<BookInCurrentCart>> listPastSales(Authentication auth) {
		if (auth.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) auth.getPrincipal();

			List<Long> nonCurrentIds = barepository.findNotCurrentByUserid(myUser.getId());

			List<List<BookInCurrentCart>> nestedList = new ArrayList<List<BookInCurrentCart>>();
			for (int i = 0; i < nonCurrentIds.size(); i++) {
				nestedList.add(repository.findBooksInBacket(nonCurrentIds.get(i)));
			}
			return nestedList;
		} else {
			return null;
		}
	}

	@RequestMapping("/topsales")
	public @ResponseBody List<RawBookInfo> listTopSales() {
		return repository.findTopSales();
	}

	@RequestMapping(value = "/checkordernumber", method = RequestMethod.POST)
	public ResponseEntity<?> checkOrderNumber(@RequestBody BookInfo orderInfo) {
		if (orepository.findById(orderInfo.getBookid()).isPresent()) {
			Order order = orepository.findById(orderInfo.getBookid()).get();

			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

			if (bc.matches(orderInfo.getPassword(), order.getPassword())) {
				return new ResponseEntity<>("The order number and password are correct", HttpStatus.OK);
			} else {
				return new ResponseEntity<>("The order password is incorrect", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("The order with that number DOES NOT exist", HttpStatus.BAD_REQUEST);
		}
	}

	// not in use!!!!
	@RequestMapping(value = "/getcurrquantity")
	@PreAuthorize("isAuthenticated()")
	public @ResponseBody QuantityOfBacket getCurrentCartQuantity(Authentication auth) {
		if (auth.getPrincipal().getClass().toString().equals("class com.pro.mybooklist.MyUser")) {
			MyUser myUser = (MyUser) auth.getPrincipal();
			Optional<User> optUser = urepository.findByUsername(myUser.getUsername());

			if (optUser.isPresent()) {
				return barepository.findQuantityInCurrent(optUser.get().getId());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	// Method not in use for bookstore project, but is used for my personal website
	@RequestMapping(value = "/sendmail", method = RequestMethod.POST)
	public ResponseEntity<?> sendSelfEmail(@RequestBody SenderInfo senderInfo)
			throws MessagingException, UnsupportedEncodingException {
		sendEmailFromWebsite(senderInfo);
		return new ResponseEntity<>("Email was sent successfully", HttpStatus.OK);
	}

	private void sendOrderInfoEmail(String username, String emailTo, Long orderId, String password)
			throws MessagingException, UnsupportedEncodingException {
		String toAddress = emailTo;
		String fromAddress = "aleksei.application.noreply@gmail.com";
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

	private void sendOrderEmailChanged(String firstname, String lastname, String emailTo, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		String toAddress = emailTo;
		String fromAddress = "aleksei.application.noreply@gmail.com";
		String senderName = "No reply";
		String subject = "Your order email changed";
		String content = "Dear [[name]],<br><br>"
				+ "Your order email address was changed!<br>From now on you will be receiving all information via this email address<br> Using the following order number you can get your order information on the website:<br>"
				+ "<h4>ORDER NUMBER: [[ORDERID]]</h4>"
				+ "If you have any questions or you want to change inforamtion please contact us through the email spotted on the website footer<br><br>"
				+ "Thank you for choosing us,<br>" + "AXOS inc.";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[name]]", firstname + ' ' + lastname);

		content = content.replace("[[ORDERID]]", orderId.toString());

		helper.setText(content, true);

		mailSender.send(message);
	}

	private void sendStatusChangeEmail(String firstname, String lastname, String emailTo, Long orderId, String status)
			throws MessagingException, UnsupportedEncodingException {
		String toAddress = emailTo;
		String fromAddress = "aleksei.application.noreply@gmail.com";
		String senderName = "No reply";
		String subject = "Your order status has changed";
		String content = "Dear [[name]],<br><br>"
				+ "Your order status has changed!<br>Your current order status is now: <h5>[[status]]</h5><br> Using the following order number you can get your order information on the website:<br>"
				+ "<h4>ORDER NUMBER: [[ORDERID]]</h4>"
				+ "If you have any questions or you want to change inforamtion please contact us through the email spotted on the website footer<br><br>"
				+ "Thank you for choosing us,<br>" + "AXOS inc.";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[name]]", firstname + ' ' + lastname);

		content = content.replace("[[status]]", status);

		content = content.replace("[[ORDERID]]", orderId.toString());

		helper.setText(content, true);

		mailSender.send(message);
	}

	private void sendEmailFromWebsite(SenderInfo senderInfo) throws MessagingException, UnsupportedEncodingException {
		String toAddress = "aleksei.shevelenkov@gmail.com";
		String fromAddress = "aleksei.application.noreply@gmail.com";
		String senderName = "Your website";
		String subject = "You received a new message from website";
		String content = "Hi, Aleksei!<br><br>"
				+ "You have just recieved a new message from [[name]] via your personal website:<br>"
				+ "<p>[[message]]</p><br>" + "The email to get in touch: [[email]]<br><br>" + "Thank you,<br>"
				+ "AXOS inc.";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(fromAddress, senderName);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[name]]", senderInfo.getName());

		content = content.replace("[[message]]", senderInfo.getMessage());

		content = content.replace("[[email]]", senderInfo.getEmail());

		helper.setText(content, true);

		mailSender.send(message);
	}

}
