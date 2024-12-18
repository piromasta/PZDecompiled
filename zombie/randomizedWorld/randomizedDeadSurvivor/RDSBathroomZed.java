package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

public final class RDSBathroomZed extends RandomizedDeadSurvivorBase {
   private final ArrayList<String> items = new ArrayList();

   public RDSBathroomZed() {
      this.name = "Bathroom Zed";
      this.setChance(12);
      this.items.add("Base.BathTowel");
      this.items.add("Base.Razor");
      this.items.add("Base.Lipstick");
      this.items.add("Base.Comb");
      this.items.add("Base.Hairspray2");
      this.items.add("Base.Toothbrush");
      this.items.add("Base.Cologne");
      this.items.add("Base.Perfume");
      this.items.add("Base.HairDryer");
      this.items.add("Base.StraightRazor");
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getRoom(var1, "bathroom");
      int var3 = 1;
      if (var2.area > 6) {
         var3 = Rand.Next(1, 3);
      }

      this.addZombies(var1, var3, Rand.Next(2) == 0 ? "Bathrobe" : "Naked", (Integer)null, var2);
      this.addRandomItemsOnGround(var2, this.items, Rand.Next(2, 5));
   }
}
