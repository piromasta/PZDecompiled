package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.CombatManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.fields.Thumpable;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 1,
   anticheats = {AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo}
)
public class PlayerHitObjectPacket extends PlayerHit implements AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat {
   @JSONField
   protected final Thumpable thumpable = new Thumpable();

   public PlayerHitObjectPacket() {
   }

   public void set(IsoPlayer var1, IsoObject var2, HandWeapon var3, boolean var4) {
      super.set(var1, var3, var4);
      this.thumpable.set(var2);
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      this.thumpable.write(var1);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.thumpable.parse(var1, var2);
   }

   public void process() {
      CombatManager.getInstance().processMaintanenceCheck(this.wielder.getCharacter(), this.weapon.getWeapon(), this.thumpable.getIsoObject());
   }

   public InventoryItem getInventoryItem() {
      return null;
   }

   public float getDistance() {
      return IsoUtils.DistanceTo(this.thumpable.getX(), this.thumpable.getY(), this.wielder.getX(), this.wielder.getY());
   }
}
