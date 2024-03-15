package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Person;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPerson is a Querydsl query type for Person
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPerson extends EntityPathBase<Person> {

    private static final long serialVersionUID = -1261627527L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPerson person = new QPerson("person");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    public final ListPath<models.flows.Affiliation, models.flows.query.QAffiliation> affiliations = this.<models.flows.Affiliation, models.flows.query.QAffiliation>createList("affiliations", models.flows.Affiliation.class, models.flows.query.QAffiliation.class, PathInits.DIRECT2);

    public final SetPath<models.Badge, QBadge> badges = this.<models.Badge, QBadge>createSet("badges", models.Badge.class, QBadge.class, PathInits.DIRECT2);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final DatePath<org.joda.time.LocalDate> birthday = createDate("birthday", org.joda.time.LocalDate.class);

    public final ListPath<models.ShiftCategories, QShiftCategories> categories = this.<models.ShiftCategories, QShiftCategories>createList("categories", models.ShiftCategories.class, QShiftCategories.class, PathInits.DIRECT2);

    public final ListPath<models.CertificatedData, QCertificatedData> certificatedData = this.<models.CertificatedData, QCertificatedData>createList("certificatedData", models.CertificatedData.class, QCertificatedData.class, PathInits.DIRECT2);

    public final ListPath<models.Certification, QCertification> certifications = this.<models.Certification, QCertification>createList("certifications", models.Certification.class, QCertification.class, PathInits.DIRECT2);

    public final SetPath<models.CheckGreenPass, QCheckGreenPass> checkGreenPass = this.<models.CheckGreenPass, QCheckGreenPass>createSet("checkGreenPass", models.CheckGreenPass.class, QCheckGreenPass.class, PathInits.DIRECT2);

    public final ListPath<models.Competence, QCompetence> competences = this.<models.Competence, QCompetence>createList("competences", models.Competence.class, QCompetence.class, PathInits.DIRECT2);

    public final ListPath<models.Contract, QContract> contracts = this.<models.Contract, QContract>createList("contracts", models.Contract.class, QContract.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath eppn = createString("eppn");

    public final StringPath fax = createString("fax");

    public final StringPath fiscalCode = createString("fiscalCode");

    public final StringPath fullname = createString("fullname");

    public final ListPath<models.flows.Group, models.flows.query.QGroup> groupsPeople = this.<models.flows.Group, models.flows.query.QGroup>createList("groupsPeople", models.flows.Group.class, models.flows.query.QGroup.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final SetPath<models.absences.InitializationGroup, models.absences.query.QInitializationGroup> initializationGroups = this.<models.absences.InitializationGroup, models.absences.query.QInitializationGroup>createSet("initializationGroups", models.absences.InitializationGroup.class, models.absences.query.QInitializationGroup.class, PathInits.DIRECT2);

    public final SetPath<models.MealTicketCard, QMealTicketCard> mealTicketCards = this.<models.MealTicketCard, QMealTicketCard>createSet("mealTicketCards", models.MealTicketCard.class, QMealTicketCard.class, PathInits.DIRECT2);

    public final ListPath<models.MealTicket, QMealTicket> mealTicketsAdmin = this.<models.MealTicket, QMealTicket>createList("mealTicketsAdmin", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final StringPath mobile = createString("mobile");

    public final StringPath name = createString("name");

    public final StringPath number = createString("number");

    public final QOffice office;

    public final NumberPath<Long> oldId = createNumber("oldId", Long.class);

    public final StringPath othersSurnames = createString("othersSurnames");

    public final NumberPath<Long> perseoId = createNumber("perseoId", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.PersonChildren, QPersonChildren> personChildren = this.<models.PersonChildren, QPersonChildren>createSet("personChildren", models.PersonChildren.class, QPersonChildren.class, PathInits.DIRECT2);

    public final SetPath<models.PersonCompetenceCodes, QPersonCompetenceCodes> personCompetenceCodes = this.<models.PersonCompetenceCodes, QPersonCompetenceCodes>createSet("personCompetenceCodes", models.PersonCompetenceCodes.class, QPersonCompetenceCodes.class, PathInits.DIRECT2);

    public final ListPath<models.PersonConfiguration, QPersonConfiguration> personConfigurations = this.<models.PersonConfiguration, QPersonConfiguration>createList("personConfigurations", models.PersonConfiguration.class, QPersonConfiguration.class, PathInits.DIRECT2);

    public final ListPath<models.PersonDay, QPersonDay> personDays = this.<models.PersonDay, QPersonDay>createList("personDays", models.PersonDay.class, QPersonDay.class, PathInits.DIRECT2);

    public final QPersonHourForOvertime personHourForOvertime;

    public final ListPath<models.PersonMonthRecap, QPersonMonthRecap> personMonths = this.<models.PersonMonthRecap, QPersonMonthRecap>createList("personMonths", models.PersonMonthRecap.class, QPersonMonthRecap.class, PathInits.DIRECT2);

    public final ListPath<models.PersonShift, QPersonShift> personShifts = this.<models.PersonShift, QPersonShift>createList("personShifts", models.PersonShift.class, QPersonShift.class, PathInits.DIRECT2);

    public final ListPath<Person, QPerson> personsInCharge = this.<Person, QPerson>createList("personsInCharge", Person.class, QPerson.class, PathInits.DIRECT2);

    public final QQualification qualification;

    public final ListPath<models.PersonReperibilityType, QPersonReperibilityType> reperibilities = this.<models.PersonReperibilityType, QPersonReperibilityType>createList("reperibilities", models.PersonReperibilityType.class, QPersonReperibilityType.class, PathInits.DIRECT2);

    public final SetPath<models.PersonReperibility, QPersonReperibility> reperibility = this.<models.PersonReperibility, QPersonReperibility>createSet("reperibility", models.PersonReperibility.class, QPersonReperibility.class, PathInits.DIRECT2);

    public final ListPath<models.PersonReperibilityType, QPersonReperibilityType> reperibilityTypes = this.<models.PersonReperibilityType, QPersonReperibilityType>createList("reperibilityTypes", models.PersonReperibilityType.class, QPersonReperibilityType.class, PathInits.DIRECT2);

    public final StringPath residence = createString("residence");

    public final ListPath<models.ShiftCategories, QShiftCategories> shiftCategories = this.<models.ShiftCategories, QShiftCategories>createList("shiftCategories", models.ShiftCategories.class, QShiftCategories.class, PathInits.DIRECT2);

    public final StringPath surname = createString("surname");

    public final StringPath telephone = createString("telephone");

    public final SetPath<models.TeleworkValidation, QTeleworkValidation> teleworkValidations = this.<models.TeleworkValidation, QTeleworkValidation>createSet("teleworkValidations", models.TeleworkValidation.class, QTeleworkValidation.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final QUser user;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final BooleanPath wantEmail = createBoolean("wantEmail");

    public final ListPath<models.ZoneToZones, QZoneToZones> zones = this.<models.ZoneToZones, QZoneToZones>createList("zones", models.ZoneToZones.class, QZoneToZones.class, PathInits.DIRECT2);

    public QPerson(String variable) {
        this(Person.class, forVariable(variable), INITS);
    }

    public QPerson(Path<? extends Person> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPerson(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPerson(PathMetadata metadata, PathInits inits) {
        this(Person.class, metadata, inits);
    }

    public QPerson(Class<? extends Person> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.personHourForOvertime = inits.isInitialized("personHourForOvertime") ? new QPersonHourForOvertime(forProperty("personHourForOvertime"), inits.get("personHourForOvertime")) : null;
        this.qualification = inits.isInitialized("qualification") ? new QQualification(forProperty("qualification")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

