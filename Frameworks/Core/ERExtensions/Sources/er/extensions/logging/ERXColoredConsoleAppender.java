package er.extensions.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Adds ANSI color escapes to logged lines depending on their log {@link Level}.
 * Note that colors were selected to fit a dark terminal background.
 * 
 * @see <a href=
 *      "https://marketplace.eclipse.org/content/ansi-escape-console">ANSI
 *      Escape in Console</a>
 * @author Copyright (c) 2020 NUREG. All rights reserved.
 */
public class ERXColoredConsoleAppender extends ERXConsoleAppender {

	public static final String ANSI_ESC = "\u001b";
	/** Control Sequence Introducer */
	public static final String ANSI_CSI = ANSI_ESC + "[";
	/** Select Graphic Rendition */
	public static final String ANSI_SGR = "m";
	public static final String ANSI_SGR_SGR_RESET = ANSI_CSI + ANSI_SGR;

	public static final int ANSI_BACKGROUND_BRIGHT_RED = 101;
	public static final int ANSI_FOREGROUND_BRIGHT_RED = 91;
	public static final int ANSI_FOREGROUND_BRIGHT_YELLOW = 93;
	public static final int ANSI_FOREGROUND_BRIGHT_WHITE = 97;
	public static final int ANSI_FOREGROUND_CYAN = 36;
	public static final int ANSI_FOREGROUND_BLUE = 34;

	public static final String FATAL_COLOR = ANSI_CSI + ANSI_BACKGROUND_BRIGHT_RED + ANSI_SGR;
	public static final String ERROR_COLOR = ANSI_CSI + ANSI_FOREGROUND_BRIGHT_RED + ANSI_SGR;
	public static final String WARN_COLOR = ANSI_CSI + ANSI_FOREGROUND_BRIGHT_YELLOW + ANSI_SGR;
	public static final String INFO_COLOR = ANSI_CSI + ANSI_FOREGROUND_BRIGHT_WHITE + ANSI_SGR;
	public static final String DEBUG_COLOR = ANSI_CSI + ANSI_FOREGROUND_CYAN + ANSI_SGR;
	public static final String TRACE_COLOR = ANSI_CSI + ANSI_FOREGROUND_BLUE + ANSI_SGR;

	@Override
	protected void subAppend(LoggingEvent event) {
		this.qw.write(getColor(event.getLevel()));
		subAppendInner(event);
		this.qw.write(ANSI_SGR_SGR_RESET);

		if (immediateFlush) {
			qw.flush();
		}
	}

	public static String getColor(Level level) {
		switch (level.toInt()) {
		case Priority.FATAL_INT:
			return FATAL_COLOR;
		case Priority.ERROR_INT:
			return ERROR_COLOR;
		case Priority.WARN_INT:
			return WARN_COLOR;
		case Priority.INFO_INT:
			return INFO_COLOR;
		case Priority.DEBUG_INT:
			return DEBUG_COLOR;
		default:
			return TRACE_COLOR;
		}
	}

}
