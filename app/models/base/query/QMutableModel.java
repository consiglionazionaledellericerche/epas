package models.base.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.base.MutableModel;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QMutableModel is a Querydsl query type for MutableModel
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
public class QMutableModel extends EntityPathBase<MutableModel> {

    private static final long serialVersionUID = 469300540L;

    public static final QMutableModel mutableModel = new QMutableModel("mutableModel");

    public final QBaseModel _super = new QBaseModel(this);

    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = createDateTime("createdAt", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = createDateTime("updatedAt", org.joda.time.LocalDateTime.class);

    public QMutableModel(String variable) {
        super(MutableModel.class, forVariable(variable));
    }

    public QMutableModel(Path<? extends MutableModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMutableModel(PathMetadata<?> metadata) {
        super(MutableModel.class, metadata);
    }

}

