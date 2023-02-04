package com.pro.mybooklist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketBook;
import com.pro.mybooklist.model.BacketBookKey;
import com.pro.mybooklist.model.BacketBookRepository;
import com.pro.mybooklist.model.BacketRepository;
import com.pro.mybooklist.model.Book;
import com.pro.mybooklist.model.BookRepository;
import com.pro.mybooklist.model.Category;
import com.pro.mybooklist.model.CategoryRepository;
import com.pro.mybooklist.model.Order;
import com.pro.mybooklist.model.OrderRepository;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;


@SpringBootApplication
@EnableScheduling
public class MybooklistApplication {
	
	/**@Autowired
	private UserRepository urepository;
	
	@Autowired
	private CategoryRepository crepository;
	
	@Autowired
	private BookRepository repository;
	
	@Autowired
	private BacketRepository barepository;
	
	@Autowired
	private BacketBookRepository bbrepository;
	
	@Autowired
	private OrderRepository orepository;*/

	public static void main(String[] args) {
		SpringApplication.run(MybooklistApplication.class, args);
	}

	/**
	 * @Bean
	public CommandLineRunner runner() {
		return (args) -> {
			crepository.save(new Category("Thriller"));
			crepository.save(new Category("Science fiction"));
			crepository.save(new Category("Romance"));
			crepository.save(new Category("Horror"));
			crepository.save(new Category("Adventure"));

			Book book1 = new Book("Great Gatsby", "Scott Fitzgerald", "123GPA123", 1925, 10.9,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fddd.webp?alt=media&token=d7f8f06b-86a3-41b9-a5d5-5e05ae1858ad");
			Book book2 = new Book("451 Fahrenheit", "Ray Bradbury", "123GPA222", 1951, 5.95,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fss.jfif?alt=media&token=316a351a-04bc-4f73-9a32-b8a44da9cf05");
			Book book3 = new Book("It", "Steven King", "123GPA223", 1986, 12.5,
					crepository.findByName("Horror").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fit.webp2320d8f8-7a30-4a76-9118-006c4a2e2463?alt=media&token=23901e7e-7b89-4803-82b5-ad9f47c6bb4d");
			Book book4 = new Book("Fight Club", "Chuck Palahniuk", "123GPA323", 1996, 10.5,
					crepository.findByName("Thriller").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Ffightcl.webp?alt=media&token=0226506b-da58-46ea-a421-40c06cd717ea");
			Book book5 = new Book("Tender is the Night", "Scott Fitzgerald", "123GPA423", 1934, 12.5,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Ftendernight.webp?alt=media&token=54cba93b-7130-45b5-a8f5-78584deb67d0");
			Book book6 = new Book("Harry Potter and the Chamber of Secrets", "Joanne Rowling", "123GPA523", 1998, 8.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry2.webp?alt=media&token=2475ad48-1160-40b0-813b-88ed1fa0b25f");
			Book book7 = new Book("Harry Potter and the Philosopher's Stone", "Joanne Rowling", "123GPA623", 1997, 7.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry1.webp?alt=media&token=5d844012-206f-49c3-9174-268d4ac9961e");
			Book book8 = new Book("Harry Potter and the Prisoner of Azkaban", "Joanne Rowling", "123GPA723", 1999, 9.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry3.webp?alt=media&token=f2bfea53-6eba-44e0-9334-001784dc6876");
			Book book9 = new Book("Harry Potter and the Goblet of Fire", "Joanne Rowling", "123GPA823", 2000, 10.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry4.webp?alt=media&token=f2975a67-7f54-4923-99f9-9c345da7e490");
			Book book10 = new Book("Harry Potter and the Order of the Phoenix", "Joanne Rowling", "123GPA923", 2003, 11.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry5.webp?alt=media&token=ec85d5ee-22cb-4119-b0b8-1a37ef79bf13");
			Book book11 = new Book("Harry Potter and Half-Blood Prince", "Joanne Rowling", "123GPA233", 2005, 12.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry6.webp?alt=media&token=b9917dc5-5b49-463c-8a79-83aaeb9087f7");
			Book book12 = new Book("Harry Potter and the Deathly Hallows", "Joanne Rowling", "123GPA243", 2007, 20.5,
					crepository.findByName("Adventure").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fharry7.webp?alt=media&token=1424278e-079d-4888-951f-f6421b35c761");
			Book book13 = new Book("Anna Karenina", "Leo Tolstoy", "123GPA253", 1877, 13,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fkarenina.webp?alt=media&token=a71c78a1-a6fa-4ff5-9d55-cfac6437038d");
			Book book14 = new Book("For Whom The Bell Tolls", "Ernest Hemingway", "123GPA263", 1940, 10.5,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Ffor%20whom%20the%20bell%20tolls.webp?alt=media&token=673bb17e-1780-4db9-a084-da217fcf4445");
			Book book15 = new Book("Death in the Afternoon", "Ernest Hemingway", "123GPA273", 1932, 9.75,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fdeath.webp?alt=media&token=c1e8c6b3-a583-4cd0-8e31-77aa74388574");
			Book book16 = new Book("Romeo and Juliet", "William Shakespeare", "123GPA283", 1597, 20,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fromeo.webp?alt=media&token=f0846463-3a3e-4126-a75e-fe9307d80317");
			Book book17 = new Book("The Brothers Karamazov", "Fyodor Dostoevsky", "123GPA293", 1880, 21.25,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fbrothers.webp?alt=media&token=e7459e75-da02-4f83-bb41-f334f465c53f");
			Book book18 = new Book("The Meek One", "Fyodor Dostoevsky", "123GPA333", 1876, 7.52,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2FThe%20meek.webp?alt=media&token=52d4787a-0e1e-4b9c-9801-e80ee85a5b40");
			Book book19 = new Book("Crime And Punishment", "Fyodor Dostoevsky", "123DPA923", 1866, 40.5,
					crepository.findByName("Thriller").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fcrime.jpg?alt=media&token=f1d9fb02-5fc0-4c56-9b73-7d1d7b1da101");
			Book book20 = new Book("Ugly Love", "Colleen Hoover", "123DPA233", 2014, 32.5,
					crepository.findByName("Thriller").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fugly%20love.jpg?alt=media&token=5c7641bc-788f-49c9-b70e-92ff2534e4a4");
			Book book21 = new Book("One Of Us Is Lying", "Karen M. McManus", "123DPA243", 2017, 24.5,
					crepository.findByName("Thriller").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fone-of-us-is-lying.jpg?alt=media&token=b7e8272d-3c46-4cf7-95ca-03e06978311e");
			Book book22 = new Book("The Alchemist", "Paulo Coelho", "123DPA253", 1988, 14,
					crepository.findByName("Science fiction").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fthe-alchemist-25th-anniversary-edition.jpg?alt=media&token=62fa5d1e-15c0-4327-9806-0fbe9ecf5ae9");
			Book book23 = new Book("Great Dune", "Frank Herbert", "123DPA263", 1979, 23.5,
					crepository.findByName("Science fiction").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fgreat-dune-trilogy.jpg?alt=media&token=8e0a59c5-af91-4616-ae3a-d1285582e577");
			Book book24 = new Book("1984", "George Orwell", "123DPA273", 1949, 44.75,
					crepository.findByName("Science fiction").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2F1984.jpg?alt=media&token=a07c55e7-9a0f-45df-ae8b-7bfb3fef6987");
			Book book25 = new Book("Animal Farm", "George Orwell", "123DPA283", 1945, 10.32,
					crepository.findByName("Science fiction").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2FThe%20meek.webp?alt=media&token=52d4787a-0e1e-4b9c-9801-e80ee85a5b40");
			Book book26 = new Book("Erich Remarque", "Three Comrades", "123DPA293", 1938, 5.25,
					crepository.findByName("Romance").get(0), "https://firebasestorage.googleapis.com/v0/b/mytest-585af.appspot.com/o/covers%2Fremark.webp?alt=media&token=6c76d008-3b71-4e34-a242-5fa54c3592ce");
	
			

			repository.save(book1);
			repository.save(book2);
			repository.save(book3);
			repository.save(book4);
			repository.save(book5);
			repository.save(book6);
			repository.save(book7);
			repository.save(book8);
			repository.save(book9);
			repository.save(book10);
			repository.save(book11);
			repository.save(book12);
			repository.save(book13);
			repository.save(book14);
			repository.save(book15);
			repository.save(book16);
			repository.save(book17);
			repository.save(book18);
			repository.save(book19);
			repository.save(book20);
			repository.save(book21);
			repository.save(book22);
			repository.save(book23);
			repository.save(book24);
			repository.save(book25);
			repository.save(book26);
			
			urepository.save(new User("First", "Userok", "user", "$2a$12$GbBmxSWIa16Y1FFohxI8n.pYdZdBYlhe.s9ARBJINm8B.hvGjlpAu", "USER", "mymail@gmail.com", true));
			urepository.save(new User("First", "Admin", "admin", "$2a$12$TmL951wLuoDyNzVIUz8CDeUFQMrLrbO49Rxv.RvoO1UFxlkS0w63S", "ADMIN", "mymail2@gmail.com", true));
			urepository.save(new User("Second", "userRole", "user2", "$2a$12$GbBmxSWIa16Y1FFohxI8n.pYdZdBYlhe.s9ARBJINm8B.hvGjlpAu", "USER", "mymail232@gmail.com", true, "Finland", "Helsinki", "Kitarakuja 3 B 43", "00400"));
			
			barepository.save(new Backet(true, urepository.findByLastname("Userok").get(0)));
			barepository.save(new Backet(true, urepository.findByLastname("Admin").get(0)));
			
			barepository.save(new Backet(false, urepository.findByLastname("Userok").get(0)));
			barepository.save(new Backet(false, urepository.findByLastname("Admin").get(0)));
			barepository.save(new Backet(true, urepository.findByLastname("userRole").get(0)));
			barepository.save(new Backet(false));
			
			
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(6)), 3, barepository.findById(new Long(34)).get(), repository.findById(new Long(6)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(7)), 3, barepository.findById(new Long(34)).get(), repository.findById(new Long(7)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(8)), 2, barepository.findById(new Long(34)).get(), repository.findById(new Long(8)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(9)), 1, barepository.findById(new Long(34)).get(), repository.findById(new Long(9)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(10)), 3, barepository.findById(new Long(34)).get(), repository.findById(new Long(10)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(11)), 2, barepository.findById(new Long(34)).get(), repository.findById(new Long(11)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(12)), 1, barepository.findById(new Long(34)).get(), repository.findById(new Long(12)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(13)), 4, barepository.findById(new Long(34)).get(), repository.findById(new Long(13)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(14)), 3, barepository.findById(new Long(34)).get(), repository.findById(new Long(14)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(34), new Long(15)), 4, barepository.findById(new Long(34)).get(), repository.findById(new Long(15)).get()));

			
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(6)), 1, barepository.findById(new Long(35)).get(), repository.findById(new Long(6)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(16)), 3, barepository.findById(new Long(35)).get(), repository.findById(new Long(16)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(8)), 2, barepository.findById(new Long(35)).get(), repository.findById(new Long(8)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(9)), 1, barepository.findById(new Long(35)).get(), repository.findById(new Long(9)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(10)), 3, barepository.findById(new Long(35)).get(), repository.findById(new Long(10)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(11)), 2, barepository.findById(new Long(35)).get(), repository.findById(new Long(11)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(12)), 1, barepository.findById(new Long(35)).get(), repository.findById(new Long(12)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(13)), 4, barepository.findById(new Long(35)).get(), repository.findById(new Long(13)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(14)), 3, barepository.findById(new Long(35)).get(), repository.findById(new Long(14)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(15)), 4, barepository.findById(new Long(35)).get(), repository.findById(new Long(15)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(17)), 1, barepository.findById(new Long(35)).get(), repository.findById(new Long(17)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(35), new Long(18)), 2, barepository.findById(new Long(35)).get(), repository.findById(new Long(18)).get()));
			
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(37), new Long(17)), 1, barepository.findById(new Long(37)).get(), repository.findById(new Long(17)).get()));
			bbrepository.save(new BacketBook(new BacketBookKey(new Long(37), new Long(18)), 2, barepository.findById(new Long(37)).get(), repository.findById(new Long(18)).get()));
			
			String password = "test";
			BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
			String hashPwd = bc.encode(password);
			
			orepository.save(new Order("First", "Admin", "Finland", "Helsinki", "Juustenintie 3J 110", "00410", "In progress", "mymail@mail.com", barepository.findById(new Long(35)).get(), hashPwd));
			orepository.save(new Order("First", "Userok","Jiji", "Hur", "Mesti 28 177", "511120", "In progress", "mymail2@mail.com", barepository.findById(new Long(34)).get(), hashPwd));
			orepository.save(new Order("Aleksei", "Shevelenkov", "Hungary", "Budapest", "Fronza 24 J 102", "22304", "shevelenkov.aa@edu.spbstu.ru", barepository.findById(new Long(37)).get(), "Make my order quick please", hashPwd));
		};
	}
	 */
}
