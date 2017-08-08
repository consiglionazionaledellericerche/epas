package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Person;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPerson is a Querydsl query type for Person
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPerson extends EntityPathBase<Person> {

    private static final long serialVersionUID = -1261627527L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPerson person = new QPerson("person");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    public final StringPath badgeNumber = createString("badgeNumber");

    public final SetPath<models.Badge, QBadge> badges = this.<models.Badge, QBadge>createSet("badges", models.Badge.class, QBadge.class, PathInits.DIRECT2);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final DatePath<org.joda.time.LocalDate> birthday = createDate("birthday", org.joda.time.LocalDate.class);

    public final ListPath<models.ShiftCategories, QShiftCategories> categories = this.<models.ShiftCategories, QShiftCategories>createList("categories", models.ShiftCategories.class, QShiftCategories.class, PathInits.DIRECT2);

    public final ListPath<models.CertificatedData, QCertificatedData> certificatedData = this.<models.CertificatedData, QCertificatedData>createList("certificatedData", models.CertificatedData.class, QCertificatedData.class, PathInits.DIRECT2);

    public final ListPath<models.Certification, QCertification> certifications = this.<models.Certification, QCertification>createList("certifications", models.Certification.class, QCertification.class, PathInits.DIRECT2);

    public final ListPath<models.Competence, QCompetence> competences = this.<models.Competence, QCompetence>createList("competences", models.Competence.class, QCompetence.class, PathInits.DIRECT2);

    public final ListPath<models.Contract, QContract> contracts = this.<models.Contract, QContract>createList("contracts", models.Contract.class, QContract.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath eppn = createString("eppn");

    public final StringPath fax = createString("fax");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> iId = createNumber("iId", Integer.class);

    public final SetPath<models.absences.InitializationGroup, models.absences.query.QInitializationGroup> initializationGroups = this.<models.absences.InitializationGroup, models.absences.query.QInitializationGroup>createSet("initializationGroups", models.absences.InitializationGroup.class, models.absences.query.QInitializationGroup.class, PathInits.DIRECT2);

    public final BooleanPath isPersonInCharge = createBoolean("isPersonInCharge");

    public final ListPath<models.MealTicket, QMealTicket> mealTicketsAdmin = this.<models.MealTicket, QMealTicket>createList("mealTicketsAdmin", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final StringPath mobile = createString("mobile");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> number = createNumber("number", Integer.class);

    public final QOffice office;

    public final NumberPath<Long> oldId = createNumber("oldId", Long.class);

    public final StringPath othersSurnames = createString("othersSurnames");

    public final ListPath<Person, QPerson> people = this.<Person, QPerson>createList("people", Person.class, QPerson.class, PathInits.DIRECT2);

    public final NumberPath<Long> perseoId = createNumber("perseoId", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.PersonChildren, QPersonChildren> personChildren = this.<models.PersonChildren, QPersonChildren>createSet("personChildren", models.PersonChildren.class, QPersonChildren.class, PathInits.DIRECT2);

    public final SetPath<models.PersonCompetenceCodes, QPersonCompetenceCodes> personCompetenceCodes = this.<models.PersonCompetenceCodes, QPersonCompetenceCodes>createSet("personCompetenceCodes", models.PersonCompetenceCodes.class, QPersonCompetenceCodes.class, PathInits.DIRECT2);

    public final ListPath<models.PersonConfiguration, QPersonConfiguration> personConfigurations = this.<models.PersonConfiguration, QPersonConfiguration>createList("personConfigurations", models.PersonConfiguration.class, QPersonConfiguration.class, PathInits.DIRECT2);

    public final ListPath<models.PersonDay, QPersonDay> personDays = this.<models.PersonDay, QPersonDay>createList("personDays", models.PersonDay.class, QPersonDay.class, PathInits.DIRECT2);

    public final QPersonHourForOvertime personHourForOvertime;

    public final QPerson personInCharge;

    public final ListPath<models.PersonMonthRecap, QPersonMonthRecap> personMonths = this.<models.PersonMonthRecap, QPersonMonthRecap>createList("personMonths", models.PersonMonthRecap.class, QPersonMonthRecap.class, PathInits.DIRECT2);

    public final QPersonShift personShift;

    public final QQualification qualification;

    public final QPersonReperibility reperibility;

    public final ListPath<models.PersonReperibilityType, QPersonReperibilityType> reperibilityTypes = this.<models.PersonReperibilityType, QPersonReperibilityType>createList("reperibilityTypes", models.PersonReperibilityType.class, QPersonReperibilityType.class, PathInits.DIRECT2);

    public final ListPath<models.ShiftCategories, QShiftCategories> shiftCategories = this.<models.ShiftCategories, QShiftCategories>createList("shiftCategories", models.ShiftCategories.class, QShiftCategories.class, PathInits.DIRECT2);

    public final StringPath surname = createString("surname");

    public final StringPath telephone = createString("telephone");

    public final QUser user;

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public final BooleanPath wantEmail = createBoolean("wantEmail");

    public QPerson(String variable) {
        this(Person.class, forVariable(variable), INITS);
    }

    public QPerson(Path<? extends Person> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPerson(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPerson(PathMetadata<?> metadata, PathInits inits) {
        this(Person.class, metadata, inits);
    }

    public QPerson(Class<? extends Person> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.personHourForOvertime = inits.isInitialized("personHourForOvertime") ? new QPersonHourForOvertime(forProperty("personHourForOvertime"), inits.get("personHourForOvertime")) : null;
        this.personInCharge = inits.isInitialized("personInCharge") ? new QPerson(forProperty("personInCharge"), inits.get("personInCharge")) : null;
        this.personShift = inits.isInitialized("personShift") ? new QPersonShift(forProperty("personShift"), inits.get("personShift")) : null;
        this.qualification = inits.isInitialized("qualification") ? new QQualification(forProperty("qualification")) : null;
        this.reperibility = inits.isInitialized("reperibility") ? new QPersonReperibility(forProperty("reperibility"), inits.get("reperibility")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

