package cnr.sync.manager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import models.Office;
import cnr.sync.dto.InstituteDTO;
import cnr.sync.dto.SeatDTO;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import dao.OfficeDao;

public class RestOfficeManager {
	
	@Inject
	private OfficeDao officeDao;

	public void saveImportedSeats(Collection<SeatDTO> seatsDTO){

		Preconditions.checkNotNull(seatsDTO);
		
		Set<InstituteDTO> institutesDTO = Sets.newHashSet();

		for(SeatDTO seat : seatsDTO){
			institutesDTO.add(seat.institute);
		}
		
		List<Office> areas = officeDao.getAreas();
		Office mainArea;
		
		if(areas.isEmpty()){
			mainArea = new Office();
		}
		else{
			mainArea = areas.iterator().next();
		}
		
		List<Office> institutes = FluentIterable.from(institutesDTO)
				.transform(InstituteDTO.toOffice.ISTANCE).toList();
		
		for(Office institute : institutes){
			institute.office = mainArea;
			institute.save();
		}
		
		for(SeatDTO seatDTO : seatsDTO){
			Office seat = new Office();
			seat.name = seatDTO.institute.code +" - "+seatDTO.name;
			seat.codeId = Integer.parseInt(seatDTO.codeId);
			seat.code = seatDTO.code;
			seat.office = Office.find("byCds", seatDTO.institute.cds).first();
			seat.save();			
		}
		
	}

}
