package cnr.sync.dto;

import models.Office;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;

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

	@Override
	public boolean equals(Object o){
		if(o != null && o instanceof InstituteDTO){
			final InstituteDTO other = (InstituteDTO) o;
			return cds.equals(other.cds) | code.equals(other.code);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cds == null) ? 0 : cds.toUpperCase().replace(" ",  "").hashCode());
		result = prime * result
				+ ((code == null || code.equals("0")) ? 0 : code.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id",id).add("name",name).add("code", code).add("cds", cds)
				.toString();
	}
}