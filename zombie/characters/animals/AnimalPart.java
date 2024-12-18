package zombie.characters.animals;

import se.krka.kahlua.j2se.KahluaTableImpl;

public class AnimalPart {
   public String item = null;
   public int minNb = -1;
   public int maxNb = -1;
   public int nb = -1;

   public AnimalPart() {
   }

   public AnimalPart(KahluaTableImpl var1) {
      this.item = var1.rawgetStr("item");
      this.minNb = var1.rawgetInt("minNb");
      this.maxNb = var1.rawgetInt("maxNb");
      this.nb = var1.rawgetInt("nb");
   }

   public String getItem() {
      return this.item;
   }

   public int getMinNb() {
      return this.minNb;
   }

   public int getMaxNb() {
      return this.maxNb;
   }

   public int getNb() {
      return this.nb;
   }
}
