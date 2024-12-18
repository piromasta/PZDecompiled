package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.characters.AttackRateChecker;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatHitWeaponRate extends AbstractAntiCheat {
   public AntiCheatHitWeaponRate() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      HandWeapon var5 = (HandWeapon)Type.tryCastTo(var4.getInventoryItem(), HandWeapon.class);
      if (var5 == null) {
         return "weapon not found";
      } else {
         AttackRateChecker var6 = var4.getWielder().getNetworkCharacterAI().attackRateChecker;
         float var7 = var6.check(var4.getTarget().getOnlineID(), var5.getProjectileCount());
         float var8 = 0.5F;
         if (var5.isAimedFirearm()) {
            if ("Auto".equals(var5.getFireMode())) {
               var8 = 0.1F;
            } else if (!var5.isTwoHandWeapon()) {
               var8 = 0.25F;
            } else {
               var8 = 0.3F;
            }
         }

         return var7 < var8 ? String.format("rate=%f < speed=%f", var7, var8) : var3;
      }
   }

   public interface IAntiCheat {
      InventoryItem getInventoryItem();

      float getDistance();

      IsoPlayer getWielder();

      IsoGameCharacter getTarget();
   }
}
