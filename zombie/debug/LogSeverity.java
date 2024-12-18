package zombie.debug;

import java.util.ArrayList;
import java.util.Arrays;

public enum LogSeverity {
   Trace("TRACE: "),
   Noise("NOISE: "),
   Debug("DEBUG: "),
   General("LOG  : "),
   Warning("WARN : "),
   Error("ERROR: "),
   Off("!OFF!");

   public static final LogSeverity All = Trace;
   public final String LogPrefix;

   private LogSeverity(String var3) {
      this.LogPrefix = var3;
   }

   public static ArrayList<LogSeverity> getValueList() {
      ArrayList var0 = new ArrayList(Arrays.asList(values()));
      return var0;
   }
}
