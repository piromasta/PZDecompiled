package zombie.ui;

import se.krka.kahlua.vm.KahluaTable;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.UIWorldMapV2;
import zombie.worldMap.WorldMapVisited;

public class AtomUIMap extends AtomUI {
   UIWorldMap map;

   public AtomUIMap(KahluaTable var1) {
      super(var1);
   }

   public void render() {
      if (this.visible) {
         super.render();
         double[] var1 = this.getAbsolutePosition(this.x, this.y);
         double[] var2 = this.getAbsolutePosition(this.x + this.width, this.y + this.height);
         this.map.setX(var1[0]);
         this.map.setY(var1[1]);
         this.map.setWidth(var2[0] - var1[0]);
         this.map.setHeight(var2[1] - var1[1]);
         this.map.render();
      }
   }

   public void init() {
      this.map = new UIWorldMap((KahluaTable)null);
      this.map.setDoStencil(false);
      UIWorldMapV2 var1 = this.map.getAPIv2();
      var1.setBoolean("ClampBaseZoomToPoint5", false);
      var1.setBoolean("Isometric", false);
      var1.setBoolean("WorldBounds", false);
      var1.setBoolean("Features", false);
      var1.setBoolean("ImagePyramid", true);
      super.init();
      this.updateInternalValues();
   }

   void updateInternalValues() {
      super.updateInternalValues();
   }

   public UIWorldMap getMapUI() {
      return this.map;
   }

   public void revealOnMap() {
      UIWorldMapV1 var1 = this.map.getAPIv1();
      int var2 = var1.getMinXInSquares();
      int var3 = var1.getMinYInSquares();
      int var4 = var1.getMaxXInSquares();
      int var5 = var1.getMaxYInSquares();
      WorldMapVisited.getInstance().setKnownInSquares(var2, var3, var4, var5);
   }
}
