package de.yatta.todoapp;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;


@Configuration
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

}
