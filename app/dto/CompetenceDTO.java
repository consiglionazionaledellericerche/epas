package dto;

import models.Competence;

import com.google.common.base.Function;

public class CompetenceDTO {
	public int year;
	public int month;
	public String code;
	public int valueApproved;
	
	public enum fromCompetence implements Function<Competence,CompetenceDTO>{
		ISTANCE;

		@Override
			public CompetenceDTO apply(Competence competence){
				CompetenceDTO competenceDTO = new CompetenceDTO();
				competenceDTO.year = competence.year;
				competenceDTO.month = competence.month;
				competenceDTO.code = competence.competenceCode.code;
				competenceDTO.valueApproved = competence.valueApproved;
				return competenceDTO;
		}
	}
}
