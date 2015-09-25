package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Institute;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QInstitute is a Querydsl query type for Institute
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QInstitute extends EntityPathBase<Institute> {

    private static final long serialVersionUID = -703259791L;

    public static final QInstitute institute = new QInstitute("institute");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final StringPath code = createString("code");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.Office, QOffice> seats = this.<models.Office, QOffice>createSet("seats", models.Office.class, QOffice.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QInstitute(String variable) {
        super(Institute.class, forVariable(variable));
    }

    public QInstitute(Path<? extends Institute> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInstitute(PathMetadata<?> metadata) {
        super(Institute.class, metadata);
    }

}

