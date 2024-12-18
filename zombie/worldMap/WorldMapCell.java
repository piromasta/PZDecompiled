package zombie.worldMap;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.iso.IsoCell;

public final class WorldMapCell {
   public int m_x;
   public int m_y;
   public final ArrayList<WorldMapFeature> m_features = new ArrayList();

   public WorldMapCell() {
   }

   public void hitTest(float var1, float var2, ArrayList<WorldMapFeature> var3) {
      var1 -= (float)(this.m_x * IsoCell.CellSizeInSquares);
      var2 -= (float)(this.m_y * IsoCell.CellSizeInSquares);

      for(int var4 = 0; var4 < this.m_features.size(); ++var4) {
         WorldMapFeature var5 = (WorldMapFeature)this.m_features.get(var4);
         if (var5.containsPoint(var1, var2)) {
            var3.add(var5);
         }
      }

   }

   public void dispose() {
      Iterator var1 = this.m_features.iterator();

      while(var1.hasNext()) {
         WorldMapFeature var2 = (WorldMapFeature)var1.next();
         var2.dispose();
      }

      this.m_features.clear();
   }
}
