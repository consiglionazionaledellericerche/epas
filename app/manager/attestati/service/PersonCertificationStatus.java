package manager.attestati.service;

import com.google.common.collect.Sets;

import models.Certification;
import models.Person;
import models.enumerate.CertificationType;

import org.assertj.core.util.Maps;

import java.util.Map;
import java.util.Set;

/**
 * La situazione inerente un mese di una persona.
 * Permette di definire lo stato e i warning se presenti.
 * 
 * @author alessandro
 *
 */
public class PersonCertificationStatus {
  
  public Person person;
  public int year;
  public int month;
  
  public boolean staticView = true;
  
  public boolean okProcessable;             //Coerenza epas e attestati
  public boolean incompleteProcessable;     //Incoerenza epas e attestati
  
  public boolean okNotProcessable;          //Coerenza interna epas non processabile
  public boolean incompleteNotProcessable;  //Incoerenza interna epas non processabile 
  
  // Strutture dati statiche
  protected Map<String, Certification> actualCertifications;
  protected Map<String, Certification> epasCertifications;
  protected Map<String, Certification> attestatiCertifications;

  // Situazione statica
  public Map<String, Certification> correctCertifications = Maps.newHashMap();
  public Map<String, Certification> toDeleteCertifications = Maps.newHashMap();
  public Map<String, Certification> problemCertifications = Maps.newHashMap();
  public Map<String, Certification> toSendCertifications = Maps.newHashMap();
  public Certification attestatiMealToOverwrite;
  
  public PersonCertificationStatus computeStaticStatus() {
    
    this.staticView = true;
    
    if (this.incompleteProcessable) {
      
      //Comparazione attuali e attestati
      Set<String> allKey = Sets.newHashSet(); 
      allKey.addAll(actualCertifications.keySet());
      allKey.addAll(attestatiCertifications.keySet());
      
      for (String key : allKey) {
        Certification actualCertification = actualCertifications.get(key);
        Certification epasCertification = epasCertifications.get(key);
        Certification attestatiCertification = attestatiCertifications.get(key);
        
        //Corretto
        if (actualCertification != null && attestatiCertification != null) {
          correctCertifications.put(key, attestatiCertification);
          continue;
        }
        
        //Da cancellare
        if (actualCertification == null) {
          
          //Patch mealTicket record
          if (attestatiCertification.certificationType.equals(CertificationType.MEAL)) {
            attestatiMealToOverwrite = attestatiCertification;
          } else {
            toDeleteCertifications.put(key, attestatiCertification);
          }
          continue;
        }
        
        //Da inserire (o riprovare a inserire)
        if (attestatiCertification == null) {
          if (epasCertification != null && epasCertification.containProblems()) {
            problemCertifications.put(key, epasCertification);
            continue;
          }
          toSendCertifications.put(key, actualCertification);
          continue;
        }
      }
    }
    
    return this;
  }
  
  public PersonCertificationStatus computeProcessStatus() {
    
    if (incompleteProcessable) {
      if (toDeleteCertifications.values().isEmpty() && problemCertifications.values().isEmpty() &&
          toSendCertifications.values().isEmpty() ) {
        this.incompleteNotProcessable = false;
        this.okNotProcessable = true;
      }
    }
    return this;
    
  }

}
