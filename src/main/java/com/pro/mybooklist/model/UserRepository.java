package com.pro.mybooklist.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
	Optional<User> findByUsername(String username);
	
	@Query(value="SELECT * FROM users WHERE email = ?1", nativeQuery=true)
	Optional<User> findByEmail(String email);
	
	@Query(value="SELECT * FROM users WHERE verification_code = ?1", nativeQuery=true)
	Optional<User> findByVerificationCode(String code);
	
	List<User> findByLastname(String lastname);
}
