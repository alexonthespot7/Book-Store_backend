package com.pro.mybooklist.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.AddressInfo;
import com.pro.mybooklist.httpforms.AddressInfoNoAuthentication;
import com.pro.mybooklist.httpforms.OrderInfo;
import com.pro.mybooklist.httpforms.OrderPasswordInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

import jakarta.mail.MessagingException;

@Service
public class OrderService {
	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private BacketBookRepository backetBookRepository;

	@Autowired
	private CommonService commonService;

	@Autowired
	private MailService mailService;

	// Method to get the list of all the orders:
	public List<Order> getOrders() {
		List<Order> orders = (List<Order>) orderRepository.findAll();
		return orders;
	}

	// Method to get the order by order id:
	public Order getOrderById(Long orderId) {
		Order order = commonService.findOrder(orderId);
		return order;
	}

	// Method to get the list of orders of the user
	public List<Order> getOrdersByUserId(Long userId, Authentication authentication) {
		commonService.checkAuthenticationAndAuthorize(authentication, userId);

		List<Order> ordersOfUser = orderRepository.findByUserid(userId);
		return ordersOfUser;
	}

	// Method to get order by it's id and password
	public Order getOrderByIdAndPassword(OrderPasswordInfo orderInfo) {
		Long orderId = orderInfo.getOrderid();
		String password = orderInfo.getPassword();

		Order order = commonService.findOrderAndCheckPassword(orderId, password);
		return order;
	}

	// Method to get total price of order by order Id:
	public TotalOfBacket getTotalOfOrderByOrderId(Long orderId) {
		commonService.findOrder(orderId);

		TotalOfBacket totalOfOrder = backetRepository.findTotalOfOrder(orderId);
		return totalOfOrder;
	}

	// Method to check provided order id and order password:
	public ResponseEntity<?> checkOrderNumber(OrderPasswordInfo orderInfo) {
		Long orderId = orderInfo.getOrderid();
		String password = orderInfo.getPassword();

		commonService.findOrderAndCheckPassword(orderId, password);

		return new ResponseEntity<>("The order number and password are correct", HttpStatus.OK);
	}

	// Method to create order out of backet by backet id and backet password:
	public OrderPasswordInfo makeSaleNoAuthentication(AddressInfoNoAuthentication addressInfo)
			throws MessagingException, UnsupportedEncodingException {
		Long backetId = addressInfo.getBacketid();
		String backetPassword = addressInfo.getPassword();

		Backet backet = commonService.findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(backetId,
				backetPassword);
		String passwordRandom = this.checkIfBacketIsEmptyAndSetBacketNotCurrentAndGeneratePassword(backet);
		String hashedPassword = commonService.encodePassword(passwordRandom);

		Long orderId = this.createOrderByAddressInfoNoAuthentication(addressInfo, backet, hashedPassword);
		OrderPasswordInfo orderPassword = new OrderPasswordInfo(orderId, passwordRandom);

		this.tryToSendOrderInfoEmail(addressInfo.getFirstname(), addressInfo.getEmail(), orderId, passwordRandom);

		return orderPassword;
	}

	private Long createOrderByAddressInfoNoAuthentication(AddressInfoNoAuthentication addressInfo, Backet backet,
			String hashedPassword) {
		Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(), addressInfo.getCountry(),
				addressInfo.getCity(), addressInfo.getStreet(), addressInfo.getPostcode(), addressInfo.getEmail(),
				backet, addressInfo.getNote(), hashedPassword);
		orderRepository.save(order);

		Long orderId = order.getOrderid();
		return orderId;
	}

	// Method to create order out of authenticated user's current backet:
	public OrderPasswordInfo makeSaleByUserId(Long userId, AddressInfo addressInfo, Authentication authentication)
			throws MessagingException, UnsupportedEncodingException {
		User user = commonService.checkAuthenticationAndAuthorize(authentication, userId);
		Backet currentBacket = commonService.findCurrentBacketOfUser(user);

		String passwordRandom = this.checkIfBacketIsEmptyAndSetBacketNotCurrentAndGeneratePassword(currentBacket);
		String hashedPassword = commonService.encodePassword(passwordRandom);

		Long orderId = this.createOrderByAddressInfo(addressInfo, currentBacket, hashedPassword);
		OrderPasswordInfo orderPassword = new OrderPasswordInfo(orderId, passwordRandom);

		this.tryToSendOrderInfoEmail(user.getUsername(), user.getEmail(), orderId, passwordRandom);

		if (!addressInfo.getEmail().equals(user.getEmail())) {
			this.tryToSendOrderInfoEmail(user.getUsername(), addressInfo.getEmail(), orderId, passwordRandom);
		}

		commonService.addCurrentBacketForUser(user);

		return orderPassword;
	}

	private Long createOrderByAddressInfo(AddressInfo addressInfo, Backet backet, String hashedPassword) {
		Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(), addressInfo.getCountry(),
				addressInfo.getCity(), addressInfo.getStreet(), addressInfo.getPostcode(), addressInfo.getEmail(),
				backet, addressInfo.getNote(), hashedPassword);
		orderRepository.save(order);

		Long orderId = order.getOrderid();
		return orderId;
	}

	private String checkIfBacketIsEmptyAndSetBacketNotCurrentAndGeneratePassword(Backet backet) {
		this.checkIfBacketIsEmpty(backet);
		this.setBacketNotCurrent(backet);

		String passwordRandom = RandomStringUtils.randomAlphanumeric(15);
		return passwordRandom;
	}

	private void checkIfBacketIsEmpty(Backet backet) {
		List<BacketBook> backetBooksInBacket = backetBookRepository.findByBacket(backet);
		if (backetBooksInBacket.size() == 0)
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "The backet is empty");
	}

	private void setBacketNotCurrent(Backet backet) {
		backet.setCurrent(false);
		backetRepository.save(backet);
	}

	private void tryToSendOrderInfoEmail(String firstnameOrUsername, String email, Long orderId, String password)
			throws MessagingException, UnsupportedEncodingException {
		try {
			mailService.sendOrderInfoEmail(firstnameOrUsername, email, orderId, password);
		} catch (MailAuthenticationException e) {
		}
	}

	// Method to update order's info by orderId:
	public ResponseEntity<?> updateOrder(Long orderId, OrderInfo orderInfo)
			throws MessagingException, UnsupportedEncodingException {
		Order order = commonService.findOrder(orderId);
		this.updateOrder(order, orderInfo, orderId);
		return new ResponseEntity<>("Order Info was updated successfully", HttpStatus.OK);
	}

	private void updateOrder(Order order, OrderInfo orderInfo, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		this.updateOrderFieldsExceptEmailAndStatus(order, orderInfo);
		order = this.handleEmailChangedCase(order, orderInfo, orderId);
		order = this.handleStatusChangedCase(order, orderInfo, orderId);

		orderRepository.save(order);
	}

	private Order updateOrderFieldsExceptEmailAndStatus(Order order, OrderInfo orderInfo) {
		order.setFirstname(orderInfo.getFirstname());
		order.setLastname(orderInfo.getLastname());
		order.setCountry(orderInfo.getCountry());
		order.setCity(orderInfo.getCity());
		order.setStreet(orderInfo.getStreet());
		order.setPostcode(orderInfo.getPostcode());

		return order;
	}

	private Order handleEmailChangedCase(Order order, OrderInfo orderInfo, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		if (!order.getEmail().equals(orderInfo.getEmail())) {
			this.tryToSendOrderEmailChanged(orderInfo, orderId);
			order.setEmail(orderInfo.getEmail());
		}

		return order;
	}

	private Order handleStatusChangedCase(Order order, OrderInfo orderInfo, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		if (!order.getStatus().equals(orderInfo.getStatus())) {
			Backet backet = order.getBacket();
			if (backet.getUser() != null) {
				this.tryToSendStatusChangeEmail(orderInfo, backet.getUser().getEmail(), orderId);
			}
			this.tryToSendStatusChangeEmail(orderInfo, orderInfo.getEmail(), orderId);
			order.setStatus(orderInfo.getStatus());
		}
		return order;
	}

	private void tryToSendOrderEmailChanged(OrderInfo orderInfo, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		try {
			mailService.sendOrderEmailChanged(orderInfo.getFirstname(), orderInfo.getEmail(), orderId);
		} catch (MailAuthenticationException e) {
		}
	}

	private void tryToSendStatusChangeEmail(OrderInfo orderInfo, String email, Long orderId)
			throws MessagingException, UnsupportedEncodingException {
		try {
			mailService.sendStatusChangeEmail(orderInfo.getFirstname(), email, orderId, orderInfo.getStatus());
		} catch (MailAuthenticationException e) {
		}
	}
}
