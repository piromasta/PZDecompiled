package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

public class FluidConsume extends SealedFluidProperties {
   private static final ConcurrentLinkedDeque<FluidConsume> pool = new ConcurrentLinkedDeque();
   private float amount = 0.0F;
   private PoisonEffect poisonEffect;

   public static FluidConsume Alloc() {
      FluidConsume var0 = (FluidConsume)pool.poll();
      if (var0 == null) {
         var0 = new FluidConsume();
      }

      return var0;
   }

   protected static void Release(FluidConsume var0) {
      var0.reset();
      pool.offer(var0);
   }

   private FluidConsume() {
      this.poisonEffect = PoisonEffect.None;
   }

   public void release() {
      Release(this);
   }

   private void reset() {
      this.clear();
   }

   public void clear() {
      super.clear();
      this.amount = 0.0F;
      this.poisonEffect = PoisonEffect.None;
   }

   protected void setAmount(float var1) {
      this.amount = var1;
   }

   protected void setPoisonEffect(PoisonEffect var1) {
      if (var1.getLevel() > this.poisonEffect.getLevel()) {
         this.poisonEffect = var1;
      }

   }

   public float getAmount() {
      return this.amount;
   }

   public PoisonEffect getPoisonEffect() {
      return this.poisonEffect;
   }

   public static void Save(FluidConsume var0, ByteBuffer var1) throws IOException {
      var1.putFloat(var0.amount);
      var1.putInt(var0.poisonEffect.getLevel());
      var0.save(var1);
   }

   public static FluidConsume Load(ByteBuffer var0, int var1) throws IOException {
      return Load(Alloc(), var0, var1);
   }

   public static FluidConsume Load(FluidConsume var0, ByteBuffer var1, int var2) throws IOException {
      var0.amount = var1.getFloat();
      var0.poisonEffect = PoisonEffect.FromLevel(var1.getInt());
      var0.load(var1, var2);
      return var0;
   }
}
