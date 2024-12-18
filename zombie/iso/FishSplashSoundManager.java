package zombie.iso;

import java.util.ArrayList;
import java.util.Comparator;
import zombie.characters.IsoPlayer;

public class FishSplashSoundManager {
   public static final FishSplashSoundManager instance = new FishSplashSoundManager();
   private final ArrayList<IsoGridSquare> squares = new ArrayList();
   private final long[] soundTime = new long[6];
   private final Comparator<IsoGridSquare> comp = (var1, var2) -> {
      float var3 = this.getClosestListener((float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z);
      float var4 = this.getClosestListener((float)var2.x + 0.5F, (float)var2.y + 0.5F, (float)var2.z);
      if (var3 > var4) {
         return 1;
      } else {
         return var3 < var4 ? -1 : 0;
      }
   };

   public FishSplashSoundManager() {
   }

   public void addSquare(IsoGridSquare var1) {
      if (!this.squares.contains(var1)) {
         this.squares.add(var1);
      }

   }

   public void update() {
      if (!this.squares.isEmpty()) {
         this.squares.sort(this.comp);
         long var1 = System.currentTimeMillis();

         for(int var3 = 0; var3 < this.soundTime.length && var3 < this.squares.size(); ++var3) {
            IsoGridSquare var4 = (IsoGridSquare)this.squares.get(var3);
            if (!(this.getClosestListener((float)var4.x + 0.5F, (float)var4.y + 0.5F, (float)var4.z) > 20.0F)) {
               int var5 = this.getFreeSoundSlot(var1);
               if (var5 == -1) {
                  break;
               }

               var4.playSoundLocal("FishBreath");
               this.soundTime[var5] = var1;
            }
         }

         this.squares.clear();
      }
   }

   private float getClosestListener(float var1, float var2, float var3) {
      float var4 = 3.4028235E38F;

      for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
         IsoPlayer var6 = IsoPlayer.players[var5];
         if (var6 != null && var6.getCurrentSquare() != null) {
            float var7 = var6.getX();
            float var8 = var6.getY();
            float var9 = var6.getZ();
            float var10 = IsoUtils.DistanceTo(var7, var8, var9 * 3.0F, var1, var2, var3 * 3.0F);
            var10 *= var6.getHearDistanceModifier();
            if (var10 < var4) {
               var4 = var10;
            }
         }
      }

      return var4;
   }

   private int getFreeSoundSlot(long var1) {
      long var3 = 9223372036854775807L;
      int var5 = -1;

      for(int var6 = 0; var6 < this.soundTime.length; ++var6) {
         if (this.soundTime[var6] < var3) {
            var3 = this.soundTime[var6];
            var5 = var6;
         }
      }

      if (var1 - var3 < 3000L) {
         return -1;
      } else {
         return var5;
      }
   }
}
