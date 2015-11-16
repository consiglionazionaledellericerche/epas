package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import models.Stamping;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import java.util.List;

/**
 * @author marco
 *
 */
public class StampingHistoryDao {
	
	private final Provider<AuditReader> auditReader;

	@Inject
	StampingHistoryDao(Provider<AuditReader> auditReader) {
		this.auditReader = auditReader;
	}
	
	@SuppressWarnings("unchecked")
	public List<HistoryValue<Stamping>> stampings(long stampingId) {
		
		final AuditQuery query = auditReader.get().createQuery()
			    .forRevisionsOfEntity(Stamping.class, false, true)
				.add(AuditEntity.id().eq( stampingId ))
				.addOrder(AuditEntity.revisionNumber().asc());
		
		return FluentIterable.from(query.getResultList())
				.transform(HistoryValue.fromTuple(Stamping.class))
				.toList();
	}
	
}
