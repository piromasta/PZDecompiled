package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;

public class FluidSample {
   private static final ConcurrentLinkedDeque<FluidSample> pool = new ConcurrentLinkedDeque();
   private final ArrayList<FluidInstance> fluids = new ArrayList();
   private boolean sealed = false;
   private float amount = 0.0F;

   public static FluidSample Alloc() {
      FluidSample var0 = (FluidSample)pool.poll();
      if (var0 == null) {
         var0 = new FluidSample();
      }

      return var0;
   }

   protected static void Release(FluidSample var0) {
      var0.reset();

      assert !Core.bDebug || !pool.contains(var0) : "Object already in pool.";

      pool.offer(var0);
   }

   private FluidSample() {
   }

   public void release() {
      Release(this);
   }

   private void reset() {
      this.sealed = false;
      this.amount = 0.0F;
      if (this.fluids.size() > 0) {
         for(int var1 = 0; var1 < this.fluids.size(); ++var1) {
            FluidInstance.Release((FluidInstance)this.fluids.get(var1));
         }

         this.fluids.clear();
      }

   }

   public void clear() {
      this.reset();
   }

   protected void addFluid(FluidInstance var1) {
      if (!this.sealed) {
         this.fluids.add(var1.copy());
         this.amount += var1.getAmount();
      } else {
         DebugLog.General.error("FluidSample is sealed");
      }

   }

   protected FluidSample seal() {
      this.sealed = true;
      return this;
   }

   public FluidSample copy() {
      FluidSample var1 = Alloc();

      for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
         var1.fluids.add(((FluidInstance)this.fluids.get(var2)).copy());
      }

      var1.sealed = true;
      var1.amount = this.amount;
      return var1;
   }

   public boolean isEmpty() {
      return this.fluids.size() == 0 || this.amount <= 0.0F;
   }

   public boolean isPureFluid() {
      return this.fluids.size() == 1;
   }

   public float getAmount() {
      return this.amount;
   }

   public int size() {
      return this.fluids.size();
   }

   public float getPercentage(int var1) {
      if (var1 >= 0 && var1 < this.fluids.size()) {
         return ((FluidInstance)this.fluids.get(var1)).getPercentage();
      } else {
         DebugLog.General.error("FluidSample index out of bounds");
         return 0.0F;
      }
   }

   public Fluid getFluid(int var1) {
      if (var1 >= 0 && var1 < this.fluids.size()) {
         return ((FluidInstance)this.fluids.get(var1)).getFluid();
      } else {
         DebugLog.General.error("FluidSample index out of bounds");
         return null;
      }
   }

   public FluidInstance getFluidInstance(int var1) {
      if (var1 >= 0 && var1 < this.fluids.size()) {
         return (FluidInstance)this.fluids.get(var1);
      } else {
         DebugLog.General.error("FluidSample index out of bounds");
         return null;
      }
   }

   public FluidInstance getFluidInstance(Fluid var1) {
      for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
         if (((FluidInstance)this.fluids.get(var2)).getFluid().equals(var1)) {
            return (FluidInstance)this.fluids.get(var2);
         }
      }

      return null;
   }

   public Fluid getPrimaryFluid() {
      if (this.isEmpty()) {
         return null;
      } else if (this.fluids.size() == 1) {
         return ((FluidInstance)this.fluids.get(0)).getFluid();
      } else {
         FluidInstance var1 = null;

         for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
            FluidInstance var3 = (FluidInstance)this.fluids.get(var2);
            if (var1 == null || var3.getAmount() > var1.getAmount()) {
               var1 = var3;
            }
         }

         return var1.getFluid();
      }
   }

   public void scaleToAmount(float var1) {
      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var2 = (FluidInstance)this.fluids.get(var3);
         var2.setAmount(var1 * var2.getPercentage());
      }

      this.amount = var1;
   }

   public static void Save(FluidSample var0, ByteBuffer var1) throws IOException {
      var1.put((byte)(var0.sealed ? 1 : 0));
      var1.putFloat(var0.amount);
      var1.putInt(var0.size());

      for(int var2 = 0; var2 < var0.size(); ++var2) {
         FluidInstance.save((FluidInstance)var0.fluids.get(var2), var1);
      }

   }

   public static FluidSample Load(ByteBuffer var0, int var1) throws IOException {
      return Load(Alloc(), var0, var1);
   }

   public static FluidSample Load(FluidSample var0, ByteBuffer var1, int var2) throws IOException {
      var0.sealed = var1.get() == 1;
      var0.amount = var1.getFloat();
      float var3 = 0.0F;
      int var4 = var1.getInt();

      for(int var5 = 0; var5 < var4; ++var5) {
         FluidInstance var6 = FluidInstance.load(var1, var2);
         var0.fluids.add(var6);
         var3 += var6.getAmount();
      }

      if (var3 != var0.amount) {
         DebugLog.General.warn("Fluids amount mismatch with saved amount, correcting. save=" + var0.amount + ", fluids=" + var3);
         var0.amount = var3;
      }

      return var0;
   }
}
