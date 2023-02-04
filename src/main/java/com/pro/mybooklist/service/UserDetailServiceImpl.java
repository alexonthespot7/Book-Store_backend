package com.pro.mybooklist.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.pro.mybooklist.MyUser;
import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;

@Service
public class UserDetailServiceImpl implements UserDetailsService {
	@Autowired
	private UserRepository urepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<User> user = urepository.findByUsername(username);
				
		MyUser myUser = null;
		
		if (user.isPresent()) {
			User currentUser = user.get();
			
			boolean enabled = currentUser.isAccountVerified();
			
			myUser = new MyUser(currentUser.getId(), username,
					currentUser.getPassword(), enabled, true, true, true,
					AuthorityUtils.createAuthorityList(currentUser.getRole()));
		} else {
			throw new UsernameNotFoundException("User (" + username + ") not found.");
		}
		
		return myUser;
	}
}
