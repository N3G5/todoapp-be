package de.yatta.todoapp;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;


@ComponentScan
@Profile("test")
public class MockedMongoConfig extends AbstractMongoConfiguration{

	@Override
	@Bean
	public MongoClient mongoClient() {
		Fongo fongo = new Fongo("Fongo");
		return fongo.getMongo();
	}

	@Override
	protected String getDatabaseName() {
		return "todoapp";
	}
	

}
