package hrm.modules.core.auth;


import hrm.base.common.exception.handler.CustomRestExceptionHandler;
import hrm.modules.core.auth.filter.GuestAuthenticationFilter;
import hrm.modules.core.auth.filter.JwtAuthenticationFilter;

import hrm.modules.core.auth.token.provider.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Order(1)
    @EnableWebSecurity
    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        private CustomRestExceptionHandler customRestExceptionHandler;

        @Autowired
        private CustomUserDetailsService customUserDetailsService;

        @Autowired
        private JwtProvider tokenProvider;

        public JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(tokenProvider, customUserDetailsService);
        }

        public GuestAuthenticationFilter guestAuthenticationFilter() {
            return new GuestAuthenticationFilter();
        }

        @Bean(BeanIds.AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.userDetailsService(customUserDetailsService) // Cung cáp userservice cho spring security
                    .passwordEncoder(passwordEncoder()); // cung cấp password encoder
        }

        private static final String[] AUTH_WHITELIST = {
                // -- Swagger UI v2
                "/v2/api-docs", "/swagger-resources", "/swagger-resources/**", "/configuration/ui",
                "/configuration/security", "/swagger-ui.html", "/webjars/**",
                // -- Swagger UI v3 (OpenAPI)
                "/v3/api-docs/**", "/swagger-ui/**"
                // other public endpoints of your API may be appended to this array
        };

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers(AUTH_WHITELIST);
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            CorsConfiguration corsConfiguration = new CorsConfiguration().applyPermitDefaultValues();
            corsConfiguration.addAllowedMethod("*");
            source.registerCorsConfiguration("/**",
                    corsConfiguration);
            return source;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .exceptionHandling()
                    .authenticationEntryPoint(customRestExceptionHandler)
                    .and().csrf().disable().cors().and()
                    .authorizeRequests()
//                    .antMatchers(AUTH_WHITELIST).permitAll()
                    .antMatchers("/auth/login").permitAll()
                    .antMatchers("/user/register").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(guestAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        }
    }
}