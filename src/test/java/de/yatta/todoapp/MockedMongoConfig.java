package de.yatta.todoapp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;


public class MockedMongoConfig extends AbstractMongoConfiguration{
	
	@Autowired
	Environment env;

	@Override
	public MongoClient mongoClient() {
		return new Fongo(getDatabaseName()).getMongo();
	}

	@Override
	protected String getDatabaseName() {
		return env.getRequiredProperty("spring.data.mongodb.database");
	}
	

}
