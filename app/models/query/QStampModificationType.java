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
import models.StampModificationType;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QStampModificationType is a Querydsl query type for StampModificationType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QStampModificationType extends EntityPathBase<StampModificationType> {

    private static final long serialVersionUID = 928972853L;

    public static final QStampModificationType stampModificationType = new QStampModificationType("stampModificationType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath code = createString("code");

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonDay, QPersonDay> personDays = this.<models.PersonDay, QPersonDay>createList("personDays", models.PersonDay.class, QPersonDay.class, PathInits.DIRECT2);

    public final SetPath<models.Stamping, QStamping> stampings = this.<models.Stamping, QStamping>createSet("stampings", models.Stamping.class, QStamping.class, PathInits.DIRECT2);

    public QStampModificationType(String variable) {
        super(StampModificationType.class, forVariable(variable));
    }

    public QStampModificationType(Path<? extends StampModificationType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStampModificationType(PathMetadata<?> metadata) {
        super(StampModificationType.class, metadata);
    }

}

