package br.com.hackgov.security;

import br.com.hackgov.repository.CidadaoRepository;
import br.com.hackgov.repository.ServidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final CidadaoRepository cidadaoRepo;
    private final ServidorRepository servidorRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return cidadaoRepo.findByEmail(email)
                .map(AuthenticatedUser::fromCidadao)
                .map(u -> (UserDetails) u)
                .orElseGet(() -> servidorRepo.findByEmailFuncional(email)
                        .map(AuthenticatedUser::fromServidor)
                        .map(u -> (UserDetails) u)
                        .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas")));
    }
}
