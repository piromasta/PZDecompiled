package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoGameCharacter;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.list.PZArrayUtil;

public final class RDSSpecificProfession extends RandomizedDeadSurvivorBase {
   private final ArrayList<String> specificProfessionDistribution = new ArrayList();

   public void randomizeDeadSurvivor(BuildingDef var1) {
      String var2 = (String)PZArrayUtil.pickRandom((List)this.specificProfessionDistribution);
      ItemPickerJava.ItemPickerRoom var3 = (ItemPickerJava.ItemPickerRoom)ItemPickerJava.rooms.get(var2);
      String var4 = var3.outfit;
      IsoGridSquare var5 = var1.getFreeSquareInRoom();
      if (var5 != null) {
         IsoDeadBody var6;
         if (var4 != null && Rand.Next(2) == 0) {
            var6 = createRandomDeadBody(var5, (IsoDirections)null, 0, 0, var4);
         } else {
            var6 = createRandomDeadBody(var5.getX(), var5.getY(), var5.getZ(), (IsoDirections)null, 0);
         }

         if (var6 != null) {
            ItemPickerJava.rollItem((ItemPickerJava.ItemPickerContainer)var3.Containers.get("body"), var6.getContainer(), true, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerRoom)null);
         }
      }
   }

   public RDSSpecificProfession() {
      this.specificProfessionDistribution.add("Carpenter");
      this.specificProfessionDistribution.add("Electrician");
      this.specificProfessionDistribution.add("Farmer");
      this.specificProfessionDistribution.add("Nurse");
      this.specificProfessionDistribution.add("Chef");
   }
}
