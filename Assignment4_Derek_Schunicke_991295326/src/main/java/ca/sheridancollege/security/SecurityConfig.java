package ca.sheridancollege.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	LoginAccessDeniedHandler accessDeniedHandler;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception{
		
		//setting authorized users for each page
		http.authorizeRequests()
			.antMatchers("/shippingHome/**").hasRole("SHIPPER")
			.antMatchers("/viewInventory/**").hasRole("SHIPPER")
			.antMatchers("/editItem/**").hasRole("SHIPPER")
			.antMatchers("/modifyItem/**").hasRole("SHIPPER")
			.antMatchers("/deleteItem/**").hasRole("SHIPPER")
			.antMatchers("/addItem/**").hasRole("SHIPPER")
			.antMatchers("/addNewItem/**").hasRole("SHIPPER")
			.antMatchers("/searchItems/**").hasRole("SHIPPER")
			.antMatchers("/searchAllItems/**").hasRole("SHIPPER")
			.antMatchers("/print/**").hasRole("SHIPPER")
			.antMatchers("/printManifest/**").hasRole("SHIPPER")
			.antMatchers("/salesHome/**").hasRole("SALES")
			.antMatchers("/viewSalesInventory/**").hasRole("SALES")
			.antMatchers("/viewSalesOrders/**").hasRole("SALES")
			.antMatchers("/editOrder/**").hasRole("SALES")
			.antMatchers("/deleteOrderItem/**").hasRole("SALES")
			.antMatchers("/modifyOrder/**").hasRole("SALES")
			.antMatchers("/addOrder/**").hasRole("SALES")
			.antMatchers("/addSalesItem/**").hasRole("SALES")
			.antMatchers("/searchSales/**").hasRole("SALES")
			.antMatchers("/searchSalesOrders/**").hasRole("SALES")
			.antMatchers("/printExcel/**").hasRole("SALES")
			.antMatchers("/printExcelDoc/**").hasRole("SALES")
			.antMatchers("/**", "/_css/**", "/generateItems/**", "/register/**", "/login").permitAll()
		.and()
			.formLogin().loginPage("/login").permitAll()
		.and()
			.logout().invalidateHttpSession(true).clearAuthentication(true)
			.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
			.logoutSuccessUrl("/login?logout=logout")
		.and()
			.exceptionHandling().accessDeniedHandler(accessDeniedHandler);
		
	}
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		
		return new BCryptPasswordEncoder();
		
	}
	
	@Autowired
	UserDetailsServiceImpl userDetailsService;
	
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception{
		
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		
	}
	
}
