package models.absences;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;

import models.Qualification;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType.JustifiedTypeName;
import models.base.BaseModel;
import models.enumerate.QualificationMapping;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

@Entity
@Table(name = "absence_types")
@Audited
public class AbsenceType extends BaseModel {

  private static final long serialVersionUID = 7157167508454574329L;

  @ManyToMany
  public List<Qualification> qualifications = Lists.newArrayList();

  @Getter
  @Required
  public String code;

  @Column(name = "certification_code")
  public String certificateCode;

  public String description;

  @Column(name = "valid_from")
  public LocalDate validFrom;

  @Column(name = "valid_to")
  public LocalDate validTo;

  @Column(name = "internal_use")
  public boolean internalUse = false;
  
  @Getter
  @Column(name = "considered_week_end")
  public boolean consideredWeekEnd = false;
  
  @Getter
  @Column(name = "time_for_mealticket")
  public boolean timeForMealTicket = false;
  
  @Getter
  @Column(name = "justified_time")
  public Integer justifiedTime;
  
  @Getter
  @ManyToMany
  @JoinTable(name = "absence_types_justified_types", 
      joinColumns = { @JoinColumn(name = "absence_types_id") }, 
      inverseJoinColumns = { @JoinColumn(name = "justified_types_id") })
  public Set<JustifiedType> justifiedTypesPermitted = Sets.newHashSet();
  
  @Getter
  @Column(name = "replacing_time")
  public Integer replacingTime;
  
  @Getter
  @ManyToOne
  @JoinColumn(name = "replacing_type_id")
  public JustifiedType replacingType;
  
  @OneToMany(mappedBy = "absenceType")
  @LazyCollection(LazyCollectionOption.EXTRA)
  public Set<Absence> absences = Sets.newHashSet();

  @ManyToMany(mappedBy = "takenCodes")
  public Set<TakableAbsenceBehaviour> takenGroup = Sets.newHashSet();

  @ManyToMany(mappedBy = "takableCodes")
  public Set<TakableAbsenceBehaviour> takableGroup = Sets.newHashSet();
  
  @ManyToMany(mappedBy = "complationCodes")
  public Set<ComplationAbsenceBehaviour> complationGroup = Sets.newHashSet();
  
  @ManyToMany(mappedBy = "replacingCodes")
  public Set<ComplationAbsenceBehaviour> replacingGroup = Sets.newHashSet();
  
  // Metodi
  
  /**
   * Descrizione limitata a 60 caratteri.
   * @return short description
   */
  @Transient
  public String getShortDescription() {
    if (description != null && description.length() > 60) {
      return description.substring(0, 60) + "...";
    }
    return description;
  }
  
  /**
   * Se il codice è scaduto.
   * @return esito
   */
  @Transient
  public boolean isExpired() {
    boolean newResult = false;
    LocalDate begin = this.validFrom;
    LocalDate end = this.validTo;
    if (begin == null) {
      begin = new LocalDate(2000, 1, 1); //molto prima di epas...
    }
    if (end == null) {
      end = new LocalDate(2100, 1, 1);   //molto dopo di epas...
    }
    if (DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(begin, end))) {
      newResult = false;
    } else {
      newResult = true;
    }
    boolean oldResult = false;
    if (validTo == null) {
      oldResult = false;
    } else {
      oldResult = LocalDate.now().isAfter(validTo);
    }
    
    if (oldResult != newResult) {
      throw new IllegalStateException();
    }
    
    return newResult;
    
  }

  @Override
  public String toString() {
    return Joiner.on(" - ").skipNulls().join(code, description);
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello all day.
   * @return esito
   */
  @Transient
  public boolean isAllDayPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.all_day)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello absence type minutes.
   * @return esito
   */  
  @Transient
  public boolean isAbsenceTypeMinutesPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.absence_type_minutes)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello specified minutes.
   * @return esito
   */
  @Transient
  public boolean isSpecifiedMinutesPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.specified_minutes)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello nothing.
   * @return esito
   */
  @Transient
  public boolean isNothingPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.nothing)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se il codice di assenza è utilizzabile per tutte le qualifiche del mapping.
   * @param mapping mapping
   * @return esito
   */
  @Transient
  public boolean isQualificationMapping(QualificationMapping mapping) {
    Set<Integer> set = ContiguousSet.create(mapping.getRange(), 
        DiscreteDomain.integers());
    Set<Integer> actuals = Sets.newHashSet();
    for (Qualification qualification : qualifications) {
      actuals.add(qualification.qualification);
    }
    for (Integer item : set) {
      if (!actuals.contains(item)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * I gruppi coinvolti dal tipo assenza.
   * 
   * @param onlyProgrammed non filtrare i soli programmati
   * @return entity set
   */
  public Set<GroupAbsenceType> involvedGroupAbsenceType(boolean onlyProgrammed) {

    //TODO: da fare la fetch perchè è usato in tabellone timbrature per ogni codice assenza.
    
    Set<GroupAbsenceType> groups = Sets.newHashSet();
    for (TakableAbsenceBehaviour behaviour : this.takableGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (TakableAbsenceBehaviour behaviour : this.takenGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : this.complationGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : this.replacingGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    if (!onlyProgrammed) {
      return groups;
    }
    Set<GroupAbsenceType> filteredGroup = Sets.newHashSet();
    for (GroupAbsenceType groupAbsenceType : groups) {
      if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.programmed)) {
        filteredGroup.add(groupAbsenceType);
      }
    }
    return filteredGroup;
  }
  
  /**
   * Se il codice è coinvolto solo in gruppi semplici.
   * @return esito
   */
  public boolean onlySimpleGroupInvolved() {
    for (GroupAbsenceType group : involvedGroupAbsenceType(false)) {
      if (group.pattern == GroupAbsenceTypePattern.simpleGrouping) {
        continue;
      }
      return false;
    }
    return true;
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   * @return absent se il completamento non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    for (DefaultAbsenceType defaultType : DefaultAbsenceType.values()) {
      if (defaultType.name().substring(2).equals(this.code)) {
        if (defaultType.certificationCode.equals(this.certificateCode)
            && defaultType.description.equals(this.description)
            && defaultType.internalUse == this.internalUse
            && defaultType.justifiedTime.equals(this.justifiedTime)
            && defaultType.consideredWeekEnd == this.consideredWeekEnd
            && defaultType.timeForMealTicket == this.timeForMealTicket
            && defaultType.replacingTime.equals(this.replacingTime)
            ) {
          //Tipi permessi
          if (defaultType.justifiedTypeNamesPermitted.size() 
              != this.justifiedTypesPermitted.size()) {
            return Optional.of(false); 
          }
          for (JustifiedType justifiedType : this.justifiedTypesPermitted) {
            if (!defaultType.justifiedTypeNamesPermitted.contains(justifiedType.name)) {
              return Optional.of(false);
            }
          }
          
          //replecing type nullable
          if (defaultType.replacingType == null) {
            if (this.replacingType != null) {
              return Optional.of(false);
            }
          } else {
            if (!defaultType.replacingType.equals(this.replacingType.name)) {
              return Optional.of(false);
            }
          }
          //valid from nullable
          if (defaultType.validFrom == null) {
            if (this.validFrom != null) {
              return Optional.of(false);
            }
          } else {
            if (!defaultType.validFrom.equals(this.validFrom)) {
              return Optional.of(false);
            }
          }
          //valid to nullable
          if (defaultType.validTo == null) {
            if (this.validTo != null) {
              return Optional.of(false);
            }
          } else {
            if (!defaultType.validTo.equals(this.validTo)) {
              return Optional.of(false);
            }
          }
          
          return Optional.of(true);
        } else {
          return Optional.of(false);
        }
      } 
    }
    return Optional.absent();
  }
  
  /**
  
    
    public Set<JustifiedTypeName> justifiedTypeNamesPermitted; //ultimo
    
    public JustifiedTypeName replacingType;
    
    public LocalDate validFrom;
    public LocalDate validTo;
   */
  
  /**
   * Tipi assenza di default.
   * 
   * @author alessandro
   *
   */
  public static enum DefaultAbsenceType {

    A_18M("18M", "Permesso assistenza parenti/affini disabili L. 104/92 in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_18("18", "Permesso assistenza parenti/affini disabili L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_18H1("18H1", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H2("18H2", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H3("18H3", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H4("18H4", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H5("18H5", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H6("18H6", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H7("18H7", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H8("18H8", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H9("18H9", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),

    A_19M("19M", "Permesso per dipendente disabile L. 104/92 in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_19("19", "Permesso per dipendente disabile L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_19H1("19H1", "Permesso per dipendente disabile L. 104/92 completamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H2("19H2", "Permesso per dipendente disabile L. 104/92 completamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H3("19H3", "Permesso per dipendente disabile L. 104/92 completamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H4("19H4", "Permesso per dipendente disabile L. 104/92 completamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H5("19H5", "Permesso per dipendente disabile L. 104/92 completamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H6("19H6", "Permesso per dipendente disabile L. 104/92 completamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H7("19H7", "Permesso per dipendente disabile L. 104/92 completamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H8("19H8", "Permesso per dipendente disabile L. 104/92 completamento 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H9("19H9", "Permesso per dipendente disabile L. 104/92 completamento 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),

    A_661M("661M", "Permesso orario per motivi personali in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_661H1("661H1", "Permesso orario per motivi personali completamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H2("661H2", "Permesso orario per motivi personali completamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H3("661H3", "Permesso orario per motivi personali completamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H4("661H4", "Permesso orario per motivi personali completamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H5("661H5", "Permesso orario per motivi personali completamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H6("661H6", "Permesso orario per motivi personali completamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H7("661H7", "Permesso orario per motivi personali completamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H8("661H8", "Permesso orario per motivi personali completamento 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H9("661H9", "Permesso orario per motivi personali completamento 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),

    A_89("89", "Permesso diritto allo studio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_89M("89M", "Permesso diritto allo studio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),

    A_09B("09B", "Permesso visita medica completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_09M("09M", "Permesso visita medica in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),

    A_92("92", "Missione",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_92H1("92H1", "Missione 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, true, true,
        0, null, null, null),
    A_92H2("92H2", "Missione 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, true, true,
        0, null, null, null),
    A_92H3("92H3", "Missione 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, true, true,
        0, null, null, null),
    A_92H4("92H4", "Missione 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, true, true,
        0, null, null, null),
    A_92H5("92H5", "Missione 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, true, true,
        0, null, null, null),
    A_92H6("92H6", "Missione 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, true, true,
        0, null, null, null),
    A_92H7("92H7", "Missione 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, true, true,
        0, null, null, null),


    A_31("31", "Ferie anno precedente",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_94("94", "festività soppresse (ex legge 937/77)",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_32("32", "Ferie anno corrente",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_37("37", "ferie anno precedente (dopo il 31/8)",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),

    A_91("91", "Riposo compensativo",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),


    A_PB("PB", "Permesso breve 36 ore anno",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes_limit), 0, false, false,
        0, null, null, null),

    A_105BP("105BP", "Lavoro Fuori Sede - Intera Giornata",
        false, ImmutableSet.of(JustifiedTypeName.assign_all_day), 0, false, false,
        0, null, null, null),
    
    A_23("23", "Astensione facoltativa post partum 100% primo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_23M("23M", "Astensione facoltativa post partum 100% primo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_23H7("23H7", "Astensione facoltativa post partum 100% primo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_23U("23U", "Astensione facoltativa post partum 100% primo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_25("25", "Astensione facoltativa post partum 30% primo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_25M("25M", "Astensione facoltativa post partum 30% primo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_25H7("25H7", "Astensione facoltativa post partum 30% primo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_25U("25U", "Astensione facoltativa post partum 30% primo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_24("24", "Astensione facoltativa post partum non retrib. primo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_24M("24M", "Astensione facoltativa post partum non retrib. primo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_24H7("24H7", "Astensione facoltativa post partum non retrib. primo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_24U("24U", "Astensione facoltativa post partum non retrib. primo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, false, false,
        0, null, null, null),

    A_232("232", "Astensione facoltativa post partum 100% secondo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_232M("232M", "Astensione facoltativa post partum 100% secondo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_232H7("232H7", "Astensione facoltativa post partum 100% secondo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_232U("232U", "Astensione facoltativa post partum 100% secondo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),
    
    A_252M("252M", "Astensione facoltativa post partum 30% secondo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_252("252", "Astensione facoltativa post partum 30% secondo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_252H7("252H7", "Astensione facoltativa post partum 30% secondo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_252U("252U", "Astensione facoltativa post partum 30% secondo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_242("242", "Astensione facoltativa post partum non retrib. secondo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_242M("242M", "Astensione facoltativa post partum non retrib. secondo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_242H7("242H7", "Astensione facoltativa post partum non retrib. secondo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_242U("242U", "Astensione facoltativa post partum non retrib. secondo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, false, false,
        0, null, null, null),

    A_233("233", "Astensione facoltativa post partum 100% terzo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_233M("233M", "Astensione facoltativa post partum 100% terzo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_233H7("233H7", "Astensione facoltativa post partum 100% terzo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_233U("233U", "Astensione facoltativa post partum 100% terzo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_253("253", "Astensione facoltativa post partum 30% terzo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_253M("253M", "Astensione facoltativa post partum 30% terzo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_253H7("253H7", "Astensione facoltativa post partum 30% terzo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_253U("253U", "Astensione facoltativa post partum 30% terzo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),


    A_243("243", "Astensione facoltativa post partum non retrib. terzo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),

    A_243M("243M", "Astensione facoltativa post partum non retrib. terzo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_243H7("243H7", "Astensione facoltativa post partum non retrib. terzo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_243U("243U", "Astensione facoltativa post partum non retrib. terzo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, false, false,
        0, null, null, null),
        
    //TODO: i codici di malattia vanno rivisitati.
    
    A_111("111", "malattia",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11S("11S", "malattia superiore a 15 gg lav.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_115("115", "malattia superiore a 12 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_116("116", "malattia superiore a 18 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_117("117", "terapia invalidante grave patologia",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_118("118", "malattia con responsabilita' di terzi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_119("119", "malattia superiore a nove mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11R("11R", "malattia con ricovero",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11C("11C", "malattia post-ricovero",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    
    A_12("12", "malattia primo figlio/a <= 3 anni retribuita 100%",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_13("13", "malattia primo figlio/a > 3 anni senza retr.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_14("14", "malattia primo figlio non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_122("122", "malattia secondo figlio/a <= 3 anni retribuita 100%",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    
    A_132("132", "malattia secondo figlio/a > 3 anni senza retr.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_142("142", "malattia secondo figlio non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, new LocalDate(1990, 1, 1), new LocalDate(2008, 12, 31)),
    
    A_123("123", "malattia terzo figlio/a <= 3 anni retribuita 100%",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_133("133", "malattia terzo figlio/a > 3 anni senza retr.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_143("143", "malattia terzo figlio non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, new LocalDate(1990, 1, 1), new LocalDate(2008, 12, 31));
    

    public String certificationCode;
    public String description;
    public boolean internalUse;
    
    public Set<JustifiedTypeName> justifiedTypeNamesPermitted;
    public Integer justifiedTime;                              
    
    public boolean consideredWeekEnd;
   
    public boolean timeForMealTicket;
    
    public Integer replacingTime;
    public JustifiedTypeName replacingType;                     //nullable
    
    public LocalDate validFrom;                                 //nullable
    public LocalDate validTo;                                   //nullable

    private DefaultAbsenceType(String certificationCode, String description, boolean internalUse, 
        Set<JustifiedTypeName> justifiedTypeNamesPermitted, Integer justifiedTime, 
        boolean consideredWeekEnd, boolean timeForMealTicket, 
        Integer replacingTime, JustifiedTypeName replacingType,
        LocalDate validFrom, LocalDate validTo) {
      this.certificationCode = certificationCode;
      this.description = description;
      this.internalUse = internalUse;
      this.justifiedTypeNamesPermitted = justifiedTypeNamesPermitted;
      this.justifiedTime = justifiedTime;
      this.consideredWeekEnd = consideredWeekEnd;
      this.timeForMealTicket = timeForMealTicket;
      this.replacingTime = replacingTime;
      this.replacingType = replacingType;
      this.validFrom = validFrom;
      this.validTo = validTo;

    }
    
    /**
     * Ricerca i codici assenza modellati e non presenti fra quelle passate in arg (db).
     * @return list
     */
    public static List<DefaultAbsenceType> missing(List<AbsenceType> allAbsenceTypes) {
      List<DefaultAbsenceType> missing = Lists.newArrayList();
      for (DefaultAbsenceType defaultTypes : DefaultAbsenceType.values()) {
        boolean found = false;
        for (AbsenceType type : allAbsenceTypes) {
          if (defaultTypes.name().substring(2).equals(type.code)) {
            found = true;
            break;
          }
        }
        if (!found) {
          missing.add(defaultTypes);
        }
      }
      return missing;
    }
    
    /**
     * L'enumerato corrispettivo del absenceType (se esiste...) 
     * @return optional dell'enumerato
     */
    public static Optional<DefaultAbsenceType> byCode(AbsenceType absenceType) {
      for (DefaultAbsenceType defaultAbsenceType : DefaultAbsenceType.values()) {
        if (absenceType.code.equals(defaultAbsenceType.name().substring(2))) {
          return Optional.of(defaultAbsenceType);
        }
      }
      return Optional.absent();
    }

  }
  
}
