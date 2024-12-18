package zombie.util;

import zombie.GameTime;

public class FrameDelay {
   public int delay = 1;
   private int count = 0;
   private float delta = 0.0F;
   private float multiplier;

   public FrameDelay() {
   }

   public FrameDelay(int var1) {
      this.delay = var1;
   }

   public boolean update() {
      if (this.count == 0) {
         this.delta = 0.0F;
         this.multiplier = 0.0F;
      }

      this.delta += GameTime.instance.getTimeDelta();
      this.multiplier += GameTime.instance.getMultiplier();
      if (++this.count > this.delay) {
         this.count = 0;
         return true;
      } else {
         return false;
      }
   }

   public float getDelta() {
      return this.delta;
   }

   public float getMultiplier() {
      return this.multiplier;
   }

   public void reset() {
      this.count = 0;
      this.delta = 0.0F;
      this.multiplier = 0.0F;
   }
}
