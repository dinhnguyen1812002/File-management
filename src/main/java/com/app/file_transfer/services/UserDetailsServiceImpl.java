package com.app.file_transfer.services;


import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String msg = "User not found";
        User user = userRepository.findByUsername(username);
         if (user == null) {
                throw new UsernameNotFoundException(msg);
            }
        return UserDetailsImpl.build(user);
    }
}
