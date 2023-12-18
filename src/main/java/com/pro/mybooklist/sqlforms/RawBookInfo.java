package com.pro.mybooklist.sqlforms;

public interface RawBookInfo {
	Long getBookid();
	String getTitle();
	String getAuthor();
	String getIsbn();
	Integer getBook_year();
	Double getPrice();
	String getUrl();
}
