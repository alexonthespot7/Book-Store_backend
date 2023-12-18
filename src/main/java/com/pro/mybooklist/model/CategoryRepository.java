package com.pro.mybooklist.model;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Long> {
	
	Optional<Category> findByName(String name);
}

