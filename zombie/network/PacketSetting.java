package zombie.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import zombie.characters.Capability;
import zombie.network.anticheats.AntiCheat;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketSetting {
   int priority();

   int reliability();

   byte ordering();

   Capability requiredCapability();

   int handlingType();

   AntiCheat[] anticheats() default {AntiCheat.None};

   public static class HandlingType {
      public static final int None = 0;
      public static final int Server = 1;
      public static final int Client = 2;
      public static final int ClientLoading = 4;
      public static final int All = 7;

      public HandlingType() {
      }

      static int getType(boolean var0, boolean var1, boolean var2) {
         int var3 = 0;
         if (var0) {
            var3 |= 1;
         }

         if (var1) {
            var3 |= 2;
         }

         if (var2) {
            var3 |= 4;
         }

         return var3;
      }
   }
}
