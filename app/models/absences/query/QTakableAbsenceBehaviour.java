package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.TakableAbsenceBehaviour;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QTakableAbsenceBehaviour is a Querydsl query type for TakableAbsenceBehaviour
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QTakableAbsenceBehaviour extends EntityPathBase<TakableAbsenceBehaviour> {

    private static final long serialVersionUID = -1159491612L;

    public static final QTakableAbsenceBehaviour takableAbsenceBehaviour = new QTakableAbsenceBehaviour("takableAbsenceBehaviour");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.absences.AmountType> amountType = createEnum("amountType", models.absences.AmountType.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Integer> fixedLimit = createNumber("fixedLimit", Integer.class);

    public final SetPath<models.absences.GroupAbsenceType, QGroupAbsenceType> groupAbsenceTypes = this.<models.absences.GroupAbsenceType, QGroupAbsenceType>createSet("groupAbsenceTypes", models.absences.GroupAbsenceType.class, QGroupAbsenceType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final EnumPath<TakableAbsenceBehaviour.TakeAmountAdjustment> takableAmountAdjustment = createEnum("takableAmountAdjustment", TakableAbsenceBehaviour.TakeAmountAdjustment.class);

    public final SetPath<models.absences.AbsenceType, QAbsenceType> takableCodes = this.<models.absences.AbsenceType, QAbsenceType>createSet("takableCodes", models.absences.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    public final SetPath<models.absences.AbsenceType, QAbsenceType> takenCodes = this.<models.absences.AbsenceType, QAbsenceType>createSet("takenCodes", models.absences.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    public QTakableAbsenceBehaviour(String variable) {
        super(TakableAbsenceBehaviour.class, forVariable(variable));
    }

    public QTakableAbsenceBehaviour(Path<? extends TakableAbsenceBehaviour> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTakableAbsenceBehaviour(PathMetadata<?> metadata) {
        super(TakableAbsenceBehaviour.class, metadata);
    }

}

