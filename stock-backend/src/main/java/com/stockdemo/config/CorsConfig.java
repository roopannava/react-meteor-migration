package com.stockdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the two frontends' dev servers to call the API from the browser.
 *
 * <p>The React+Redux (Vite) dev server runs on {@code :5173} and calls the API
 * directly in the browser, so it needs CORS. The Meteor app ({@code :3000})
 * fetches server-side via a Method, which does not need CORS, but its origin is
 * allowed here too for convenience. In production you would serve the frontend
 * from the same origin and drop this.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET");
    }
}
