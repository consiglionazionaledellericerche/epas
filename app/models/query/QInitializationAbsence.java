package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.InitializationAbsence;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;


/**
 * QInitializationAbsence is a Querydsl query type for InitializationAbsence
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QInitializationAbsence extends EntityPathBase<InitializationAbsence> {

    private static final long serialVersionUID = 30532825L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInitializationAbsence initializationAbsence = new QInitializationAbsence("initializationAbsence");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> absenceDays = createNumber("absenceDays", Integer.class);

    public final QAbsenceType absenceType;

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final NumberPath<Integer> recoveryDays = createNumber("recoveryDays", Integer.class);

    public QInitializationAbsence(String variable) {
        this(InitializationAbsence.class, forVariable(variable), INITS);
    }

    public QInitializationAbsence(Path<? extends InitializationAbsence> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QInitializationAbsence(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QInitializationAbsence(PathMetadata<?> metadata, PathInits inits) {
        this(InitializationAbsence.class, metadata, inits);
    }

    public QInitializationAbsence(Class<? extends InitializationAbsence> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absenceType = inits.isInitialized("absenceType") ? new QAbsenceType(forProperty("absenceType"), inits.get("absenceType")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

