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
package io.lavagna.config;

import io.lavagna.service.ConfigurationRepository;
import io.lavagna.service.MySqlFullTextSupportService;
import io.lavagna.service.NotificationService;
import io.lavagna.service.Scheduler;
import io.lavagna.service.StatisticsService;

import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
public class SchedulingServiceConfig {

	@Bean
	public Scheduler getScheduler(TaskScheduler taskScheduler, Environment env,
			ConfigurationRepository configurationRepository,
			MySqlFullTextSupportService mySqlFullTextSupportService,
			NotificationService notificationService,
			StatisticsService statisticsService) {
		return new Scheduler(taskScheduler, env, configurationRepository,
				mySqlFullTextSupportService, notificationService,
				statisticsService);
	}
}
