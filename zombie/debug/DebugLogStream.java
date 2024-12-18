package zombie.debug;

import java.io.PrintStream;
import java.util.HashSet;
import zombie.core.Core;
import zombie.util.StringUtils;

public class DebugLogStream extends PrintStream {
   private LogSeverity m_logSeverity;
   private final PrintStream m_wrappedStream;
   private final PrintStream m_wrappedWarnStream;
   private final PrintStream m_wrappedErrStream;
   private final IDebugLogFormatter m_formatter;
   private static final int LEFT_JUSTIFY = 36;
   private final HashSet m_debugOnceHashSet;

   public DebugLogStream(PrintStream var1, PrintStream var2, PrintStream var3, IDebugLogFormatter var4) {
      this(var1, var2, var3, var4, LogSeverity.Off);
   }

   public DebugLogStream(PrintStream var1, PrintStream var2, PrintStream var3, IDebugLogFormatter var4, LogSeverity var5) {
      super(var1);
      this.m_debugOnceHashSet = new HashSet();
      this.m_wrappedStream = var1;
      this.m_wrappedWarnStream = var2;
      this.m_wrappedErrStream = var3;
      this.m_formatter = var4;
      this.m_logSeverity = var5;
   }

   public void setLogSeverity(LogSeverity var1) {
      this.m_logSeverity = var1;
   }

   public LogSeverity getLogSeverity() {
      return this.m_logSeverity;
   }

   public PrintStream getWrappedOutStream() {
      return this.m_wrappedStream;
   }

   public PrintStream getWrappedWarnStream() {
      return this.m_wrappedWarnStream;
   }

   public PrintStream getWrappedErrStream() {
      return this.m_wrappedErrStream;
   }

   public IDebugLogFormatter getFormatter() {
      return this.m_formatter;
   }

   protected void write(PrintStream var1, LogSeverity var2, String var3) {
      if (this.isLogEnabled(var2)) {
         String var4 = this.m_formatter.format(var2, "", true, var3);
         if (var4 != null) {
            var1.print(var4);
         }
      }

   }

   protected void writeln(PrintStream var1, LogSeverity var2, String var3) {
      if (this.isLogEnabled(var2)) {
         String var4 = this.m_formatter.format(var2, "", true, var3);
         if (var4 != null) {
            var1.println(var4);
         }
      }

   }

   protected void writeln(PrintStream var1, LogSeverity var2, String var3, Object... var4) {
      if (this.isLogEnabled(var2)) {
         String var5 = this.m_formatter.format(var2, "", true, var3, var4);
         if (var5 != null) {
            var1.println(var5);
         }
      }

   }

   protected void writeWithCallerPrefixln(PrintStream var1, LogSeverity var2, int var3, boolean var4, Object var5) {
      if (this.isLogEnabled(var2)) {
         String var6 = generateCallerPrefix_Internal(var3, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
         String var7 = this.m_formatter.format(var2, var6, var4, "%s", var5);
         if (!var4) {
            if (this.m_debugOnceHashSet.contains(var6)) {
               return;
            }

            this.m_debugOnceHashSet.add(var6);
         }

         if (var7 != null) {
            var1.println(var7);
         }
      }

   }

   protected void writeWithCallerPrefixln(PrintStream var1, LogSeverity var2, int var3, boolean var4, String var5, Object... var6) {
      if (this.isLogEnabled(var2)) {
         String var7 = generateCallerPrefix_Internal(var3, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
         String var8 = String.format(var5, var6);
         String var9 = this.m_formatter.format(var2, var7, var4, var8);
         if (var9 != null) {
            var1.println(var9);
         }
      }

   }

   private void writeln(PrintStream var1, String var2) {
      this.writeln(var1, LogSeverity.General, var2);
   }

   private void writeln(PrintStream var1, String var2, Object... var3) {
      this.writeln(var1, LogSeverity.General, var2, var3);
   }

   public static String generateCallerPrefix() {
      return generateCallerPrefix_Internal(1, 0, DebugLog.isLogTraceFileLocationEnabled(), "");
   }

   private static String generateCallerPrefix_Internal(int var0, int var1, boolean var2, String var3) {
      StackTraceElement var4 = tryGetCallerTraceElement(4 + var0);
      String var10000;
      if (var4 == null) {
         var10000 = StringUtils.leftJustify("(UnknownStack)", var1);
         return var10000 + var3;
      } else {
         String var5 = getStackTraceElementString(var4, var2);
         String var6;
         if (var1 <= 0) {
            var6 = var5 + var3;
            return var6;
         } else {
            var10000 = StringUtils.leftJustify(var5, var1);
            var6 = var10000 + var3;
            return var6;
         }
      }
   }

   public static StackTraceElement tryGetCallerTraceElement(int var0) {
      try {
         StackTraceElement[] var1 = Thread.currentThread().getStackTrace();
         if (var1.length <= var0) {
            return null;
         } else {
            StackTraceElement var2 = var1[var0];
            return var2;
         }
      } catch (SecurityException var3) {
         return null;
      }
   }

   public static String getStackTraceElementString(StackTraceElement var0, boolean var1) {
      if (var0 == null) {
         return "(UnknownStack)";
      } else {
         String var2 = getUnqualifiedClassName(var0.getClassName());
         String var3 = var0.getMethodName();
         String var4;
         if (var0.isNativeMethod()) {
            var4 = " (Native Method)";
         } else if (var1) {
            int var5 = var0.getLineNumber();
            String var6 = var0.getFileName();
            var4 = String.format("(%s:%d)", var6, var5);
         } else {
            var4 = "";
         }

         String var7 = var2 + "." + var3 + var4;
         return var7;
      }
   }

   public static String getTopStackTraceString(Throwable var0) {
      if (var0 == null) {
         return "Null Exception";
      } else {
         StackTraceElement[] var1 = var0.getStackTrace();
         if (var1 != null && var1.length != 0) {
            StackTraceElement var2 = var1[0];
            return getStackTraceElementString(var2, true);
         } else {
            return "No Stack Trace Available";
         }
      }
   }

   public void printStackTrace() {
      this.printStackTrace(0, (String)null);
   }

   public void printStackTrace(String var1) {
      this.printStackTrace(0, var1);
   }

   public void printStackTrace(int var1) {
      this.printStackTrace(var1, (String)null);
   }

   public void printStackTrace(String var1, int var2) {
      this.printStackTrace(var2, var1);
   }

   private void printStackTrace(int var1, String var2) {
      if (var2 != null) {
         this.m_wrappedErrStream.println(var2);
      }

      StackTraceElement[] var3 = Thread.currentThread().getStackTrace();
      this.m_wrappedErrStream.println(StackTraceContainer.getStackTraceString(var3, "\t", var1 + 3, -1));
   }

   private static String getUnqualifiedClassName(String var0) {
      String var1 = var0;
      int var2 = var0.lastIndexOf(46);
      if (var2 > -1 && var2 < var0.length() - 1) {
         var1 = var0.substring(var2 + 1);
      }

      return var1;
   }

   public boolean isEnabled() {
      return this.getLogSeverity() != LogSeverity.Off;
   }

   public boolean isLogEnabled(LogSeverity var1) {
      if (!this.isEnabled()) {
         return false;
      } else if (var1.ordinal() >= DebugLog.getMinimumLogSeverity().ordinal()) {
         return true;
      } else {
         return var1.ordinal() >= this.getLogSeverity().ordinal();
      }
   }

   public void trace(Object var1) {
      this.trace(1, var1);
   }

   public void trace(String var1, Object... var2) {
      this.trace(1, var1, var2);
   }

   public void debugln(Object var1) {
      this.debugln(1, var1);
   }

   public void debugln(String var1, Object... var2) {
      this.debugln(1, var1, var2);
   }

   public void debugOnceln(Object var1) {
      this.debugOnceln(1, var1);
   }

   public void debugOnceln(String var1, Object... var2) {
      this.debugOnceln(1, var1, var2);
   }

   public void noise(Object var1) {
      this.noise(1, var1);
   }

   public void noise(String var1, Object... var2) {
      this.noise(1, var1, var2);
   }

   public void warn(Object var1) {
      this.warn(1, var1);
   }

   public void warn(String var1, Object... var2) {
      this.warn(1, var1, var2);
   }

   public void warnOnce(Object var1) {
      this.warnOnce(1, var1);
   }

   public void warnOnce(String var1, Object... var2) {
      this.warnOnce(1, var1, var2);
   }

   public void error(Object var1) {
      this.error(1, var1);
   }

   public void error(String var1, Object... var2) {
      this.error(1, var1, var2);
   }

   public void debugln(int var1, Object var2) {
      if (Core.bDebug) {
         this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Debug, var1 + 1, true, var2);
      }

   }

   public void debugln(int var1, String var2, Object... var3) {
      if (Core.bDebug) {
         this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Debug, var1 + 1, true, var2, var3);
      }

   }

   public void debugOnceln(int var1, Object var2) {
      if (Core.bDebug) {
         this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Debug, var1 + 1, false, var2);
      }

   }

   public void debugOnceln(int var1, String var2, Object... var3) {
      if (Core.bDebug) {
         this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Debug, var1 + 1, false, var2, var3);
      }

   }

   public void noise(int var1, Object var2) {
      if (Core.bDebug) {
         this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Noise, var1 + 1, true, var2);
      }

   }

   public void noise(int var1, String var2, Object... var3) {
      if (Core.bDebug) {
         this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Noise, var1 + 1, true, var2, var3);
      }

   }

   public void warn(int var1, Object var2) {
      this.writeWithCallerPrefixln(this.m_wrappedWarnStream, LogSeverity.Warning, var1 + 1, true, var2);
   }

   public void warn(int var1, String var2, Object... var3) {
      this.writeWithCallerPrefixln(this.m_wrappedWarnStream, LogSeverity.Warning, var1 + 1, true, var2, var3);
   }

   public void error(int var1, Object var2) {
      this.writeWithCallerPrefixln(this.m_wrappedErrStream, LogSeverity.Error, var1 + 1, true, var2);
   }

   public void error(int var1, String var2, Object... var3) {
      this.writeWithCallerPrefixln(this.m_wrappedErrStream, LogSeverity.Error, var1 + 1, true, var2, var3);
   }

   public void trace(int var1, Object var2) {
      this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Trace, var1 + 1, true, var2);
   }

   public void trace(int var1, String var2, Object... var3) {
      this.writeWithCallerPrefixln(this.m_wrappedStream, LogSeverity.Trace, var1 + 1, true, var2, var3);
   }

   public void warnOnce(int var1, Object var2) {
      this.writeWithCallerPrefixln(this.m_wrappedWarnStream, LogSeverity.Warning, var1 + 1, false, var2);
   }

   public void warnOnce(int var1, String var2, Object... var3) {
      this.writeWithCallerPrefixln(this.m_wrappedWarnStream, LogSeverity.Warning, var1 + 1, false, var2, var3);
   }

   public void print(boolean var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, var1 ? "true" : "false");
   }

   public void print(char var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public void print(int var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public void print(long var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public void print(float var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public void print(double var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public void print(String var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public void print(Object var1) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.valueOf(var1));
   }

   public PrintStream printf(String var1, Object... var2) {
      this.write(this.m_wrappedStream, LogSeverity.General, String.format(var1, var2));
      return this;
   }

   public void println() {
      this.writeln(this.m_wrappedStream, "");
   }

   public void println(boolean var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(char var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(int var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(long var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(float var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(double var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(char[] var1) {
      this.writeln(this.m_wrappedStream, "%s", String.valueOf(var1));
   }

   public void println(String var1) {
      this.writeln(this.m_wrappedStream, var1);
   }

   public void println(Object var1) {
      this.writeln(this.m_wrappedStream, "%s", var1);
   }

   public void println(String var1, Object... var2) {
      this.writeln(this.m_wrappedStream, LogSeverity.General, var1, var2);
   }

   public void printUnitTest(String var1, boolean var2, Object... var3) {
      if (!var2) {
         this.error(var1 + ", fail", var3);
      } else {
         this.println(var1 + ", pass", var3);
      }

   }

   public void printException(Throwable var1, String var2, LogSeverity var3) {
      this.printException(var1, var2, generateCallerPrefix(), var3);
   }

   public void printException(Throwable var1, String var2, String var3, LogSeverity var4) {
      if (var1 == null) {
         this.warn("Null exception passed.");
      } else if (this.isLogEnabled(var4)) {
         PrintStream var5;
         boolean var6;
         switch (var4) {
            case Trace:
            case General:
               var5 = this.m_wrappedStream;
               var6 = false;
               break;
            case Warning:
               var5 = this.m_wrappedWarnStream;
               var6 = false;
               break;
            default:
               this.error("Unhandled LogSeverity: %s. Defaulted to Error.", String.valueOf(var4));
            case Error:
               var5 = this.m_wrappedErrStream;
               var6 = true;
         }

         if (var6) {
            StringBuilder var7 = new StringBuilder();
            if (var2 != null) {
               var7.append(String.format("%s> Exception thrown%s\t%s at %s. Message: %s", var3, System.lineSeparator(), var1.toString(), getTopStackTraceString(var1), var2));
            } else {
               var7.append(String.format("%s> Exception thrown%s\t%s at %s.", var3, System.lineSeparator(), var1.toString(), getTopStackTraceString(var1)));
            }

            var7.append(System.lineSeparator());
            StackTraceContainer.getStackTraceString(var7, var1, "Stack trace:", "\t", 0, -1);
            this.write(var5, var4, var7.toString());
         } else if (var2 != null) {
            this.writeln(var5, var4, String.format("%s> Exception thrown %s at %s. Message: %s", var3, var1.toString(), getTopStackTraceString(var1), var2));
         } else {
            this.writeln(var5, var4, String.format("%s> Exception thrown %s at %s.", var3, var1.toString(), getTopStackTraceString(var1)));
         }

      }
   }
}
