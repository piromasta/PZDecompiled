package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitLongDistance extends AbstractAntiCheat {
   public AntiCheatHitLongDistance() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      float var5 = var4.getDistance();
      int var6 = var1.ReleventRange * 8;
      return var5 > (float)var6 ? String.format("distance=%f > range=%d", var5, var6) : var3;
   }

   public interface IAntiCheat {
      float getDistance();
   }
}
