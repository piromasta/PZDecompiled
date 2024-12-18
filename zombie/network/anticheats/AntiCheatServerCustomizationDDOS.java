package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatServerCustomizationDDOS extends AbstractAntiCheat {
   public AntiCheatServerCustomizationDDOS() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      return System.currentTimeMillis() / 1000L - var4.getLastConnect() <= 3600L ? "invalid rate" : var3;
   }

   public interface IAntiCheat {
      long getLastConnect();
   }
}
