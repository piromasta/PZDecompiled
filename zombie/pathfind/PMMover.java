package zombie.pathfind;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.iso.IsoDirections;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLLevelTransition;

public final class PMMover {
   public MoverType type;
   public boolean bCanCrawl;
   public boolean bCrawling;
   public boolean bIgnoreCrawlCost;
   public boolean bCanThump;
   public boolean bCanClimbFences;
   public int minLevel;
   public int maxLevel;
   public ArrayList<HLChunkLevel> allowedChunkLevels;
   public ArrayList<HLLevelTransition> allowedLevelTransitions;

   public PMMover() {
   }

   public PMMover set(PathFindRequest var1) {
      this.bCrawling = false;
      this.bCanClimbFences = false;
      if (var1.mover instanceof IsoAnimal) {
         this.type = MoverType.Animal;
         this.bCanClimbFences = ((IsoAnimal)var1.mover).canClimbFences();
      } else if (var1.mover instanceof IsoPlayer) {
         this.type = MoverType.Player;
      } else {
         if (!(var1.mover instanceof IsoZombie)) {
            throw new IllegalArgumentException("unsupported Mover " + var1.mover);
         }

         this.type = MoverType.Zombie;
         this.bCrawling = ((IsoZombie)var1.mover).bCrawling;
      }

      this.bCanCrawl = var1.bCanCrawl;
      this.bIgnoreCrawlCost = var1.bIgnoreCrawlCost;
      this.bCanThump = var1.bCanThump;
      this.minLevel = var1.minLevel;
      this.maxLevel = var1.maxLevel;
      this.allowedChunkLevels = var1.allowedChunkLevels;
      this.allowedLevelTransitions = var1.allowedLevelTransitions;
      return this;
   }

   public PMMover set(PMMover var1) {
      this.type = var1.type;
      this.bCanCrawl = var1.bCanCrawl;
      this.bCrawling = var1.bCrawling;
      this.bIgnoreCrawlCost = var1.bIgnoreCrawlCost;
      this.bCanThump = var1.bCanThump;
      this.minLevel = var1.minLevel;
      this.maxLevel = var1.maxLevel;
      this.allowedChunkLevels = var1.allowedChunkLevels;
      this.allowedLevelTransitions = var1.allowedLevelTransitions;
      return this;
   }

   public boolean isAnimal() {
      return this.type == MoverType.Animal;
   }

   public boolean isPlayer() {
      return this.type == MoverType.Player;
   }

   public boolean isZombie() {
      return this.type == MoverType.Zombie;
   }

   public boolean isAllowedChunkLevel(Square var1) {
      if (this.allowedChunkLevels != null && !this.allowedChunkLevels.isEmpty()) {
         Chunk var2 = PolygonalMap2.instance.getChunkFromSquarePos(var1.x, var1.y);
         HLChunkLevel var3 = var2.getLevelData(var1.z).getHighLevelData();
         return this.allowedChunkLevels.contains(var3);
      } else {
         return true;
      }
   }

   public boolean isAllowedLevelTransition(IsoDirections var1, Square var2, boolean var3) {
      if (this.allowedLevelTransitions != null && !this.allowedLevelTransitions.isEmpty()) {
         for(int var4 = 0; var4 < this.allowedLevelTransitions.size(); ++var4) {
            HLLevelTransition var5 = (HLLevelTransition)this.allowedLevelTransitions.get(var4);
            if (var3) {
               if (var5.getTopFloorSquare() == var2) {
                  return true;
               }
            } else if (var5.getBottomFloorSquare() == var2) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }
}
