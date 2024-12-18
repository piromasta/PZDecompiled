package zombie.audio.parameters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.audio.FMODAmbientWallLevelData;
import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.utils.BooleanGrid;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

public final class ParameterInside extends FMODGlobalParameter {
   static final FloodFill floodFill = new FloodFill();

   public ParameterInside() {
      super("Inside");
   }

   public float calculateCurrentValue() {
      IsoGameCharacter var1 = this.getCharacter();
      if (var1 == null) {
         return 0.0F;
      } else {
         IsoGridSquare var2 = var1.getCurrentSquare();
         if (var2 == null) {
            return 0.0F;
         } else {
            int var3 = ((IsoPlayer)var1).getIndex();
            if (var2.isInARoom()) {
               return this.calculateInsideFraction(var3, var2);
            } else {
               if (var2.haveRoof) {
                  for(int var4 = var2.getZ() - 1; var4 >= 0; --var4) {
                     IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var2.getX(), var2.getY(), var4);
                     if (var5 != null && var5.getRoom() != null) {
                        return this.calculateInsideFraction(var3, var2);
                     }
                  }
               }

               return var1.getVehicle() == null ? 0.0F : -1.0F;
            }
         }
      }
   }

   private IsoGameCharacter getCharacter() {
      IsoPlayer var1 = null;

      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         IsoPlayer var3 = IsoPlayer.players[var2];
         if (var3 != null && (var1 == null || var1.isDead() && var3.isAlive() || var1.Traits.Deaf.isSet() && !var3.Traits.Deaf.isSet())) {
            var1 = var3;
         }
      }

      return var1;
   }

   public static void renderDebug() {
      if (DebugOptions.instance.ParameterInsideRender.getValue()) {
         if (!FMODAmbientWallLevelData.isOutside(IsoCamera.frameState.CamCharacterSquare)) {
            floodFill.reset();
            floodFill.calculate(IsoCamera.frameState.playerIndex, IsoCamera.frameState.CamCharacterSquare);
            float var0 = 0.05F;

            for(int var1 = 0; var1 < floodFill.choices.size(); ++var1) {
               IsoGridSquare var2 = (IsoGridSquare)floodFill.choices.get(var1);
               if (!floodFill.outsideAdjacent.getValue(floodFill.gridX(var2.x), floodFill.gridY(var2.y))) {
                  LineDrawer.addRect((float)var2.x + var0, (float)var2.y + var0, (float)var2.z, 1.0F - var0 * 2.0F, 1.0F - var0 * 2.0F, 0.5F, 0.5F, 0.5F);
               } else {
                  IsoGridSquare var3 = var2.getAdjacentSquare(IsoDirections.N);
                  IsoGridSquare var4 = var2.getAdjacentSquare(IsoDirections.S);
                  IsoGridSquare var5 = var2.getAdjacentSquare(IsoDirections.W);
                  IsoGridSquare var6 = var2.getAdjacentSquare(IsoDirections.E);
                  if ((!FMODAmbientWallLevelData.isOutside(var3) || !FMODAmbientWallLevelData.passesSoundNorth(var2, true)) && (!FMODAmbientWallLevelData.isOutside(var4) || !FMODAmbientWallLevelData.passesSoundNorth(var4, true)) && (!FMODAmbientWallLevelData.isOutside(var5) || !FMODAmbientWallLevelData.passesSoundWest(var2, true)) && (!FMODAmbientWallLevelData.isOutside(var6) || !FMODAmbientWallLevelData.passesSoundWest(var6, true))) {
                     LineDrawer.addRect((float)var2.x + var0, (float)var2.y + var0, (float)var2.z, 1.0F - var0 * 2.0F, 1.0F - var0 * 2.0F, 1.0F, 1.0F, 1.0F);
                  } else {
                     LineDrawer.addRect((float)var2.x + var0, (float)var2.y + var0, (float)var2.z, 1.0F - var0 * 2.0F, 1.0F - var0 * 2.0F, 0.0F, 1.0F, 0.0F);
                  }
               }
            }

         }
      }
   }

   float calculateInsideFraction(int var1, IsoGridSquare var2) {
      if (var2 != null && !FMODAmbientWallLevelData.isOutside(var2)) {
         floodFill.reset();
         floodFill.calculate(var1, var2);
         int var3 = 0;
         int var4 = 0;

         for(int var5 = 0; var5 < floodFill.choices.size(); ++var5) {
            IsoGridSquare var6 = (IsoGridSquare)floodFill.choices.get(var5);
            if (floodFill.outsideAdjacent.getValue(floodFill.gridX(var6.x), floodFill.gridY(var6.y))) {
               ++var3;
               IsoGridSquare var7 = var6.getAdjacentSquare(IsoDirections.N);
               IsoGridSquare var8 = var6.getAdjacentSquare(IsoDirections.S);
               IsoGridSquare var9 = var6.getAdjacentSquare(IsoDirections.W);
               IsoGridSquare var10 = var6.getAdjacentSquare(IsoDirections.E);
               if (FMODAmbientWallLevelData.isOutside(var7) && FMODAmbientWallLevelData.passesSoundNorth(var6, false) || FMODAmbientWallLevelData.isOutside(var8) && FMODAmbientWallLevelData.passesSoundNorth(var8, false) || FMODAmbientWallLevelData.isOutside(var9) && FMODAmbientWallLevelData.passesSoundWest(var6, false) || FMODAmbientWallLevelData.isOutside(var10) && FMODAmbientWallLevelData.passesSoundWest(var10, false)) {
                  ++var4;
               }
            }
         }

         return var3 == 0 ? 1.0F : 1.0F - (float)var4 / (float)var3;
      } else {
         return 0.0F;
      }
   }

   private static final class FloodFill {
      private IsoChunkMap chunkMap;
      private IsoGridSquare start = null;
      private final int FLOOD_SIZE = 16;
      private final BooleanGrid visited = new BooleanGrid(16, 16);
      private final BooleanGrid outsideAdjacent = new BooleanGrid(16, 16);
      private final ArrayDeque<IsoGridSquare> stack = new ArrayDeque();
      private final ArrayList<IsoGridSquare> choices = new ArrayList(256);

      private FloodFill() {
      }

      void calculate(int var1, IsoGridSquare var2) {
         IsoCell var3 = IsoWorld.instance.CurrentCell;
         this.chunkMap = var3.getChunkMap(var1);
         this.start = var2;
         this.push(this.start.getX(), this.start.getY());

         while((var2 = this.pop()) != null) {
            int var5 = var2.getX();

            int var4;
            for(var4 = var2.getY(); this.shouldVisit(var5, var4, var5, var4 - 1, IsoDirections.N); --var4) {
            }

            boolean var6 = false;
            boolean var7 = false;

            while(true) {
               this.visited.setValue(this.gridX(var5), this.gridY(var4), true);
               IsoGridSquare var8 = this.chunkMap.getGridSquare(var5, var4, this.start.getZ());
               if (var8 != null) {
                  this.choices.add(var8);
               }

               if (!var6 && this.shouldVisit(var5, var4, var5 - 1, var4, IsoDirections.W)) {
                  this.push(var5 - 1, var4);
                  var6 = true;
               } else if (var6 && !this.shouldVisit(var5, var4, var5 - 1, var4, IsoDirections.W)) {
                  var6 = false;
               } else if (var6 && !this.shouldVisit(var5 - 1, var4, var5 - 1, var4 - 1, IsoDirections.N)) {
                  this.push(var5 - 1, var4);
               }

               if (!var7 && this.shouldVisit(var5, var4, var5 + 1, var4, IsoDirections.E)) {
                  this.push(var5 + 1, var4);
                  var7 = true;
               } else if (var7 && !this.shouldVisit(var5, var4, var5 + 1, var4, IsoDirections.E)) {
                  var7 = false;
               } else if (var7 && !this.shouldVisit(var5 + 1, var4, var5 + 1, var4 - 1, IsoDirections.N)) {
                  this.push(var5 + 1, var4);
               }

               ++var4;
               if (!this.shouldVisit(var5, var4 - 1, var5, var4, IsoDirections.S)) {
                  break;
               }
            }
         }

      }

      boolean shouldVisit(int var1, int var2, int var3, int var4, IsoDirections var5) {
         if (this.gridX(var3) < 16 && this.gridX(var3) >= 0) {
            if (this.gridY(var4) < 16 && this.gridY(var4) >= 0) {
               if (this.visited.getValue(this.gridX(var3), this.gridY(var4))) {
                  return false;
               } else {
                  IsoGridSquare var6 = this.chunkMap.getGridSquare(var3, var4, this.start.getZ());
                  if (var6 == null) {
                     return false;
                  } else if (FMODAmbientWallLevelData.isOutside(var6)) {
                     this.outsideAdjacent.setValue(this.gridX(var1), this.gridY(var2), true);
                     return false;
                  } else {
                     IsoGridSquare var7 = this.chunkMap.getGridSquare(var1, var2, this.start.getZ());
                     switch (var5) {
                        case N:
                           return FMODAmbientWallLevelData.passesSoundNorth(var7, false);
                        case S:
                           return FMODAmbientWallLevelData.passesSoundNorth(var6, false);
                        case W:
                           return FMODAmbientWallLevelData.passesSoundWest(var7, false);
                        case E:
                           return FMODAmbientWallLevelData.passesSoundWest(var6, false);
                        default:
                           throw new IllegalArgumentException("unhandled direction");
                     }
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      void push(int var1, int var2) {
         IsoGridSquare var3 = this.chunkMap.getGridSquare(var1, var2, this.start.getZ());
         this.stack.push(var3);
      }

      IsoGridSquare pop() {
         return this.stack.isEmpty() ? null : (IsoGridSquare)this.stack.pop();
      }

      int gridX(int var1) {
         return var1 - (this.start.getX() - 8);
      }

      int gridY(int var1) {
         return var1 - (this.start.getY() - 8);
      }

      void reset() {
         this.choices.clear();
         this.stack.clear();
         this.visited.clear();
         this.outsideAdjacent.clear();
      }
   }
}
