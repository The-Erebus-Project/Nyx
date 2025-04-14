package io.github.vizanarkonin.nyx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class NyxApplication extends SpringBootServletInitializer {

	@Override
   	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
      	return application.sources(NyxApplication.class);
   	}
    
	public static void main(String[] args) {
		SpringApplication.run(NyxApplication.class, args);
	}
}
