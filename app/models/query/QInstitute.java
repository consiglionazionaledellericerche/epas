package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Institute;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInstitute is a Querydsl query type for Institute
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QInstitute extends EntityPathBase<Institute> {

    private static final long serialVersionUID = -703259791L;

    public static final QInstitute institute = new QInstitute("institute");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final StringPath cds = createString("cds");

    public final StringPath code = createString("code");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    public final NumberPath<Long> perseoId = createNumber("perseoId", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.Office, QOffice> seats = this.<models.Office, QOffice>createSet("seats", models.Office.class, QOffice.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QInstitute(String variable) {
        super(Institute.class, forVariable(variable));
    }

    public QInstitute(Path<? extends Institute> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInstitute(PathMetadata metadata) {
        super(Institute.class, metadata);
    }

}

