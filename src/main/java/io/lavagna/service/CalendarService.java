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
import io.lavagna.model.CalendarInfo;
import io.lavagna.model.CardFullWithCounts;
import io.lavagna.model.CardLabel;
import io.lavagna.model.CardLabel.LabelType;
import io.lavagna.model.Key;
import io.lavagna.model.LabelAndValue;
import io.lavagna.model.LabelListValueWithMetadata;
import io.lavagna.model.Project;
import io.lavagna.model.SearchResults;
import io.lavagna.model.User;
import io.lavagna.model.UserWithPermission;
import io.lavagna.model.util.CalendarTokenNotFoundException;
import io.lavagna.service.calendarutils.CalendarEventHandler;
import io.lavagna.service.calendarutils.CalendarEvents;
import io.lavagna.service.calendarutils.CalendarVEventHandler;
import io.lavagna.service.calendarutils.StandardCalendarEventHandler;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
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
    private final CardDataService cardDataService;
    private final ProjectService projectService;
    private final CardLabelRepository cardLabelRepository;

    public CalendarService(ConfigurationRepository configurationRepository, SearchService searchService,
        UserService userService, UserRepository userRepository, CardDataService cardDataService,
        ProjectService projectService, CardLabelRepository cardLabelRepository) {
        this.configurationRepository = configurationRepository;
        this.searchService = searchService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.cardDataService = cardDataService;
        this.projectService = projectService;
        this.cardLabelRepository = cardLabelRepository;
    }

    @Transactional(readOnly = false)
    public void setCalendarFeedDisabled(User user, boolean isDisabled) {
        userRepository.setCalendarFeedDisabled(user, isDisabled);
    }

    @Transactional(readOnly = false)
    public CalendarInfo findCalendarInfoFromUser(User user) {
        try {
            return userRepository.findCalendarInfoFromUserId(user);
        } catch (CalendarTokenNotFoundException ex) {
            String token = UUID.randomUUID().toString();// <- this use secure random
            String hashedToken = DigestUtils.sha256Hex(token);
            userRepository.registerCalendarToken(user, hashedToken);
            return findCalendarInfoFromUser(user);
        }
    }

    private UserWithPermission findUserFromCalendarToken(String token) {
        int userId = userRepository.findUserIdFromCalendarToken(token);
        return userService.findUserWithPermission(userId);
    }

    private void addMilestoneEvents(CalendarEventHandler handler, UserWithPermission user)
        throws URISyntaxException, ParseException {

        final SimpleDateFormat releaseDateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        List<Project> projects = projectService.findAllProjects(user);
        for (Project project : projects) {
            CardLabel milestoneLabel = cardLabelRepository.findLabelByName(project.getId(), "MILESTONE",
                CardLabel.LabelDomain.SYSTEM);


            for (LabelListValueWithMetadata m : cardLabelRepository.findListValuesByLabelId(milestoneLabel.getId())) {
                if (m.getMetadata().containsKey("releaseDate")) {

                    java.util.Date date = releaseDateFormatter.parse(m.getMetadata().get("releaseDate") + " 12:00");

                    SearchFilter filter = filter(SearchFilter.FilterType.MILESTONE, SearchFilter.ValueType.STRING,
                        m.getValue());
                    SearchFilter notTrashFilter = filter(SearchFilter.FilterType.NOTLOCATION,
                        SearchFilter.ValueType.STRING, BoardColumn.BoardColumnLocation.TRASH.toString());
                    SearchResults cards = searchService.find(Arrays.asList(filter, notTrashFilter), project.getId(),
                        null, user);

                    handler.addMilestoneEvent(project.getShortName(), date, m, cards);
                }
            }
        }

    }

    private void addCardEvents(CalendarEventHandler handler, UserWithPermission user)
        throws URISyntaxException, ParseException {

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

        for (CardFullWithCounts card : map.values()) {

            for (LabelAndValue lav : card.getLabelsWithType(LabelType.TIMESTAMP)) {
                handler.addCardEvent(card, lav);
            }
        }

    }

    public CalendarEvents getUserCalendar(UserWithPermission user) throws URISyntaxException, ParseException {

        final CalendarEvents events = new CalendarEvents(new HashMap<Date, Set<LabelListValueWithMetadata>>(),
            new HashMap<Date, Set<CardFullWithCounts>>());

        final CalendarEventHandler handler = new StandardCalendarEventHandler(events);

        // Milestones
        addMilestoneEvents(handler, user);

        // Cards
        addCardEvents(handler, user);

        return events;
    }

    public Calendar getCalDavCalendar(String userToken) throws URISyntaxException, ParseException {
        UserWithPermission user;

        try {
            user = findUserFromCalendarToken(userToken);
        } catch (EmptyResultDataAccessException ex) {
            throw new SecurityException("Invalid token");
        }

        if (userRepository.isCalendarFeedDisabled(user)) {
            throw new SecurityException("Calendar feed disabled");
        }

        final Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Lavagna//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        calendar.getProperties().add(Method.PUBLISH);

        final List<VEvent> events = new ArrayList<>();
        final String applicationUrl = StringUtils
            .appendIfMissing(configurationRepository.getValue(Key.BASE_APPLICATION_URL), "/");
        final CalendarEventHandler handler = new CalendarVEventHandler(applicationUrl, cardDataService, userRepository,
            events);

        // Milestones
        addMilestoneEvents(handler, user);

        // Cards
        addCardEvents(handler, user);

        calendar.getComponents().addAll(events);

        return calendar;
    }
}
