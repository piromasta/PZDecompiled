package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatNone extends AbstractAntiCheat {
   public AntiCheatNone() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      return super.validate(var1, var2);
   }
}
