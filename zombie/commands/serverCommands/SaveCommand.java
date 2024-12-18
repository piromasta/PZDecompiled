package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

@CommandName(
   name = "save"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_Save"
)
@RequiredCapability(
   requiredCapability = Capability.SaveWorld
)
public class SaveCommand extends CommandBase {
   public SaveCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      ServerMap.instance.QueueSaveAll();
      INetworkPacket.sendToAll(PacketTypes.PacketType.StartPause, (UdpConnection)null);
      return "World saved";
   }
}
