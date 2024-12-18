package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.Translator;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSSirTwiggy extends RandomizedZoneStoryBase {
   public RZSSirTwiggy() {
      this.name = "SirTwiggy";
      this.chance = 100;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.SirTwiggy.toString());
      this.alwaysDo = true;
   }

   public void randomizeZoneStory(Zone var1) {
      IsoGridSquare var2 = this.getRandomExtraFreeSquare(this, var1);
      ArrayList var3 = this.addZombiesOnSquare(1, "Sir_Twiggy", 0, var2);
      IsoZombie var4 = (IsoZombie)var3.get(0);
      var4.getHumanVisual().setSkinTextureIndex(0);
      SurvivorDesc var5 = var4.getDescriptor();
      if (var5 != null) {
         var5.setForename("Ted");
         var5.setSurname("Wigginton");
         InventoryItem var6 = InventoryItemFactory.CreateItem("Base.OfficialDocument");
         var6.setName(Translator.getText("IGUI_Document_Twiggys"));
         var4.addItemToSpawnAtDeath(var6);
      }
   }
}
