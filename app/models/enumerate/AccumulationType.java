package models.enumerate;

public enum AccumulationType {

	no("no"),
	yearly("annuale"),
	monthly("mensile"),
	always("sempre");
	
	public String description;
	
	private AccumulationType(String description){
		this.description = description;
	}
}
