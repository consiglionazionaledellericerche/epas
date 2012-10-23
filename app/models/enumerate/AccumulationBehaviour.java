package models.enumerate;

public enum AccumulationBehaviour {

	nothing("niente"),
	replaceCodeAndDecreaseAccumulation("sostituisce il codice e decrementa l'accumulo"),
	noMoreAbsencesAccepted("non accetta ulteriori assenze");
	
	public String description;
	
	private AccumulationBehaviour(String description){
		this.description = description;
	}
}
