package cnr.sync.dto;

import models.Office;

import com.google.common.base.Function;

public class InstituteDTO{
    public int id;
    public String name;
    public String code;
    public String cds;
    public String dismissionDate;
    
	public enum toOffice implements Function<InstituteDTO,Office>{
		ISTANCE;

		@Override
			public Office apply(InstituteDTO instituteDTO){
				Office office = new Office();
				office.name = instituteDTO.name;
				office.contraction = instituteDTO.code;
				office.cds = instituteDTO.cds;
				return office;
		}
	}
}