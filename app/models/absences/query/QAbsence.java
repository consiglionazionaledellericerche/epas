package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.Absence;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAbsence is a Querydsl query type for Absence
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAbsence extends EntityPathBase<Absence> {

    private static final long serialVersionUID = -580774647L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsence absence = new QAbsence("absence");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final SimplePath<play.db.jpa.Blob> absenceFile = createSimple("absenceFile", play.db.jpa.Blob.class);

    public final QAbsenceType absenceType;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> justifiedMinutes = createNumber("justifiedMinutes", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final models.query.QPersonDay personDay;

    public QAbsence(String variable) {
        this(Absence.class, forVariable(variable), INITS);
    }

    public QAbsence(Path<? extends Absence> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsence(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsence(PathMetadata<?> metadata, PathInits inits) {
        this(Absence.class, metadata, inits);
    }

    public QAbsence(Class<? extends Absence> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absenceType = inits.isInitialized("absenceType") ? new QAbsenceType(forProperty("absenceType"), inits.get("absenceType")) : null;
        this.personDay = inits.isInitialized("personDay") ? new models.query.QPersonDay(forProperty("personDay"), inits.get("personDay")) : null;
    }

}

