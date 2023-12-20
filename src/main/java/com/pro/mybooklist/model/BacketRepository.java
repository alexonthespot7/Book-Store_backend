package com.pro.mybooklist.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.pro.mybooklist.sqlforms.QuantityOfBacket;
import com.pro.mybooklist.sqlforms.TotalOfBacket;

@Repository
public interface BacketRepository extends CrudRepository<Backet, Long> {
	Optional<Backet> findById(Long backetid);
	
	@Query(value="SELECT backet.* FROM backet WHERE current AND userid = ?1", nativeQuery=true)
	List<Backet> findCurrentByUserid(Long userId);
	
	@Query(value="SELECT backetid FROM backet WHERE NOT current AND userid =?1", nativeQuery = true)
	List<Long> findNotCurrentByUserid(Long userId);
	
	@Query(value="SELECT ba.backetid AS backetid, SUM(quantity * price) AS total FROM backet AS ba JOIN backet_book AS bb ON (bb.backetid = ba.backetid) JOIN book AS bo ON (bo.id = bb.bookid) WHERE current AND userid=?1 GROUP BY ba.backetid", nativeQuery = true)
	TotalOfBacket findTotalOfCurrentCart(Long userId);
	
	@Query(value="SELECT ba.backetid AS backetid, SUM(quantity * price) AS total FROM backet AS ba JOIN orders AS o ON (o.backetid = ba.backetid) JOIN backet_book AS bb ON (bb.backetid = ba.backetid) JOIN book AS bo ON (bo.id = bb.bookid) WHERE orderid=?1 GROUP BY ba.backetid", nativeQuery = true)
	TotalOfBacket findTotalOfOrder(Long orderid);
	
	@Query(value="SELECT ba.backetid AS backetid, SUM(quantity * price) AS total FROM backet AS ba JOIN backet_book AS bb ON (bb.backetid = ba.backetid) JOIN book AS bo ON (bo.id = bb.bookid) WHERE ba.backetid=?1 GROUP BY ba.backetid", nativeQuery = true)
	TotalOfBacket findTotalOfBacket(Long backetid);
	
	@Query(value="SELECT ba.backetid, SUM(quantity) AS items FROM backet AS ba JOIN backet_book AS bb ON (bb.backetid = ba.backetid) WHERE current AND userid =?1 GROUP BY ba.backetid", nativeQuery = true)
	QuantityOfBacket findQuantityInCurrent(Long userId);
}
