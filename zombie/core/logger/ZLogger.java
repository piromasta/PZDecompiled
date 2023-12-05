package zombie.core.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ZLogger {
   private final String name;
   private final OutputStreams outputStreams = new OutputStreams();
   private File file = null;
   private static final SimpleDateFormat s_fileNameSdf = new SimpleDateFormat("dd-MM-yy_HH-mm-ss");
   private static final SimpleDateFormat s_logSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
   private static final long s_maxSizeKo = 10000L;

   public ZLogger(String var1, boolean var2) {
      this.name = var1;

      try {
         String var10003 = LoggerManager.getLogsDir();
         this.file = new File(var10003 + File.separator + getLoggerName(var1) + ".txt");
         this.outputStreams.file = new PrintStream(this.file);
      } catch (FileNotFoundException var4) {
         var4.printStackTrace();
      }

      if (var2) {
         this.outputStreams.console = System.out;
      }

   }

   private static String getLoggerName(String var0) {
      String var10000 = s_fileNameSdf.format(Calendar.getInstance().getTime());
      return var10000 + "_" + var0;
   }

   public void write(String var1) {
      this.write(var1, (String)null);
   }

   public void write(String var1, String var2) {
      this.write(var1, var2, false);
   }

   public void write(String var1, String var2, boolean var3) {
      try {
         this.writeUnsafe(var1, var2, var3);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }

   public synchronized void writeUnsafe(String var1, String var2, boolean var3) throws Exception {
      StringBuilder var4 = new StringBuilder();
      var4.setLength(0);
      if (!var3) {
         var4.append("[").append(s_logSdf.format(Calendar.getInstance().getTime())).append("]");
      }

      if (!StringUtils.isNullOrEmpty(var2)) {
         var4.append("[").append(var2).append("]");
      }

      int var5 = var1.length();
      if (var1.lastIndexOf(10) == var1.length() - 1) {
         --var5;
      }

      if (!var3) {
         var4.append(" ").append(var1, 0, var5).append(".");
      } else {
         var4.append(var1, 0, var5);
      }

      this.outputStreams.println(var4.toString());
      this.checkSizeUnsafe();
   }

   public synchronized void write(Exception var1) {
      var1.printStackTrace(this.outputStreams.file);
      this.checkSize();
   }

   private synchronized void checkSize() {
      try {
         this.checkSizeUnsafe();
      } catch (Exception var2) {
         DebugLog.General.error("Exception thrown checking log file size.");
         DebugLog.General.error(var2);
         var2.printStackTrace();
      }

   }

   private synchronized void checkSizeUnsafe() throws Exception {
      long var1 = this.file.length() / 1024L;
      if (var1 > 10000L) {
         this.outputStreams.file.close();
         String var10003 = LoggerManager.getLogsDir();
         this.file = new File(var10003 + File.separator + getLoggerName(this.name) + ".txt");
         this.outputStreams.file = new PrintStream(this.file);
      }

   }

   private static class OutputStreams {
      public PrintStream file;
      public PrintStream console;

      private OutputStreams() {
      }

      public void println(String var1) {
         if (this.file != null) {
            this.file.println(var1);
            this.file.flush();
         }

         if (this.console != null) {
            this.console.println(var1);
         }

      }
   }
}
