package zombie.tileDepth;

import zombie.core.Core;
import zombie.core.textures.Texture;

public final class TileSeamManager {
   public static final TileSeamManager instance = new TileSeamManager();
   private final Texture[] m_textures = new Texture[TileSeamManager.Tiles.values().length];
   private final float[][] m_vertices = new float[TileSeamManager.Tiles.values().length][];

   public TileSeamManager() {
   }

   public void init() {
      Tiles[] var1 = TileSeamManager.Tiles.values();
      Texture var2 = Texture.getSharedTexture("media/depthmaps/SEAMS_01.png", 0);

      for(int var3 = 0; var3 < var1.length; ++var3) {
         int var4 = var3 % 8;
         int var5 = var3 / 8;
         this.m_textures[var3] = new Texture(var2.getTextureId(), "SEAMS_01_" + var3, var4 * 64 * Core.TileScale, var5 * 128 * Core.TileScale, 64 * Core.TileScale, 128 * Core.TileScale);
      }

      this.m_vertices[TileSeamManager.Tiles.FloorSouth.ordinal()] = new float[]{5.0F, 221.0F, 69.0F, 253.0F, 63.0F, 256.0F, -1.0F, 224.0F};
      this.m_vertices[TileSeamManager.Tiles.FloorEast.ordinal()] = new float[]{57.0F, 253.0F, 121.0F, 221.0F, 127.0F, 224.0F, 63.0F, 256.0F};
      this.m_vertices[TileSeamManager.Tiles.FloorSouthOneThird.ordinal()] = new float[]{5.0F, 157.0F, 69.0F, 189.0F, 63.0F, 192.0F, -1.0F, 160.0F};
      this.m_vertices[TileSeamManager.Tiles.FloorEastOneThird.ordinal()] = new float[]{57.0F, 189.0F, 121.0F, 157.0F, 127.0F, 160.0F, 63.0F, 192.0F};
      this.m_vertices[TileSeamManager.Tiles.FloorSouthTwoThirds.ordinal()] = new float[]{5.0F, 93.0F, 69.0F, 125.0F, 63.0F, 128.0F, -1.0F, 96.0F};
      this.m_vertices[TileSeamManager.Tiles.FloorEastTwoThirds.ordinal()] = new float[]{57.0F, 125.0F, 121.0F, 93.0F, 127.0F, 96.0F, 63.0F, 128.0F};
   }

   public Texture getTexture(Tiles var1) {
      return this.m_textures[var1.ordinal()];
   }

   public float[] getVertices(Tiles var1) {
      return this.m_vertices[var1.ordinal()];
   }

   public static enum Tiles {
      FloorSouth,
      FloorEast,
      WallSouth,
      WallEast,
      FloorSouthOneThird,
      FloorEastOneThird,
      FloorSouthTwoThirds,
      FloorEastTwoThirds;

      private Tiles() {
      }
   }
}
