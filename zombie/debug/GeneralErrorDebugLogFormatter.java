package zombie.debug;

class GeneralErrorDebugLogFormatter implements IDebugLogFormatter {
   GeneralErrorDebugLogFormatter() {
   }

   public String format(LogSeverity var1, String var2, boolean var3, String var4) {
      return DebugLog.formatString(DebugType.General, var1, var2, var3, var4);
   }

   public String format(LogSeverity var1, String var2, boolean var3, String var4, Object... var5) {
      return DebugLog.formatString(DebugType.General, var1, var2, var3, var4, var5);
   }
}
