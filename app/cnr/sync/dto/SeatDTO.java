package cnr.sync.dto;

import models.Office;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;

public class SeatDTO {
	public int id;
	public String name;
	public String code;
	public String codeId;
	public InstituteDTO institute;
    public String dismissionDate;
    
    public enum toOffice implements Function<SeatDTO,Office>{
		ISTANCE;

		@Override
			public Office apply(SeatDTO seatDTO){
				Office office = new Office();
				office.name = seatDTO.institute.code != null ? 
						seatDTO.institute.code  +" - "+seatDTO.name
						: seatDTO.name;
				office.codeId = seatDTO.codeId;
				office.code = seatDTO.code;
				return office;
		}
	}
		
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id",id).add("name",name).add("code", code).toString();
	}
}
