package com.pro.mybooklist.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "backet_book")
public class BacketBook {

	@EmbeddedId
	private BacketBookKey id;

	@Column(name = "quantity")
	private int quantity;

	@ManyToOne
	@MapsId("backetid")
	@JoinColumn(name = "backetid", nullable = false)
	private Backet backet;

	@ManyToOne
	@MapsId("bookid")
	@JoinColumn(name = "bookid", nullable = false)
	private Book book;

	public BacketBook() {}

	public BacketBook(int quantity, Backet backet, Book book) {
		super();
		BacketBookKey backetBookKey = new BacketBookKey(backet.getBacketid(), book.getId());
		this.id = backetBookKey;
		this.quantity = quantity;
		this.backet = backet;
		this.book = book;
	}

	public BacketBook(Backet backet, Book book) {
		super();
		BacketBookKey backetBookKey = new BacketBookKey(backet.getBacketid(), book.getId());
		this.id = backetBookKey;
		this.quantity = 1;
		this.backet = backet;
		this.book = book;
	}

	public BacketBookKey getId() {
		return id;
	}

	public void setId(BacketBookKey id) {
		this.id = id;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Backet getBacket() {
		return backet;
	}

	public void setBacket(Backet backet) {
		this.backet = backet;
	}

	public Book getBook() {
		return book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

}
