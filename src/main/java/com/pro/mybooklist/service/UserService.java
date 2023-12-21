package com.pro.mybooklist.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.httpforms.AccountCredentials;
import com.pro.mybooklist.httpforms.EmailInfo;
import com.pro.mybooklist.httpforms.SignupCredentials;
import com.pro.mybooklist.httpforms.TokenInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BacketRepository backetRepository;

	@Autowired
	private CommonService commonService;

	@Autowired
	private AuthenticationService jwtService;

	@Autowired
	private MailService mailService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Value("${spring.mail.username}")
	private String springMailUsername;

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	// Login method
	public ResponseEntity<?> getToken(AccountCredentials credentials) {
		String emailOrUsername = credentials.getUsername();

		User user = this.findUserByEmailOrUsername(emailOrUsername);

		this.handleAccountUnverified(user);

		String username = user.getUsername();
		String password = credentials.getPassword();

		User authenticatedUser = this.authenticateUser(username, password);

		return returnAuthenticationInfo(authenticatedUser);
	}

	// Signup method
	public ResponseEntity<?> signUp(SignupCredentials credentials)
			throws MessagingException, UnsupportedEncodingException {
		String username = credentials.getUsername();
		String email = credentials.getEmail();

		this.checkUsernameOrEmailInUse(username, email);

		User newUser = this.createUnverifiedUserBySignupCredentials(credentials);

		try {
			mailService.sendVerificationEmail(newUser);

			return new ResponseEntity<>("We sent verification link to your email address :)", HttpStatus.OK);
		} catch (MailAuthenticationException e) {
			this.verifyUser(newUser);

			return new ResponseEntity<>("Registration went well, you can login now", HttpStatus.ACCEPTED);
		}
	}

	// Verification method
	public ResponseEntity<?> verifyUser(TokenInfo tokenInfo) {
		String token = tokenInfo.getToken();

		User user = this.findUserByVerificationCode(token);

		if (user.isAccountVerified())
			return new ResponseEntity<>("User is already verified", HttpStatus.CONFLICT);

		this.verifyUserAndCreateCurrentBacket(user);

		return new ResponseEntity<>("Verification went well", HttpStatus.OK);
	}

	// Reset password by email method:
	public ResponseEntity<?> resetPassword(EmailInfo emailInfo)
			throws MessagingException, UnsupportedEncodingException {
		String email = emailInfo.getEmail();

		User user = this.findUserByEmail(email);

		this.handleAccountUnverified(user);

		String password = RandomStringUtils.random(15);

		try {
			mailService.sendPasswordEmail(user, password);

			this.setNewPassword(user, password);

			return new ResponseEntity<>("A temporary password was sent to your email address", HttpStatus.OK);

		} catch (MailAuthenticationException e) {
			return new ResponseEntity<>("The email service is not in use now", HttpStatus.NOT_IMPLEMENTED);
		}
	}

	private User findUserByEmailOrUsername(String emailOrUsername) {
		Optional<User> optionalUser = userRepository.findByEmail(emailOrUsername);

		if (!optionalUser.isPresent()) {
			optionalUser = userRepository.findByUsername(emailOrUsername);
			if (!optionalUser.isPresent())
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wrong username/email");
		}

		User user = optionalUser.get();
		return user;
	}

	private void verifyUserAndCreateCurrentBacket(User user) {
		this.verifyUser(user);
		this.addCurrentBacketByUserId(user);
	}

	private void verifyUser(User user) {
		user.setAccountVerified(true);
		user.setVerificationCode(null);
		userRepository.save(user);
	}

	private void addCurrentBacketByUserId(User user) {
		Long userId = user.getId();

		List<Backet> currentBackets = backetRepository.findCurrentByUserid(userId);

		if (currentBackets.size() > 0)
			throw new ResponseStatusException(HttpStatus.PARTIAL_CONTENT,
					"User shouldn't have had a backet before verification");

		Backet newCurrentBacketForUser = new Backet(true, user);

		backetRepository.save(newCurrentBacketForUser);
	}

	private User authenticateUser(String username, String password) {
		UsernamePasswordAuthenticationToken authenticationCredentials = new UsernamePasswordAuthenticationToken(
				username, password);

		Authentication authenticationInstance = authenticationManager.authenticate(authenticationCredentials);

		String authenticatedUsername = authenticationInstance.getName();

		Optional<User> optionalAuthenticatedUser = userRepository.findByUsername(authenticatedUsername);

		if (!optionalAuthenticatedUser.isPresent())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");

		User authenticatedUser = optionalAuthenticatedUser.get();

		return authenticatedUser;
	}

	private ResponseEntity<?> returnAuthenticationInfo(User authenticatedUser) {
		String authenticatedUsername = authenticatedUser.getUsername();

		String jwts = jwtService.getToken(authenticatedUsername);
		String role = authenticatedUser.getRole();
		String id = authenticatedUser.getId().toString();

		return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts).header(HttpHeaders.ALLOW, role)
				.header(HttpHeaders.HOST, id)
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization, Allow", "Host").build();
	}

	private void checkUsernameOrEmailInUse(String username, String email) {
		this.checkEmailInUse(email);
		this.checkUsernameInUse(username);
	}

	private void checkEmailInUse(String email) {
		Optional<User> optionalUser = userRepository.findByEmail(email);
		if (optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Email is already in use");
	}

	private void checkUsernameInUse(String username) {
		Optional<User> optionalUser = userRepository.findByUsername(username);
		if (optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
	}

	private User createUnverifiedUserBySignupCredentials(SignupCredentials credentials) {
		String randomCode = RandomStringUtils.random(64);
		String rawPassword = credentials.getPassword();
		String hashPwd = commonService.encodePassword(rawPassword);

		User newUser = new User(credentials.getFirstname(), credentials.getLastname(), credentials.getUsername(),
				hashPwd, "USER", credentials.getEmail(), randomCode, false);
		userRepository.save(newUser);

		return newUser;
	}

	private User findUserByVerificationCode(String verificationCode) {
		Optional<User> optionalUser = userRepository.findByVerificationCode(verificationCode);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification code is incorrect");

		User user = optionalUser.get();

		return user;
	}

	private User findUserByEmail(String email) {
		Optional<User> optionalUser = userRepository.findByEmail(email);

		if (!optionalUser.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"User with this email (" + email + ") doesn't exist");

		User user = optionalUser.get();

		return user;
	}

	private void setNewPassword(User user, String password) {
		String hashedPassword = commonService.encodePassword(password);
		user.setPassword(hashedPassword);
		userRepository.save(user);
	}

	private void handleAccountUnverified(User user) {
		if (!user.isAccountVerified()) {
			if (!this.springMailUsername.equals("default_value"))
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is not verified");

			this.verifyUserAndCreateCurrentBacket(user);
		}
	}
}
