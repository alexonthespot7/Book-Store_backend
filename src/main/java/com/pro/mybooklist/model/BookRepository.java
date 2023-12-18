package com.pro.mybooklist.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.pro.mybooklist.sqlforms.BookInCurrentCart;
import com.pro.mybooklist.sqlforms.RawBookInfo;

public interface BookRepository extends CrudRepository<Book, Long> {
	@Query(value = "SELECT bo.id AS bookid, ba.backetid, title, author, isbn, book_year, price, url, ca.name AS category, bb.quantity FROM book AS bo JOIN category AS ca ON (ca.categoryid = bo.categoryid) JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) JOIN users AS u ON (u.id = ba.userid) WHERE u.id = ?1 AND current", nativeQuery = true)
	List<BookInCurrentCart> findBooksInCurrentBacketByUserid(Long userId);

	@Query(value = "SELECT bo.id AS bookid FROM book AS bo JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) JOIN users AS u ON (u.id = ba.userid) WHERE u.id = ?1 AND current", nativeQuery = true)
	List<Long> idsOfBooksInCurrentCart(Long userId);
	
	@Query(value = "SELECT bo.id AS bookid FROM book AS bo JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) WHERE ba.backetid = ?1 AND current", nativeQuery = true)
	List<Long> idsOfBooksByBacketid(Long backetId);
	
	@Query(value = "SELECT bo.id AS bookid, ba.backetid, title, author, isbn, book_year, price, url, ca.name AS category, bb.quantity FROM book AS bo JOIN category AS ca ON (ca.categoryid = bo.categoryid) JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) WHERE ba.backetid = ?1 AND NOT current", nativeQuery = true)
	List<BookInCurrentCart> findBooksInPastSaleByBacketid(Long backetid);
	
	@Query(value = "SELECT bo.id AS bookid, ba.backetid, title, author, isbn, book_year, price, url, ca.name AS category, bb.quantity FROM book AS bo JOIN category AS ca ON (ca.categoryid = bo.categoryid) JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) JOIN orders as o ON (o.backetid = ba.backetid) WHERE orderid = ?1", nativeQuery = true)
	List<BookInCurrentCart> findBooksInOrder(Long orderid);
	
	@Query(value = "SELECT bo.id AS bookid, ba.backetid, title, author, isbn, book_year, price, url, ca.name AS category, bb.quantity FROM book AS bo JOIN category AS ca ON (ca.categoryid = bo.categoryid) JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) WHERE ba.backetid = ?1", nativeQuery = true)
	List<BookInCurrentCart> findBooksInBacket(Long backetid);

	@Query(value = "SELECT bo.id AS bookid, title, author, isbn, book_year, price, url FROM book AS bo JOIN backet_book AS bb ON (bb.bookid = bo.id) JOIN backet AS ba ON (ba.backetid = bb.backetid) WHERE NOT current GROUP BY bo.id ORDER BY SUM(quantity) DESC LIMIT 10", nativeQuery = true)
	List<RawBookInfo> topSales();
	
	List<Book> findByCategory(Category category);
	
	Optional<Book> findByIsbn(String isbn);
}
