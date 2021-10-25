import ch.qos.logback.core.util.FileSize
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.StandardCharsets

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = StandardCharsets.UTF_8

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

String logsDir = System.getProperty('catalina.base', '.') + "/logs"
appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "$logsDir/dw-%d{yyyy-MM-dd}.log"
        maxHistory = 30
        totalSizeCap = FileSize.valueOf("2GB")
    }
}

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode()) { Environment.is
    if(targetDir != null) {
        appender("FULL_STACKTRACE", FileAppender) {
            file = "${targetDir}/stacktrace.log"
            append = true
            encoder(PatternLayoutEncoder) {
                charset = StandardCharsets.UTF_8
                pattern = "%level %logger - %msg%n"
            }
        }
        logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    }
    logger("com.erratick.datawarehouse.query.QueryService", DEBUG)
    root(ERROR, ['STDOUT'])
} else {
    root(ERROR, ['ROLLING'])
    logger('grails.app.controllers', INFO, ['ROLLING'], false)
    logger('grails.app.services', INFO, ['ROLLING'], false)
}
