package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.ColorMixer;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
public class FluidInstance {
   private static final ConcurrentLinkedDeque<FluidInstance> pool = new ConcurrentLinkedDeque();
   @DebugNonRecursive
   private FluidContainer parent;
   private Fluid fluid;
   private float amount;
   private float percentage;
   private final Color color = new Color();

   private static FluidInstance Alloc() {
      FluidInstance var0 = (FluidInstance)pool.poll();
      if (var0 == null) {
         var0 = new FluidInstance();
      }

      return var0;
   }

   protected static FluidInstance Alloc(Fluid var0) {
      FluidInstance var1 = Alloc();
      var1.fluid = var0;
      var1.color.set(var0.getColor());
      return var1;
   }

   protected static void Release(FluidInstance var0) {
      var0.reset();
      if (!DebugOptions.instance.Checks.ObjectPoolContains.getValue() || !pool.contains(var0)) {
         pool.offer(var0);
      }
   }

   protected static void save(FluidInstance var0, ByteBuffer var1) throws IOException {
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (var0.getFluid().getFluidType() != FluidType.Modded) {
         var2.addFlags(1);
         var1.put(var0.getFluid().getFluidType().getId());
      } else {
         var2.addFlags(2);
         GameWindow.WriteString(var1, var0.getFluid().getFluidTypeString());
      }

      if (!var0.getColor().equals(var0.getFluid().getColor())) {
         var2.addFlags(4);
         var0.getColor().saveCompactNoAlpha(var1);
      }

      var1.putFloat(var0.amount);
      var2.write();
      var2.release();
   }

   protected static FluidInstance load(ByteBuffer var0, int var1) throws IOException {
      FluidInstance var2 = Alloc();
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var0);
      Fluid var5;
      if (var3.hasFlags(1)) {
         byte var4 = var0.get();
         var5 = Fluid.Get(FluidType.FromId(var4));
         var2.fluid = var5;
         if (var5 != null) {
            var2.color.set(var5.getColor());
         }
      } else if (var3.hasFlags(2)) {
         String var6 = GameWindow.ReadString(var0);
         var5 = Fluid.Get(var6);
         var2.fluid = var5;
         if (var5 != null) {
            var2.color.set(var5.getColor());
         }
      }

      if (var3.hasFlags(4)) {
         var2.getColor().loadCompactNoAlpha(var0);
      }

      var2.amount = var0.getFloat();
      var3.release();
      return var2;
   }

   private FluidInstance() {
   }

   public FluidInstance copy() {
      FluidInstance var1 = Alloc();
      var1.fluid = this.fluid;
      var1.amount = this.amount;
      var1.percentage = this.percentage;
      var1.color.set(this.color);
      return var1;
   }

   protected void setParent(FluidContainer var1) {
      this.parent = var1;
   }

   public Fluid getFluid() {
      return this.fluid;
   }

   public String getTranslatedName() {
      return this.fluid.getTranslatedName();
   }

   public float getAmount() {
      return this.amount;
   }

   protected void setAmount(float var1) {
      this.amount = var1;
   }

   protected float getPercentage() {
      return this.percentage;
   }

   protected void setPercentage(float var1) {
      this.percentage = var1;
   }

   protected Color getColor() {
      return this.color;
   }

   public void mixColor(Color var1, float var2) {
      if (this.amount == 0.0F) {
         this.setColor(var1);
      } else if (var2 != 0.0F) {
         float var3 = var2 / (var2 + this.amount);
         ColorMixer.LerpLCH(this.color, var1, var3, this.color);
      }
   }

   public void setColor(Color var1) {
      this.color.set(var1);
      if (this.parent != null) {
         this.parent.invalidateColor();
      }

   }

   public void setColor(float var1, float var2, float var3) {
      this.color.set(var1, var2, var3);
      if (this.parent != null) {
         this.parent.invalidateColor();
      }

   }

   private void reset() {
      this.parent = null;
      this.fluid = null;
      this.amount = 0.0F;
      this.percentage = 0.0F;
      this.color.set(1.0F, 1.0F, 1.0F);
   }
}
