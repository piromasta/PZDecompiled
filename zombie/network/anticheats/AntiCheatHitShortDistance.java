package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitShortDistance extends AbstractAntiCheat {
   private static final int MAX_RELEVANT_RANGE = 10;

   public AntiCheatHitShortDistance() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      float var5 = var4.getDistance();
      return var5 > 10.0F ? String.format("distance=%f > range=%d", var5, 10) : var3;
   }

   public interface IAntiCheat {
      float getDistance();
   }
}
