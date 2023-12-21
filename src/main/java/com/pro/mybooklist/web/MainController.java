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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.httpforms.AddressInfo;
import com.pro.mybooklist.httpforms.BacketInfo;
import com.pro.mybooklist.httpforms.BookQuantityInfo;
import com.pro.mybooklist.httpforms.AddressInfoNoAuthentication;
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

	@Value("${spring.mail.username}")
	private String springMailUsername;

	@RequestMapping("/users")
	@PreAuthorize("hasAuthority('ADMIN')")
	public @ResponseBody List<User> getUsers() {
		return (List<User>) urepository.findAll();
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

	// Method not in use for bookstore project, but is used for my personal website
	@RequestMapping(value = "/sendmail", method = RequestMethod.POST)
	public ResponseEntity<?> sendSelfEmail(@RequestBody SenderInfo senderInfo)
			throws MessagingException, UnsupportedEncodingException {
		sendEmailFromWebsite(senderInfo);
		return new ResponseEntity<>("Email was sent successfully", HttpStatus.OK);
	}

	private void sendOrderEmailChanged(String firstname, String lastname, String emailTo, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		String toAddress = emailTo;
		String fromAddress = springMailUsername;
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
		String fromAddress = springMailUsername;
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
		String fromAddress = springMailUsername;
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
