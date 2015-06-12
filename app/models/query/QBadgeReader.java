package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.BadgeReader;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;


/**
 * QBadgeReader is a Querydsl query type for BadgeReader
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QBadgeReader extends EntityPathBase<BadgeReader> {

    private static final long serialVersionUID = 642176162L;

    public static final QBadgeReader badgeReader = new QBadgeReader("badgeReader");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath code = createString("code");

    public final StringPath description = createString("description");

    public final BooleanPath enabled = createBoolean("enabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath location = createString("location");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Stamping, QStamping> stampings = this.<models.Stamping, QStamping>createList("stampings", models.Stamping.class, QStamping.class, PathInits.DIRECT2);

    public QBadgeReader(String variable) {
        super(BadgeReader.class, forVariable(variable));
    }

    public QBadgeReader(Path<? extends BadgeReader> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBadgeReader(PathMetadata<?> metadata) {
        super(BadgeReader.class, metadata);
    }

}

