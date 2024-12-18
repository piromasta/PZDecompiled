package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.Objects;
import zombie.Lua.LuaManager;
import zombie.ui.UIFont;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.worldMap.UIWorldMap;

public class WorldMapSymbolsV1 {
   private static final Pool<WorldMapTextSymbolV1> s_textPool = new Pool(WorldMapTextSymbolV1::new);
   private static final Pool<WorldMapTextureSymbolV1> s_texturePool = new Pool(WorldMapTextureSymbolV1::new);
   private final UIWorldMap m_ui;
   private final WorldMapSymbols m_uiSymbols;
   private final ArrayList<WorldMapBaseSymbolV1> m_symbols = new ArrayList();
   private final Listener m_listener = new Listener(this);

   public WorldMapSymbolsV1(UIWorldMap var1, WorldMapSymbols var2) {
      Objects.requireNonNull(var1);
      this.m_ui = var1;
      this.m_uiSymbols = var2;
      this.m_uiSymbols.addListener(this.m_listener);
      this.reinit();
   }

   public WorldMapTextSymbolV1 addTranslatedText(String var1, UIFont var2, float var3, float var4) {
      this.m_uiSymbols.addTranslatedText(var1, var2, var3, var4, 1.0F, 1.0F, 1.0F, 1.0F);
      WorldMapTextSymbolV1 var6 = (WorldMapTextSymbolV1)this.m_symbols.get(this.m_symbols.size() - 1);
      return var6;
   }

   public WorldMapTextSymbolV1 addUntranslatedText(String var1, UIFont var2, float var3, float var4) {
      this.m_uiSymbols.addUntranslatedText(var1, var2, var3, var4, 1.0F, 1.0F, 1.0F, 1.0F);
      WorldMapTextSymbolV1 var6 = (WorldMapTextSymbolV1)this.m_symbols.get(this.m_symbols.size() - 1);
      return var6;
   }

   public WorldMapTextureSymbolV1 addTexture(String var1, float var2, float var3) {
      this.m_uiSymbols.addTexture(var1, var2, var3, 1.0F, 1.0F, 1.0F, 1.0F);
      WorldMapTextureSymbolV1 var5 = (WorldMapTextureSymbolV1)this.m_symbols.get(this.m_symbols.size() - 1);
      return var5;
   }

   public int hitTest(float var1, float var2) {
      return this.m_uiSymbols.hitTest(this.m_ui, var1, var2);
   }

   public int getSymbolCount() {
      return this.m_symbols.size();
   }

   public WorldMapBaseSymbolV1 getSymbolByIndex(int var1) {
      return (WorldMapBaseSymbolV1)this.m_symbols.get(var1);
   }

   public void removeSymbolByIndex(int var1) {
      this.m_uiSymbols.removeSymbolByIndex(var1);
   }

   public void clear() {
      this.m_uiSymbols.clear();
   }

   private void reinit() {
      int var1;
      for(var1 = 0; var1 < this.m_symbols.size(); ++var1) {
         ((WorldMapBaseSymbolV1)this.m_symbols.get(var1)).release();
      }

      this.m_symbols.clear();

      for(var1 = 0; var1 < this.m_uiSymbols.getSymbolCount(); ++var1) {
         WorldMapBaseSymbol var2 = this.m_uiSymbols.getSymbolByIndex(var1);
         WorldMapTextSymbol var3 = (WorldMapTextSymbol)Type.tryCastTo(var2, WorldMapTextSymbol.class);
         if (var3 != null) {
            WorldMapTextSymbolV1 var4 = ((WorldMapTextSymbolV1)s_textPool.alloc()).init(this, var3);
            this.m_symbols.add(var4);
         }

         WorldMapTextureSymbol var6 = (WorldMapTextureSymbol)Type.tryCastTo(var2, WorldMapTextureSymbol.class);
         if (var6 != null) {
            WorldMapTextureSymbolV1 var5 = ((WorldMapTextureSymbolV1)s_texturePool.alloc()).init(this, var6);
            this.m_symbols.add(var5);
         }
      }

   }

   public static void setExposed(LuaManager.Exposer var0) {
      var0.setExposed(WorldMapSymbolsV1.class);
      var0.setExposed(WorldMapTextSymbolV1.class);
      var0.setExposed(WorldMapTextureSymbolV1.class);
   }

   private static final class Listener implements IWorldMapSymbolListener {
      final WorldMapSymbolsV1 m_api;

      Listener(WorldMapSymbolsV1 var1) {
         this.m_api = var1;
      }

      public void onAdd(WorldMapBaseSymbol var1) {
         int var2 = this.indexOf(var1);
         WorldMapTextSymbol var3 = (WorldMapTextSymbol)Type.tryCastTo(var1, WorldMapTextSymbol.class);
         if (var3 != null) {
            WorldMapTextSymbolV1 var6 = ((WorldMapTextSymbolV1)WorldMapSymbolsV1.s_textPool.alloc()).init(this.m_api, var3);
            this.m_api.m_symbols.add(var2, var6);
         } else {
            WorldMapTextureSymbol var4 = (WorldMapTextureSymbol)Type.tryCastTo(var1, WorldMapTextureSymbol.class);
            if (var4 != null) {
               WorldMapTextureSymbolV1 var5 = ((WorldMapTextureSymbolV1)WorldMapSymbolsV1.s_texturePool.alloc()).init(this.m_api, var4);
               this.m_api.m_symbols.add(var2, var5);
            } else {
               throw new RuntimeException("unhandled symbol class " + var1.getClass().getSimpleName());
            }
         }
      }

      public void onBeforeRemove(WorldMapBaseSymbol var1) {
         int var2 = this.indexOf(var1);
         WorldMapBaseSymbolV1 var3 = (WorldMapBaseSymbolV1)this.m_api.m_symbols.remove(var2);
         var3.release();
      }

      public void onAfterRemove(WorldMapBaseSymbol var1) {
      }

      public void onBeforeClear() {
      }

      public void onAfterClear() {
         this.m_api.reinit();
      }

      int indexOf(WorldMapBaseSymbol var1) {
         return this.m_api.m_uiSymbols.indexOf(var1);
      }
   }

   public static class WorldMapTextSymbolV1 extends WorldMapBaseSymbolV1 {
      WorldMapTextSymbol m_textSymbol;

      public WorldMapTextSymbolV1() {
      }

      WorldMapTextSymbolV1 init(WorldMapSymbolsV1 var1, WorldMapTextSymbol var2) {
         super.init(var1, var2);
         this.m_textSymbol = var2;
         return this;
      }

      public void setTranslatedText(String var1) {
         if (!StringUtils.isNullOrWhitespace(var1)) {
            this.m_textSymbol.setTranslatedText(var1);
            this.m_owner.m_uiSymbols.invalidateLayout();
         }
      }

      public void setUntranslatedText(String var1) {
         if (!StringUtils.isNullOrWhitespace(var1)) {
            this.m_textSymbol.setUntranslatedText(var1);
            this.m_owner.m_uiSymbols.invalidateLayout();
         }
      }

      public String getTranslatedText() {
         return this.m_textSymbol.getTranslatedText();
      }

      public String getUntranslatedText() {
         return this.m_textSymbol.getUntranslatedText();
      }

      public boolean isText() {
         return true;
      }
   }

   public static class WorldMapTextureSymbolV1 extends WorldMapBaseSymbolV1 {
      WorldMapTextureSymbol m_textureSymbol;

      public WorldMapTextureSymbolV1() {
      }

      WorldMapTextureSymbolV1 init(WorldMapSymbolsV1 var1, WorldMapTextureSymbol var2) {
         super.init(var1, var2);
         this.m_textureSymbol = var2;
         return this;
      }

      public String getSymbolID() {
         return this.m_textureSymbol.getSymbolID();
      }

      public boolean isTexture() {
         return true;
      }
   }

   protected static class WorldMapBaseSymbolV1 extends PooledObject {
      WorldMapSymbolsV1 m_owner;
      WorldMapBaseSymbol m_symbol;

      protected WorldMapBaseSymbolV1() {
      }

      WorldMapBaseSymbolV1 init(WorldMapSymbolsV1 var1, WorldMapBaseSymbol var2) {
         this.m_owner = var1;
         this.m_symbol = var2;
         return this;
      }

      public float getWorldX() {
         return this.m_symbol.m_x;
      }

      public float getWorldY() {
         return this.m_symbol.m_y;
      }

      public float getDisplayX() {
         this.m_owner.m_uiSymbols.checkLayout(this.m_owner.m_ui);
         return this.m_symbol.m_layoutX + this.m_owner.m_ui.getAPIv1().worldOriginX();
      }

      public float getDisplayY() {
         this.m_owner.m_uiSymbols.checkLayout(this.m_owner.m_ui);
         return this.m_symbol.m_layoutY + this.m_owner.m_ui.getAPIv1().worldOriginY();
      }

      public float getDisplayWidth() {
         this.m_owner.m_uiSymbols.checkLayout(this.m_owner.m_ui);
         return this.m_symbol.widthScaled(this.m_owner.m_ui);
      }

      public float getDisplayHeight() {
         this.m_owner.m_uiSymbols.checkLayout(this.m_owner.m_ui);
         return this.m_symbol.heightScaled(this.m_owner.m_ui);
      }

      public void setAnchor(float var1, float var2) {
         this.m_symbol.setAnchor(var1, var2);
      }

      public void setPosition(float var1, float var2) {
         this.m_symbol.setPosition(var1, var2);
         this.m_owner.m_uiSymbols.invalidateLayout();
      }

      public void setCollide(boolean var1) {
         this.m_symbol.setCollide(var1);
      }

      public void setVisible(boolean var1) {
         this.m_symbol.setVisible(var1);
      }

      public boolean isVisible() {
         return this.m_symbol.isVisible();
      }

      public void setRGBA(float var1, float var2, float var3, float var4) {
         this.m_symbol.setRGBA(var1, var2, var3, var4);
      }

      public float getRed() {
         return this.m_symbol.m_r;
      }

      public float getGreen() {
         return this.m_symbol.m_g;
      }

      public float getBlue() {
         return this.m_symbol.m_b;
      }

      public float getAlpha() {
         return this.m_symbol.m_a;
      }

      public void setScale(float var1) {
         this.m_symbol.setScale(var1);
      }

      public boolean isText() {
         return false;
      }

      public boolean isTexture() {
         return false;
      }
   }
}
