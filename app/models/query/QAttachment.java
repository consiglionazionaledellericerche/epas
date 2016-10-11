package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Attachment;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAttachment is a Querydsl query type for Attachment
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAttachment extends EntityPathBase<Attachment> {

    private static final long serialVersionUID = 868098887L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAttachment attachment = new QAttachment("attachment");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final SimplePath<play.db.jpa.Blob> file = createSimple("file", play.db.jpa.Blob.class);

    public final StringPath filename = createString("filename");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final EnumPath<models.enumerate.AttachmentType> type = createEnum("type", models.enumerate.AttachmentType.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QAttachment(String variable) {
        this(Attachment.class, forVariable(variable), INITS);
    }

    public QAttachment(Path<? extends Attachment> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAttachment(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAttachment(PathMetadata<?> metadata, PathInits inits) {
        this(Attachment.class, metadata, inits);
    }

    public QAttachment(Class<? extends Attachment> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

