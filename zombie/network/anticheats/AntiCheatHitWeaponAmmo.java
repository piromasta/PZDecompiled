package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatHitWeaponAmmo extends AbstractAntiCheat {
   public AntiCheatHitWeaponAmmo() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      HandWeapon var5 = (HandWeapon)Type.tryCastTo(var4.getInventoryItem(), HandWeapon.class);
      if (var5 == null) {
         return "weapon not found";
      } else {
         return var5.isAimedFirearm() && var5.getCurrentAmmoCount() <= 0 ? String.format("ammo=%d", var5.getCurrentAmmoCount()) : var3;
      }
   }

   public interface IAntiCheat {
      InventoryItem getInventoryItem();
   }
}
