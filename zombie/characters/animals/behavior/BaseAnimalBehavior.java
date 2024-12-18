package zombie.characters.animals.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import zombie.GameTime;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Position3D;
import zombie.characters.Stats;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.util.PZCalendar;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class BaseAnimalBehavior {
   private static final Vector2 tempVector2 = new Vector2();
   protected IsoAnimal parent = null;
   public float wanderMulMod = 0.0F;
   public boolean blockMovement = false;
   private float goToMomTimer = 0.0F;
   public int sitInTime = 0;
   public int sitOutTime = 0;
   public float blockedFor = 0.0F;
   public float attackAnimalTimer = 0.0F;
   private int followChrTimer = 0;
   private long lastFleeTimer = 0L;
   public float lastAlerted = 0.0F;
   private float idleAnimTimer = 0.0F;
   public float behaviorCheckTimer = 0.0F;
   public BehaviorAction behaviorAction = null;
   public Object behaviorObject = null;
   public boolean isDoingBehavior = false;
   public float behaviorMaxTime = 5000.0F;
   public float behaviorFailsafe = 0.0F;
   public int hutchPathTimer = -1;
   public int enterHutchTimerAfterDestroy = 0;
   public long forcedOutsideHutch = 0L;
   private float timerFleeAgain = 0.0F;

   public BaseAnimalBehavior(IsoAnimal var1) {
      this.parent = var1;
   }

   public void wanderIdle() {
      if (this.blockMovement) {
         this.blockedFor += GameTime.getInstance().getMultiplier();
         if (this.blockedFor >= 8000.0F) {
            this.blockMovement = false;
            this.blockedFor = 0.0F;
         }
      }

      if (!this.isDoingBehavior && !this.blockMovement && !this.parent.isAlerted() && !this.parent.getData().goingToMom && !this.parent.isAnimalEating() && !this.parent.isAnimalMoving() && this.parent.soundSourceTarget == null && !this.parent.walkToCharLuring && this.parent.drinkFromTrough == null && this.parent.eatFromTrough == null && this.parent.eatFromGround == null) {
         if (this.parent.isOnFire() && !this.parent.isAnimalMoving()) {
            this.parent.setVariable("animalRunning", true);
            this.parent.pathToLocation(Rand.Next(this.parent.getCurrentSquare().x - 10, this.parent.getCurrentSquare().x + 10), Rand.Next(this.parent.getCurrentSquare().y - 10, this.parent.getCurrentSquare().y + 10), this.parent.getCurrentSquare().getZ());
         } else {
            if (this.parent.getStateEventDelayTimer() < -1000.0F && this.parent.isAnimalMoving()) {
               this.parent.setStateEventDelayTimer(0.0F);
               this.parent.stopAllMovementNow();
            }

            if (this.parent.adef.sitRandomly) {
               this.checkSit();
            }

            if (this.parent.isAnimalSitting()) {
               if (Rand.NextBool(this.parent.adef.idleEmoteChance) && StringUtils.isNullOrEmpty(this.parent.getVariableString("sittingAnim")) && this.parent.adef.sittingTypeNbr > 0) {
                  this.parent.setVariable("sittingAnim", "sit" + Rand.Next(1, this.parent.adef.sittingTypeNbr + 1));
               }

            } else {
               if (this.parent.mother != null && this.parent.mother.getCurrentSquare() != null && this.parent.canGoThere(this.parent.mother.getCurrentSquare())) {
                  if (this.goToMomTimer > 0.0F) {
                     this.goToMomTimer -= GameTime.getInstance().getMultiplier();
                  } else {
                     float var1 = -1.0F;
                     if (this.parent.getCurrentSquare() != null && this.parent.mother.getCurrentSquare() != null) {
                        var1 = this.parent.getCurrentSquare().DistToProper(this.parent.mother.getCurrentSquare());
                     }

                     if (var1 >= 6.0F && this.parent.getCurrentState() == AnimalIdleState.instance()) {
                        this.parent.setVariable("animalRunning", true);
                        this.goToMomTimer = 300.0F;
                        this.parent.pathToLocation(this.parent.mother.getCurrentSquare().x, this.parent.mother.getCurrentSquare().y, this.parent.mother.getCurrentSquare().getZ());
                        return;
                     }
                  }
               }

               if (this.idleAnimTimer > 0.0F) {
                  this.idleAnimTimer -= GameTime.getInstance().getMultiplier();
                  if (this.idleAnimTimer < 0.0F) {
                     this.idleAnimTimer = 0.0F;
                  }
               }

               if (Rand.NextBool(this.parent.adef.idleEmoteChance) && StringUtils.isNullOrEmpty(this.parent.getVariableString("idleAction")) && this.idleAnimTimer == 0.0F) {
                  if (this.parent.stressLevel < 10.0F && this.parent.adef.happyAnim > 0 && Rand.NextBool(3)) {
                     this.parent.setVariable("idleAction", "happy" + Rand.Next(1, this.parent.adef.happyAnim + 1));
                  } else if (this.parent.adef.idleTypeNbr > 0) {
                     this.parent.setVariable("idleAction", "idle" + Rand.Next(1, this.parent.adef.idleTypeNbr + 1));
                  }

                  this.idleAnimTimer = 1500.0F;
               }

               if (this.parent.movingToFood != null) {
                  this.parent.setStateEventDelayTimer(this.pickRandomWanderInterval());
               }

               if (this.parent.getStateEventDelayTimer() <= 0.0F) {
                  this.parent.setVariable("animalRunning", false);
                  int var10 = (int)this.parent.getX();
                  int var2 = (int)this.parent.getY();
                  if (this.parent.getData().getAttachedPlayer() != null) {
                     var10 = PZMath.fastfloor(this.parent.getData().getAttachedPlayer().getX());
                     var2 = PZMath.fastfloor(this.parent.getData().getAttachedPlayer().getY());
                  } else if (this.parent.getData().getAttachedTree() != null && this.parent.getData().getAttachedTree().getSquare() != null) {
                     var10 = PZMath.fastfloor(this.parent.getData().getAttachedTree().getX());
                     var2 = PZMath.fastfloor(this.parent.getData().getAttachedTree().getY());
                  } else if (this.parent.mother != null && this.parent.mother.getCurrentSquare() != null) {
                     var10 = this.parent.mother.getCurrentSquare().getX();
                     var2 = this.parent.mother.getCurrentSquare().getY();
                  }

                  this.parent.setStateEventDelayTimer(this.pickRandomWanderInterval());
                  if ((double)this.parent.getStats().hunger > 0.3) {
                     this.parent.setStateEventDelayTimer(200.0F);
                  }

                  int var3 = var10 + (Rand.Next(16) - 8);
                  int var4 = var2 + (Rand.Next(16) - 8);
                  if (this.parent.dZone != null && this.parent.getStats().hunger < 0.9F && this.parent.getStats().thirst < 0.9F) {
                     DesignationZoneAnimal var5 = DesignationZoneAnimal.getZone(var3, var4, this.parent.getCurrentSquare().getZ());

                     for(int var6 = 0; var5 == null && var6 < 100; ++var6) {
                        var3 = var10 + (Rand.Next(16) - 8);
                        var4 = var2 + (Rand.Next(16) - 8);
                        var5 = DesignationZoneAnimal.getZone(var3, var4, this.parent.getCurrentSquare().getZ());
                     }

                     if (RainManager.isRaining() && (double)RainManager.getRainIntensity() > 0.05) {
                        ArrayList var7 = new ArrayList();

                        for(int var8 = 0; var8 < this.parent.connectedDZone.size(); ++var8) {
                           DesignationZoneAnimal var9 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var8);
                           if (!var9.roofAreas.isEmpty() && !var7.contains(var9)) {
                              var7.add(var9);
                           }
                        }

                        if (!var7.isEmpty()) {
                           DesignationZoneAnimal var11 = (DesignationZoneAnimal)var7.get(Rand.Next(0, var7.size()));
                           Position3D var12 = (Position3D)var11.roofAreas.get(Rand.Next(0, var11.roofAreas.size()));
                           var3 = (int)var12.x;
                           var4 = (int)var12.y;
                        }
                     }
                  }

                  if (this.parent.getCell().getGridSquare((double)var3, (double)var4, (double)this.parent.getZ()) != null && this.parent.getCell().getGridSquare((double)var3, (double)var4, (double)this.parent.getZ()).isFree(true)) {
                     if (this.parent.adef.periodicRun && Rand.NextBool(5)) {
                        this.parent.setVariable("animalRunning", true);
                     }

                     this.parent.pathToLocation(var3, var4, (int)this.parent.getZ());
                  }
               }

            }
         }
      }
   }

   public void walkedOnSpot() {
      this.parent.setMoving(false);
      this.parent.getPathFindBehavior2().reset();
      this.parent.setStateEventDelayTimer(200.0F);
   }

   public void goAttack(IsoGameCharacter var1) {
      if (!this.blockMovement && BehaviorAction.FIGHTANIMAL != this.behaviorAction) {
         if (var1 != null && !var1.isDead() && var1.getCurrentSquare() != null && var1.isExistInTheWorld()) {
            this.behaviorAction = BehaviorAction.FIGHTANIMAL;
            this.behaviorObject = var1;
            this.isDoingBehavior = true;
            this.parent.stopAllMovementNow();
            this.parent.pathToCharacter(var1);
            if (var1 instanceof IsoAnimal) {
               ((IsoAnimal)var1).fightingOpponent = this.parent;
               ((IsoAnimal)var1).getBehavior().blockMovement = true;
            }

         } else {
            this.parent.fightingOpponent = null;
            this.attackAnimalTimer = 0.0F;
         }
      }
   }

   public void checkSit() {
      if ((double)this.parent.getStats().getHunger() > 0.2 || this.parent.getData().getAttachedPlayer() != null) {
         this.parent.clearVariable("idleAction");
         this.sitInTime = 0;
         this.sitOutTime = 0;
      }

      if (this.parent.getCurrentSquare() != null) {
         if (RainManager.isRaining() && RainManager.getRainIntensity() > 0.05F && !this.parent.getCurrentSquare().haveRoof) {
            this.parent.clearVariable("idleAction");
            this.sitInTime = 0;
            this.sitOutTime = 0;
         }

         if (!((double)this.parent.getStats().getHunger() > 0.3) && this.parent.getData().getAttachedPlayer() == null) {
            if (!RainManager.isRaining() || this.parent.getCurrentSquare() == null || this.parent.getCurrentSquare().haveRoof) {
               if (this.sitInTime == 0 && this.sitOutTime == 0) {
                  this.sitInTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(10800, 25200);
                  this.sitOutTime = this.sitInTime + Rand.Next(3600, 7200);
               }

               if (this.sitInTime > 0 && GameTime.instance.getCalender().getTimeInMillis() / 1000L > (long)this.sitInTime) {
                  this.parent.setVariable("idleAction", "sit");
                  this.sitInTime = 0;
               }

               if (this.sitOutTime > 0 && GameTime.instance.getCalender().getTimeInMillis() / 1000L > (long)this.sitOutTime) {
                  this.parent.clearVariable("idleAction");
                  this.sitOutTime = 0;
               }

            }
         }
      }
   }

   public float pickRandomWanderInterval() {
      float var1 = Rand.Next(this.parent.adef.wanderMul - this.wanderMulMod, (this.parent.adef.wanderMul - this.wanderMulMod) * 5.0F);
      if (ServerOptions.getInstance().UltraSpeedDoesnotAffectToAnimals.getValue() && GameTime.getInstance().getMultiplier() > 60.0F) {
         var1 *= 10.0F;
      }

      if (this.parent.geneticDisorder.contains("fidget")) {
         var1 /= 10.0F;
      }

      if (RainManager.isRaining()) {
         var1 *= 3.0F;
      }

      return var1;
   }

   public void updateAttackTimer() {
      if (this.attackAnimalTimer > 0.0F) {
         this.attackAnimalTimer -= GameTime.getInstance().getMultiplier();
         if (this.attackAnimalTimer < 0.0F) {
            this.attackAnimalTimer = 0.0F;
         }
      }

   }

   public void update() {
      this.fleeFromAttacker();
      this.fleeFromChr();
      this.followChr();
      this.updateAttackTimer();
      this.updateAcceptance();
      this.updateGoingToHutch();
      if (this.behaviorCheckTimer > 150.0F && (this.parent.getStats().hunger > 0.5F || this.parent.getStats().thirst > 0.5F)) {
         this.behaviorCheckTimer = 150.0F;
      }

      if (this.behaviorCheckTimer > 0.0F) {
         this.behaviorCheckTimer -= GameTime.getInstance().getMultiplier();
         if (this.behaviorCheckTimer < 0.0F) {
            this.behaviorCheckTimer = 0.0F;
         }
      } else {
         this.behaviorCheckTimer = 500.0F;
         this.checkBehavior();
      }

      if (this.isDoingBehavior) {
         this.behaviorFailsafe += GameTime.getInstance().getMultiplier();
         if (this.behaviorFailsafe > this.behaviorMaxTime) {
            this.behaviorFailsafe = 0.0F;
            this.isDoingBehavior = false;
            this.behaviorAction = null;
            this.behaviorObject = null;
         }
      }

   }

   private void updateGoingToHutch() {
      if (this.isDoingBehavior && BehaviorAction.ENTERHUTCH == this.behaviorAction) {
         IsoHutch var1 = (IsoHutch)this.behaviorObject;
         if (this.hutchPathTimer > -1 && var1 != null) {
            this.hutchPathTimer = (int)((float)this.hutchPathTimer - GameTime.getInstance().getMultiplier());
            if (this.hutchPathTimer <= 0 && var1 != null) {
               this.parent.stopAllMovementNow();
               this.parent.pathToLocation(var1.getSquare().x + var1.getEnterSpotX(), var1.getSquare().y + var1.getEnterSpotY(), var1.getSquare().getZ());
               this.hutchPathTimer = -1;
            }
         }
      }

   }

   public void doBehaviorAction() {
      if (this.isDoingBehavior) {
         this.isDoingBehavior = false;
         if (this.behaviorAction == null) {
            System.out.println("action was null, object: " + this.behaviorObject);
            return;
         }

         switch (this.behaviorAction) {
            case EATTROUGH:
               this.eatFromTrough();
               break;
            case EATGROUND:
               this.eatFromGround();
               break;
            case DRINKGROUND:
               this.drinkFromGround();
               break;
            case EATMOM:
               this.eatFromMom();
               break;
            case DRINK:
               this.drinkFromTrough();
               break;
            case FERTILIZE:
               this.fertilize();
               break;
            case ENTERHUTCH:
               this.enterHutch();
               break;
            case FIGHTANIMAL:
               this.fightAnimal();
         }

         this.resetBehaviorAction();
      }

      this.timerFleeAgain = 0.0F;
   }

   public void fightAnimal() {
      IsoAnimal var1;
      IsoGameCharacter var2;
      if (this.parent.fightingOpponent != null && !this.parent.fightingOpponent.isDead() && this.parent.fightingOpponent.getCurrentSquare() != null && this.parent.fightingOpponent.isExistInTheWorld()) {
         if (this.parent.fightingOpponent.DistToProper(this.parent) <= (float)this.parent.adef.attackDist) {
            this.parent.stopAllMovementNow();
            this.parent.faceThisObject(this.parent.fightingOpponent);
            this.parent.atkTarget = this.parent.fightingOpponent;
            this.attackAnimalTimer = (float)this.parent.adef.attackTimer;
            var2 = this.parent.fightingOpponent;
            if (var2 instanceof IsoAnimal) {
               var1 = (IsoAnimal)var2;
               var1.atkTarget = this.parent;
               var1.getBehavior().attackAnimalTimer = (float)var1.adef.attackTimer;
               var1.getBehavior().blockMovement = true;
            } else {
               this.attackAnimalTimer = 1500.0F;
            }
         }

         var2 = this.parent.fightingOpponent;
         if (var2 instanceof IsoAnimal) {
            var1 = (IsoAnimal)var2;
            var1.fightingOpponent = null;
         }

         this.parent.fightingOpponent = null;
      } else {
         var2 = this.parent.fightingOpponent;
         if (var2 instanceof IsoAnimal) {
            var1 = (IsoAnimal)var2;
            var1.fightingOpponent = null;
         }

         this.parent.fightingOpponent = null;
      }
   }

   private void enterHutch() {
      IsoHutch var1 = (IsoHutch)this.behaviorObject;
      if (var1 != null && var1.isOpen() && var1.getSquare() != null && this.parent.getCurrentSquare() != null && var1.getAnimalInside().size() < var1.getMaxAnimals() && var1.getSquare().DistToProper(this.parent.getCurrentSquare()) < 2.0F && var1.addAnimalInside(this.parent)) {
         var1.animalOutside.remove(this.parent);
         this.parent.removeFromWorld();
         this.parent.removeFromSquare();
      }

   }

   public void resetBehaviorAction() {
      this.parent.setTurnDelta(this.parent.adef.turnDelta);
      this.behaviorAction = null;
      this.behaviorObject = null;
      this.parent.fightingOpponent = null;
   }

   private void fertilize() {
      IsoAnimal var1 = (IsoAnimal)this.behaviorObject;
      var1.getBehavior().blockMovement = false;
      if (var1.getSquare() != null && this.parent.getCurrentSquare() != null && var1.getSquare().DistToProper(this.parent.getCurrentSquare()) >= 3.0F) {
         this.resetBehaviorAction();
      } else {
         var1.fertilize(this.parent, false);
      }
   }

   private void drinkFromTrough() {
      IsoFeedingTrough var1 = (IsoFeedingTrough)this.behaviorObject;
      if (this.parent.getCurrentSquare() != null && this.parent.getCurrentSquare().DistToProper(var1.getSquare()) > this.parent.adef.distToEat) {
         this.parent.ignoredTrough.add(var1);
      } else {
         this.parent.setVariable("idleAction", "eat");
         if (this.parent.adef.eatingTypeNbr > 0) {
            this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
         }

         this.parent.drinkFromTrough = var1;
      }
   }

   private void eatFromTrough() {
      IsoFeedingTrough var1 = (IsoFeedingTrough)this.behaviorObject;
      if (this.parent.getCurrentSquare() != null && var1.getSquare() != null && this.parent.getCurrentSquare().DistToProper(var1.getSquare()) > this.parent.adef.distToEat) {
         this.parent.ignoredTrough.add(var1);
      } else {
         this.parent.setVariable("idleAction", "eat");
         if (this.parent.adef.eatingTypeNbr > 0) {
            this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
         }

         this.parent.eatFromTrough = var1;
      }
   }

   private void eatFromGround() {
      IsoWorldInventoryObject var1 = (IsoWorldInventoryObject)this.behaviorObject;
      if (this.parent.getCurrentSquare() == null || var1.getSquare() == null || !(this.parent.getCurrentSquare().DistToProper(var1.getSquare()) > this.parent.adef.distToEat)) {
         this.parent.setVariable("idleAction", "eat");
         if (this.parent.adef.eatingTypeNbr > 0) {
            this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
         }

         this.parent.eatFromGround = var1;
      }
   }

   private void drinkFromGround() {
      IsoWorldInventoryObject var1 = (IsoWorldInventoryObject)this.behaviorObject;
      if (this.parent.getCurrentSquare() == null || var1.getSquare() == null || !(this.parent.getCurrentSquare().DistToProper(var1.getSquare()) > this.parent.adef.distToEat)) {
         this.parent.setVariable("idleAction", "eat");
         if (this.parent.adef.eatingTypeNbr > 0) {
            this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
         }

         this.parent.eatFromGround = var1;
      }
   }

   private void clearIdleAction() {
      if (!StringUtils.isNullOrEmpty(this.parent.getVariableString("idleAction")) && this.parent.getVariableString("idleAction").startsWith("idle")) {
         this.parent.clearVariable("idleAction");
      }

   }

   public void checkBehavior() {
      if (!this.isDoingBehavior) {
         if (!this.blockMovement) {
            this.parent.fightingOpponent = null;
         }

         this.clearIdleAction();
         if (this.parent.getVehicle() != null) {
            this.isDoingBehavior = false;
            this.parent.luredBy = null;
            this.blockMovement = false;
            this.parent.clearVariable("idleAction");
         }

         if ((this.parent.getVehicle() != null || this.parent.isExistInTheWorld()) && this.parent.luredBy == null && !this.parent.getBehavior().blockMovement && !this.isDoingBehavior && StringUtils.isNullOrEmpty(this.parent.getVariableString("idleAction"))) {
            if (this.checkEatBehavior()) {
               if (this.parent.getVehicle() == null) {
                  this.isDoingBehavior = true;
                  this.parent.setTurnDelta(0.9F);
               }

            } else if (this.checkDrinkBehavior()) {
               if (this.parent.getVehicle() == null) {
                  this.isDoingBehavior = true;
                  this.parent.setTurnDelta(0.9F);
               }

            } else if (this.checkFertilizeFemale()) {
               if (this.parent.getVehicle() == null) {
                  this.isDoingBehavior = true;
                  this.parent.setTurnDelta(0.9F);
               }

            } else if (this.callToHutch((IsoHutch)null, false)) {
               if (this.parent.getVehicle() == null) {
                  this.isDoingBehavior = true;
                  this.parent.setTurnDelta(0.9F);
               }

            } else if (this.checkAttackBehavior()) {
               if (this.parent.getVehicle() == null) {
                  this.isDoingBehavior = true;
                  this.parent.setTurnDelta(0.9F);
               }

            }
         }
      }
   }

   private boolean checkAttackBehavior() {
      if (!this.parent.isFemale() && !this.parent.isBaby()) {
         if (this.parent.adef.dontAttackOtherMale) {
            return false;
         } else if (this.parent.canDoAction() && !this.parent.hasHitReaction()) {
            if (this.attackAnimalTimer > 0.0F) {
               return false;
            } else if (this.parent.isInMatingSeason() && this.parent.getAge() >= this.parent.getMinAgeForBaby()) {
               for(int var1 = 0; var1 < this.parent.connectedDZone.size(); ++var1) {
                  DesignationZoneAnimal var2 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var1);

                  for(int var3 = 0; var3 < var2.getAnimalsConnected().size(); ++var3) {
                     IsoAnimal var4 = (IsoAnimal)var2.getAnimalsConnected().get(var3);
                     if (!var4.isFemale() && var4 != this.parent && var4.fightingOpponent == null && var4.getAnimalType().equalsIgnoreCase(this.parent.getAnimalType()) && var4.getAge() >= var4.getMinAgeForBaby() && !var4.getBehavior().isDoingBehavior && !var4.isAnimalEating()) {
                        this.goAttack(var4);
                        this.parent.fightingOpponent = var4;
                        return true;
                     }
                  }
               }

               return false;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean callToHutch(IsoHutch var1, boolean var2) {
      if (this.enterHutchTimerAfterDestroy > 0) {
         this.enterHutchTimerAfterDestroy = (int)((float)this.enterHutchTimerAfterDestroy - GameTime.getInstance().getMultiplier());
         if (this.enterHutchTimerAfterDestroy < 0) {
            this.enterHutchTimerAfterDestroy = 0;
         }

         return false;
      } else if (!var2 && (!this.parent.isExistInTheWorld() || this.parent.adef.enterHutchTime == 0 || this.parent.adef.exitHutchTime == 0 || this.parent.adef.hutches == null || !this.parent.adef.isInsideHutchTime((Integer)null) || this.parent.hutch != null)) {
         return false;
      } else {
         boolean var3 = false;
         if (var1 == null) {
            var1 = this.parent.getData().getRegionHutch();
            var3 = true;
         }

         if (var1 == null) {
            return false;
         } else {
            if (var2) {
               this.forcedOutsideHutch = GameTime.getInstance().getCalender().getTimeInMillis() + 7200001L;
            }

            if (!var3) {
               return this.canGoToHutch(var1, var2);
            } else {
               for(int var4 = 0; var4 < this.parent.connectedDZone.size(); ++var4) {
                  DesignationZoneAnimal var5 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var4);

                  for(int var6 = 0; var6 < var5.hutchs.size(); ++var6) {
                     var1 = (IsoHutch)var5.hutchs.get(var6);
                     if (this.canGoToHutch(var1, var2)) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      }
   }

   public boolean canGoToHutch(IsoHutch var1, boolean var2) {
      if (var1.getAnimalInside().size() < var1.getMaxAnimals()) {
         if (var2 && !var1.isOpen()) {
            var1.toggleDoor();
         }

         if (var1.isOpen()) {
            if (this.hutchPathTimer <= -1) {
               if (this.parent.isBaby()) {
                  this.hutchPathTimer = Rand.Next(10, 30);
               } else if (this.parent.isFemale()) {
                  this.hutchPathTimer = Rand.Next(350, 500);
               } else {
                  this.hutchPathTimer = Rand.Next(950, 1000);
               }
            }

            if (var2) {
               this.hutchPathTimer = 1;
            }

            this.isDoingBehavior = true;
            this.behaviorAction = BehaviorAction.ENTERHUTCH;
            this.behaviorObject = var1;
            return true;
         }
      }

      return false;
   }

   private boolean checkFertilizeFemale() {
      this.parent.getData().findFemaleToInseminate((PZCalendar)null);
      if (!this.parent.getData().animalToInseminate.isEmpty()) {
         IsoAnimal var1 = (IsoAnimal)this.parent.getData().animalToInseminate.get(Rand.Next(0, this.parent.getData().animalToInseminate.size()));
         var1.getBehavior().blockMovement = true;
         var1.stopAllMovementNow();
         this.parent.stopAllMovementNow();
         this.behaviorAction = BehaviorAction.FERTILIZE;
         this.behaviorObject = var1;
         this.parent.getData().animalToInseminate.remove(var1);
         this.parent.pathToCharacter(var1);
         return true;
      } else {
         return false;
      }
   }

   private boolean checkDrinkBehavior() {
      if (!this.parent.isBaby() && !(this.parent.getStats().thirst < this.parent.adef.thirstHungerTrigger) && this.parent.fightingOpponent == null) {
         for(int var1 = 0; var1 < this.parent.connectedDZone.size(); ++var1) {
            DesignationZoneAnimal var2 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var1);
            if (this.tryDrinkFromGround(var2)) {
               return true;
            }
         }

         if (this.tryDrinkFromTrough()) {
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean tryDrinkFromGround(DesignationZoneAnimal var1) {
      if (!var1.foodOnGround.isEmpty()) {
         for(int var2 = 0; var2 < var1.foodOnGround.size(); ++var2) {
            IsoWorldInventoryObject var3 = (IsoWorldInventoryObject)var1.foodOnGround.get(var2);
            if (var3.getItem().isPureWater(true)) {
               this.behaviorAction = BehaviorAction.DRINKGROUND;
               this.behaviorObject = var3;
               if (var3.getSquare() != null && this.parent.getCurrentSquare() != null && var3.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                  this.parent.stopAllMovementNow();
                  this.parent.pathToLocation(var3.getSquare().getX(), var3.getSquare().getY(), var3.getSquare().getZ());
               } else {
                  this.isDoingBehavior = true;
                  this.doBehaviorAction();
               }

               return true;
            }
         }
      }

      return false;
   }

   private boolean tryDrinkFromTrough() {
      ArrayList var1 = this.getRandomTroughList();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoFeedingTrough var3 = (IsoFeedingTrough)var1.get(var2);
         if (!this.parent.ignoredTrough.contains(var3) && this.canDrinkFromTrough(var3)) {
            this.behaviorAction = BehaviorAction.DRINK;
            this.behaviorObject = var3;
            if (var3.getSquare() != null && this.parent.getCurrentSquare() != null && var3.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
               this.parent.stopAllMovementNow();
               this.parent.pathToTrough(var3);
            } else {
               this.isDoingBehavior = true;
               this.doBehaviorAction();
            }

            return true;
         }
      }

      return false;
   }

   public boolean canDrinkFromTrough(IsoFeedingTrough var1) {
      return var1 != null && !(var1.getWater() <= 0.0F);
   }

   public boolean canEatThis(InventoryItem var1) {
      if (!this.parent.adef.eatTypeTrough.contains("All") && !this.parent.adef.eatTypeTrough.contains(var1.getFullType()) && !this.parent.adef.eatTypeTrough.contains(var1.getAnimalFeedType())) {
         return var1 instanceof Food && this.parent.adef.eatTypeTrough.contains(((Food)var1).getFoodType());
      } else {
         return true;
      }
   }

   private boolean tryEatFromGround(DesignationZoneAnimal var1) {
      if (!var1.foodOnGround.isEmpty()) {
         for(int var2 = 0; var2 < var1.foodOnGround.size(); ++var2) {
            IsoWorldInventoryObject var3 = (IsoWorldInventoryObject)var1.foodOnGround.get(var2);
            if (this.parent.adef.eatTypeTrough != null && this.canEatThis(var3.getItem())) {
               this.behaviorAction = BehaviorAction.EATGROUND;
               this.behaviorObject = var3;
               if (var3.getSquare() != null && this.parent.getCurrentSquare() != null && var3.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                  this.parent.stopAllMovementNow();
                  this.parent.pathToLocation(var3.getSquare().getX(), var3.getSquare().getY(), var3.getSquare().getZ());
               } else {
                  this.isDoingBehavior = true;
                  this.doBehaviorAction();
               }

               return true;
            }
         }
      }

      return false;
   }

   public boolean checkEatBehavior() {
      if (!(this.parent.getStats().hunger < this.parent.adef.thirstHungerTrigger) && this.parent.fightingOpponent == null) {
         if (this.parent.getVehicle() != null) {
            return this.eatFromVehicle();
         } else if (this.parent.isBaby() && this.tryToEatFromMom(true)) {
            return true;
         } else {
            for(int var1 = 0; var1 < this.parent.connectedDZone.size(); ++var1) {
               DesignationZoneAnimal var2 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var1);
               if (this.tryEatFromGround(var2)) {
                  return true;
               }
            }

            if (this.tryEatFromTrough()) {
               return true;
            } else if (this.tryEatGrass()) {
               return false;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void forceEatFromMom() {
      this.parent.stopAllMovementNow();
      this.tryToEatFromMom(false);
      this.behaviorCheckTimer = 500.0F;
   }

   private boolean tryToEatFromMom(boolean var1) {
      if (this.parent.mother != null && this.parent.mother.getBehavior() != null && !this.parent.mother.getBehavior().isDoingBehavior && this.parent.mother.isExistInTheWorld() && this.parent.mother.haveEnoughMilkToFeedFrom() && this.parent.mother.getCurrentState().equals(AnimalIdleState.instance())) {
         this.parent.stopAllMovementNow();
         this.parent.mother.getBehavior().blockMovement = true;
         this.parent.mother.stopAllMovementNow();
         this.parent.pathToLocation(this.parent.mother.getCurrentSquare().x, this.parent.mother.getCurrentSquare().y, this.parent.mother.getCurrentSquare().getZ());
         this.behaviorObject = this.parent.mother;
         this.behaviorAction = BehaviorAction.EATMOM;
         this.isDoingBehavior = true;
         if (var1 && this.parent.mother != null && !this.parent.mother.getBabies().isEmpty()) {
            for(int var2 = 0; var2 < this.parent.mother.getBabies().size(); ++var2) {
               if (this.parent.mother.getBabies().get(var2) != this.parent) {
                  ((IsoAnimal)this.parent.mother.getBabies().get(var2)).getBehavior().forceEatFromMom();
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void eatFromMom() {
      this.parent.mother.getBehavior().blockMovement = false;
      this.parent.faceThisObject(this.parent.mother);
      this.parent.setVariable("idleAction", "eat");
      this.parent.setVariable("eatingAnim", "feed");
   }

   private boolean tryEatFromTrough() {
      ArrayList var1 = this.getRandomTroughList();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoFeedingTrough var3 = (IsoFeedingTrough)var1.get(var2);
         if (!this.parent.ignoredTrough.contains(var3) && this.canEatFromTrough(var3)) {
            this.behaviorAction = BehaviorAction.EATTROUGH;
            this.behaviorObject = var3;
            if (var3.getSquare() != null && this.parent.getCurrentSquare() != null && var3.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
               this.parent.stopAllMovementNow();
               this.parent.pathToTrough(var3);
            } else {
               this.isDoingBehavior = true;
               this.doBehaviorAction();
            }

            return true;
         }
      }

      return false;
   }

   private boolean tryEatGrass() {
      if (this.parent.adef.eatGrass && this.parent.getCurrentSquare() != null) {
         IsoObject var1 = this.parent.getCurrentSquare().getFloor();
         if (var1 != null && var1.getSprite().getProperties().Is("grassFloor") && this.parent.getCurrentSquare().checkHaveGrass()) {
            this.parent.stopAllMovementNow();
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
               this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.getBehavior().wanderMulMod = 0.0F;
            this.parent.getData().eatingGrass = true;
            return true;
         }
      }

      if (this.parent.getStateEventDelayTimer() <= 0.0F) {
         this.parent.setStateEventDelayTimer(10.0F);
      }

      return false;
   }

   private boolean canEatFromTrough(IsoFeedingTrough var1) {
      if (this.parent.adef.eatTypeTrough != null && var1.getContainer() != null) {
         for(int var2 = 0; var2 < var1.getContainer().getItems().size(); ++var2) {
            InventoryItem var3 = (InventoryItem)var1.getContainer().getItems().get(var2);
            if (!(var3 instanceof Food) || !((Food)var3).isRotten()) {
               if (this.parent.adef.eatTypeTrough.contains("All") || this.parent.adef.eatTypeTrough.contains(var3.getFullType()) || this.parent.adef.eatTypeTrough.contains(var3.getAnimalFeedType())) {
                  return true;
               }

               if (var3 instanceof Food && this.parent.adef.eatTypeTrough.contains(((Food)var3).getFoodType())) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public ArrayList<IsoFeedingTrough> getRandomTroughList() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.parent.connectedDZone.size(); ++var2) {
         DesignationZoneAnimal var3 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var2);
         var1.addAll(var3.troughs);
      }

      shuffleList(var1);
      return var1;
   }

   public static void shuffleList(ArrayList<IsoFeedingTrough> var0) {
      int var1 = var0.size();
      Random var2 = new Random();
      var2.nextInt();

      for(int var3 = 0; var3 < var1; ++var3) {
         int var4 = var3 + var2.nextInt(var1 - var3);
         swap(var0, var3, var4);
      }

   }

   private static void swap(List<IsoFeedingTrough> var0, int var1, int var2) {
      IsoFeedingTrough var3 = (IsoFeedingTrough)var0.get(var1);
      var0.set(var1, (IsoFeedingTrough)var0.get(var2));
      var0.set(var2, var3);
   }

   public boolean eatFromVehicle() {
      if (this.parent.isBaby() && this.parent.needMom() && this.parent.getMother() != null && this.parent.mother.haveEnoughMilkToFeedFrom()) {
         Stats var10000 = this.parent.getStats();
         var10000.hunger -= 0.2F;
         var10000 = this.parent.getStats();
         var10000.thirst -= 0.2F;
         this.parent.getStats().hunger = Math.max(0.0F, this.parent.getStats().hunger);
         this.parent.getStats().thirst = Math.max(0.0F, this.parent.getStats().thirst);
         this.parent.mother.getData().setMilkQuantity(this.parent.mother.getData().getMilkQuantity() - Rand.Next(0.2F / this.parent.adef.hungerBoost, 0.5F / this.parent.adef.hungerBoost));
         return true;
      } else {
         BaseVehicle var1 = this.parent.getVehicle();
         VehiclePart var2 = var1.getPartById("TrailerAnimalFood");
         InventoryItem var3 = null;
         if (var2 != null && var2.getItemContainer() != null) {
            for(int var4 = 0; var4 < var2.getItemContainer().getItems().size(); ++var4) {
               InventoryItem var5 = (InventoryItem)var2.getItemContainer().getItems().get(var4);
               if (this.parent.adef.eatTypeTrough != null) {
                  for(int var6 = 0; var6 < this.parent.adef.eatTypeTrough.size(); ++var6) {
                     String var7 = (String)this.parent.adef.eatTypeTrough.get(var6);
                     if (var5 instanceof Food) {
                        if (var7.equals(((Food)var5).getFoodType()) || var7.equals(var5.getAnimalFeedType())) {
                           var3 = var5;
                           break;
                        }
                     } else if (var5 instanceof DrainableComboItem && var7.equals(var5.getAnimalFeedType())) {
                        var3 = var5;
                        break;
                     }
                  }
               }

               if (var3 != null) {
                  break;
               }
            }

            if (var3 != null) {
               this.parent.getData().eatItem(var3, false);
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private void updateAcceptance() {
      if (this.parent.heldBy != null) {
         if (this.parent.playerAcceptanceList.get(this.parent.heldBy.getOnlineID()) == null) {
            this.parent.playerAcceptanceList.put(this.parent.heldBy.getOnlineID(), Rand.Next(0.0F, 20.0F));
         }

         float var1 = (Float)this.parent.playerAcceptanceList.get(this.parent.heldBy.getOnlineID());
         var1 += Rand.Next(0.002F, 0.005F) * ((100.0F - this.parent.stressLevel) / 100.0F) * GameTime.getInstance().getMultiplier();
         var1 = Math.min(100.0F, var1);
         if (var1 > 100.0F) {
            var1 = 100.0F;
         }

         this.parent.playerAcceptanceList.put(this.parent.heldBy.getOnlineID(), var1);
      }

   }

   private void followChr() {
      if (!this.blockMovement && this.parent.eatFromTrough == null && this.parent.eatFromGround == null) {
         if (this.parent.getData().getAttachedPlayer() != null) {
            if (this.followChrTimer > 0) {
               this.followChrTimer = (int)((float)this.followChrTimer - GameTime.getInstance().getMultiplier());
               return;
            }

            this.followChrTimer = 150;
            IsoPlayer var1 = this.parent.getData().getAttachedPlayer();
            float var2 = IsoUtils.DistanceTo(this.parent.getX(), this.parent.getY(), var1.getX(), var1.getY());
            if (var1.isPlayerMoving() && var2 > 2.0F) {
               this.walkToChr(var1, 2);
            }

            if (var2 < 4.0F && !this.parent.isMoving()) {
               return;
            }

            if (var2 >= 4.0F && !var1.isPlayerMoving()) {
               this.walkToChr(var1, 5);
            }

            if (var2 >= 15.0F) {
               this.parent.getData().getAttachedPlayer().getAttachedAnimals().remove(this.parent);
               this.parent.getData().setAttachedPlayer((IsoPlayer)null);
            }
         } else {
            this.parent.setVariable("animalSpeed", 1.0F);
            this.followChrTimer = 0;
         }

      }
   }

   private void walkToChr(IsoGameCharacter var1, Integer var2) {
      this.parent.setVariable("animalSpeed", 1.2F);
      tempVector2.x = var1.getX();
      tempVector2.y = var1.getY();
      Vector2 var10000 = tempVector2;
      var10000.x -= this.parent.getX();
      var10000 = tempVector2;
      var10000.y -= this.parent.getY();
      float var3 = -1.0F;
      tempVector2.setLength(tempVector2.getLength() + var3);
      this.parent.pathToLocation((int)(this.parent.getX() + tempVector2.x), (int)(this.parent.getY() + tempVector2.y), (int)var1.getZ());
      if (tempVector2.getLength() > 5.0F) {
         this.parent.setVariable("animalRunning", true);
      } else {
         this.parent.setVariable("animalRunning", false);
      }

   }

   private void fleeFromAttacker() {
      if (this.parent.attackedBy == null) {
         this.removeAttacked();
      } else {
         long var1 = GameTime.getInstance().getCalender().getTimeInMillis();
         this.lastFleeTimer = var1;
         if (var1 - this.parent.attackedTimer <= 1100000L && !(this.parent.DistTo(this.parent.fightingOpponent) > 40.0F)) {
            if (this.parent.getData().getAttachedPlayer() != null) {
               this.parent.getData().getAttachedPlayer().removeAttachedAnimal(this.parent);
               this.parent.getData().setAttachedPlayer((IsoPlayer)null);
            }

            this.parent.getData().setAttachedTree((IsoObject)null);
            if (this.parent.adef.attackBack && this.parent.attackedBy instanceof IsoPlayer) {
               if (!this.parent.attackedBy.isInvisible() && !((IsoPlayer)this.parent.attackedBy).isGhostMode()) {
                  this.parent.fightingOpponent = this.parent.attackedBy;
               } else {
                  this.parent.fightingOpponent = null;
               }

            } else {
               tempVector2.x = this.parent.getX();
               tempVector2.y = this.parent.getY();
               Vector2 var10000 = tempVector2;
               var10000.x -= this.parent.attackedBy.getX();
               var10000 = tempVector2;
               var10000.y -= this.parent.attackedBy.getY();
               tempVector2.setLength(Math.max(20.0F, 20.0F - tempVector2.getLength()));
               int var3 = tempVector2.floorX();
               int var4 = tempVector2.floorY();
               this.parent.setVariable("animalRunning", true);
               if (!this.parent.isAnimalMoving() && !this.parent.isAnimalAttacking()) {
                  this.resetBehaviorAction();
                  if (this.parent.getDir() != IsoDirections.fromAngle(tempVector2)) {
                     this.parent.pathToLocation((int)this.parent.getX() + var3, (int)this.parent.getY() + var4, (int)this.parent.getZ());
                  }

                  if (!this.parent.isAnimalMoving()) {
                     this.parent.pathToLocation((int)this.parent.getX() + var3, (int)this.parent.getY() + var4, (int)this.parent.getZ());
                  }
               }

            }
         } else {
            this.removeAttacked();
         }
      }
   }

   private void removeAttacked() {
      this.parent.setAttackedBy((IsoGameCharacter)null);
      this.parent.attackedTimer = -1L;
   }

   private void fleeFromChr() {
      if (this.parent.spottedChr != null) {
         if (this.parent.spottedChr != this.parent.atkTarget) {
            if (this.parent.getData().getAttachedPlayer() != this.parent.spottedChr) {
               if (this.parent.fightingOpponent != this.parent.spottedChr) {
                  if (this.timerFleeAgain > 0.0F) {
                     this.timerFleeAgain -= GameTime.getInstance().getMultiplier();
                     if (this.timerFleeAgain < 0.0F) {
                        this.timerFleeAgain = 0.0F;
                     }
                  }

                  boolean var1 = this.parent.geneticDisorder.contains("craven");
                  int var2;
                  if (!var1 && !this.parent.isWild() && this.parent.spottedChr instanceof IsoPlayer && !this.parent.adef.alwaysFleeHumans) {
                     if (!((IsoPlayer)this.parent.spottedChr).isPlayerMoving()) {
                        return;
                     }

                     var2 = 0;
                     var2 += (int)(this.parent.stressLevel / 1.8F);
                     float var3 = (Float)this.parent.playerAcceptanceList.get(((IsoPlayer)this.parent.spottedChr).getOnlineID());
                     var2 -= (int)(var3 / 2.0F);
                     if (var3 < 70.0F) {
                        float var4 = this.parent.DistTo(this.parent.spottedChr);
                        var2 += (int)(var4 * 5.0F);
                     }

                     if (((IsoPlayer)this.parent.spottedChr).isRunning()) {
                        var2 *= 4;
                     }

                     var2 = Math.max(0, var2);
                     if (var2 == 0) {
                        return;
                     }

                     if (var3 >= 40.0F && this.parent.stressLevel < 70.0F) {
                        return;
                     }

                     if (Rand.Next(10000) > var2) {
                        return;
                     }
                  }

                  if (var1 || this.parent.luredBy != null || this.parent.isWild() || !(this.parent.spottedChr instanceof IsoZombie) || this.parent.adef.fleeZombies) {
                     tempVector2.x = this.parent.getX();
                     tempVector2.y = this.parent.getY();
                     Vector2 var10000 = tempVector2;
                     var10000.x -= this.parent.spottedChr.getX();
                     var10000 = tempVector2;
                     var10000.y -= this.parent.spottedChr.getY();
                     tempVector2.setLength(Math.max(10.0F, 10.0F - tempVector2.getLength()));
                     if (this.parent.isWild()) {
                        tempVector2.setLength(Math.max(30.0F, 30.0F - tempVector2.getLength()));
                     }

                     var2 = tempVector2.floorX();
                     int var5 = tempVector2.floorY();
                     this.parent.setVariable("animalRunning", Math.abs(var5) > 5 || Math.abs(var2) > 5);
                     if (this.parent.isWild()) {
                        this.parent.setVariable("animalRunning", true);
                     }

                     if (!this.parent.isWild() && this.parent.spottedChr instanceof IsoPlayer && (Float)this.parent.playerAcceptanceList.get(((IsoPlayer)this.parent.spottedChr).getOnlineID()) > 50.0F) {
                        this.parent.setVariable("animalRunning", false);
                     }

                     if (this.parent.stressLevel > 50.0F) {
                        this.parent.setVariable("animalRunning", true);
                     }

                     if (!this.parent.isAnimalMoving() && this.timerFleeAgain == 0.0F && !this.parent.isAnimalAttacking()) {
                        this.resetBehaviorAction();
                        this.timerFleeAgain = 200.0F;
                        this.parent.playStressedSound();
                        if (this.parent.getDir() != IsoDirections.fromAngle(tempVector2)) {
                           this.parent.pathToLocation((int)this.parent.getX() + var2, (int)this.parent.getY() + var5, (int)this.parent.getZ());
                        }

                        if (!this.parent.isAnimalMoving()) {
                           this.parent.pathToLocation((int)this.parent.getX() + var2, (int)this.parent.getY() + var5, (int)this.parent.getZ());
                        }
                     }

                  }
               }
            }
         }
      }
   }

   public void forceFleeFromChr(IsoGameCharacter var1) {
      tempVector2.x = this.parent.getX();
      tempVector2.y = this.parent.getY();
      Vector2 var10000 = tempVector2;
      var10000.x -= var1.getX();
      var10000 = tempVector2;
      var10000.y -= var1.getY();
      tempVector2.setLength(Math.max(10.0F, 10.0F - tempVector2.getLength()));
      int var2 = tempVector2.floorX();
      int var3 = tempVector2.floorY();
      this.parent.setVariable("animalRunning", true);
      this.resetBehaviorAction();
      this.timerFleeAgain = 200.0F;
      this.parent.playStressedSound();
      if (this.parent.getDir() != IsoDirections.fromAngle(tempVector2)) {
         this.parent.pathToLocation((int)this.parent.getX() + var2, (int)this.parent.getY() + var3, (int)this.parent.getZ());
      }

      if (!this.parent.isAnimalMoving()) {
         this.parent.pathToLocation((int)this.parent.getX() + var2, (int)this.parent.getY() + var3, (int)this.parent.getZ());
      }

   }

   public void spotted(IsoMovingObject var1, boolean var2, float var3) {
      this.parent.spottedChr = null;
      if (this.lastAlerted > 0.0F) {
         this.lastAlerted -= GameTime.getInstance().getMultiplier();
      }

      if (this.lastAlerted < 0.0F) {
         this.lastAlerted = 0.0F;
      }

      if (!GameClient.bClient) {
         if (this.parent.getCurrentSquare() != null) {
            if (var1.getCurrentSquare() != null) {
               if (!this.parent.getCurrentSquare().getProperties().Is(IsoFlagType.smoke)) {
                  if (!(var1 instanceof IsoPlayer) || !((IsoPlayer)var1).isGhostMode()) {
                     IsoGameCharacter var4 = (IsoGameCharacter)Type.tryCastTo(var1, IsoGameCharacter.class);
                     if (var4 == null || !var4.isDead()) {
                        if (var4 instanceof IsoPlayer && !this.parent.isWild()) {
                           if (this.parent.playerAcceptanceList.get(var4.getOnlineID()) == null) {
                              this.parent.playerAcceptanceList.put(var4.getOnlineID(), Rand.Next(0.0F, 20.0F));
                           }

                           if (var3 < 10.0F) {
                              float var5 = (Float)this.parent.playerAcceptanceList.get(var4.getOnlineID());
                              var5 += Rand.Next(5.0E-4F, 6.0E-4F) * ((100.0F - this.parent.stressLevel) / 100.0F) * GameTime.getInstance().getMultiplier();
                              var5 = Math.min(100.0F, var5);
                              if (var5 > 100.0F) {
                                 var5 = 100.0F;
                              }

                              this.parent.playerAcceptanceList.put(var4.getOnlineID(), var5);
                              if (var4.isRunning()) {
                                 this.parent.changeStress(GameTime.getInstance().getMultiplier() / 1000.0F);
                              }

                              if (!this.parent.isAnimalMoving() && this.parent.fightingOpponent == null && this.parent.adef.attackIfStressed && this.parent.stressLevel > 80.0F && var5 < 30.0F && !this.parent.getBehavior().isDoingBehavior && this.parent.atkTarget == null && this.attackAnimalTimer == 0.0F && Rand.NextBool(300 - (int)this.parent.stressLevel)) {
                                 this.parent.fightingOpponent = var4;
                                 this.parent.setVariable("animalRunning", true);
                                 this.parent.getBehavior().goAttack(var4);
                                 return;
                              }
                           }
                        }

                        if (!(var4 instanceof IsoZombie) || this.parent.adef.fleeZombies) {
                           if (this.parent.isWild() && var3 < 3.0F) {
                              this.parent.spottedChr = var1;
                              this.fleeFromChr();
                           } else {
                              boolean var11 = false;
                              if (this.parent.isWild() && var4 instanceof IsoPlayer && var4.isPlayerMoving()) {
                                 float var6 = 8000.0F;
                                 if (var4.isSneaking()) {
                                    var6 = 800.0F;
                                 }

                                 if (var4.isRunning()) {
                                    var6 = 500000.0F;
                                 }

                                 if (var3 <= (float)this.parent.adef.spottingDist) {
                                    var6 *= 1.0F + ((float)this.parent.adef.spottingDist - var3);
                                 }

                                 var6 /= (float)(var4.getPerkLevel(PerkFactory.Perks.Tracking) + 2) * 0.5F;
                                 var6 /= (float)(var4.getPerkLevel(PerkFactory.Perks.Sneak) + 2) * 0.3F;
                                 var6 /= (float)(var4.getPerkLevel(PerkFactory.Perks.Lightfoot) + 2) * 0.25F;
                                 var6 /= (float)(var4.getPerkLevel(PerkFactory.Perks.Nimble) + 2) * 0.25F;
                                 Vector2 var7 = IsoGameCharacter.getTempo();
                                 var7.x = var1.getX();
                                 var7.y = var1.getY();
                                 var7.x -= this.parent.getX();
                                 var7.y -= this.parent.getY();
                                 if (var1.getCurrentSquare().getZ() != this.parent.getCurrentSquare().getZ()) {
                                    int var8 = Math.abs(var1.getCurrentSquare().getZ() - this.parent.getCurrentSquare().getZ()) * 5;
                                    ++var8;
                                    var6 /= (float)var8;
                                 }

                                 var7.normalize();
                                 Vector2 var12 = this.parent.getLookVector(IsoGameCharacter.getTempo2());
                                 float var9 = var12.dot(var7);
                                 if (var9 < -0.4F) {
                                    var6 /= 16.0F;
                                 } else if (var9 < -0.2F) {
                                    var6 /= 3.0F;
                                 } else if (var9 < -0.0F) {
                                    var6 /= 1.5F;
                                 } else if (var9 < 0.2F) {
                                    var6 /= 1.5F;
                                 } else if (var9 <= 0.4F) {
                                    var6 *= 3.0F;
                                 } else if (var9 <= 0.6F) {
                                    var6 *= 11.0F;
                                 } else if (var9 <= 0.8F) {
                                    var6 *= 24.0F;
                                 } else {
                                    var6 *= 44.0F;
                                 }

                                 int var10 = Rand.Next(25000);
                                 if (var10 > (int)var6) {
                                    if (var4.isSneaking() && var4.isOutside() && var3 <= (float)this.parent.adef.spottingDist && this.parent.adef.addTrackingXp) {
                                       if (GameServer.bServer) {
                                          if (var4.isPlayerMoving() && Rand.NextBool(200)) {
                                             GameServer.addXp((IsoPlayer)var4, PerkFactory.Perks.Tracking, (float)((int)Rand.Next(1.0F, 3.0F)));
                                          } else if (!var4.isPlayerMoving() && Rand.NextBool(200)) {
                                             GameServer.addXp((IsoPlayer)var4, PerkFactory.Perks.Tracking, (float)((int)Rand.Next(1.0F, 3.0F)));
                                          }

                                          if (var4.isSneaking()) {
                                             if (var4.isPlayerMoving() && Rand.NextBool(385)) {
                                                GameServer.addXp((IsoPlayer)var4, PerkFactory.Perks.Sneak, (float)((int)Rand.Next(1.0F, 3.0F)));
                                             }

                                             if (var4.isPlayerMoving() && Rand.NextBool(385)) {
                                                GameServer.addXp((IsoPlayer)var4, PerkFactory.Perks.Nimble, (float)((int)Rand.Next(1.0F, 3.0F)));
                                             }

                                             if (var4.isPlayerMoving() && Rand.NextBool(385)) {
                                                GameServer.addXp((IsoPlayer)var4, PerkFactory.Perks.Lightfoot, (float)((int)Rand.Next(1.0F, 3.0F)));
                                             }
                                          }
                                       } else if (!GameClient.bClient) {
                                          if (var4.isPlayerMoving() && Rand.NextBool(200)) {
                                             var4.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(1.0F, 3.0F));
                                          } else if (!var4.isPlayerMoving() && Rand.NextBool(200)) {
                                             var4.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(1.0F, 3.0F));
                                          }

                                          if (var4.isSneaking()) {
                                             if (var4.isPlayerMoving() && Rand.NextBool(385)) {
                                                var4.getXp().AddXP(PerkFactory.Perks.Sneak, Rand.Next(1.0F, 3.0F));
                                             }

                                             if (var4.isPlayerMoving() && Rand.NextBool(385)) {
                                                var4.getXp().AddXP(PerkFactory.Perks.Nimble, Rand.Next(1.0F, 3.0F));
                                             }

                                             if (var4.isPlayerMoving() && Rand.NextBool(385)) {
                                                var4.getXp().AddXP(PerkFactory.Perks.Lightfoot, Rand.Next(1.0F, 3.0F));
                                             }
                                          }
                                       }
                                    }

                                    return;
                                 }

                                 var11 = true;
                              }

                              if (var4 instanceof IsoZombie && var3 <= 10.0F) {
                                 var11 = true;
                                 this.parent.setDebugStress(this.parent.getStress() + Rand.Next(2.0E-4F, 8.0E-4F) * GameTime.getInstance().getMultiplier() / (12.0F - var3));
                                 if (this.parent.getStress() > 100.0F) {
                                    this.parent.setDebugStress(100.0F);
                                 }

                                 if (var3 <= 6.0F) {
                                    this.parent.spottedChr = var1;
                                    this.fleeFromChr();
                                    return;
                                 }
                              }

                              if (this.parent.getCurrentSquare() == null) {
                                 this.parent.ensureOnTile();
                              }

                              if (var1.getCurrentSquare() == null) {
                                 var1.ensureOnTile();
                              }

                              if (var3 < (float)this.parent.adef.spottingDist && var11) {
                                 if (this.parent.adef.canBeAlerted && this.lastAlerted <= 0.0F) {
                                    this.parent.setIsAlerted(true);
                                    this.parent.alertedChr = var1;
                                 } else {
                                    this.parent.spottedChr = var1;
                                    this.fleeFromChr();
                                 }
                              }

                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean canBeAttached() {
      return this.parent.adef.canBeAttached;
   }

   public void setBlockMovement(boolean var1) {
      if (var1) {
         this.parent.stopAllMovementNow();
      } else {
         this.blockedFor = 0.0F;
      }

      this.blockMovement = var1;
   }

   public void setHourBeforeLeavingHutch(int var1) {
      this.forcedOutsideHutch = GameTime.getInstance().getCalender().getTimeInMillis() + 3600000L * (long)var1 + 1L;
   }
}
