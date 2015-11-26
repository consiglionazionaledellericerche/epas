package models.base.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.base.PeriodModel;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QPeriodModel is a Querydsl query type for PeriodModel
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
public class QPeriodModel extends EntityPathBase<PeriodModel> {

    private static final long serialVersionUID = 1259083055L;

    public static final QPeriodModel periodModel = new QPeriodModel("periodModel");

    public final QBaseModel _super = new QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QPeriodModel(String variable) {
        super(PeriodModel.class, forVariable(variable));
    }

    public QPeriodModel(Path<? extends PeriodModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPeriodModel(PathMetadata<?> metadata) {
        super(PeriodModel.class, metadata);
    }

}

