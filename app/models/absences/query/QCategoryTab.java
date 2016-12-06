package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.CategoryTab;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QCategoryTab is a Querydsl query type for CategoryTab
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCategoryTab extends EntityPathBase<CategoryTab> {

    private static final long serialVersionUID = -240071037L;

    public static final QCategoryTab categoryTab = new QCategoryTab("categoryTab");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final SetPath<models.absences.CategoryGroupAbsenceType, QCategoryGroupAbsenceType> categoryGroupAbsenceTypes = this.<models.absences.CategoryGroupAbsenceType, QCategoryGroupAbsenceType>createSet("categoryGroupAbsenceTypes", models.absences.CategoryGroupAbsenceType.class, QCategoryGroupAbsenceType.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isDefault = createBoolean("isDefault");

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> priority = createNumber("priority", Integer.class);

    public QCategoryTab(String variable) {
        super(CategoryTab.class, forVariable(variable));
    }

    public QCategoryTab(Path<? extends CategoryTab> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCategoryTab(PathMetadata<?> metadata) {
        super(CategoryTab.class, metadata);
    }

}

