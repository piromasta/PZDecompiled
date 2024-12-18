package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Terrain extends OptionGroup {
   public final RenderTiles RenderTiles = (RenderTiles)this.newOptionGroup(new RenderTiles());

   public Terrain() {
   }

   public static final class RenderTiles extends OptionGroup {
      public final BooleanDebugOption Enable = this.newDebugOnlyOption("Enable", true);
      public final BooleanDebugOption NewRender = this.newDebugOnlyOption("NewRender", true);
      public final BooleanDebugOption Shadows = this.newDebugOnlyOption("Shadows", true);
      public final BooleanDebugOption BloodDecals = this.newDebugOnlyOption("BloodDecals", true);
      public final BooleanDebugOption Water = this.newDebugOnlyOption("Water", true);
      public final BooleanDebugOption WaterShore = this.newDebugOnlyOption("WaterShore", true);
      public final BooleanDebugOption WaterBody = this.newDebugOnlyOption("WaterBody", true);
      public final BooleanDebugOption Lua = this.newDebugOnlyOption("Lua", true);
      public final BooleanDebugOption VegetationCorpses = this.newDebugOnlyOption("VegetationCorpses", true);
      public final BooleanDebugOption MinusFloorCharacters = this.newDebugOnlyOption("MinusFloorCharacters", true);
      public final BooleanDebugOption RenderGridSquares = this.newDebugOnlyOption("RenderGridSquares", true);
      public final BooleanDebugOption RenderSprites = this.newDebugOnlyOption("RenderSprites", true);
      public final BooleanDebugOption OverlaySprites = this.newDebugOnlyOption("OverlaySprites", true);
      public final BooleanDebugOption AttachedAnimSprites = this.newDebugOnlyOption("AttachedAnimSprites", true);
      public final BooleanDebugOption AttachedChildren = this.newDebugOnlyOption("AttachedChildren", true);
      public final BooleanDebugOption AttachedWallBloodSplats = this.newDebugOnlyOption("AttachedWallBloodSplats", true);
      public final BooleanDebugOption UseShaders = this.newOption("UseShaders", true);
      public final BooleanDebugOption HighContrastBg = this.newDebugOnlyOption("HighContrastBg", false);
      public final IsoGridSquare IsoGridSquare = (IsoGridSquare)this.newOptionGroup(new IsoGridSquare());

      public RenderTiles() {
      }

      public static final class IsoGridSquare extends OptionGroup {
         public final BooleanDebugOption RenderMinusFloor = this.newDebugOnlyOption("RenderMinusFloor", true);
         public final BooleanDebugOption DoorsAndWalls = this.newDebugOnlyOption("DoorsAndWalls", true);
         public final BooleanDebugOption DoorsAndWalls_SimpleLighting = this.newDebugOnlyOption("DoorsAndWallsSL", true);
         public final BooleanDebugOption Objects = this.newDebugOnlyOption("Objects", true);
         public final BooleanDebugOption MeshCutdown = this.newDebugOnlyOption("MeshCutDown", true);
         public final BooleanDebugOption IsoPadding = this.newDebugOnlyOption("IsoPadding", true);
         public final BooleanDebugOption IsoPaddingDeDiamond = this.newDebugOnlyOption("IsoPaddingDeDiamond", true);
         public final BooleanDebugOption IsoPaddingAttached = this.newDebugOnlyOption("IsoPaddingAttached", true);
         public final BooleanDebugOption ShoreFade = this.newDebugOnlyOption("ShoreFade", true);
         public final Walls Walls = (Walls)this.newOptionGroup(new Walls());
         public final Floor Floor = (Floor)this.newOptionGroup(new Floor());

         public IsoGridSquare() {
         }

         public static final class Walls extends OptionGroup {
            public final BooleanDebugOption NW = this.newDebugOnlyOption("NW", true);
            public final BooleanDebugOption W = this.newDebugOnlyOption("W", true);
            public final BooleanDebugOption N = this.newDebugOnlyOption("N", true);
            public final BooleanDebugOption Render = this.newDebugOnlyOption("Render", true);
            public final BooleanDebugOption Lighting = this.newDebugOnlyOption("Lighting", true);
            public final BooleanDebugOption LightingDebug = this.newDebugOnlyOption("LightingDebug", false);
            public final BooleanDebugOption LightingOldDebug = this.newDebugOnlyOption("LightingOldDebug", false);
            public final BooleanDebugOption AttachedSprites = this.newDebugOnlyOption("AttachedSprites", true);

            public Walls() {
            }
         }

         public static final class Floor extends OptionGroup {
            public final BooleanDebugOption Lighting = this.newDebugOnlyOption("Lighting", true);
            public final BooleanDebugOption LightingOld = this.newDebugOnlyOption("LightingOld", false);
            public final BooleanDebugOption LightingDebug = this.newDebugOnlyOption("LightingDebug", false);

            public Floor() {
            }
         }
      }
   }
}
