package zombie.worldMap;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;

public final class WorldMapData extends Asset {
   public static final HashMap<String, WorldMapData> s_fileNameToData = new HashMap();
   public String m_relativeFileName;
   public final ArrayList<WorldMapCell> m_cells = new ArrayList();
   public final HashMap<Integer, WorldMapCell> m_cellLookup = new HashMap();
   public int m_minX;
   public int m_minY;
   public int m_maxX;
   public int m_maxY;
   public static final AssetType ASSET_TYPE = new AssetType("WorldMapData");

   public static WorldMapData getOrCreateData(String var0) {
      WorldMapData var1 = (WorldMapData)s_fileNameToData.get(var0);
      if (var1 == null && Files.exists(Paths.get(var0), new LinkOption[0])) {
         var1 = (WorldMapData)WorldMapDataAssetManager.instance.load(new AssetPath(var0));
         s_fileNameToData.put(var0, var1);
      }

      return var1;
   }

   public WorldMapData(AssetPath var1, AssetManager var2) {
      super(var1, var2);
   }

   public WorldMapData(AssetPath var1, AssetManager var2, AssetManager.AssetParams var3) {
      super(var1, var2);
   }

   public void clear() {
      Iterator var1 = this.m_cells.iterator();

      while(var1.hasNext()) {
         WorldMapCell var2 = (WorldMapCell)var1.next();
         var2.dispose();
      }

      this.m_cells.clear();
      this.m_cellLookup.clear();
      this.m_minX = 0;
      this.m_minY = 0;
      this.m_maxX = 0;
      this.m_maxY = 0;
   }

   public int getWidthInCells() {
      return this.m_maxX - this.m_minX + 1;
   }

   public int getHeightInCells() {
      return this.m_maxY - this.m_minY + 1;
   }

   public int getWidthInSquares() {
      return this.getWidthInCells() * IsoCell.CellSizeInSquares;
   }

   public int getHeightInSquares() {
      return this.getHeightInCells() * IsoCell.CellSizeInSquares;
   }

   public void onLoaded() {
      this.m_minX = 2147483647;
      this.m_minY = 2147483647;
      this.m_maxX = -2147483648;
      this.m_maxY = -2147483648;
      this.m_cellLookup.clear();

      WorldMapCell var2;
      for(Iterator var1 = this.m_cells.iterator(); var1.hasNext(); this.m_maxY = Math.max(this.m_maxY, var2.m_y)) {
         var2 = (WorldMapCell)var1.next();
         Integer var3 = this.getCellKey(var2.m_x, var2.m_y);
         this.m_cellLookup.put(var3, var2);
         this.m_minX = Math.min(this.m_minX, var2.m_x);
         this.m_minY = Math.min(this.m_minY, var2.m_y);
         this.m_maxX = Math.max(this.m_maxX, var2.m_x);
      }

   }

   public WorldMapCell getCell(int var1, int var2) {
      Integer var3 = this.getCellKey(var1, var2);
      return (WorldMapCell)this.m_cellLookup.get(var3);
   }

   private Integer getCellKey(int var1, int var2) {
      return var1 + var2 * 1000;
   }

   public void hitTest(float var1, float var2, ArrayList<WorldMapFeature> var3) {
      int var4 = (int)PZMath.floor(var1 / (float)IsoCell.CellSizeInSquares);
      int var5 = (int)PZMath.floor(var2 / (float)IsoCell.CellSizeInSquares);
      if (var4 >= this.m_minX && var4 <= this.m_maxX && var5 >= this.m_minY && var5 <= this.m_maxY) {
         WorldMapCell var6 = this.getCell(var4, var5);
         if (var6 != null) {
            var6.hitTest(var1, var2, var3);
         }
      }
   }

   public static void Reset() {
   }

   public AssetType getType() {
      return ASSET_TYPE;
   }

   protected void onBeforeEmpty() {
      super.onBeforeEmpty();
      this.clear();
   }
}
