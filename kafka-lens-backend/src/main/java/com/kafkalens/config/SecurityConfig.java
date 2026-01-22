package com.kafkalens.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정.
 *
 * <p>Basic Authentication을 사용하여 API 엔드포인트를 보호합니다.</p>
 *
 * <h3>보안 설정</h3>
 * <ul>
 *     <li>/api/** - 인증 필요</li>
 *     <li>/actuator/health - 공개 접근 허용</li>
 *     <li>기타 엔드포인트 - 공개 접근 허용</li>
 * </ul>
 *
 * <h3>CORS 설정</h3>
 * <p>프론트엔드(localhost:3000, localhost:5173)에서의 접근을 허용합니다.</p>
 *
 * @see SecurityProperties
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    /**
     * SecurityConfig 생성자.
     *
     * @param securityProperties 보안 설정 프로퍼티
     */
    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Security Filter Chain 설정.
     *
     * <p>HTTP 보안 설정을 구성합니다.</p>
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 중 오류 발생 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API이므로)
                .csrf(AbstractHttpConfigurer::disable)
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 세션 관리 - 무상태
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // Actuator 헬스 체크는 공개
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // API 엔드포인트는 인증 필요
                        .requestMatchers("/api/**").authenticated()
                        // 기타 요청은 공개
                        .anyRequest().permitAll()
                )
                // HTTP Basic 인증 사용
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * UserDetailsService 설정.
     *
     * <p>application.yml에서 설정된 사용자 정보를 로드합니다.</p>
     *
     * @return UserDetailsService 구현체
     */
    @Bean
    public UserDetailsService userDetailsService() {
        List<UserDetails> users = securityProperties.getUsers().stream()
                .map(this::createUserDetails)
                .toList();

        return new InMemoryUserDetailsManager(users);
    }

    /**
     * SecurityProperties.User를 UserDetails로 변환.
     *
     * @param user SecurityProperties.User 객체
     * @return UserDetails 객체
     */
    private UserDetails createUserDetails(SecurityProperties.User user) {
        return User.builder()
                .username(user.getUsername())
                .password(passwordEncoder().encode(user.getPassword()))
                .roles(user.getRoles().toArray(new String[0]))
                .build();
    }

    /**
     * 비밀번호 인코더 설정.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정.
     *
     * <p>프론트엔드 개발 서버에서의 접근을 허용합니다.</p>
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 허용할 Origin
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",  // React 개발 서버
                "http://localhost:5173"   // Vite 개발 서버
        ));
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));
        // 자격 증명 허용
        configuration.setAllowCredentials(true);
        // 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
