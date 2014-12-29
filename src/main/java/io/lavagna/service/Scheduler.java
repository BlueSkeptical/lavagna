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

import io.lavagna.common.DatabaseMigrationDoneEvent;
import io.lavagna.common.Json;
import io.lavagna.model.Key;
import io.lavagna.model.MailConfig;

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * Simple scheduler. Note: it's not cluster aware.
 */
@Service
public class Scheduler implements ApplicationListener<DatabaseMigrationDoneEvent> {

	private static final Logger LOG = LogManager.getLogger();

	private final TaskScheduler taskScheduler;
	private final Environment env;
	private final ConfigurationRepository configurationRepository;
	private final MySqlFullTextSupportService mySqlFullTextSupportService;
	private final NotificationService notificationService;

	@Autowired
	public Scheduler(TaskScheduler taskScheduler, Environment env, ConfigurationRepository configurationRepository,
			MySqlFullTextSupportService mySqlFullTextSupportService, NotificationService notificationService) {

		this.taskScheduler = taskScheduler;
		this.env = env;
		this.configurationRepository = configurationRepository;
		this.mySqlFullTextSupportService = mySqlFullTextSupportService;
		this.notificationService = notificationService;
	}

	private static class EmailNotificationHandler implements Runnable {

		private final ConfigurationRepository configurationRepository;
		private final NotificationService notificationService;

		private EmailNotificationHandler(ConfigurationRepository configurationRepository,
				NotificationService notificationService) {
			this.configurationRepository = configurationRepository;
			this.notificationService = notificationService;
		}

		@Override
		public void run() {

			Date upTo = new Date();
			Set<Integer> usersToNotify = notificationService.check(upTo);

			Map<Key, String> conf = configurationRepository.findConfigurationFor(EnumSet.of(Key.SMTP_ENABLED,
					Key.SMTP_CONFIG));

			boolean enabled = Boolean.parseBoolean(ObjectUtils.firstNonNull(conf.get(Key.SMTP_ENABLED), "false"));
			MailConfig mailConfig = Json.GSON.fromJson(conf.get(Key.SMTP_CONFIG), MailConfig.class);
			for (int userId : usersToNotify) {
				try {
					notificationService.notifyUser(userId, upTo, enabled, mailConfig);
				} catch (MailException me) {
					LOG.error("Error while sending email to userId " + userId, me);
				}
			}
		}
	}

	@Override
	public void onApplicationEvent(DatabaseMigrationDoneEvent event) {
		if ("MYSQL".equals(env.getProperty("datasource.dialect"))) {
			taskScheduler.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					mySqlFullTextSupportService.syncNewCards();
					mySqlFullTextSupportService.syncUpdatedCards();
					mySqlFullTextSupportService.syncNewCardData();
					mySqlFullTextSupportService.syncUpdatedCardData();
				}
			}, 2 * 1000);
		}

		taskScheduler.scheduleAtFixedRate(new EmailNotificationHandler(configurationRepository, notificationService),
				30 * 1000);
	}
}
