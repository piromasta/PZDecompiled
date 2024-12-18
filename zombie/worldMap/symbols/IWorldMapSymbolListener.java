package zombie.worldMap.symbols;

public interface IWorldMapSymbolListener {
   void onAdd(WorldMapBaseSymbol var1);

   void onBeforeRemove(WorldMapBaseSymbol var1);

   void onAfterRemove(WorldMapBaseSymbol var1);

   void onBeforeClear();

   void onAfterClear();
}
