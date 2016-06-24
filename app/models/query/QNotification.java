package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Notification;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QNotification is a Querydsl query type for Notification
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QNotification extends EntityPathBase<Notification> {

    private static final long serialVersionUID = -1246275057L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotification notification = new QNotification("notification");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    public final QUser destination;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath message = createString("message");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath read = createBoolean("read");

    public final EnumPath<models.enumerate.NotificationSubject> subject = createEnum("subject", models.enumerate.NotificationSubject.class);

    public final NumberPath<Integer> subjectId = createNumber("subjectId", Integer.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotification(String variable) {
        this(Notification.class, forVariable(variable), INITS);
    }

    public QNotification(Path<? extends Notification> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QNotification(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QNotification(PathMetadata<?> metadata, PathInits inits) {
        this(Notification.class, metadata, inits);
    }

    public QNotification(Class<? extends Notification> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.destination = inits.isInitialized("recipient") ? new QUser(forProperty("recipient"), inits.get("recipient")) : null;
    }

}

