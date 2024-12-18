package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.secure.PZcrypt;
import zombie.network.ServerWorldDatabase;

@CommandName(
   name = "setpassword"
)
@CommandArgs(
   required = {"(.+)", "(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_SetPassword"
)
@RequiredCapability(
   requiredCapability = Capability.ManipulateWhitelist
)
public class SetPasswordCommand extends CommandBase {
   public SetPasswordCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      String var2 = PZcrypt.hash(ServerWorldDatabase.encrypt(this.getCommandArg(1)));
      ZLogger var10000 = LoggerManager.getLogger("admin");
      String var10001 = this.getExecutorUsername();
      var10000.write(var10001 + " changing password for the " + var1.trim());

      try {
         return ServerWorldDatabase.instance.changePassword(var1.trim(), var2.trim());
      } catch (SQLException var4) {
         var4.printStackTrace();
         return "exception occurs";
      }
   }
}
