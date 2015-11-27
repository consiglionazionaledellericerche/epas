package play.db.jpa.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.SimplePath;

import play.db.jpa.Model;


/**
 * QModel is a Querydsl query type for Model
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
public class QModel extends EntityPathBase<Model> {

    private static final long serialVersionUID = 948359744L;

    public static final QModel model = new QModel("model");

    public final QGenericModel _super = new QGenericModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QModel(String variable) {
        super(Model.class, forVariable(variable));
    }

    public QModel(Path<? extends Model> path) {
        super(path.getType(), path.getMetadata());
    }

    public QModel(PathMetadata<?> metadata) {
        super(Model.class, metadata);
    }

}

