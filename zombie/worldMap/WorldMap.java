package zombie.worldMap;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetStateObserver;
import zombie.inventory.types.MapItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;
import zombie.worldMap.symbols.MapSymbolDefinitions;

public final class WorldMap implements AssetStateObserver {
   public final ArrayList<WorldMapData> m_data = new ArrayList();
   public final ArrayList<WorldMapImages> m_images = new ArrayList();
   public int m_minDataX;
   public int m_minDataY;
   public int m_maxDataX;
   public int m_maxDataY;
   public int m_minX;
   public int m_minY;
   public int m_maxX;
   public int m_maxY;
   private boolean m_boundsFromData = false;
   public final ArrayList<WorldMapData> m_lastDataInDirectory = new ArrayList();

   public WorldMap() {
   }

   public void setBoundsInCells(int var1, int var2, int var3, int var4) {
      this.setBoundsInSquares(var1 * IsoCell.CellSizeInSquares, var2 * IsoCell.CellSizeInSquares, var3 * IsoCell.CellSizeInSquares + (IsoCell.CellSizeInSquares - 1), var4 * IsoCell.CellSizeInSquares + (IsoCell.CellSizeInSquares - 1));
   }

   public void setBoundsInSquares(int var1, int var2, int var3, int var4) {
      this.m_minX = var1;
      this.m_minY = var2;
      this.m_maxX = var3;
      this.m_maxY = var4;
   }

   public void setBoundsFromData() {
      this.m_boundsFromData = true;
      this.setBoundsInCells(this.m_minDataX, this.m_minDataY, this.m_maxDataX, this.m_maxDataY);
   }

   public void setBoundsFromWorld() {
      IsoMetaGrid var1 = IsoWorld.instance.getMetaGrid();
      this.setBoundsInCells(var1.getMinX(), var1.getMinY(), var1.getMaxX(), var1.getMaxY());
   }

   public void addData(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         String var2 = ZomboidFileSystem.instance.getString(var1);
         WorldMapData var3 = WorldMapData.getOrCreateData(var2);
         if (var3 != null && !this.m_data.contains(var3)) {
            var3.m_relativeFileName = var1;
            this.m_data.add(var3);
            var3.getObserverCb().add(this);
            if (var3.isReady()) {
               this.updateDataBounds();
            }
         }

      }
   }

   public int getDataCount() {
      return this.m_data.size();
   }

   public WorldMapData getDataByIndex(int var1) {
      return (WorldMapData)this.m_data.get(var1);
   }

   public boolean isDataLoaded() {
      for(int var1 = 0; var1 < this.getDataCount(); ++var1) {
         WorldMapData var2 = this.getDataByIndex(var1);
         if (var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void clearData() {
      Iterator var1 = this.m_data.iterator();

      while(var1.hasNext()) {
         WorldMapData var2 = (WorldMapData)var1.next();
         var2.getObserverCb().remove(this);
      }

      this.m_data.clear();
      this.m_lastDataInDirectory.clear();
      this.updateDataBounds();
   }

   public void endDirectoryData() {
      if (this.hasData()) {
         WorldMapData var1 = this.getDataByIndex(this.getDataCount() - 1);
         if (!this.m_lastDataInDirectory.contains(var1)) {
            this.m_lastDataInDirectory.add(var1);
         }
      }

   }

   public boolean isLastDataInDirectory(WorldMapData var1) {
      return this.m_lastDataInDirectory.contains(var1);
   }

   private void updateDataBounds() {
      this.m_minDataX = 2147483647;
      this.m_minDataY = 2147483647;
      this.m_maxDataX = -2147483648;
      this.m_maxDataY = -2147483648;

      for(int var1 = 0; var1 < this.m_data.size(); ++var1) {
         WorldMapData var2 = (WorldMapData)this.m_data.get(var1);
         if (var2.isReady()) {
            this.m_minDataX = Math.min(this.m_minDataX, var2.m_minX);
            this.m_minDataY = Math.min(this.m_minDataY, var2.m_minY);
            this.m_maxDataX = Math.max(this.m_maxDataX, var2.m_maxX);
            this.m_maxDataY = Math.max(this.m_maxDataY, var2.m_maxY);
         }
      }

      if (this.m_minDataX > this.m_maxDataX) {
         this.m_minDataX = this.m_maxDataX = this.m_minDataY = this.m_maxDataY = 0;
      }

   }

   public boolean hasData() {
      return !this.m_data.isEmpty();
   }

   public void addImages(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         WorldMapImages var2 = WorldMapImages.getOrCreate(var1);
         if (var2 != null && !this.m_images.contains(var2)) {
            this.m_images.add(var2);
         }

      }
   }

   public void addImagePyramid(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         WorldMapImages var2 = WorldMapImages.getOrCreateWithFileName(var1);
         if (var2 != null && !this.m_images.contains(var2)) {
            this.m_images.add(var2);
         }

      }
   }

   public boolean hasImages() {
      return !this.m_images.isEmpty();
   }

   public int getImagesCount() {
      return this.m_images.size();
   }

   public WorldMapImages getImagesByIndex(int var1) {
      return (WorldMapImages)this.m_images.get(var1);
   }

   public void clearImages() {
      this.m_images.clear();
   }

   public WorldMapImages getWorldMapImagesByFileName(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         return null;
      } else {
         for(int var2 = 0; var2 < this.getImagesCount(); ++var2) {
            WorldMapImages var3 = this.getImagesByIndex(var2);
            if (var1.equalsIgnoreCase(var3.getAbsolutePath())) {
               return var3;
            }
         }

         return null;
      }
   }

   public int getMinXInCells() {
      return this.m_minX / IsoCell.CellSizeInSquares;
   }

   public int getMinYInCells() {
      return this.m_minY / IsoCell.CellSizeInSquares;
   }

   public int getMaxXInCells() {
      return this.m_maxX / IsoCell.CellSizeInSquares;
   }

   public int getMaxYInCells() {
      return this.m_maxY / IsoCell.CellSizeInSquares;
   }

   public int getWidthInCells() {
      return this.getMaxXInCells() - this.getMinXInCells() + 1;
   }

   public int getHeightInCells() {
      return this.getMaxYInCells() - this.getMinYInCells() + 1;
   }

   public int getMinXInSquares() {
      return this.m_minX;
   }

   public int getMinYInSquares() {
      return this.m_minY;
   }

   public int getMaxXInSquares() {
      return this.m_maxX;
   }

   public int getMaxYInSquares() {
      return this.m_maxY;
   }

   public int getWidthInSquares() {
      return this.m_maxX - this.m_minX + 1;
   }

   public int getHeightInSquares() {
      return this.m_maxY - this.m_minY + 1;
   }

   public WorldMapCell getCell(int var1, int var2) {
      for(int var3 = 0; var3 < this.m_data.size(); ++var3) {
         WorldMapData var4 = (WorldMapData)this.m_data.get(var3);
         if (var4.isReady()) {
            WorldMapCell var5 = var4.getCell(var1, var2);
            if (var5 != null) {
               return var5;
            }
         }
      }

      return null;
   }

   public int getDataWidthInCells() {
      return this.m_maxDataX - this.m_minDataX + 1;
   }

   public int getDataHeightInCells() {
      return this.m_maxDataY - this.m_minDataY + 1;
   }

   public int getDataWidthInSquares() {
      return this.getDataWidthInCells() * IsoCell.CellSizeInSquares;
   }

   public int getDataHeightInSquares() {
      return this.getDataHeightInCells() * IsoCell.CellSizeInSquares;
   }

   public static void Reset() {
      WorldMapSettings.Reset();
      WorldMapVisited.Reset();
      WorldMapData.Reset();
      WorldMapImages.Reset();
      MapSymbolDefinitions.Reset();
      MapItem.Reset();
   }

   public void onStateChanged(Asset.State var1, Asset.State var2, Asset var3) {
      this.updateDataBounds();
      if (this.m_boundsFromData) {
         this.setBoundsInCells(this.m_minDataX, this.m_minDataY, this.m_maxDataX, this.m_maxDataY);
      }

   }
}
