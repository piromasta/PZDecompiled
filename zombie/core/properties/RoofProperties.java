package zombie.core.properties;

import zombie.iso.IsoDirections;
import zombie.iso.sprite.IsoSprite;
import zombie.seams.SeamFile;
import zombie.seams.SeamManager;

public final class RoofProperties {
   public SeamFile.Tile seamFileTile = null;

   public RoofProperties() {
   }

   public static RoofProperties initSprite(IsoSprite var0) {
      if (var0 != null && var0.tilesetName != null) {
         SeamFile.Tile var1 = SeamManager.getInstance().getHighestPriorityTile(var0.tilesetName, var0.tileSheetIndex % 8, var0.tileSheetIndex / 8);
         if (var1 == null) {
            return null;
         } else {
            if (var1.m_properties != null && var1.m_properties.containsKey("master")) {
               var1 = SeamManager.getInstance().getHighestPriorityTileFromName((String)var1.m_properties.get("master"));
               if (var1 == null) {
                  return null;
               }
            }

            RoofProperties var2 = new RoofProperties();
            var2.seamFileTile = var1;
            return var2;
         }
      } else {
         return null;
      }
   }

   public boolean hasPossibleSeamSameLevel(IsoDirections var1) {
      boolean var10000;
      switch (var1) {
         case E:
            var10000 = this.seamFileTile.joinE != null && !this.seamFileTile.joinE.isEmpty();
            break;
         case S:
            var10000 = this.seamFileTile.joinS != null && !this.seamFileTile.joinS.isEmpty();
            break;
         default:
            var10000 = false;
      }

      return var10000;
   }

   public boolean hasPossibleSeamLevelBelow(IsoDirections var1) {
      boolean var10000;
      switch (var1) {
         case E:
            var10000 = this.seamFileTile.joinBelowE != null && !this.seamFileTile.joinBelowE.isEmpty();
            break;
         case S:
            var10000 = this.seamFileTile.joinBelowS != null && !this.seamFileTile.joinBelowS.isEmpty();
            break;
         default:
            var10000 = false;
      }

      return var10000;
   }

   public boolean isJoinedSameLevelEast(RoofProperties var1) {
      return this.seamFileTile.joinE != null && this.seamFileTile.joinE.contains(var1.seamFileTile.tileName);
   }

   public boolean isJoinedSameLevelSouth(RoofProperties var1) {
      return this.seamFileTile.joinS != null && this.seamFileTile.joinS.contains(var1.seamFileTile.tileName);
   }

   public boolean isJoinedLevelBelowEast(RoofProperties var1) {
      return this.seamFileTile.joinBelowE != null && this.seamFileTile.joinBelowE.contains(var1.seamFileTile.tileName);
   }

   public boolean isJoinedLevelBelowSouth(RoofProperties var1) {
      return this.seamFileTile.joinBelowS != null && this.seamFileTile.joinBelowS.contains(var1.seamFileTile.tileName);
   }
}
