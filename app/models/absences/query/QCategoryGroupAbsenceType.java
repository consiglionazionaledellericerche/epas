package models.absences.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.absences.CategoryGroupAbsenceType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCategoryGroupAbsenceType is a Querydsl query type for CategoryGroupAbsenceType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCategoryGroupAbsenceType extends EntityPathBase<CategoryGroupAbsenceType> {

    private static final long serialVersionUID = 107691754L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCategoryGroupAbsenceType categoryGroupAbsenceType = new QCategoryGroupAbsenceType("categoryGroupAbsenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final models.contractual.query.QContractualClause contractualClause;

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

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QCategoryGroupAbsenceType(String variable) {
        this(CategoryGroupAbsenceType.class, forVariable(variable), INITS);
    }

    public QCategoryGroupAbsenceType(Path<? extends CategoryGroupAbsenceType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCategoryGroupAbsenceType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCategoryGroupAbsenceType(PathMetadata metadata, PathInits inits) {
        this(CategoryGroupAbsenceType.class, metadata, inits);
    }

    public QCategoryGroupAbsenceType(Class<? extends CategoryGroupAbsenceType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contractualClause = inits.isInitialized("contractualClause") ? new models.contractual.query.QContractualClause(forProperty("contractualClause")) : null;
        this.tab = inits.isInitialized("tab") ? new QCategoryTab(forProperty("tab")) : null;
    }

}

