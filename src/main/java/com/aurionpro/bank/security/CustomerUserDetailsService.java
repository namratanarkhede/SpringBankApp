package com.aurionpro.bank.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aurionpro.bank.entity.User;
import com.aurionpro.bank.repo.UserRepo;



@Service
public class CustomerUserDetailsService implements UserDetailsService{

	@Autowired
	private UserRepo userRepo;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User user = userRepo.findByUsername(username).orElseThrow(
				()-> new UsernameNotFoundException("User not found"));
		
		Set<GrantedAuthority> authorities = user
				.getRoles()
				.stream()
				.map((role)->new SimpleGrantedAuthority(role.getRoleName()))
				.collect(Collectors.toSet());
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
	}

	
}
