package core;
/*
 * Task class that will be used for creation of task objects.
 * Task objects will be converted into GSON objects which will be handled by storage
 */

public class Task {

	protected String taskName;
	protected String taskDescription;
	//private Date time;
	//private String priority;
	
	// Constructor method
	public Task(String tName, String tDesc){
		this.taskName = tName;
		this.taskDescription = tDesc;
	}

	// Empty constructor
	public Task(){		

	}
	
	public void setTaskName(String tName){
		this.taskName = tName;
	}
	
	public String getTaskName(){
		return this.taskName;
	}
	
	public void setTaskDescription(String tDesc){
		this.taskDescription = tDesc;
	}
	
	public String getTaskDescription(){
		return this.taskDescription;
	}
	
	// Override method for toString
	public String toString() {
		return new StringBuffer(" Task Name : ").append(this.taskName)
		                .append(" Task Description : ").append(this.taskDescription).toString();
	}
}
