package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.core.BoxedStaticValues;
import zombie.network.GameClient;
import zombie.ui.UIFont;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.network.WorldMapSymbolNetworkInfo;

public final class WorldMapSymbolsV2 {
   private static final Pool<WorldMapTextSymbolV2> s_textPool = new Pool(WorldMapTextSymbolV2::new);
   private static final Pool<WorldMapTextureSymbolV2> s_texturePool = new Pool(WorldMapTextureSymbolV2::new);
   private final UIWorldMap m_ui;
   private final WorldMapSymbols m_uiSymbols;
   private final ArrayList<WorldMapBaseSymbolV2> m_symbols = new ArrayList();
   private final Listener m_listener = new Listener(this);

   public WorldMapSymbolsV2(UIWorldMap var1, WorldMapSymbols var2) {
      Objects.requireNonNull(var1);
      this.m_ui = var1;
      this.m_uiSymbols = var2;
      this.m_uiSymbols.addListener(this.m_listener);
      this.reinit();
   }

   public WorldMapTextSymbolV2 addTranslatedText(String var1, UIFont var2, float var3, float var4) {
      WorldMapTextSymbol var5 = this.m_uiSymbols.addTranslatedText(var1, var2, var3, var4, 1.0F, 1.0F, 1.0F, 1.0F);
      return (WorldMapTextSymbolV2)this.m_symbols.get(this.m_uiSymbols.indexOf(var5));
   }

   public WorldMapTextSymbolV2 addUntranslatedText(String var1, UIFont var2, float var3, float var4) {
      WorldMapTextSymbol var5 = this.m_uiSymbols.addUntranslatedText(var1, var2, var3, var4, 1.0F, 1.0F, 1.0F, 1.0F);
      return (WorldMapTextSymbolV2)this.m_symbols.get(this.m_uiSymbols.indexOf(var5));
   }

   public WorldMapTextureSymbolV2 addTexture(String var1, float var2, float var3) {
      WorldMapTextureSymbol var4 = this.m_uiSymbols.addTexture(var1, var2, var3, 1.0F, 1.0F, 1.0F, 1.0F);
      return (WorldMapTextureSymbolV2)this.m_symbols.get(this.m_uiSymbols.indexOf(var4));
   }

   public int hitTest(float var1, float var2) {
      return this.m_uiSymbols.hitTest(this.m_ui, var1, var2);
   }

   public int getSymbolCount() {
      return this.m_symbols.size();
   }

   public WorldMapBaseSymbolV2 getSymbolByIndex(int var1) {
      return (WorldMapBaseSymbolV2)this.m_symbols.get(var1);
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
         ((WorldMapBaseSymbolV2)this.m_symbols.get(var1)).release();
      }

      this.m_symbols.clear();

      for(var1 = 0; var1 < this.m_uiSymbols.getSymbolCount(); ++var1) {
         WorldMapBaseSymbol var2 = this.m_uiSymbols.getSymbolByIndex(var1);
         WorldMapTextSymbol var3 = (WorldMapTextSymbol)Type.tryCastTo(var2, WorldMapTextSymbol.class);
         if (var3 != null) {
            WorldMapTextSymbolV2 var4 = ((WorldMapTextSymbolV2)s_textPool.alloc()).init(this, var3);
            this.m_symbols.add(var4);
         }

         WorldMapTextureSymbol var6 = (WorldMapTextureSymbol)Type.tryCastTo(var2, WorldMapTextureSymbol.class);
         if (var6 != null) {
            WorldMapTextureSymbolV2 var5 = ((WorldMapTextureSymbolV2)s_texturePool.alloc()).init(this, var6);
            this.m_symbols.add(var5);
         }
      }

   }

   public void sendShareSymbol(WorldMapBaseSymbolV2 var1, WorldMapSymbolNetworkInfo var2) {
      WorldMapClient.getInstance().sendShareSymbol(var1.m_symbol, var2);
   }

   public void sendRemoveSymbol(WorldMapBaseSymbolV2 var1) {
      WorldMapClient.getInstance().sendRemoveSymbol(var1.m_symbol);
   }

   public void sendModifySymbol(WorldMapBaseSymbolV2 var1) {
      WorldMapClient.getInstance().sendModifySymbol(var1.m_symbol);
   }

   public void sendSetPrivateSymbol(WorldMapBaseSymbolV2 var1) {
      WorldMapClient.getInstance().sendSetPrivateSymbol(var1.m_symbol);
   }

   public static void setExposed(LuaManager.Exposer var0) {
      var0.setExposed(WorldMapSymbolsV2.class);
      var0.setExposed(WorldMapTextSymbolV2.class);
      var0.setExposed(WorldMapTextureSymbolV2.class);
   }

   private static final class Listener implements IWorldMapSymbolListener {
      final WorldMapSymbolsV2 m_api;

      Listener(WorldMapSymbolsV2 var1) {
         this.m_api = var1;
      }

      public void onAdd(WorldMapBaseSymbol var1) {
         int var2 = this.indexOf(var1);
         WorldMapTextSymbol var3 = (WorldMapTextSymbol)Type.tryCastTo(var1, WorldMapTextSymbol.class);
         if (var3 != null) {
            WorldMapTextSymbolV2 var6 = ((WorldMapTextSymbolV2)WorldMapSymbolsV2.s_textPool.alloc()).init(this.m_api, var3);
            this.m_api.m_symbols.add(var2, var6);
         } else {
            WorldMapTextureSymbol var4 = (WorldMapTextureSymbol)Type.tryCastTo(var1, WorldMapTextureSymbol.class);
            if (var4 != null) {
               WorldMapTextureSymbolV2 var5 = ((WorldMapTextureSymbolV2)WorldMapSymbolsV2.s_texturePool.alloc()).init(this.m_api, var4);
               this.m_api.m_symbols.add(var2, var5);
            } else {
               throw new RuntimeException("unhandled symbol class " + var1.getClass().getSimpleName());
            }
         }
      }

      public void onBeforeRemove(WorldMapBaseSymbol var1) {
         int var2 = this.indexOf(var1);
         WorldMapBaseSymbolV2 var3 = (WorldMapBaseSymbolV2)this.m_api.m_symbols.remove(var2);
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

   public static class WorldMapTextSymbolV2 extends WorldMapBaseSymbolV2 {
      WorldMapTextSymbol m_textSymbol;

      public WorldMapTextSymbolV2() {
      }

      WorldMapTextSymbolV2 init(WorldMapSymbolsV2 var1, WorldMapTextSymbol var2) {
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

   public static class WorldMapTextureSymbolV2 extends WorldMapBaseSymbolV2 {
      WorldMapTextureSymbol m_textureSymbol;

      public WorldMapTextureSymbolV2() {
      }

      WorldMapTextureSymbolV2 init(WorldMapSymbolsV2 var1, WorldMapTextureSymbol var2) {
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

   protected static class WorldMapBaseSymbolV2 extends PooledObject {
      WorldMapSymbolsV2 m_owner;
      WorldMapBaseSymbol m_symbol;

      protected WorldMapBaseSymbolV2() {
      }

      WorldMapBaseSymbolV2 init(WorldMapSymbolsV2 var1, WorldMapBaseSymbol var2) {
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

      public void setSharing(KahluaTable var1) {
         if (var1 != null && !var1.isEmpty()) {
            KahluaTableImpl var2 = (KahluaTableImpl)var1;
            WorldMapSymbolNetworkInfo var3 = new WorldMapSymbolNetworkInfo();
            var3.setAuthor(GameClient.username);
            boolean var4 = var2.rawgetBool("everyone");
            boolean var5 = var2.rawgetBool("faction");
            boolean var6 = var2.rawgetBool("safehouse");
            var3.setVisibleToEveryone(var4);
            var3.setVisibleToFaction(var5 && !var4);
            var3.setVisibleToSafehouse(var6 && !var4);
            if (!var4) {
               KahluaTableImpl var7 = (KahluaTableImpl)Type.tryCastTo(var2.rawget("players"), KahluaTableImpl.class);
               if (var7 != null && !var7.isEmpty()) {
                  int var8 = 1;

                  for(int var9 = var7.len(); var8 <= var9; ++var8) {
                     String var10 = var7.rawgetStr(BoxedStaticValues.toDouble((double)var8));
                     var3.addPlayer(var10);
                  }
               }
            }

            this.m_owner.sendShareSymbol(this, var3);
         } else if (!this.m_symbol.isPrivate()) {
            this.m_owner.sendSetPrivateSymbol(this);
         }
      }

      public boolean isShared() {
         return this.m_symbol.getNetworkInfo() != null;
      }

      public boolean isPrivate() {
         return this.m_symbol.getNetworkInfo() == null;
      }

      public String getAuthor() {
         return this.isShared() ? this.m_symbol.getNetworkInfo().getAuthor() : GameClient.username;
      }

      public boolean isVisibleToEveryone() {
         return this.isShared() && this.m_symbol.getNetworkInfo().isVisibleToEveryone();
      }

      public boolean isVisibleToFaction() {
         return this.isShared() && this.m_symbol.getNetworkInfo().isVisibleToFaction();
      }

      public boolean isVisibleToSafehouse() {
         return this.isShared() && this.m_symbol.getNetworkInfo().isVisibleToSafehouse();
      }

      public int getVisibleToPlayerCount() {
         return this.isShared() ? this.m_symbol.getNetworkInfo().getPlayerCount() : 0;
      }

      public String getVisibleToPlayerByIndex(int var1) {
         return this.isShared() ? this.m_symbol.getNetworkInfo().getPlayerByIndex(var1) : null;
      }

      public boolean canClientModify() {
         return this.isShared() ? StringUtils.equals(GameClient.username, this.getAuthor()) : true;
      }
   }
}
