/**
 *
 */
package models.enumerate;

/**
 * @author cristian
 *
 */
public enum StampTypeValues implements Identified {


	/*
 id |          code          |       description        | identifier
----+------------------------+--------------------------+------------
  1 | motiviDiServizio       | Motivi di servizio       | s
  2 | visitaMedica           | Visita Medica            | vm
  3 | permessoSindacale      | Permesso sindacale       | ps
  4 | incaricoDiInsegnamento | Incarico di insegnamento | is
  5 | dirittoAlloStudio      | Diritto allo studio      | das
  6 | motiviPersonali        | Motivi personali         | mp
  7 | reperibilita           | Reperibilità             | r
  8 | intramoenia            | Intramoenia              | i
  9 | guardiaMedica          | Guardia Medica           | gm
	 */

	MOTIVI_DI_SERVIZIO(1l),
	VISITA_MEDICA(2l),
	PERMESSO_SINDACALE(3l),
	INCARICO_DI_INSEGNAMENTO(4l),
	DIRITTO_ALLO_STUDIO(5l),
	MOTIVI_PERSONALI(6l),
	REPERIBILITA(7l),
	INTRAMOENIA(8l),
	GUARDIA_MEDICA(9l);

	private long id;

	StampTypeValues(Long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

}