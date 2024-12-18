package zombie.core;

import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.iso.weather.WeatherShader;
import zombie.tileDepth.CutawayAttachedShader;
import zombie.tileDepth.TileDepthShader;
import zombie.tileDepth.TileSeamShader;
import zombie.viewCone.BlurShader;
import zombie.viewCone.ChunkRenderShader;

public class SceneShaderStore {
   public static ChunkRenderShader ChunkRenderShader;
   public static DefaultShader DefaultShader;
   public static Shader WeatherShader;
   public static Shader BlurShader;
   public static CutawayAttachedShader CutawayAttachedShader;
   public static TileDepthShader OpaqueDepthShader;
   public static TileDepthShader TileDepthShader;
   public static TileSeamShader TileSeamShader;
   public static int DefaultShaderID;

   public SceneShaderStore() {
   }

   public static void shaderOptionsChanged() {
      try {
         if (CutawayAttachedShader == null) {
            CutawayAttachedShader = new CutawayAttachedShader("CutawayAttached");
         }

         if (OpaqueDepthShader == null) {
            OpaqueDepthShader = new TileDepthShader("opaqueWithDepth");
         }

         if (TileDepthShader == null) {
            TileDepthShader = new TileDepthShader("tileWithDepth");
         }

         if (TileSeamShader == null) {
            TileSeamShader = new TileSeamShader("seamFix2");
         }

         if (DefaultShader == null) {
            DefaultShader = new DefaultShader("default");
            DefaultShaderID = DefaultShader.getID();
         }

         if (BlurShader == null) {
            BlurShader = new BlurShader("blur");
         }

         if (CutawayAttachedShader != null && !CutawayAttachedShader.isCompiled()) {
            CutawayAttachedShader = null;
         }

         if (OpaqueDepthShader != null && !OpaqueDepthShader.isCompiled()) {
            OpaqueDepthShader = null;
         }

         if (TileDepthShader != null && !TileDepthShader.isCompiled()) {
            TileDepthShader = null;
         }

         if (TileSeamShader != null && !TileSeamShader.isCompiled()) {
            TileSeamShader = null;
         }

         if (BlurShader != null && !BlurShader.isCompiled()) {
            BlurShader = null;
         }

         if (DefaultShader != null && !DefaultShader.isCompiled()) {
            DefaultShader = null;
         }

         if (WeatherShader == null) {
            WeatherShader = new WeatherShader("screen");
         }

         if (WeatherShader != null && !WeatherShader.isCompiled()) {
            WeatherShader = null;
         }
      } catch (Exception var1) {
         WeatherShader = null;
         BlurShader = null;
      }

   }

   public static void initShaders() {
      try {
         if (CutawayAttachedShader == null) {
            RenderThread.invokeOnRenderContext(() -> {
               CutawayAttachedShader = new CutawayAttachedShader("CutawayAttached");
            });
         }

         if (OpaqueDepthShader == null) {
            RenderThread.invokeOnRenderContext(() -> {
               OpaqueDepthShader = new TileDepthShader("opaqueWithDepth");
            });
         }

         if (TileDepthShader == null) {
            RenderThread.invokeOnRenderContext(() -> {
               TileDepthShader = new TileDepthShader("tileWithDepth");
            });
         }

         if (TileSeamShader == null) {
            RenderThread.invokeOnRenderContext(() -> {
               TileSeamShader = new TileSeamShader("seamFix2");
            });
         }

         if (WeatherShader == null && !Core.SafeMode && !Core.SafeModeForced) {
            RenderThread.invokeOnRenderContext(() -> {
               WeatherShader = new WeatherShader("screen");
            });
         }

         if (BlurShader == null && !Core.SafeMode && !Core.SafeModeForced) {
            RenderThread.invokeOnRenderContext(() -> {
               BlurShader = new BlurShader("blur");
            });
         }

         if (ChunkRenderShader == null && !Core.SafeMode && !Core.SafeModeForced) {
            RenderThread.invokeOnRenderContext(() -> {
               ChunkRenderShader = new ChunkRenderShader("chunkShader");
            });
         }

         if (ChunkRenderShader == null || !ChunkRenderShader.isCompiled()) {
            ChunkRenderShader = null;
         }

         if (CutawayAttachedShader != null && !CutawayAttachedShader.isCompiled()) {
            CutawayAttachedShader = null;
         }

         if (OpaqueDepthShader != null && !OpaqueDepthShader.isCompiled()) {
            OpaqueDepthShader = null;
         }

         if (TileDepthShader != null && !TileDepthShader.isCompiled()) {
            TileDepthShader = null;
         }

         if (TileSeamShader != null && !TileSeamShader.isCompiled()) {
            TileSeamShader = null;
         }

         if (WeatherShader == null || !WeatherShader.isCompiled()) {
            WeatherShader = null;
         }

         if (WeatherShader == null || !WeatherShader.isCompiled()) {
            WeatherShader = null;
         }
      } catch (Exception var1) {
         WeatherShader = null;
         BlurShader = null;
         var1.printStackTrace();
      }

   }

   public static void initGlobalShader() {
      try {
         if (DefaultShader == null && !Core.SafeMode && !Core.SafeModeForced) {
            RenderThread.invokeOnRenderContext(() -> {
               DefaultShader = new DefaultShader("default");
            });
            DefaultShaderID = DefaultShader.getID();
         }

         if (DefaultShader == null || !DefaultShader.isCompiled()) {
            DefaultShader = null;
         }
      } catch (Exception var1) {
         DefaultShader = null;
         var1.printStackTrace();
      }

   }
}
