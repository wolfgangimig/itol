package com.wilutions.itol;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

	private final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private final String messageFormat = 
			"{0} " + // ISO Date
			"{2} " + // Level
			"[{1}] " + // Thread name
			"{3}:{4} " + // Class name, line
			"{5} " + // Message
			"\n";

	@Override
	public String format(LogRecord record) {
		
		String iso = isoFormat.format(new Date(record.getMillis()));
		
		String threadName = Thread.currentThread().getName();
		
		Level level = record.getLevel();
		
		String sourceClass = "?";
		int lineNumber = 0;
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		boolean lookingForLogger = true;
		for (StackTraceElement frame : stack) {
			String cname = frame.getClassName();
			boolean isLoggerImpl = isLoggerImplFrame(cname);
			if (lookingForLogger) {
                // Skip all frames until we have found the first logger frame.
                if (isLoggerImpl) {
                    lookingForLogger = false;
                }
			}
			else {
                if (!isLoggerImpl) {
                    // skip reflection call
                    if (!cname.startsWith("java.lang.reflect.") && !cname.startsWith("sun.reflect.")) {
                       // We've found the relevant frame.
        				sourceClass = cname;
        				lineNumber = frame.getLineNumber();
        				break;
                    }
                }
			}
		}
		
		int p = sourceClass.lastIndexOf('.');
		if (p >= 0) sourceClass = sourceClass.substring(p+1);
		
		String message = record.getMessage();
		
		return MessageFormat.format(messageFormat, iso, threadName, level, sourceClass, lineNumber, message);
	}

    private boolean isLoggerImplFrame(String cname) {
        return (cname.equals("java.util.logging.Logger") ||
                cname.startsWith("java.util.logging.LoggingProxyImpl") ||
                cname.startsWith("sun.util.logging."));
    }

}
