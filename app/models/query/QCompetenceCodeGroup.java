package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.CompetenceCodeGroup;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QCompetenceCodeGroup is a Querydsl query type for CompetenceCodeGroup
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCompetenceCodeGroup extends EntityPathBase<CompetenceCodeGroup> {

    private static final long serialVersionUID = 1280154917L;

    public static final QCompetenceCodeGroup competenceCodeGroup = new QCompetenceCodeGroup("competenceCodeGroup");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.CompetenceCode, QCompetenceCode> competenceCodes = this.<models.CompetenceCode, QCompetenceCode>createList("competenceCodes", models.CompetenceCode.class, QCompetenceCode.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath label = createString("label");

    public final EnumPath<models.enumerate.LimitType> limitType = createEnum("limitType", models.enumerate.LimitType.class);

    public final EnumPath<models.enumerate.LimitUnit> limitUnit = createEnum("limitUnit", models.enumerate.LimitUnit.class);

    public final NumberPath<Integer> limitValue = createNumber("limitValue", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QCompetenceCodeGroup(String variable) {
        super(CompetenceCodeGroup.class, forVariable(variable));
    }

    public QCompetenceCodeGroup(Path<? extends CompetenceCodeGroup> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCompetenceCodeGroup(PathMetadata<?> metadata) {
        super(CompetenceCodeGroup.class, metadata);
    }

}

