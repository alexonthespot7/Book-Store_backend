package com.pro.mybooklist.web;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
	
	private static final Logger log =
			 LoggerFactory.getLogger(UserController.class);

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

	/**
	 * Rest method for login, using jwt security of java spring boot. If credentials
	 * are authenticated by filter, frontend will receive jwt token to use in the
	 * further requests
	 **/

	// - Backend - DONE, Frontend - DONE
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResponseEntity<?> getToken(@RequestBody AccountCredentials credentials) {
		Optional<User> userByMail = urepository.findByEmail(credentials.getUsername());

		UsernamePasswordAuthenticationToken creds;

		if (userByMail.isPresent()) {
			if (userByMail.get().isAccountVerified()) {
				creds = new UsernamePasswordAuthenticationToken(userByMail.get().getUsername(),
						credentials.getPassword());
			} else {
				return new ResponseEntity<>("Email is not verified", HttpStatus.CONFLICT);
			}
		} else {
			if (urepository.findByUsername(credentials.getUsername()).isPresent()) {
				if (urepository.findByUsername(credentials.getUsername()).get().isAccountVerified()) {
					creds = new UsernamePasswordAuthenticationToken(credentials.getUsername(),
							credentials.getPassword());
				} else {
					return new ResponseEntity<>("Email is not verified", HttpStatus.CONFLICT);
				}
			} else {
				return new ResponseEntity<>("Bad credentials", HttpStatus.UNAUTHORIZED);
			}
		}

		Authentication auth = authenticationManager.authenticate(creds);

		String jwts = jwtService.getToken(auth.getName());

		Optional<User> currentUser = urepository.findByUsername(auth.getName());
		if (currentUser.isPresent()) {
			return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts)
					.header(HttpHeaders.ALLOW, currentUser.get().getRole())
					.header(HttpHeaders.HOST, currentUser.get().getId().toString())
					.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization, Allow", "Host").build();
		} else {
			return new ResponseEntity<>("Bad credentials", HttpStatus.UNAUTHORIZED);
		}

	}

	/**
	 * Rest sign up method, which uses smtp service to send verification mail to the
	 * user email address. Before user clicks the link, their account isn't verified
	 * and login cannot be completed.
	 */

	// - Backend - DONE, Frontend - DONE
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	public ResponseEntity<?> signUp(@RequestBody SignupCredentials creds, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(creds.getPassword());
		String randomCode = RandomStringUtils.random(64);

		if (urepository.findByUsername(creds.getUsername()).isPresent()) {
			return new ResponseEntity<>("Username is already in use", HttpStatus.CONFLICT);
		} else if (urepository.findByEmail(creds.getEmail()).isPresent()) {
			return new ResponseEntity<>("Email is already in use", HttpStatus.NOT_ACCEPTABLE);
		} else {
			User newUser = new User(creds.getFirstname(), creds.getLastname(), creds.getUsername(), hashPwd, "USER",
					creds.getEmail(), randomCode, false);
			urepository.save(newUser);
			this.sendVerificationEmail(newUser);
			return new ResponseEntity<>("We sent verification link to your email address :)", HttpStatus.OK);
		}
	}

	// Rest method to verify user's registration token - Backend - DONE, Frontend -
	// DONE
	@RequestMapping(value = "/verify", method = RequestMethod.POST)
	public ResponseEntity<?> verifyRequest(@RequestBody TokenInfo tokenInfo) {
		String token = tokenInfo.getToken();

		if (this.verify(token)) {
			return new ResponseEntity<>("Verification went well", HttpStatus.OK);
		} else {
			return new ResponseEntity<>("Verification code is incorrect or you are already verified",
					HttpStatus.CONFLICT);
		}
	}
	
	@RequestMapping(value="/changerole/{userid}", method=RequestMethod.PUT)
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
	
	@RequestMapping(value="/updateuser/{userid}", method=RequestMethod.PUT)
	@PreAuthorize("authentication.getPrincipal().getId() == #userId")
	public ResponseEntity<?> updateUser(@PathVariable("userid") Long userId, @RequestBody User user) {
		if (urepository.findById(userId).isPresent() && user.getId() == userId) {
			User userToUpdate = urepository.findById(userId).get();
			
			userToUpdate.setLastname(user.getLastname());
			userToUpdate.setFirstname(user.getFirstname());
			userToUpdate.setCountry(user.getCountry());
			userToUpdate.setCity(user.getCity());
			userToUpdate.setStreet(user.getStreet());
			userToUpdate.setPostcode(user.getPostcode());
			
			urepository.save(userToUpdate);
			
			return new ResponseEntity<>("User info was updated successfully", HttpStatus.OK);
		} else {
			return new ResponseEntity<>("Bad request", HttpStatus.BAD_REQUEST);
		}
	}

	// rest method to change user's password - Backend - DONE, Frontend - DONE
	@RequestMapping(value = "/changepassword", method = RequestMethod.POST)
	@PreAuthorize("authentication.getPrincipal().getUsername() == #passwordInfo.getUsername()")
	public ResponseEntity<?> changePassword(@RequestBody PasswordInfo passwordInfo) {
		Optional<User> userOptional = urepository.findByUsername(passwordInfo.getUsername());
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

		if (userOptional.isPresent()) {
			User user = userOptional.get();

			if (bc.matches(passwordInfo.getOldPassword(), user.getPassword())) {
				String hashPwd = bc.encode(passwordInfo.getNewPassword());

				user.setPassword(hashPwd);
				urepository.save(user);

				return new ResponseEntity<>("The password was changed", HttpStatus.OK);
			} else {
				return new ResponseEntity<>("The old password is incorrect", HttpStatus.CONFLICT);
			}

		} else {
			return new ResponseEntity<>("Forbidden request", HttpStatus.FORBIDDEN);
		}
	}

	// rest method to reset user's password by creating temporary password and
	// sending it to user's email address; - Backend - DONE, Frontend - DONE
	@RequestMapping(value = "/resetpassword", method = RequestMethod.POST)
	public ResponseEntity<?> resetPassword(@RequestBody EmailInfo emailInfo)
			throws UnsupportedEncodingException, MessagingException {
		Optional<User> user = urepository.findByEmail(emailInfo.getEmail());

		if (user.isPresent()) {
			User currentUser = user.get();

			if (currentUser.isAccountVerified()) {
				String password = RandomStringUtils.random(15);

				BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
				String hashPwd = bc.encode(password);
				currentUser.setPassword(hashPwd);
				urepository.save(currentUser);

				this.sendPasswordEmail(currentUser, password);

				return new ResponseEntity<>("A temporary password was sent to your email address", HttpStatus.OK);
			} else {
				return new ResponseEntity<>("User with this email (" + emailInfo.getEmail() + ") is not verified",
						HttpStatus.CONFLICT);
			}
		} else {
			return new ResponseEntity<>("User with this email (" + emailInfo.getEmail() + ") doesn't exist",
					HttpStatus.UNAUTHORIZED);
		}
	}

	// inside backend method to send verification link to the newly registered
	// user's email address
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

	// inside backend method to send temporary password to user's email address, if
	// the user requires password reset
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

	// inside backend method to verify user by provided verification code;
	private boolean verify(String verificationCode) {
		Optional<User> user = urepository.findByVerificationCode(verificationCode);

		if (user.isPresent() && !user.get().isAccountVerified()) {
			User currentUser = user.get();

			currentUser.setVerificationCode(null);
			currentUser.setAccountVerified(true);
			urepository.save(currentUser);
			
			if (barepository.findCurrentByUserid(currentUser.getId()).size() > 0) {
				return false;
			} else {
				barepository.save(new Backet(true, currentUser));
			}
			return true;
		} else {
			return false;
		}
	}
}
