package cnr.sync.manager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.OfficeManager;
import models.Office;
import play.Logger;
import cnr.sync.dto.InstituteDTO;
import cnr.sync.dto.SeatDTO;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import dao.OfficeDao;

public class RestOfficeManager {
	
	@Inject
	private OfficeDao officeDao;
	@Inject
	private OfficeManager officeManager;
	
	public void saveImportedSeats(Collection<SeatDTO> seatsDTO){

		Preconditions.checkNotNull(seatsDTO);
		
		Set<InstituteDTO> institutesDTO = Sets.newHashSet();

		for(SeatDTO seat : seatsDTO){
			institutesDTO.add(seat.institute);
		}
		Logger.info("Istituti da importare %s", institutesDTO);
		
		List<Office> areas = officeDao.getAreas();
		Office mainArea;
		
		if(areas.isEmpty()){
			mainArea = new Office();
		}
		else{
			mainArea = areas.iterator().next();
		}
		
		Set<Office> institutes = FluentIterable.from(institutesDTO)
				.transform(InstituteDTO.toOffice.ISTANCE).toSet();
		
		for(Office institute : institutes){
			institute.office = mainArea;
			if(officeManager.saveOffice(institute)){
				Logger.info("Importato Istituto %s", institute.name);
			}
			else{
				Optional<Office> existentOffice = officeDao.byCds(institute.cds);
				if(existentOffice.isPresent()){
					existentOffice.get().copy(institute);
					officeManager.saveOffice(existentOffice.get());
				}
				Logger.warn("Trovato istituto duplicato durante l'import - %s", institute.name);
			}
		}
		
		for(SeatDTO seatDTO : seatsDTO){
			Office seat = new Office();
			seat.name = seatDTO.name;
			seat.codeId = seatDTO.codeId;
			seat.code = seatDTO.code;
			seat.office = officeDao.byCds(seatDTO.institute.cds).orNull();
			if(officeManager.saveOffice(seat)){
				Logger.info("Importata Sede %s", seat.name);
			}
			else{
				Optional<Office> existentSeat = officeDao.byCodeId(seat.codeId);
				if(existentSeat.isPresent()){
					existentSeat.get().copy(seat);
					officeManager.saveOffice(existentSeat.get());
				}
				Logger.warn("Trovata sede duplicata durante l'import - %s", seat.name);
			}
		}
		
	}

}
