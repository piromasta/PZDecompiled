package zombie.core.znet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

public class ZNet {
   private static final SimpleDateFormat s_logSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");

   public ZNet() {
   }

   public static native void init();

   private static native void setLogLevel(int var0);

   public static void SetLogLevel(int var0) {
      LogSeverity var1;
      switch (var0) {
         case 0:
            var1 = LogSeverity.Warning;
            break;
         case 1:
            var1 = LogSeverity.General;
            break;
         case 2:
            var1 = LogSeverity.Debug;
            break;
         default:
            var1 = LogSeverity.Error;
      }

      DebugLog.enableLog(DebugType.Network, var1);
   }

   public static void SetLogLevel(LogSeverity var0) {
      setLogLevel(var0.ordinal());
   }

   private static void logPutsCallback(String var0) {
      String var1 = s_logSdf.format(Calendar.getInstance().getTime());
      DebugLog.Network.print("[" + var1 + "] > " + var0);
      System.out.flush();
   }
}
