package zombie.worldMap;

import zombie.worldMap.symbols.WorldMapSymbolsV2;

public class UIWorldMapV2 extends UIWorldMapV1 {
   protected WorldMapSymbolsV2 m_symbolsV2 = null;

   public UIWorldMapV2(UIWorldMap var1) {
      super(var1);
   }

   public WorldMapSymbolsV2 getSymbolsAPIv2() {
      if (this.m_symbolsV2 == null) {
         this.m_symbolsV2 = new WorldMapSymbolsV2(this.m_ui, this.m_ui.m_symbols);
      }

      return this.m_symbolsV2;
   }

   public boolean isDimUnsharedSymbols() {
      return this.m_renderer.isDimUnsharedSymbols();
   }
}
