package com.pro.mybooklist.repositorytest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;

import jakarta.transaction.Transactional;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class BacketRepositoryTest {
	@Autowired
	private UserRepository urepository;

	@Autowired
	private BacketRepository backetrepository;

	@BeforeAll
	public void resetUserRepo() {
		backetrepository.deleteAll();
	}

	// CRUD tests for the backet repository
	// Create functionality
	@Test
	@Rollback
	public void testCreateBacketWithUser() {
		Backet newBacket1 = this.createBacketWithUser("user1");
		assertThat(newBacket1.getBacketid()).isNotNull();

		this.createBacketWithUser("user2");
		List<Backet> backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).hasSize(2);
	}
	
	@Test
	@Rollback
	public void testCreateBacketNoUser() {
		Backet newBacket1 = this.createBacketNoUser();
		assertThat(newBacket1.getBacketid()).isNotNull();

		this.createBacketNoUser();
		List<Backet> backets = (List<Backet>) backetrepository.findAll();
		assertThat(backets).hasSize(2);
	}

	private Backet createBacketWithUser(String username) {
		User user = this.createUser(username);
		Backet newBacket = new Backet(true, user);
		backetrepository.save(newBacket);
		
		return newBacket;
	}
	
	private Backet createBacketNoUser() {
		Backet newBacket = new Backet("some_pwd");
		backetrepository.save(newBacket);
		
		return newBacket;
	}

	private User createUser(String username) {
		Optional<User> optionalUser = urepository.findByUsername(username);

		if (optionalUser.isPresent())
			return optionalUser.get();

		User user = new User("Firstname", "Lastname", username, "hash_pwd", "USER", "email@mail.com", false);
		urepository.save(user);
		
		return user;
	}
}
