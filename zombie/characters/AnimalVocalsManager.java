package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import zombie.characters.animals.AnimalSoundState;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class AnimalVocalsManager {
   public static final AnimalVocalsManager instance = new AnimalVocalsManager();
   private final HashSet<IsoAnimal> m_added = new HashSet();
   private final ObjectPool<ObjectWithDistance> m_objectPool = new ObjectPool(ObjectWithDistance::new);
   private final ArrayList<ObjectWithDistance> m_objects = new ArrayList();
   private final Slot[] m_slots;
   private long m_updateMS = 0L;
   private final Comparator<ObjectWithDistance> comp = new Comparator<ObjectWithDistance>() {
      public int compare(ObjectWithDistance var1, ObjectWithDistance var2) {
         return Float.compare(var1.distSq, var2.distSq);
      }
   };

   public AnimalVocalsManager() {
      byte var1 = 20;
      this.m_slots = (Slot[])PZArrayUtil.newInstance(Slot.class, var1, Slot::new);
   }

   public void addCharacter(IsoAnimal var1) {
      if (!this.m_added.contains(var1)) {
         this.m_added.add(var1);
         ObjectWithDistance var2 = (ObjectWithDistance)this.m_objectPool.alloc();
         var2.character = var1;
         this.m_objects.add(var2);
      }
   }

   public void update() {
      if (!GameServer.bServer) {
         long var1 = System.currentTimeMillis();
         if (var1 - this.m_updateMS >= 500L) {
            this.m_updateMS = var1;

            int var3;
            for(var3 = 0; var3 < this.m_slots.length; ++var3) {
               this.m_slots[var3].playing = false;
            }

            if (this.m_objects.isEmpty()) {
               this.stopNotPlaying();
            } else {
               IsoAnimal var5;
               for(var3 = 0; var3 < this.m_objects.size(); ++var3) {
                  ObjectWithDistance var4 = (ObjectWithDistance)this.m_objects.get(var3);
                  var5 = var4.character;
                  var4.distSq = this.getClosestListener(var5.getX(), var5.getY(), var5.getZ());
               }

               this.m_objects.sort(this.comp);
               var3 = PZMath.min(this.m_slots.length, this.m_objects.size());

               int var6;
               int var7;
               for(var7 = 0; var7 < var3; ++var7) {
                  var5 = ((ObjectWithDistance)this.m_objects.get(var7)).character;
                  if (this.shouldPlay(var5)) {
                     var6 = this.getExistingSlot(var5);
                     if (var6 != -1) {
                        this.m_slots[var6].playSound(var5);
                     }
                  }
               }

               for(var7 = 0; var7 < var3; ++var7) {
                  var5 = ((ObjectWithDistance)this.m_objects.get(var7)).character;
                  if (this.shouldPlay(var5)) {
                     var6 = this.getExistingSlot(var5);
                     if (var6 == -1) {
                        var6 = this.getFreeSlot();
                        this.m_slots[var6].playSound(var5);
                     }
                  }
               }

               this.stopNotPlaying();
               this.postUpdate();
               this.m_added.clear();
               this.m_objectPool.release((List)this.m_objects);
               this.m_objects.clear();
            }
         }
      }
   }

   boolean shouldPlay(IsoAnimal var1) {
      if (var1.isDead()) {
         return false;
      } else {
         if (var1.getVehicle() != null) {
            if (var1.getVehicle().getMovingObjectIndex() == -1) {
               return false;
            }
         } else if (var1.getCurrentSquare() == null) {
            return false;
         }

         return var1.getAnimalSoundState("voice").shouldPlay();
      }
   }

   int getExistingSlot(IsoAnimal var1) {
      for(int var2 = 0; var2 < this.m_slots.length; ++var2) {
         if (this.m_slots[var2].character == var1) {
            return var2;
         }
      }

      return -1;
   }

   int getFreeSlot() {
      for(int var1 = 0; var1 < this.m_slots.length; ++var1) {
         if (!this.m_slots[var1].playing) {
            return var1;
         }
      }

      return -1;
   }

   void stopNotPlaying() {
      for(int var1 = 0; var1 < this.m_slots.length; ++var1) {
         Slot var2 = this.m_slots[var1];
         if (!var2.playing) {
            var2.stopPlaying();
            var2.character = null;
         }
      }

   }

   public void postUpdate() {
   }

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

   public void render() {
      if (Core.bDebug) {
      }

   }

   public static void Reset() {
      for(int var0 = 0; var0 < instance.m_slots.length; ++var0) {
         instance.m_slots[var0].stopPlaying();
         instance.m_slots[var0].character = null;
         instance.m_slots[var0].playing = false;
      }

   }

   static final class Slot {
      IsoAnimal character = null;
      boolean playing = false;

      Slot() {
      }

      void playSound(IsoAnimal var1) {
         if (this.character != null && this.character != var1 && this.character.getAnimalSoundState("voice").getEventInstance() != 0L) {
            this.character.getAnimalSoundState("voice").stop();
         }

         this.character = var1;
         this.playing = true;
         AnimalSoundState var2 = this.character.getAnimalSoundState("voice");
         if (!var2.isPlayingDesiredSound()) {
            String var3 = var2.getDesiredSoundName();
            int var4 = var2.getDesiredSoundPriority();
            var1.getAnimalSoundState("voice").start(var3, var4);
         }

      }

      void stopPlaying() {
         if (this.character != null && this.character.getAnimalSoundState("voice").getEventInstance() != 0L) {
            if (!this.character.isDead()) {
               this.character.getAnimalSoundState("voice").stop();
            }
         }
      }
   }

   static final class ObjectWithDistance {
      IsoAnimal character;
      float distSq;

      ObjectWithDistance() {
      }
   }
}
