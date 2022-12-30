/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
 */

package py.datanode;

import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;

public class LoggingPropertiesConfigurator {
  public static void configure() {
    configure(Level.WARN, "datanode.log");
  }

  public static void configure(Level level, String fileName) {
    Properties log4jProperties = new Properties();
    log4jProperties.put("log4j.rootLogger", level.toString() + ", stdout, DataNode");

    log4jProperties.put("log4j.appender.DataNode", "org.apache.log4j.RollingFileAppender");
    log4jProperties.put("log4j.appender.DataNode.File", "logs/" + fileName);
    log4jProperties.put("log4j.appender.DataNode.layout", "org.apache.log4j.PatternLayout");
    log4jProperties
        .put("log4j.appender.DataNode.layout.ConversionPattern", "%-5p[%d][%t]%C(%L):%m%n");
    log4jProperties.put("log4j.appender.DataNode.MaxBackupIndex", "10");
    log4jProperties.put("log4j.appender.DataNode.MaxFileSize", "400MB");

    log4jProperties.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
    log4jProperties.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
    log4jProperties
        .put("log4j.appender.stdout.layout.ConversionPattern", "%-5p[%d][%t]%C(%L):%m%n");

    log4jProperties.put("log4j.logger.org.hibernate", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.org.springframework", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.com.opensymphony", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.org.apache", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.com.googlecode", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.com.twitter.common.stats", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.com.mchange", "ERROR, stdout, DataNode");
    log4jProperties.put("log4j.logger.py.datanode.DataNodeAppEngine", "DEBUG, DataNode");

    PropertyConfigurator.configure(log4jProperties);
  }
}
