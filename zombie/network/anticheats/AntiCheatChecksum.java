package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatChecksum extends AbstractAntiCheat {
   public AntiCheatChecksum() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      return var1.checksumState != UdpConnection.ChecksumState.Done ? "invalid checksum" : var3;
   }
}
