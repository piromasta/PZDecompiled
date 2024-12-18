package zombie.commands.serverCommands;

import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

@CommandName(
   name = "createhorde2"
)
@CommandArgs(
   varArgs = true
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_CreateHorde2"
)
@RequiredCapability(
   requiredCapability = Capability.CreateHorde
)
public class CreateHorde2Command extends CommandBase {
   public CreateHorde2Command(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      int var1 = -1;
      int var2 = -1;
      int var3 = -1;
      int var4 = -1;
      int var5 = -1;
      boolean var6 = false;
      boolean var7 = false;
      boolean var8 = false;
      boolean var9 = false;
      boolean var10 = false;
      float var11 = 1.0F;
      String var12 = null;

      int var17;
      for(int var13 = 0; var13 < this.getCommandArgsCount() - 1; var13 += 2) {
         String var14 = this.getCommandArg(var13);
         String var15 = this.getCommandArg(var13 + 1);
         switch (var14) {
            case "-count":
               var1 = PZMath.tryParseInt(var15, -1);
               break;
            case "-radius":
               var2 = PZMath.tryParseInt(var15, -1);
               break;
            case "-x":
               var3 = PZMath.tryParseInt(var15, -1);
               break;
            case "-y":
               var4 = PZMath.tryParseInt(var15, -1);
               break;
            case "-z":
               var5 = PZMath.tryParseInt(var15, -1);
               break;
            case "-outfit":
               var12 = StringUtils.discardNullOrWhitespace(var15);
               break;
            case "-crawler":
               var6 = !"false".equals(var15);
               break;
            case "-isFallOnFront":
               var7 = !"false".equals(var15);
               break;
            case "-isFakeDead":
               var8 = !"false".equals(var15);
               break;
            case "-knockedDown":
               var9 = !"false".equals(var15);
               break;
            case "-isInvulnerable":
               var10 = !"false".equals(var15);
               break;
            case "-health":
               var11 = Float.valueOf(var15);
               break;
            default:
               return this.getHelp();
         }
      }

      if (var1 > 500) {
         System.out.println("Zombie spawn commands are capped at 500 maximum zombies per command");
      }

      var1 = PZMath.clamp(var1, 1, 500);
      IsoGridSquare var18 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var18 == null) {
         return "invalid location";
      } else if (var12 != null && OutfitManager.instance.FindMaleOutfit(var12) == null && OutfitManager.instance.FindFemaleOutfit(var12) == null) {
         return "invalid outfit";
      } else {
         Integer var19 = null;
         if (var12 != null) {
            if (OutfitManager.instance.FindFemaleOutfit(var12) == null) {
               var19 = -2147483648;
            } else if (OutfitManager.instance.FindMaleOutfit(var12) == null) {
               var19 = 2147483647;
            }
         }

         for(int var20 = 0; var20 < var1; ++var20) {
            int var16 = var2 <= 0 ? var3 : Rand.Next(var3 - var2, var3 + var2 + 1);
            var17 = var2 <= 0 ? var4 : Rand.Next(var4 - var2, var4 + var2 + 1);
            LuaManager.GlobalObject.addZombiesInOutfit(var16, var17, var5, 1, var12, var19, var6, var7, var8, var9, var10, false, var11);
         }

         LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created a horde of " + var1 + " zombies near " + var3 + "," + var4, "IMPORTANT");
         return "Horde spawned.";
      }
   }
}
