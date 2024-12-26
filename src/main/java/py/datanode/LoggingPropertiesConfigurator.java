/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
