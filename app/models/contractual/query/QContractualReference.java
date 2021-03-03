package models.contractual.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.contractual.ContractualReference;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractualReference is a Querydsl query type for ContractualReference
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QContractualReference extends EntityPathBase<ContractualReference> {

    private static final long serialVersionUID = 1586832865L;

    public static final QContractualReference contractualReference = new QContractualReference("contractualReference");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final ListPath<models.contractual.ContractualClause, QContractualClause> contractualClauses = this.<models.contractual.ContractualClause, QContractualClause>createList("contractualClauses", models.contractual.ContractualClause.class, QContractualClause.class, PathInits.DIRECT2);

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

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

    public QContractualReference(PathMetadata metadata) {
        super(ContractualReference.class, metadata);
    }

}

