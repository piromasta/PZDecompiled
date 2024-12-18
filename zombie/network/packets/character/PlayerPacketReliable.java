package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.Power, AntiCheat.Role, AntiCheat.Speed, AntiCheat.NoClip, AntiCheat.Player}
)
public class PlayerPacketReliable extends PlayerPacket {
   public PlayerPacketReliable() {
   }
}
