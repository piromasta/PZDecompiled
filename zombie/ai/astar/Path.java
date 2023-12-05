package zombie.ai.astar;

import java.util.ArrayList;
import java.util.Stack;

public class Path {
   private ArrayList<Step> steps = new ArrayList();
   public float cost = 0.0F;
   public static Stack<Step> stepstore = new Stack();
   static Step containsStep = new Step();

   public Path() {
   }

   public float costPerStep() {
      return this.steps.isEmpty() ? this.cost : this.cost / (float)this.steps.size();
   }

   public void appendStep(int var1, int var2, int var3) {
      Step var4 = null;
      var4 = new Step();
      var4.x = var1;
      var4.y = var2;
      var4.z = var3;
      this.steps.add(var4);
   }

   public boolean contains(int var1, int var2, int var3) {
      containsStep.x = var1;
      containsStep.y = var2;
      containsStep.z = var3;
      return this.steps.contains(containsStep);
   }

   public int getLength() {
      return this.steps.size();
   }

   public Step getStep(int var1) {
      return (Step)this.steps.get(var1);
   }

   public int getX(int var1) {
      return this.getStep(var1).x;
   }

   public int getY(int var1) {
      return this.getStep(var1).y;
   }

   public int getZ(int var1) {
      return this.getStep(var1).z;
   }

   public static Step createStep() {
      if (stepstore.isEmpty()) {
         for(int var0 = 0; var0 < 200; ++var0) {
            Step var1 = new Step();
            stepstore.push(var1);
         }
      }

      return (Step)stepstore.push(containsStep);
   }

   public void prependStep(int var1, int var2, int var3) {
      Step var4 = null;
      var4 = new Step();
      var4.x = var1;
      var4.y = var2;
      var4.z = var3;
      this.steps.add(0, var4);
   }

   public static class Step {
      public int x;
      public int y;
      public int z;

      public Step(int var1, int var2, int var3) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
      }

      public Step() {
      }

      public boolean equals(Object var1) {
         if (!(var1 instanceof Step var2)) {
            return false;
         } else {
            return var2.x == this.x && var2.y == this.y && var2.z == this.z;
         }
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getZ() {
         return this.z;
      }

      public int hashCode() {
         return this.x * this.y * this.z;
      }
   }
}
