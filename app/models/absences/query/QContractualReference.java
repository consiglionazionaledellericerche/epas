package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import models.contractual.ContractualReference;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContractualReference is a Querydsl query type for ContractualReference
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractualReference extends EntityPathBase<ContractualReference> {

    private static final long serialVersionUID = 412010129L;

    public static final QContractualReference contractualReference = new QContractualReference("contractualReference");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.contractual.ContractualClause, QContractualClause> contractualClauses = this.<models.contractual.ContractualClause, QContractualClause>createList("contractualClauses", models.contractual.ContractualClause.class, QContractualClause.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final SimplePath<play.db.jpa.Blob> file = createSimple("file", play.db.jpa.Blob.class);

    public final StringPath filename = createString("filename");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final StringPath url = createString("url");

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QContractualReference(String variable) {
        super(ContractualReference.class, forVariable(variable));
    }

    public QContractualReference(Path<? extends ContractualReference> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContractualReference(PathMetadata<?> metadata) {
        super(ContractualReference.class, metadata);
    }

}

