/**
 * 
 */
package helpers.attestati;

/**
 * Contiene le informazioni essenziali prelevate dal sistema centrale del CNR
 * E' utilizzata anche per effettuare i controlli di esistenza delle persone 
 * sia nel sistema centrale del CNR che in ePAS
 * 
 * @author cristian
 *
 */
public final class Dipendente implements Comparable<Dipendente> {

	private final String matricola, cognomeNome;
	
	public Dipendente(final String matricola, final String nomeCognome) {
		this.matricola = matricola;
		this.cognomeNome = nomeCognome;
	}

	public String getMatricola() { return matricola; }
	public String getCognomeNome() { return cognomeNome; }
	
	/** 
	 * Metodo necessario per i controlli di "contains" dei Set 
	 * Se è presente una matricola si usa quella per i confronti, altrimeni si utilizza il nome e cognome
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//Se è presente una matricola si usa quella per i confronti, altrimeni si utilizza il nome e cognome
		if (matricola == null || matricola.isEmpty()) {
			result = prime * result
					+ ((cognomeNome == null) ? 0 : cognomeNome.toUpperCase().replace(" ",  "").hashCode());				
		} else {
			result = prime * result
					+ ((matricola == null || matricola.equals("")) ? 0 : matricola.hashCode());
		}
		return result;
	}


	/**
	 * Metodo necessario per i controlli di "contains" dei Set
	 * Se per entrambi gli oggetti confrontati è presente una matricola 
	 * si usa quella per i confronti, altrimeni si utilizza il nome e cognome

	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Dipendente))
			return false;
		Dipendente other = (Dipendente) obj;
		//Se per entrambi gli oggetti confrontati è presente una matricola 
		//si usa quella per i confronti, altrimeni si utilizza il nome e cognome
		if (matricola != null && !matricola.isEmpty() && other.matricola != null && !other.matricola.isEmpty())
			return matricola.equals(other.matricola);
		
		if (cognomeNome == null) {
			if (other.cognomeNome != null)
				return false;
		} else if (!cognomeNome.toUpperCase().replace(" ",  "").equals(other.cognomeNome.toUpperCase().replace(" ", "")))
			return false;
		if (matricola == null) {
			if (other.matricola != null)
				return false;
		} else if (!matricola.equals(other.matricola))
			return false;
		return true;
	}


	@Override
	public int compareTo(final Dipendente other) {
		return other.cognomeNome.compareTo(cognomeNome);
	}

	@Override
	public String toString() {
		return String.format("%s (matricola=%s)", cognomeNome, matricola);
	}
}

