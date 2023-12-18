package com.pro.mybooklist.httpforms;

public class BookInfo {
	private Long bookid;
	private String password;
	
	public BookInfo(Long bookid, String password) {
		this.bookid = bookid;
		this.password = password;
	}

	public Long getBookid() {
		return bookid;
	}

	public void setBookid(Long bookid) {
		this.bookid = bookid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
