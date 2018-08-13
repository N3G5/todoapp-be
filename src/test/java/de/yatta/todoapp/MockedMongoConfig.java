package de.yatta.todoapp;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;


@Configuration
@EnableMongoRepositories
@ComponentScan
@Profile("test")
public class MockedMongoConfig extends AbstractMongoConfiguration{

	@Override
	public MongoClient mongoClient() {
		Fongo fongo = new Fongo(getDatabaseName());
		return fongo.getMongo();
	}

	@Override
	protected String getDatabaseName() {
		return "todoapp";
	}
	
	@Bean
	public MongoTemplate mongoTemplate() {
		return new MongoTemplate(mongoClient(), getDatabaseName());
	}

}
