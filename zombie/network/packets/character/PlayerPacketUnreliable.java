package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;

@PacketSetting(
   ordering = 0,
   priority = 2,
   reliability = 0,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.Power, AntiCheat.Role, AntiCheat.Speed, AntiCheat.NoClip}
)
public class PlayerPacketUnreliable extends PlayerPacket {
   public PlayerPacketUnreliable() {
   }
}
