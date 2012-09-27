/**
 * 
 */
package models.enumerate;

/**
 * @author cristian
 *
 */
public enum StampTypeValues {
	
	MOTIVI_DI_SERVIZIO(1l),
	VISITA_MEDICA(2l),
	PERMESSO_SINDACALE(3l),
	INCARICO_DI_INSEGNAMENTO(4l),
	DIRITTO_ALLO_STUDIO(5l),
	MOTIVI_PERSONALI(6l),
	REPERIBILITA(7l),
	INTRAMOENIA(8l),
	GUARDIA_MEDICA(9l);
	
	private Long id;
	
	StampTypeValues(Long id) {
		this.id = id;
	};
	
	public Long getId() {
		return id;
	}
}