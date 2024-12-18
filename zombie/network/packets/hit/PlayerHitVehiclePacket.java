package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.CombatManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.fields.Vehicle;
import zombie.vehicles.BaseVehicle;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo}
)
public class PlayerHitVehiclePacket extends PlayerHit implements AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat {
   @JSONField
   protected final Vehicle vehicle = new Vehicle();
   @JSONField
   protected float damage;

   public PlayerHitVehiclePacket() {
   }

   public void set(IsoPlayer var1, BaseVehicle var2, HandWeapon var3, boolean var4, float var5) {
      super.set(var1, var3, var4);
      this.vehicle.set(var2);
      this.damage = var5;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.damage = var1.getFloat();
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      this.vehicle.write(var1);
      var1.putFloat(this.damage);
   }

   public void process() {
      if (GameServer.bServer) {
         this.vehicle.getVehicle().processHit(this.wielder.getCharacter(), this.weapon.getWeapon(), this.damage);
         CombatManager.getInstance().processMaintanenceCheck(this.wielder.getCharacter(), this.weapon.getWeapon(), this.vehicle.getVehicle());
      }

   }

   public InventoryItem getInventoryItem() {
      return null;
   }

   public float getDistance() {
      return 0.0F;
   }
}
