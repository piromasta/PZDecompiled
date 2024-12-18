package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;

public class AnimalAllele {
   public String name;
   public float currentValue;
   public float trueRatioValue;
   public boolean dominant = true;
   public boolean used = false;
   public String geneticDisorder;

   public AnimalAllele() {
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      GameWindow.WriteString(var1, this.name);
      var1.putFloat(this.currentValue);
      var1.putFloat(this.trueRatioValue);
      var1.put((byte)(this.dominant ? 1 : 0));
      GameWindow.WriteString(var1, this.geneticDisorder);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      this.name = GameWindow.ReadString(var1);
      this.currentValue = var1.getFloat();
      this.trueRatioValue = var1.getFloat();
      this.dominant = var1.get() == 1;
      this.geneticDisorder = GameWindow.ReadString(var1);
   }

   public String getName() {
      return this.name;
   }

   public float getCurrentValue() {
      return this.currentValue;
   }

   public void setCurrentValue(float var1) {
      this.currentValue = var1;
   }

   public float getTrueRatioValue() {
      return this.trueRatioValue;
   }

   public void setTrueRatioValue(float var1) {
      this.trueRatioValue = var1;
   }

   public boolean isDominant() {
      return this.dominant;
   }

   public void setDominant(boolean var1) {
      this.dominant = var1;
   }

   public void setUsed(boolean var1) {
      this.used = var1;
   }

   public boolean isUsed() {
      return this.used;
   }

   public String getGeneticDisorder() {
      return this.geneticDisorder;
   }

   public void setGeneticDisorder(String var1) {
      this.geneticDisorder = var1;
   }
}
