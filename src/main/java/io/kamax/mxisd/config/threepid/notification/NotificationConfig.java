/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.config.threepid.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties("notification")
public class NotificationConfig {

    private Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    private Map<String, String> handler = new HashMap<>();

    public Map<String, String> getHandler() {
        return handler;
    }

    public void setHandler(Map<String, String> handler) {
        this.handler = handler;
    }

    @PostConstruct
    public void build() {
        log.info("--- Notification config ---");
        log.info("Handlers:");
        handler.forEach((k, v) -> {
            log.info("\t{}: {}", k, v);
        });
    }

}
