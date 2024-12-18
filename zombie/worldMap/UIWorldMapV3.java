package zombie.worldMap;

public class UIWorldMapV3 extends UIWorldMapV2 {
   public UIWorldMapV3(UIWorldMap var1) {
      super(var1);
   }

   public boolean isDataLoaded() {
      return this.m_worldMap.isDataLoaded();
   }

   public int getDataWidthInCells() {
      return this.m_worldMap.getDataWidthInCells();
   }

   public int getDataHeightInCells() {
      return this.m_worldMap.getDataHeightInCells();
   }

   public void addImagePyramid(String var1) {
      boolean var2 = this.m_worldMap.hasImages();
      this.m_worldMap.addImagePyramid(var1);
      if (!var2 && this.m_worldMap.getWidthInSquares() > 1 && this.m_ui.getWidth() > 0.0) {
         this.m_renderer.setMap(this.m_worldMap, this.m_ui.getAbsoluteX().intValue(), this.m_ui.getAbsoluteY().intValue(), this.m_ui.getWidth().intValue(), this.m_ui.getHeight().intValue());
         this.resetView();
      }

   }

   public void clearImages() {
      this.m_worldMap.clearImages();
   }

   public int getImagePyramidMinX(String var1) {
      WorldMapImages var2 = this.m_worldMap.getWorldMapImagesByFileName(var1);
      return var2 == null ? -1 : var2.getMinX();
   }

   public int getImagePyramidMinY(String var1) {
      WorldMapImages var2 = this.m_worldMap.getWorldMapImagesByFileName(var1);
      return var2 == null ? -1 : var2.getMinY();
   }

   public int getImagePyramidMaxX(String var1) {
      WorldMapImages var2 = this.m_worldMap.getWorldMapImagesByFileName(var1);
      return var2 == null ? -1 : var2.getMaxX();
   }

   public int getImagePyramidMaxY(String var1) {
      WorldMapImages var2 = this.m_worldMap.getWorldMapImagesByFileName(var1);
      return var2 == null ? -1 : var2.getMaxY();
   }

   public int getImagePyramidWidthInSquares(String var1) {
      WorldMapImages var2 = this.m_worldMap.getWorldMapImagesByFileName(var1);
      return var2 == null ? -1 : var2.getWidthInSquares();
   }

   public int getImagePyramidHeightInSquares(String var1) {
      WorldMapImages var2 = this.m_worldMap.getWorldMapImagesByFileName(var1);
      return var2 == null ? -1 : var2.getHeightInSquares();
   }

   public void setMaxZoom(float var1) {
      this.m_ui.m_renderer.setMaxZoom(var1);
   }

   public float getMaxZoom() {
      return this.m_ui.m_renderer.getMaxZoom();
   }

   public void transitionTo(float var1, float var2, float var3) {
      if (this.m_worldMap.hasData() && this.m_worldMap.isDataLoaded() || this.m_worldMap.hasImages()) {
         if (this.m_renderer.getWorldMap() != null) {
            this.m_renderer.transitionTo(var1, var2, var3);
         }
      }
   }

   public void setDisplayedArea(float var1, float var2, float var3, float var4) {
      this.m_renderer.centerOn(var1 + (var3 - var1) / 2.0F, var2 + (var4 - var2) / 2.0F);
      double var5;
      if (this.m_renderer.getHeight() <= this.m_renderer.getWidth()) {
         var5 = (double)((var4 - var2) / (float)this.m_renderer.getHeight());
         this.setZoom((float)MapProjection.zoomAtMetersPerPixel(var5, (double)this.m_renderer.getHeight()));
      } else {
         var5 = (double)((float)this.m_renderer.getWidth() / (var3 - var1));
         this.setZoom((float)MapProjection.zoomAtMetersPerPixel(var5, (double)this.m_renderer.getWidth()));
      }

   }
}
