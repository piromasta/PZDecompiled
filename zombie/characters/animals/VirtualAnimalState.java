package zombie.characters.animals;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.core.random.Rand;
import zombie.util.list.PZArrayUtil;

public abstract class VirtualAnimalState {
   static final ArrayList<AnimalZoneJunction> tempJunctions = new ArrayList();
   static final ArrayList<AnimalZoneJunction> possibleJunctions = new ArrayList();
   static float distanceBoost = 1.0F;
   protected VirtualAnimal m_animal;

   public void addAnimalTracks(AnimalTracksDefinitions.AnimalTracksType var1) {
      if (var1 != null) {
         AnimalChunk var2 = AnimalManagerWorker.getInstance().getAnimalChunk(this.m_animal.m_x, this.m_animal.m_y);
         if (var2 != null) {
            for(int var3 = 0; var3 < var2.m_animalTracks.size(); ++var3) {
               if (((AnimalTracks)var2.m_animalTracks.get(var3)).animalType.equalsIgnoreCase(this.m_animal.migrationGroup) && ((AnimalTracks)var2.m_animalTracks.get(var3)).trackType.equalsIgnoreCase(var1.type)) {
                  return;
               }
            }

            var2.addTracks(this.m_animal, var1);
         }
      }
   }

   public VirtualAnimalState(VirtualAnimal var1) {
      this.m_animal = var1;
   }

   public abstract void update();

   protected AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
      return null;
   }

   protected void reachedEnd() {
   }

   void moveAlongPath(String var1, float var2) {
      AnimalZone var3 = AnimalZones.getInstance().getClosestZone(this.m_animal.m_x, this.m_animal.m_y, var1);
      if (var3 == null) {
         this.m_animal.m_animals.forEach((var0) -> {
            var0.setAnimalZone((AnimalZone)null);
         });
      } else {
         this.moveAlongPath(var2 * distanceBoost * this.m_animal.speed, var3);
      }
   }

   void moveAlongPath(float var1, AnimalZone var2) {
      Vector2f var3 = new Vector2f();
      float var4 = var2.getClosestPointOnPolyline(this.m_animal.m_x, this.m_animal.m_y, var3);
      float var6 = var2.getPolylineLength();
      float var5;
      AnimalZoneJunction var7;
      if (this.m_animal.m_bMoveForwardOnZone) {
         var5 = var4 + var1 / var6;
         var2.getJunctionsBetween(var4, var5, tempJunctions);
         if (!tempJunctions.isEmpty()) {
            var7 = this.visitJunction(var2, ((AnimalZoneJunction)tempJunctions.get(0)).isFirstPointOnZone1() || ((AnimalZoneJunction)tempJunctions.get(0)).isLastPointOnZone1(), tempJunctions);
            if (var7 != null) {
               var2 = var7.m_zoneOther;
               var5 = var2.getDistanceOfPointFromStart(var7.m_pointIndexOther) / var2.getPolylineLength();
               if (var7.isFirstPointOnZone2()) {
                  this.m_animal.m_bMoveForwardOnZone = true;
               } else if (var7.isLastPointOnZone2()) {
                  this.m_animal.m_bMoveForwardOnZone = false;
               } else {
                  this.m_animal.m_bMoveForwardOnZone = Rand.NextBool(2);
               }

               if (this.m_animal.m_bMoveForwardOnZone) {
                  var5 += 1.0F / var2.getPolylineLength();
               } else {
                  var5 -= 1.0F / var2.getPolylineLength();
               }

               this.m_animal.m_currentZoneAction = var2.m_action;
            }
         }

         if (var5 >= 1.0F) {
            var5 = 1.0F;
            this.m_animal.m_bMoveForwardOnZone = false;
            this.reachedEnd();
         }
      } else {
         var5 = var4 - var1 / var6;
         var2.getJunctionsBetween(var5, var4, tempJunctions);
         if (!tempJunctions.isEmpty()) {
            var7 = this.visitJunction(var2, ((AnimalZoneJunction)tempJunctions.get(0)).isFirstPointOnZone1() || ((AnimalZoneJunction)tempJunctions.get(0)).isLastPointOnZone1(), tempJunctions);
            if (var7 != null) {
               var2 = var7.m_zoneOther;
               var5 = var2.getDistanceOfPointFromStart(var7.m_pointIndexOther) / var2.getPolylineLength();
               if (var7.isFirstPointOnZone2()) {
                  this.m_animal.m_bMoveForwardOnZone = true;
               } else if (var7.isLastPointOnZone2()) {
                  this.m_animal.m_bMoveForwardOnZone = false;
               } else {
                  this.m_animal.m_bMoveForwardOnZone = Rand.NextBool(2);
               }

               if (this.m_animal.m_bMoveForwardOnZone) {
                  var5 += 1.0F / var2.getPolylineLength();
               } else {
                  var5 -= 1.0F / var2.getPolylineLength();
               }

               this.m_animal.m_currentZoneAction = var2.m_action;
            }
         }

         if (var5 <= 0.0F) {
            var5 = 0.0F;
            this.m_animal.m_bMoveForwardOnZone = true;
            this.reachedEnd();
         }
      }

      var2.getPointOnPolyline(var5, var3);
      var2.getDirectionOnPolyline(var5, this.m_animal.m_forwardDirection);
      if (!this.m_animal.m_bMoveForwardOnZone) {
         this.m_animal.m_forwardDirection.mul(-1.0F);
      }

      AnimalManagerWorker var9 = AnimalManagerWorker.getInstance();
      this.m_animal.m_animals.forEach((var2x) -> {
         var2x.setAnimalZone(var2);
         var2x.setMoveForwardOnZone(this.m_animal.m_bMoveForwardOnZone);
      });
      var9.moveAnimal(this.m_animal, var3.x, var3.y);
   }

   public static class StateSleep extends VirtualAnimalState {
      public StateSleep(VirtualAnimal var1) {
         super(var1);
         var1.m_wakeTime = GameTime.getInstance().getWorldAgeHours() + (double)((float)this.m_animal.timeToSleep / 60.0F);
      }

      public void update() {
         this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.m_animal.migrationGroup, "sleep"));
         double var1 = GameTime.getInstance().getWorldAgeHours();
         if (var1 > this.m_animal.m_wakeTime) {
            this.m_animal.debugForceSleep = false;
            this.m_animal.m_state = new StateMoveFromSleep(this.m_animal);
            this.m_animal.m_nextRestTime = -1.0;
         }

      }
   }

   public static class StateMoveFromEat extends VirtualAnimalState {
      public StateMoveFromEat(VirtualAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.m_animal.m_currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
      }

      public AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
         possibleJunctions.clear();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            AnimalZoneJunction var5 = (AnimalZoneJunction)var3.get(var4);
            if ("Follow".equals(var5.m_zoneOther.m_action)) {
               possibleJunctions.add(var5);
            }
         }

         AnimalZoneJunction var6 = (AnimalZoneJunction)PZArrayUtil.pickRandom((List)possibleJunctions);
         if (var6 == null) {
            return null;
         } else {
            this.m_animal.m_state = new StateFollow(this.m_animal);
            return var6;
         }
      }
   }

   public static class StateMoveToEat extends VirtualAnimalState {
      public StateMoveToEat(VirtualAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.m_animal.m_currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
      }

      public void reachedEnd() {
         this.m_animal.m_state = new StateEat(this.m_animal);
      }
   }

   public static class StateMoveFromSleep extends VirtualAnimalState {
      public StateMoveFromSleep(VirtualAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.m_animal.m_currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
      }

      public AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
         possibleJunctions.clear();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            AnimalZoneJunction var5 = (AnimalZoneJunction)var3.get(var4);
            if ("Follow".equals(var5.m_zoneOther.m_action)) {
               possibleJunctions.add(var5);
            }
         }

         AnimalZoneJunction var6 = (AnimalZoneJunction)PZArrayUtil.pickRandom((List)possibleJunctions);
         if (var6 == null) {
            return null;
         } else {
            this.m_animal.m_state = new StateFollow(this.m_animal);
            return var6;
         }
      }
   }

   public static class StateMoveToSleep extends VirtualAnimalState {
      public StateMoveToSleep(VirtualAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.m_animal.m_currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
      }

      public void reachedEnd() {
         this.m_animal.m_state = new StateSleep(this.m_animal);
      }
   }

   public static class StateFollow extends VirtualAnimalState {
      public StateFollow(VirtualAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.m_animal.m_currentZoneAction, GameTime.getInstance().getMultiplier() / 2.0F * 0.2F);
         if (!this.m_animal.isRemoved()) {
            this.checkNextEatTime();
            if ("Eat".equals(this.m_animal.m_currentZoneAction) && this.m_animal.isTimeToEat()) {
               this.m_animal.m_state = new StateMoveToEat(this.m_animal);
            } else {
               this.checkNextRestTime();
               if ("Sleep".equals(this.m_animal.m_currentZoneAction) && this.m_animal.isTimeToSleep()) {
                  this.m_animal.m_state = new StateMoveToSleep(this.m_animal);
               } else {
                  this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.m_animal.migrationGroup, "walk"));
               }
            }
         }
      }

      private void checkNextEatTime() {
         if (this.m_animal.m_nextEatTime < 0.0) {
            this.m_animal.m_nextEatTime = MigrationGroupDefinitions.getNextEatTime(this.m_animal.migrationGroup);
         }

      }

      private void checkNextRestTime() {
         if (this.m_animal.m_nextRestTime < 0.0) {
            this.m_animal.m_nextRestTime = MigrationGroupDefinitions.getNextSleepTime(this.m_animal.migrationGroup);
         }

      }

      public AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
         possibleJunctions.clear();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            AnimalZoneJunction var5 = (AnimalZoneJunction)var3.get(var4);
            if ("Eat".equals(var5.m_zoneOther.m_action) && this.m_animal.isTimeToEat()) {
               return var5;
            }

            if ("Sleep".equals(var5.m_zoneOther.m_action) && this.m_animal.isTimeToSleep()) {
               return var5;
            }

            if ("Follow".equals(var5.m_zoneOther.m_action)) {
               possibleJunctions.add(var5);
            }
         }

         if ("Follow".equals(var1.m_action) && !var2 && Rand.NextBool(possibleJunctions.size() + 1)) {
            return null;
         } else {
            return (AnimalZoneJunction)PZArrayUtil.pickRandom((List)possibleJunctions);
         }
      }
   }

   public static class StateEat extends VirtualAnimalState {
      public StateEat(VirtualAnimal var1) {
         super(var1);
         var1.m_eatStartTime = GameTime.getInstance().getWorldAgeHours();
      }

      public void update() {
         double var1 = GameTime.getInstance().getWorldAgeHours();
         this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.m_animal.migrationGroup, "eat"));
         if (var1 - this.m_animal.m_eatStartTime > (double)((float)this.m_animal.timeToEat / 60.0F)) {
            this.m_animal.m_state = new StateMoveFromEat(this.m_animal);
            this.m_animal.m_nextEatTime = -1.0;
            this.m_animal.debugForceEat = false;
         }

      }
   }
}
