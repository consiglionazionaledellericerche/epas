package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.CategoryGroupAbsenceType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QCategoryGroupAbsenceType is a Querydsl query type for CategoryGroupAbsenceType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCategoryGroupAbsenceType extends EntityPathBase<CategoryGroupAbsenceType> {

    private static final long serialVersionUID = 107691754L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCategoryGroupAbsenceType categoryGroupAbsenceType = new QCategoryGroupAbsenceType("categoryGroupAbsenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final SetPath<models.absences.GroupAbsenceType, QGroupAbsenceType> groupAbsenceTypes = this.<models.absences.GroupAbsenceType, QGroupAbsenceType>createSet("groupAbsenceTypes", models.absences.GroupAbsenceType.class, QGroupAbsenceType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> priority = createNumber("priority", Integer.class);

    public final QCategoryTab tab;

    public QCategoryGroupAbsenceType(String variable) {
        this(CategoryGroupAbsenceType.class, forVariable(variable), INITS);
    }

    public QCategoryGroupAbsenceType(Path<? extends CategoryGroupAbsenceType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCategoryGroupAbsenceType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCategoryGroupAbsenceType(PathMetadata<?> metadata, PathInits inits) {
        this(CategoryGroupAbsenceType.class, metadata, inits);
    }

    public QCategoryGroupAbsenceType(Class<? extends CategoryGroupAbsenceType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.tab = inits.isInitialized("tab") ? new QCategoryTab(forProperty("tab")) : null;
    }

}

