/*
 * Copyright (c) 2010-2021, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.basics;

import org.sikuli.script.SX;
import org.sikuli.script.support.Commons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Debug is a utility class that wraps println statements and allows more or less command line
 * output to be turned on.<br> <br> For debug messages only ( Debug.log() ):<br> Use system
 * property: sikuli.Debug to set the debug level (default = 1)<br> On the command line, use
 * -Dsikuli.Debug=n to set it to level n<br> -Dsikuli.Debug will disable any debug messages <br>
 * (which is equivalent to using Settings.Debuglogs = false)<br> <br> It prints if the level
 * number is less than or equal to the currently set DEBUG_LEVEL.<br> <br> For messages
 * ActionLogs, InfoLogs see Settings<br> <br> You might send all messages generated by this
 * class to a file:<br>-Dsikuli.Logfile=pathname (no path given: SikuliLog.txt in working
 * folder)<br> This can be restricted to Debug.user only (others go to System.out):<br>
 * -Dsikuli.LogfileUser=pathname (no path given: UserLog.txt in working folder)<br>
 * <p>
 * You might redirect info, action, error and debug messages to your own logger object<br>
 * Start with setLogger() and then define with setLoggerXyz() the redirection targets
 * <p>
 * This solution is NOT threadsafe !!!
 */
public class Debug {

  //<editor-fold desc="01 verbose/quiet">
  private static boolean verbose = false;

  public static void setVerbose() {
    Debug.setDebugLevel(3);
    verbose = true;
  }

  public static boolean isVerbose() {
    return verbose;
  }

  private static boolean quiet = false;

  public static void setQuiet() {
    quiet = true;
  }

  public static boolean isQuiet() {
    return quiet;
  }

  static boolean console = false;

  public static void setConsole() {
    console = true;
  }

  public static boolean isConsole() {
    return console;
  }
  //</editor-fold>

  private static boolean ideIsStarting = false;

  public static boolean isIDEstarting(boolean... state) {
    if (state.length > 0) {
      ideIsStarting = state[0];
    }
    return ideIsStarting;
  }

  static String IDE_START_LOG = "";
  static File IDE_START_LOG_FILE = null;

  public static String getIdeStartLog() {
    if (IDE_START_LOG_FILE == null & !isIDEstarting()) {
      IDE_START_LOG_FILE = new File(Commons.getAppDataStore(), "SikulixIDEstartlog.txt");
      boolean success = !IDE_START_LOG.isEmpty();
      if (!FileManager.writeStringToFile(IDE_START_LOG, IDE_START_LOG_FILE)) {
        success = false;
        error("Debug::IDE_START_LOG_FILE: not saved (%s)", IDE_START_LOG_FILE);
      }
      if (!success) {
        IDE_START_LOG_FILE.delete();
        IDE_START_LOG_FILE = null;
      }
    }
    return IDE_START_LOG;
  }

  public static File getIdeStartLogFile() {
    return IDE_START_LOG_FILE;
  }

  public static String printIdeStartLog() {
    return print(getIdeStartLog());
  }

  public static void runShutDown() {
    //TODO DEBUG shutdown
  }

  public static long timeNow() {
    return new Date().getTime();
  }

  public static long timeSince(long start) {
    return new Date().getTime() - start;
  }

  private static PrintStream printout = null;
  private static PrintStream printoutuser = null;

  static {
    setDebugLogFile();
    setUserLogFile();
  }

  //<editor-fold desc="highlight">
  private static boolean searchHighlight = false;

  public static void highlightOn() {
    searchHighlight = true;
    Settings.Highlight = true;
  }

  public static void highlightOff() {
    searchHighlight = false;
    Settings.Highlight = false;
  }

  public static boolean shouldHighlight() {
    return searchHighlight;
  }
  //</editor-fold>

  //<editor-fold desc="logger callback">
  private static boolean isJython;
  private static boolean isJRuby;
  private static boolean loggerRedirectSupported = true;
  private static Object privateLogger = null;
  private static boolean privateLoggerPrefixAll = true;
  private static Method privateLoggerUser = null;
  private static String privateLoggerUserName = "";
  private static String privateLoggerUserPrefix = "";
  private static Method privateLoggerInfo = null;
  private static String privateLoggerInfoName = "";
  private static final String infoPrefix = "info";
  private static String privateLoggerInfoPrefix = "[" + infoPrefix + "] ";
  private static Method privateLoggerAction = null;
  private static String privateLoggerActionName = "";
  private static final String actionPrefix = "log";
  private static String privateLoggerActionPrefix = "[" + actionPrefix + "] ";
  private static Method privateLoggerError = null;
  private static String privateLoggerErrorName = "";
  private static final String errorPrefix = "error";
  private static String privateLoggerErrorPrefix = "[" + errorPrefix + "] ";
  private static Method privateLoggerDebug = null;
  private static String privateLoggerDebugName = "";
  private static final String debugPrefix = "debug";
  private static String privateLoggerDebugPrefix = "";

  private enum CallbackType {
    INFO, ACTION, ERROR, DEBUG, USER;
  }

  /**
   * A logger object that is intended, to get Sikuli's log messages per redirection
   *
   * @param logger the logger object
   */
  public static void setLogger(Object logger) {
    if (!doSetLogger(logger)) return;
    privateLoggerPrefixAll = true;
    logx(3, "Debug: setLogger %s", logger);
  }

  /**
   * same as setLogger(), but the Sikuli prefixes are omitted in all redirected messages
   *
   * @param logger the logger object
   */
  public static void setLoggerNoPrefix(Object logger) {
    if (!doSetLogger(logger)) return;
    privateLoggerPrefixAll = false;
  }

  /**
   * sets the redirection for all message types user, info, action, error and debug
   * must be the name of an instance method of the previously defined logger and<br>
   * must accept exactly one string parameter, that contains the message text
   *
   * @param mAll name of the method where the message should be sent
   * @return true if the method is available false otherwise
   */
  public static boolean setLoggerAll(String mAll) {
    if (!loggerRedirectSupported) {
      logx(3, "Debug: setLoggerAll: logger redirect not supported");
      return false;
    }
    if (privateLogger != null) {
      logx(3, "Debug.setLoggerAll: %s", mAll);
      boolean success = true;
      success &= setLoggerUser(mAll);
      success &= setLoggerInfo(mAll);
      success &= setLoggerAction(mAll);
      success &= setLoggerError(mAll);
      success &= setLoggerDebug(mAll);
      return success;
    }
    return false;
  }

  /**
   * specify the target method for redirection of Sikuli's user log messages [user]<br>
   * must be the name of an instance method of the previously defined logger and<br>
   * must accept exactly one string parameter, that contains the info message
   *
   * @param mUser name of the method where the message should be sent
   *              <br>reset to default logging by either null or empty string
   * @return true if the method is available false otherwise
   */
  public static boolean setLoggerUser(String mUser) {
    if (mUser == null || mUser.isEmpty()) {
      privateLoggerUserName = "";
      return true;
    }
    return doSetLoggerCallback(mUser, CallbackType.USER);
  }

  /**
   * specify the target method for redirection of Sikuli's info messages [info]<br>
   * must be the name of an instance method of the previously defined logger and<br>
   * must accept exactly one string parameter, that contains the info message
   *
   * @param mInfo name of the method where the message should be sent
   *              <br>reset to default logging by either null or empty string
   * @return true if the method is available false otherwise
   */
  public static boolean setLoggerInfo(String mInfo) {
    if (mInfo == null || mInfo.isEmpty()) {
      privateLoggerInfoName = "";
      return true;
    }
    return doSetLoggerCallback(mInfo, CallbackType.INFO);
  }

  /**
   * specify the target method for redirection of Sikuli's action messages [log]<br>
   * must be the name of an instance method of the previously defined logger and<br>
   * must accept exactly one string parameter, that contains the info message
   *
   * @param mAction name of the method where the message should be sent
   *                <br>reset to default logging by either null or empty string
   * @return true if the method is available false otherwise
   */
  public static boolean setLoggerAction(String mAction) {
    if (mAction == null || mAction.isEmpty()) {
      privateLoggerActionName = "";
      return true;
    }
    return doSetLoggerCallback(mAction, CallbackType.ACTION);
  }

  /**
   * specify the target method for redirection of Sikuli's error messages [error]<br>
   * must be the name of an instance method of the previously defined logger and<br>
   * must accept exactly one string parameter, that contains the info message
   *
   * @param mError name of the method where the message should be sent
   *               <br>reset to default logging by either null or empty string
   * @return true if the method is available false otherwise
   */
  public static boolean setLoggerError(String mError) {
    if (mError == null || mError.isEmpty()) {
      privateLoggerErrorName = "";
      return true;
    }
    return doSetLoggerCallback(mError, CallbackType.ERROR);
  }

  /**
   * specify the target method for redirection of Sikuli's debug messages [debug]<br>
   * must be the name of an instance method of the previously defined logger and<br>
   * must accept exactly one string parameter, that contains the info message
   *
   * @param mDebug name of the method where the message should be sent
   *               <br>reset to default logging by either null or empty string
   * @return true if the method is available false otherwise
   */
  public static boolean setLoggerDebug(String mDebug) {
    if (mDebug == null || mDebug.isEmpty()) {
      privateLoggerDebugName = "";
      return true;
    }
    return doSetLoggerCallback(mDebug, CallbackType.DEBUG);
  }

  private static boolean doSetLogger(Object logger) {
    String className = logger.getClass().getName();
    isJython = className.contains("org.python");
    isJRuby = className.contains("org.jruby");
    if (isJRuby) {
      logx(3, "Debug: setLogger: given instance's class: %s", className);
      error("setLogger: not yet supported in JRuby script");
      loggerRedirectSupported = false;
      return false;
    }
    privateLogger = logger;
    return true;
  }

  private static boolean doSetLoggerCallback(String mName, CallbackType type) {
    if (privateLogger == null) {
      error("Debug: setLogger: no logger specified yet");
      return false;
    }
    if (!loggerRedirectSupported) {
      logx(3, "Debug: setLogger: %s (%s) logger redirect not supported", mName, type);
    }
    if (isJython) {
      Object[] args = new Object[]{privateLogger, mName, type.toString()};
      Object checkCallback = Commons.runFunctionScriptingSupport("checkCallback", args);
      if (checkCallback == null || !((Boolean) checkCallback)) {
        logx(3, "Debug: setLogger: Jython: checkCallback returned: %s", args[0]);
        return false;
      }
    }
    try {
      if (type == CallbackType.INFO) {
        if (!isJython && !isJRuby) {
          privateLoggerInfo = privateLogger.getClass().getMethod(mName, new Class[]{String.class});
        }
        privateLoggerInfoName = mName;
        return true;
      } else if (type == CallbackType.ACTION) {
        if (!isJython && !isJRuby) {
          privateLoggerAction = privateLogger.getClass().getMethod(mName, new Class[]{String.class});
        }
        privateLoggerActionName = mName;
        return true;
      } else if (type == CallbackType.ERROR) {
        if (!isJython && !isJRuby) {
          privateLoggerError = privateLogger.getClass().getMethod(mName, new Class[]{String.class});
        }
        privateLoggerErrorName = mName;
        return true;
      } else if (type == CallbackType.DEBUG) {
        if (!isJython && !isJRuby) {
          privateLoggerDebug = privateLogger.getClass().getMethod(mName, new Class[]{String.class});
        }
        privateLoggerDebugName = mName;
        return true;
      } else if (type == CallbackType.USER) {
        if (!isJython && !isJRuby) {
          privateLoggerUser = privateLogger.getClass().getMethod(mName, new Class[]{String.class});
        }
        privateLoggerUserName = mName;
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      error("Debug: setLoggerInfo: redirecting to %s failed: \n%s", mName, e.getMessage());
    }
    return false;
  }

  private static boolean doRedirect(CallbackType type, String pre, String message, Object... args) {
    boolean success = false;
    String error = "";
    if (privateLogger != null) {
      String prefix = "", pln = "";
      Method plf = null;
      if (type == CallbackType.INFO && !privateLoggerInfoName.isEmpty()) {
        prefix = privateLoggerPrefixAll ? privateLoggerInfoPrefix : "";
        plf = privateLoggerInfo;
        pln = privateLoggerInfoName;
      } else if (type == CallbackType.ACTION && !privateLoggerActionName.isEmpty()) {
        prefix = privateLoggerPrefixAll ? privateLoggerActionPrefix : "";
        plf = privateLoggerAction;
        pln = privateLoggerActionName;
      } else if (type == CallbackType.ERROR && !privateLoggerErrorName.isEmpty()) {
        prefix = privateLoggerPrefixAll ? privateLoggerErrorPrefix : "";
        plf = privateLoggerError;
        pln = privateLoggerErrorName;
      } else if (type == CallbackType.DEBUG && !privateLoggerDebugName.isEmpty()) {
        prefix = privateLoggerPrefixAll ?
            (privateLoggerDebugPrefix.isEmpty() ? pre : privateLoggerDebugPrefix) : "";
        plf = privateLoggerDebug;
        pln = privateLoggerDebugName;
      } else if (type == CallbackType.USER && !privateLoggerUserName.isEmpty()) {
        prefix = privateLoggerPrefixAll ?
            (privateLoggerUserPrefix.isEmpty() ? pre : privateLoggerUserPrefix) : "";
        plf = privateLoggerUser;
        pln = privateLoggerUserName;
      }
      if (!pln.isEmpty()) {
        String msg = null;
        if (args == null || args.length == 0) {
          msg = prefix + message;
        } else {
          msg = String.format(prefix + message, args);
        }
        if (isJython) {
          Object runLoggerCallback = Commons.runFunctionScriptingSupport("runLoggerCallback",
              new Object[]{privateLogger, pln, msg});
          success = runLoggerCallback != null && (Boolean) runLoggerCallback;
        } else if (isJRuby) {
          success = false;
        } else {
          try {
            plf.invoke(privateLogger,

                new Object[]{msg});
            return true;
          } catch (Exception e) {
            error = ": " + e.getMessage();
            success = false;
          }
        }
        if (!success) {
          Debug.error("calling (%s) logger.%s failed - resetting to default%s", type, pln, error);
          if (type == CallbackType.INFO) {
            privateLoggerInfoName = "";
          } else if (type == CallbackType.ACTION) {
            privateLoggerActionName = "";
          } else if (type == CallbackType.ERROR) {
            privateLoggerErrorName = "";
          } else if (type == CallbackType.DEBUG) {
            privateLoggerDebugName = "";
          } else if (type == CallbackType.USER) {
            privateLoggerUserName = "";
          }
        }
      }
    }
    return success;
  }
  //</editor-fold>

  //<editor-fold desc="logfiles">

  /**
   * specify, where the logs should be written:<br>
   * null - use from property sikuli.Logfile
   * empty - use SikuliLog.txt in working folder
   * not empty - use given filename
   *
   * @param fileName null, empty or absolute filename
   * @return success
   */
  public static boolean setDebugLogFile(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      fileName = "SikulixLog.txt";
    }
    File fLog = Commons.asFile(fileName);
    if (fLog.exists()) {
      fLog.delete();
    }
    fileName = fLog.getAbsolutePath();
    try {
      PrintStream printoutNew = new PrintStream(fLog);
      if (printout != null) {
        printout.close();
      }
      printout = printoutNew;
      log(3, "Debug: setLogFile: " + fileName);
      return true;
    } catch (Exception ex) {
      System.out.printf("[Error] Logfile %s not accessible", fileName);
      System.out.println();
      return false;
    }
  }

  public static void setDebugLogFile() {
    String fileName = System.getProperty("sikuli.Logfile");
    if (fileName == null) {
      return;
    }
    if (fileName.isBlank()) {
      fileName = "SikulixLog.txt";
    }
    setDebugLogFile(fileName);
  }

  /**
   * specify, where the user logs (Debug.user) should be written:<br>
   * null - use from property sikuli.LogfileUser
   * empty - use UserLog.txt in working folder
   * not empty - use given filename
   *
   * @param fileName null, empty or absolute filename
   * @return success
   */
  public static boolean setUserLogFile(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      fileName = "SikulixUserLog.txt";
    }
    File fLog = Commons.asFile(fileName);
    if (fLog.exists()) {
      fLog.delete();
    }
    fileName = fLog.getAbsolutePath();
    try {
      PrintStream printoutuserNew = new PrintStream(fLog);
      if (printoutuser != null) {
        printoutuser.close();
      }
      printoutuser = printoutuserNew;
      log(3, "Debug: setUserLogFile: " + fileName);
      return true;
    } catch (FileNotFoundException ex) {
      System.out.printf("[Error] User logfile %s not accessible", fileName);
      System.out.println();
      return false;
    }
  }

  public static void setUserLogFile() {
    String fileName = System.getProperty("sikuli.UserLogfile");
    if (fileName == null) {
      return;
    }
    if (fileName.isBlank()) {
      fileName = "SikulixUserLog.txt";
    }
    setUserLogFile(fileName);
  }
  //</editor-fold>

  //<editor-fold desc="debug level">
  private static int DEBUG_LEVEL = 0;

  /**
   * @return current debug level
   */
  public static int getDebugLevel() {
    return DEBUG_LEVEL;
  }

  /**
   * set debug level to given value
   *
   * @param level value
   */
  public static void setDebugLevel(int level) {
    DEBUG_LEVEL = level;
    if (DEBUG_LEVEL > 0) {
      Settings.DebugLogs = true;
    } else {
      Settings.DebugLogs = false;
    }
  }

  public static void on(int level) {
    setDebugLevel(level);
  }

  public static boolean is(int level) {
    return DEBUG_LEVEL >= level;
  }

  public static int is() {
    return DEBUG_LEVEL;
  }

  public static void off() {
    setDebugLevel(0);
  }

  public static void reset() {
    setDebugLevel(0);
  }
  //</editor-fold>

  //<editor-fold desc="logging">
  private static final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

  /**
   * messages given by the user<br> switch on/off: Settings.UserLogs<br> depending on
   * Settings.UserLogTime, the prefix contains a timestamp <br> the user prefix (default "user")
   * can be set: Settings,UserLogPrefix
   *
   * @param message String or format string (String.format)
   * @param args    to use with format string
   */
  public static void user(String message, Object... args) {
    if (Settings.UserLogs) {
      if (Settings.UserLogTime) {
//TODO replace the hack -99 to filter user logs
        log(-99, String.format("%s (%s)",
            Settings.UserLogPrefix, df.format(new Date())), message, args);
      } else {
        log(-99, String.format("%s", Settings.UserLogPrefix), message, args);
      }
    }
  }

  /**
   * Sikuli messages from actions like click, ...<br> switch on/off: Settings.ActionLogs
   *
   * @param message String or format string (String.format)
   * @param args    to use with format string
   */
  public static void action(String message, Object... args) {
    if (Settings.ActionLogs) {
      if (doRedirect(CallbackType.ACTION, "", message, args)) {
        return;
      }
      if (is(3)) {
        logx(3, message, args);
      } else {
        log(-1, actionPrefix, message, args);
      }
    }
  }

  /**
   * informative Sikuli messages <br> switch on/off: Settings.InfoLogs
   *
   * @param message String or format string (String.format)
   * @param args    to use with format string
   */
  public static void info(String message, Object... args) {
    if (Settings.InfoLogs) {
      if (is(3)) {
        logx(3, message, args);
      } else {
        if (doRedirect(CallbackType.INFO, "", message, args)) {
          return;
        }
        log(-1, infoPrefix, message, args);
      }
    }
  }

  /**
   * Sikuli error messages<br> switch on/off: always on
   *
   * @param message String or format string (String.format)
   * @param args    to use with format string
   */
  public static void error(String message, Object... args) {
    if (doRedirect(CallbackType.ERROR, "", message, args)) {
      return;
    }
    log(-1, errorPrefix, message, args);
  }

  /**
   * Sikuli debug messages with default level<br> switch on/off: Settings.DebugLogs (off) and/or
   * -Dsikuli.Debug
   *
   * @param message String or format string (String.format)
   * @param args    to use with format string
   */
  public static void log(String message, Object... args) {
    log(0, message, args);
  }

  /**
   * Sikuli debug messages with level<br> switch on/off: Settings.DebugLogs (off) and/or
   * -Dsikuli.Debug
   *
   * @param level   value
   * @param message String or format string (String.format)
   * @param args    to use with format string
   */
  public static void log(int level, String message, Object... args) {
    if (Settings.DebugLogs || isVerbose()) {
      String prefix = debugPrefix;
      log(level, prefix, message, args);
    }
  }

  public static void logx(int level, String message, Object... args) {
    if (level == -1 || level == -100) {
      log(level, errorPrefix, message, args);
    } else if (level == -2) {
      log(level, actionPrefix, message, args);
    } else if (level == -3) {
      log(level, "", message, args);
    } else {
      log(level, debugPrefix, message, args);
    }
  }

  @Deprecated
  public static String logp(String msg, Object... args) {
    return print(msg, args);
  }

  public static String print(String msg, Object... args) {
    String out = msg;
    if (args != null && args.length > 0) {
      out = String.format(msg, args);
    }
    log(-1, "", out);
    return out;
  }

  private static synchronized void log(int level, String prefix, String message, Object... args) {
//TODO replace the hack -99 to filter user logs
    if (isQuiet()) { //TODO
      return;
    }
    String sout = "";
    String stime = "";
    if (level <= DEBUG_LEVEL) {
      if (Settings.LogTime && level != -99) {
        stime = String.format(" (%s)", df.format(new Date()));
      }
      if (!prefix.isEmpty()) {
        prefix = "[" + prefix + stime + "] ";
      }
      if (args != null && args.length > 0) {
        sout = String.format(message, args);
      } else {
        sout = message;
      }
      boolean isRedirected = false;
      if (level > -99) {
        isRedirected = doRedirect(CallbackType.DEBUG, prefix, sout);
      } else if (level == -99) {
        isRedirected = doRedirect(CallbackType.USER, prefix, sout);
      }
      if (!isRedirected) {
        if (level == -99 && printoutuser != null) {
          printoutuser.print(prefix + sout);
          printoutuser.println();
        } else if (printout != null) {
          printout.print(prefix + sout);
          printout.println();
        } else {
          if (isIDEstarting()) {
            String log = (prefix.isEmpty() ? "" : String.format("[SXLOG %4.3f] ", Commons.getSinceStart())) + prefix + sout;
            IDE_START_LOG += log + System.lineSeparator();
            if (isConsole()) {
              System.out.println(log);
            }
          } else {
            System.out.println(prefix + sout);
          }
        }
      }
    }
  }

  public static void pop(String msg, Object... args) {
    SX.popup(print(msg, args), "Debug Message");
  }
  //</editor-fold>

  //<editor-fold desc="profiling">
  private long _beginTime = 0;
  private String _message;
  private String _title = null;

  /**
   * Sikuli profiling messages<br> switch on/off: Settings.ProfileLogs, default off
   *
   * @param message String or format string
   * @param args    to use with format string
   */
  public static void profile(String message, Object... args) {
    if (Settings.ProfileLogs) {
      log(-1, "profile", message, args);
    }
  }

  /**
   * profile convenience: entering a method
   *
   * @param message String or format string
   * @param args    to use with format string
   */
  public static void enter(String message, Object... args) {
    profile("entering: " + message, args);
  }

  /**
   * profile convenience: exiting a method
   *
   * @param message String or format string
   * @param args    to use with format string
   */
  public static void exit(String message, Object... args) {
    profile("exiting: " + message, args);
  }

  /**
   * start timer
   * <br>log output depends on Settings.ProfileLogs
   *
   * @return timer
   */
  public static Debug startTimer() {
    return startTimer("");
  }

  /**
   * start timer with a message
   * <br>log output depends on Settings.ProfileLogs
   *
   * @param message String or format string
   * @param args    to use with format string
   * @return timer
   */
  public static Debug startTimer(String message, Object... args) {
    Debug timer = new Debug();
    timer.startTiming(message, args);
    return timer;
  }

  /**
   * stop timer and print timer message
   * <br>log output depends on Settings.ProfileLogs
   *
   * @return the time in msec
   */
  public long end() {
    if (_title == null) {
      return endTiming(_message, false, new Object[0]);
    } else {
      return endTiming(_title, false, new Object[0]);
    }
  }

  /**
   * lap timer and print message with timer message
   * <br>log output depends on Settings.ProfileLogs
   *
   * @param message String or format string
   * @return the time in msec
   */
  public long lap(String message) {
    if (_title == null) {
      return endTiming("(" + message + ") " + _message, true, new Object[0]);
    } else {
      return endTiming("(" + message + ") " + _title, true, new Object[0]);
    }
  }

  private void startTiming(String message, Object... args) {
    int pos;
    if ((pos = message.indexOf("\t")) < 0) {
      _title = null;
      _message = message;
    } else {
      _title = message.substring(0, pos);
      _message = message.replace("\t", " ");
    }
    if (!"".equals(_message)) {
      profile("TStart: " + _message, args);
    }
    _beginTime = (new Date()).getTime();
  }

  private long endTiming(String message, boolean isLap, Object... args) {
    if (_beginTime == 0) {
      profile("TError: timer not started (%s)", message);
      return -1;
    }
    long t = (new Date()).getTime();
    long dt = t - _beginTime;
    if (!isLap) {
      _beginTime = 0;
    }
    if (!"".equals(message)) {
      profile(String.format((isLap ? "TLap:" : "TEnd") +
          " (%.3f sec): ", (float) dt / 1000) + message, args);
    }
    return dt;
  }
  //</editor-fold>
}
