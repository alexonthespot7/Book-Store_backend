package com.pro.mybooklist.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends CrudRepository<Order, Long> {
	Optional<Order> findById(Long orderid);
	
	@Query(value = "SELECT o.* FROM orders AS o JOIN backet AS ba ON (ba.backetid = o.backetid) WHERE userid = ?1", nativeQuery = true)
	List<Order> findByUserid(Long userId);
}
