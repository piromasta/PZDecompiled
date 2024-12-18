package zombie.commands.serverCommands;

import java.util.Iterator;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.characters.animals.IsoAnimal;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.animal.AnimalInstanceManager;

@CommandName(
   name = "list"
)
@CommandArgs(
   required = {"(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_List"
)
@RequiredCapability(
   requiredCapability = Capability.LoginOnServer
)
public class ListCommand extends CommandBase {
   public ListCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   public static String List(String var0) {
      StringBuilder var1 = new StringBuilder();
      if ("animals".equalsIgnoreCase(var0)) {
         if (GameServer.bServer) {
            var1.append("Server animals list:\n");
         } else if (GameClient.bClient) {
            var1.append("Client animals list:\n");
         }

         Iterator var2 = AnimalInstanceManager.getInstance().getAnimals().iterator();

         while(var2.hasNext()) {
            IsoAnimal var3 = (IsoAnimal)var2.next();
            if (var3 != null) {
               var1.append("* ").append(var3.getOnlineID()).append(" ").append(var3.getAnimalType()).append(" ").append(var3.getBreed().getName()).append(" ").append(var3.getHutch() == null ? "world" : "hutch").append("\n");
            }
         }
      } else {
         var1.append("Subsystem error: ").append(var0);
      }

      return var1.toString();
   }

   protected String Command() {
      return List(this.getCommandArg(0));
   }
}
