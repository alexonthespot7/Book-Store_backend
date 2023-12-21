package com.pro.mybooklist;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pro.mybooklist.model.Backet;
import com.pro.mybooklist.model.BacketRepository;

@Component
public class ScheduledTasks {
	@Autowired
	private BacketRepository barepository;

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
	@Transactional
	public void deleteUnusedCarts() {
		List<Backet> backets = (List<Backet>) barepository.findAll();
		
		if (backets.size() > 0) {
			for (int i = 0; i < backets.size(); i++) {
				if (backets.get(i).getUser() == null && backets.get(i).isCurrent()) {
					if (LocalDate.now().isAfter(LocalDate.parse(backets.get(i).getExpiryDate()))) {
						barepository.delete(backets.get(i));
					}
				}
			}
		}
	}
}
