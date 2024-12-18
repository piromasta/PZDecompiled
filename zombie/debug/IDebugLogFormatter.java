package zombie.debug;

public interface IDebugLogFormatter {
   String format(LogSeverity var1, String var2, boolean var3, String var4);

   String format(LogSeverity var1, String var2, boolean var3, String var4, Object... var5);
}
