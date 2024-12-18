package zombie.characters.animals;

import java.util.ArrayList;
import zombie.characters.action.ActionGroup;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.gameStates.IngameState;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class AnimalManagerMain {
   private static AnimalManagerMain instance;

   public AnimalManagerMain() {
   }

   static AnimalManagerMain getInstance() {
      if (instance == null) {
         instance = new AnimalManagerMain();
      }

      return instance;
   }

   void loadChunk(int var1, int var2) {
      AnimalManagerWorker.getInstance().loadChunk(var1, var2);
   }

   void unloadChunk(int var1, int var2) {
      AnimalManagerWorker.getInstance().unloadChunk(var1, var2);
   }

   void addAnimal(VirtualAnimal var1) {
      AnimalManagerWorker.getInstance().addAnimal(var1);
   }

   void saveRealAnimals() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoAnimal var4 = (IsoAnimal)Type.tryCastTo((IsoMovingObject)var2.get(var3), IsoAnimal.class);
         if (var4 != null) {
            var1.add(var4);
         }
      }

      AnimalPopulationManager.getInstance().n_saveRealAnimals(var1);
   }

   void fromWorker(ArrayList<VirtualAnimal> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         VirtualAnimal var3 = (VirtualAnimal)var1.get(var2);
         IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(PZMath.fastfloor(var3.getX()), PZMath.fastfloor(var3.getY()), PZMath.fastfloor(var3.getZ()));
         if (var4 != null) {
            float var5 = 0.1F;
            float var6 = (float)(var4.chunk.wx * 8);
            float var7 = (float)(var4.chunk.wy * 8);
            float var8 = var6 + 8.0F;
            float var9 = var7 + 8.0F;

            for(int var10 = 0; var10 < var3.m_animals.size(); ++var10) {
               IsoAnimal var11 = (IsoAnimal)var3.m_animals.get(var10);
               if (var11.virtualID == 0.0 && StringUtils.isNullOrEmpty(var11.migrationGroup)) {
                  var11.setForceX(var3.getX());
                  var11.setForceY(var3.getY());
                  var4 = IsoWorld.instance.CurrentCell.getGridSquare(PZMath.fastfloor(var11.getX()), PZMath.fastfloor(var11.getY()), PZMath.fastfloor(var3.getZ()));
               } else {
                  float var12 = var3.getX() + (float)Rand.Next(-3, 3);
                  float var13 = var3.getY() + (float)Rand.Next(-3, 3);
                  var11.setForceX(PZMath.clamp(var12, var6 + var5, var8 - var5));
                  var11.setForceY(PZMath.clamp(var13, var7 + var5, var9 - var5));
                  var4 = IsoWorld.instance.CurrentCell.getGridSquare(PZMath.fastfloor(var11.getX()), PZMath.fastfloor(var11.getY()), PZMath.fastfloor(var3.getZ()));
               }

               var11.setZ(var3.getZ());
               var11.setDir(IsoDirections.getRandom());
               var11.setForwardDirection(var3.m_forwardDirection.x + (float)Rand.Next(360), var3.m_forwardDirection.y + (float)Rand.Next(360));
               var11.setCurrent(var4);
               var11.setLast(var4);
               var11.setMovingSquareNow();
               var11.virtualID = var3.id;
               var11.migrationGroup = var3.migrationGroup;
               if (!IngameState.bLoading) {
                  var11.fromMeta = true;
               }

               var11.addToWorld();
               var11.getActionContext().setGroup(ActionGroup.getActionGroup(var11.adef.animset));
               var11.advancedAnimator.OnAnimDataChanged(false);
               var11.updateStatsAway(0);

               assert !IsoWorld.instance.CurrentCell.getObjectList().contains(var11);

               if (IsoWorld.instance.CurrentCell.getObjectList().contains(var11)) {
                  DebugLog.Animal.error("Animal already in IsoCell.ObjectList.");
               } else {
                  IsoWorld.instance.CurrentCell.getObjectList().add(var11);
               }
            }
         }
      }

   }
}
