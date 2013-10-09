package models.enumerate;

public enum AccumulationBehaviour {

	nothing("niente"),
	replaceCodeAndDecreaseAccumulation("sostituisce il codice e decrementa l'accumulo"),
	noMoreAbsencesAccepted("non accetta ulteriori assenze");
	
	public String description;
	
	private AccumulationBehaviour(String description){
		this.description = description;
	}
	
	public static AccumulationBehaviour getByDescription(String description){
		if(description.equals("niente"))
			return AccumulationBehaviour.nothing;
		if(description.equals("sostituisce il codice e decrementa l'accumulo"))
			return AccumulationBehaviour.replaceCodeAndDecreaseAccumulation;
		if(description.equals("non accetta ulteriori assenze"))
			return AccumulationBehaviour.noMoreAbsencesAccepted;
		
		return null;
	}
}
