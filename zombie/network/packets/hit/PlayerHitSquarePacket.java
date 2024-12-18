package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.anticheats.AntiCheatItem;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.Item, AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo}
)
public class PlayerHitSquarePacket extends PlayerHit implements AntiCheatItem.IAntiCheat, AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat {
   public PlayerHitSquarePacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public InventoryItem getInventoryItem() {
      return null;
   }

   public float getDistance() {
      return 0.0F;
   }

   public void process() {
   }
}
