package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatSafety;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.fields.Hit;
import zombie.network.fields.IMovable;
import zombie.vehicles.BaseVehicle;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.HitDamage, AntiCheat.HitShortDistance, AntiCheat.Safety, AntiCheat.Speed}
)
public class VehicleHitPlayerPacket extends VehicleHit implements AntiCheatHitDamage.IAntiCheat, AntiCheatHitShortDistance.IAntiCheat, AntiCheatSafety.IAntiCheat, AntiCheatSpeed.IAntiCheat {
   public VehicleHitPlayerPacket() {
   }

   public void set(IsoPlayer var1, IsoPlayer var2, BaseVehicle var3, float var4, boolean var5, int var6, float var7, boolean var8) {
   }

   public float getDistance() {
      return 0.0F;
   }

   public IsoGameCharacter getTarget() {
      return null;
   }

   public IsoPlayer getWielder() {
      return null;
   }

   public IMovable getMovable() {
      return null;
   }

   public boolean isRelevant(UdpConnection var1) {
      return false;
   }

   public void preProcess() {
   }

   public void process() {
   }

   public void postProcess() {
   }

   public Hit getHit() {
      return null;
   }
}
