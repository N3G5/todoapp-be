package de.yatta.todoapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

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
		todo.setPriority(Priority.MEDIUM);
		assertEquals(Priority.MEDIUM, todo.getPriority());
	}
	
	@Test
	public void testChangeRanking() {
		Todo todo = new Todo("First");
		todo.setRanking(2);
		assertEquals(2, todo.getRanking());
	}
	
	@Test
	public void testMoveTodoUp() {
		todoRepo.deleteAll();
		controller.createTodo(new Todo("Bottom"));
		controller.createTodo(new Todo("Top"));
		/* Index	Title
		 * 0		Top
		 * 1		Bottom
		 */
		List<Todo> todos = controller.getAllTodos();
		// move "Bottom" one up
		controller.moveTodoUp(todos.get(1).getId(), todos.get(1));
		/* Index	Title
		 * 0		Bottom
		 * 1		Top
		 */
		todos = controller.getAllTodos();
		assertEquals("Bottom", todos.get(0).getTitle());
		assertEquals("Top", todos.get(1).getTitle());		
	}

	@Test
	public void testMoveTodoDown() {
		todoRepo.deleteAll();
		controller.createTodo(new Todo("Bottom"));
		controller.createTodo(new Todo("Top"));
		/* Index	Title
		 * 0		Top
		 * 1		Bottom
		 */
		List<Todo> todos = controller.getAllTodos();
		// move "Top" one down
		controller.moveTodoDown(todos.get(0).getId(), todos.get(0));
		/* Index	Title
		 * 0		Bottom
		 * 1		Top
		 */
		todos = controller.getAllTodos();
		assertEquals("Bottom", todos.get(0).getTitle());
		assertEquals("Top", todos.get(1).getTitle());		
	}
	
}
