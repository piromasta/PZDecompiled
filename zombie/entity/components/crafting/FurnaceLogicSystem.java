package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;

public class FurnaceLogicSystem extends EngineSystem {
   private EntityBucket furnaceLogicEntities;
   private final List<Resource> tempSlotInputResources = new ArrayList();
   private final List<Resource> tempSlotOutputResources = new ArrayList();
   private final CraftRecipeData slotCraftTestData;
   private final CraftRecipeData slotCraftData;
   private CraftRecipeMonitor debugMonitor;

   public FurnaceLogicSystem(int var1) {
      super(false, true, var1);
      this.slotCraftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
      this.slotCraftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
      this.debugMonitor = CraftRecipeMonitor.Create();
      this.debugMonitor.setPrintToConsole(true);
   }

   public void addedToEngine(Engine var1) {
      this.furnaceLogicEntities = var1.getBucket(Family.all(ComponentType.FurnaceLogic, ComponentType.Resources).get());
   }

   public void removedFromEngine(Engine var1) {
   }

   private boolean isValidEntity(GameEntity var1) {
      return var1.isEntityValid() && var1.isValidEngineEntity();
   }

   public void updateSimulation() {
      if (!GameClient.bClient) {
         ImmutableArray var1 = this.furnaceLogicEntities.getEntities();
         if (var1.size() != 0) {
            for(int var9 = 0; var9 < var1.size(); ++var9) {
               GameEntity var2 = (GameEntity)var1.get(var9);
               if (this.isValidEntity(var2)) {
                  FurnaceLogic var3 = (FurnaceLogic)var2.getComponent(ComponentType.FurnaceLogic);
                  Resources var4 = (Resources)var2.getComponent(ComponentType.Resources);
                  if (var3.isValid() && var4.isValid()) {
                     ResourceGroup var5 = var4.getResourceGroup(var3.getFuelInputsGroupName());
                     ResourceGroup var6 = var4.getResourceGroup(var3.getFuelOutputsGroupName());
                     ResourceGroup var7 = var4.getResourceGroup(var3.getFurnaceInputsGroupName());
                     ResourceGroup var8 = var4.getResourceGroup(var3.getFurnaceOutputsGroupName());
                     if (this.verifyFurnaceSlots(var3, var7, var8) && var3.getSlotSize() != 0) {
                        this.updateFurnaceLogic(var3, var5, var6);
                     }
                  }
               }
            }

         }
      }
   }

   private boolean verifyFurnaceSlots(FurnaceLogic var1, ResourceGroup var2, ResourceGroup var3) {
      if (var2 != null && !var2.getResources().isEmpty() && var3 != null && !var3.getResources().isEmpty()) {
         if (var2.getResources().size() != var3.getResources().size()) {
            return false;
         } else {
            if (var1.getSlotSize() != 0 && var1.getSlotSize() != var2.getResources().size()) {
               var1.clearSlots();
            }

            for(int var6 = 0; var6 < var2.getResources().size(); ++var6) {
               Resource var4 = (Resource)var2.getResources().get(var6);
               Resource var5 = (Resource)var3.getResources().get(var6);
               if (var4.getType() != ResourceType.Item || var5.getType() != ResourceType.Item) {
                  DebugLog.General.warn("FurnaceSlot must be of type item!");
                  var1.clearSlots();
                  return false;
               }

               FurnaceLogic.FurnaceSlot var7 = var1.getSlot(var6);
               if (var7 == null) {
                  var7 = var1.createSlot(var6);
               }

               var7.initialize(var4.getId(), var5.getId());
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void updateFurnaceLogic(FurnaceLogic var1, ResourceGroup var2, ResourceGroup var3) {
      if (var1.isRunning()) {
         if (var3 != null && var3.isDirty() && !var1.getCraftData().canCreateOutputs(var3.getResources())) {
            this.cancel(var1, var3);
            return;
         }

         if (var1.isFinished()) {
            this.finish(var1, var3);
            return;
         }

         this.updateFurnaceSlots(var1, false);
         var1.setElapsedTime(var1.getElapsedTime() + 1);
         if (var1.getElapsedTime() > var1.getCurrentRecipe().getTime()) {
            var1.setElapsedTime(var1.getCurrentRecipe().getTime());
         }

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

   private void updateFurnaceSlots(FurnaceLogic var1, boolean var2) {
      int var3 = var1.getSlotSize();

      for(int var4 = 0; var4 < var3; ++var4) {
         FurnaceLogic.FurnaceSlot var5 = var1.getSlot(var4);
         if (var5 != null) {
            ResourceItem var6;
            ResourceItem var7;
            if (var2) {
               var5.clearRecipe();
               var6 = var1.getInputSlotResource(var4);
               var7 = var1.getOutputSlotResource(var4);
               if (var6 != null) {
                  var6.setProgress(0.0);
               }

               if (var7 != null) {
                  var7.setProgress(0.0);
               }
            } else {
               var6 = var1.getInputSlotResource(var4);
               var7 = var1.getOutputSlotResource(var4);
               if (!var6.isEmpty() && !var7.isFull()) {
                  if (var5.getCurrentRecipe() == null) {
                     this.getSlotRecipe(var1, var5, var4);
                     if (var5.getCurrentRecipe() != null) {
                        var5.setElapsedTime(var5.getElapsedTime() + 1);
                     }
                  } else {
                     CraftRecipe var8 = var5.getCurrentRecipe();
                     if (var6.isDirty() || var7.isDirty()) {
                        this.setTempSlotResources(var6, var7);
                        this.slotCraftTestData.setRecipe(var5.getCurrentRecipe());
                        if (!this.slotCraftTestData.canConsumeInputs(this.tempSlotInputResources) || !this.slotCraftTestData.canCreateOutputs(this.tempSlotOutputResources)) {
                           var5.clearRecipe();
                           continue;
                        }
                     }

                     var5.setElapsedTime(var5.getElapsedTime() + 1);
                     if (var5.getElapsedTime() >= var8.getTime()) {
                        this.craftSlotRecipe(var1, var5, var6, var7);
                     }

                     var6.setProgress((double)var5.getElapsedTime() / (double)var8.getTime());
                  }
               } else if (var5.getCurrentRecipe() != null) {
                  var5.clearRecipe();
               }
            }
         }
      }

   }

   private void setTempSlotResources(Resource var1, Resource var2) {
      this.tempSlotInputResources.clear();
      this.tempSlotOutputResources.clear();
      this.tempSlotInputResources.add(var1);
      this.tempSlotOutputResources.add(var2);
   }

   private void getSlotRecipe(FurnaceLogic var1, FurnaceLogic.FurnaceSlot var2, int var3) {
      ResourceItem var4 = var1.getInputSlotResource(var3);
      ResourceItem var5 = var1.getOutputSlotResource(var3);
      this.setTempSlotResources(var4, var5);
      this.slotCraftTestData.setRecipe((CraftRecipe)null);
      CraftRecipe var6 = CraftUtil.getPossibleRecipe(this.slotCraftTestData, var1.getFurnaceRecipes(), this.tempSlotInputResources, this.tempSlotOutputResources);
      if (var6 != null) {
         var2.setRecipe(var6);
      } else {
         var2.clearRecipe();
      }

   }

   private void craftSlotRecipe(FurnaceLogic var1, FurnaceLogic.FurnaceSlot var2, Resource var3, Resource var4) {
      this.setTempSlotResources(var3, var4);
      this.slotCraftData.setRecipe(var2.getCurrentRecipe());
      if (this.slotCraftData.canConsumeInputs(this.tempSlotInputResources) && this.slotCraftData.canCreateOutputs(this.tempSlotOutputResources)) {
         this.slotCraftData.consumeInputs(this.tempSlotInputResources);
         this.slotCraftData.createOutputs(this.tempSlotOutputResources);
      }

      var2.clearRecipe();
   }

   private void start(FurnaceLogic var1, StartMode var2, IsoPlayer var3, ResourceGroup var4) {
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

   private void cancel(FurnaceLogic var1, ResourceGroup var2) {
      this.stop(var1, true, var2);
   }

   private void finish(FurnaceLogic var1, ResourceGroup var2) {
      this.stop(var1, false, var2);
   }

   private void stop(FurnaceLogic var1, boolean var2, ResourceGroup var3) {
      if (!GameClient.bClient) {
         if (var1.isValid()) {
            if (var1.isRunning()) {
               if (var2) {
                  var1.getCraftData().luaCallOnFailed();
               } else if (var1.getCraftData().createOutputs(var3.getResources())) {
                  var1.getCraftData().luaCallOnCreate();
               }

               this.updateFurnaceSlots(var1, true);
               var1.setDoAutomaticCraftCheck(true);
               var1.setRecipe((CraftRecipe)null);
               if (GameServer.bServer) {
               }

            }
         }
      }
   }
}
