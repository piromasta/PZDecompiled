package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.fields.Hit;
import zombie.network.fields.IMovable;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.HitDamage, AntiCheat.HitShortDistance, AntiCheat.Speed}
)
public class VehicleHitZombiePacket extends VehicleHit implements AntiCheatHitDamage.IAntiCheat, AntiCheatHitShortDistance.IAntiCheat, AntiCheatSpeed.IAntiCheat {
   public VehicleHitZombiePacket() {
   }

   public boolean set(Object... var1) {
      return false;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public Hit getHit() {
      return null;
   }

   public float getDistance() {
      return 0.0F;
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
}
