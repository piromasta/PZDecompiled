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

   public void addTexture(String var1, String var2, int var3, int var4) {
      MapSymbolDefinition var5 = new MapSymbolDefinition();
      var5.id = var1;
      var5.texturePath = var2;
      var5.width = var3;
      var5.height = var4;
      this.m_symbolList.add(var5);
      this.m_symbolByID.put(var1, var5);
   }

   public void addTexture(String var1, String var2) {
      Texture var3 = Texture.getSharedTexture(var2);
      if (var3 == null) {
         this.addTexture(var1, var2, 18, 18);
      } else {
         this.addTexture(var1, var2, var3.getWidth(), var3.getHeight());
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
   }
}
