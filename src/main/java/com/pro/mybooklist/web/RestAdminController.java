package com.pro.mybooklist.web;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.httpforms.OrderInfo;
import com.pro.mybooklist.httpforms.RoleInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@CrossOrigin(origins = "*")
@RestController
@PreAuthorize("hasAuthority('ADMIN')")
public class RestAdminController {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	AuthenticationManager authenticationManager;

	@Value("${spring.mail.username}")
	private String springMailUsername;

	@GetMapping("/users")
	public @ResponseBody List<User> getUsers() {
		return (List<User>) userRepository.findAll();
	}

	@GetMapping("/orders")
	public @ResponseBody List<Order> getOrders() {
		return (List<Order>) orderRepository.findAll();
	}

	@GetMapping("/orders/{orderid}")
	public @ResponseBody Order getOrderById(@PathVariable("orderid") Long orderId) {
		Optional<Order> optionalOrder = orderRepository.findById(orderId);

		if (!optionalOrder.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order wasn't found by id");

		Order order = optionalOrder.get();
		return order;
	}

	@PutMapping("/updateorder/{orderid}")
	public ResponseEntity<?> updateOrder(@PathVariable("orderid") Long orderId, @RequestBody OrderInfo orderInfo)
			throws MessagingException, UnsupportedEncodingException {
		Optional<Order> optionalOrder = orderRepository.findById(orderId);

		if (!optionalOrder.isPresent())
			return new ResponseEntity<>("The order wasn't found by id", HttpStatus.NOT_FOUND);

		Order order = optionalOrder.get();

		order.setFirstname(orderInfo.getFirstname());
		order.setLastname(orderInfo.getLastname());
		order.setCountry(orderInfo.getCountry());
		order.setCity(orderInfo.getCity());
		order.setStreet(orderInfo.getStreet());
		order.setPostcode(orderInfo.getPostcode());

		if (!order.getEmail().equals(orderInfo.getEmail())) {
			try {
				this.sendOrderEmailChanged(orderInfo.getFirstname(), orderInfo.getLastname(), orderInfo.getEmail(),
						orderId);
			} catch (MailAuthenticationException e) {
			}
			order.setEmail(orderInfo.getEmail());
		}
		if (!order.getStatus().equals(orderInfo.getStatus())) {
			Backet backet = order.getBacket();
			if (backet.getUser() != null) {
				try {
					this.sendStatusChangeEmail(orderInfo.getFirstname(), orderInfo.getLastname(),
							backet.getUser().getEmail(), orderId, orderInfo.getStatus());
				} catch (MailAuthenticationException e) {
				}
			}
			try {
				this.sendStatusChangeEmail(orderInfo.getFirstname(), orderInfo.getLastname(), orderInfo.getEmail(),
						orderId, orderInfo.getStatus());
			} catch (MailAuthenticationException e) {
			}
			order.setStatus(orderInfo.getStatus());
		}
		orderRepository.save(order);
		return new ResponseEntity<>("Order Info was updated successfully", HttpStatus.OK);
	}
	
	@PutMapping("/changerole/{userid}")
	public ResponseEntity<?> changeUserRole(@PathVariable("userid") Long userId, @RequestBody RoleInfo roleInfo,
			Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof MyUser))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		MyUser myUser = (MyUser) authentication.getPrincipal();
		String username = myUser.getUsername();
		Optional<User> optionalAdmin = userRepository.findByUsername(username);

		if (!optionalAdmin.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized");

		User admin = optionalAdmin.get();

		Optional<User> optionalUser = userRepository.findById(userId);

		if (!optionalUser.isPresent())
			return new ResponseEntity<>("User wasn't found by id", HttpStatus.NOT_FOUND);

		User user = optionalUser.get();

		if (admin.getId() == user.getId())
			return new ResponseEntity<>("You can't change your own role", HttpStatus.NOT_ACCEPTABLE);

		user.setRole(roleInfo.getRole());
		userRepository.save(user);

		return new ResponseEntity<>("Role of the user was successfully changed", HttpStatus.OK);
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
}
