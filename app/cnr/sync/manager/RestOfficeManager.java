package cnr.sync.manager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import play.Logger;
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
//			institute.save();
			Logger.info("Importato Istituto %s", institute.name);
		}
		
		for(SeatDTO seatDTO : seatsDTO){
			Office seat = new Office();
			seat.name = seatDTO.institute.code != null ? 
					seatDTO.institute.code  +" - "+seatDTO.name
					: seatDTO.name;
			seat.codeId = seatDTO.codeId;
			seat.code = seatDTO.code;
			seat.office = Office.find("byCds", seatDTO.institute.cds).first();
			Logger.info("Importata Sede %s", seat.name);
//			seat.save();			
		}
		
	}

}
