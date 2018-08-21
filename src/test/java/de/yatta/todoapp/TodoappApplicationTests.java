package de.yatta.todoapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import de.yatta.todoapp.controller.TodoController;
import de.yatta.todoapp.model.Priority;
import de.yatta.todoapp.model.Todo;
import de.yatta.todoapp.repositories.TodoRepository;

@RunWith(SpringRunner.class)
@DataMongoTest
@TestPropertySource("classpath:application.test.properties")
@ActiveProfiles("test")
@ContextConfiguration(classes = TodoappApplication.class)
public class TodoappApplicationTests {
	
	@Autowired TodoRepository todoRepo;
	
	@Autowired
	private TodoController controller;
	
	@Test
	public void contextLoads() {
		assertNotNull(controller);
		assertNotNull(todoRepo);
	}
	
	@Test
	public void testSaveInRepo() {
		Todo todo = todoRepo.save(new Todo("First todo"));
		Todo foundTodo = todoRepo.findById(todo.getId()).get();
		assertNotNull(foundTodo);
		assertEquals(todo.getTitle(), foundTodo.getTitle());
	}
	
	@Test
	public void testPriority() {
		Todo todo = new Todo("First todo");
		assertEquals("First todo", todo.getTitle());
		todo.setPriority(Priority.HIGH);
		assertEquals(Priority.HIGH, todo.getPriority());
	}

	
}
