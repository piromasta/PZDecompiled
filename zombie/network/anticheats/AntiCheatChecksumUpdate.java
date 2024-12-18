package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;

public class AntiCheatChecksumUpdate extends AbstractAntiCheat {
   public AntiCheatChecksumUpdate() {
   }

   public boolean preUpdate(UdpConnection var1) {
      if (var1.checksumState == UdpConnection.ChecksumState.Different && var1.checksumTime + 8000L < System.currentTimeMillis()) {
         DebugLog.log("timed out connection because checksum was different");
         var1.checksumState = UdpConnection.ChecksumState.Init;
         return false;
      } else {
         return true;
      }
   }
}
