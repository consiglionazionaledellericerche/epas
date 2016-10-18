package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.InitializationGroup;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QInitializationGroup is a Querydsl query type for InitializationGroup
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QInitializationGroup extends EntityPathBase<InitializationGroup> {

    private static final long serialVersionUID = -656690549L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInitializationGroup initializationGroup = new QInitializationGroup("initializationGroup");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> complationUsed = createNumber("complationUsed", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> forcedBegin = createDate("forcedBegin", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> forcedEnd = createDate("forcedEnd", org.joda.time.LocalDate.class);

    public final QGroupAbsenceType groupAbsenceType;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DatePath<org.joda.time.LocalDate> initializationDate = createDate("initializationDate", org.joda.time.LocalDate.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final models.query.QPerson person;

    public final NumberPath<Integer> residualMinutesCurrentYear = createNumber("residualMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> residualMinutesLastYear = createNumber("residualMinutesLastYear", Integer.class);

    public final NumberPath<Integer> takableTotal = createNumber("takableTotal", Integer.class);

    public final NumberPath<Integer> takableUsed = createNumber("takableUsed", Integer.class);

    public final NumberPath<Integer> vacationYear = createNumber("vacationYear", Integer.class);

    public QInitializationGroup(String variable) {
        this(InitializationGroup.class, forVariable(variable), INITS);
    }

    public QInitializationGroup(Path<? extends InitializationGroup> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QInitializationGroup(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QInitializationGroup(PathMetadata<?> metadata, PathInits inits) {
        this(InitializationGroup.class, metadata, inits);
    }

    public QInitializationGroup(Class<? extends InitializationGroup> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.groupAbsenceType = inits.isInitialized("groupAbsenceType") ? new QGroupAbsenceType(forProperty("groupAbsenceType"), inits.get("groupAbsenceType")) : null;
        this.person = inits.isInitialized("person") ? new models.query.QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

