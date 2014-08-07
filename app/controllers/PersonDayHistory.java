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
import edu.emory.mathcs.backport.java.util.Collections;

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
	
	
	public static void personDayHistory(long personDayId) {
		List<HistoryValue<Stamping>> allStampings = personDayHistoryDao
				.stampings(personDayId);
		
		Set<Long> stampingIds = Sets.newHashSet();
		for(HistoryValue<Stamping> historyValue : allStampings) {
			stampingIds.add(historyValue.value.id);
		}
		
		List<Long> sortedStampingsIds = Lists.newArrayList(stampingIds);
		Collections.sort(sortedStampingsIds);
		
		//Lista di stampings
		List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
		
		for(Long stampingId : sortedStampingsIds) {
		
			List<HistoryValue<Stamping>> historyStamping = stampingHistoryDao
					.stampings(stampingId);
			historyStampingsList.add(historyStamping);
		}
		
		render(historyStampingsList);
	}
}
