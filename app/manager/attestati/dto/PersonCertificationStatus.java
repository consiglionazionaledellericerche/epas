package manager.attestati.dto;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import models.Certification;
import models.Person;

import org.assertj.core.util.Maps;

import java.util.List;
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
  
  // ##############################################################################################
  // Da inviare
  public Map<String, Certification> notSendedYet = Maps.newHashMap();
  
  // ##############################################################################################
  // Lo stato in epas
  public Map<String, Certification> epasCorrect  = Maps.newHashMap();       
  public Map<String, Certification> epasProblemsTryAgain = Maps.newHashMap();
  public Map<String, Certification> epasOutdateNotErasable = Maps.newHashMap();
                                                        
  
  //##############################################################################################
  // Lo stato in attestati
  //public Map<String, Certification> attestatiCorrect;
  public Map<String, Certification> attestatiIncorrectNotErasable = Maps.newHashMap();
  
  
  /**
   * 
   * @param person
   * @param year
   * @param month
   * @param certifications lo stato corretto attuale secondo epas
   * @param epasCertifications
   * @param attestatiCertifications
   */
  public PersonCertificationStatus(Person person, int year, int month, 
      List<Certification> certifications,
      Map<String, Certification> epasCertifications, 
      Map<String, Certification> attestatiCertifications) {
    
    this.person = person;
    this.year = year;
    this.month = month;
    
    Map<String, Certification> certificationsMap = Maps.newHashMap();
    
    // Tutte le chiavi (e costruzione mappa certifications)
    Set<String> allCertificationKeys = Sets.newHashSet();
    for (Certification certification : certifications) {
      certificationsMap.put(certification.aMapKey(), certification);
      allCertificationKeys.add(certification.aMapKey());
    }
    allCertificationKeys.addAll(epasCertifications.keySet());
    allCertificationKeys.addAll(attestatiCertifications.keySet());
    
    // Clean situation e smistamento gruppi.
    
    // Per ogni chiave smisto le certificazioni
    for (String key : allCertificationKeys) {
      
      Certification certification = certificationsMap.get(key);
      Certification epasCertification = epasCertifications.get(key);
      Certification attestatiCertification = attestatiCertifications.get(key);
      
      // x x x | Attuale, inviata da epas ed in attestati
      if (certification != null && epasCertification != null && attestatiCertification != null) {
        
        if (epasCertification.containProblems()) {
          epasCertification.problems = null;
          epasCertification.save();
        }
        epasCorrect.put(key, epasCertification);
        //attestatiCorrect.put(key, attestatiCertification);
      }
      
      // x x o | Attuale, inviata da epas ma non in attestati 
      else if (certification != null && epasCertification != null && attestatiCertification == null) {
        
        if (epasCertification.containProblems()) {
          epasProblemsTryAgain.put(key, epasCertification);
        } else {
          epasCertification.delete();
          notSendedYet.put(key, certification);
        }
        continue;
      }
      
      // x o x | Attuale, non inviata da epas ma in attestati
      else if (certification != null && epasCertification == null && attestatiCertification != null) {
        certification.save();
        epasCorrect.put(key, certification);
      }
      
      // x o o | Attuale, non inviata da epas e non in attestati
      else if (certification != null && epasCertification == null && attestatiCertification == null) {
        notSendedYet.put(key, certification);
      }
      
      // o x x | Obsoleta, inviata da epas e in attestati
      else if (certification == null && epasCertification != null && attestatiCertification != null) {
        if (!removeAttestati(attestatiCertification)) {
          epasOutdateNotErasable.put(key, attestatiCertification);
        } else {
          epasCertification.delete();          
        }
      }
      
      // o x o |  Obsoleta, inviata da epas e non in attestati
      else if (certification == null && epasCertification != null && attestatiCertification == null) {
        epasCertification.delete();
      }
      
      //  o o x | Obsoleta, non inviata da epas ma in attestati
      else if (certification == null && epasCertification == null && attestatiCertification != null) {
        if (!removeAttestati(attestatiCertification)) {
          attestatiIncorrectNotErasable.put(key, attestatiCertification);
        }
      }
    }
  }

  public static boolean removeAttestati(Certification configuration) {
    return false;
  }
}
