package com.wilutions.itol.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.wilutions.com.JoaDll;
import com.wilutions.joa.OfficeAddinUtil;

public class LoggerConfig {
	
	public final static String LEVEL_INFO = "INFO";
	public final static String LEVEL_DEBUG = "DEBUG";
	
	private String file = null;
	private String level = LEVEL_INFO;
	private boolean append = true;
	
	public LoggerConfig() {
		
	}
	
	public void init() {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			String logprops = OfficeAddinUtil.getResourceAsString(classLoader, "com/wilutions/itol/logging.properties");

			String logFile = getFile();
			String logLevel = getLevel();
			if (!logLevel.isEmpty() && !logFile.isEmpty()) {
				logFile = logFile.replace('\\', '/');
				String jutilLevel = logLevel.equals(LEVEL_DEBUG) ? "FINE" : "INFO";
				logprops = MessageFormat.format(logprops, jutilLevel, logFile, isAppend());
				ByteArrayInputStream istream = new ByteArrayInputStream(logprops.getBytes());
				LogManager.getLogManager().readConfiguration(istream);

				for (Handler handler : Logger.getLogger("").getHandlers()) {
					handler.setFormatter(new LogFormatter());
				}

				Logger log = Logger.getLogger(getClass().getName());
				log.info("Logger initialized");
			}
			else {
				Logger.getLogger("").setLevel(Level.INFO);
			}
			
			String logFileNameWithoutExt = new File(logFile).getName();
			{
				int p = logFileNameWithoutExt.lastIndexOf('.');
				if (p >= 0) logFileNameWithoutExt = logFileNameWithoutExt.substring(0, p);
			}

			// Initialize JOA Logfile
			// Remark: Another instance runs in Outlook.exe (other process)
			{
				File joaLogFile = new File(new File(logFile).getParent(), logFileNameWithoutExt + "-joa.log");
				JoaDll.nativeInitLogger(joaLogFile.getAbsolutePath(), logLevel, isAppend());
			}
			
		}
		catch (Throwable e) {
			System.out.println("Logger configuration not found or inaccessible. ");
			e.printStackTrace();
		}
		finally {
		}

	}
	
	protected void copyFrom(LoggerConfig rhs) {
		this.file = rhs.file;
		this.level = rhs.level;
		this.append = rhs.append;
	}

	protected void unsetUserRelatedValues() {
		this.file = null;
		this.level = LEVEL_INFO;
		this.append = true;
	}

	public String getFile() {
		if (Default.value(file).isEmpty()) {
			file = new File(Profile.DEFAULT_TEMP_DIR, "itol.log").getAbsolutePath();
		}
		return file;
	}

	public void setFile(String logFile) {
		this.file = logFile;
	}

	public String getLevel() {
		if (level == null) level = LEVEL_INFO;
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}


}
