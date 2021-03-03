package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.CertificatedData;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCertificatedData is a Querydsl query type for CertificatedData
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCertificatedData extends EntityPathBase<CertificatedData> {

    private static final long serialVersionUID = -122268133L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCertificatedData certificatedData = new QCertificatedData("certificatedData");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath absencesSent = createString("absencesSent");

    public final StringPath cognomeNome = createString("cognomeNome");

    public final StringPath competencesSent = createString("competencesSent");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isOk = createBoolean("isOk");

    public final StringPath matricola = createString("matricola");

    public final StringPath mealTicketSent = createString("mealTicketSent");

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath problems = createString("problems");

    public final StringPath trainingHoursSent = createString("trainingHoursSent");

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QCertificatedData(String variable) {
        this(CertificatedData.class, forVariable(variable), INITS);
    }

    public QCertificatedData(Path<? extends CertificatedData> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCertificatedData(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCertificatedData(PathMetadata metadata, PathInits inits) {
        this(CertificatedData.class, metadata, inits);
    }

    public QCertificatedData(Class<? extends CertificatedData> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

