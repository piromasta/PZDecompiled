package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.popman.ObjectPool;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapRenderer;

public abstract class WorldMapStyleLayer {
   public String m_id;
   public float m_minZoom = 0.0F;
   public IWorldMapStyleFilter m_filter;
   public String m_filterKey;
   public String m_filterValue;

   public WorldMapStyleLayer(String var1) {
      this.m_id = var1;
   }

   public abstract String getTypeString();

   static <S extends Stop> int findStop(float var0, ArrayList<S> var1) {
      if (var1.isEmpty()) {
         return -2;
      } else if (var0 <= ((Stop)var1.get(0)).m_zoom) {
         return -1;
      } else {
         for(int var2 = 0; var2 < var1.size() - 1; ++var2) {
            if (var0 <= ((Stop)var1.get(var2 + 1)).m_zoom) {
               return var2;
            }
         }

         return var1.size() - 1;
      }
   }

   protected RGBAf evalColor(RenderArgs var1, ArrayList<ColorStop> var2) {
      if (var2.isEmpty()) {
         return ((RGBAf)WorldMapStyleLayer.RGBAf.s_pool.alloc()).init(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         float var3 = var1.drawer.m_zoomF;
         int var4 = findStop(var3, var2);
         int var5 = var4 == -1 ? 0 : var4;
         int var6 = PZMath.min(var4 + 1, var2.size() - 1);
         ColorStop var7 = (ColorStop)var2.get(var5);
         ColorStop var8 = (ColorStop)var2.get(var6);
         float var9 = var5 == var6 ? 1.0F : (PZMath.clamp(var3, var7.m_zoom, var8.m_zoom) - var7.m_zoom) / (var8.m_zoom - var7.m_zoom);
         float var10 = PZMath.lerp((float)var7.r, (float)var8.r, var9) / 255.0F;
         float var11 = PZMath.lerp((float)var7.g, (float)var8.g, var9) / 255.0F;
         float var12 = PZMath.lerp((float)var7.b, (float)var8.b, var9) / 255.0F;
         float var13 = PZMath.lerp((float)var7.a, (float)var8.a, var9) / 255.0F;
         return ((RGBAf)WorldMapStyleLayer.RGBAf.s_pool.alloc()).init(var10, var11, var12, var13);
      }
   }

   protected float evalFloat(RenderArgs var1, ArrayList<FloatStop> var2) {
      if (var2.isEmpty()) {
         return 1.0F;
      } else {
         float var3 = var1.drawer.m_zoomF;
         int var4 = findStop(var3, var2);
         int var5 = var4 == -1 ? 0 : var4;
         int var6 = PZMath.min(var4 + 1, var2.size() - 1);
         FloatStop var7 = (FloatStop)var2.get(var5);
         FloatStop var8 = (FloatStop)var2.get(var6);
         float var9 = var5 == var6 ? 1.0F : (PZMath.clamp(var3, var7.m_zoom, var8.m_zoom) - var7.m_zoom) / (var8.m_zoom - var7.m_zoom);
         return PZMath.lerp(var7.f, var8.f, var9);
      }
   }

   protected Texture evalTexture(RenderArgs var1, ArrayList<TextureStop> var2) {
      if (var2.isEmpty()) {
         return null;
      } else {
         float var3 = var1.drawer.m_zoomF;
         int var4 = findStop(var3, var2);
         int var5 = var4 == -1 ? 0 : var4;
         int var6 = PZMath.min(var4 + 1, var2.size() - 1);
         TextureStop var7 = (TextureStop)var2.get(var5);
         TextureStop var8 = (TextureStop)var2.get(var6);
         if (var7 == var8) {
            return var3 < var7.m_zoom ? null : var7.texture;
         } else if (!(var3 < var7.m_zoom) && !(var3 > var8.m_zoom)) {
            float var9 = var5 == var6 ? 1.0F : (PZMath.clamp(var3, var7.m_zoom, var8.m_zoom) - var7.m_zoom) / (var8.m_zoom - var7.m_zoom);
            return var9 < 0.5F ? var7.texture : var8.texture;
         } else {
            return null;
         }
      }
   }

   public boolean filter(WorldMapFeature var1, FilterArgs var2) {
      return this.m_filter == null ? false : this.m_filter.filter(var1, var2);
   }

   public abstract void render(WorldMapFeature var1, RenderArgs var2);

   public void renderCell(RenderArgs var1) {
   }

   public static class Stop {
      public float m_zoom;

      Stop(float var1) {
         this.m_zoom = var1;
      }
   }

   public static final class RGBAf {
      public float r;
      public float g;
      public float b;
      public float a;
      public static final ObjectPool<RGBAf> s_pool = new ObjectPool(RGBAf::new);

      public RGBAf() {
         this.r = this.g = this.b = this.a = 1.0F;
      }

      public RGBAf init(float var1, float var2, float var3, float var4) {
         this.r = var1;
         this.g = var2;
         this.b = var3;
         this.a = var4;
         return this;
      }
   }

   public static final class RenderArgs {
      public WorldMapRenderer renderer;
      public WorldMapRenderer.Drawer drawer;
      public int cellX;
      public int cellY;

      public RenderArgs() {
      }
   }

   public static class ColorStop extends Stop {
      public int r;
      public int g;
      public int b;
      public int a;

      public ColorStop(float var1, int var2, int var3, int var4, int var5) {
         super(var1);
         this.r = var2;
         this.g = var3;
         this.b = var4;
         this.a = var5;
      }
   }

   public static class FloatStop extends Stop {
      public float f;

      public FloatStop(float var1, float var2) {
         super(var1);
         this.f = var2;
      }
   }

   public static class TextureStop extends Stop {
      public String texturePath;
      public Texture texture;

      public TextureStop(float var1, String var2) {
         super(var1);
         this.texturePath = var2;
         this.texture = Texture.getTexture(var2);
      }
   }

   public interface IWorldMapStyleFilter {
      boolean filter(WorldMapFeature var1, FilterArgs var2);
   }

   public static final class FilterArgs {
      public WorldMapRenderer renderer;

      public FilterArgs() {
      }
   }
}
