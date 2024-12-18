package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class AnimalUpdateReliablePacket extends AnimalUpdatePacket {
   public AnimalUpdateReliablePacket() {
   }
}
