package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
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
public class PlayerHitAnimalPacket extends PlayerHit implements AntiCheatHitDamage.IAntiCheat, AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat, AntiCheatHitWeaponRange.IAntiCheat, AntiCheatHitWeaponRate.IAntiCheat {
   public PlayerHitAnimalPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public Hit getHit() {
      return null;
   }

   public InventoryItem getInventoryItem() {
      return null;
   }

   public float getDistance() {
      return 0.0F;
   }

   public IsoPlayer getWielder() {
      return this.wielder.getPlayer();
   }

   public IsoGameCharacter getTarget() {
      return null;
   }

   public void process() {
   }
}
