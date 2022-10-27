/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers;

import com.google.common.base.Optional;
import dao.ContractualReferenceDao;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import javax.inject.Inject;
import models.contractual.ContractualReference;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.Blob;
import play.libs.MimeTypes;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei riferimenti contrattuali.
 *
 * @author Cristian Lucchesi
 */
@With({Resecure.class})
public class ContractualReferences extends Controller {

  @Inject
  static ContractualReferenceDao contractualReferenceDao;
  
  /**
   * Mostra tutti i riferimenti contrattuali.
   */
  public static void list() {
    List<ContractualReference> contractualReferences = 
        contractualReferenceDao.all(Optional.of(true));
    render(contractualReferences);
  }
  
  /**
   * Nuovo riferimento contrattuale.
   */
  public static void blank() {
    ContractualReference contractualReference = new ContractualReference();
    render("@edit", contractualReference);    
  }

  /**
   * Modifica del riferimento contrattuale.
   *
   * @param contractualReferenceId id
   */
  public static void edit(Long contractualReferenceId) {
    ContractualReference contractualReference = 
        ContractualReference.findById(contractualReferenceId);
    notFoundIfNull(contractualReference);
    render(contractualReference);
  }
  
  /**
   * Salva il riferimento contrattuale.
   *
   * @param contractualReference riferimento contrattuale
   * @throws FileNotFoundException sollevata in caso di problemi con il file caricato
   */
  public static void save(@Valid ContractualReference contractualReference, File file) 
      throws FileNotFoundException {

    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@editContractualClause", contractualReference);
    }

    contractualReference.setFilename(file.getName());
    Blob blob = new Blob();
    blob.set(new FileInputStream(file), MimeTypes.getContentType(file.getName()));
    contractualReference.setFile(blob);
    
    contractualReference.save();
    flash.success("Operazione eseguita.");
    edit(contractualReference.id);
  }

  /**
   * Rimuove il riferimento contrattuale.
   *
   * @param contractualReferenceId tab
   */
  public static void delete(Long contractualReferenceId) {
    ContractualReference contractualReference = 
        ContractualReference.findById(contractualReferenceId);
    notFoundIfNull(contractualReference);
    contractualReference.delete();
    flash.success("Operazione effettuata.");
    list();
  }
  
  /**
   * Cancella il file associato ad un Riferimento normativo/contrattuale.
   *
   * @param contractualReferenceId id del riferimento di cui cancellare
   *     l'allegato.
   */
  public static void deleteFile(Long contractualReferenceId) {
    ContractualReference contractualReference = 
        ContractualReference.findById(contractualReferenceId);
    notFoundIfNull(contractualReference);
    contractualReference.getFile().getFile().delete();
    contractualReference.setFilename(null);
    contractualReference.save();
    flash.success("Operazione effettuata.");
    edit(contractualReferenceId);    
  }
  
  /**
   * Restituisce il file associato al riferimento contrattuale.
   *
   * @param contractualReferenceId id del riferimento contrattuale.
   */
  public static void getFile(Long contractualReferenceId) {
    final ContractualReference contractualReference = 
        ContractualReference.findById(contractualReferenceId);
    notFoundIfNull(contractualReference);
    notFoundIfNull(contractualReference.getFile());
    renderBinary(contractualReference.getFile().get(), contractualReference.getFilename(), false);
  }

}