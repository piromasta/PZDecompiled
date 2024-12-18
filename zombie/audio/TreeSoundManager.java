package zombie.audio;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;

public class TreeSoundManager {
   private ArrayList<IsoGridSquare> squares = new ArrayList();
   private final Slot[] slots = new Slot[10];
   private Comparator<IsoGridSquare> comp = (var1x, var2) -> {
      float var3 = this.getClosestListener((float)var1x.x + 0.5F, (float)var1x.y + 0.5F, (float)var1x.z);
      float var4 = this.getClosestListener((float)var2.x + 0.5F, (float)var2.y + 0.5F, (float)var2.z);
      if (var3 > var4) {
         return 1;
      } else {
         return var3 < var4 ? -1 : 0;
      }
   };

   public TreeSoundManager() {
      for(int var1 = 0; var1 < this.slots.length; ++var1) {
         this.slots[var1] = new Slot();
      }

   }

   public void addSquare(IsoGridSquare var1) {
      if (!this.squares.contains(var1)) {
         this.squares.add(var1);
      }

   }

   public void update() {
      for(int var1 = 0; var1 < this.slots.length; ++var1) {
         this.slots[var1].bPlaying = false;
      }

      long var7 = System.currentTimeMillis();
      if (this.squares.isEmpty()) {
         this.stopNotPlaying(var7);
      } else {
         Collections.sort(this.squares, this.comp);
         int var3 = Math.min(this.squares.size(), this.slots.length);

         int var4;
         IsoGridSquare var5;
         int var6;
         for(var4 = 0; var4 < var3; ++var4) {
            var5 = (IsoGridSquare)this.squares.get(var4);
            if (this.shouldPlay(var5)) {
               var6 = this.getExistingSlot(var5);
               if (var6 != -1) {
                  this.slots[var6].playSound(var5);
                  this.slots[var6].soundTime = var7;
               }
            }
         }

         for(var4 = 0; var4 < var3; ++var4) {
            var5 = (IsoGridSquare)this.squares.get(var4);
            if (this.shouldPlay(var5)) {
               var6 = this.getExistingSlot(var5);
               if (var6 == -1) {
                  var6 = this.getFreeSlot();
                  this.slots[var6].playSound(var5);
                  this.slots[var6].soundTime = var7;
               }
            }
         }

         this.stopNotPlaying(var7);
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

   boolean shouldPlay(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else {
         return !(this.getClosestListener((float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z) > 20.0F);
      }
   }

   int getExistingSlot(IsoGridSquare var1) {
      for(int var2 = 0; var2 < this.slots.length; ++var2) {
         if (this.slots[var2].square == var1) {
            return var2;
         }
      }

      return -1;
   }

   private int getFreeSlot() {
      for(int var1 = 0; var1 < this.slots.length; ++var1) {
         if (!this.slots[var1].bPlaying) {
            return var1;
         }
      }

      return -1;
   }

   private int getFreeSlot(long var1) {
      long var3 = 9223372036854775807L;
      int var5 = -1;

      for(int var6 = 0; var6 < this.slots.length; ++var6) {
         if (this.slots[var6].soundTime < var3) {
            var3 = this.slots[var6].soundTime;
            var5 = var6;
         }
      }

      if (var1 - var3 < 1000L) {
         return -1;
      } else {
         return var5;
      }
   }

   void stopNotPlaying(long var1) {
      for(int var3 = 0; var3 < this.slots.length; ++var3) {
         Slot var4 = this.slots[var3];
         if (!var4.bPlaying && var4.soundTime <= var1 - 1000L) {
            var4.stopPlaying();
            var4.square = null;
         }
      }

   }

   private static final class Slot {
      long soundTime = 0L;
      IsoGridSquare square = null;
      boolean bPlaying = false;
      BaseSoundEmitter emitter = null;
      long instance = 0L;

      private Slot() {
      }

      void playSound(IsoGridSquare var1) {
         if (this.emitter == null) {
            this.emitter = (BaseSoundEmitter)(Core.SoundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
         }

         this.emitter.setPos((float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z);
         if (!this.emitter.isPlaying("Bushes")) {
            this.instance = this.emitter.playSoundImpl("Bushes", (IsoObject)null);
            this.emitter.setParameterValueByName(this.instance, "Occlusion", 0.0F);
         }

         this.square = var1;
         this.bPlaying = true;
         this.emitter.tick();
      }

      void stopPlaying() {
         if (this.emitter != null && this.instance != 0L) {
            if (this.emitter.hasSustainPoints(this.instance)) {
               this.emitter.triggerCue(this.instance);
               this.instance = 0L;
            } else {
               this.emitter.stopAll();
               this.instance = 0L;
            }
         } else {
            if (this.emitter != null && !this.emitter.isEmpty()) {
               this.emitter.tick();
            }

         }
      }
   }
}
