package com.pro.mybooklist.web;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.List;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.httpforms.AccountCredentials;
import com.pro.mybooklist.httpforms.EmailInfo;
import com.pro.mybooklist.httpforms.PasswordInfo;
import com.pro.mybooklist.httpforms.RoleInfo;
import com.pro.mybooklist.httpforms.SignupCredentials;
import com.pro.mybooklist.httpforms.TokenInfo;
import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;
import com.pro.mybooklist.service.AuthenticationService;

@RestController
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserRepository urepository;

	@Autowired
	private BacketRepository barepository;

	@Autowired
	private AuthenticationService jwtService;

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String springMailUsername;

	@RequestMapping(value = "/changerole/{userid}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<?> changeRole(@PathVariable("userid") Long userId, @RequestBody RoleInfo roleInfo) {
		Optional<User> optUser = urepository.findById(userId);

		if (optUser.isPresent()) {
			User curruser = optUser.get();

			curruser.setRole(roleInfo.getRole());
			urepository.save(curruser);

			return new ResponseEntity<>("Role of the user was successfully changed", HttpStatus.OK);
		} else {
			return new ResponseEntity<>("There is no such user", HttpStatus.BAD_REQUEST);
		}
	}

}
