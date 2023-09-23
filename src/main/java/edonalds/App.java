package edonalds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableWebMvc
//@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class App implements WebMvcConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //CorsRegistration corsRegistration = registry.addMapping("/**").allowedMethods("GET", "POST", "OPTIONS", "PUT", "DELETE");
    }

//    @Bean(name = "auditingDateTimeProvider")
//    public DateTimeProvider dateTimeProvider() {
//        return () -> Optional.of(OffsetDateTime.now());
//    }

}
