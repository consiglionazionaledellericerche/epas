package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.CategoryGroupAbsenceType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QCategoryGroupAbsenceType is a Querydsl query type for CategoryGroupAbsenceType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCategoryGroupAbsenceType extends EntityPathBase<CategoryGroupAbsenceType> {

    private static final long serialVersionUID = 107691754L;

    public static final QCategoryGroupAbsenceType categoryGroupAbsenceType = new QCategoryGroupAbsenceType("categoryGroupAbsenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> priority = createNumber("priority", Integer.class);

    public QCategoryGroupAbsenceType(String variable) {
        super(CategoryGroupAbsenceType.class, forVariable(variable));
    }

    public QCategoryGroupAbsenceType(Path<? extends CategoryGroupAbsenceType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCategoryGroupAbsenceType(PathMetadata<?> metadata) {
        super(CategoryGroupAbsenceType.class, metadata);
    }

}

