package models.absences.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.absences.CategoryTab;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCategoryTab is a Querydsl query type for CategoryTab
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QCategoryTab(String variable) {
        super(CategoryTab.class, forVariable(variable));
    }

    public QCategoryTab(Path<? extends CategoryTab> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCategoryTab(PathMetadata metadata) {
        super(CategoryTab.class, metadata);
    }

}

