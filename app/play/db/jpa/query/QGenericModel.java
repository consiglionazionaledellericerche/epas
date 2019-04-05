package play.db.jpa.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import play.db.jpa.GenericModel;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QGenericModel is a Querydsl query type for GenericModel
 */
@Generated("com.querydsl.codegen.SupertypeSerializer")
public class QGenericModel extends EntityPathBase<GenericModel> {

    private static final long serialVersionUID = 1320149147L;

    public static final QGenericModel genericModel = new QGenericModel("genericModel");

    public final QJPABase _super = new QJPABase(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QGenericModel(String variable) {
        super(GenericModel.class, forVariable(variable));
    }

    public QGenericModel(Path<? extends GenericModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGenericModel(PathMetadata metadata) {
        super(GenericModel.class, metadata);
    }

}

