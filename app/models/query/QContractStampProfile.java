package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import models.ContractStampProfile;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QContractStampProfile is a Querydsl query type for ContractStampProfile
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractStampProfile extends EntityPathBase<ContractStampProfile> {

    private static final long serialVersionUID = 1750135740L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractStampProfile contractStampProfile = new QContractStampProfile("contractStampProfile");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QContract contract;

    public final DatePath<org.joda.time.LocalDate> endTo = createDate("endTo", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath fixedworkingtime = createBoolean("fixedworkingtime");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final DatePath<org.joda.time.LocalDate> startFrom = createDate("startFrom", org.joda.time.LocalDate.class);

    public QContractStampProfile(String variable) {
        this(ContractStampProfile.class, forVariable(variable), INITS);
    }

    public QContractStampProfile(Path<? extends ContractStampProfile> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractStampProfile(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractStampProfile(PathMetadata<?> metadata, PathInits inits) {
        this(ContractStampProfile.class, metadata, inits);
    }

    public QContractStampProfile(Class<? extends ContractStampProfile> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

