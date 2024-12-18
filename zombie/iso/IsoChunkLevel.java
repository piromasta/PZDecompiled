package zombie.iso;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.audio.FMODAmbientWallLevelData;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.objects.RainManager;

public final class IsoChunkLevel {
   public static final byte CLDSF_NONE = 0;
   public static final byte CLDSF_SHOULD_RENDER = 1;
   public static final byte CLDSF_RAIN_RANDOM_XY = 2;
   public IsoChunk m_chunk;
   public int m_level;
   public final IsoGridSquare[] squares = new IsoGridSquare[64];
   public final boolean[] lightCheck = new boolean[4];
   public boolean physicsCheck = false;
   public final byte[] m_rainFlags = new byte[64];
   public final float[] m_rainSplashFrame = new float[64];
   public boolean m_bRaining = false;
   public int m_rainSplashFrameNum = -1;
   public FMODAmbientWallLevelData m_fmodAmbientWallLevelData = null;
   private static final ConcurrentLinkedQueue<IsoChunkLevel> pool = new ConcurrentLinkedQueue();

   public IsoChunkLevel() {
   }

   public IsoChunkLevel init(IsoChunk var1, int var2) {
      this.m_chunk = var1;
      this.m_level = var2;
      Arrays.fill(this.m_rainSplashFrame, -1.0F);
      return this;
   }

   public IsoChunk getChunk() {
      return this.m_chunk;
   }

   public int getLevel() {
      return this.m_level;
   }

   public void updateRainSplashes() {
      if (this.m_rainSplashFrameNum != IsoCamera.frameState.frameCount) {
         this.m_rainSplashFrameNum = IsoCamera.frameState.frameCount;
         boolean var1 = IsoWorld.instance.CurrentCell.getRainIntensity() > 0 || RainManager.isRaining() && RainManager.RainIntensity > 0.0F;
         if (var1) {
            this.m_bRaining = true;
            if (!IsoCamera.frameState.Paused) {
               int var2 = IsoWorld.instance.CurrentCell.getRainIntensity();
               if (var2 == 0) {
                  var2 = Math.min(PZMath.fastfloor(RainManager.RainIntensity / 0.2F) + 1, 5);
               }

               for(int var3 = 0; var3 < this.m_rainSplashFrame.length; ++var3) {
                  if (this.m_rainSplashFrame[var3] < 0.0F) {
                     if (Rand.NextBool(Rand.AdjustForFramerate((int)(5.0F / (float)var2) * 100))) {
                        this.m_rainSplashFrame[var3] = 0.0F;
                        byte[] var10000 = this.m_rainFlags;
                        var10000[var3] = (byte)(var10000[var3] | 2);
                     }
                  } else {
                     float[] var4 = this.m_rainSplashFrame;
                     var4[var3] += 0.08F * (30.0F / (float)PerformanceSettings.getLockFPS());
                     if (this.m_rainSplashFrame[var3] >= 1.0F) {
                        this.m_rainSplashFrame[var3] = -1.0F;
                     }
                  }
               }

            }
         } else {
            if (this.m_bRaining) {
               this.m_bRaining = false;
               Arrays.fill(this.m_rainSplashFrame, -1.0F);
            }

         }
      }
   }

   public void renderRainSplashes(int var1) {
      if (this.m_bRaining) {
         FBORenderCutaways.ChunkLevelData var2 = this.m_chunk.getCutawayDataForLevel(this.m_level);

         for(int var3 = 0; var3 < this.m_rainSplashFrame.length; ++var3) {
            if (!(this.m_rainSplashFrame[var3] < 0.0F)) {
               IsoGridSquare var4 = this.m_chunk.getGridSquare(var3 % 8, var3 / 8, this.m_level);
               if (var2.shouldRenderSquare(var1, var4)) {
                  var4.renderRainSplash(var1, var4.getLightInfo(var1), this.m_rainSplashFrame[var3], (this.m_rainFlags[var3] & 2) != 0);
                  byte[] var10000 = this.m_rainFlags;
                  var10000[var3] &= -3;
               }
            }
         }

      }
   }

   public void clear() {
      Arrays.fill(this.squares, (Object)null);
      Arrays.fill(this.lightCheck, true);
      this.physicsCheck = false;
      Arrays.fill(this.m_rainFlags, (byte)0);
      Arrays.fill(this.m_rainSplashFrame, -1.0F);
      this.m_bRaining = false;
      this.m_rainSplashFrameNum = -1;
   }

   public static IsoChunkLevel alloc() {
      IsoChunkLevel var0 = (IsoChunkLevel)pool.poll();
      if (var0 == null) {
         var0 = new IsoChunkLevel();
      }

      return var0;
   }

   public void release() {
      if (this.m_fmodAmbientWallLevelData != null) {
         this.m_fmodAmbientWallLevelData.release();
         this.m_fmodAmbientWallLevelData = null;
      }

      pool.add(this);
   }
}
