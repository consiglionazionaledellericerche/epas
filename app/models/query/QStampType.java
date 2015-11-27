package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SetPath;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

import models.StampType;


/**
 * QStampType is a Querydsl query type for StampType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QStampType extends EntityPathBase<StampType> {

    private static final long serialVersionUID = 1278906105L;

    public static final QStampType stampType = new QStampType("stampType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath code = createString("code");

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath identifier = createString("identifier");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.Stamping, QStamping> stampings = this.<models.Stamping, QStamping>createSet("stampings", models.Stamping.class, QStamping.class, PathInits.DIRECT2);

    public QStampType(String variable) {
        super(StampType.class, forVariable(variable));
    }

    public QStampType(Path<? extends StampType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStampType(PathMetadata<?> metadata) {
        super(StampType.class, metadata);
    }

}

