package de.yatta.todoapp.controller;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import de.yatta.todoapp.model.Todo;
import de.yatta.todoapp.repositories.TodoRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TodoController {

    @Autowired
    TodoRepository todoRepository;

    @GetMapping("/todos")
    public ResponseEntity<List<Todo>> getAllTodos() {
        List<Todo> todos = todoRepository.findAll();
        // if there are no todos return 404
        if (todos == null || todos.size() == 0) {
        	return ResponseEntity.ok().body(new ArrayList<Todo>());        	
        }
        
        int maxLayer = this.getMaxLayer(todos);
        // sort by layer (high to low) and count max layer
        ArrayList<ArrayList<Todo>> layers = this.create2DTodoArrayList(maxLayer);        
        
        for (int p = todos.size() - 1; p >= 0; p--) {
        	layers.get(this.getLayer(todos.get(p))).add(todos.get(p));
        }
        // add childs from highest layer to lowest (layer 1 are the childs of parent, so only go up to layer 2)        
        for (int l = layers.size() - 1; l >= 2; l--) {
        	ArrayList<Todo> childLayer = layers.get(l);
        	ArrayList<Todo> parentLayer = layers.get(l - 1);
        	parentLayer = this.mapChildsToParents(parentLayer, childLayer);        	
        }
        // add layer 1 to parent, if childs exist
        if (maxLayer > 1) {
            layers.set(0, this.mapChildsToParents(layers.get(0), layers.get(1)));
        }

        // now the structure is done, so order parents and return
        layers.set(0, this.sortListByLayer(layers.get(0), 0));
        return ResponseEntity.ok().body(layers.get(0));
    }

    @PostMapping("/todos")
    public ResponseEntity createTodo(@Valid @RequestBody Todo todo) {
        // title is unique, so check if title already exists.
    	if (this.getTodoByTitle(todo.getTitle()) != null)
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is already a Todo with the title " + todo.getTitle() + ".");
    	
    	// new todo can not be completed & add creation date server side
    	todo.setCompleted(false);
        todo.setCreatedAt(new Date());
        // for a parent todo, just add the rank 0
        if (todo.getUpperTask() == null || todo.getUpperTask().equals("")) {
        	todo.getRanking().add(0);
        } else {
        	// for child todo, get the rank of the parent and apply 0
        	ArrayList<Integer> parentRank = todoRepository.findById(todo.getUpperTask()).get().getRanking();
        	parentRank.add(0);
        	todo.setRanking(parentRank);
        }

        int layer = this.getLayer(todo);
        // set rank up for each other todo on this or a higher layer
        todoRepository.findAll().forEach((Todo e) -> {
        	// if this todo has the same parent as the new todo, increase its rank this layer
        	if (e.getRanking().size() - 1 >= layer) {
        		todoRepository.save(this.setRankUp(e, layer));
        	}
        });
        return ResponseEntity.ok().body(todoRepository.save(todo));
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
		// get todo to delete, if it does not exist, return error
    	Todo deleteTodo;
    	try {
			deleteTodo = todoRepository.findById(id).get();	
		} catch (NoSuchElementException nsee) {
			return ResponseEntity.badRequest().build();
		}
		// get ranking list, layer, rank in layer to delete
		ArrayList<Integer> rankingToDelete = deleteTodo.getRanking();		
		int layer = this.getLayer(rankingToDelete);
	    int rank = rankingToDelete.get(layer);
	    
		List<Todo> todos = todoRepository.findAll();
	    for (int t = todos.size() - 1; t >= 0; t--) {
    		// now delete all todos, where the ranking is the same (childs of the todo to delete)
	    	if (this.isChildOf(todos.get(t), rankingToDelete)) {
	    		todoRepository.delete(todos.get(t));
	    		todos.remove(t);
	    	} else if (todos.get(t).getRanking().size() -1 >= layer && todos.get(t).getRanking().get(layer) > rank) {
	    		// set rankings down for each todo, that is behind the one to delete
	    		todoRepository.save(this.setRankDown(todos.get(t), layer));
	    	}
	    }
	    
	    // delete actual todo
	    todoRepository.deleteById(id);
	    return ResponseEntity.ok().build();
    }
    
    // 2a
    @PutMapping(value="/todos/moveup/{id}")
    public ResponseEntity<?> moveTodoUp(@PathVariable("id") String id, @Valid @RequestBody Todo todo) {
    	List<Todo> todos = todoRepository.findAll();
    	// find todo to move up
    	int layer = this.getLayer(todo);
    	for (int c = 0; c < todos.size(); c++) {
    		if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer) -1) {
        		// if found, move up
    			todoRepository.save(this.setRankUp(todos.get(c), layer));
    		} else if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer)) {
        		// move todo & his childs down
    			todoRepository.save(this.setRankDown(todos.get(c), layer));
    		}
    	}
        return ResponseEntity.ok().body(true);
    }

    // 2a
    @PutMapping(value="/todos/movedown/{id}")
    public ResponseEntity<?> moveTodoDown(@PathVariable("id") String id, @Valid @RequestBody Todo todo) {    	
    	List<Todo> todos = todoRepository.findAll();
    	// find todo to move up
    	int layer = todo.getRanking().size() - 1;
    	for (int c = 0; c < todos.size(); c++) {
    		if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer) + 1) {
        		// if found, move up
    			todoRepository.save(this.setRankDown(todos.get(c), layer));
    		} else if (todos.get(c).getRanking().size() -1 >= layer && todos.get(c).getRanking().get(layer) == todo.getRanking().get(layer)) {
        		// move todo & his childs down
    			todoRepository.save(this.setRankUp(todos.get(c), layer));
    		}
    	}
    	return ResponseEntity.ok().body(true);
    }

    //################################################################################
    // HELPER FUNCTIONS
    //################################################################################
    
    // return the max layer, of a one dimension arraylist (without childs)
    public int getMaxLayer(List<Todo> todos) {
    	int max = 0;
    	for (int t = 0; t < todos.size(); t++) {        	
        	if (todos.get(t).getRanking().size() > max) {
        		max = todos.get(t).getRanking().size();
        	}
        }
    	return max;
    }
    
    // calc & return the layer of the todo
    public int getLayer(Todo todo) {
    	 return todo.getRanking().size() -1;
    }
    public int getLayer(ArrayList<Integer> rank) {
   	 return rank.size() -1;
   }
    
    // get todo by title (null if not found)
    public Todo getTodoByTitle(String title) {
    	List<Todo> todos = todoRepository.findAll();
    	for (int t = 0; t < todos.size(); t++) {
    		if (todos.get(t).getTitle().equals(title))
    			return todos.get(t);
    	}
    	return null;
    }
    
    // set the rank up for one todo on this layer
    public Todo setRankUp(Todo todo, int layer) {
		todo.getRanking().set(layer, todo.getRanking().get(layer) + 1);
    	return todo;
    }
    
    // set the rank up for one todo on this layer
    public Todo setRankDown(Todo todo, int layer) {
		todo.getRanking().set(layer, todo.getRanking().get(layer) - 1);
    	return todo;
    }
    
    // returns true, if the todo has the same ranking or is a child of this
    public boolean isChildOf(Todo todo, ArrayList<Integer> rank) {
    	ArrayList<Integer> currentRanking = todo.getRanking();
    	// check if current todo is on higher ranking
    	if (currentRanking.size() <= rank.size()) {
    		return false;
    	}
    	// check that each rank matches
    	boolean delQ = true;
    	for (int r = 0; r < rank.size(); r++) {                    		
    		if (rank.get(r) != currentRanking.get(r)) {
    			delQ = false;
    		}
    	}
    	return delQ;
    }
    
    // create an empty 2D arraylist of type todo
    public ArrayList<ArrayList<Todo>> create2DTodoArrayList(int size) {
    	ArrayList<ArrayList<Todo>> list = new ArrayList<ArrayList<Todo>>();
        for (int m = 0; m < size; m++) {
        	list.add(new ArrayList<Todo>());
        }
        return list;
    }
    
    // sort an todo arraylist by the rank of the given layer
    public ArrayList<Todo> sortListByLayer(ArrayList<Todo> todos, int layer) {
    	todos.sort((a, b) -> {
        	if (a.getRanking().get(layer) == b.getRanking().get(layer))
        		return 0;
    		if (a.getRanking().get(layer) < b.getRanking().get(layer))
        		return -1;
        	else
        		return 1;
        });
    	return todos;
    }
    
    // map all childs to parents by id == upperTask
    public ArrayList<Todo> mapChildsToParents(ArrayList<Todo> parents, ArrayList<Todo> childs) {
    	for (int c = childs.size() - 1; c >= 0; c--) {
        	for (int p = parents.size() - 1; p >= 0; p--) {
        		if (parents.get(p).getId().equals(childs.get(c).getUpperTask())) {
    				parents.get(p).addChild(childs.get(c));
    			}
        	}
        }
    	return parents;
    }
    
}
