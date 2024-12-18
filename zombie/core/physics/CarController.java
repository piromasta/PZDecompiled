package zombie.core.physics;

import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleType;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.pathfind.VehiclePoly;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.EngineRPMData;
import zombie.vehicles.TransmissionNumber;

public final class CarController {
   public final BaseVehicle vehicleObject;
   public float clientForce = 0.0F;
   public float EngineForce = 0.0F;
   public float BrakingForce = 0.0F;
   private float VehicleSteering = 0.0F;
   boolean isGas = false;
   boolean isGasR = false;
   boolean isBreak = false;
   private float atRestTimer = -1.0F;
   private float regulatorTimer = 0.0F;
   public boolean isEnable = false;
   private final Transform tempXfrm = new Transform();
   private final Vector2 tempVec2 = new Vector2();
   private final Vector3f tempVec3f = new Vector3f();
   private final Vector3f tempVec3f_2 = new Vector3f();
   private final Vector3f tempVec3f_3 = new Vector3f();
   private static final Vector3f _UNIT_Y = new Vector3f(0.0F, 1.0F, 0.0F);
   public boolean acceleratorOn = false;
   public boolean brakeOn = false;
   public float speed = 0.0F;
   public static GearInfo[] gears = new GearInfo[3];
   public final ClientControls clientControls = new ClientControls();
   private boolean engineStartingFromKeyboard;
   private static final BulletVariables bulletVariables;
   float drunkDelayCommandTimer = 0.0F;
   boolean wasBreaking = false;
   boolean wasGas = false;
   boolean wasGasR = false;
   boolean wasSteering = false;

   public CarController(BaseVehicle var1) {
      this.vehicleObject = var1;
      this.engineStartingFromKeyboard = false;
      VehicleScript var2 = var1.getScript();
      float var3 = var1.savedPhysicsZ;
      if (Float.isNaN(var3)) {
         float var4 = 0.0F;
         if (var2.getWheelCount() > 0) {
            Vector3f var5 = var2.getModelOffset();
            var4 += var5.y();
            var4 += var2.getWheel(0).getOffset().y() - var2.getWheel(0).radius;
         }

         float var6 = var2.getCenterOfMassOffset().y() - var2.getExtents().y() / 2.0F;
         var3 = (float)(PZMath.fastfloor(var1.getZ()) * 3) * 0.8164967F - Math.min(var4, var6);
         if (var2.getWheelCount() == 0) {
            var3 = PZMath.max(var3, (float)(PZMath.fastfloor(var1.getZ()) * 3) * 0.8164967F + 0.1F);
         }

         var1.jniTransform.origin.y = var3;
      }

      if (!GameServer.bServer) {
         Bullet.addVehicle(var1.VehicleID, var1.getX(), var1.getY(), var3, var1.savedRot.x, var1.savedRot.y, var1.savedRot.z, var1.savedRot.w, var2.getFullName());
         Bullet.setVehicleStatic(var1, var1.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote));
      }

   }

   public GearInfo findGear(float var1) {
      for(int var2 = 0; var2 < gears.length; ++var2) {
         if (var1 >= (float)gears[var2].minSpeed && var1 < (float)gears[var2].maxSpeed) {
            return gears[var2];
         }
      }

      return null;
   }

   public void accelerator(boolean var1) {
      this.acceleratorOn = var1;
   }

   public void brake(boolean var1) {
      this.brakeOn = var1;
   }

   public ClientControls getClientControls() {
      return this.clientControls;
   }

   public void update() {
      if (this.vehicleObject.getVehicleTowedBy() == null) {
         VehicleScript var1 = this.vehicleObject.getScript();
         this.speed = this.vehicleObject.getCurrentSpeedKmHour();
         boolean var2 = this.vehicleObject.getDriver() != null && this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.Drunk) > 1;
         float var3 = 0.0F;
         Vector3f var4 = this.vehicleObject.getLinearVelocity(this.tempVec3f_2);
         var4.y = 0.0F;
         if ((double)var4.length() > 0.5) {
            var4.normalize();
            Vector3f var5 = this.tempVec3f;
            this.vehicleObject.getForwardVector(var5);
            var3 = var4.dot(var5);
         }

         float var14 = 1.0F;
         float var6;
         if (GameClient.bClient) {
            var6 = this.vehicleObject.jniSpeed / Math.min(120.0F, (float)ServerOptions.instance.SpeedLimit.getValue());
            var6 *= var6;
            var14 = GameTime.getInstance().Lerp(1.0F, BaseVehicle.getFakeSpeedModifier(), var6);
         }

         var6 = this.vehicleObject.getCurrentSpeedKmHour() * var14;
         this.isGas = false;
         this.isGasR = false;
         this.isBreak = false;
         if (this.clientControls.forward) {
            if (var3 < 0.0F) {
               this.isBreak = true;
            }

            if (var3 >= 0.0F) {
               this.isGas = true;
            }

            this.isGasR = false;
         }

         if (this.clientControls.backward) {
            if (var3 > 0.0F) {
               this.isBreak = true;
            }

            if (var3 <= 0.0F) {
               this.isGasR = true;
            }

            this.isGas = false;
         }

         if (this.clientControls.brake) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
         }

         if (this.clientControls.forward && this.clientControls.backward) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
         }

         if (var2 && this.vehicleObject.engineState != BaseVehicle.engineStateTypes.Idle) {
            if (this.isBreak && !this.wasBreaking) {
               this.isBreak = this.delayCommandWhileDrunk(this.isBreak);
            }

            if (this.isGas && !this.wasGas) {
               this.isGas = this.delayCommandWhileDrunk(this.isGas);
            }

            if (this.isGasR && !this.wasGasR) {
               this.isGasR = this.delayCommandWhileDrunk(this.isGas);
            }

            if (this.clientControls.steering != 0.0F && !this.wasSteering) {
               this.clientControls.steering = this.delayCommandWhileDrunk(this.clientControls.steering);
            }
         }

         this.updateRegulator();
         this.wasBreaking = this.isBreak;
         this.wasGas = this.isGas;
         this.wasGasR = this.isGasR;
         this.wasSteering = this.clientControls.steering != 0.0F;
         if (!this.isGasR && this.vehicleObject.isInvalidChunkAhead()) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
         } else if (!this.isGas && this.vehicleObject.isInvalidChunkBehind()) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
         }

         if (this.clientControls.shift) {
            this.isGas = false;
            this.isBreak = false;
            this.isGasR = false;
            this.clientControls.wasUsingParkingBrakes = false;
         }

         float var7 = this.vehicleObject.throttle;
         if (!this.isGas && !this.isGasR) {
            var7 -= GameTime.getInstance().getMultiplier() / 30.0F;
         } else {
            var7 += GameTime.getInstance().getMultiplier() / 30.0F;
         }

         if (var7 < 0.0F) {
            var7 = 0.0F;
         }

         if (var7 > 1.0F) {
            var7 = 1.0F;
         }

         if (this.vehicleObject.isRegulator() && !this.isGas && !this.isGasR) {
            var7 = 0.5F;
            if (var6 < this.vehicleObject.getRegulatorSpeed()) {
               this.isGas = true;
            }
         }

         this.vehicleObject.throttle = var7;
         float var8 = GameTime.getInstance().getMultiplier() / 0.8F;
         ControlState var9 = CarController.ControlState.NoControl;
         if (this.isBreak) {
            var9 = CarController.ControlState.Braking;
         } else if (this.isGas && !this.isGasR) {
            var9 = CarController.ControlState.Forward;
         } else if (!this.isGas && this.isGasR) {
            var9 = CarController.ControlState.Reverse;
         }

         if (var9 != CarController.ControlState.NoControl) {
            UIManager.speedControls.SetCurrentGameSpeed(1);
         }

         if (var9 == CarController.ControlState.NoControl) {
            this.control_NoControl();
         }

         if (var9 == CarController.ControlState.Reverse) {
            this.control_Reverse(var6);
         }

         if (var9 == CarController.ControlState.Forward) {
            this.control_ForwardNew(var6);
         }

         this.updateBackSignal();
         if (var9 == CarController.ControlState.Braking) {
            this.control_Braking();
         }

         this.updateBrakeLights();
         BaseVehicle var10 = this.vehicleObject.getVehicleTowedBy();
         if (var10 != null && var10.getDriver() == null && this.vehicleObject.getDriver() != null && !GameClient.bClient) {
            this.vehicleObject.addPointConstraint((IsoPlayer)null, var10, this.vehicleObject.getTowAttachmentSelf(), var10.getTowAttachmentSelf());
         }

         this.updateRammingSound(var6);
         float var11;
         if (Math.abs(this.clientControls.steering) > 0.1F) {
            var11 = 1.0F - this.speed / this.vehicleObject.getMaxSpeed();
            if (var11 < 0.1F) {
               var11 = 0.1F;
            }

            this.VehicleSteering -= (this.clientControls.steering + this.VehicleSteering) * 0.06F * var8 * var11;
         } else if ((double)Math.abs(this.VehicleSteering) <= 0.04) {
            this.VehicleSteering = 0.0F;
         } else if (this.VehicleSteering > 0.0F) {
            this.VehicleSteering -= 0.04F * var8;
            this.VehicleSteering = Math.max(this.VehicleSteering, 0.0F);
         } else {
            this.VehicleSteering += 0.04F * var8;
            this.VehicleSteering = Math.min(this.VehicleSteering, 0.0F);
         }

         var11 = var1.getSteeringClamp(this.speed);
         this.VehicleSteering = PZMath.clamp(this.VehicleSteering, -var11, var11);
         BulletVariables var12 = bulletVariables.set(this.vehicleObject, this.EngineForce, this.BrakingForce, this.VehicleSteering);
         this.checkTire(var12);
         this.EngineForce = var12.engineForce;
         this.BrakingForce = var12.brakingForce;
         this.VehicleSteering = var12.vehicleSteering;
         if (this.vehicleObject.isDoingOffroad()) {
            int var13 = this.vehicleObject.getTransmissionNumber();
            if (var13 <= 0) {
               var13 = 1;
            }

            this.EngineForce = (float)((double)this.EngineForce / ((double)var13 * 1.5));
         }

         this.vehicleObject.setCurrentSteering(this.VehicleSteering);
         this.vehicleObject.setBraking(this.isBreak);
         if (!GameServer.bServer) {
            this.checkShouldBeActive();
            Bullet.controlVehicle(this.vehicleObject.VehicleID, this.EngineForce, this.BrakingForce, this.VehicleSteering);
            if (this.EngineForce > 0.0F && this.vehicleObject.engineState == BaseVehicle.engineStateTypes.Idle && !this.engineStartingFromKeyboard) {
               this.engineStartingFromKeyboard = true;
               if (GameClient.bClient) {
                  Boolean var15 = this.vehicleObject.getDriver().getInventory().haveThisKeyId(this.vehicleObject.getKeyId()) != null ? Boolean.TRUE : Boolean.FALSE;
                  GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "startEngine", "haveKey", var15);
               } else {
                  this.vehicleObject.tryStartEngine();
               }
            }

            if (this.engineStartingFromKeyboard && this.EngineForce == 0.0F) {
               this.engineStartingFromKeyboard = false;
            }
         }

         if (this.vehicleObject.engineState != BaseVehicle.engineStateTypes.Running) {
            this.acceleratorOn = false;
            if (!GameServer.bServer && this.vehicleObject.jniSpeed > 5.0F && this.vehicleObject.getScript().getWheelCount() > 0) {
               Bullet.controlVehicle(this.vehicleObject.VehicleID, 0.0F, this.BrakingForce, this.VehicleSteering);
            } else {
               this.park();
            }
         }

      }
   }

   public void updateTrailer() {
      BaseVehicle var1 = this.vehicleObject.getVehicleTowedBy();
      if (var1 != null) {
         if (GameServer.bServer) {
            if (var1.getDriver() == null && this.vehicleObject.getDriver() != null) {
               this.vehicleObject.addPointConstraint((IsoPlayer)null, var1, this.vehicleObject.getTowAttachmentSelf(), var1.getTowAttachmentSelf());
            }

         } else {
            this.speed = this.vehicleObject.getCurrentSpeedKmHour();
            this.isGas = false;
            this.isGasR = false;
            this.isBreak = false;
            this.wasGas = false;
            this.wasGasR = false;
            this.wasBreaking = false;
            this.vehicleObject.throttle = 0.0F;
            if (var1.getDriver() == null && this.vehicleObject.getDriver() != null && !GameClient.bClient) {
               this.vehicleObject.addPointConstraint((IsoPlayer)null, var1, this.vehicleObject.getTowAttachmentSelf(), var1.getTowAttachmentSelf());
            } else {
               this.checkShouldBeActive();
               this.EngineForce = 0.0F;
               this.BrakingForce = 0.0F;
               this.VehicleSteering = 0.0F;
               if (!this.vehicleObject.getScriptName().contains("Trailer")) {
                  this.BrakingForce = 10.0F;
               }

               Bullet.controlVehicle(this.vehicleObject.VehicleID, this.EngineForce, this.BrakingForce, this.VehicleSteering);
            }
         }
      }
   }

   private void updateRegulator() {
      if (this.regulatorTimer > 0.0F) {
         this.regulatorTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
      }

      if (this.clientControls.shift) {
         if (this.clientControls.forward && this.regulatorTimer <= 0.0F) {
            if (this.vehicleObject.getRegulatorSpeed() < this.vehicleObject.getMaxSpeed() + 20.0F && (!this.vehicleObject.isRegulator() && this.vehicleObject.getRegulatorSpeed() == 0.0F || this.vehicleObject.isRegulator())) {
               if (this.vehicleObject.getRegulatorSpeed() == 0.0F && this.vehicleObject.getCurrentSpeedForRegulator() != this.vehicleObject.getRegulatorSpeed()) {
                  this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getCurrentSpeedForRegulator());
               } else {
                  this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getRegulatorSpeed() + 5.0F);
               }
            }

            this.vehicleObject.setRegulator(true);
            this.regulatorTimer = 20.0F;
         } else if (this.clientControls.backward && this.regulatorTimer <= 0.0F) {
            this.regulatorTimer = 20.0F;
            if (this.vehicleObject.getRegulatorSpeed() >= 5.0F && (!this.vehicleObject.isRegulator() && this.vehicleObject.getRegulatorSpeed() == 0.0F || this.vehicleObject.isRegulator())) {
               this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getRegulatorSpeed() - 5.0F);
            }

            this.vehicleObject.setRegulator(true);
            if (this.vehicleObject.getRegulatorSpeed() <= 0.0F) {
               this.vehicleObject.setRegulatorSpeed(0.0F);
               this.vehicleObject.setRegulator(false);
            }
         }
      } else if (this.isGasR || this.isBreak) {
         this.vehicleObject.setRegulator(false);
      }

   }

   public void control_NoControl() {
      float var1 = GameTime.getInstance().getMultiplier() / 0.8F;
      if (!this.vehicleObject.isEngineRunning()) {
         if (this.vehicleObject.engineSpeed > 0.0) {
            this.vehicleObject.engineSpeed = Math.max(this.vehicleObject.engineSpeed - (double)(50.0F * var1), 0.0);
         }
      } else {
         BaseVehicle var10000;
         if (this.vehicleObject.engineSpeed > (double)this.vehicleObject.getScript().getEngineIdleSpeed()) {
            if (!this.vehicleObject.isRegulator()) {
               var10000 = this.vehicleObject;
               var10000.engineSpeed -= (double)(20.0F * var1);
            }
         } else {
            var10000 = this.vehicleObject;
            var10000.engineSpeed += (double)(20.0F * var1);
         }
      }

      if (!this.vehicleObject.isRegulator()) {
         this.vehicleObject.transmissionNumber = TransmissionNumber.N;
      }

      this.EngineForce = 0.0F;
      if (this.vehicleObject.engineSpeed > 1000.0) {
         this.BrakingForce = 15.0F;
      } else {
         this.BrakingForce = 10.0F;
      }

   }

   private void control_Braking() {
      float var1 = GameTime.getInstance().getMultiplier() / 0.8F;
      BaseVehicle var10000;
      if (this.vehicleObject.engineSpeed > (double)this.vehicleObject.getScript().getEngineIdleSpeed()) {
         var10000 = this.vehicleObject;
         var10000.engineSpeed -= (double)((float)Rand.Next(10, 30) * var1);
      } else {
         var10000 = this.vehicleObject;
         var10000.engineSpeed += (double)((float)Rand.Next(20) * var1);
      }

      this.vehicleObject.transmissionNumber = TransmissionNumber.N;
      this.EngineForce = 0.0F;
      this.BrakingForce = this.vehicleObject.getBrakingForce();
      if (this.clientControls.brake) {
         this.BrakingForce *= 13.0F;
      }

   }

   private void control_Forward(float var1) {
      float var2 = GameTime.getInstance().getMultiplier() / 0.8F;
      IsoGameCharacter var3 = this.vehicleObject.getDriver();
      boolean var4 = var3 != null && var3.Traits.SpeedDemon.isSet();
      boolean var5 = var3 != null && var3.Traits.SundayDriver.isSet();
      int var6 = this.vehicleObject.getScript().gearRatioCount;
      float var7 = 0.0F;
      boolean var8;
      if (this.vehicleObject.transmissionNumber == TransmissionNumber.N) {
         this.vehicleObject.transmissionNumber = TransmissionNumber.Speed1;
         var8 = false;

         while(true) {
            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1) {
               var7 = 3000.0F * var1 / 30.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed2) {
               var7 = 3000.0F * var1 / 40.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed3) {
               var7 = 3000.0F * var1 / 60.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed4) {
               var7 = 3000.0F * var1 / 85.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed5) {
               var7 = 3000.0F * var1 / 105.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed6) {
               var7 = 3000.0F * var1 / 130.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed7) {
               var7 = 3000.0F * var1 / 160.0F;
            }

            if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed8) {
               var7 = 3000.0F * var1 / 200.0F;
            }

            if (var7 > 3000.0F) {
               this.vehicleObject.changeTransmission(this.vehicleObject.transmissionNumber.getNext(var6));
               var8 = true;
            }

            if (!var8 || this.vehicleObject.transmissionNumber.getIndex() >= var6) {
               break;
            }

            var8 = false;
         }
      }

      if (this.vehicleObject.engineSpeed > 3000.0 && this.vehicleObject.transmissionChangeTime.Check()) {
         this.vehicleObject.changeTransmission(this.vehicleObject.transmissionNumber.getNext(var6));
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1) {
         var7 = 3000.0F * var1 / 30.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed2) {
         var7 = 3000.0F * var1 / 40.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed3) {
         var7 = 3000.0F * var1 / 60.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed4) {
         var7 = 3000.0F * var1 / 85.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed5) {
         var7 = 3000.0F * var1 / 105.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed6) {
         var7 = 3000.0F * var1 / 130.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed7) {
         var7 = 3000.0F * var1 / 160.0F;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed8) {
         var7 = 3000.0F * var1 / 200.0F;
      }

      BaseVehicle var10000 = this.vehicleObject;
      var10000.engineSpeed -= Math.min(0.5 * (this.vehicleObject.engineSpeed - (double)var7), 100.0) * (double)var2;
      if (var4) {
         if (var1 < 50.0F) {
            var10000 = this.vehicleObject;
            var10000.engineSpeed -= Math.min(0.06 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - var1)) * (double)var2;
         }
      } else if (var1 < 30.0F) {
         var10000 = this.vehicleObject;
         var10000.engineSpeed -= Math.min(0.02 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - var1)) * (double)var2;
      }

      this.EngineForce = (float)((double)this.vehicleObject.getEnginePower() * (0.5 + this.vehicleObject.engineSpeed / 24000.0));
      this.EngineForce -= this.EngineForce * (var1 / 200.0F);
      var8 = false;
      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1 && this.vehicleObject.getVehicleTowedBy() != null) {
         if (this.vehicleObject.getVehicleTowedBy().getScript().getPassengerCount() == 0 && this.vehicleObject.getVehicleTowedBy().getScript().getMass() > 200.0F) {
            var8 = true;
         }

         if (var1 < (float)(var8 ? 20 : 5)) {
            this.EngineForce *= Math.min(1.2F, this.vehicleObject.getVehicleTowedBy().getMass() / 500.0F);
            if (var8) {
               this.EngineForce *= 4.0F;
            }
         }
      }

      if (this.vehicleObject.engineSpeed > 6000.0) {
         this.EngineForce = (float)((double)this.EngineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
      }

      if (var5) {
         this.EngineForce *= 0.75F;
         if (var1 > this.vehicleObject.getMaxSpeed() * 0.6F) {
            this.EngineForce *= (this.vehicleObject.getMaxSpeed() * 0.75F + 20.0F - var1) / 20.0F;
         }
      }

      if (var4) {
         if (var1 > this.vehicleObject.getMaxSpeed() * 1.15F) {
            this.EngineForce *= (this.vehicleObject.getMaxSpeed() * 1.15F + 20.0F - var1) / 20.0F;
         }
      } else if (var1 > this.vehicleObject.getMaxSpeed()) {
         this.EngineForce *= (this.vehicleObject.getMaxSpeed() + 20.0F - var1) / 20.0F;
      }

      this.BrakingForce = 0.0F;
      if (this.clientControls.wasUsingParkingBrakes) {
         this.clientControls.wasUsingParkingBrakes = false;
         this.EngineForce *= 8.0F;
      }

      if (GameClient.bClient && (double)this.vehicleObject.jniSpeed >= ServerOptions.instance.SpeedLimit.getValue()) {
         this.EngineForce = 0.0F;
      }

   }

   private void control_ForwardNew(float var1) {
      float var2 = GameTime.getInstance().getMultiplier() / 0.8F;
      IsoGameCharacter var3 = this.vehicleObject.getDriver();
      boolean var4 = var3 != null && var3.Traits.SpeedDemon.isSet();
      boolean var5 = var3 != null && var3.Traits.SundayDriver.isSet();
      int var6 = this.vehicleObject.getScript().gearRatioCount;
      float var7 = 0.0F;
      EngineRPMData[] var8 = this.vehicleObject.getVehicleEngineRPM().m_rpmData;
      float var9 = this.vehicleObject.getMaxSpeed() / (float)var6;
      float var10 = PZMath.clamp(var1, 0.0F, this.vehicleObject.getMaxSpeed());
      int var11 = (int)PZMath.floor(var10 / var9) + 1;
      var11 = PZMath.min(var11, var6);
      var7 = var8[var11 - 1].gearChange;
      TransmissionNumber var12 = TransmissionNumber.Speed1;
      switch (var11) {
         case 1:
            var12 = TransmissionNumber.Speed1;
            break;
         case 2:
            var12 = TransmissionNumber.Speed2;
            break;
         case 3:
            var12 = TransmissionNumber.Speed3;
            break;
         case 4:
            var12 = TransmissionNumber.Speed4;
            break;
         case 5:
            var12 = TransmissionNumber.Speed5;
            break;
         case 6:
            var12 = TransmissionNumber.Speed6;
            break;
         case 7:
            var12 = TransmissionNumber.Speed7;
            break;
         case 8:
            var12 = TransmissionNumber.Speed8;
      }

      if (this.vehicleObject.transmissionNumber == TransmissionNumber.N) {
         this.vehicleObject.transmissionNumber = var12;
      } else if (this.vehicleObject.transmissionNumber.getIndex() - 1 >= 0 && this.vehicleObject.transmissionNumber.getIndex() < var12.getIndex() && this.vehicleObject.getEngineSpeed() >= (double)var8[this.vehicleObject.transmissionNumber.getIndex() - 1].gearChange && var1 >= var9 * (float)this.vehicleObject.transmissionNumber.getIndex()) {
         this.vehicleObject.transmissionNumber = var12;
         this.vehicleObject.engineSpeed = (double)var8[this.vehicleObject.transmissionNumber.getIndex() - 1].afterGearChange;
      }

      if (this.vehicleObject.transmissionNumber.getIndex() < var6 && this.vehicleObject.transmissionNumber.getIndex() - 1 >= 0) {
         this.vehicleObject.engineSpeed = Math.min(this.vehicleObject.engineSpeed, (double)(var8[this.vehicleObject.transmissionNumber.getIndex() - 1].gearChange + 100.0F));
      }

      float var13;
      BaseVehicle var10000;
      if (this.vehicleObject.engineSpeed > (double)var7) {
         var10000 = this.vehicleObject;
         var10000.engineSpeed -= Math.min(0.5 * (this.vehicleObject.engineSpeed - (double)var7), 10.0) * (double)var2;
      } else {
         float var15;
         switch (this.vehicleObject.transmissionNumber) {
            case Speed1:
               var15 = 10.0F;
               break;
            case Speed2:
               var15 = 8.0F;
               break;
            case Speed3:
               var15 = 7.0F;
               break;
            case Speed4:
               var15 = 6.0F;
               break;
            case Speed5:
               var15 = 5.0F;
               break;
            default:
               var15 = 4.0F;
         }

         var13 = var15;
         var10000 = this.vehicleObject;
         var10000.engineSpeed += (double)(var13 * var2);
      }

      var13 = (float)this.vehicleObject.getEnginePower();
      var13 = this.vehicleObject.getScript().getEngineForce();
      float var10001;
      switch (this.vehicleObject.transmissionNumber) {
         case Speed1:
            var10001 = 1.5F;
            break;
         default:
            var10001 = 1.0F;
      }

      var13 *= var10001;
      this.EngineForce = (float)((double)var13 * (0.30000001192092896 + this.vehicleObject.engineSpeed / 30000.0));
      this.EngineForce -= this.EngineForce * (var1 / 200.0F);
      boolean var14 = false;
      if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1 && this.vehicleObject.getVehicleTowedBy() != null) {
         if (this.vehicleObject.getVehicleTowedBy().getScript().getPassengerCount() == 0 && this.vehicleObject.getVehicleTowedBy().getScript().getMass() > 200.0F) {
            var14 = true;
         }

         if (var1 < (float)(var14 ? 20 : 5)) {
            this.EngineForce *= Math.min(1.2F, this.vehicleObject.getVehicleTowedBy().getMass() / 500.0F);
            if (var14) {
               this.EngineForce *= 4.0F;
            }
         }
      }

      if (this.vehicleObject.engineSpeed > 6000.0) {
         this.EngineForce = (float)((double)this.EngineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
      }

      if (var5) {
         this.EngineForce *= 0.75F;
         if (var1 > this.vehicleObject.getMaxSpeed() * 0.6F) {
            this.EngineForce *= (this.vehicleObject.getMaxSpeed() * 0.75F + 20.0F - var1) / 20.0F;
         }
      }

      if (var4) {
         if (var1 > this.vehicleObject.getMaxSpeed() * 1.15F) {
            this.EngineForce *= (this.vehicleObject.getMaxSpeed() * 1.15F + 20.0F - var1) / 20.0F;
         }
      } else if (var1 > this.vehicleObject.getMaxSpeed()) {
         this.EngineForce *= (this.vehicleObject.getMaxSpeed() + 20.0F - var1) / 20.0F;
      }

      this.BrakingForce = 0.0F;
      if (this.clientControls.wasUsingParkingBrakes) {
         this.clientControls.wasUsingParkingBrakes = false;
         this.EngineForce *= 8.0F;
      }

      if (GameClient.bClient && (double)this.vehicleObject.jniSpeed >= ServerOptions.instance.SpeedLimit.getValue()) {
         this.EngineForce = 0.0F;
      }

   }

   private void control_Reverse(float var1) {
      float var2 = GameTime.getInstance().getMultiplier() / 0.8F;
      var1 *= 1.5F;
      IsoGameCharacter var3 = this.vehicleObject.getDriver();
      boolean var4 = var3 != null && var3.Traits.SpeedDemon.isSet();
      boolean var5 = var3 != null && var3.Traits.SundayDriver.isSet();
      this.vehicleObject.transmissionNumber = TransmissionNumber.R;
      float var6 = 1000.0F * var1 / 30.0F;
      BaseVehicle var10000 = this.vehicleObject;
      var10000.engineSpeed -= Math.min(0.5 * (this.vehicleObject.engineSpeed - (double)var6), 100.0) * (double)var2;
      if (var4) {
         var10000 = this.vehicleObject;
         var10000.engineSpeed -= Math.min(0.06 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - var1)) * (double)var2;
      } else {
         var10000 = this.vehicleObject;
         var10000.engineSpeed -= Math.min(0.02 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - var1)) * (double)var2;
      }

      this.EngineForce = (float)((double)(-1.0F * (float)this.vehicleObject.getEnginePower()) * (0.75 + this.vehicleObject.engineSpeed / 24000.0));
      if (this.vehicleObject.engineSpeed > 6000.0) {
         this.EngineForce = (float)((double)this.EngineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
      }

      if (var5) {
         this.EngineForce *= 0.7F;
         if (var1 < -5.0F) {
            this.EngineForce *= (15.0F + var1) / 10.0F;
         }
      }

      if (var1 < -1.0F * this.vehicleObject.getScript().maxSpeedReverse) {
         this.EngineForce = 0.0F;
      }

      this.BrakingForce = 0.0F;
   }

   private void updateRammingSound(float var1) {
      if (this.vehicleObject.isEngineRunning() && (var1 < 1.0F && this.EngineForce > this.vehicleObject.getScript().getEngineIdleSpeed() * 2.0F || var1 > -0.5F && this.EngineForce < this.vehicleObject.getScript().getEngineIdleSpeed() * -2.0F)) {
         if (this.vehicleObject.ramSound == 0L) {
            this.vehicleObject.ramSound = this.vehicleObject.playSoundImpl("VehicleSkid", (IsoObject)null);
            this.vehicleObject.ramSoundTime = System.currentTimeMillis() + 1000L + (long)Rand.Next(2000);
         }

         if (this.vehicleObject.ramSound != 0L && this.vehicleObject.ramSoundTime < System.currentTimeMillis()) {
            this.vehicleObject.stopSound(this.vehicleObject.ramSound);
            this.vehicleObject.ramSound = 0L;
         }
      } else if (this.vehicleObject.ramSound != 0L) {
         this.vehicleObject.stopSound(this.vehicleObject.ramSound);
         this.vehicleObject.ramSound = 0L;
      }

   }

   private void updateBackSignal() {
      if (this.isGasR && this.vehicleObject.isEngineRunning() && this.vehicleObject.hasBackSignal() && !this.vehicleObject.isBackSignalEmitting()) {
         if (GameClient.bClient) {
            GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "onBackSignal", "state", "start");
         } else {
            this.vehicleObject.onBackMoveSignalStart();
         }
      }

      if (!this.isGasR && this.vehicleObject.isBackSignalEmitting()) {
         if (GameClient.bClient) {
            GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "onBackSignal", "state", "stop");
         } else {
            this.vehicleObject.onBackMoveSignalStop();
         }
      }

   }

   private void updateBrakeLights() {
      if (this.isBreak) {
         if (this.vehicleObject.getStoplightsOn()) {
            return;
         }

         if (GameClient.bClient) {
            GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "setStoplightsOn", "on", Boolean.TRUE);
         }

         if (!GameServer.bServer) {
            this.vehicleObject.setStoplightsOn(true);
         }
      } else {
         if (!this.vehicleObject.getStoplightsOn()) {
            return;
         }

         if (GameClient.bClient) {
            GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "setStoplightsOn", "on", Boolean.FALSE);
         }

         if (!GameServer.bServer) {
            this.vehicleObject.setStoplightsOn(false);
         }
      }

   }

   private boolean delayCommandWhileDrunk(boolean var1) {
      this.drunkDelayCommandTimer += GameTime.getInstance().getMultiplier();
      if ((float)Rand.AdjustForFramerate(4 * this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.Drunk)) < this.drunkDelayCommandTimer) {
         this.drunkDelayCommandTimer = 0.0F;
         return true;
      } else {
         return false;
      }
   }

   private float delayCommandWhileDrunk(float var1) {
      this.drunkDelayCommandTimer += GameTime.getInstance().getMultiplier();
      if ((float)Rand.AdjustForFramerate(4 * this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.Drunk)) < this.drunkDelayCommandTimer) {
         this.drunkDelayCommandTimer = 0.0F;
         return var1;
      } else {
         return 0.0F;
      }
   }

   private void checkTire(BulletVariables var1) {
      if (this.vehicleObject.getPartById("TireFrontLeft") == null || this.vehicleObject.getPartById("TireFrontLeft").getInventoryItem() == null) {
         var1.brakingForce = (float)((double)var1.brakingForce / 1.2);
         var1.engineForce = (float)((double)var1.engineForce / 1.2);
      }

      if (this.vehicleObject.getPartById("TireFrontRight") == null || this.vehicleObject.getPartById("TireFrontRight").getInventoryItem() == null) {
         var1.brakingForce = (float)((double)var1.brakingForce / 1.2);
         var1.engineForce = (float)((double)var1.engineForce / 1.2);
      }

      if (this.vehicleObject.getPartById("TireRearLeft") == null || this.vehicleObject.getPartById("TireRearLeft").getInventoryItem() == null) {
         var1.brakingForce = (float)((double)var1.brakingForce / 1.3);
         var1.engineForce = (float)((double)var1.engineForce / 1.3);
      }

      if (this.vehicleObject.getPartById("TireRearRight") == null || this.vehicleObject.getPartById("TireRearRight").getInventoryItem() == null) {
         var1.brakingForce = (float)((double)var1.brakingForce / 1.3);
         var1.engineForce = (float)((double)var1.engineForce / 1.3);
      }

   }

   public void updateControls() {
      if (!GameServer.bServer) {
         boolean var2;
         boolean var3;
         boolean var4;
         boolean var5;
         boolean var6;
         if (this.vehicleObject.isKeyboardControlled()) {
            boolean var1 = GameKeyboard.isKeyDown("Left");
            var2 = GameKeyboard.isKeyDown("Right");
            var3 = GameKeyboard.isKeyDown("Forward");
            var4 = GameKeyboard.isKeyDown("Backward");
            var5 = GameKeyboard.isKeyDown("Brake");
            var6 = GameKeyboard.isKeyDown("CruiseControl");
            this.clientControls.steering = 0.0F;
            if (var1) {
               --this.clientControls.steering;
            }

            if (var2) {
               ++this.clientControls.steering;
            }

            this.clientControls.forward = var3;
            this.clientControls.backward = var4;
            this.clientControls.brake = var5;
            this.clientControls.shift = var6;
            if (this.clientControls.brake) {
               this.clientControls.wasUsingParkingBrakes = true;
            }
         }

         int var8 = this.vehicleObject.getJoypad();
         if (var8 != -1) {
            var2 = JoypadManager.instance.isLeftPressed(var8);
            var3 = JoypadManager.instance.isRightPressed(var8);
            var4 = JoypadManager.instance.isRTPressed(var8);
            var5 = JoypadManager.instance.isLTPressed(var8);
            var6 = JoypadManager.instance.isBPressed(var8);
            float var7 = JoypadManager.instance.getMovementAxisX(var8);
            this.clientControls.steering = var7;
            this.clientControls.forward = var4;
            this.clientControls.backward = var5;
            this.clientControls.brake = var6;
         }

         if (this.clientControls.forceBrake != 0L) {
            long var9 = System.currentTimeMillis() - this.clientControls.forceBrake;
            if (var9 > 0L && var9 < 1000L) {
               this.clientControls.brake = true;
               this.clientControls.shift = false;
            }
         }
      }

   }

   public void park() {
      if (!GameServer.bServer && this.vehicleObject.getScript().getWheelCount() > 0) {
         Bullet.controlVehicle(this.vehicleObject.VehicleID, 0.0F, this.vehicleObject.getBrakingForce(), 0.0F);
      }

      this.isGas = this.wasGas = false;
      this.isGasR = this.wasGasR = false;
      this.clientControls.reset();
      this.vehicleObject.transmissionNumber = TransmissionNumber.N;
      if (this.vehicleObject.getVehicleTowing() != null) {
         this.vehicleObject.getVehicleTowing().getController().park();
      }

   }

   protected boolean shouldBeActive() {
      if (this.vehicleObject.physicActiveCheck != -1L) {
         return true;
      } else {
         BaseVehicle var1 = this.vehicleObject.getVehicleTowedBy();
         if (var1 == null) {
            float var2 = this.vehicleObject.isEngineRunning() ? this.EngineForce : 0.0F;
            return Math.abs(var2) > 0.01F;
         } else {
            return var1.getController() == null ? false : var1.getController().shouldBeActive();
         }
      }
   }

   public void checkShouldBeActive() {
      if (this.shouldBeActive()) {
         if (!this.isEnable) {
            Bullet.setVehicleActive(this.vehicleObject.VehicleID, true);
            this.isEnable = true;
         }

         this.atRestTimer = 1.0F;
      } else if (this.isEnable && this.vehicleObject.isAtRest()) {
         if (this.atRestTimer > 0.0F) {
            this.atRestTimer -= GameTime.getInstance().getTimeDelta();
         }

         if (this.atRestTimer <= 0.0F) {
            Bullet.setVehicleActive(this.vehicleObject.VehicleID, false);
            this.isEnable = false;
         }
      }

   }

   public boolean isGasPedalPressed() {
      return this.isGas || this.isGasR;
   }

   public boolean isBrakePedalPressed() {
      return this.isBreak;
   }

   public void debug() {
      if (Core.bDebug && DebugOptions.instance.VehicleRenderOutline.getValue()) {
         VehicleScript var1 = this.vehicleObject.getScript();
         int var2 = PZMath.fastfloor(this.vehicleObject.getZ());
         Vector3f var3 = this.tempVec3f;
         this.vehicleObject.getForwardVector(var3);
         Transform var4 = this.tempXfrm;
         this.vehicleObject.getWorldTransform(var4);
         VehiclePoly var5 = this.vehicleObject.getPoly();
         LineDrawer.addLine(var5.x1, var5.y1, (float)var2, var5.x2, var5.y2, (float)var2, 1.0F, 1.0F, 1.0F, (String)null, true);
         LineDrawer.addLine(var5.x2, var5.y2, (float)var2, var5.x3, var5.y3, (float)var2, 1.0F, 1.0F, 1.0F, (String)null, true);
         LineDrawer.addLine(var5.x3, var5.y3, (float)var2, var5.x4, var5.y4, (float)var2, 1.0F, 1.0F, 1.0F, (String)null, true);
         LineDrawer.addLine(var5.x4, var5.y4, (float)var2, var5.x1, var5.y1, (float)var2, 1.0F, 1.0F, 1.0F, (String)null, true);
         _UNIT_Y.set(0.0F, 1.0F, 0.0F);

         float var7;
         float var8;
         int var10;
         for(var10 = 0; var10 < this.vehicleObject.getScript().getWheelCount(); ++var10) {
            VehicleScript.Wheel var6 = var1.getWheel(var10);
            this.tempVec3f.set(var6.getOffset());
            if (var1.getModel() != null) {
               this.tempVec3f.add(var1.getModelOffset());
            }

            this.vehicleObject.getWorldPos(this.tempVec3f, this.tempVec3f);
            var7 = this.tempVec3f.x;
            var8 = this.tempVec3f.y;
            this.vehicleObject.getWheelForwardVector(var10, this.tempVec3f);
            LineDrawer.addLine(var7, var8, (float)var2, var7 + this.tempVec3f.x, var8 + this.tempVec3f.z, (float)var2, 1.0F, 1.0F, 1.0F, (String)null, true);
            this.drawRect(this.tempVec3f, var7 - WorldSimulation.instance.offsetX, var8 - WorldSimulation.instance.offsetY, var6.width, var6.radius);
         }

         if (this.vehicleObject.collideX != -1.0F) {
            this.vehicleObject.getForwardVector(var3);
            this.drawCircle(this.vehicleObject.collideX, this.vehicleObject.collideY, 0.3F);
            this.vehicleObject.collideX = -1.0F;
            this.vehicleObject.collideY = -1.0F;
         }

         var10 = this.vehicleObject.getJoypad();
         float var11;
         if (var10 != -1) {
            var11 = JoypadManager.instance.getMovementAxisX(var10);
            var7 = JoypadManager.instance.getMovementAxisY(var10);
            var8 = JoypadManager.instance.getDeadZone(var10, 0);
            if (Math.abs(var7) > var8 || Math.abs(var11) > var8) {
               Vector2 var9 = this.tempVec2.set(var11, var7);
               var9.setLength(4.0F);
               var9.rotate(-0.7853982F);
               LineDrawer.addLine(this.vehicleObject.getX(), this.vehicleObject.getY(), this.vehicleObject.getZ(), this.vehicleObject.getX() + var9.x, this.vehicleObject.getY() + var9.y, this.vehicleObject.getZ(), 1.0F, 1.0F, 1.0F, (String)null, true);
            }
         }

         var11 = this.vehicleObject.getX();
         var7 = this.vehicleObject.getY();
         var8 = this.vehicleObject.getZ();
         LineDrawer.addLine(var11 - 0.5F, var7, var8, var11 + 0.5F, var7, var8, 1.0F, 1.0F, 1.0F, (String)null, true);
         LineDrawer.addLine(var11, var7 - 0.5F, var8, var11, var7 + 0.5F, var8, 1.0F, 1.0F, 1.0F, (String)null, true);
      }
   }

   public void drawRect(Vector3f var1, float var2, float var3, float var4, float var5) {
      this.drawRect(var1, var2, var3, var4, var5, 1.0F, 1.0F, 1.0F);
   }

   public void drawRect(Vector3f var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = var1.x;
      float var10 = var1.y;
      float var11 = var1.z;
      Vector3f var12 = this.tempVec3f_3;
      var1.cross(_UNIT_Y, var12);
      float var13 = 1.0F;
      var1.x *= var13 * var5;
      var1.z *= var13 * var5;
      var12.x *= var13 * var4;
      var12.z *= var13 * var4;
      float var14 = var2 + var1.x;
      float var15 = var3 + var1.z;
      float var16 = var2 - var1.x;
      float var17 = var3 - var1.z;
      float var18 = var14 - var12.x / 2.0F;
      float var19 = var14 + var12.x / 2.0F;
      float var20 = var16 - var12.x / 2.0F;
      float var21 = var16 + var12.x / 2.0F;
      float var22 = var17 - var12.z / 2.0F;
      float var23 = var17 + var12.z / 2.0F;
      float var24 = var15 - var12.z / 2.0F;
      float var25 = var15 + var12.z / 2.0F;
      var18 += WorldSimulation.instance.offsetX;
      var24 += WorldSimulation.instance.offsetY;
      var19 += WorldSimulation.instance.offsetX;
      var25 += WorldSimulation.instance.offsetY;
      var20 += WorldSimulation.instance.offsetX;
      var22 += WorldSimulation.instance.offsetY;
      var21 += WorldSimulation.instance.offsetX;
      var23 += WorldSimulation.instance.offsetY;
      int var26 = PZMath.fastfloor(this.vehicleObject.getZ());
      LineDrawer.addLine(var18, var24, (float)var26, var19, var25, (float)var26, var6, var7, var8, (String)null, true);
      LineDrawer.addLine(var18, var24, (float)var26, var20, var22, (float)var26, var6, var7, var8, (String)null, true);
      LineDrawer.addLine(var19, var25, (float)var26, var21, var23, (float)var26, var6, var7, var8, (String)null, true);
      LineDrawer.addLine(var20, var22, (float)var26, var21, var23, (float)var26, var6, var7, var8, (String)null, true);
      var1.set(var9, var10, var11);
   }

   public void drawCircle(float var1, float var2, float var3) {
      this.drawCircle(var1, var2, var3, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void drawCircle(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      LineDrawer.DrawIsoCircle(var1, var2, this.vehicleObject.getZ(), var3, 16, var4, var5, var6, var7);
   }

   static {
      gears[0] = new GearInfo(0, 25, 0.0F);
      gears[1] = new GearInfo(25, 50, 0.5F);
      gears[2] = new GearInfo(50, 1000, 0.5F);
      bulletVariables = new BulletVariables();
   }

   public static final class ClientControls {
      public float steering;
      public boolean forward;
      public boolean backward;
      public boolean brake;
      public boolean shift;
      public boolean wasUsingParkingBrakes;
      public long forceBrake = 0L;

      public ClientControls() {
      }

      public void reset() {
         this.steering = 0.0F;
         this.forward = false;
         this.backward = false;
         this.brake = false;
         this.shift = false;
         this.wasUsingParkingBrakes = false;
         this.forceBrake = 0L;
      }
   }

   public static final class GearInfo {
      int minSpeed;
      int maxSpeed;
      float minRPM;

      GearInfo(int var1, int var2, float var3) {
         this.minSpeed = var1;
         this.maxSpeed = var2;
         this.minRPM = var3;
      }
   }

   static enum ControlState {
      NoControl,
      Braking,
      Forward,
      Reverse;

      private ControlState() {
      }
   }

   public static final class BulletVariables {
      float engineForce;
      float brakingForce;
      float vehicleSteering;
      BaseVehicle vehicle;

      public BulletVariables() {
      }

      BulletVariables set(BaseVehicle var1, float var2, float var3, float var4) {
         this.vehicle = var1;
         this.engineForce = var2;
         this.brakingForce = var3;
         this.vehicleSteering = var4;
         return this;
      }
   }
}
