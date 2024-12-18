package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoFire;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSmoke extends AbstractAntiCheat {
   public AntiCheatSmoke() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      if (var4.getSmoke() && !IsoFire.CanAddSmoke(var4.getSquare().getSquare(), var4.getIgnition())) {
         return "invalid square";
      } else {
         return !var1.RelevantTo(var4.getSquare().getX(), var4.getSquare().getY()) ? "irrelevant square" : var3;
      }
   }

   public interface IAntiCheat {
      boolean getSmoke();

      boolean getIgnition();

      Square getSquare();
   }
}