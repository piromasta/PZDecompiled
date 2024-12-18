package zombie.core.utils;

import zombie.GameTime;
import zombie.SandboxOptions;

public class GameTimer {
   double start = GameTime.getInstance().getWorldAgeHours();
   double period;

   public GameTimer(int var1) {
      this.period = (double)var1 / (double)SandboxOptions.getInstance().getDayLengthMinutes() * 24.0 / 60.0;
   }

   public boolean check() {
      boolean var1 = GameTime.getInstance().getWorldAgeHours() - this.start > this.period;
      if (var1) {
         this.start = GameTime.getInstance().getWorldAgeHours();
      }

      return var1;
   }
}
