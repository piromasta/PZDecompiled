package zombie.debug;

import java.io.PrintStream;
import zombie.util.Type;

public class AutoRepiperDebugLogStream extends DebugLogStream {
   private final DebugType m_defaultDebugType;

   public AutoRepiperDebugLogStream(PrintStream var1, DebugType var2, LogSeverity var3) {
      super(var1, (PrintStream)null, (PrintStream)null, (IDebugLogFormatter)null, var3);
      this.m_defaultDebugType = var2;
   }

   public DebugType getDefaultDebugType() {
      return this.m_defaultDebugType;
   }

   protected DebugType parseRepipeDirection(Object var1) {
      String var2 = (String)Type.tryCastTo(var1, String.class);
      if (var2 == null) {
         return this.getDefaultDebugType();
      } else {
         int var3 = var2.indexOf(58);
         if (var3 <= 0) {
            return this.getDefaultDebugType();
         } else {
            String var4 = var2.substring(0, var3);
            if (var4.indexOf(10) <= -1 && var4.indexOf(32) <= -1 && var4.indexOf(9) <= -1) {
               DebugType var5 = this.getDefaultDebugType();
               DebugType[] var6 = DebugType.values();
               int var7 = var6.length;

               for(int var8 = 0; var8 < var7; ++var8) {
                  DebugType var9 = var6[var8];
                  if (var9.name().equalsIgnoreCase(var4)) {
                     var5 = var9;
                     break;
                  }
               }

               return var5;
            } else {
               return this.getDefaultDebugType();
            }
         }
      }
   }

   protected LogSeverity parseRepipedLogSeverity(Object var1, LogSeverity var2) {
      String var3 = (String)Type.tryCastTo(var1, String.class);
      if (var3 == null) {
         return var2;
      } else {
         int var4 = 0;

         for(int var5 = 0; var5 < 2; ++var5) {
            int var6 = var3.indexOf(58, var4);
            if (var6 <= 0) {
               break;
            }

            String var7 = var3.substring(var4, var6);
            LogSeverity var8 = this.parseRepipedLogSeverityExact(var7, var2);
            if (var8 != null) {
               return var8;
            }

            var4 = var6 + 1;
         }

         return var2;
      }
   }

   private LogSeverity parseRepipedLogSeverityExact(String var1, LogSeverity var2) {
      if (var1.indexOf(10) <= -1 && var1.indexOf(32) <= -1 && var1.indexOf(9) <= -1) {
         if (var1.equalsIgnoreCase("TRACE")) {
            return LogSeverity.Trace;
         } else if (var1.equalsIgnoreCase("NOISE")) {
            return LogSeverity.Noise;
         } else if (var1.equalsIgnoreCase("DEBUG")) {
            return LogSeverity.Debug;
         } else if (var1.equalsIgnoreCase("WARN")) {
            return LogSeverity.Warning;
         } else {
            return var1.equalsIgnoreCase("ERROR") ? LogSeverity.Error : null;
         }
      } else {
         return var2;
      }
   }

   protected PrintStream getRepipedStream(PrintStream var1, DebugType var2) {
      return this.getRepipedStream(var1, var2.getLogStream());
   }

   protected PrintStream getRepipedStream(PrintStream var1, DebugLogStream var2) {
      if (var1 == this.getWrappedOutStream()) {
         return var2.getWrappedOutStream();
      } else if (var1 == this.getWrappedWarnStream()) {
         return var2.getWrappedWarnStream();
      } else {
         return var1 == this.getWrappedErrStream() ? var2.getWrappedErrStream() : var2.getWrappedOutStream();
      }
   }

   protected void write(PrintStream var1, LogSeverity var2, String var3) {
      DebugType var4 = this.parseRepipeDirection(var3);
      PrintStream var5 = this.getRepipedStream(var1, var4);
      LogSeverity var6 = this.parseRepipedLogSeverity(var3, var2);
      var4.getLogStream().write(var5, var6, var3);
   }

   protected void writeln(PrintStream var1, LogSeverity var2, String var3) {
      DebugType var4 = this.parseRepipeDirection(var3);
      PrintStream var5 = this.getRepipedStream(var1, var4);
      LogSeverity var6 = this.parseRepipedLogSeverity(var3, var2);
      var4.getLogStream().writeln(var5, var6, var3);
   }

   protected void writeln(PrintStream var1, LogSeverity var2, String var3, Object... var4) {
      DebugType var5 = this.parseRepipeDirection(var3);
      PrintStream var6 = this.getRepipedStream(var1, var5);
      LogSeverity var7 = this.parseRepipedLogSeverity(var3, var2);
      var5.getLogStream().writeln(var6, var7, var3, var4);
   }

   protected void writeWithCallerPrefixln(PrintStream var1, LogSeverity var2, int var3, boolean var4, Object var5) {
      DebugType var6 = this.parseRepipeDirection(var5);
      PrintStream var7 = this.getRepipedStream(var1, var6);
      LogSeverity var8 = this.parseRepipedLogSeverity(var5, var2);
      var6.getLogStream().writeWithCallerPrefixln(var7, var8, var3 + 1, var4, var5);
   }

   protected void writeWithCallerPrefixln(PrintStream var1, LogSeverity var2, int var3, boolean var4, String var5, Object... var6) {
      DebugType var7 = this.parseRepipeDirection(var5);
      PrintStream var8 = this.getRepipedStream(var1, var7);
      LogSeverity var9 = this.parseRepipedLogSeverity(var5, var2);
      var7.getLogStream().writeWithCallerPrefixln(var8, var9, var3, var4, var5, var6);
   }
}
