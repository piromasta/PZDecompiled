package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.core.textures.Texture;

public final class MapSymbolDefinitions {
   private static MapSymbolDefinitions instance;
   private final ArrayList<MapSymbolDefinition> m_symbolList = new ArrayList();
   private final HashMap<String, MapSymbolDefinition> m_symbolByID = new HashMap();

   public MapSymbolDefinitions() {
   }

   public static MapSymbolDefinitions getInstance() {
      if (instance == null) {
         instance = new MapSymbolDefinitions();
      }

      return instance;
   }

   public void addTexture(String var1, String var2, int var3, int var4, String var5) {
      MapSymbolDefinition var6 = new MapSymbolDefinition();
      var6.id = var1;
      var6.texturePath = var2;
      var6.width = var3;
      var6.height = var4;
      if (var5 != null) {
         var6.tab = var5;
      }

      this.m_symbolList.add(var6);
      this.m_symbolByID.put(var1, var6);
   }

   public void addTexture(String var1, String var2) {
      Texture var3 = Texture.getSharedTexture(var2);
      if (var3 == null) {
         this.addTexture(var1, var2, 18, 18, (String)null);
      } else {
         this.addTexture(var1, var2, 20, 20, (String)null);
      }
   }

   public void addTexture(String var1, String var2, String var3) {
      Texture var4 = Texture.getSharedTexture(var2);
      if (var4 == null) {
         this.addTexture(var1, var2, 18, 18, var3);
      } else {
         this.addTexture(var1, var2, 20, 20, var3);
      }
   }

   public int getSymbolCount() {
      return this.m_symbolList.size();
   }

   public MapSymbolDefinition getSymbolByIndex(int var1) {
      return (MapSymbolDefinition)this.m_symbolList.get(var1);
   }

   public MapSymbolDefinition getSymbolById(String var1) {
      return (MapSymbolDefinition)this.m_symbolByID.get(var1);
   }

   public static void Reset() {
      if (instance != null) {
         getInstance().m_symbolList.clear();
         getInstance().m_symbolByID.clear();
      }
   }

   public static final class MapSymbolDefinition {
      private String id;
      private String texturePath;
      private int width;
      private int height;
      private String tab;

      public MapSymbolDefinition() {
      }

      public String getId() {
         return this.id;
      }

      public String getTexturePath() {
         return this.texturePath;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public String getTab() {
         return this.tab;
      }
   }
}
