package br.com.buscapeca.buscapecaapi;

import br.com.buscapeca.buscapecaapi.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class BuscaPecaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuscaPecaApiApplication.class, args);
    }

}
