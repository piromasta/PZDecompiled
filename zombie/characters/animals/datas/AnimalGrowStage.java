package zombie.characters.animals.datas;

import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.IsoAnimal;

public class AnimalGrowStage {
   public int ageToGrow = 0;
   public String nextStage = null;
   public String nextStageMale = null;
   public String stage = null;

   public AnimalGrowStage() {
   }

   public int getAgeToGrow(IsoAnimal var1) {
      AnimalAllele var2 = var1.getUsedGene("ageToGrow");
      float var3 = 1.0F;
      if (var2 != null) {
         var3 = var2.currentValue;
      }

      int var4 = this.ageToGrow;
      float var5 = 0.25F - var3 / 4.0F + 1.0F;
      return (int)((float)var4 * var5);
   }
}
