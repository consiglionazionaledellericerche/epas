package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.AbsenceType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAbsenceType is a Querydsl query type for AbsenceType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAbsenceType extends EntityPathBase<AbsenceType> {

    private static final long serialVersionUID = -2064225309L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceType absenceType = new QAbsenceType("absenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final SetPath<models.absences.Absence, QAbsence> absences = this.<models.absences.Absence, QAbsence>createSet("absences", models.absences.Absence.class, QAbsence.class, PathInits.DIRECT2);

    public final StringPath certificateCode = createString("certificateCode");

    public final StringPath code = createString("code");

    public final SetPath<models.absences.ComplationAbsenceBehaviour, QComplationAbsenceBehaviour> complationGroup = this.<models.absences.ComplationAbsenceBehaviour, QComplationAbsenceBehaviour>createSet("complationGroup", models.absences.ComplationAbsenceBehaviour.class, QComplationAbsenceBehaviour.class, PathInits.DIRECT2);

    public final BooleanPath consideredWeekEnd = createBoolean("consideredWeekEnd");

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath internalUse = createBoolean("internalUse");

    public final NumberPath<Integer> justifiedTime = createNumber("justifiedTime", Integer.class);

    public final SetPath<models.absences.JustifiedType, QJustifiedType> justifiedTypesPermitted = this.<models.absences.JustifiedType, QJustifiedType>createSet("justifiedTypesPermitted", models.absences.JustifiedType.class, QJustifiedType.class, PathInits.DIRECT2);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Qualification, models.query.QQualification> qualifications = this.<models.Qualification, models.query.QQualification>createList("qualifications", models.Qualification.class, models.query.QQualification.class, PathInits.DIRECT2);

    public final SetPath<models.absences.ComplationAbsenceBehaviour, QComplationAbsenceBehaviour> replacingGroup = this.<models.absences.ComplationAbsenceBehaviour, QComplationAbsenceBehaviour>createSet("replacingGroup", models.absences.ComplationAbsenceBehaviour.class, QComplationAbsenceBehaviour.class, PathInits.DIRECT2);

    public final NumberPath<Integer> replacingTime = createNumber("replacingTime", Integer.class);

    public final QJustifiedType replacingType;

    public final SetPath<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour> takableGroup = this.<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour>createSet("takableGroup", models.absences.TakableAbsenceBehaviour.class, QTakableAbsenceBehaviour.class, PathInits.DIRECT2);

    public final SetPath<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour> takenGroup = this.<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour>createSet("takenGroup", models.absences.TakableAbsenceBehaviour.class, QTakableAbsenceBehaviour.class, PathInits.DIRECT2);

    public final BooleanPath timeForMealTicket = createBoolean("timeForMealTicket");

    public final DatePath<org.joda.time.LocalDate> validFrom = createDate("validFrom", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> validTo = createDate("validTo", org.joda.time.LocalDate.class);

    public QAbsenceType(String variable) {
        this(AbsenceType.class, forVariable(variable), INITS);
    }

    public QAbsenceType(Path<? extends AbsenceType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceType(PathMetadata<?> metadata, PathInits inits) {
        this(AbsenceType.class, metadata, inits);
    }

    public QAbsenceType(Class<? extends AbsenceType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.replacingType = inits.isInitialized("replacingType") ? new QJustifiedType(forProperty("replacingType")) : null;
    }

}

