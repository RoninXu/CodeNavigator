package com.codenavigator.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI codeNavigatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeNavigator API")
                        .description("智能对话引导学习框架API文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CodeNavigator Team")
                                .email("support@codenavigator.com")
                                .url("https://github.com/codenavigator"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("开发环境"),
                        new Server().url("https://api.codenavigator.com").description("生产环境")
                ));
    }
}