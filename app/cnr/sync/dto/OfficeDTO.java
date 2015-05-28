package cnr.sync.dto;

import com.google.common.base.MoreObjects;

public class OfficeDTO {
	public int id;
	public String name;
	public String code;
	public String codeId;
	public Institute institute;
    public String dismissionDate;
	
	protected class Institute{
	    public int id;
	    public String name;
	    public String code;
	    public String cds;
	    public String dismissionDate;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id",id).add("name",name).add("code", code).toString();
	}
}
