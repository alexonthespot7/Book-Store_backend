package com.pro.mybooklist.service;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;

@Service
public class CommonService {
	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger log = LoggerFactory.getLogger(CommonService.class);

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

	// Method to find backet, check
	public Backet findBacketAndCheckIsPrivateAndCheckPasswordAndCheckIsCurrent(Long backetId, String password) {
		Backet backet = this.findBacketAndCheckIsPrivateAndCheckPassword(backetId, password);

		this.checkIfBacketIsCurrent(backet);

		return backet;
	}

	// Method to encode password:
	public String encodePassword(String password) {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);

		return hashPwd;
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

	private void checkPassword(String password, String passwordHash) {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		if (!bc.matches(password, passwordHash))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is wrong");
	}
}
