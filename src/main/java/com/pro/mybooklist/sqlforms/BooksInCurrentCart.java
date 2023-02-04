package com.pro.mybooklist.sqlforms;

public interface BooksInCurrentCart {
	Long getBookid();
	Long getBacketid();
	String getTitle();
	String getAuthor();
	String getIsbn();
	Integer getBook_year();
	Double getPrice();
	String getCategory();
	Integer getQuantity();
	String getUrl();
}
