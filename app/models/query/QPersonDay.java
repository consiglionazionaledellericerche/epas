package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonDay;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonDay is a Querydsl query type for PersonDay
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonDay extends EntityPathBase<PersonDay> {

    private static final long serialVersionUID = 113218915L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonDay personDay = new QPersonDay("personDay");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.absences.Absence, models.absences.query.QAbsence> absences = this.<models.absences.Absence, models.absences.query.QAbsence>createList("absences", models.absences.Absence.class, models.absences.query.QAbsence.class, PathInits.DIRECT2);

    public final NumberPath<Integer> approvedOnHoliday = createNumber("approvedOnHoliday", Integer.class);

    public final NumberPath<Integer> approvedOutOpening = createNumber("approvedOutOpening", Integer.class);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> decurtedMeal = createNumber("decurtedMeal", Integer.class);

    public final NumberPath<Integer> difference = createNumber("difference", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath future = createBoolean("future");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath ignoreShortLeave = createBoolean("ignoreShortLeave");

    public final BooleanPath isHoliday = createBoolean("isHoliday");

    public final BooleanPath isTicketAvailable = createBoolean("isTicketAvailable");

    public final BooleanPath isTicketForcedByAdmin = createBoolean("isTicketForcedByAdmin");

    public final BooleanPath isWorkingInAnotherPlace = createBoolean("isWorkingInAnotherPlace");

    public final NumberPath<Integer> justifiedTimeBetweenZones = createNumber("justifiedTimeBetweenZones", Integer.class);

    public final NumberPath<Integer> justifiedTimeMeal = createNumber("justifiedTimeMeal", Integer.class);

    public final NumberPath<Integer> justifiedTimeNoMeal = createNumber("justifiedTimeNoMeal", Integer.class);

    public final StringPath note = createString("note");

    public final NumberPath<Integer> onHoliday = createNumber("onHoliday", Integer.class);

    public final NumberPath<Integer> outOpening = createNumber("outOpening", Integer.class);

    public final BooleanPath past = createBoolean("past");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final NumberPath<Integer> progressive = createNumber("progressive", Integer.class);

    public final ListPath<models.Stamping, QStamping> stampings = this.<models.Stamping, QStamping>createList("stampings", models.Stamping.class, QStamping.class, PathInits.DIRECT2);

    public final NumberPath<Integer> stampingsTime = createNumber("stampingsTime", Integer.class);

    public final QStampModificationType stampModificationType;

    public final NumberPath<Integer> timeAtWork = createNumber("timeAtWork", Integer.class);

    public final BooleanPath today = createBoolean("today");

    public final ListPath<models.PersonDayInTrouble, QPersonDayInTrouble> troubles = this.<models.PersonDayInTrouble, QPersonDayInTrouble>createList("troubles", models.PersonDayInTrouble.class, QPersonDayInTrouble.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> workingTimeInMission = createNumber("workingTimeInMission", Integer.class);

    public QPersonDay(String variable) {
        this(PersonDay.class, forVariable(variable), INITS);
    }

    public QPersonDay(Path<? extends PersonDay> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonDay(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonDay(PathMetadata metadata, PathInits inits) {
        this(PersonDay.class, metadata, inits);
    }

    public QPersonDay(Class<? extends PersonDay> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
        this.stampModificationType = inits.isInitialized("stampModificationType") ? new QStampModificationType(forProperty("stampModificationType")) : null;
    }

}

