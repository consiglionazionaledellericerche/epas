package play.db.jpa.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.SimplePath;

import play.db.jpa.GenericModel;


/**
 * QGenericModel is a Querydsl query type for GenericModel
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
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

    public QGenericModel(PathMetadata<?> metadata) {
        super(GenericModel.class, metadata);
    }

}

