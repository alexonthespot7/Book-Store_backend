package com.pro.mybooklist;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pro.mybooklist.model.User;
import com.pro.mybooklist.model.UserRepository;
import com.pro.mybooklist.service.AuthenticationService;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
	@Autowired
	private AuthenticationService jwtService;
	
	@Autowired
	private UserRepository urepository;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		
		String jws = request.getHeader(HttpHeaders.AUTHORIZATION);
		
		if (jws != null) {
			String user = jwtService.getAuthUser(request);
			
			Optional<User> curruser = urepository.findByUsername(user);
			
			Authentication authentication;
			
			if (curruser.isPresent()) {
				boolean enabled = curruser.get().isAccountVerified();
				MyUser myUser = new MyUser(curruser.get().getId(), user,
						curruser.get().getPassword(), enabled, true, true, true,
						AuthorityUtils.createAuthorityList(curruser.get().getRole()));
				authentication = new UsernamePasswordAuthenticationToken(myUser, null, AuthorityUtils.createAuthorityList(curruser.get().getRole()));
			} else {
				authentication = new UsernamePasswordAuthenticationToken(12, null, Collections.emptyList());
			}
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		
		filterChain.doFilter(request, response);
	}

}
