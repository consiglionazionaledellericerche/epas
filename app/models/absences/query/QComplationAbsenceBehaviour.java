package models.absences.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.absences.ComplationAbsenceBehaviour;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QComplationAbsenceBehaviour is a Querydsl query type for ComplationAbsenceBehaviour
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QComplationAbsenceBehaviour extends EntityPathBase<ComplationAbsenceBehaviour> {

    private static final long serialVersionUID = 194414252L;

    public static final QComplationAbsenceBehaviour complationAbsenceBehaviour = new QComplationAbsenceBehaviour("complationAbsenceBehaviour");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.absences.AmountType> amountType = createEnum("amountType", models.absences.AmountType.class);

    public final SetPath<models.absences.AbsenceType, QAbsenceType> complationCodes = this.<models.absences.AbsenceType, QAbsenceType>createSet("complationCodes", models.absences.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final SetPath<models.absences.GroupAbsenceType, QGroupAbsenceType> groupAbsenceTypes = this.<models.absences.GroupAbsenceType, QGroupAbsenceType>createSet("groupAbsenceTypes", models.absences.GroupAbsenceType.class, QGroupAbsenceType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.absences.AbsenceType, QAbsenceType> replacingCodes = this.<models.absences.AbsenceType, QAbsenceType>createSet("replacingCodes", models.absences.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QComplationAbsenceBehaviour(String variable) {
        super(ComplationAbsenceBehaviour.class, forVariable(variable));
    }

    public QComplationAbsenceBehaviour(Path<? extends ComplationAbsenceBehaviour> path) {
        super(path.getType(), path.getMetadata());
    }

    public QComplationAbsenceBehaviour(PathMetadata metadata) {
        super(ComplationAbsenceBehaviour.class, metadata);
    }

}

