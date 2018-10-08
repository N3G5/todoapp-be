package de.yatta.todoapp.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "todos")
@JsonIgnoreProperties(value = { "createdAt" }, allowGetters = true)
public class Todo {
	@Id
	private String id;

	@NotBlank
	@Size(max = 100)
	@Indexed(unique = true)
	private String title;

	private Boolean completed = false;

	private Date createdAt; // 2a
	
	private Priority priority; // 1a
	
	private int ranking; // 2a
		
	private String upperTask;
	
	@Transient
	private List<Todo> childs; // 3a
	
	public Todo() {
		super();
	}

	public Todo(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Boolean getCompleted() {
		return completed;
	}

	public void setCompleted(Boolean completed) {
		this.completed = completed;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	
	// 2a
	public int getRanking() {
		return this.ranking;
	}
	
	// 2a
	public void setRanking(int ranking) {
		this.ranking = ranking;
	}

	// 1a
	public Priority getPriority() {
		return priority;
	}
	
	// 1a
 	public void setPriority(Priority todoPrio) {
		this.priority = todoPrio;
	}
	
	// 3a
	public String getUpperTask() {
		return this.upperTask;
	}
	
	// 3a
 	public void setUpperTask(String upperTask) {
		this.upperTask = upperTask;
	}

	// 3a
	public List<Todo> getChilds() {
		return this.childs;
	}
	
	// 3a
 	public void addChild(Todo child) {
		if (this.childs == null)
			this.childs = new ArrayList<Todo>();
 		this.childs.add(child);
	}
 	
 	
	@Override
	public String toString() {
		return String.format("Todo[id=%s, title='%s', completed='%s']", id, title, completed);
	}

}
