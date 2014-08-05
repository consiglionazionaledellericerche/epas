package controllers;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import models.Stamping;
import play.mvc.Controller;
import dao.HistoryValue;
import dao.PersonDayHistoryDao;
import dao.StampingHistoryDao;

/**
 * @author marco
 *
 */
//@With(Resecure.class)
public class PersonDayHistory extends Controller {
	
	@Inject
	static PersonDayHistoryDao personDayHistoryDao;
	
	@Inject
	static StampingHistoryDao stampingHistoryDao;
	
	
	public static void stampings(long personDayId) {
		List<HistoryValue<Stamping>> allStampings = personDayHistoryDao
				.stampings(personDayId);
		
		Set<Long> stampingIds = Sets.newHashSet();
		for(HistoryValue<Stamping> historyValue : allStampings) {
			stampingIds.add(historyValue.value.id);
		}
		
		List<HistoryValue<Stamping>> results = Lists.newArrayList();
		
		for(Long stampingId : stampingIds) {
			
			List<HistoryValue<Stamping>> stampings = stampingHistoryDao
					.stampings(stampingId);
			results.addAll(stampings);
		}
		
		render(results);
	}
}
