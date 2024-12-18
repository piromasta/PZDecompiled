package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatHitWeaponRange extends AbstractAntiCheat {
   private static final float predictedAdditionalRange = 1.0F;

   public AntiCheatHitWeaponRange() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      HandWeapon var5 = (HandWeapon)Type.tryCastTo(var4.getInventoryItem(), HandWeapon.class);
      if (var5 == null) {
         return "weapon not found";
      } else {
         return var4.getDistance() - 1.0F > var5.getMaxRange() ? String.format("distance=%f > range=%f", var4.getDistance(), var5.getMaxRange()) : var3;
      }
   }

   public interface IAntiCheat {
      InventoryItem getInventoryItem();

      float getDistance();
   }
}
