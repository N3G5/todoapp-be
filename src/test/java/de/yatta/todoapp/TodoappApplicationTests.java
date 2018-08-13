package de.yatta.todoapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import de.yatta.todoapp.controller.TodoController;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TodoappApplicationTests {
	
	@Autowired
	private TodoController controller;
	
	@Test
	public void contextLoads() {
		assertNotNull(controller);;
	}
	
	
	

}
