package com.pro.mybooklist.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface BacketBookRepository extends CrudRepository<BacketBook, Long>{
	Optional<BacketBook> findById(BacketBookKey backetBookId);
	
	List<BacketBook> findByBacket(Backet backet);
	
	long deleteByBacket(Backet backet);
	
	long deleteById(BacketBookKey backetBookId);
}
