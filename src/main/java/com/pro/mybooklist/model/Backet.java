package com.pro.mybooklist.model;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.bytebuddy.utility.RandomString;

@Entity
public class Backet {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "backetid", nullable = false, updatable = false)
	private Long backetid;
	
	@Column(name = "current", nullable = false)
	private boolean current;
	
	@Column(name = "expiry_date")
	private String expiryDate;
	
	@Column(name = "password")
	private String passwordHash;
	
	@ManyToOne
	@JoinColumn(name="userid")
	private User user;
	
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "backet")
	private List<BacketBook> backetbooks;
	
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, mappedBy = "backet")
	private Order order;
	
	public Backet() {}
	
	public Backet(boolean current, User user) {
		this.current = current;
		this.user = user;
		this.expiryDate = null;
		this.passwordHash = null;
	}
	
	public Backet(boolean current) {
		this.current = current;
		this.user = null;
		this.expiryDate = LocalDate.now().plusDays(1).toString();
		
		String password = "test";
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		String hashPwd = bc.encode(password);
		this.passwordHash = hashPwd;
	}
	
	public Backet(String passwordHash) {
		this.current = true;
		this.user = null;
		this.expiryDate = LocalDate.now().plusDays(1).toString();
		
		this.passwordHash = passwordHash;
	}

	public Long getBacketid() {
		return backetid;
	}

	public void setBacketid(Long backetid) {
		this.backetid = backetid;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<BacketBook> getBacketbooks() {
		return backetbooks;
	}

	public void setBacketbooks(List<BacketBook> backetbooks) {
		this.backetbooks = backetbooks;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	
	
}
