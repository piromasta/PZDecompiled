package zombie.tileDepth;

import zombie.core.textures.Texture;

public class TileDepthMapManager {
   public static TileDepthMapManager instance = new TileDepthMapManager();
   Texture[] presets;

   public TileDepthMapManager() {
   }

   public Texture getTextureForPreset(TileDepthPreset var1) {
      return this.presets[var1.index];
   }

   public void init() {
      this.presets = new Texture[8];

      for(int var1 = 0; var1 < TileDepthMapManager.TileDepthPreset.Max.index; ++var1) {
         TileDepthTexture var2 = TileDepthTextureManager.getInstance().getPresetDepthTexture(var1, 0);
         this.presets[var1] = var2 == null ? null : var2.getTexture();
      }

   }

   public static enum TileDepthPreset {
      Floor(0),
      WDoorFrame(2),
      NDoorFrame(3),
      WWall(4),
      NWall(5),
      NWWall(6),
      SEWall(7),
      Max(8);

      private static final TileDepthPreset[] VALUES = values();
      private final int index;

      private TileDepthPreset(int var3) {
         this.index = var3;
      }

      public int index() {
         return this.index;
      }

      public static TileDepthPreset fromIndex(int var0) {
         return VALUES[var0];
      }
   }
}
