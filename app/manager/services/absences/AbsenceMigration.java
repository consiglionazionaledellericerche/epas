package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;

import java.util.List;
import java.util.Set;

@Slf4j
public class AbsenceMigration {
  
  private final AbsenceComponentDao absenceComponentDao;

  public enum DefaultCategoryType {
    
    GENERAL("Assenze generali cnr", 1),
    PERMISSION("Permessi vari", 2),
    POST_PARTUM("Congedi parentali", 3),
    LAW_104_92("Disabilità legge 104/92", 5), 
    MALATTIA("Malattia Dipendente", 6),
    MALATTIA_FIGLIO_1("Malattia primo figlio", 7),
    MALATTIA_FIGLIO_2("Malattia secondo figlio", 8),
    MALATTIA_FIGLIO_3("Malattia terzo figlio", 9),
    PUBLIC_FUNCTION("Pubblica Funzione", 10),
    OTHER_CODES("Altri Codici", 11),
    AUTOMATIC_CODES("Codici Automatici", 12),;
    
    public String name;
    public int priority;
    
    private DefaultCategoryType(String name, int priority) {
      this.name = name;
      this.priority = priority;
    }
    
  }
  
  public enum DefaultComplation {
    C_18, C_19, C_661, 
    C_23, C_25, C_232, C_252, C_233, C_253, 
    C_89, C_09;
  }
  
  public enum DefaultTakable {
    T_18, T_19, T_661, 
    T_23, T_25, T_232, T_252, T_233, T_253, 
    T_89, T_09, T_FERIE_CNR, T_RIPOSI_CNR, T_MISSIONE, T_95, T_ALTRI,
    T_MALATTIA,
    T_MALATTIA_FIGLIO_1_12,
    T_MALATTIA_FIGLIO_1_13,
    T_MALATTIA_FIGLIO_1_14,
    T_MALATTIA_FIGLIO_2_12,
    T_MALATTIA_FIGLIO_2_13,
    T_MALATTIA_FIGLIO_2_14,
    T_MALATTIA_FIGLIO_3_12,
    T_MALATTIA_FIGLIO_3_13,
    T_MALATTIA_FIGLIO_3_14,
    T_PB
    ;
  }
  
  public enum DefaultGroup {
    G_18, G_19, G_661, 
    G_23, G_25, G_232, G_252, G_233, G_253,
    
    G_89, G_09, MISSIONE, ALTRI, FERIE_CNR, RIPOSI_CNR, G_95,
    MALATTIA, 
    MALATTIA_FIGLIO_1_12,
    MALATTIA_FIGLIO_1_13,
    MALATTIA_FIGLIO_1_14,
    MALATTIA_FIGLIO_2_12,
    MALATTIA_FIGLIO_2_13,
    MALATTIA_FIGLIO_2_14,
    MALATTIA_FIGLIO_3_12,
    MALATTIA_FIGLIO_3_13,
    MALATTIA_FIGLIO_3_14,
    PB
    ;
  }
  
  @Inject
  public AbsenceMigration(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }
  
  @SuppressWarnings("deprecation")
  private void migrateAllAbsences() {
    
    JustifiedType nothing = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing);
    JustifiedType specifiedMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    JustifiedType absenceTypeMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.absence_type_minutes);
    JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    JustifiedType halfDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.half_day);
    JustifiedType assignAllDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.assign_all_day); 
    
    int absencesToUpdate = 50;
    
    List<GroupAbsenceType> groupAbsenceTypes = GroupAbsenceType.findAll();
    for (GroupAbsenceType groupAbsenceType : groupAbsenceTypes) {
      groupAbsenceType = GroupAbsenceType.findById(groupAbsenceType.id);
      log.info(groupAbsenceType.name);
      Set<AbsenceType> absenceTypes = Sets.newHashSet();
      //if (groupAbsenceType.name.equals(DefaultGroup.ALTRI.name())) {
        if (groupAbsenceType.takableAbsenceBehaviour != null) {
          absenceTypes.addAll(groupAbsenceType.takableAbsenceBehaviour.takenCodes);
          absenceTypes.addAll(groupAbsenceType.takableAbsenceBehaviour.takableCodes);
        }
        if (groupAbsenceType.complationAbsenceBehaviour != null) {
          absenceTypes.addAll(groupAbsenceType.complationAbsenceBehaviour.complationCodes);
          absenceTypes.addAll(groupAbsenceType.complationAbsenceBehaviour.replacingCodes);
        }
      //}
      
      
      for (AbsenceType absenceType : absenceTypes) {
        absenceType = AbsenceType.findById(absenceType.id);
        for (Absence absence : absenceType.absences) {
          absence = Absence.findById(absence.id);
          log.info("{} {}", absence.absenceType.code, absence.personDay.person.fullName());
          
          migrateAbsence(absence, 
              nothing, specifiedMinutes, absenceTypeMinutes, allDay, halfDay, assignAllDay);
          
          absencesToUpdate--;
          if (absencesToUpdate == 0) {
            absencesToUpdate = 100;
            JPAPlugin.closeTx(false);
            JPAPlugin.startTx(false);
            nothing = JustifiedType.findById(nothing.id);
            specifiedMinutes = JustifiedType.findById(specifiedMinutes.id);
            absenceTypeMinutes = JustifiedType.findById(absenceTypeMinutes.id);
            allDay = JustifiedType.findById(allDay.id);
            halfDay = JustifiedType.findById(halfDay.id);
            assignAllDay = JustifiedType.findById(assignAllDay.id);
          }
        }
      }
    }
    
  }
  
  private void expireCode(AbsenceType absenceType) {
    if (absenceType.isExpired()) {
      absenceType.validTo = new LocalDate(2015, 12 , 31);
      absenceType.save();
    }
  }
   
  private void migrateAbsence(Absence absence, JustifiedType nothing,
      JustifiedType specifiedMinutes, JustifiedType absenceTypeMinutes, JustifiedType allDay,
      JustifiedType halfDay, JustifiedType assignAllDay) {
 
    if (absence.absenceType.justifiedTimeAtWork == null) {
      return;
    }
    
    // Fix 89
    if (absence.absenceType.code.equals("89")) {
      absence.justifiedType = nothing;
      absence.save();
      return;
    }
    
    // Fix 09H*
    if (absence.absenceType.code.equals("09H1") 
        || absence.absenceType.code.equals("09H2")
        || absence.absenceType.code.equals("09H3")
        || absence.absenceType.code.equals("09H4")
        || absence.absenceType.code.equals("09H5")
        || absence.absenceType.code.equals("09H6")
        || absence.absenceType.code.equals("09H7")) {
      absence.justifiedMinutes = absence.absenceType.justifiedTimeAtWork.minutes;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("09M").get();
      absence.justifiedType = specifiedMinutes;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    
    // Fix 18H*
    if (absence.absenceType.code.equals("18H1") 
        || absence.absenceType.code.equals("18H2")
        || absence.absenceType.code.equals("18H3")
        || absence.absenceType.code.equals("18H4")
        || absence.absenceType.code.equals("18H5")
        || absence.absenceType.code.equals("18H6")
        || absence.absenceType.code.equals("18H7")
        || absence.absenceType.code.equals("18H8")) {
      absence.justifiedMinutes = absence.absenceType.justifiedTimeAtWork.minutes;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("18M").get();
      absence.justifiedType = specifiedMinutes;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    if (absence.absenceType.code.equals("18H9")) {
      absence.justifiedMinutes = 60 * 9;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("18M").get();
      absence.justifiedType = specifiedMinutes;
      expireCode(absence.absenceType);
      absence.save();
    }
    
    // Fix 19H*
    if (absence.absenceType.code.equals("19H1") 
        || absence.absenceType.code.equals("19H2")
        || absence.absenceType.code.equals("19H3")
        || absence.absenceType.code.equals("19H4")
        || absence.absenceType.code.equals("19H5")
        || absence.absenceType.code.equals("19H6")
        || absence.absenceType.code.equals("19H7")
        || absence.absenceType.code.equals("19H8")) {
      absence.justifiedMinutes = absence.absenceType.justifiedTimeAtWork.minutes;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("19M").get();
      absence.justifiedType = specifiedMinutes;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    if (absence.absenceType.code.equals("19H9")) {
      absence.justifiedMinutes = 60 * 9;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("19M").get();
      absence.justifiedType = specifiedMinutes;
      expireCode(absence.absenceType);
      absence.save();
    }
    
    // Fix 661H*
    if (absence.absenceType.code.equals("661H7") 
        && absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {
      absence.absenceType = absenceComponentDao.absenceTypeByCode("661M").get();
      absence.justifiedMinutes = 60 * 7;
      absence.justifiedType = specifiedMinutes;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    if (absence.absenceType.code.equals("661H1") 
        || absence.absenceType.code.equals("661H2")
        || absence.absenceType.code.equals("661H3")
        || absence.absenceType.code.equals("661H4")
        || absence.absenceType.code.equals("661H5")
        || absence.absenceType.code.equals("661H6")
        || absence.absenceType.code.equals("661H7")
        || absence.absenceType.code.equals("661H8")) {
      absence.justifiedMinutes = absence.absenceType.justifiedTimeAtWork.minutes;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("661M").get();
      absence.justifiedType = specifiedMinutes;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    if (absence.absenceType.code.equals("661H9")) {
      absence.justifiedMinutes = 60 * 9;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("661M").get();
      absence.justifiedType = specifiedMinutes;
      expireCode(absence.absenceType);
      absence.save();
    }
    
    // Fix 89H*
    if (absence.absenceType.code.equals("89H1") 
        || absence.absenceType.code.equals("89H2")
        || absence.absenceType.code.equals("89H3")
        || absence.absenceType.code.equals("89H4")
        || absence.absenceType.code.equals("89H5")
        || absence.absenceType.code.equals("89H6")
        || absence.absenceType.code.equals("89H7")) {
      absence.justifiedMinutes = absence.absenceType.justifiedTimeAtWork.minutes;
      absence.absenceType = absenceComponentDao.absenceTypeByCode("89M").get();
      absence.justifiedType = specifiedMinutes;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    
    // Fix 90
    if (absence.absenceType.code.equals("90")) {
      absence.justifiedType = allDay;
      absence.save();
      expireCode(absence.absenceType);
      return;
    }
    
    if (absence.absenceType.code.equals("PEPE") || absence.absenceType.code.equals("RITING")) {
      absence.justifiedType = specifiedMinutes; //PEPE //RITING
      absence.save();
      return;
    }
    
    // Assenze orarie.
    if (absence.absenceType.justifiedTimeAtWork.minutes != null 
        && absence.absenceType.justifiedTimeAtWork.minutes > 0) {
      absence.justifiedType = absenceTypeMinutes;
      absence.save();
      return;
    }
    
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {  
      absence.justifiedType = allDay;
      absence.save();
      return;
    }
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.Nothing)) { 
      absence.justifiedType = nothing;
      absence.save();
      return;
    }
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.HalfDay)) {
      absence.justifiedType = halfDay;
      absence.save();
      return;
    }
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay)) {
      absence.justifiedType = assignAllDay;
      absence.save();
      return;
    }
    
  }
  
  public void buildDefaultGroups() { 
    
    LocalDate expireDate = new LocalDate(2015, 12, 31);
    
    final JustifiedType nothing = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing);
    final JustifiedType specifiedMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    final JustifiedType absenceTypeMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.absence_type_minutes);
    final JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    final JustifiedType halfDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.half_day);
    final JustifiedType assignAllDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.assign_all_day);

    final JustifiedType allDayLimit = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day_limit);
    final JustifiedType specifiedMinutesLimit = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes_limit);

    final CategoryGroupAbsenceType generalCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.GENERAL.name, 
            DefaultCategoryType.GENERAL.priority);
    
    final CategoryGroupAbsenceType permissionCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.PERMISSION.name, 
            DefaultCategoryType.PERMISSION.priority);
    
    final CategoryGroupAbsenceType postpartumCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.POST_PARTUM.name, 
            DefaultCategoryType.POST_PARTUM.priority);
    
    final CategoryGroupAbsenceType lawCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.LAW_104_92.name, 
            DefaultCategoryType.LAW_104_92.priority);
    
    final CategoryGroupAbsenceType publicFunctionCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.PUBLIC_FUNCTION.name, 
            DefaultCategoryType.PUBLIC_FUNCTION.priority);
    
    final CategoryGroupAbsenceType otherCodesCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.OTHER_CODES.name, 
            DefaultCategoryType.OTHER_CODES.priority);
    
    final CategoryGroupAbsenceType malattiaCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.MALATTIA.name, 
            DefaultCategoryType.MALATTIA.priority);
    
    final CategoryGroupAbsenceType malattiaFiglio1Category = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.MALATTIA_FIGLIO_1.name, 
            DefaultCategoryType.MALATTIA_FIGLIO_1.priority);
    
    final CategoryGroupAbsenceType malattiaFiglio2Category = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.MALATTIA_FIGLIO_2.name, 
            DefaultCategoryType.MALATTIA_FIGLIO_2.priority);
    
    final CategoryGroupAbsenceType malattiaFiglio3Category = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.MALATTIA_FIGLIO_3.name, 
            DefaultCategoryType.MALATTIA_FIGLIO_3.priority);
    
    final CategoryGroupAbsenceType automaticCodes = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.AUTOMATIC_CODES.name, 
            DefaultCategoryType.AUTOMATIC_CODES.priority);
    
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    for (AbsenceType absenceType : absenceTypes) {
      absenceType.code = absenceType.code.toUpperCase();
      absenceType.justifiedTime = 0;
      if (absenceType.justifiedTimeAtWork == null) {
        // TODO: andrebbero disabilitate.
        log.info("absenceType.code {} è justifiedTimeAtWork nullo", absenceType.code);
        continue;
      }
      
      // Fix 661H7
      if (absenceType.code.equals("661H7") 
          && absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
        absenceType.justifiedTime = 60 * 7;
        absenceType.save();
        continue;
      }
      
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.Nothing)) {
        absenceType.justifiedTypesPermitted.add(nothing);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {
        absenceType.justifiedTypesPermitted.add(allDay);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.HalfDay)) {
        absenceType.justifiedTypesPermitted.add(halfDay);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay)) {
        absenceType.justifiedTypesPermitted.add(assignAllDay);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.OneHour)) {
        absenceType.justifiedTime = 60;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.TwoHours)) {
        absenceType.justifiedTime = 120;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.ThreeHours)) {
        absenceType.justifiedTime = 180;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHours)) {
        absenceType.justifiedTime = 240;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHours)) {
        absenceType.justifiedTime = 300;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHours)) {
        absenceType.justifiedTime = 360;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHours)) {
        absenceType.justifiedTime = 420;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.EightHours)) {
        absenceType.justifiedTime = 480;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.OneHourMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.TwoHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.ThreeHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.EightHoursMealTimeCounting))
      {
        absenceType.timeForMealTicket = true;
      }
      
      absenceType.save();

    }

    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_18.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr18 = absenceComponentDao.buildOrEditAbsenceType("18", 
          "Permesso assistenza parenti/affini disabili L. 104/92 intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "18", null);

      absenceComponentDao.buildOrEditAbsenceType("18H1", 
          "Permesso assistenza parenti/affini disabili L. 104/92 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H1", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H2", 
          "Permesso assistenza parenti/affini disabili L. 104/92 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H2", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H3", 
          "Permesso assistenza parenti/affini disabili L. 104/92 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H3", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H4", 
          "Permesso assistenza parenti/affini disabili L. 104/92 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H4", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H5", 
          "Permesso assistenza parenti/affini disabili L. 104/92 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H5", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H6", 
          "Permesso assistenza parenti/affini disabili L. 104/92 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H6", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H7", 
          "Permesso assistenza parenti/affini disabili L. 104/92 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H7", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("18H8", 
          "Permesso assistenza parenti/affini disabili L. 104/92 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "18H8", expireDate);

      AbsenceType m18 = absenceComponentDao.buildOrEditAbsenceType("18M", 
          "Permesso assistenza parenti/affini disabili L. 104/92 in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      AbsenceType h1c18 = absenceComponentDao.buildOrEditAbsenceType("18H1C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 1 ora", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 60, false, false, false, null, null);
      AbsenceType h2c18 = absenceComponentDao.buildOrEditAbsenceType("18H2C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 2 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 120, false, false, false, null, null);
      AbsenceType h3c18 = absenceComponentDao.buildOrEditAbsenceType("18H3C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 3 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 180, false, false, false, null, null);
      AbsenceType h4c18 = absenceComponentDao.buildOrEditAbsenceType("18H4C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 4 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 240, false, false, false, null, null);
      AbsenceType h5c18 = absenceComponentDao.buildOrEditAbsenceType("18H5C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 5 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 300, false, false, false, null, null);
      AbsenceType h6c18 = absenceComponentDao.buildOrEditAbsenceType("18H6C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 6 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 360, false, false, false, null, null);
      AbsenceType h7c18 = absenceComponentDao.buildOrEditAbsenceType("18H7C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 7 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 420, false, false, false, null, null);
      AbsenceType h8c18 = absenceComponentDao.buildOrEditAbsenceType("18H8C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 8 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 480, false, false, false, null, null);
      AbsenceType h9c18 = absenceComponentDao.buildOrEditAbsenceType("18H9C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 9 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 540, false, false, false, null, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c18 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_18.name());

      if (!c18.isPresent()) {

        c18 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c18.get().name = DefaultComplation.C_18.name();
        c18.get().amountType = AmountType.minutes;
        c18.get().complationCodes.add(m18);
        c18.get().replacingCodes = 
            Sets.newHashSet(h1c18, h2c18, h3c18, h4c18, h5c18, h6c18, h7c18, h8c18, h9c18);
        c18.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t18 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_18.name());

      if (!t18.isPresent()) {

        t18 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t18.get().name = DefaultTakable.T_18.name();
        t18.get().amountType = AmountType.units;
        t18.get().takableCodes = Sets.newHashSet(cnr18, m18);
        t18.get().takenCodes = Sets.newHashSet(cnr18, m18);
        t18.get().fixedLimit = 3;
        t18.get().save();
      }

      // Group Creation
      GroupAbsenceType group18 = new GroupAbsenceType();
      group18.category = lawCategory;
      group18.name = DefaultGroup.G_18.name();
      group18.description = "18 - Permesso assistenza parenti/affini disabili L. 104/92 tre giorni mese";
      group18.pattern = GroupAbsenceTypePattern.programmed;
      group18.periodType = PeriodType.month;
      group18.complationAbsenceBehaviour = c18.get();
      group18.takableAbsenceBehaviour = t18.get();
      group18.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_19.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr19 = absenceComponentDao.buildOrEditAbsenceType("19", 
          "Permesso per dipendente disabile L. 104/92 intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "19", null);

      absenceComponentDao.buildOrEditAbsenceType("19H1", 
          "Permesso per dipendente disabile L. 104/92 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H1", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H2", 
          "Permesso per dipendente disabile L. 104/92 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H2", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H3", 
          "Permesso per dipendente disabile L. 104/92 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H3", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H4", 
          "Permesso per dipendente disabile L. 104/92 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H4", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H5", 
          "Permesso per dipendente disabile L. 104/92 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H5", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H6", 
          "Permesso per dipendente disabile L. 104/92 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H6", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H7", 
          "Permesso per dipendente disabile L. 104/92 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H7", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("19H8", 
          "Permesso per dipendente disabile L. 104/92 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "19H8", expireDate);

      AbsenceType m19 = absenceComponentDao.buildOrEditAbsenceType("19M", 
          "Permesso per dipendente disabile L. 104/92 in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      AbsenceType h1c19 = absenceComponentDao.buildOrEditAbsenceType("19H1C", 
          "Permesso per dipendente disabile L. 104/92 completamento 1 ora", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 60, false, false, false, null, null);
      AbsenceType h2c19 = absenceComponentDao.buildOrEditAbsenceType("19H2C", 
          "Permesso per dipendente disabile L. 104/92 completamento 2 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 120, false, false, false, null, null);
      AbsenceType h3c19 = absenceComponentDao.buildOrEditAbsenceType("19H3C", 
          "Permesso per dipendente disabile L. 104/92 completamento 3 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 180, false, false, false, null, null);
      AbsenceType h4c19 = absenceComponentDao.buildOrEditAbsenceType("19H4C", 
          "Permesso per dipendente disabile L. 104/92 completamento 4 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 240, false, false, false, null, null);
      AbsenceType h5c19 = absenceComponentDao.buildOrEditAbsenceType("19H5C", 
          "Permesso per dipendente disabile L. 104/92 completamento 5 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 300, false, false, false, null, null);
      AbsenceType h6c19 = absenceComponentDao.buildOrEditAbsenceType("19H6C", 
          "Permesso per dipendente disabile L. 104/92 completamento 6 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 360, false, false, false, null, null);
      AbsenceType h7c19 = absenceComponentDao.buildOrEditAbsenceType("19H7C", 
          "Permesso per dipendente disabile L. 104/92 completamento 7 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 420, false, false, false, null, null);
      AbsenceType h8c19 = absenceComponentDao.buildOrEditAbsenceType("19H8C", 
          "Permesso per dipendente disabile L. 104/92 completamento 8 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 480, false, false, false, null, null);
      AbsenceType h9c19 = absenceComponentDao.buildOrEditAbsenceType("19H9C", 
          "Permesso per dipendente disabile L. 104/92 completamento 9 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 540, false, false, false, null, null);
      
      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c19 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_19.name());

      if (!c19.isPresent()) {

        c19 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c19.get().name = DefaultComplation.C_19.name();
        c19.get().amountType = AmountType.minutes;
        c19.get().complationCodes.add(m19);
        c19.get().replacingCodes = 
            Sets.newHashSet(h1c19, h2c19, h3c19, h4c19, h5c19, h6c19, h7c19, h8c19, h9c19);
        c19.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t19 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_19.name());

      if (!t19.isPresent()) {

        t19 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t19.get().name = DefaultTakable.T_19.name();
        t19.get().amountType = AmountType.units;
        t19.get().takableCodes = Sets.newHashSet(cnr19, m19);
        t19.get().takenCodes = Sets.newHashSet(cnr19, m19);
        t19.get().fixedLimit = 3;
        t19.get().save();
      }

      // Group Creation
      GroupAbsenceType group19 = new GroupAbsenceType();
      group19.category = lawCategory;
      group19.name = DefaultGroup.G_19.name();
      group19.description = "19 - Permesso per dipendente disabile L. 104/92 tre giorni mese";
      group19.pattern = GroupAbsenceTypePattern.programmed;
      group19.periodType = PeriodType.month;
      group19.complationAbsenceBehaviour = c19.get();
      group19.takableAbsenceBehaviour = t19.get();
      group19.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_661.name()).isPresent()) {

      //Update AbsenceType
//      AbsenceType cnr661 = absenceComponentDao.buildOrEditAbsenceType("661", 
//          "Permesso orario per motivi personali intera giornata", 
//          0, Sets.newHashSet(allDay), false, false, false, "661");

      absenceComponentDao.buildOrEditAbsenceType("661H1", 
          "Permesso orario per motivi personali 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H1", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H2", 
          "Permesso orario per motivi personali 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H2", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H3", 
          "Permesso orario per motivi personali 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H3", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H4", 
          "Permesso orario per motivi personali 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H4", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H5", 
          "Permesso orario per motivi personali 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H5", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H6", 
          "Permesso orario per motivi personali 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H6", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H7", 
          "Permesso orario per motivi personali 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H7", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("661H8", 
          "Permesso orario per motivi personali 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "661H8", expireDate);

      AbsenceType m661 = absenceComponentDao.buildOrEditAbsenceType("661M", 
          "Permesso orario per motivi personali in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      AbsenceType h1c661 = absenceComponentDao.buildOrEditAbsenceType("661H1C", 
          "Permesso orario per motivi personali completamento 1 ora", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 60, false, false, false, null, null);
      AbsenceType h2c661 = absenceComponentDao.buildOrEditAbsenceType("661H2C", 
          "Permesso orario per motivi personali completamento 2 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 120, false, false, false, null, null);
      AbsenceType h3c661 = absenceComponentDao.buildOrEditAbsenceType("661H3C", 
          "Permesso orario per motivi personali completamento 3 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 180, false, false, false, null, null);
      AbsenceType h4c661 = absenceComponentDao.buildOrEditAbsenceType("661H4C", 
          "Permesso orario per motivi personali completamento 4 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 240, false, false, false, null, null);
      AbsenceType h5c661 = absenceComponentDao.buildOrEditAbsenceType("661H5C", 
          "Permesso orario per motivi personali completamento 5 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 300, false, false, false, null, null);
      AbsenceType h6c661 = absenceComponentDao.buildOrEditAbsenceType("661H6C", 
          "Permesso orario per motivi personali completamento 6 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 360, false, false, false, null, null);
      AbsenceType h7c661 = absenceComponentDao.buildOrEditAbsenceType("661H7C", 
          "Permesso orario per motivi personali completamento 7 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 420, false, false, false, null, null);
      AbsenceType h8c661 = absenceComponentDao.buildOrEditAbsenceType("661H8C", 
          "Permesso orario per motivi personali completamento 8 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 480, false, false, false, null, null);
      AbsenceType h9c661 = absenceComponentDao.buildOrEditAbsenceType("661H9C", 
          "Permesso orario per motivi personali completamento 9 ore", 
          0, Sets.newHashSet(nothing), absenceTypeMinutes, 540, false, false, false, null, null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c661 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_661.name());

      if (!c661.isPresent()) {

        c661 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c661.get().name = DefaultComplation.C_661.name();
        c661.get().amountType = AmountType.minutes;
        c661.get().complationCodes.add(m661);
        c661.get().replacingCodes = 
            Sets.newHashSet(h1c661, h2c661, h3c661, h4c661, h5c661, h6c661, h7c661, h8c661, h9c661);
        c661.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t661 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_661.name());

      if (!t661.isPresent()) {

        t661 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t661.get().name = DefaultTakable.T_661.name();
        t661.get().amountType = AmountType.minutes;
        t661.get().takableCodes = Sets.newHashSet(m661);
        t661.get().takenCodes = Sets.newHashSet(m661);
        t661.get().fixedLimit = 1080;
        t661.get().takableAmountAdjustment = TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent;
        t661.get().save();
      }

      // Group Creation
      GroupAbsenceType group661 = new GroupAbsenceType();
      group661.category = permissionCategory;
      group661.name = DefaultGroup.G_661.name();
      group661.description = "661 - Permesso orario per motivi personali 18 ore anno";
      group661.pattern = GroupAbsenceTypePattern.programmed;
      group661.periodType = PeriodType.year;
      group661.complationAbsenceBehaviour = c661.get();
      group661.takableAbsenceBehaviour = t661.get();
      group661.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_25.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr25 = absenceComponentDao.buildOrEditAbsenceType("25", 
          "Astensione facoltativa post partum 30% primo figlio intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "25", null);
      
      //Update AbsenceType
      AbsenceType cnr25u = absenceComponentDao.buildOrEditAbsenceType("25U", 
          "Astensione facoltativa post partum 30% primo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(allDayLimit), null, 0, false, false, false, null, null);
     
      AbsenceType m25 = absenceComponentDao.buildOrEditAbsenceType("25M", 
          "Astensione facoltativa post partum 30% primo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      
      AbsenceType cnr25h7 = absenceComponentDao.buildOrEditAbsenceType("25H7", 
          "Astensione facoltativa post partum 30% primo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "25H7", null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c25 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_25.name());

      if (!c25.isPresent()) {

        c25 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c25.get().name = DefaultComplation.C_25.name();
        c25.get().amountType = AmountType.units;
        c25.get().complationCodes.add(m25);
        c25.get().replacingCodes = Sets.newHashSet(cnr25h7);
        c25.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t25 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_25.name());

      if (!t25.isPresent()) {

        t25 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t25.get().name = DefaultTakable.T_25.name();
        t25.get().amountType = AmountType.units;
        t25.get().takableCodes = Sets.newHashSet(cnr25, cnr25u, m25);
        t25.get().takenCodes = Sets.newHashSet(cnr25, cnr25u, m25);
        t25.get().fixedLimit = 150;
        t25.get().save();
      }

      // Group Creation
      GroupAbsenceType group25 = new GroupAbsenceType();
      group25.category = postpartumCategory;
      group25.name = DefaultGroup.G_25.name();
      group25.description = "Astensione facoltativa post partum 30% primo figlio 0-6 anni 150 giorni";
      group25.pattern = GroupAbsenceTypePattern.programmed;
      group25.periodType = PeriodType.child1_0_6;
      group25.complationAbsenceBehaviour = c25.get();
      group25.takableAbsenceBehaviour = t25.get();
      group25.save();

    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_23.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr23 = absenceComponentDao.buildOrEditAbsenceType("23", 
          "Astensione facoltativa post partum 100% primo figlio intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "23", null);
      
      //Update AbsenceType
      AbsenceType cnr23u = absenceComponentDao.buildOrEditAbsenceType("23U", 
          "Astensione facoltativa post partum 100% primo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(allDayLimit), null, 0, false, false, false, null, null);
      
      AbsenceType m23 = absenceComponentDao.buildOrEditAbsenceType("23M", 
          "Astensione facoltativa post partum 100% primo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      
      AbsenceType cnr23h7 = absenceComponentDao.buildOrEditAbsenceType("23H7", 
          "Astensione facoltativa post partum 100% primo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "23H7", null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c23 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_23.name());

      if (!c23.isPresent()) {

        c23 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c23.get().name = DefaultComplation.C_23.name();
        c23.get().amountType = AmountType.units;
        c23.get().complationCodes.add(m23);
        c23.get().replacingCodes = Sets.newHashSet(cnr23h7);
        c23.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t23 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_23.name());

      if (!t23.isPresent()) {

        t23 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t23.get().name = DefaultTakable.T_23.name();
        t23.get().amountType = AmountType.units;
        t23.get().takableCodes = Sets.newHashSet(cnr23, cnr23u, m23);
        t23.get().takenCodes = Sets.newHashSet(cnr23, cnr23u, m23);
        t23.get().fixedLimit = 30;
        t23.get().save();
      }

      // Group Creation
      GroupAbsenceType group23 = new GroupAbsenceType();
      group23.category = postpartumCategory;
      group23.name = DefaultGroup.G_23.name();
      group23.description = "Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni";
      group23.chainDescription = "23 - Astensione facoltativa post partum primo figlio";
      group23.pattern = GroupAbsenceTypePattern.programmed;
      group23.periodType = PeriodType.child1_0_12;
      group23.complationAbsenceBehaviour = c23.get();
      group23.takableAbsenceBehaviour = t23.get();
      
      group23.nextGroupToCheck = absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_25.name()).get();
      
      group23.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_252.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr252 = absenceComponentDao.buildOrEditAbsenceType("252", 
          "Astensione facoltativa post partum 30% secondo figlio intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "252", null);
      
      //Update AbsenceType
      AbsenceType cnr252u = absenceComponentDao.buildOrEditAbsenceType("252U", 
          "Astensione facoltativa post partum 30% secondo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(allDayLimit), null, 0, false, false, false, null, null);
     
      AbsenceType m252 = absenceComponentDao.buildOrEditAbsenceType("252M", 
          "Astensione facoltativa post partum 30% secondo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      
      AbsenceType cnr252h7 = absenceComponentDao.buildOrEditAbsenceType("252H7", 
          "Astensione facoltativa post partum 30% secondo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "252H7", null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c252 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_252.name());

      if (!c252.isPresent()) {

        c252 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c252.get().name = DefaultComplation.C_252.name();
        c252.get().amountType = AmountType.units;
        c252.get().complationCodes.add(m252);
        c252.get().replacingCodes = Sets.newHashSet(cnr252h7);
        c252.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t252 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_252.name());

      if (!t252.isPresent()) {

        t252 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t252.get().name = DefaultTakable.T_252.name();
        t252.get().amountType = AmountType.units;
        t252.get().takableCodes = Sets.newHashSet(cnr252, cnr252u, m252);
        t252.get().takenCodes = Sets.newHashSet(cnr252, cnr252u, m252);
        t252.get().fixedLimit = 150;
        t252.get().save();
      }

      // Group Creation
      GroupAbsenceType group252 = new GroupAbsenceType();
      group252.category = postpartumCategory;
      group252.name = DefaultGroup.G_252.name();
      group252.description = "Astensione facoltativa post partum 30% secondo figlio 0-6 anni 150 giorni";
      group252.pattern = GroupAbsenceTypePattern.programmed;
      group252.periodType = PeriodType.child2_0_6;
      group252.complationAbsenceBehaviour = c252.get();
      group252.takableAbsenceBehaviour = t252.get();
      group252.save();

    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_232.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr232 = absenceComponentDao.buildOrEditAbsenceType("232", 
          "Astensione facoltativa post partum 100% secondo figlio intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "232", null);
      
      //Update AbsenceType
      AbsenceType cnr232u = absenceComponentDao.buildOrEditAbsenceType("232U", 
          "Astensione facoltativa post partum 100% secondo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(allDayLimit), null, 0, false, false, false, null, null);
      
      AbsenceType m232 = absenceComponentDao.buildOrEditAbsenceType("232M", 
          "Astensione facoltativa post partum 100% secondo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      
      AbsenceType cnr232h7 = absenceComponentDao.buildOrEditAbsenceType("232H7", 
          "Astensione facoltativa post partum 100% secondo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "232H7", null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c232 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_232.name());

      if (!c232.isPresent()) {

        c232 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c232.get().name = DefaultComplation.C_232.name();
        c232.get().amountType = AmountType.units;
        c232.get().complationCodes.add(m232);
        c232.get().replacingCodes = Sets.newHashSet(cnr232h7);
        c232.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t232 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_232.name());

      if (!t232.isPresent()) {

        t232 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t232.get().name = DefaultTakable.T_232.name();
        t232.get().amountType = AmountType.units;
        t232.get().takableCodes = Sets.newHashSet(cnr232, cnr232u, m232);
        t232.get().takenCodes = Sets.newHashSet(cnr232, cnr232u, m232);
        t232.get().fixedLimit = 30;
        t232.get().save();
      }

      // Group Creation
      GroupAbsenceType group232 = new GroupAbsenceType();
      group232.category = postpartumCategory;
      group232.name = DefaultGroup.G_232.name();
      group232.description = "Astensione facoltativa post partum 100% secondo figlio 0-12 anni 30 giorni";
      group232.chainDescription = "232 - Astensione facoltativa post partum secondo figlio";
      group232.pattern = GroupAbsenceTypePattern.programmed;
      group232.periodType = PeriodType.child2_0_12;
      group232.complationAbsenceBehaviour = c232.get();
      group232.takableAbsenceBehaviour = t232.get();
      
      group232.nextGroupToCheck = absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_252.name()).get();
      
      group232.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_253.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr253 = absenceComponentDao.buildOrEditAbsenceType("253", 
          "Astensione facoltativa post partum 30% terzo figlio intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "253", null);
      
      //Update AbsenceType
      AbsenceType cnr253u = absenceComponentDao.buildOrEditAbsenceType("253U", 
          "Astensione facoltativa post partum 30% terzo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(allDayLimit), null, 0, false, false, false, null, null);
     
      AbsenceType m253 = absenceComponentDao.buildOrEditAbsenceType("253M", 
          "Astensione facoltativa post partum 30% terzo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      
      AbsenceType cnr253h7 = absenceComponentDao.buildOrEditAbsenceType("253H7", 
          "Astensione facoltativa post partum 30% terzo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "253H7", null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c253 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_253.name());

      if (!c253.isPresent()) {

        c253 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c253.get().name = DefaultComplation.C_253.name();
        c253.get().amountType = AmountType.units;
        c253.get().complationCodes.add(m253);
        c253.get().replacingCodes = Sets.newHashSet(cnr253h7);
        c253.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t253 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_253.name());

      if (!t253.isPresent()) {

        t253 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t253.get().name = DefaultTakable.T_253.name();
        t253.get().amountType = AmountType.units;
        t253.get().takableCodes = Sets.newHashSet(cnr253, cnr253u, m253);
        t253.get().takenCodes = Sets.newHashSet(cnr253, cnr253u, m253);
        t253.get().fixedLimit = 150;
        t253.get().save();
      }

      // Group Creation
      GroupAbsenceType group253 = new GroupAbsenceType();
      group253.category = postpartumCategory;
      group253.name = DefaultGroup.G_253.name();
      group253.description = "Astensione facoltativa post partum 30% terzo figlio 0-6 anni 150 giorni";
      group253.pattern = GroupAbsenceTypePattern.programmed;
      group253.periodType = PeriodType.child3_0_6;
      group253.complationAbsenceBehaviour = c253.get();
      group253.takableAbsenceBehaviour = t253.get();
      group253.save();

    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_233.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr233 = absenceComponentDao.buildOrEditAbsenceType("233", 
          "Astensione facoltativa post partum 100% terzo figlio intera giornata", 
          0, Sets.newHashSet(allDay), null, 0, false, false, false, "233", null);
      
      //Update AbsenceType
      AbsenceType cnr233u = absenceComponentDao.buildOrEditAbsenceType("233U", 
          "Astensione facoltativa post partum 100% terzo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(allDayLimit), null, 0, false, false, false, null, null);
      
      AbsenceType m233 = absenceComponentDao.buildOrEditAbsenceType("233M", 
          "Astensione facoltativa post partum 100% terzo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);
      
      AbsenceType cnr233h7 = absenceComponentDao.buildOrEditAbsenceType("233H7", 
          "Astensione facoltativa post partum 100% terzo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "233H7", null);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c233 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_233.name());

      if (!c233.isPresent()) {

        c233 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c233.get().name = DefaultComplation.C_233.name();
        c233.get().amountType = AmountType.units;
        c233.get().complationCodes.add(m233);
        c233.get().replacingCodes = Sets.newHashSet(cnr233h7);
        c233.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t233 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_233.name());

      if (!t233.isPresent()) {

        t233 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t233.get().name = DefaultTakable.T_233.name();
        t233.get().amountType = AmountType.units;
        t233.get().takableCodes = Sets.newHashSet(cnr233, cnr233u, m233);
        t233.get().takenCodes = Sets.newHashSet(cnr233, cnr233u, m233);
        t233.get().fixedLimit = 30;
        t233.get().save();
      }

      // Group Creation
      GroupAbsenceType group233 = new GroupAbsenceType();
      group233.category = postpartumCategory;
      group233.name = DefaultGroup.G_233.name();
      group233.description = "Astensione facoltativa post partum 100% terzo figlio 0-12 anni 30 giorni";
      group233.chainDescription = "233 - Astensione facoltativa post partum terzo figlio";
      group233.pattern = GroupAbsenceTypePattern.programmed;
      group233.periodType = PeriodType.child3_0_12;
      group233.complationAbsenceBehaviour = c233.get();
      group233.takableAbsenceBehaviour = t233.get();
      
      group233.nextGroupToCheck = absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_253.name()).get();
      
      group233.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_89.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr89 = absenceComponentDao.buildOrEditAbsenceType("89", 
          "Permesso diritto allo studio completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "89", null);
      
      AbsenceType m89 = absenceComponentDao.buildOrEditAbsenceType("89M", 
          "Permesso diritto allo studio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);

      absenceComponentDao.buildOrEditAbsenceType("89H1", 
          "Permesso diritto allo studio 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H1", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H2", 
          "Permesso diritto allo studio 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H2", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H3", 
          "Permesso diritto allo studio 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H3", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H4", 
          "Permesso diritto allo studio 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H4", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H5", 
          "Permesso diritto allo studio 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H5", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H6", 
          "Permesso diritto allo studio 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H6", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H7", 
          "Permesso diritto allo studio 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H7", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("89H8", 
          "Permesso diritto allo studio 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "89H8", expireDate);

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c89 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_89.name());

      if (!c89.isPresent()) {

        c89 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c89.get().name = DefaultComplation.C_89.name();
        c89.get().amountType = AmountType.units;
        c89.get().complationCodes.add(m89);
        c89.get().replacingCodes = Sets.newHashSet(cnr89);
        c89.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t89 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_89.name());

      if (!t89.isPresent()) {

        t89 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t89.get().name = DefaultTakable.T_89.name();
        t89.get().amountType = AmountType.minutes;
        t89.get().takableCodes = Sets.newHashSet(m89);
        t89.get().takenCodes = Sets.newHashSet(m89);
        t89.get().fixedLimit = 9000;
        t89.get().takableAmountAdjustment = TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent;
        t89.get().save();
      }

      // Group Creation
      GroupAbsenceType group89 = new GroupAbsenceType();
      group89.category = permissionCategory;
      group89.name = DefaultGroup.G_89.name();
      group89.description = "89 - Permesso diritto allo studio 150 ore anno";
      group89.pattern = GroupAbsenceTypePattern.programmed;
      group89.periodType = PeriodType.year;
      group89.complationAbsenceBehaviour = c89.get();
      group89.takableAbsenceBehaviour = t89.get();
      group89.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_09.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr09B = absenceComponentDao.buildOrEditAbsenceType("09B", 
          "Permesso visita medica completamento giornata", 
          0, Sets.newHashSet(nothing), allDay, 0, false, false, false, "09B", null);
      
      AbsenceType m09 = absenceComponentDao.buildOrEditAbsenceType("09M", 
          "Permesso visita medica in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), null, 0, true, false, false, null, null);

      absenceComponentDao.buildOrEditAbsenceType("09H1", 
          "Permesso orario per visita medica 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H1", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H2", 
          "Permesso orario per visita medica 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H2", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H3", 
          "Permesso orario per visita medica 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H3", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H4", 
          "Permesso orario per visita medica 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H4", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H5", 
          "Permesso orario per visita medica 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H5", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H6", 
          "Permesso orario per visita medica 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H6", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H7", 
          "Permesso orario per visita medica 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H7", expireDate);
      absenceComponentDao.buildOrEditAbsenceType("09H8", 
          "Permesso orario per visita medica 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), null, 0, false, false, false, "09H8", expireDate);
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> t09 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_09.name());
      
      if (!t09.isPresent()) {

        t09 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t09.get().name = DefaultTakable.T_09.name();
        t09.get().amountType = AmountType.units;
        t09.get().takableCodes = Sets.newHashSet(m09);
        t09.get().takenCodes = Sets.newHashSet(m09);
        t09.get().fixedLimit = -1;
        t09.get().save();
      }

      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c09 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_09.name());

      if (!c09.isPresent()) {

        c09 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c09.get().name = DefaultComplation.C_09.name();
        c09.get().amountType = AmountType.units;
        c09.get().complationCodes.add(m09);
        c09.get().replacingCodes = Sets.newHashSet(cnr09B);
        c09.get().save();

      }

      // Group Creation
      GroupAbsenceType group09 = new GroupAbsenceType();
      group09.category = permissionCategory;
      group09.name = DefaultGroup.G_09.name();
      group09.description = "09 - Permesso visita medica";
      group09.pattern = GroupAbsenceTypePattern.programmed;
      group09.periodType = PeriodType.always;
      group09.complationAbsenceBehaviour = c09.get();
      group09.takableAbsenceBehaviour = t09.get();
      group09.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MISSIONE.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMissione = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MISSIONE.name());

      if (!tMissione.isPresent()) {

        tMissione = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMissione.get().name = DefaultTakable.T_MISSIONE.name();
        tMissione.get().amountType = AmountType.units;

        AbsenceType missione92 = absenceComponentDao.buildOrEditAbsenceType("92", 
            "Missione", 0, Sets.newHashSet(allDay), null, 0, false, true, false, "92", null);
        
        AbsenceType h192 = absenceComponentDao.buildOrEditAbsenceType("92H1", 
            "Missione 1 ora", 60, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H1", null);
        
        AbsenceType h292 = absenceComponentDao.buildOrEditAbsenceType("92H2", 
            "Missione 2 ore", 120, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H2", null);
        
        AbsenceType h392 = absenceComponentDao.buildOrEditAbsenceType("92H3", 
            "Missione 3 ore", 180, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H3", null);
        
        AbsenceType h492 = absenceComponentDao.buildOrEditAbsenceType("92H4", 
            "Missione 4 ore", 240, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H4", null);
        
        AbsenceType h592 = absenceComponentDao.buildOrEditAbsenceType("92H5", 
            "Missione 5 ore", 300, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H5", null);
        
        AbsenceType h692 = absenceComponentDao.buildOrEditAbsenceType("92H6", 
            "Missione 6 ore", 360, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H6", null);
        
        AbsenceType h792 = absenceComponentDao.buildOrEditAbsenceType("92H7", 
            "Missione 7 ore", 420, Sets.newHashSet(absenceTypeMinutes), null, 0, false, true, false, "92H7", null);
        
        tMissione.get().takableCodes = Sets
            .newHashSet(missione92, h192, h292, h392, h492, h592, h692, h792);
        tMissione.get().takenCodes = Sets
            .newHashSet(missione92, h192, h292, h392, h492, h592, h692, h792);
        
        //tMissione.get().takenCodes = Sets
        //    .newHashSet(missione92, h192, h292, h392, h492, h592, h692, h792);
        tMissione.get().fixedLimit = -1;
        tMissione.get().save();

      }
      
      // Group Creation
      GroupAbsenceType groupMission = new GroupAbsenceType();
      groupMission.category = generalCategory;
      groupMission.name = DefaultGroup.MISSIONE.name();
      groupMission.description = "Missione";
      groupMission.pattern = GroupAbsenceTypePattern.simpleGrouping;
      groupMission.periodType = PeriodType.always;
      groupMission.takableAbsenceBehaviour = tMissione.get();
      groupMission.save();
    }
    
    
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tFerie = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_FERIE_CNR.name());
      
      if (!tFerie.isPresent()) {

        tFerie = Optional.fromNullable(new TakableAbsenceBehaviour());
        tFerie.get().name = DefaultTakable.T_FERIE_CNR.name();
        tFerie.get().amountType = AmountType.units;
        tFerie.get().takableCodes = Sets.newHashSet();
        tFerie.get().takenCodes = Sets.newHashSet();
        tFerie.get().fixedLimit = -1;
        
        AbsenceType ferie32 = absenceComponentDao.buildOrEditAbsenceType("32", 
            "Ferie anno corrente", 0, Sets.newHashSet(allDay), null, 0, false, false, false, "32", null);
        
        AbsenceType ferie31 = absenceComponentDao.buildOrEditAbsenceType("31", 
            "Ferie anno precedente", 0, Sets.newHashSet(allDay), null, 0, false, false, false, "31", null);
        
        AbsenceType ferie37 = absenceComponentDao.buildOrEditAbsenceType("37", 
            "ferie anno precedente (dopo il 31/8)", 0, Sets.newHashSet(allDay), null, 0, false, false, false, "37", null);
        
        AbsenceType permesso94 = absenceComponentDao.buildOrEditAbsenceType("94", 
            "festività soppresse (ex legge 937/77)", 0, Sets.newHashSet(allDay), null, 0, false, false, false, "94", null);
        
        tFerie.get().takableCodes = Sets.newHashSet(ferie31, ferie32, ferie37, permesso94);
        tFerie.get().takenCodes = Sets.newHashSet(ferie31, ferie32, ferie37, permesso94);
        
        tFerie.get().save();
      }

      // Group Creation
      GroupAbsenceType groupFerieCnr = new GroupAbsenceType();
      groupFerieCnr.category = generalCategory;
      groupFerieCnr.name = DefaultGroup.FERIE_CNR.name();
      groupFerieCnr.description = "Ferie e permessi legge CNR";
      groupFerieCnr.pattern = GroupAbsenceTypePattern.vacationsCnr;
      groupFerieCnr.periodType = PeriodType.always;
      groupFerieCnr.takableAbsenceBehaviour = tFerie.get();
      groupFerieCnr.save();
    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.RIPOSI_CNR.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tRiposi = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_RIPOSI_CNR.name());
      
      if (!tRiposi.isPresent()) {

        tRiposi = Optional.fromNullable(new TakableAbsenceBehaviour());
        tRiposi.get().name = DefaultTakable.T_RIPOSI_CNR.name();
        tRiposi.get().amountType = AmountType.units;
        tRiposi.get().takableCodes = Sets.newHashSet();
        tRiposi.get().takenCodes = Sets.newHashSet();
        tRiposi.get().fixedLimit = -1;
        
        AbsenceType riposo91 = absenceComponentDao.buildOrEditAbsenceType("91", 
            "Riposo compensativo", 0, Sets.newHashSet(allDay), null, 0, false, true, false, "91", null);
        
        tRiposi.get().takableCodes = Sets.newHashSet(riposo91);
        tRiposi.get().takenCodes = Sets.newHashSet(riposo91);
        
        tRiposi.get().save();
      }

      // Group Creation
      GroupAbsenceType groupRiposi = new GroupAbsenceType();
      groupRiposi.category = generalCategory;
      groupRiposi.name = DefaultGroup.RIPOSI_CNR.name();
      groupRiposi.description = "Riposi compensativi CNR";
      groupRiposi.pattern = GroupAbsenceTypePattern.compensatoryRestCnr;
      groupRiposi.periodType = PeriodType.always;
      groupRiposi.takableAbsenceBehaviour = tRiposi.get();
      groupRiposi.save();
    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_95.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> t95 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_95.name());
      
      if (!t95.isPresent()) {

        t95 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t95.get().name = DefaultTakable.T_95.name();
        t95.get().amountType = AmountType.units;
        t95.get().takableCodes = Sets.newHashSet();
        t95.get().takenCodes = Sets.newHashSet();
        t95.get().fixedLimit = -1;
        t95.get().save();
      }
      
      // Set boolean independente
      absenceTypes = AbsenceType.findAll();
      for (AbsenceType absenceType : absenceTypes) {
        if (absenceType.code.startsWith("95")) {
          log.info("AbsenceCode {}", absenceType.code );
          if (absenceType.isExpired()) {
            continue;
          }
          t95.get().takableCodes.add(absenceType);
          t95.get().takenCodes.add(absenceType);
          t95.get().save();
        }
      }

      // Group Creation
      GroupAbsenceType group95 = new GroupAbsenceType();
      group95.category = publicFunctionCategory;
      group95.name = DefaultGroup.G_95.name();
      group95.description = "Permessi pubblica funzione";
      group95.pattern = GroupAbsenceTypePattern.simpleGrouping;
      group95.periodType = PeriodType.always;
      group95.takableAbsenceBehaviour = t95.get();
      group95.save();
      
    }
    
    JPA.em().flush();
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA.name());
      
      if (!tMalattia.isPresent()) {

        tMalattia = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia.get().name = DefaultTakable.T_MALATTIA.name();
        tMalattia.get().amountType = AmountType.units;
        tMalattia.get().takableCodes = Sets.newHashSet();
        tMalattia.get().takenCodes = Sets.newHashSet();
        tMalattia.get().fixedLimit = -1;
        tMalattia.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("11C").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("11C").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("11C").get());
        }
        if (absenceComponentDao.absenceTypeByCode("11R").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("11R").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("11R").get());
        }
        if (absenceComponentDao.absenceTypeByCode("11R5").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("11R").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("11R").get());
        }
        if (absenceComponentDao.absenceTypeByCode("11R9").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("11R").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("11R").get());
        }
        if (absenceComponentDao.absenceTypeByCode("11S").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("11S").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("11S").get());
        }
        if (absenceComponentDao.absenceTypeByCode("111").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("111").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("111").get());
        }
        if (absenceComponentDao.absenceTypeByCode("115").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("115").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("115").get());
        }
        if (absenceComponentDao.absenceTypeByCode("116").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("116").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("116").get());
        }
        if (absenceComponentDao.absenceTypeByCode("117").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("117").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("117").get());
        }
        if (absenceComponentDao.absenceTypeByCode("118").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("118").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("118").get());
        }
        if (absenceComponentDao.absenceTypeByCode("119").isPresent()) {
          tMalattia.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("119").get());
          tMalattia.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("119").get());
        }
        
        tMalattia.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaCategory;
      groupMalattia.name = DefaultGroup.MALATTIA.name();
      groupMalattia.description = "111 - Malattia";
      groupMalattia.pattern = GroupAbsenceTypePattern.simpleGrouping;
      groupMalattia.periodType = PeriodType.always;
      groupMalattia.takableAbsenceBehaviour = tMalattia.get();
      groupMalattia.save();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_1_12.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia12 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_1_12.name());
      
      if (!tMalattia12.isPresent()) {

        tMalattia12 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia12.get().name = DefaultTakable.T_MALATTIA_FIGLIO_1_12.name();
        tMalattia12.get().amountType = AmountType.units;
        tMalattia12.get().takableCodes = Sets.newHashSet();
        tMalattia12.get().takenCodes = Sets.newHashSet();
        tMalattia12.get().fixedLimit = -1;
        tMalattia12.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("12").isPresent()) {
          tMalattia12.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("12").get());
          tMalattia12.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("12").get());
        }
       
        tMalattia12.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio1Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_1_12.name();
      groupMalattia.description = "12 - Malattia primo figlio <= 3 anni retribuita 100%";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child1_0_3;
      groupMalattia.takableAbsenceBehaviour = tMalattia12.get();
      groupMalattia.save();
    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_1_13.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia13 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_1_13.name());
      
      if (!tMalattia13.isPresent()) {

        tMalattia13 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia13.get().name = DefaultTakable.T_MALATTIA_FIGLIO_1_13.name();
        tMalattia13.get().amountType = AmountType.units;
        tMalattia13.get().takableCodes = Sets.newHashSet();
        tMalattia13.get().takenCodes = Sets.newHashSet();
        tMalattia13.get().fixedLimit = -1;
        tMalattia13.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("13").isPresent()) {
          tMalattia13.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("13").get());
          tMalattia13.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("13").get());
        }
       
        tMalattia13.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio1Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_1_13.name();
      groupMalattia.description = "13 - Malattia primo figlio oltre 3 anni non retribuita";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child1_3_12;
      groupMalattia.takableAbsenceBehaviour = tMalattia13.get();
      groupMalattia.save();
    }
    

    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_1_14.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia14 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_1_14.name());
      
      if (!tMalattia14.isPresent()) {

        tMalattia14 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia14.get().name = DefaultTakable.T_MALATTIA_FIGLIO_1_14.name();
        tMalattia14.get().amountType = AmountType.units;
        tMalattia14.get().takableCodes = Sets.newHashSet();
        tMalattia14.get().takenCodes = Sets.newHashSet();
        tMalattia14.get().fixedLimit = -1;
        tMalattia14.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("14").isPresent()) {
          tMalattia14.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("14").get());
          tMalattia14.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("14").get());
        }
       
        tMalattia14.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio1Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_1_14.name();
      groupMalattia.description = "14 - Malattia primo figlio <= 3 anni non retribuita";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child1_0_3;
      groupMalattia.takableAbsenceBehaviour = tMalattia14.get();
      groupMalattia.save();
    }
    

    
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_2_12.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia12 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_2_12.name());
      
      if (!tMalattia12.isPresent()) {

        tMalattia12 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia12.get().name = DefaultTakable.T_MALATTIA_FIGLIO_2_12.name();
        tMalattia12.get().amountType = AmountType.units;
        tMalattia12.get().takableCodes = Sets.newHashSet();
        tMalattia12.get().takenCodes = Sets.newHashSet();
        tMalattia12.get().fixedLimit = -1;
        tMalattia12.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("122").isPresent()) {
          tMalattia12.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("122").get());
          tMalattia12.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("122").get());
        }
       
        tMalattia12.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio2Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_2_12.name();
      groupMalattia.description = "122 - Malattia secondo figlio <= 3 anni retribuita 100%";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child2_0_3;
      groupMalattia.takableAbsenceBehaviour = tMalattia12.get();
      groupMalattia.save();
    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_2_13.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia13 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_2_13.name());
      
      if (!tMalattia13.isPresent()) {

        tMalattia13 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia13.get().name = DefaultTakable.T_MALATTIA_FIGLIO_2_13.name();
        tMalattia13.get().amountType = AmountType.units;
        tMalattia13.get().takableCodes = Sets.newHashSet();
        tMalattia13.get().takenCodes = Sets.newHashSet();
        tMalattia13.get().fixedLimit = -1;
        tMalattia13.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("132").isPresent()) {
          tMalattia13.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("132").get());
          tMalattia13.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("132").get());
        }
       
        tMalattia13.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio2Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_2_13.name();
      groupMalattia.description = "132 - Malattia secondo figlio oltre 3 anni non retribuita";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child2_3_12;
      groupMalattia.takableAbsenceBehaviour = tMalattia13.get();
      groupMalattia.save();
    }
    

    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_2_14.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia14 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_2_14.name());
      
      if (!tMalattia14.isPresent()) {

        tMalattia14 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia14.get().name = DefaultTakable.T_MALATTIA_FIGLIO_2_14.name();
        tMalattia14.get().amountType = AmountType.units;
        tMalattia14.get().takableCodes = Sets.newHashSet();
        tMalattia14.get().takenCodes = Sets.newHashSet();
        tMalattia14.get().fixedLimit = -1;
        tMalattia14.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("142").isPresent()) {
          tMalattia14.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("142").get());
          tMalattia14.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("142").get());
        }
       
        tMalattia14.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio2Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_2_14.name();
      groupMalattia.description = "142 - Malattia secondo figlio <= 3 anni non retribuita";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child2_0_3;
      groupMalattia.takableAbsenceBehaviour = tMalattia14.get();
      groupMalattia.save();
    }
    
    
    
    
    
    
    
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_3_12.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia12 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_3_12.name());
      
      if (!tMalattia12.isPresent()) {

        tMalattia12 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia12.get().name = DefaultTakable.T_MALATTIA_FIGLIO_3_12.name();
        tMalattia12.get().amountType = AmountType.units;
        tMalattia12.get().takableCodes = Sets.newHashSet();
        tMalattia12.get().takenCodes = Sets.newHashSet();
        tMalattia12.get().fixedLimit = -1;
        tMalattia12.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("123").isPresent()) {
          tMalattia12.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("123").get());
          tMalattia12.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("123").get());
        }
       
        tMalattia12.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio3Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_3_12.name();
      groupMalattia.description = "123 - Malattia terzo figlio <= 3 anni retribuita 100%";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child3_0_3;
      groupMalattia.takableAbsenceBehaviour = tMalattia12.get();
      groupMalattia.save();
    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_3_13.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia13 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_3_13.name());
      
      if (!tMalattia13.isPresent()) {

        tMalattia13 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia13.get().name = DefaultTakable.T_MALATTIA_FIGLIO_3_13.name();
        tMalattia13.get().amountType = AmountType.units;
        tMalattia13.get().takableCodes = Sets.newHashSet();
        tMalattia13.get().takenCodes = Sets.newHashSet();
        tMalattia13.get().fixedLimit = -1;
        tMalattia13.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("133").isPresent()) {
          tMalattia13.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("133").get());
          tMalattia13.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("133").get());
        }
       
        tMalattia13.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio3Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_3_13.name();
      groupMalattia.description = "133 - Malattia terzo figlio oltre 3 anni non retribuita";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child3_3_12;
      groupMalattia.takableAbsenceBehaviour = tMalattia13.get();
      groupMalattia.save();
    }
    

    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MALATTIA_FIGLIO_3_14.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tMalattia14 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MALATTIA_FIGLIO_3_14.name());
      
      if (!tMalattia14.isPresent()) {

        tMalattia14 = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMalattia14.get().name = DefaultTakable.T_MALATTIA_FIGLIO_3_14.name();
        tMalattia14.get().amountType = AmountType.units;
        tMalattia14.get().takableCodes = Sets.newHashSet();
        tMalattia14.get().takenCodes = Sets.newHashSet();
        tMalattia14.get().fixedLimit = -1;
        tMalattia14.get().save();
        
        if (absenceComponentDao.absenceTypeByCode("143").isPresent()) {
          tMalattia14.get().takableCodes.add(absenceComponentDao.absenceTypeByCode("143").get());
          tMalattia14.get().takenCodes.add(absenceComponentDao.absenceTypeByCode("143").get());
        }
       
        tMalattia14.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupMalattia = new GroupAbsenceType();
      groupMalattia.category = malattiaFiglio3Category;
      groupMalattia.name = DefaultGroup.MALATTIA_FIGLIO_3_14.name();
      groupMalattia.description = "143 - Malattia terzo figlio <= 3 anni non retribuita";
      groupMalattia.pattern = GroupAbsenceTypePattern.programmed;
      groupMalattia.periodType = PeriodType.child3_0_3;
      groupMalattia.takableAbsenceBehaviour = tMalattia14.get();
      groupMalattia.save();
    }
    
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.PB.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tPb = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_PB.name());
      
      if (!tPb.isPresent()) {

        tPb = Optional.fromNullable(new TakableAbsenceBehaviour());
        tPb.get().name = DefaultTakable.T_PB.name();
        tPb.get().amountType = AmountType.minutes;
        tPb.get().takableCodes = Sets.newHashSet();
        tPb.get().takenCodes = Sets.newHashSet();
        tPb.get().fixedLimit = 2160;
        tPb.get().save();
        
      //Update AbsenceType
        AbsenceType cnrPb = absenceComponentDao.buildOrEditAbsenceType("PB", 
            "Permesso breve 36 ore anno", 
            0, Sets.newHashSet(specifiedMinutesLimit), null, 0, true, false, false, null, null);
       
        tPb.get().takableCodes.add(cnrPb);
        tPb.get().takenCodes.add(cnrPb);
        tPb.get().save();
      }
      
      // Group Creation
      GroupAbsenceType groupPb = new GroupAbsenceType();
      groupPb.category = malattiaFiglio3Category;
      groupPb.name = DefaultGroup.PB.name();
      groupPb.description = "PB - Permesso breve 36 ore anno";
      groupPb.pattern = GroupAbsenceTypePattern.programmed;
      groupPb.periodType = PeriodType.year;
      groupPb.takableAbsenceBehaviour = tPb.get();
      groupPb.save();
    }
    
    
    
    
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.ALTRI.name()).isPresent()) {
      
      //Takable Creation
      Optional<TakableAbsenceBehaviour> tAltri = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_ALTRI.name());
      
      if (!tAltri.isPresent()) {

        tAltri = Optional.fromNullable(new TakableAbsenceBehaviour());
        tAltri.get().name = DefaultTakable.T_ALTRI.name();
        tAltri.get().amountType = AmountType.units;
        tAltri.get().takableCodes = Sets.newHashSet();
        tAltri.get().takenCodes = Sets.newHashSet();
        tAltri.get().fixedLimit = -1;
        tAltri.get().save();
      }
      
      JPA.em().flush();

      // Group Creation
      GroupAbsenceType groupAltri = new GroupAbsenceType();
      groupAltri.category = otherCodesCategory;;
      groupAltri.name = DefaultGroup.ALTRI.name();
      groupAltri.description = "Altri Codici";
      groupAltri.pattern = GroupAbsenceTypePattern.simpleGrouping;
      groupAltri.periodType = PeriodType.always;
      groupAltri.takableAbsenceBehaviour = tAltri.get();
      groupAltri.save();
    }
    
    JPA.em().flush();
    
    Optional<TakableAbsenceBehaviour> tAltri = absenceComponentDao
        .takableAbsenceBehaviourByName(DefaultTakable.T_ALTRI.name());
    tAltri.get().takableCodes = Sets.newHashSet();
    tAltri.get().takenCodes = Sets.newHashSet();
    tAltri.get().save();
    
    JPA.em().flush();
    
    // Set boolean independente
    absenceTypes = AbsenceType.findAll();
    for (AbsenceType absenceType : absenceTypes) {
      
      if (absenceType.code.equals("90")) {
        log.info("trovato");
      }
      
      absenceType.refresh();
      
      if (absenceType.takableGroup.isEmpty() && absenceType.takenGroup.isEmpty() 
          && absenceType.complationGroup.isEmpty() && absenceType.replacingGroup.isEmpty()) {
        log.info("AbsenceCode {}", absenceType.code );
        tAltri.get().takableCodes.add(absenceType);
        tAltri.get().takenCodes.add(absenceType);
        tAltri.get().save();
      }
    }
    
    migrateAllAbsences();
    
    absenceComponentDao.renameCode("661H1C", "661H1");
    absenceComponentDao.renameCode("661H2C", "661H2");
    absenceComponentDao.renameCode("661H3C", "661H3");
    absenceComponentDao.renameCode("661H4C", "661H4");
    absenceComponentDao.renameCode("661H5C", "661H5");
    absenceComponentDao.renameCode("661H6C", "661H6");
    absenceComponentDao.renameCode("661H7C", "661H7");
    absenceComponentDao.renameCode("661H8C", "661H8");
    absenceComponentDao.renameCode("661H9C", "661H9");
    
    absenceComponentDao.renameCode("18H1C", "18H1");
    absenceComponentDao.renameCode("18H2C", "18H2");
    absenceComponentDao.renameCode("18H3C", "18H3");
    absenceComponentDao.renameCode("18H4C", "18H4");
    absenceComponentDao.renameCode("18H5C", "18H5");
    absenceComponentDao.renameCode("18H6C", "18H6");
    absenceComponentDao.renameCode("18H7C", "18H7");
    absenceComponentDao.renameCode("18H8C", "18H8");
    absenceComponentDao.renameCode("18H9C", "18H9");
    
    absenceComponentDao.renameCode("19H1C", "19H1");
    absenceComponentDao.renameCode("19H2C", "19H2");
    absenceComponentDao.renameCode("19H3C", "19H3");
    absenceComponentDao.renameCode("19H4C", "19H4");
    absenceComponentDao.renameCode("19H5C", "19H5");
    absenceComponentDao.renameCode("19H6C", "19H6");
    absenceComponentDao.renameCode("19H7C", "19H7");
    absenceComponentDao.renameCode("19H8C", "19H8");
    absenceComponentDao.renameCode("19H9C", "19H9");
    
  }
  
  


}
