package zombie.entity.components.crafting;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.OutputScript;

public class MashingLogicSystem extends EngineSystem {
   private static final float fermentMinTemp = 12.0F;
   private static final float fermentOptimalTemp = 21.0F;
   private static final float fermentMaxTemp = 30.0F;
   private static double currentWorldAge = 0.0;
   private EntityBucket mashingEntities;

   public MashingLogicSystem(int var1) {
      super(true, false, var1);
   }

   public void addedToEngine(Engine var1) {
      this.mashingEntities = var1.getBucket(Family.all(ComponentType.MashingLogic, ComponentType.Resources).get());
   }

   public void removedFromEngine(Engine var1) {
   }

   private boolean isValidEntity(GameEntity var1) {
      return var1.isEntityValid() && var1.isValidEngineEntity();
   }

   public void update() {
      if (!GameClient.bClient) {
         ImmutableArray var1 = this.mashingEntities.getEntities();
         if (var1.size() != 0) {
            currentWorldAge = GameTime.instance.getWorldAgeHours();

            for(int var7 = 0; var7 < var1.size(); ++var7) {
               GameEntity var2 = (GameEntity)var1.get(var7);
               if (this.isValidEntity(var2)) {
                  MashingLogic var3 = (MashingLogic)var2.getComponent(ComponentType.MashingLogic);
                  Resources var4 = (Resources)var2.getComponent(ComponentType.Resources);
                  if (var3.isValid() && var4.isValid()) {
                     ResourceGroup var5 = var4.getResourceGroup(var3.getInputsGroupName());
                     if (var5 != null && !var5.getResources().isEmpty()) {
                        ResourceFluid var6 = (ResourceFluid)var5.get(var3.getResourceFluidID());
                        if (var6 != null) {
                           this.updateMashingLogic(var3, var5, var6);
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private void updateMashingLogic(MashingLogic var1, ResourceGroup var2, ResourceFluid var3) {
      if (var1.isRunning()) {
         if (var1.isFinished()) {
            this.finish(var1, var3);
            return;
         }

         float var4 = CraftUtil.getEntityTemperature(var1.getGameEntity());
         if (var4 < 12.0F) {
            var1.setLastWorldAge(currentWorldAge);
         } else {
            if (var4 > 30.0F) {
               this.cancel(var1, var3);
               return;
            }

            double var5 = 0.0;
            if (var1.getLastWorldAge() >= 0.0) {
               var5 = currentWorldAge - var1.getLastWorldAge();
            }

            var1.setElapsedTime(var1.getElapsedTime() + var5);
            if (var1.getElapsedTime() > (double)var1.getCurrentRecipe().getTime()) {
               var1.setElapsedTime((double)var1.getCurrentRecipe().getTime());
            }

            var1.setLastWorldAge(currentWorldAge);
         }

         if (var1.isStopRequested()) {
            this.cancel(var1, var3);
            var1.setStopRequested(false);
            var1.setRequestingPlayer((IsoPlayer)null);
         }
      } else if (var1.isStartRequested()) {
         DebugLog.General.debugln("Requesting start...");
         this.start(var1, StartMode.Manual, var1.getRequestingPlayer(), var2, var3);
         var1.setStartRequested(false);
         var1.setRequestingPlayer((IsoPlayer)null);
      }

   }

   private void start(MashingLogic var1, StartMode var2, IsoPlayer var3, ResourceGroup var4, ResourceFluid var5) {
      if (!GameClient.bClient) {
         if (var1.canStart(var2, var3)) {
            DebugLog.General.debugln("Start...");
            CraftRecipe var6 = var1.getPossibleRecipe();
            if (var6 == null) {
               return;
            }

            var1.setRecipe(var6);
            var1.setElapsedTime(0.0);
            var1.setLastWorldAge(currentWorldAge);
            FluidContainer var7 = var5.getFluidContainer();
            var1.setBarrelConsumedAmount(var7.getAmount());
            if (!var1.getCraftData().consumeInputs(var4.getResources())) {
               var1.setRecipe((CraftRecipe)null);
               return;
            }

            var1.getCraftData().luaCallOnStart();
            DebugLog.General.debugln("START_PASSED");
            if (GameServer.bServer) {
            }
         }

      }
   }

   private void cancel(MashingLogic var1, ResourceFluid var2) {
      this.stop(var1, true, var2);
   }

   private void finish(MashingLogic var1, ResourceFluid var2) {
      this.stop(var1, false, var2);
   }

   private void stop(MashingLogic var1, boolean var2, ResourceFluid var3) {
      if (!GameClient.bClient) {
         if (var1.isValid()) {
            if (var1.isRunning()) {
               DebugLog.General.debugln("Stop, cancelled = " + var2);
               if (var2) {
                  var1.getCraftData().luaCallOnFailed();
               } else {
                  this.createResultFluid(var1, var2, var3);
               }

               var1.setRecipe((CraftRecipe)null);
               if (GameServer.bServer) {
               }

            }
         }
      }
   }

   private void createResultFluid(MashingLogic var1, boolean var2, ResourceFluid var3) {
      FluidContainer var4 = var3.getFluidContainer();
      var3.clear();
      float var5 = var1.getBarrelConsumedAmount();
      if (var2) {
         var4.addFluid(Fluid.TaintedWater, var5);
      } else {
         CraftRecipe var6 = var1.getCurrentRecipe();
         if (var6 == null) {
            return;
         }

         ArrayList var7 = var6.getOutputs();

         for(int var8 = 0; var8 < var7.size(); ++var8) {
            OutputScript var9 = (OutputScript)var7.get(var8);
            if (var9.getResourceType() == ResourceType.Fluid) {
               Fluid var10 = var9.getFluid();
               var4.addFluid(var10, var5);
               return;
            }
         }
      }

   }
}
