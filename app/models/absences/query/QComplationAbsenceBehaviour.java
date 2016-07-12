package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.ComplationAbsenceBehaviour;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QComplationAbsenceBehaviour is a Querydsl query type for ComplationAbsenceBehaviour
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QComplationAbsenceBehaviour extends EntityPathBase<ComplationAbsenceBehaviour> {

    private static final long serialVersionUID = 194414252L;

    public static final QComplationAbsenceBehaviour complationAbsenceBehaviour = new QComplationAbsenceBehaviour("complationAbsenceBehaviour");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.absences.AmountType> amountType = createEnum("amountType", models.absences.AmountType.class);

    public final SetPath<models.absences.AbsenceType, QAbsenceType> complationCodes = this.<models.absences.AbsenceType, QAbsenceType>createSet("complationCodes", models.absences.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.absences.AbsenceType, QAbsenceType> replacingCodes = this.<models.absences.AbsenceType, QAbsenceType>createSet("replacingCodes", models.absences.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    public QComplationAbsenceBehaviour(String variable) {
        super(ComplationAbsenceBehaviour.class, forVariable(variable));
    }

    public QComplationAbsenceBehaviour(Path<? extends ComplationAbsenceBehaviour> path) {
        super(path.getType(), path.getMetadata());
    }

    public QComplationAbsenceBehaviour(PathMetadata<?> metadata) {
        super(ComplationAbsenceBehaviour.class, metadata);
    }

}

