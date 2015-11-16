package cnr.sync.manager;

import cnr.sync.dto.InstituteDTO;
import cnr.sync.dto.OfficeDTO;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import dao.OfficeDao;
import manager.OfficeManager;
import models.Institute;
import models.Office;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Validation;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

public class RestOfficeManager {

	@Inject
	private OfficeDao officeDao;
	@Inject
	private OfficeManager officeManager;

	private final static Logger log = LoggerFactory.getLogger(RestOfficeManager.class);

	public int saveImportedSeats(Collection<OfficeDTO> officeDTOList){

		Preconditions.checkNotNull(officeDTOList);

		Set<InstituteDTO> institutesDTO = Sets.newHashSet();

//		Estrazione senza doppioni di tutti gli istituti dalla lista degli uffici
		for(OfficeDTO office : officeDTOList){
			institutesDTO.add(office.institute);
		}

//      Conversione dei DTO in istituti
		Set<Institute> institutes = FluentIterable.from(institutesDTO)
				.transform(InstituteDTO.toInstitute.ISTANCE).toSet();

		for(Institute institute : institutes){
			institute.validateAndCreate();
			if(!Validation.hasErrors()){
//				Caso di un nuovo istituto non ancora presente nel db
				log.info("Importato nuovo Istituto {}", institute.name);
			}
			else{
				Optional<Institute> existentInstitute = officeDao.byCds(institute.cds);
				if(existentInstitute.isPresent()){
//					Aggiornamento dati dell'istituto se già presente
					existentInstitute.get().name = institute.name;
					existentInstitute.get().code = institute.code;
					existentInstitute.get().save();
					log.info("Sincronizzato istituto esistente durante l'import - {}", institute.name);
				}
				else{
					log.warn("Trovato istituto duplicato durante l'import! "
							+ "valorizzare correttamente il campo cds per "
							+ "effettuarne la sincronizzazione: {}-{}", institute.name,institute.cds);
				}
			}
		}

		int syncedOffices = 0;

		for(OfficeDTO officeDTO : officeDTOList){

			Office office = new Office();
			office.name = officeDTO.name;
			office.codeId = officeDTO.codeId;
			office.code = officeDTO.code;

			Optional<Institute> institute=officeDao.byCds(officeDTO.institute.cds);

			if(!institute.isPresent()){
				log.warn("Impossibile trovare l'istituto associato alla sede {} importata da Perseo." +
						" Sede non inserita.",office);
				continue;
			}

			office.institute = institute.get();

			office.validateAndSave();
			if(!Validation.hasErrors()){
				syncedOffices++;
				officeManager.generateConfAndPermission(office);
				log.info("Importata Sede {}", office.name);
			}
			else{
				Optional<Office> existentSeat = officeDao.byCode(office.code);
				if(existentSeat.isPresent()){
					existentSeat.get().name = office.name;
					existentSeat.get().codeId = office.codeId;
					existentSeat.get().save();
					syncedOffices++;
					log.info("Sincronizzata sede esistente durante l'import - {}", office.name);
				}
				else{
					log.warn("Trovata Sede duplicata durante l'import! "
							+ "valorizzare correttamente il campo Codice Sede per "
							+ "effettuarne la sincronizzazione: {}-{}", office.name,office.codeId);
				}
			}
		}
		return syncedOffices;
	}

}