package zombie.entity.components.crafting;

import zombie.characters.IsoPlayer;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;

public class CraftLogicSystem extends EngineSystem {
   private EntityBucket craftLogicEntities;

   public CraftLogicSystem(int var1) {
      super(false, true, var1);
   }

   public void addedToEngine(Engine var1) {
      this.craftLogicEntities = var1.getBucket(Family.all(ComponentType.CraftLogic, ComponentType.Resources).get());
   }

   public void removedFromEngine(Engine var1) {
   }

   private boolean isValidEntity(GameEntity var1) {
      return var1.isEntityValid() && var1.isValidEngineEntity();
   }

   public void updateSimulation() {
      if (!GameClient.bClient) {
         ImmutableArray var1 = this.craftLogicEntities.getEntities();
         if (var1.size() != 0) {
            for(int var7 = 0; var7 < var1.size(); ++var7) {
               GameEntity var2 = (GameEntity)var1.get(var7);
               if (this.isValidEntity(var2)) {
                  CraftLogic var3 = (CraftLogic)var2.getComponent(ComponentType.CraftLogic);
                  Resources var4 = (Resources)var2.getComponent(ComponentType.Resources);
                  if (var3.isValid() && var4.isValid()) {
                     ResourceGroup var5 = var4.getResourceGroup(var3.getInputsGroupName());
                     ResourceGroup var6 = var4.getResourceGroup(var3.getOutputsGroupName());
                     if (var5 != null && !var5.getResources().isEmpty() && var6 != null && !var6.getResources().isEmpty()) {
                        this.updateCraftLogic(var3, var5, var6);
                     }
                  }
               }
            }

         }
      }
   }

   private void updateCraftLogic(CraftLogic var1, ResourceGroup var2, ResourceGroup var3) {
      if (var1.isRunning()) {
         if (var3.isDirty() && !var1.getCraftData().canCreateOutputs(var3.getResources())) {
            this.cancel(var1, var3);
            return;
         }

         if (var1.isFinished()) {
            this.finish(var1, var3);
            return;
         }

         var1.setElapsedTime(var1.getElapsedTime() + 1);
         if (var1.getElapsedTime() > var1.getCurrentRecipe().getTime()) {
            var1.setElapsedTime(var1.getCurrentRecipe().getTime());
         }

         var1.getCraftData().luaCallOnUpdate();
         if (var1.isStopRequested()) {
            this.cancel(var1, var3);
            var1.setStopRequested(false);
            var1.setRequestingPlayer((IsoPlayer)null);
         }
      } else if (var1.isStartRequested()) {
         this.start(var1, StartMode.Manual, var1.getRequestingPlayer(), var2);
         var1.setStartRequested(false);
         var1.setRequestingPlayer((IsoPlayer)null);
      } else if (var1.getStartMode() == StartMode.Automatic && (var1.isDoAutomaticCraftCheck() || var2.isDirty())) {
         this.start(var1, StartMode.Automatic, (IsoPlayer)null, var2);
         var1.setDoAutomaticCraftCheck(false);
      }

   }

   private void start(CraftLogic var1, StartMode var2, IsoPlayer var3, ResourceGroup var4) {
      if (!GameClient.bClient) {
         if (var1.canStart(var2, var3)) {
            CraftRecipe var5 = var1.getPossibleRecipe();
            if (var5 == null) {
               return;
            }

            var1.setRecipe(var5);
            if (!var1.getCraftData().consumeInputs(var4.getResources())) {
               var1.setRecipe((CraftRecipe)null);
               return;
            }

            var1.getCraftData().luaCallOnStart();
            if (GameServer.bServer) {
            }
         }

      }
   }

   private void cancel(CraftLogic var1, ResourceGroup var2) {
      this.stop(var1, true, var2);
   }

   private void finish(CraftLogic var1, ResourceGroup var2) {
      this.stop(var1, false, var2);
   }

   private void stop(CraftLogic var1, boolean var2, ResourceGroup var3) {
      if (!GameClient.bClient) {
         if (var1.isValid()) {
            if (var1.isRunning()) {
               if (var2) {
                  var1.getCraftData().luaCallOnFailed();
               } else if (var1.getCraftData().createOutputs(var3.getResources())) {
                  var1.getCraftData().luaCallOnCreate();
               }

               var1.setDoAutomaticCraftCheck(true);
               var1.setRecipe((CraftRecipe)null);
               if (GameServer.bServer) {
               }

            }
         }
      }
   }
}
