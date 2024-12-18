package zombie.core.random;

import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.network.GameServer;

public interface RandInterface {
   void init();

   int Next(int var1);

   long Next(long var1);

   int Next(int var1, int var2);

   long Next(long var1, long var3);

   float Next(float var1, float var2);

   default boolean NextBool(int var1) {
      return this.Next(var1) == 0;
   }

   default boolean NextBoolFromChance(float var1) {
      float var2 = PZMath.clamp(var1, 0.0F, 1.0F);
      if (var2 == 0.0F) {
         return false;
      } else {
         float var3 = 1.0F / var2;
         int var4 = (int)(var3 + 0.5F);
         return this.NextBool(var4);
      }
   }

   default int AdjustForFramerate(int var1) {
      if (GameServer.bServer) {
         var1 = (int)((float)var1 * 0.33333334F);
      } else {
         var1 = (int)((float)var1 * ((float)PerformanceSettings.getLockFPS() / 30.0F));
      }

      return var1;
   }
}
