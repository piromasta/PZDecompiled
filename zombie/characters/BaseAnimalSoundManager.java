package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;

public abstract class BaseAnimalSoundManager {
   protected final ArrayList<IsoAnimal> characters = new ArrayList();
   private final long[] soundTime;
   private final int staleSlotMS;
   private final Comparator<IsoAnimal> comp = new Comparator<IsoAnimal>() {
      public int compare(IsoAnimal var1, IsoAnimal var2) {
         float var3 = BaseAnimalSoundManager.this.getClosestListener(var1.getX(), var1.getY(), var1.getZ());
         float var4 = BaseAnimalSoundManager.this.getClosestListener(var2.getX(), var2.getY(), var2.getZ());
         if (var3 > var4) {
            return 1;
         } else {
            return var3 < var4 ? -1 : 0;
         }
      }
   };

   public BaseAnimalSoundManager(int var1, int var2) {
      this.soundTime = new long[var1];
      this.staleSlotMS = var2;
   }

   public void addCharacter(IsoAnimal var1) {
      if (!this.characters.contains(var1)) {
         this.characters.add(var1);
      }

   }

   public void update() {
      if (!this.characters.isEmpty()) {
         this.characters.sort(this.comp);
         long var1 = System.currentTimeMillis();

         for(int var3 = 0; var3 < this.soundTime.length && var3 < this.characters.size(); ++var3) {
            IsoAnimal var4 = (IsoAnimal)this.characters.get(var3);
            if (var4.getCurrentSquare() != null) {
               int var5 = this.getFreeSoundSlot(var1);
               if (var5 == -1) {
                  break;
               }

               this.playSound(var4);
               this.soundTime[var5] = var1;
            }
         }

         this.postUpdate();
         this.characters.clear();
      }
   }

   public abstract void playSound(IsoAnimal var1);

   public abstract void postUpdate();

   private float getClosestListener(float var1, float var2, float var3) {
      float var4 = 3.4028235E38F;

      for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
         IsoPlayer var6 = IsoPlayer.players[var5];
         if (var6 != null && var6.getCurrentSquare() != null) {
            float var7 = var6.getX();
            float var8 = var6.getY();
            float var9 = var6.getZ();
            float var10 = IsoUtils.DistanceToSquared(var7, var8, var9 * 3.0F, var1, var2, var3 * 3.0F);
            var10 *= PZMath.pow(var6.getHearDistanceModifier(), 2.0F);
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

      if (var1 - var3 < (long)this.staleSlotMS) {
         return -1;
      } else {
         return var5;
      }
   }
}
