package com.pro.mybooklist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "orderid", nullable = false, updatable = false)
	private Long orderid;
	
	@Column(name = "firstname")
	private String firstname;
	
	@Column(name = "lastname")
	private String lastname;
	
	@Column(nullable = false)
	private String country;
	
	@Column(nullable = false)
	private String city;
	
	@Column(nullable = false)
	private String street;
	
	@Column(nullable = false)
	private String postcode;
	
	@Column(name="status", nullable = false, updatable = true)
	private String status;
	
	@Column(name="email", nullable = false)
	private String email;
	
	@Column(name = "note")
	private String note;

	@Column(name = "password")
	private String password;
	
	@OneToOne
	@JoinColumn(name="backetid", referencedColumnName = "backetid", nullable = false)
	private Backet backet;
	
	public Order() {}

	public Order(String firstname, String lastname, String country, String city, String street, String postcode, String status, String email, Backet backet, String password) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.country = country;
		this.city = city;
		this.street = street;
		this.postcode = postcode;
		this.status = status;
		this.email = email;
		this.backet = backet;
		this.note = "";
		this.password = password;
	}

	public Order(String firstname, String lastname, String country, String city, String street, String postcode, String email, Backet backet, String note, String password) {
		super();
		this.firstname = firstname;
		this.lastname = lastname;
		this.country = country;
		this.city = city;
		this.street = street;
		this.postcode = postcode;
		this.email = email;
		this.note = note;
		this.backet = backet;
		this.status = "Created";
		this.password = password;
	}

	public Long getOrderid() {
		return orderid;
	}

	public void setOrderid(Long orderid) {
		this.orderid = orderid;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Backet getBacket() {
		return backet;
	}

	public void setBacket(Backet backet) {
		this.backet = backet;
	}
}
