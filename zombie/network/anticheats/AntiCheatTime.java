package zombie.network.anticheats;

import zombie.GameTime;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTime extends AbstractAntiCheat {
   public static int skip = 0;

   public AntiCheatTime() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      var1.validator.gametimeTimeoutReset();
      IAntiCheat var4 = (IAntiCheat)var2;
      if (!GameServer.bFastForward) {
         if (skip == 0) {
            float var5 = var4.getMultiplier();
            if (Math.abs(GameTime.instance.getMultiplier() / GameTime.getInstance().FPSMultiplier - var5) > 1.0F) {
               return "invalid time";
            }
         } else {
            --skip;
         }
      }

      return var3;
   }

   public interface IAntiCheat {
      float getMultiplier();
   }
}
