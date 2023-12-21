package com.pro.mybooklist.httpforms;

public class BacketInfo {
	private Long id;
	private String password;
	
	public BacketInfo() {}
	
	public BacketInfo(Long id, String password) {
		this.id = id;
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
