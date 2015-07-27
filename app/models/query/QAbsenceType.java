package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.AbsenceType;


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

    private static final long serialVersionUID = 1593874803L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceType absenceType = new QAbsenceType("absenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final SetPath<models.Absence, QAbsence> absences = this.<models.Absence, QAbsence>createSet("absences", models.Absence.class, QAbsence.class, PathInits.DIRECT2);

    public final QAbsenceTypeGroup absenceTypeGroup;

    public final StringPath certificateCode = createString("certificateCode");

    public final StringPath code = createString("code");

    public final BooleanPath consideredWeekEnd = createBoolean("consideredWeekEnd");

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath internalUse = createBoolean("internalUse");

    public final EnumPath<models.enumerate.JustifiedTimeAtWork> justifiedTimeAtWork = createEnum("justifiedTimeAtWork", models.enumerate.JustifiedTimeAtWork.class);

    public final BooleanPath mealTicketCalculation = createBoolean("mealTicketCalculation");

    public final BooleanPath multipleUse = createBoolean("multipleUse");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Qualification, QQualification> qualifications = this.<models.Qualification, QQualification>createList("qualifications", models.Qualification.class, QQualification.class, PathInits.DIRECT2);

    public final BooleanPath replacingAbsence = createBoolean("replacingAbsence");

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
        this.absenceTypeGroup = inits.isInitialized("absenceTypeGroup") ? new QAbsenceTypeGroup(forProperty("absenceTypeGroup"), inits.get("absenceTypeGroup")) : null;
    }

}

