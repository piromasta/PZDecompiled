package zombie.commands.serverCommands;

import zombie.GameTime;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandNames({@CommandName(
   name = "setTimeSpeed"
), @CommandName(
   name = "sts"
)})
@CommandArgs(
   required = {"(\\d+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_SetTimeSpeed"
)
@RequiredCapability(
   requiredCapability = Capability.ConnectWithDebug
)
public class SetTimeSpeedCommand extends CommandBase {
   public SetTimeSpeedCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      float var1 = Float.parseFloat(this.getCommandArg(0));
      GameTime.getInstance().setMultiplier(var1);
      INetworkPacket.sendToAll(PacketTypes.PacketType.SetMultiplier, (UdpConnection)null);
      return "Multiplier was set on the following value: " + var1;
   }
}
