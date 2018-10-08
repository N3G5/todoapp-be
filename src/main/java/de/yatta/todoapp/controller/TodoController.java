package de.yatta.todoapp.controller;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import de.yatta.todoapp.model.Todo;
import de.yatta.todoapp.repositories.TodoRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TodoController {

    @Autowired
    TodoRepository todoRepository;

    @GetMapping("/todos")
    public List<Todo> getAllTodos() {
    	// 2a
        Sort sortByRankingAsc = new Sort(Sort.Direction.ASC, "ranking");
        List<Todo> todos = todoRepository.findAll(sortByRankingAsc);
        // split todos into parents and childs
        List<Todo> childTodos = new ArrayList<Todo>();
        for (int p = todos.size() - 1; p >= 0; p--) {
        	if (todos.get(p).getUpperTask() != null) {
        		childTodos.add(todos.get(p));
        		todos.remove(p);
        	}     	
        }
        // add all todos with a parent to the child array of the parent
        for (int c = childTodos.size() - 1; c >= 0; c--) {
        	for (int p = todos.size() - 1; p >= 0; p--) {	
        		if (todos.get(p).getId().equals(childTodos.get(c).getUpperTask())) {
        			todos.get(p).addChild(childTodos.get(c));
        		}
        	}
        }
        return todos;
    }

    @PostMapping("/todos")
    public Todo createTodo(@Valid @RequestBody Todo todo) {
        todo.setCompleted(false);
        todo.setRanking(0);
        todo.setCreatedAt(new Date());
        // set rank up, for each other todo
        todoRepository.findAll().forEach((Todo e) -> {
        	e.setRanking(e.getRanking() + 1);
        	todoRepository.save(e);
        });
        return todoRepository.save(todo);
    }

    @GetMapping(value="/todos/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable("id") String id) {
        return todoRepository.findById(id)
                .map(todo -> ResponseEntity.ok().body(todo))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value="/todos/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable("id") String id,
                                           @Valid @RequestBody Todo todo) {
        return todoRepository.findById(id)
                .map(todoData -> {
                    todoData.setTitle(todo.getTitle());
                    todoData.setCompleted(todo.getCompleted());
                    todoData.setPriority(todo.getPriority());
                    Todo updatedTodo = todoRepository.save(todoData);
                    return ResponseEntity.ok().body(updatedTodo);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value="/todos/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable("id") String id) {
    	return todoRepository.findById(id)
                .map(todo -> {
                    todoRepository.deleteById(id);
                    // TODO: check for childs of this todo and delete them
                    // update all rankings for other todos
                    Sort sortByRankingAsc = new Sort(Sort.Direction.ASC, "ranking");
                    List<Todo> todos = todoRepository.findAll(sortByRankingAsc);
                    int rank = 0;
                    for(int i = 0; i < todos.size(); i++) {
                    	todos.get(i).setRanking(rank);
                    	todoRepository.save(todos.get(i));
                    	rank++;
                    }                    
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
    
    // 2a
    @PutMapping(value="/todos/moveup/{id}")
    public ResponseEntity<?> moveTodoUp(@PathVariable("id") String id, @Valid @RequestBody Todo todo) {
    	// get all todos sorted by ranking
    	Sort sortByRankingAsc = new Sort(Sort.Direction.ASC, "ranking");
    	List<Todo> todos = todoRepository.findAll(sortByRankingAsc);
        // find todo index of moved todo
    	int index = -1;
    	for (int i = 0; i < todos.size(); i++) {
    		if (todos.get(i).getId().equals(todo.getId()))
        		index = i;
    	}
        // switch rank with next todo
        int rank = todo.getRanking();
        todo.setRanking(todos.get(index - 1).getRanking());
        todos.get(index - 1).setRanking(rank);
        
        todoRepository.save(todo);
        todoRepository.save(todos.get(index - 1));
        
        return null;
    }    
    
    // 2a
    @PutMapping(value="/todos/movedown/{id}")
    public ResponseEntity<?> moveTodoDown(@PathVariable("id") String id, @Valid @RequestBody Todo todo) {
    	// get all todos sorted by ranking
    	Sort sortByRankingAsc = new Sort(Sort.Direction.ASC, "ranking");
    	List<Todo> todos = todoRepository.findAll(sortByRankingAsc);
        // find todo index of moved todo
    	int index = -1;    
    	for (int i = 0; i < todos.size(); i++) {
    		if (todos.get(i).getId().equals(todo.getId()))
        		index = i;
    	}
    	// switch rank with next todo
        int rank = todo.getRanking();
        todo.setRanking(todos.get(index + 1).getRanking());
        todos.get(index + 1).setRanking(rank);        
        
        todoRepository.save(todo);
        todoRepository.save(todos.get(index + 1));
        
        return null;
    }
    
}
