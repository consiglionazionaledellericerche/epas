package cnr.sync.dto;

import com.google.common.base.MoreObjects;

public class OfficeDTO {
	public int id;
	public String name;
	public String code;
	public String codeId;
	public InstituteDTO institute;
    public String dismissionDate;
    
//    public enum toOffice implements Function<OfficeDTO, Office>{
//		ISTANCE;
//
//		@Override
//			public Office apply(OfficeDTO officeDTO){
//				Office office = new Office();
//
//				office.name = officeDTO.name;
//				office.codeId = officeDTO.codeId;
//				office.code = officeDTO.code;
//
//				return office;
//		}
//	}
		
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id",id).add("name",name).add("code", code).toString();
	}
}
