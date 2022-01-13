package er.extensions.logging;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.status.StatusLogger;

import er.extensions.foundation.ERXSystem;

public class ERXLog4jConfigurationFactory extends ConfigurationFactory {

	private static final String LOG4J_PREFIX = "log4j.";
	private static final String APPENDER_PREFIX = "appender.";
	private static final String LOGGER_PREFIX = "logger.";

	private Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
		builder.setConfigurationName(name);

		Map<String, String> properties = ERXSystem.getProperties()
				.entrySet()
				.stream()
				.filter(e -> e.getKey().toString().startsWith(LOG4J_PREFIX))
				.collect(Collectors.toMap(e -> e.getKey().toString().substring(LOG4J_PREFIX.length()), e -> String.valueOf(e.getValue())));

		processAppenders(properties, builder);
		processLoggers(properties, builder);
		processRootCategory(properties, builder);

		checkForRemaining(properties, "main");

		return builder.build();
	}

	private void processAppenders(Map<String, String> properties, ConfigurationBuilder<BuiltConfiguration> builder) {
		Map<String, String> allAppenderProperties = getSubMap(properties, APPENDER_PREFIX);
		List<String> appenderNames = allAppenderProperties.keySet()
				.stream()
				.filter(k -> !k.contains("."))
				.collect(Collectors.toList());

		for (String appenderName : appenderNames) {
			String type = allAppenderProperties.remove(appenderName);
			Map<String, String> appenderProperties = getSubMap(allAppenderProperties, appenderName + ".");

			switch (type) {
			case "er.extensions.logging.ERXConsoleAppender":
				processERXConsoleAppender(appenderName, appenderProperties, builder);
				break;
			case "org.apache.log4j.net.SMTPAppender":
				processSMTPAppender(appenderName, appenderProperties, builder);
				break;
			default:
				StatusLogger.getLogger().error("Invalid appender type {}", type);
				continue;
			}

			checkForRemaining(appenderProperties, "appender " + appenderName);
		}

		checkForRemaining(allAppenderProperties, "appender");
	}

	private void processERXConsoleAppender(String appenderName, Map<String, String> appenderProperties, ConfigurationBuilder<BuiltConfiguration> builder) {
		AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "CONSOLE")
				.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);

		appenderBuilder.add(processLayout(appenderProperties, builder));

		builder.add(appenderBuilder);
	}

	private void processSMTPAppender(String appenderName, Map<String, String> appenderProperties, ConfigurationBuilder<BuiltConfiguration> builder) {
		AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "SMTP");

		// TODO using this instead of copyAttribute for now ...
		appenderProperties.remove("BufferSize");
		appenderBuilder.addAttribute("bufferSize", 0);

		copyAttribute(appenderProperties, "From", appenderBuilder, "from");
		copyAttribute(appenderProperties, "SMTPHost", appenderBuilder, "smtpHost");
		copyAttribute(appenderProperties, "Subject", appenderBuilder, "subject");
		copyAttribute(appenderProperties, "To", appenderBuilder, "to");

		appenderBuilder.add(processLayout(appenderProperties, builder));

		String threshold = appenderProperties.remove("Threshold");
		if (threshold != null) {
			appenderBuilder.add(builder.newFilter("ThresholdFilter", Result.NEUTRAL, Result.DENY).addAttribute("level", threshold));
		}

		builder.add(appenderBuilder);
	}

	private LayoutComponentBuilder processLayout(Map<String, String> appenderProperties, ConfigurationBuilder<BuiltConfiguration> builder) {
		String layoutPlugin = "PatternLayout";
		appenderProperties.remove("layout");
		// TODO change layoutPlugin accordingly, see ERXPatternLayout
		LayoutComponentBuilder layoutBuilder = builder.newLayout(layoutPlugin);

		String conversionPattern = appenderProperties.remove("layout.ConversionPattern");
		if (conversionPattern != null) {
			// apparently ISO8601 was changed to use "T" in Log4j2
			conversionPattern = conversionPattern.replace("{ISO8601}", "{DEFAULT}");
			// TODO necessary because ERXPatternLayout is not yet supported
			conversionPattern = conversionPattern.replaceAll("%V\\{.*?\\}", "");
			layoutBuilder.addAttribute("pattern", conversionPattern);
		}

		return layoutBuilder;
	}

	private void processLoggers(Map<String, String> properties, ConfigurationBuilder<BuiltConfiguration> builder) {
		Map<String, String> loggers = getSubMap(properties, LOGGER_PREFIX);

		for (Entry<String, String> logger : loggers.entrySet()) {
			builder.add(builder.newLogger(logger.getKey(), Level.valueOf(logger.getValue())));
			properties.remove(LOGGER_PREFIX + logger.getKey());
		}
	}

	private void processRootCategory(Map<String, String> properties, ConfigurationBuilder<BuiltConfiguration> builder) {
		String rootCategory = properties.remove("rootCategory");
		if (rootCategory != null) {
			String[] elements = rootCategory.split(",");
			RootLoggerComponentBuilder rootLoggerBuilder = builder.newRootLogger(Level.valueOf(elements[0]));
			for (int i = 1; i < elements.length; i++) {
				rootLoggerBuilder.add(builder.newAppenderRef(elements[i]));
			}
			builder.add(rootLoggerBuilder);
		}
	}

	public void copyAttribute(Map<String, String> properties, String propertyName, ComponentBuilder builder, String attributeName) {
		String value = properties.remove(propertyName);
		if (value != null) {
			builder.addAttribute(attributeName, value);
		}
	}

	private Map<String, String> getSubMap(Map<String, String> map, String prefix) {
		Map<String, String> result = map.entrySet()
				.stream()
				.filter(e -> e.getKey().startsWith(prefix))
				.collect(Collectors.toMap(t -> t.getKey().substring(prefix.length()), Entry<String, String>::getValue));
		for (String key : result.keySet()) {
			map.remove(prefix + key);
		}
		return result;
	}

	private void checkForRemaining(Map<String, String> properties, String name) {
		if (!properties.isEmpty()) {
			StatusLogger.getLogger().error("Not all *{}* properties were interpreted, remaining ones: {}", name, properties);
		}
	}

	@Override
	public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
		return getConfiguration(loggerContext, source.toString(), null);
	}

	@Override
	public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
		return createConfiguration(name, newConfigurationBuilder());
	}

	@Override
	protected String[] getSupportedTypes() {
		return new String[] { "*" };
	}

}
