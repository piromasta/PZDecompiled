package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.fields.Hit;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitDamage extends AbstractAntiCheat {
   private static final int MAX_DAMAGE = 100;

   public AntiCheatHitDamage() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      float var5 = var4.getHit().getDamage();
      return var4.getHit().getDamage() > 100.0F ? String.format("damage=%f is too big", var5) : var3;
   }

   public interface IAntiCheat {
      Hit getHit();
   }
}
