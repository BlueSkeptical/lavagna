/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lavagna.service;

import static io.lavagna.service.SearchFilter.filter;
import io.lavagna.model.BoardColumn;
import io.lavagna.model.CardFullWithCounts;
import io.lavagna.model.CardLabel;
import io.lavagna.model.Key;
import io.lavagna.model.LabelAndValue;
import io.lavagna.model.User;
import io.lavagna.model.UserWithPermission;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.TimeZones;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CalendarService {

	private final ConfigurationRepository configurationRepository;
	private final SearchService searchService;
	private final UserRepository userRepository;
	private final UserService userService;

	@Autowired
	public CalendarService(ConfigurationRepository configurationRepository, SearchService searchService,
			UserService userService, UserRepository userRepository) {
		this.configurationRepository = configurationRepository;
		this.searchService = searchService;
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@Transactional(readOnly = false)
	public String findCalendarTokenFromUser(User user) {
		try {
			return userRepository.findCalendarTokenFromUser(user);
		} catch (CalendarTokenNotFoundException ex) {
			String token = UUID.randomUUID().toString();// <- this use secure random
			String hashedToken = DigestUtils.sha256Hex(token);
			userRepository.registerCalendarToken(user, hashedToken);
			return hashedToken;
		}
	}

	private UserWithPermission findUserFromCalendarToken(String token) {
		int userId = userRepository.findUserIdFromCalendarToken(token);
		return userService.findUserWithPermission(userId);
	}

	private long getLong(int x, int y) {
		return (((long) x) << 32) | (y & 0xffffffffL);
	}

	private String getEventName(LabelAndValue lav, CardFullWithCounts card) {
		StringBuilder sb = new StringBuilder();
		if (lav.getLabelDomain() == CardLabel.LabelDomain.SYSTEM) {
			sb.append(StringUtils.capitalize(lav.getLabelName().replace('_', ' ').toLowerCase()));
		} else {
			sb.append(lav.getLabelName());
		}
		return sb.append(": ").append(card.getName()).toString();
	}

	public Calendar getUserCalendar(String userToken) throws URISyntaxException {
		UserWithPermission user;

		try {
			user = findUserFromCalendarToken(userToken);
		} catch (EmptyResultDataAccessException ex) {
			throw new SecurityException("Invalid token");
		}

		final Calendar calendar = new Calendar();
		calendar.getProperties().add(new ProdId("-//Lavagna//iCal4j 1.0//EN"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);
		calendar.getProperties().add(Method.PUBLISH);

		Map<Integer, CardFullWithCounts> map = new LinkedHashMap<>();

		SearchFilter locationFilter = filter(SearchFilter.FilterType.LOCATION, SearchFilter.ValueType.STRING,
				BoardColumn.BoardColumnLocation.BOARD.toString());

		SearchFilter aFilter = filter(SearchFilter.FilterType.ASSIGNED, SearchFilter.ValueType.CURRENT_USER, "me");
		for (CardFullWithCounts card : searchService.find(Arrays.asList(locationFilter, aFilter), null, null, user)
				.getFound()) {
			map.put(card.getId(), card);
		}

		SearchFilter wFilter = filter(SearchFilter.FilterType.WATCHED_BY, SearchFilter.ValueType.CURRENT_USER, "me");
		for (CardFullWithCounts card : searchService.find(Arrays.asList(locationFilter, wFilter), null, null, user)
				.getFound()) {
			map.put(card.getId(), card);
		}

		final String applicationUrl = StringUtils.appendIfMissing(
				configurationRepository.getValue(Key.BASE_APPLICATION_URL), "/");

		final List<VEvent> events = new ArrayList<>();
		final String utcTimeZone = TimeZones.getUtcTimeZone().getDisplayName();
		for (CardFullWithCounts card : map.values()) {

			Url cardUrl = new Url(new URI(String.format("%s%s/%s-%s", applicationUrl, card.getProjectShortName(),
					card.getBoardShortName(), card.getSequence())));

			for (LabelAndValue lav : card.getLabels()) {
				if (lav.getLabelType() == CardLabel.LabelType.TIMESTAMP) {
					String name = getEventName(lav, card);

					final VEvent event = new VEvent(new Date(lav.getLabelValueTimestamp()), name);
					final VAlarm reminder = new VAlarm(new Dur(0, 0, 0, 0));
					reminder.getProperties().add(Action.DISPLAY);
					reminder.getProperties().add(new Description(name));
					event.getAlarms().add(reminder);

					final UUID id = new UUID(getLong(card.getColumnId(), card.getId()),
							getLong(lav.getLabelId(), lav.getLabelValueId()));
					event.getProperties().add(new Uid(id.toString()));

					TzId tzParam = new TzId(utcTimeZone);
					event.getProperties().getProperty(Property.DTSTART).getParameters().add(tzParam);
					// TODO add organizer e-mail
					Organizer organizer = new Organizer(URI.create("mailto:lavagna"));
					event.getProperties().add(organizer);
					// TODO add description
					//event.getProperties().add(new Description("Event desc"));
					event.getProperties().add(cardUrl);

					events.add(event);
				}
			}
		}

		calendar.getComponents().addAll(events);

		return calendar;
	}

}
