package zombie.ai.states.animals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.util.list.PZArrayUtil;

public final class AnimalZoneState extends State {
   private static final int PARAMETER_ACTION = 0;
   private static final int PARAMETER_STATE = 1;
   private static final AnimalZoneState _instance = new AnimalZoneState();

   public AnimalZoneState() {
   }

   public static AnimalZoneState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      var2.put(0, ((IsoAnimal)var1).getAnimalZone().getAction());
      var2.put(1, new StateFollow((IsoAnimal)var1));
   }

   public void execute(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      ZoneState var3 = (ZoneState)var2.get(1);
      var3.update();
   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
   }

   public boolean isMoving(IsoGameCharacter var1) {
      return var1.isMoving();
   }

   public static class StateFollow extends ZoneState {
      double m_nextRestTime = -1.0;

      public StateFollow(IsoAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
         if ("Eat".equals(this.getCurrentZoneAction())) {
            this.setState(new StateMoveToEat(this.m_animal));
         } else {
            if (this.m_nextRestTime < 0.0) {
               this.m_nextRestTime = GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(2, 5) / 60.0;
            }

            if (GameTime.getInstance().getWorldAgeHours() > this.m_nextRestTime) {
               this.setState(new StateSleep(this.m_animal));
            }

         }
      }

      public AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
         possibleJunctions.clear();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            AnimalZoneJunction var5 = (AnimalZoneJunction)var3.get(var4);
            if ("Eat".equals(var5.m_zoneOther.getAction())) {
               return var5;
            }

            if ("Follow".equals(var5.m_zoneOther.getAction())) {
               possibleJunctions.add(var5);
            }
         }

         if ("Follow".equals(var1.getAction()) && !var2 && Rand.NextBool(possibleJunctions.size() + 1)) {
            return null;
         } else {
            return (AnimalZoneJunction)PZArrayUtil.pickRandom((List)possibleJunctions);
         }
      }
   }

   private abstract static class ZoneState {
      protected static final ArrayList<AnimalZoneJunction> tempJunctions = new ArrayList();
      protected static final ArrayList<AnimalZoneJunction> possibleJunctions = new ArrayList();
      IsoAnimal m_animal;

      ZoneState(IsoAnimal var1) {
         this.m_animal = var1;
      }

      String getCurrentZoneAction() {
         HashMap var1 = this.m_animal.getStateMachineParams(AnimalZoneState.instance());
         return (String)var1.get(0);
      }

      void setCurrentZoneAction(String var1) {
         HashMap var2 = this.m_animal.getStateMachineParams(AnimalZoneState.instance());
         var2.put(0, var1);
      }

      void setState(ZoneState var1) {
         HashMap var2 = this.m_animal.getStateMachineParams(AnimalZoneState.instance());
         var2.put(1, var1);
      }

      public abstract void update();

      protected AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
         return null;
      }

      protected void reachedEnd() {
      }

      void moveAlongPath(String var1, float var2) {
         AnimalZone var3 = this.m_animal.getAnimalZone();
         if (var3 == null) {
            this.m_animal.setAnimalZone((AnimalZone)null);
         } else {
            this.moveAlongPath(var2, var3);
         }
      }

      void moveAlongPath(float var1, AnimalZone var2) {
         Vector2f var3 = new Vector2f();
         float var4 = var2.getClosestPointOnPolyline(this.m_animal.getX(), this.m_animal.getY(), var3);
         float var6 = var2.getPolylineLength();
         float var5;
         AnimalZoneJunction var7;
         if (this.m_animal.isMoveForwardOnZone()) {
            var5 = var4 + var1 / var6;
            var2.getJunctionsBetween(var4, var5, tempJunctions);
            if (!tempJunctions.isEmpty()) {
               var7 = this.visitJunction(var2, ((AnimalZoneJunction)tempJunctions.get(0)).isFirstPointOnZone1() || ((AnimalZoneJunction)tempJunctions.get(0)).isLastPointOnZone1(), tempJunctions);
               if (var7 != null) {
                  var2 = var7.m_zoneOther;
                  var5 = var2.getDistanceOfPointFromStart(var7.m_pointIndexOther) / var2.getPolylineLength();
                  if (var7.isFirstPointOnZone2()) {
                     this.m_animal.setMoveForwardOnZone(true);
                  } else if (var7.isLastPointOnZone2()) {
                     this.m_animal.setMoveForwardOnZone(false);
                  } else {
                     this.m_animal.setMoveForwardOnZone(Rand.NextBool(2));
                  }

                  if (this.m_animal.isMoveForwardOnZone()) {
                     var5 += 1.0F / var2.getPolylineLength();
                  } else {
                     var5 -= 1.0F / var2.getPolylineLength();
                  }

                  this.setCurrentZoneAction(var2.getAction());
               }
            }

            if (var5 >= 1.0F) {
               var5 = 1.0F;
               this.m_animal.setMoveForwardOnZone(false);
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
                     this.m_animal.setMoveForwardOnZone(true);
                  } else if (var7.isLastPointOnZone2()) {
                     this.m_animal.setMoveForwardOnZone(false);
                  } else {
                     this.m_animal.setMoveForwardOnZone(Rand.NextBool(2));
                  }

                  if (this.m_animal.isMoveForwardOnZone()) {
                     var5 += 1.0F / var2.getPolylineLength();
                  } else {
                     var5 -= 1.0F / var2.getPolylineLength();
                  }

                  this.setCurrentZoneAction(var2.getAction());
               }
            }

            if (var5 <= 0.0F) {
               var5 = 0.0F;
               this.m_animal.setMoveForwardOnZone(true);
               this.reachedEnd();
            }
         }

         var2.getPointOnPolyline(var5, var3);
         this.m_animal.setNextX(var3.x);
         this.m_animal.setNextY(var3.y);
         var2.getDirectionOnPolyline(var5, var3);
         if (!this.m_animal.isMoveForwardOnZone()) {
            var3.mul(-1.0F);
         }

         this.m_animal.setForwardDirection(var3.x, var3.y);
         this.m_animal.setAnimalZone(var2);
      }
   }

   public static class StateSleep extends ZoneState {
      double m_wakeTime = GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(1, 3) / 60.0;

      public StateSleep(IsoAnimal var1) {
         super(var1);
      }

      public void update() {
         double var1 = GameTime.getInstance().getWorldAgeHours();
         if (var1 > this.m_wakeTime) {
            this.setCurrentZoneAction("Follow");
            this.setState(new StateFollow(this.m_animal));
         }

      }
   }

   public static class StateMoveFromEat extends ZoneState {
      public StateMoveFromEat(IsoAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
      }

      public AnimalZoneJunction visitJunction(AnimalZone var1, boolean var2, ArrayList<AnimalZoneJunction> var3) {
         possibleJunctions.clear();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            AnimalZoneJunction var5 = (AnimalZoneJunction)var3.get(var4);
            if ("Follow".equals(var5.m_zoneOther.getAction())) {
               possibleJunctions.add(var5);
            }
         }

         AnimalZoneJunction var6 = (AnimalZoneJunction)PZArrayUtil.pickRandom((List)possibleJunctions);
         if (var6 == null) {
            return null;
         } else {
            this.setState(new StateFollow(this.m_animal));
            return var6;
         }
      }
   }

   public static class StateMoveToEat extends ZoneState {
      public StateMoveToEat(IsoAnimal var1) {
         super(var1);
      }

      public void update() {
         this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
      }

      public void reachedEnd() {
         this.setState(new StateEat(this.m_animal));
      }
   }

   public static class StateEat extends ZoneState {
      double m_startTime = GameTime.getInstance().getWorldAgeHours();

      public StateEat(IsoAnimal var1) {
         super(var1);
      }

      public void update() {
         double var1 = GameTime.getInstance().getWorldAgeHours();
         if (var1 - this.m_startTime > 0.03333333333333333) {
            this.setState(new StateMoveFromEat(this.m_animal));
         }

      }
   }
}
