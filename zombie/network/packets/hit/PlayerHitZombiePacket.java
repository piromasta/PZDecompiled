package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.anticheats.AntiCheatHitWeaponRange;
import zombie.network.anticheats.AntiCheatHitWeaponRate;
import zombie.network.fields.Hit;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.HitDamage, AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo, AntiCheat.HitWeaponRange, AntiCheat.HitWeaponRate}
)
public class PlayerHitZombiePacket extends PlayerHit implements AntiCheatHitDamage.IAntiCheat, AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat, AntiCheatHitWeaponRange.IAntiCheat, AntiCheatHitWeaponRate.IAntiCheat {
   public PlayerHitZombiePacket() {
   }

   public Hit getHit() {
      return null;
   }

   public float getDistance() {
      return 0.0F;
   }

   public IsoPlayer getWielder() {
      return null;
   }

   public IsoGameCharacter getTarget() {
      return null;
   }

   public InventoryItem getInventoryItem() {
      return null;
   }

   public void process() {
   }
}
