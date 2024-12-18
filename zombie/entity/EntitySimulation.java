package zombie.entity;

import zombie.GameTime;

public class EntitySimulation {
   private static final long MILLIS_PER_TICK = 100L;
   private static final double SECONDS_PER_TICK = 6.0;
   private static long currentTimeMillis = 0L;
   private static int simulationTicksThisFrame = 0;
   private static long lastTimeStamp = 0L;

   public EntitySimulation() {
   }

   public static long getMillisPerTick() {
      return 100L;
   }

   public static double secondsPerTick() {
      return 6.0;
   }

   public static long getCurrentTimeMillis() {
      return currentTimeMillis;
   }

   public static int getSimulationTicksThisFrame() {
      return simulationTicksThisFrame;
   }

   protected static void update() {
      long var0 = (long)(GameTime.instance.getTimeDelta() * 1000.0F);
      currentTimeMillis += var0;
      long var2 = currentTimeMillis - lastTimeStamp;
      if (var2 >= 100L) {
         simulationTicksThisFrame = (int)(var2 / 100L);
         lastTimeStamp = currentTimeMillis - (var2 - (long)simulationTicksThisFrame * 100L);
      } else {
         simulationTicksThisFrame = 0;
      }

   }

   protected static void reset() {
      currentTimeMillis = 0L;
      simulationTicksThisFrame = 0;
      lastTimeStamp = 0L;
   }
}
