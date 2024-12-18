package zombie.debug;

class GenericDebugLogFormatter implements IDebugLogFormatter {
   private final DebugType m_debugType;

   public GenericDebugLogFormatter(DebugType var1) {
      this.m_debugType = var1;
   }

   public String format(LogSeverity var1, String var2, boolean var3, String var4) {
      return DebugLog.formatString(this.m_debugType, var1, var2, var3, var4);
   }

   public String format(LogSeverity var1, String var2, boolean var3, String var4, Object... var5) {
      return DebugLog.formatString(this.m_debugType, var1, var2, var3, var4, var5);
   }
}
