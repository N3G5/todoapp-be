package de.yatta.todoapp.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import de.yatta.todoapp.model.Todo;

@Repository
public interface TodoRepository extends MongoRepository<Todo, String>{

}
