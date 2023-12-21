package com.pro.mybooklist.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.AddressInfoNoAuthentication;
import com.pro.mybooklist.httpforms.OrderPasswordInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
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

		this.checkIfBacketIsEmpty(backet);

		this.setBacketNotCurrent(backet);

		String passwordRandom = RandomStringUtils.random(15);
		String hashedPassword = commonService.encodePassword(passwordRandom);

		Long orderId = this.createOrderByAddressInfo(addressInfo, backet, hashedPassword);

		OrderPasswordInfo orderPassword = new OrderPasswordInfo(orderId, passwordRandom);

		try {
			mailService.sendOrderInfoEmail(addressInfo.getFirstname() + " " + addressInfo.getLastname(),
					addressInfo.getEmail(), orderId, passwordRandom);
		} catch (MailAuthenticationException e) {
		}
		return orderPassword;
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

	private Long createOrderByAddressInfo(AddressInfoNoAuthentication addressInfo, Backet backet,
			String hashedPassword) {
		Order order = new Order(addressInfo.getFirstname(), addressInfo.getLastname(), addressInfo.getCountry(),
				addressInfo.getCity(), addressInfo.getStreet(), addressInfo.getPostcode(), addressInfo.getEmail(),
				backet, addressInfo.getNote(), hashedPassword);
		orderRepository.save(order);

		Long orderId = order.getOrderid();
		return orderId;
	}
}