package com.modekoo.parse;

import com.modekoo.parse.config.ConfigBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ConfigBean.class})
public class ParseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParseApplication.class, args);
	}

}
