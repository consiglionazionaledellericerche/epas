package models.absences.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.absences.AbsenceTypeJustifiedBehaviour;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAbsenceTypeJustifiedBehaviour is a Querydsl query type for AbsenceTypeJustifiedBehaviour
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QAbsenceTypeJustifiedBehaviour extends EntityPathBase<AbsenceTypeJustifiedBehaviour> {

    private static final long serialVersionUID = 575965601L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceTypeJustifiedBehaviour absenceTypeJustifiedBehaviour = new QAbsenceTypeJustifiedBehaviour("absenceTypeJustifiedBehaviour");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QAbsenceType absenceType;

    public final NumberPath<Integer> data = createNumber("data", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QJustifiedBehaviour justifiedBehaviour;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QAbsenceTypeJustifiedBehaviour(String variable) {
        this(AbsenceTypeJustifiedBehaviour.class, forVariable(variable), INITS);
    }

    public QAbsenceTypeJustifiedBehaviour(Path<? extends AbsenceTypeJustifiedBehaviour> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAbsenceTypeJustifiedBehaviour(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAbsenceTypeJustifiedBehaviour(PathMetadata metadata, PathInits inits) {
        this(AbsenceTypeJustifiedBehaviour.class, metadata, inits);
    }

    public QAbsenceTypeJustifiedBehaviour(Class<? extends AbsenceTypeJustifiedBehaviour> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absenceType = inits.isInitialized("absenceType") ? new QAbsenceType(forProperty("absenceType"), inits.get("absenceType")) : null;
        this.justifiedBehaviour = inits.isInitialized("justifiedBehaviour") ? new QJustifiedBehaviour(forProperty("justifiedBehaviour")) : null;
    }

}

