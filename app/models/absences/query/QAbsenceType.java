package models.absences.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.absences.AbsenceType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAbsenceType is a Querydsl query type for AbsenceType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    public final StringPath documentation = createString("documentation");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath internalUse = createBoolean("internalUse");

    public final BooleanPath isRealAbsence = createBoolean("isRealAbsence");

    public final SetPath<models.absences.AbsenceTypeJustifiedBehaviour, QAbsenceTypeJustifiedBehaviour> justifiedBehaviours = this.<models.absences.AbsenceTypeJustifiedBehaviour, QAbsenceTypeJustifiedBehaviour>createSet("justifiedBehaviours", models.absences.AbsenceTypeJustifiedBehaviour.class, QAbsenceTypeJustifiedBehaviour.class, PathInits.DIRECT2);

    public final NumberPath<Integer> justifiedTime = createNumber("justifiedTime", Integer.class);

    public final SetPath<models.absences.JustifiedType, QJustifiedType> justifiedTypesPermitted = this.<models.absences.JustifiedType, QJustifiedType>createSet("justifiedTypesPermitted", models.absences.JustifiedType.class, QJustifiedType.class, PathInits.DIRECT2);

    public final EnumPath<models.enumerate.MealTicketBehaviour> mealTicketBehaviour = createEnum("mealTicketBehaviour", models.enumerate.MealTicketBehaviour.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Qualification, models.query.QQualification> qualifications = this.<models.Qualification, models.query.QQualification>createList("qualifications", models.Qualification.class, models.query.QQualification.class, PathInits.DIRECT2);

    public final BooleanPath reperibilityCompatible = createBoolean("reperibilityCompatible");

    public final SetPath<models.absences.ComplationAbsenceBehaviour, QComplationAbsenceBehaviour> replacingGroup = this.<models.absences.ComplationAbsenceBehaviour, QComplationAbsenceBehaviour>createSet("replacingGroup", models.absences.ComplationAbsenceBehaviour.class, QComplationAbsenceBehaviour.class, PathInits.DIRECT2);

    public final NumberPath<Integer> replacingTime = createNumber("replacingTime", Integer.class);

    public final QJustifiedType replacingType;

    public final SetPath<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour> takableGroup = this.<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour>createSet("takableGroup", models.absences.TakableAbsenceBehaviour.class, QTakableAbsenceBehaviour.class, PathInits.DIRECT2);

    public final SetPath<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour> takenGroup = this.<models.absences.TakableAbsenceBehaviour, QTakableAbsenceBehaviour>createSet("takenGroup", models.absences.TakableAbsenceBehaviour.class, QTakableAbsenceBehaviour.class, PathInits.DIRECT2);

    public final BooleanPath toUpdate = createBoolean("toUpdate");

    public final DatePath<org.joda.time.LocalDate> validFrom = createDate("validFrom", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> validTo = createDate("validTo", org.joda.time.LocalDate.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QAbsenceType(String variable) {
        this(AbsenceType.class, forVariable(variable), INITS);
    }

    public QAbsenceType(Path<? extends AbsenceType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAbsenceType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAbsenceType(PathMetadata metadata, PathInits inits) {
        this(AbsenceType.class, metadata, inits);
    }

    public QAbsenceType(Class<? extends AbsenceType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.replacingType = inits.isInitialized("replacingType") ? new QJustifiedType(forProperty("replacingType")) : null;
    }

}

