package zombie.world;

import zombie.core.Translator;

public class Wind {
   public Wind() {
   }

   public static float getWindKnots(float var0) {
      return var0 * 19.0F / 36.0F;
   }

   public static int getWindsockSegments(float var0) {
      return Math.max(0, Math.min(5, (int)Math.floor((double)(var0 * 19.0F / 108.0F))));
   }

   public static int getBeaufortNumber(float var0) {
      if (var0 < 4.0F) {
         return 0;
      } else if (var0 < 9.0F) {
         return 1;
      } else if (var0 < 16.0F) {
         return 2;
      } else if (var0 < 23.0F) {
         return 3;
      } else if (var0 < 31.0F) {
         return 4;
      } else if (var0 < 40.0F) {
         return 5;
      } else if (var0 < 50.0F) {
         return 6;
      } else if (var0 < 60.0F) {
         return 7;
      } else if (var0 < 72.0F) {
         return 8;
      } else if (var0 < 84.0F) {
         return 9;
      } else {
         return var0 < 97.0F ? 10 : 11;
      }
   }

   public static String getName(int var0) {
      return String.format(Translator.getText("UI_GameLoad_windName" + var0));
   }

   public static String getDescription(int var0) {
      return String.format(Translator.getText("UI_GameLoad_windDescription" + var0));
   }
}
