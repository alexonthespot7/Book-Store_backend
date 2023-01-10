package com.pro.mybooklist;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;


@SpringBootApplication
public class MybooklistApplication {

	public static void main(String[] args) {
		SpringApplication.run(MybooklistApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(BookRepository repository, CategoryRepository crepository) {
		return (args) -> {
			crepository.save(new Category("Thriller"));
			crepository.save(new Category("Science fiction"));
			crepository.save(new Category("Romance"));
			crepository.save(new Category("Horror"));

			Book book1 = new Book("Great Gatsby", "Scott Fitzgerald", "123GPA123", 1925, 10.9,
					crepository.findByName("Romance").get(0));
			Book book2 = new Book("451 Fahrenheit", "Ray Bradbury", "123GPA222", 1951, 5.95,
					crepository.findByName("Romance").get(0));
			Book book3 = new Book("It", "Steven King", "123GPA223", 1986, 12.5,
					crepository.findByName("Horror").get(0));

			repository.save(book1);
			repository.save(book2);
			repository.save(book3);

		};
	}
}
