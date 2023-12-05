package zombie.network;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;

public class ConnectionManager {
   private static final SimpleDateFormat s_logSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");

   public ConnectionManager() {
   }

   public static void log(String var0, String var1, UdpConnection var2) {
      DebugLog.Network.println("[%s] > ConnectionManager: [%s] \"%s\" connection: %s", s_logSdf.format(Calendar.getInstance().getTime()), var0, var1, GameClient.bClient ? GameClient.connection : var2);
   }
}
