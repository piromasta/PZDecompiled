package zombie.network.fields;

import zombie.characters.animals.IsoAnimal;

public class AnimalStateVariables {
   private static final AnimalStateVariables instance = new AnimalStateVariables();

   public static AnimalStateVariables getInstance() {
      return instance;
   }

   private AnimalStateVariables() {
   }

   public int getVariables(IsoAnimal var1) {
      int var2 = 0;
      var2 |= var1.isOnFloor() ? 1 : 0;
      var2 |= var1.getVariableBoolean("animalRunning") ? 4 : 0;
      return var2;
   }

   public void setVariables(IsoAnimal var1, int var2) {
      var1.setOnFloor((var2 & 1) != 0);
      var1.setVariable("animalRunning", (var2 & 4) != 0);
   }
}
