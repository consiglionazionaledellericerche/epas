package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.GroupAbsenceType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QGroupAbsenceType is a Querydsl query type for GroupAbsenceType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QGroupAbsenceType extends EntityPathBase<GroupAbsenceType> {

    private static final long serialVersionUID = 1642458572L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroupAbsenceType groupAbsenceType = new QGroupAbsenceType("groupAbsenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final BooleanPath automatic = createBoolean("automatic");

    public final QCategoryGroupAbsenceType category;

    public final StringPath chainDescription = createString("chainDescription");

    public final QComplationAbsenceBehaviour complationAbsenceBehaviour;

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath initializable = createBoolean("initializable");

    public final StringPath name = createString("name");

    public final QGroupAbsenceType nextGroupToCheck;

    public final EnumPath<GroupAbsenceType.GroupAbsenceTypePattern> pattern = createEnum("pattern", GroupAbsenceType.GroupAbsenceTypePattern.class);

    public final EnumPath<GroupAbsenceType.PeriodType> periodType = createEnum("periodType", GroupAbsenceType.PeriodType.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<GroupAbsenceType, QGroupAbsenceType> previousGroupChecked = this.<GroupAbsenceType, QGroupAbsenceType>createSet("previousGroupChecked", GroupAbsenceType.class, QGroupAbsenceType.class, PathInits.DIRECT2);

    public final QTakableAbsenceBehaviour takableAbsenceBehaviour;

    public QGroupAbsenceType(String variable) {
        this(GroupAbsenceType.class, forVariable(variable), INITS);
    }

    public QGroupAbsenceType(Path<? extends GroupAbsenceType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QGroupAbsenceType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QGroupAbsenceType(PathMetadata<?> metadata, PathInits inits) {
        this(GroupAbsenceType.class, metadata, inits);
    }

    public QGroupAbsenceType(Class<? extends GroupAbsenceType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new QCategoryGroupAbsenceType(forProperty("category"), inits.get("category")) : null;
        this.complationAbsenceBehaviour = inits.isInitialized("complationAbsenceBehaviour") ? new QComplationAbsenceBehaviour(forProperty("complationAbsenceBehaviour")) : null;
        this.nextGroupToCheck = inits.isInitialized("nextGroupToCheck") ? new QGroupAbsenceType(forProperty("nextGroupToCheck"), inits.get("nextGroupToCheck")) : null;
        this.takableAbsenceBehaviour = inits.isInitialized("takableAbsenceBehaviour") ? new QTakableAbsenceBehaviour(forProperty("takableAbsenceBehaviour")) : null;
    }

}

