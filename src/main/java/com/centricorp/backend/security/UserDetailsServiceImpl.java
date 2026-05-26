package com.centricorp.backend.security;

import com.centricorp.backend.entity.Usuario;
import com.centricorp.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService que carga usuarios desde la tabla "usuarios".
 * Spring Security llama a este servicio durante la autenticación para obtener
 * el UserDetails y luego compara la contraseña con el PasswordEncoder configurado.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username
                ));

        // Rol base — mapeamos el valor de la columna 'rol' (ej. ADMIN, USUARIO) a ROLE_ADMIN, ROLE_USUARIO
        String roleName = (usuario.getRol() != null && !usuario.getRol().isEmpty()) 
                ? "ROLE_" + usuario.getRol().toUpperCase() 
                : "ROLE_USUARIO"; // Default fallback

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(roleName)))
                .build();
    }
}
