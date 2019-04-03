package models.base.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.base.BaseModel;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import models.play.query.QGenericModel;


/**
 * QBaseModel is a Querydsl query type for BaseModel
 */
@Generated("com.querydsl.codegen.SupertypeSerializer")
public class QBaseModel extends EntityPathBase<BaseModel> {

    private static final long serialVersionUID = 721081311L;

    public static final QBaseModel baseModel = new QBaseModel("baseModel");

    public final QGenericModel _super = new QGenericModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public QBaseModel(String variable) {
        super(BaseModel.class, forVariable(variable));
    }

    public QBaseModel(Path<? extends BaseModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseModel(PathMetadata metadata) {
        super(BaseModel.class, metadata);
    }

}

