package dao.history;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import com.google.common.collect.FluentIterable;
import com.google.inject.Provider;

import models.Competence;
import models.flows.CompetenceRequest;

public class CompetenceRequestHistoryDao {

	private final Provider<AuditReader> auditReader;

	@Inject
	CompetenceRequestHistoryDao(Provider<AuditReader> auditReader) {
		this.auditReader = auditReader;
	}

	@SuppressWarnings("unchecked")
	public List<HistoryValue<CompetenceRequest>> competenceRequests(long competenceRequestId) {

		final AuditQuery query = auditReader.get().createQuery()
				.forRevisionsOfEntity(CompetenceRequest.class, false, true)
				.add(AuditEntity.id().eq(competenceRequestId))
				.addOrder(AuditEntity.revisionNumber().asc());

		return FluentIterable.from(query.getResultList())
				.transform(HistoryValue.fromTuple(CompetenceRequest.class))
				.toList();

	}
}
