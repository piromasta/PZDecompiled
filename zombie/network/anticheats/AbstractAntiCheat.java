package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public abstract class AbstractAntiCheat {
   public AbstractAntiCheat() {
   }

   public boolean update(UdpConnection var1) {
      return true;
   }

   public void react(UdpConnection var1, INetworkPacket var2) {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      return null;
   }

   public boolean preUpdate(UdpConnection var1) {
      return true;
   }
}
