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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TodoController {

    @Autowired
    TodoRepository todoRepository;

    @GetMapping("/todos")
    public List<Todo> getAllTodos() {
    	// 2a
        List<Todo> todos = todoRepository.findAll();
        if (todos == null || todos.size() == 0) {
        	return new ArrayList<Todo>();
        }
        ArrayList<Todo> parents = new ArrayList<Todo>();
        int maxLayer = 0;
        // add parents
        for (int t = todos.size() - 1; t >= 0; t--) {
        	// also calc max Layer        	
        	if (todos.get(t).getRanking().size() > maxLayer) {
        		maxLayer = todos.get(t).getRanking().size();
        	}
        	if (todos.get(t).getRanking().size() == 1) {
        		parents.add(todos.get(t));
        		todos.remove(t);
        	}        	
        }        
        // sort by layer (high to low) and count max layer
        ArrayList<ArrayList<Todo>> layer = new ArrayList<ArrayList<Todo>>();
        for (int m = 0; m < maxLayer; m++) {
        	layer.add(new ArrayList<Todo>());
        }
        for (int p = todos.size() - 1; p >= 0; p--) {
        	int index = todos.get(p).getRanking().size() - 1;
        	layer.get(index).add(todos.get(p));
        }        
        // add childs from highest layer to lowest (layer 1 are the childs of parent, so dont use them here)        
        for (int l = layer.size() - 1; l >= 2; l--) {
        	ArrayList<Todo> currentLayer = layer.get(l);
        	ArrayList<Todo> prevLayer = layer.get(l - 1);
        	for (int t = 0; t < currentLayer.size(); t++) {
        		for (int p = 0; p < prevLayer.size(); p++) { 
        			if (prevLayer.get(p).getId().equals(currentLayer.get(t).getUpperTask())) {
        				prevLayer.get(p).addChild(currentLayer.get(t));
        			}
        		}
        	}
        }
        // add layer 1 to parent, if exist
        if (maxLayer > 1) {
        	ArrayList<Todo> childs = layer.get(1);
            for (int c = childs.size() - 1; c >= 0; c--) {
            	for (int p = parents.size() - 1; p >= 0; p--) {
            		if (parents.get(p).getId().equals(childs.get(c).getUpperTask())) {
        				parents.get(p).addChild(childs.get(c));
        			}
            	}
            }
        }

        // now the structure is done, but the parents are probally not ordered
        parents.sort((a, b) -> {
        	if (a.getRanking().get(0) < b.getRanking().get(0)) {
        		return -1;	
        	} else {
        		return 1;
        	}        	
        });
        return parents;
    }

    @PostMapping("/todos")
    public Todo createTodo(@Valid @RequestBody Todo todo) {
        // TODO: check if the new title already exists.
        // - title is unique, so the client needs to receive an error, when title is duplicated
    	
    	todo.setCompleted(false);
        todo.setCreatedAt(new Date());
        // if todo has no parent, add it with rank 0
        if (todo.getUpperTask() == null || todo.getUpperTask().equals("") ) {
        	ArrayList<Integer> newRank = new ArrayList<Integer>();
        	newRank.add(0);
        	todo.setRanking(newRank);                	
            // set rank up, for each other todo
            todoRepository.findAll().forEach((Todo e) -> {
            	ArrayList<Integer> temp = e.getRanking();
            	temp.set(0, temp.get(0) + 1);
            	e.setRanking(temp);
            	todoRepository.save(e);
            });
        } else {
        	// if todo is on a child layer
        	// get parent
        	Optional<Todo> optParentTodo = todoRepository.findById(todo.getUpperTask());
        	Todo parentTodo = optParentTodo.get();
        	ArrayList<Integer> parentTodoRank = parentTodo.getRanking();
        	ArrayList<Integer> newRank = new ArrayList<Integer>();
        	newRank = parentTodoRank;
        	newRank.add(0);
        	todo.setRanking(newRank);
        	int layer = todo.getRanking().size() -1;        	
            // set rank up, for each other todo on this layer
            todoRepository.findAll().forEach((Todo e) -> {
            	// if this todo has the same parent as the new todo, increase its rank this layer
            	if (e.getRanking().size() - 1 >= layer) {
            		e.getRanking().set(layer, e.getRanking().get(layer) + 1);            		
            		todoRepository.save(e);
            	}            	
            });
        }
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
                	// get ranking of todo to delete
                	Optional<Todo> optDeleteTodo = todoRepository.findById(id);                	
                	Todo deleteTodo = optDeleteTodo.get();
                	ArrayList<Integer> rankingToDelete = deleteTodo.getRanking();
                	// now delete all todos, where the ranking is the same
                	List<Todo> todos = todoRepository.findAll();
                    for (int t = 0; t < todos.size(); t++) {
                    	ArrayList<Integer> currentRanking = todos.get(t).getRanking();
                    	// check if current todo is on higher ranking than todo to delete
                    	if (currentRanking.size() <= rankingToDelete.size()) {
                    		continue;
                    	}
                    	boolean delQ = true;
                    	for (int r = 0; r < rankingToDelete.size(); r++) {                    		
                    		if (rankingToDelete.get(r) != currentRanking.get(r)) {
                    			delQ = false;
                    		}
                    	}
                    	if (delQ) {
                    		todoRepository.delete(todos.get(t));
                    	}
                    }

                    // apply new rankings for this layer                    
                    int layer = rankingToDelete.size() - 1;
                    int rank = rankingToDelete.get(layer);                    
                    todos = todoRepository.findAll();
                    for (int t = todos.size() - 1; t >= 0; t--) {
                    	if (todos.get(t).getRanking().size() -1 >= layer && todos.get(t).getRanking().get(layer) > rank) {
                    		todos.get(t).getRanking().set(layer, todos.get(t).getRanking().get(layer) - 1);
                    		todoRepository.save(todos.get(t));
                    	}
                    }                    

                    // delete parent todo
                    todoRepository.deleteById(id);                                       
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
    
    // 2a
    @PutMapping(value="/todos/moveup/{id}")
    public ResponseEntity<?> moveTodoUp(@PathVariable("id") String id, @Valid @RequestBody Todo todo) {
    	// TODO: even if the front end checks that u dont move the first and last todo,
    	// its a good idea to check server side again
    	List<Todo> todos = todoRepository.findAll();
    	// find todo to move up
    	int layer = todo.getRanking().size() - 1;
    	for (int c = 0; c < todos.size(); c++) {
    		// if found, move up
    		if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer) -1) {
    			todos.get(c).getRanking().set(layer, todos.get(c).getRanking().get(layer) + 1);
    			todoRepository.save(todos.get(c));
    		} else if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer)) {
        		// move todo & his childs down
    			todos.get(c).getRanking().set(layer, todos.get(c).getRanking().get(layer) - 1);
    			todoRepository.save(todos.get(c));
    		}
    	}
        return null;
    }    

    // 2a
    @PutMapping(value="/todos/movedown/{id}")
    public ResponseEntity<?> moveTodoDown(@PathVariable("id") String id, @Valid @RequestBody Todo todo) {    	
    	// TODO: even if the front end checks that u dont move the first and last todo,
    	// its a good idea to check server side again
    	List<Todo> todos = todoRepository.findAll();
    	// find todo to move up
    	int layer = todo.getRanking().size() - 1;
    	for (int c = 0; c < todos.size(); c++) {
    		// if found, move up
    		if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer) + 1) {
    			todos.get(c).getRanking().set(layer, todos.get(c).getRanking().get(layer) - 1);
    			todoRepository.save(todos.get(c));
    		} else if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer)) {
        		// move todo & his childs down
    			todos.get(c).getRanking().set(layer, todos.get(c).getRanking().get(layer) + 1);
    			todoRepository.save(todos.get(c));
    		}
    	}
        return null;
    }

}
