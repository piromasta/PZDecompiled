package zombie.commands.serverCommands;

import zombie.AmbientStreamManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;

@CommandName(
   name = "gunshot"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_Gunshot"
)
@RequiredCapability(
   requiredCapability = Capability.MakeEventsAlarmGunshot
)
public class GunShotCommand extends CommandBase {
   public GunShotCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      AmbientStreamManager.instance.doGunEvent();
      LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " did gunshot");
      return "Gunshot fired";
   }
}
