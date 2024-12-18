package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatOwner;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.HitShortDistance, AntiCheat.Owner}
)
public class AnimalHitAnimalPacket extends AnimalHit implements AntiCheatHitShortDistance.IAntiCheat, AntiCheatOwner.IAntiCheat {
   public AnimalHitAnimalPacket() {
   }

   public boolean set(Object... var1) {
      return false;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public float getDistance() {
      return 0.0F;
   }

   public IsoGameCharacter getCharacter() {
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
