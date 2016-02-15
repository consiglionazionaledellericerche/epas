package cnr.sync.manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import cnr.sync.dto.InstituteDto;
import cnr.sync.dto.OfficeDto;

import dao.OfficeDao;

import lombok.extern.slf4j.Slf4j;

import manager.OfficeManager;

import models.Institute;
import models.Office;

import play.data.validation.Validation;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;

@Slf4j
public class RestOfficeManager {

  @Inject
  private OfficeDao officeDao;
  @Inject
  private OfficeManager officeManager;

  public int saveImportedSeats(Collection<OfficeDto> officeDtoList) {

    Preconditions.checkNotNull(officeDtoList);

    Set<InstituteDto> institutesDto = Sets.newHashSet();

    //  Estrazione senza doppioni di tutti gli istituti dalla lista degli uffici
    for (OfficeDto office : officeDtoList) {
      institutesDto.add(office.institute);
    }

    //  Conversione dei DTO in istituti
    Set<Institute> institutes = FluentIterable.from(institutesDto)
            .transform(InstituteDto.ToInstitute.ISTANCE).toSet();

    for (Institute institute : institutes) {
      institute.validateAndCreate();
      if (!Validation.hasErrors()) {
        //  Caso di un nuovo istituto non ancora presente nel db
        log.info("Importato nuovo Istituto {}", institute.name);
      } else {
        Optional<Institute> existentInstitute = officeDao.byCds(institute.cds);
        if (existentInstitute.isPresent()) {
          //  Aggiornamento dati dell'istituto se già presente
          existentInstitute.get().name = institute.name;
          existentInstitute.get().code = institute.code;
          existentInstitute.get().save();
          log.info("Sincronizzato istituto esistente durante l'import - {}", institute.name);
        } else {
          log.warn("Trovato istituto duplicato durante l'import! "
                  + "valorizzare correttamente il campo cds per "
                  + "effettuarne la sincronizzazione: {}-{}", institute.name, institute.cds);
        }
      }
    }

    int syncedOffices = 0;

    for (OfficeDto officeDto : officeDtoList) {

      Office office = new Office();
      officeDto.copyInto(office);

      Optional<Institute> institute = officeDao.byCds(officeDto.institute.cds);

      if (!institute.isPresent()) {
        log.warn("Impossibile trovare l'istituto associato alla sede {} importata da Perseo."
                + " Sede non inserita.", office);
        continue;
      }

      office.institute = institute.get();

      office.validateAndSave();
      if (!Validation.hasErrors()) {
        syncedOffices++;
        officeManager.setSystemUserPermission(office);
        log.info("Importata Sede {}", office.name);
      } else {
        Optional<Office> existentSeat = officeDao.byCode(office.code);
        if (existentSeat.isPresent()) {

          officeDto.copyInto(existentSeat.get());
          existentSeat.get().save();
          syncedOffices++;
          log.info("Sincronizzata sede esistente durante l'import - {}", office.name);
        } else {
          log.warn("Trovata Sede duplicata durante l'import! "
                  + "valorizzare correttamente il campo Codice Sede per "
                  + "effettuarne la sincronizzazione: {}-{}", office.name, office.codeId);
        }
      }
    }
    return syncedOffices;
  }
}
