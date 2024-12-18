package zombie.pathfind;

final class Sync {
   private int fps = 20;
   private long period;
   private long excess;
   private long beforeTime;
   private long overSleepTime;

   Sync() {
      this.period = 1000000000L / (long)this.fps;
      this.beforeTime = System.nanoTime();
      this.overSleepTime = 0L;
   }

   void begin() {
      this.beforeTime = System.nanoTime();
      this.overSleepTime = 0L;
   }

   void startFrame() {
      this.excess = 0L;
   }

   void endFrame() {
      long var1 = System.nanoTime();
      long var3 = var1 - this.beforeTime;
      long var5 = this.period - var3 - this.overSleepTime;
      if (var5 > 0L) {
         try {
            Thread.sleep(var5 / 1000000L);
         } catch (InterruptedException var8) {
         }

         this.overSleepTime = System.nanoTime() - var1 - var5;
      } else {
         this.excess -= var5;
         this.overSleepTime = 0L;
      }

      this.beforeTime = System.nanoTime();
   }
}
