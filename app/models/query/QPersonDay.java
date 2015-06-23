package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonDay;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonDay is a Querydsl query type for PersonDay
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonDay extends EntityPathBase<PersonDay> {

    private static final long serialVersionUID = 113218915L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonDay personDay = new QPersonDay("personDay");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.Absence, QAbsence> absences = this.<models.Absence, QAbsence>createList("absences", models.Absence.class, QAbsence.class, PathInits.DIRECT2);

    public final BooleanPath acceptedHolidayWorkingTime = createBoolean("acceptedHolidayWorkingTime");

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> difference = createNumber("difference", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isHoliday = createBoolean("isHoliday");

    public final BooleanPath isTicketAvailable = createBoolean("isTicketAvailable");

    public final BooleanPath isTicketForcedByAdmin = createBoolean("isTicketForcedByAdmin");

    public final BooleanPath isWorkingInAnotherPlace = createBoolean("isWorkingInAnotherPlace");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final NumberPath<Integer> progressive = createNumber("progressive", Integer.class);

    public final ListPath<models.Stamping, QStamping> stampings = this.<models.Stamping, QStamping>createList("stampings", models.Stamping.class, QStamping.class, PathInits.DIRECT2);

    public final QStampModificationType stampModificationType;

    public final NumberPath<Integer> timeAtWork = createNumber("timeAtWork", Integer.class);

    public final ListPath<models.PersonDayInTrouble, QPersonDayInTrouble> troubles = this.<models.PersonDayInTrouble, QPersonDayInTrouble>createList("troubles", models.PersonDayInTrouble.class, QPersonDayInTrouble.class, PathInits.DIRECT2);

    public QPersonDay(String variable) {
        this(PersonDay.class, forVariable(variable), INITS);
    }

    public QPersonDay(Path<? extends PersonDay> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonDay(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonDay(PathMetadata<?> metadata, PathInits inits) {
        this(PersonDay.class, metadata, inits);
    }

    public QPersonDay(Class<? extends PersonDay> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
        this.stampModificationType = inits.isInitialized("stampModificationType") ? new QStampModificationType(forProperty("stampModificationType")) : null;
    }

}

