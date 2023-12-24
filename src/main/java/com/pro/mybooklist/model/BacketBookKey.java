package com.pro.mybooklist.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class BacketBookKey implements Serializable {
	private static final long serialVersionUID = -6860012402807015054L;

	@Column(name = "backetid")
	Long backetid;
	
	@Column(name = "bookid")
	Long bookid;
	
	public BacketBookKey() {}

	public BacketBookKey(Long backetid, Long bookid) {
		super();
		this.backetid = backetid;
		this.bookid = bookid;
	}

	public Long getBacketid() {
		return backetid;
	}

	public void setBacketid(Long backetid) {
		this.backetid = backetid;
	}

	public Long getBookid() {
		return bookid;
	}

	public void setBookid(Long bookid) {
		this.bookid = bookid;
	}

	@Override
	public int hashCode() {
		return Objects.hash(backetid, bookid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BacketBookKey other = (BacketBookKey) obj;
		return Objects.equals(backetid, other.backetid) && Objects.equals(bookid, other.bookid);
	}
}
