package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SetPath;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

import models.BadgeSystem;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;


/**
 * QBadgeSystem is a Querydsl query type for BadgeSystem
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QBadgeSystem extends EntityPathBase<BadgeSystem> {

  private static final long serialVersionUID = 689827342L;

  private static final PathInits INITS = PathInits.DIRECT2;

  public static final QBadgeSystem badgeSystem = new QBadgeSystem("badgeSystem");

  public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

  public final ListPath<models.BadgeReader, QBadgeReader> badgeReaders = this.<models.BadgeReader, QBadgeReader>createList("badgeReaders", models.BadgeReader.class, QBadgeReader.class, PathInits.DIRECT2);

  public final SetPath<models.Badge, QBadge> badges = this.<models.Badge, QBadge>createSet("badges", models.Badge.class, QBadge.class, PathInits.DIRECT2);

  public final StringPath description = createString("description");

  public final BooleanPath enabled = createBoolean("enabled");

  //inherited
  public final SimplePath<Object> entityId = _super.entityId;

  //inherited
  public final NumberPath<Long> id = _super.id;

  public final StringPath name = createString("name");

  public final QOffice office;

  //inherited
  public final BooleanPath persistent = _super.persistent;

  public QBadgeSystem(String variable) {
    this(BadgeSystem.class, forVariable(variable), INITS);
  }

  public QBadgeSystem(Path<? extends BadgeSystem> path) {
    this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
  }

  public QBadgeSystem(PathMetadata<?> metadata) {
    this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
  }

  public QBadgeSystem(PathMetadata<?> metadata, PathInits inits) {
    this(BadgeSystem.class, metadata, inits);
  }

  public QBadgeSystem(Class<? extends BadgeSystem> type, PathMetadata<?> metadata, PathInits inits) {
    super(type, metadata, inits);
    this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
  }

}

