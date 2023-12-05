package zombie.commands.serverCommands;

import zombie.characters.IsoPlayer;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

@CommandName(
   name = "lightning"
)
@CommandArgs(
   optional = "(.+)"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_Lightning"
)
@RequiredRight(
   requiredRights = 60
)
public class LightningCommand extends CommandBase {
   public LightningCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1;
      if (this.getCommandArgsCount() == 0) {
         if (this.connection == null) {
            return "Pass a username";
         }

         var1 = this.getExecutorUsername();
      } else {
         var1 = this.getCommandArg(0);
      }

      IsoPlayer var2 = GameServer.getPlayerByUserNameForCommand(var1);
      if (var2 == null) {
         return "User \"" + var1 + "\" not found";
      } else {
         int var3 = PZMath.fastfloor(var2.getX());
         int var4 = PZMath.fastfloor(var2.getY());
         ClimateManager.getInstance().transmitServerTriggerLightning(var3, var4, false, true, true);
         LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " thunder start");
         return "Lightning triggered";
      }
   }
}
