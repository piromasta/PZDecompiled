package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.iso.weather.ClimateManager;

@CommandName(
   name = "startstorm"
)
@CommandArgs(
   optional = "(\\d+)"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_StartStorm"
)
@RequiredCapability(
   requiredCapability = Capability.StartStopRain
)
public class StartStormCommand extends CommandBase {
   public StartStormCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      float var1 = 24.0F;
      if (this.getCommandArgsCount() == 1) {
         try {
            var1 = Float.parseFloat(this.getCommandArg(0));
         } catch (Throwable var3) {
            ExceptionLogger.logException(var3);
            return "Invalid duration value";
         }
      }

      ClimateManager.getInstance().transmitServerStopWeather();
      ClimateManager.getInstance().transmitServerTriggerStorm(var1);
      LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " started thunderstorm");
      return "Thunderstorm started";
   }
}
