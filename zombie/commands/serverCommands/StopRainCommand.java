package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.iso.weather.ClimateManager;

@CommandName(
   name = "stoprain"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_StopRain"
)
@RequiredCapability(
   requiredCapability = Capability.StartStopRain
)
public class StopRainCommand extends CommandBase {
   public StopRainCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      ClimateManager.getInstance().transmitServerStopRain();
      LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " stopped rain");
      return "Rain stopped";
   }
}
