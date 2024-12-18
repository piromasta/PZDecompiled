package zombie.gameStates;

import fmod.javafmod;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.FliesSound;
import zombie.GameProfiler;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.LootRespawn;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.audio.FMODAmbientWalls;
import zombie.audio.ObjectAmbientEmitters;
import zombie.audio.parameters.ParameterRoomTypeEx;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorFactory;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.professions.ProfessionFactory;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.TraitFactory;
import zombie.chat.ChatElement;
import zombie.core.ActionManager;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.Languages;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.TransactionManager;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.stash.StashSystem;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.entity.GameEntityManager;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.energy.Energy;
import zombie.erosion.ErosionGlobals;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.SGlobalObjects;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.inventory.ItemSoundManager;
import zombie.iso.BentFences;
import zombie.iso.BrokenFences;
import zombie.iso.BuildingDef;
import zombie.iso.ContainerOverlays;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LightingThread;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SearchMode;
import zombie.iso.TileOverlays;
import zombie.iso.WorldMarkers;
import zombie.iso.WorldStreamer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCorpses;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderOcclusion;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.Temperature;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.meta.Meta;
import zombie.modding.ActiveMods;
import zombie.network.BodyDamageSync;
import zombie.network.ChunkChecksum;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PassengerMap;
import zombie.network.ServerGUI;
import zombie.network.ServerOptions;
import zombie.network.WarManager;
import zombie.network.server.AnimEventEmulator;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.AnimalController;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.HutchManager;
import zombie.radio.ZomboidRadio;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.ScriptManager;
import zombie.seams.SeamManager;
import zombie.seating.SeatingManager;
import zombie.spnetwork.SinglePlayerClient;
import zombie.spnetwork.SinglePlayerServer;
import zombie.spriteModel.SpriteModelManager;
import zombie.text.templating.TemplateText;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryManager;
import zombie.ui.ActionProgressBar;
import zombie.ui.FPSGraph;
import zombie.ui.ScreenFader;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.vehicles.EditVehicleState;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleIDMap;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehiclesDB2;
import zombie.worldMap.WorldMap;
import zombie.worldMap.WorldMapVisited;
import zombie.worldMap.editor.WorldMapEditorState;
import zombie.worldMap.network.WorldMapClient;

public final class IngameState extends GameState {
   public static int WaitMul = 20;
   public static IngameState instance;
   public static float draww;
   public static float drawh;
   public static Long GameID = 0L;
   static int last = -1;
   static float xPos;
   static float yPos;
   static float offx;
   static float offy;
   static float zoom;
   static HashMap<String, Integer> ContainerTypes = new HashMap();
   static int nSaveCycle = 1800;
   static boolean bDoChars = false;
   static boolean keySpacePreviousFrame = false;
   public long numberTicks = 0L;
   public boolean Paused = false;
   public float SaveDelay = 0.0F;
   boolean alt = false;
   int insanityScareCount = 5;
   int insanitypic = -1;
   int timesincelastinsanity = 10000000;
   GameState RedirectState = null;
   boolean bDidServerDisconnectState = false;
   boolean fpsKeyDown = false;
   private final ArrayList<Long> debugTimes = new ArrayList();
   private int tickCount = 0;
   private float SadisticMusicDirectorTime;
   public boolean showAnimationViewer = false;
   public boolean showAttachmentEditor = false;
   public boolean showChunkDebugger = false;
   public boolean showSpriteModelEditor = false;
   public boolean showTileGeometryEditor = false;
   public boolean showGlobalObjectDebugger = false;
   public String showVehicleEditor = null;
   public String showWorldMapEditor = null;
   public boolean showSeamEditor = false;
   public static boolean bLoading = false;

   public IngameState() {
      instance = this;
   }

   public static void renderDebugOverhead(IsoCell var0, int var1, int var2, int var3, int var4) {
      Mouse.update();
      int var5 = Mouse.getX();
      int var6 = Mouse.getY();
      var5 -= var3;
      var6 -= var4;
      var5 /= var2;
      var6 /= var2;
      SpriteRenderer.instance.renderi((Texture)null, var3, var4, var2 * var0.getWidthInTiles(), var2 * var0.getHeightInTiles(), 0.7F, 0.7F, 0.7F, 1.0F, (Consumer)null);
      IsoGridSquare var7 = var0.getGridSquare(var5 + var0.ChunkMap[0].getWorldXMinTiles(), var6 + var0.ChunkMap[0].getWorldYMinTiles(), 0);
      int var8;
      int var9;
      if (var7 != null) {
         var8 = 48;
         var9 = 48;
         TextManager.instance.DrawString((double)var9, (double)var8, "SQUARE FLAGS", 1.0, 1.0, 1.0, 1.0);
         var8 += 20;
         var9 += 8;

         int var10;
         for(var10 = 0; var10 < IsoFlagType.MAX.index(); ++var10) {
            if (var7.Is(IsoFlagType.fromIndex(var10))) {
               TextManager.instance.DrawString((double)var9, (double)var8, IsoFlagType.fromIndex(var10).toString(), 0.6, 0.6, 0.8, 1.0);
               var8 += 18;
            }
         }

         byte var11 = 48;
         var8 += 16;
         TextManager.instance.DrawString((double)var11, (double)var8, "SQUARE OBJECT TYPES", 1.0, 1.0, 1.0, 1.0);
         var8 += 20;
         var9 = var11 + 8;

         for(var10 = 0; var10 < 64; ++var10) {
            if (var7.getHasTypes().isSet(var10)) {
               TextManager.instance.DrawString((double)var9, (double)var8, IsoObjectType.fromIndex(var10).toString(), 0.6, 0.6, 0.8, 1.0);
               var8 += 18;
            }
         }
      }

      for(var8 = 0; var8 < var0.getWidthInTiles(); ++var8) {
         for(var9 = 0; var9 < var0.getHeightInTiles(); ++var9) {
            IsoGridSquare var12 = var0.getGridSquare(var8 + var0.ChunkMap[0].getWorldXMinTiles(), var9 + var0.ChunkMap[0].getWorldYMinTiles(), var1);
            if (var12 != null) {
               if (!var12.getProperties().Is(IsoFlagType.solid) && !var12.getProperties().Is(IsoFlagType.solidtrans)) {
                  if (!var12.getProperties().Is(IsoFlagType.exterior)) {
                     SpriteRenderer.instance.renderi((Texture)null, var3 + var8 * var2, var4 + var9 * var2, var2, var2, 0.8F, 0.8F, 0.8F, 1.0F, (Consumer)null);
                  }
               } else {
                  SpriteRenderer.instance.renderi((Texture)null, var3 + var8 * var2, var4 + var9 * var2, var2, var2, 0.5F, 0.5F, 0.5F, 255.0F, (Consumer)null);
               }

               if (var12.Has(IsoObjectType.tree)) {
                  SpriteRenderer.instance.renderi((Texture)null, var3 + var8 * var2, var4 + var9 * var2, var2, var2, 0.4F, 0.8F, 0.4F, 1.0F, (Consumer)null);
               }

               if (var12.getProperties().Is(IsoFlagType.collideN)) {
                  SpriteRenderer.instance.renderi((Texture)null, var3 + var8 * var2, var4 + var9 * var2, var2, 1, 0.2F, 0.2F, 0.2F, 1.0F, (Consumer)null);
               }

               if (var12.getProperties().Is(IsoFlagType.collideW)) {
                  SpriteRenderer.instance.renderi((Texture)null, var3 + var8 * var2, var4 + var9 * var2, 1, var2, 0.2F, 0.2F, 0.2F, 1.0F, (Consumer)null);
               }
            }
         }
      }

   }

   public static float translatePointX(float var0, float var1, float var2, float var3) {
      var0 -= var1;
      var0 *= var2;
      var0 += var3;
      var0 += draww / 2.0F;
      return var0;
   }

   public static float invTranslatePointX(float var0, float var1, float var2, float var3) {
      var0 -= draww / 2.0F;
      var0 -= var3;
      var0 /= var2;
      var0 += var1;
      return var0;
   }

   public static float invTranslatePointY(float var0, float var1, float var2, float var3) {
      var0 -= drawh / 2.0F;
      var0 -= var3;
      var0 /= var2;
      var0 += var1;
      return var0;
   }

   public static float translatePointY(float var0, float var1, float var2, float var3) {
      var0 -= var1;
      var0 *= var2;
      var0 += var3;
      var0 += drawh / 2.0F;
      return var0;
   }

   public static void renderRect(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = translatePointX(var0, xPos, zoom, offx);
      float var9 = translatePointY(var1, yPos, zoom, offy);
      float var10 = translatePointX(var0 + var2, xPos, zoom, offx);
      float var11 = translatePointY(var1 + var3, yPos, zoom, offy);
      var2 = var10 - var8;
      var3 = var11 - var9;
      if (!(var8 >= (float)Core.getInstance().getScreenWidth()) && !(var10 < 0.0F) && !(var9 >= (float)Core.getInstance().getScreenHeight()) && !(var11 < 0.0F)) {
         SpriteRenderer.instance.render((Texture)null, var8, var9, var2, var3, var4, var5, var6, var7, (Consumer)null);
      }
   }

   public static void renderLine(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = translatePointX(var0, xPos, zoom, offx);
      float var9 = translatePointY(var1, yPos, zoom, offy);
      float var10 = translatePointX(var2, xPos, zoom, offx);
      float var11 = translatePointY(var3, yPos, zoom, offy);
      if ((!(var8 >= (float)Core.getInstance().getScreenWidth()) || !(var10 >= (float)Core.getInstance().getScreenWidth())) && (!(var9 >= (float)Core.getInstance().getScreenHeight()) || !(var11 >= (float)Core.getInstance().getScreenHeight())) && (!(var8 < 0.0F) || !(var10 < 0.0F)) && (!(var9 < 0.0F) || !(var11 < 0.0F))) {
         SpriteRenderer.instance.renderline((Texture)null, (int)var8, (int)var9, (int)var10, (int)var11, var4, var5, var6, var7);
      }
   }

   public static void renderDebugOverhead2(IsoCell var0, int var1, float var2, int var3, int var4, float var5, float var6, int var7, int var8) {
      draww = (float)var7;
      drawh = (float)var8;
      xPos = var5;
      yPos = var6;
      offx = (float)var3;
      offy = (float)var4;
      zoom = var2;
      IsoChunkMap var9 = var0.getChunkMap(0);
      float var10 = (float)var9.getWorldXMinTiles();
      float var11 = (float)var9.getWorldYMinTiles();
      float var12 = (float)var9.getWorldXMaxTiles();
      float var13 = (float)var9.getWorldYMaxTiles();
      renderRect(var10, var11, (float)var0.getWidthInTiles(), (float)var0.getWidthInTiles(), 0.7F, 0.7F, 0.7F, 1.0F);

      int var14;
      for(var14 = 0; var14 < var0.getWidthInTiles(); ++var14) {
         for(int var15 = 0; var15 < var0.getHeightInTiles(); ++var15) {
            IsoGridSquare var16 = var0.getGridSquare(var14 + var9.getWorldXMinTiles(), var15 + var9.getWorldYMinTiles(), var1);
            float var17 = (float)var14 + var10;
            float var18 = (float)var15 + var11;
            if (var16 != null) {
               if (!var16.getProperties().Is(IsoFlagType.solid) && !var16.getProperties().Is(IsoFlagType.solidtrans)) {
                  if (!var16.getProperties().Is(IsoFlagType.exterior)) {
                     renderRect(var17, var18, 1.0F, 1.0F, 0.8F, 0.8F, 0.8F, 1.0F);
                  }
               } else {
                  renderRect(var17, var18, 1.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.0F);
               }

               if (var16.Has(IsoObjectType.tree)) {
                  renderRect(var17, var18, 1.0F, 1.0F, 0.4F, 0.8F, 0.4F, 1.0F);
               }

               if (var16.getProperties().Is(IsoFlagType.collideN)) {
                  renderRect(var17, var18, 1.0F, 0.2F, 0.2F, 0.2F, 0.2F, 1.0F);
               }

               if (var16.getProperties().Is(IsoFlagType.collideW)) {
                  renderRect(var17, var18, 0.2F, 1.0F, 0.2F, 0.2F, 0.2F, 1.0F);
               }
            }
         }
      }

      var14 = IsoCell.CellSizeInSquares;
      IsoMetaGrid var29 = IsoWorld.instance.MetaGrid;
      renderRect((float)(var29.minX * var14), (float)(var29.minY * var14), (float)(var29.getWidth() * var14), (float)(var29.getHeight() * var14), 1.0F, 1.0F, 1.0F, 0.05F);
      int var30;
      if ((double)var2 > 0.1) {
         for(var30 = var29.minY; var30 <= var29.maxY; ++var30) {
            renderLine((float)(var29.minX * var14), (float)(var30 * var14), (float)((var29.maxX + 1) * var14), (float)(var30 * var14), 1.0F, 1.0F, 1.0F, 0.15F);
         }

         for(var30 = var29.minX; var30 <= var29.maxX; ++var30) {
            renderLine((float)(var30 * var14), (float)(var29.minY * var14), (float)(var30 * var14), (float)((var29.maxY + 1) * var14), 1.0F, 1.0F, 1.0F, 0.15F);
         }
      }

      for(var30 = 0; var30 < var29.getWidth(); ++var30) {
         for(int var31 = 0; var31 < var29.getHeight(); ++var31) {
            if (var29.hasCell(var30, var31)) {
               LotHeader var32 = var29.getCell(var30, var31).info;
               if (var32 == null) {
                  renderRect((float)((var29.minX + var30) * var14 + 1), (float)((var29.minY + var31) * var14 + 1), (float)(var14 - 2), (float)(var14 - 2), 0.2F, 0.0F, 0.0F, 0.3F);
               } else {
                  int var19;
                  BuildingDef var20;
                  if (!((double)var2 > 0.8)) {
                     for(var19 = 0; var19 < var32.Buildings.size(); ++var19) {
                        var20 = (BuildingDef)var32.Buildings.get(var19);
                        if (var20.bAlarmed) {
                           renderRect((float)var20.getX(), (float)var20.getY(), (float)var20.getW(), (float)var20.getH(), 0.8F, 0.8F, 0.5F, 0.3F);
                        } else {
                           renderRect((float)var20.getX(), (float)var20.getY(), (float)var20.getW(), (float)var20.getH(), 0.5F, 0.5F, 0.8F, 0.3F);
                        }
                     }
                  } else {
                     for(var19 = 0; var19 < var32.Buildings.size(); ++var19) {
                        var20 = (BuildingDef)var32.Buildings.get(var19);

                        for(int var21 = 0; var21 < var20.rooms.size(); ++var21) {
                           RoomDef var22 = (RoomDef)var20.rooms.get(var21);
                           if (var22.level == var1) {
                              float var23 = 0.5F;
                              float var24 = 0.5F;
                              float var25 = 0.8F;
                              float var26 = 0.3F;
                              if (var20.bAlarmed) {
                                 var23 = 0.8F;
                                 var24 = 0.8F;
                                 var25 = 0.5F;
                              }

                              for(int var27 = 0; var27 < var22.rects.size(); ++var27) {
                                 RoomDef.RoomRect var28 = (RoomDef.RoomRect)var22.rects.get(var27);
                                 renderRect((float)var28.getX(), (float)var28.getY(), (float)var28.getW(), (float)var28.getH(), var23, var24, var25, var26);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public static void copyWorld(String var0, String var1) {
      String var10000 = ZomboidFileSystem.instance.getGameModeCacheDir();
      String var2 = var10000 + File.separator + var0 + File.separator;
      var2 = var2.replace("/", File.separator);
      var2 = var2.replace("\\", File.separator);
      String var3 = var2.substring(0, var2.lastIndexOf(File.separator));
      var3 = var3.replace("\\", "/");
      File var4 = new File(var3);
      var10000 = ZomboidFileSystem.instance.getGameModeCacheDir();
      var2 = var10000 + File.separator + var1 + File.separator;
      var2 = var2.replace("/", File.separator);
      var2 = var2.replace("\\", File.separator);
      String var5 = var2.substring(0, var2.lastIndexOf(File.separator));
      var5 = var5.replace("\\", "/");
      File var6 = new File(var5);

      try {
         copyDirectory(var4, var6);
      } catch (IOException var8) {
         var8.printStackTrace();
      }

   }

   public static void copyDirectory(File var0, File var1) throws IOException {
      if (var0.isDirectory()) {
         if (!var1.exists()) {
            var1.mkdir();
         }

         String[] var2 = var0.list();
         boolean var3 = GameLoadingState.convertingFileMax == -1;
         if (var3) {
            GameLoadingState.convertingFileMax = var2.length;
         }

         for(int var4 = 0; var4 < var2.length; ++var4) {
            if (var3) {
               ++GameLoadingState.convertingFileCount;
            }

            copyDirectory(new File(var0, var2[var4]), new File(var1, var2[var4]));
         }
      } else {
         FileInputStream var5 = new FileInputStream(var0);
         FileOutputStream var6 = new FileOutputStream(var1);
         var6.getChannel().transferFrom(var5.getChannel(), 0L, var0.length());
         var5.close();
         var6.close();
      }

   }

   public static void createWorld(String var0) {
      var0 = var0.replace(" ", "_").trim();
      String var10000 = ZomboidFileSystem.instance.getGameModeCacheDir();
      String var1 = var10000 + File.separator + var0 + File.separator;
      var1 = var1.replace("/", File.separator);
      var1 = var1.replace("\\", File.separator);
      String var2 = var1.substring(0, var1.lastIndexOf(File.separator));
      var2 = var2.replace("\\", "/");
      File var3 = new File(var2);
      if (!var3.exists()) {
         var3.mkdirs();
      }

      Core.GameSaveWorld = var0;
   }

   public void debugFullyStreamedIn(int var1, int var2) {
      IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, 0);
      if (var3 != null) {
         if (var3.getBuilding() != null) {
            BuildingDef var4 = var3.getBuilding().getDef();
            if (var4 != null) {
               boolean var5 = var4.isFullyStreamedIn();

               for(int var6 = 0; var6 < var4.overlappedChunks.size(); var6 += 2) {
                  short var7 = var4.overlappedChunks.get(var6);
                  short var8 = var4.overlappedChunks.get(var6 + 1);
                  if (var5) {
                     renderRect((float)(var7 * 8), (float)(var8 * 8), 8.0F, 8.0F, 0.0F, 1.0F, 0.0F, 0.5F);
                  } else {
                     renderRect((float)(var7 * 8), (float)(var8 * 8), 8.0F, 8.0F, 1.0F, 0.0F, 0.0F, 0.5F);
                  }
               }

            }
         }
      }
   }

   public void UpdateStuff() {
      GameClient.bIngame = true;
      this.SaveDelay += GameTime.instance.getMultiplier();
      if (this.SaveDelay / 60.0F > 30.0F) {
         this.SaveDelay = 0.0F;
      }

      GameTime.instance.LastLastTimeOfDay = GameTime.instance.getLastTimeOfDay();
      GameTime.instance.setLastTimeOfDay(GameTime.getInstance().getTimeOfDay());
      boolean var1 = false;
      if (!GameServer.bServer && IsoPlayer.getInstance() != null) {
         var1 = IsoPlayer.allPlayersAsleep();
      }

      GameTime.getInstance().update(var1 && UIManager.getFadeAlpha() == 1.0);
      GameProfiler var10000;
      if (!this.Paused) {
         var10000 = GameProfiler.getInstance();
         ScriptManager var10002 = ScriptManager.instance;
         Objects.requireNonNull(var10002);
         var10000.invokeAndMeasure("ScriptManager.update", var10002::update);
      }

      if (!this.Paused) {
         try {
            var10000 = GameProfiler.getInstance();
            WorldSoundManager var12 = WorldSoundManager.instance;
            Objects.requireNonNull(var12);
            var10000.invokeAndMeasure("WorldSoundManager.update", var12::update);
         } catch (Exception var11) {
            ExceptionLogger.logException(var11);
         }

         try {
            GameProfiler.getInstance().invokeAndMeasure("IsoFireManager.Update", IsoFireManager::Update);
         } catch (Exception var10) {
            ExceptionLogger.logException(var10);
         }

         try {
            GameProfiler.getInstance().invokeAndMeasure("RainManager.Update", RainManager::Update);
         } catch (Exception var9) {
            ExceptionLogger.logException(var9);
         }

         var10000 = GameProfiler.getInstance();
         Meta var13 = Meta.instance;
         Objects.requireNonNull(var13);
         var10000.invokeAndMeasure("Meta.update", var13::update);

         try {
            var10000 = GameProfiler.getInstance();
            VirtualZombieManager var14 = VirtualZombieManager.instance;
            Objects.requireNonNull(var14);
            var10000.invokeAndMeasure("VirtualZombieManager.update", var14::update);
            var10000 = GameProfiler.getInstance();
            MapCollisionData var15 = MapCollisionData.instance;
            Objects.requireNonNull(var15);
            var10000.invokeAndMeasure("MapCollisionData.updateMain", var15::updateMain);
            var10000 = GameProfiler.getInstance();
            ZombiePopulationManager var16 = ZombiePopulationManager.instance;
            Objects.requireNonNull(var16);
            var10000.invokeAndMeasure("ZombiePopulationManager.updateMain", var16::updateMain);
            var10000 = GameProfiler.getInstance();
            PathfindNative var17 = PathfindNative.instance;
            Objects.requireNonNull(var17);
            var10000.invokeAndMeasure("PathfindNative.checkUseNativeCode", var17::checkUseNativeCode);
            if (PathfindNative.USE_NATIVE_CODE) {
               var10000 = GameProfiler.getInstance();
               var17 = PathfindNative.instance;
               Objects.requireNonNull(var17);
               var10000.invokeAndMeasure("PathfindNative.updateMain", var17::updateMain);
            } else {
               var10000 = GameProfiler.getInstance();
               PolygonalMap2 var18 = PolygonalMap2.instance;
               Objects.requireNonNull(var18);
               var10000.invokeAndMeasure("PolygonalMap2.updateMain", var18::updateMain);
            }
         } catch (Throwable var8) {
            ExceptionLogger.logException(var8);
         }

         try {
            GameProfiler.getInstance().invokeAndMeasure("LootRespawn.update", LootRespawn::update);
         } catch (Exception var7) {
            ExceptionLogger.logException(var7);
         }

         if (GameServer.bServer) {
            try {
               AmbientStreamManager.instance.update();
            } catch (Exception var6) {
               ExceptionLogger.logException(var6);
            }

            try {
               AnimEventEmulator.getInstance().update();
            } catch (Exception var5) {
               ExceptionLogger.logException(var5);
            }
         }

         if (GameClient.bClient) {
            try {
               BodyDamageSync.instance.update();
            } catch (Exception var4) {
               ExceptionLogger.logException(var4);
            }
         }

         if (!GameServer.bServer) {
            try {
               GameProfiler.getInstance().invokeAndMeasure("ItemSoundManager.update", ItemSoundManager::update);
               var10000 = GameProfiler.getInstance();
               FliesSound var19 = FliesSound.instance;
               Objects.requireNonNull(var19);
               var10000.invokeAndMeasure("FliesSound.update", var19::update);
               GameProfiler.getInstance().invokeAndMeasure("CorpseFlies.update", CorpseFlies::update);
               GameProfiler.getInstance().invokeAndMeasure("WorldMapVisited.update", WorldMapVisited::update);
            } catch (Exception var3) {
               ExceptionLogger.logException(var3);
            }
         }

         var10000 = GameProfiler.getInstance();
         SearchMode var20 = SearchMode.getInstance();
         Objects.requireNonNull(var20);
         var10000.invokeAndMeasure("SearchMode.update", var20::update);
         var10000 = GameProfiler.getInstance();
         RenderSettings var21 = RenderSettings.getInstance();
         Objects.requireNonNull(var21);
         var10000.invokeAndMeasure("RenderSettings.update", var21::update);
      }

   }

   private void TickMusicDirector() {
      if (!this.Paused && !GameServer.bServer) {
         LuaManager.call("SadisticMusicDirectorTick", (Object)null);
      }

   }

   public void enter() {
      bLoading = true;
      UIManager.useUIFBO = Core.getInstance().supportsFBO() && Core.getInstance().getOptionUIFBO();
      if (!Core.getInstance().getUseShaders()) {
         SceneShaderStore.WeatherShader = null;
      }

      GameSounds.fix3DListenerPosition(false);
      IsoPlayer.getInstance().updateUsername();
      IsoPlayer.getInstance().setSceneCulled(false);
      IsoPlayer.getInstance().getInventory().addItemsToProcessItems();
      GameID = (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      GameID = GameID + (long)Rand.Next(10000000);
      ZombieSpawnRecorder.instance.init();
      if (!GameServer.bServer) {
         IsoWorld.instance.CurrentCell.ChunkMap[0].processAllLoadGridSquare();
         IsoWorld.instance.CurrentCell.ChunkMap[0].update();
         if (!GameClient.bClient) {
            LightingThread.instance.GameLoadingUpdate();
         }
      }

      bLoading = false;

      try {
         MapCollisionData.instance.startGame();
      } catch (Throwable var3) {
         ExceptionLogger.logException(var3);
      }

      IsoWorld.instance.CurrentCell.putInVehicle(IsoPlayer.getInstance());
      SoundManager.instance.setMusicState("Tutorial".equals(Core.GameMode) ? "Tutorial" : "InGame");
      ClimateManager.getInstance().update();
      AmbientStreamManager.instance.checkHaveElectricity();
      LuaEventManager.triggerEvent("OnGameStart");
      LuaEventManager.triggerEvent("OnLoad");
      if (GameClient.bClient) {
         GameClient.instance.sendPlayerConnect(IsoPlayer.getInstance());
         DebugLog.log("Waiting for player-connect response from server");

         for(; IsoPlayer.getInstance().OnlineID == -1; GameClient.instance.update()) {
            try {
               Thread.sleep(10L);
            } catch (InterruptedException var2) {
               var2.printStackTrace();
            }
         }

         ClimateManager.getInstance().update();
         LightingThread.instance.GameLoadingUpdate();
      }

      if (GameClient.bClient && SteamUtils.isSteamModeEnabled()) {
         SteamFriends.UpdateRichPresenceConnectionInfo("In game", "+connect " + GameClient.ip + ":" + GameClient.port);
      }

      FBORenderOcclusion.getInstance().init();
      UIManager.setbFadeBeforeUI(false);
      UIManager.FadeIn(0.3499999940395355);
   }

   public void exit() {
      DebugType.ExitDebug.debugln("IngameState.exit 1");
      if (SteamUtils.isSteamModeEnabled()) {
         SteamFriends.UpdateRichPresenceConnectionInfo("", "");
      }

      UIManager.useUIFBO = false;
      if (FPSGraph.instance != null) {
         FPSGraph.instance.setVisible(false);
      }

      UIManager.updateBeforeFadeOut();
      SoundManager.instance.setMusicState("MainMenu");
      ScreenFader var1 = new ScreenFader();
      var1.startFadeToBlack();
      boolean var2 = UIManager.useUIFBO;
      UIManager.useUIFBO = false;
      DebugType.ExitDebug.debugln("IngameState.exit 2");

      int var4;
      while(var1.isFading()) {
         boolean var3 = true;

         for(var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
            if (IsoPlayer.players[var4] != null) {
               IsoPlayer.setInstance(IsoPlayer.players[var4]);
               IsoCamera.setCameraCharacter(IsoPlayer.players[var4]);
               IsoSprite.globalOffsetX = -1.0F;
               Core.getInstance().StartFrame(var4, var3);
               IsoCamera.frameState.set(var4);
               IsoWorld.instance.render();
               Core.getInstance().EndFrame(var4);
               var3 = false;
            }
         }

         Core.getInstance().RenderOffScreenBuffer();
         Core.getInstance().StartFrameUI();
         UIManager.render();
         var1.update();
         var1.render();
         Core.getInstance().EndFrameUI();
         DebugType.ExitDebug.debugln("IngameState.exit 3 (alpha=" + var1.getAlpha() + ")");
         if (var1.isFading()) {
            try {
               Thread.sleep(33L);
            } catch (Exception var11) {
            }
         }
      }

      UIManager.useUIFBO = var2;
      DebugType.ExitDebug.debugln("IngameState.exit 4");
      RenderThread.setWaitForRenderState(false);
      SpriteRenderer.instance.notifyRenderStateQueue();

      while(WorldStreamer.instance.isBusy()) {
         try {
            Thread.sleep(1L);
         } catch (InterruptedException var10) {
            var10.printStackTrace();
         }
      }

      DebugType.ExitDebug.debugln("IngameState.exit 5");
      WorldStreamer.instance.stop();
      LightingThread.instance.stop();
      MapCollisionData.instance.stop();
      AnimalPopulationManager.getInstance().stop();
      ZombiePopulationManager.instance.stop();
      if (PathfindNative.USE_NATIVE_CODE) {
         PathfindNative.instance.stop();
      } else {
         PolygonalMap2.instance.stop();
      }

      AnimalInstanceManager.getInstance().stop();
      DebugType.ExitDebug.debugln("IngameState.exit 6");

      int var5;
      for(int var12 = 0; var12 < IsoWorld.instance.CurrentCell.ChunkMap.length; ++var12) {
         IsoChunkMap var14 = IsoWorld.instance.CurrentCell.ChunkMap[var12];

         for(var5 = 0; var5 < IsoChunkMap.ChunkGridWidth * IsoChunkMap.ChunkGridWidth; ++var5) {
            IsoChunk var6 = var14.getChunk(var5 % IsoChunkMap.ChunkGridWidth, var5 / IsoChunkMap.ChunkGridWidth);
            if (var6 != null && var6.refs.contains(var14)) {
               var6.refs.remove(var14);
               if (var6.refs.isEmpty()) {
                  var6.removeFromWorld();
                  var6.doReuseGridsquares();
               }
            }
         }
      }

      ModelManager.instance.Reset();
      IsoPlayer.Reset();
      ZombieSpawnRecorder.instance.quit();
      DebugType.ExitDebug.debugln("IngameState.exit 7");
      IsoPlayer.numPlayers = 1;
      Core.getInstance().OffscreenBuffer.destroy();
      WeatherFxMask.destroy();
      IsoRegions.reset();
      Temperature.reset();
      WorldMarkers.instance.reset();
      IsoMarkers.instance.reset();
      SearchMode.reset();
      ZomboidRadio.getInstance().Reset();
      IsoWaveSignal.Reset();
      GameEntityManager.Reset();
      SpriteConfigManager.Reset();
      Fluid.Reset();
      Energy.Reset();
      CraftRecipeManager.Reset();
      ErosionGlobals.Reset();
      IsoGenerator.Reset();
      StashSystem.Reset();
      LootRespawn.Reset();
      VehicleCache.Reset();
      VehicleIDMap.instance.Reset();
      IsoWorld.instance.KillCell();
      ItemSoundManager.Reset();
      IsoChunk.Reset();
      ChunkChecksum.Reset();
      ClientServerMap.Reset();
      SinglePlayerClient.Reset();
      SinglePlayerServer.Reset();
      PassengerMap.Reset();
      DeadBodyAtlas.instance.Reset();
      WorldItemAtlas.instance.Reset();
      FBORenderCorpses.getInstance().Reset();
      FBORenderItems.getInstance().Reset();
      FBORenderChunkManager.instance.Reset();
      CorpseFlies.Reset();
      if (PlayerDB.isAvailable()) {
         PlayerDB.getInstance().close();
      }

      VehiclesDB2.instance.Reset();
      WorldMap.Reset();
      AnimalDefinitions.Reset();
      AnimalZones.Reset();
      MigrationGroupDefinitions.Reset();
      DesignationZone.Reset();
      DesignationZoneAnimal.Reset();
      HutchManager.getInstance().clear();
      if (GameWindow.bLoadedAsClient) {
         WorldMapClient.instance.Reset();
      }

      WorldStreamer.instance = new WorldStreamer();
      WorldSimulation.instance.destroy();
      WorldSimulation.instance = new WorldSimulation();
      DebugType.ExitDebug.debugln("IngameState.exit 8");
      VirtualZombieManager.instance.Reset();
      VirtualZombieManager.instance = new VirtualZombieManager();
      ReanimatedPlayers.instance = new ReanimatedPlayers();
      ScriptManager.instance.Reset();
      GameSounds.Reset();
      VehicleType.Reset();
      TemplateText.Reset();
      CombatManager.getInstance().Reset();
      ClimateMoon.getInstance().Reset();
      ClimateManager.getInstance().Reset();
      LuaEventManager.Reset();
      MapObjects.Reset();
      CGlobalObjects.Reset();
      SGlobalObjects.Reset();
      AmbientStreamManager.instance.stop();
      SoundManager.instance.stop();
      IsoPlayer.setInstance((IsoPlayer)null);
      IsoCamera.clearCameraCharacter();
      TutorialManager.instance.StealControl = false;
      UIManager.init();
      ScriptManager.instance.Reset();
      ClothingDecals.Reset();
      BeardStyles.Reset();
      HairStyles.Reset();
      OutfitManager.Reset();
      AnimationSet.Reset();
      GameSounds.Reset();
      SurvivorFactory.Reset();
      ProfessionFactory.Reset();
      TraitFactory.Reset();
      ChooseGameInfo.Reset();
      AttachedLocations.Reset();
      BodyLocations.Reset();
      ContainerOverlays.instance.Reset();
      BentFences.getInstance().Reset();
      BrokenFences.getInstance().Reset();
      TileOverlays.instance.Reset();
      LuaHookManager.Reset();
      CustomPerks.Reset();
      PerkFactory.Reset();
      CustomSandboxOptions.Reset();
      SandboxOptions.Reset();
      LuaManager.init();
      JoypadManager.instance.Reset();
      GameKeyboard.doLuaKeyPressed = true;
      GameWindow.ActivatedJoyPad = null;
      GameWindow.OkToSaveOnExit = false;
      GameWindow.bLoadedAsClient = false;
      Core.bLastStand = false;
      Core.ChallengeID = null;
      Core.bTutorial = false;
      Core.getInstance().setChallenge(false);
      Core.getInstance().setForceSnow(false);
      Core.getInstance().setZombieGroupSound(true);
      Core.getInstance().setFlashIsoCursor(false);
      SystemDisabler.Reset();
      Texture.nullTextures.clear();
      SpriteModelManager.getInstance().Reset();
      TileGeometryManager.getInstance().Reset();
      TileDepthTextureManager.getInstance().Reset();
      SeamManager.getInstance().Reset();
      SeatingManager.getInstance().Reset();
      DebugType.ExitDebug.debugln("IngameState.exit 9");
      ZomboidFileSystem.instance.Reset();
      WarManager.clear();
      if (!Core.SoundDisabled && !GameServer.bServer) {
         javafmod.FMOD_System_Update();
      }

      try {
         ZomboidFileSystem.instance.init();
      } catch (IOException var9) {
         ExceptionLogger.logException(var9);
      }

      Core.OptionModsEnabled = true;
      DebugType.ExitDebug.debugln("IngameState.exit 10");
      ZomboidFileSystem.instance.loadMods("default");
      ZomboidFileSystem.instance.loadModPackFiles();
      Languages.instance.init();
      Translator.loadFiles();
      DebugType.ExitDebug.debugln("IngameState.exit 11");
      CustomPerks.instance.init();
      CustomPerks.instance.initLua();
      CustomSandboxOptions.instance.init();
      CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);

      try {
         ScriptManager.instance.Load();
      } catch (Exception var8) {
         ExceptionLogger.logException(var8);
      }

      SpriteModelManager.getInstance().init();
      ModelManager.instance.initAnimationMeshes(true);
      ModelManager.instance.loadModAnimations();
      ClothingDecals.init();
      BeardStyles.init();
      HairStyles.init();
      OutfitManager.init();
      VoiceStyles.init();
      TileGeometryManager.getInstance().init();
      TileDepthTextureAssignmentManager.getInstance().init();
      TileDepthTextureManager.getInstance().init();
      SeamManager.getInstance().init();
      SeatingManager.getInstance().init();
      DebugType.ExitDebug.debugln("IngameState.exit 12");

      try {
         TextManager.instance.Init();
         LuaManager.LoadDirBase();
      } catch (Exception var7) {
         ExceptionLogger.logException(var7);
      }

      ZomboidGlobals.Load();
      DebugType.ExitDebug.debugln("IngameState.exit 13");
      LuaEventManager.triggerEvent("OnGameBoot");
      SoundManager.instance.resumeSoundAndMusic();
      IsoPlayer[] var13 = IsoPlayer.players;
      var4 = var13.length;

      for(var5 = 0; var5 < var4; ++var5) {
         IsoPlayer var15 = var13[var5];
         if (var15 != null) {
            var15.dirtyRecalcGridStack = true;
         }
      }

      RenderThread.setWaitForRenderState(true);
      DebugType.ExitDebug.debugln("IngameState.exit 14");
   }

   public void yield() {
      SoundManager.instance.setMusicState("PauseMenu");
   }

   public GameState redirectState() {
      if (this.RedirectState != null) {
         GameState var1 = this.RedirectState;
         this.RedirectState = null;
         return var1;
      } else {
         return new MainScreenState();
      }
   }

   public void reenter() {
      SoundManager.instance.setMusicState("InGame");
   }

   public void renderframetext(int var1) {
      IngameState.s_performance.renderFrameText.invokeAndMeasure(this, var1, IngameState::renderFrameTextInternal);
   }

   private void renderFrameTextInternal(int var1) {
      IndieGL.disableAlphaTest();
      IndieGL.disableDepthTest();
      ArrayList var2 = UIManager.getUI();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         UIElementInterface var4 = (UIElementInterface)var2.get(var3);
         if (!(var4 instanceof ActionProgressBar) && var4.isVisible() && var4.isFollowGameWorld() && (var4.getRenderThisPlayerOnly() == -1 || var4.getRenderThisPlayerOnly() == var1)) {
            var4.render();
         }
      }

      ActionProgressBar var6 = UIManager.getProgressBar((double)var1);
      if (var6 != null && var6.isVisible()) {
         var6.render();
      }

      WorldMarkers.instance.render();
      IsoMarkers.instance.render();
      TextDrawObject.RenderBatch(var1);
      ChatElement.RenderBatch(var1);
      if (DebugOptions.instance.Character.Debug.Render.FMODRoomType.getValue()) {
         ParameterRoomTypeEx.renderRoomTones();
      }

      try {
         Core.getInstance().EndFrameText(var1);
      } catch (Exception var5) {
      }

   }

   public void renderframe(int var1) {
      IngameState.s_performance.renderFrame.invokeAndMeasure(this, var1, IngameState::renderFrameInternal);
   }

   private void renderFrameInternal(int var1) {
      if (IsoPlayer.getInstance() == null) {
         IsoPlayer.setInstance(IsoPlayer.players[0]);
         IsoCamera.setCameraCharacter(IsoPlayer.getInstance());
      }

      RenderSettings.getInstance().applyRenderSettings(var1);
      ActionProgressBar var2 = UIManager.getProgressBar((double)var1);
      if (var2 != null) {
         var2.update(var1);
      }

      IndieGL.disableAlphaTest();
      IndieGL.disableDepthTest();
      if (IsoPlayer.getInstance() != null && !IsoPlayer.getInstance().isAsleep() || UIManager.getFadeAlpha((double)var1) < 1.0F) {
         ModelOutlines.instance.startFrameMain(var1);
         IsoWorld.instance.render();
         ModelOutlines.instance.endFrameMain(var1);
         RenderSettings.getInstance().legacyPostRender(var1);
         LuaEventManager.triggerEvent("OnPostRender");
      }

      LineDrawer.clear();
      if (Core.bDebug && GameKeyboard.isKeyPressed("ToggleAnimationText")) {
         DebugOptions.instance.Animation.Debug.setValue(!DebugOptions.instance.Animation.Debug.getValue());
      }

      try {
         Core.getInstance().EndFrame(var1);
      } catch (Exception var4) {
      }

   }

   public void renderframeui() {
      IngameState.s_performance.renderFrameUI.invokeAndMeasure(this, IngameState::renderFrameUI);
   }

   private void renderFrameUI() {
      if (Core.getInstance().StartFrameUI()) {
         TextManager.instance.DrawTextFromGameWorld();
         SkyBox.getInstance().draw();
         GameProfiler.getInstance().invokeAndMeasure("UIManager.render", UIManager::render);
         ZomboidRadio.getInstance().render();
         if (Core.bDebug && IsoPlayer.getInstance() != null && IsoPlayer.getInstance().isGhostMode()) {
            IsoWorld.instance.CurrentCell.ChunkMap[0].drawDebugChunkMap();
         }

         DeadBodyAtlas.instance.renderUI();
         WorldItemAtlas.instance.renderUI();
         if (Core.bDebug) {
            if (GameKeyboard.isKeyDown("Display FPS")) {
               if (!this.fpsKeyDown) {
                  this.fpsKeyDown = true;
                  if (FPSGraph.instance == null) {
                     FPSGraph.instance = new FPSGraph();
                  }

                  FPSGraph.instance.setVisible(!FPSGraph.instance.isVisible());
               }
            } else {
               this.fpsKeyDown = false;
            }

            if (FPSGraph.instance != null) {
               FPSGraph.instance.render();
            }
         }

         if (!GameServer.bServer) {
            for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
               IsoPlayer var2 = IsoPlayer.players[var1];
               if (var2 != null && !var2.isDead() && var2.isAsleep()) {
                  float var3 = GameClient.bFastForward ? GameTime.getInstance().ServerTimeOfDay : GameTime.getInstance().getTimeOfDay();
                  LuaEventManager.triggerEvent("OnSleepingTick", BoxedStaticValues.toDouble((double)var1), BoxedStaticValues.toDouble((double)var3));
               }
            }
         }

         ActiveMods.renderUI();
         JoypadManager.instance.renderUI();
      }

      if (Core.bDebug && DebugOptions.instance.Animation.AnimRenderPicker.getValue() && IsoPlayer.players[0] != null) {
         IsoPlayer.players[0].advancedAnimator.render();
      }

      if (Core.bDebug) {
         ModelOutlines.instance.renderDebug();
      }

      Core.getInstance().EndFrameUI();
   }

   public void render() {
      IngameState.s_performance.render.invokeAndMeasure(this, IngameState::renderInternal);
   }

   private void renderInternal() {
      boolean var1 = true;

      int var2;
      for(var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         if (IsoPlayer.players[var2] == null) {
            if (var2 == 0) {
               SpriteRenderer.instance.prePopulating();
            }
         } else {
            IsoPlayer.setInstance(IsoPlayer.players[var2]);
            IsoCamera.setCameraCharacter(IsoPlayer.players[var2]);
            Core.getInstance().StartFrame(var2, var1);
            IsoCamera.frameState.set(var2);
            var1 = false;
            IsoSprite.globalOffsetX = -1.0F;
            this.renderframe(var2);
         }
      }

      if (PerformanceSettings.FBORenderChunk) {
         FBORenderObjectHighlight.getInstance().clearHighlightOnceFlag();
      }

      if (DebugOptions.instance.OffscreenBuffer.Render.getValue()) {
         Core.getInstance().RenderOffScreenBuffer();
      }

      for(var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         if (IsoPlayer.players[var2] != null) {
            IsoPlayer.setInstance(IsoPlayer.players[var2]);
            IsoCamera.setCameraCharacter(IsoPlayer.players[var2]);
            IsoCamera.frameState.set(var2);
            Core.getInstance().StartFrameText(var2);
            this.renderframetext(var2);
         }
      }

      UIManager.resize();
      this.renderframeui();
   }

   public GameStateMachine.StateAction update() {
      GameStateMachine.StateAction var1;
      try {
         IngameState.s_performance.update.start();
         var1 = this.updateInternal();
      } finally {
         IngameState.s_performance.update.end();
      }

      return var1;
   }

   private GameStateMachine.StateAction updateInternal() {
      ++this.tickCount;
      int var1;
      if (this.tickCount < 60) {
         for(var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            if (IsoPlayer.players[var1] != null) {
               IsoPlayer.players[var1].dirtyRecalcGridStackTime = 20.0F;
            }
         }
      }

      LuaEventManager.triggerEvent("OnTickEvenPaused", BoxedStaticValues.toDouble((double)this.numberTicks));
      DebugFileWatcher.instance.update();
      AdvancedAnimator.checkModifiedFiles();
      if (Core.bDebug) {
         this.debugTimes.clear();
         this.debugTimes.add(System.nanoTime());
      }

      if (Core.bExiting) {
         DebugType.ExitDebug.debugln("IngameState Exiting...");
         DebugType.ExitDebug.debugln("IngameState.updateInternal 1");
         Core.bExiting = false;
         if (GameClient.bClient) {
            for(var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
               IsoPlayer var17 = IsoPlayer.players[var1];
               if (var17 != null) {
                  ClientPlayerDB.getInstance().clientSendNetworkPlayerInt(var17);
               }
            }

            try {
               Thread.sleep(500L);
            } catch (InterruptedException var6) {
            }

            WorldStreamer.instance.stop();
            GameClient.instance.doDisconnect("exiting");
         }

         DebugType.ExitDebug.debugln("IngameState.updateInternal 2");
         if (PlayerDB.isAllow()) {
            PlayerDB.getInstance().saveLocalPlayersForce();
            PlayerDB.getInstance().m_canSavePlayers = false;
         }

         if (ClientPlayerDB.isAllow()) {
            ClientPlayerDB.getInstance().canSavePlayers = false;
         }

         try {
            GameWindow.save(true);
         } catch (Throwable var5) {
            ExceptionLogger.logException(var5);
         }

         DebugType.ExitDebug.debugln("IngameState.updateInternal 3");

         try {
            LuaEventManager.triggerEvent("OnPostSave");
         } catch (Exception var4) {
            ExceptionLogger.logException(var4);
         }

         if (ClientPlayerDB.isAllow()) {
            ClientPlayerDB.getInstance().close();
         }

         DebugType.ExitDebug.debugln("IngameState Exiting done.");
         return GameStateMachine.StateAction.Continue;
      } else if (GameWindow.bServerDisconnected) {
         TutorialManager.instance.StealControl = true;
         if (!this.bDidServerDisconnectState) {
            this.bDidServerDisconnectState = true;
            this.RedirectState = new ServerDisconnectState();
            return GameStateMachine.StateAction.Yield;
         } else {
            GameClient.connection = null;
            GameClient.instance.bConnected = false;
            GameClient.bClient = false;
            GameWindow.bServerDisconnected = false;
            ConnectionManager.getInstance().process();
            return GameStateMachine.StateAction.Continue;
         }
      } else {
         if (Core.bDebug) {
            label434: {
               if (this.showGlobalObjectDebugger || GameKeyboard.isKeyPressed(60) && GameKeyboard.isKeyDown(29)) {
                  this.showGlobalObjectDebugger = false;
                  DebugLog.General.debugln("Activating DebugGlobalObjectState.");
                  this.RedirectState = new DebugGlobalObjectState();
                  return GameStateMachine.StateAction.Yield;
               }

               if (!this.showChunkDebugger && !GameKeyboard.isKeyPressed(60)) {
                  if (!this.showAnimationViewer && (!GameKeyboard.isKeyPressed(65) || !GameKeyboard.isKeyDown(29))) {
                     if (!this.showAttachmentEditor && (!GameKeyboard.isKeyPressed(65) || !GameKeyboard.isKeyDown(42))) {
                        if (this.showVehicleEditor == null && !GameKeyboard.isKeyPressed(65)) {
                           if (this.showSpriteModelEditor || GameKeyboard.isKeyPressed(66) && GameKeyboard.isKeyDown(29)) {
                              this.showSpriteModelEditor = false;
                              DebugLog.General.debugln("Activating SpriteModelEditorState.");
                              SpriteModelEditorState var21 = SpriteModelEditorState.checkInstance();
                              this.RedirectState = var21;
                              return GameStateMachine.StateAction.Yield;
                           }

                           if (this.showTileGeometryEditor || GameKeyboard.isKeyPressed(66) && GameKeyboard.isKeyDown(42)) {
                              this.showTileGeometryEditor = false;
                              DebugLog.General.debugln("Activating TileGeometryState.");
                              TileGeometryState var20 = TileGeometryState.checkInstance();
                              this.RedirectState = var20;
                              return GameStateMachine.StateAction.Yield;
                           }

                           if (this.showWorldMapEditor == null && !GameKeyboard.isKeyPressed(66)) {
                              if (!this.showSeamEditor && !GameKeyboard.isKeyPressed(67)) {
                                 break label434;
                              }

                              this.showSeamEditor = false;
                              DebugLog.General.debugln("Activating SeamEditorState.");
                              SeamEditorState var18 = SeamEditorState.checkInstance();
                              this.RedirectState = var18;
                              return GameStateMachine.StateAction.Yield;
                           }

                           WorldMapEditorState var15 = WorldMapEditorState.checkInstance();
                           this.showWorldMapEditor = null;
                           this.RedirectState = var15;
                           return GameStateMachine.StateAction.Yield;
                        }

                        DebugLog.General.debugln("Activating EditVehicleState.");
                        EditVehicleState var14 = EditVehicleState.checkInstance();
                        if (!StringUtils.isNullOrWhitespace(this.showVehicleEditor)) {
                           var14.setScript(this.showVehicleEditor);
                        }

                        this.showVehicleEditor = null;
                        this.RedirectState = var14;
                        return GameStateMachine.StateAction.Yield;
                     }

                     this.showAttachmentEditor = false;
                     DebugLog.General.debugln("Activating AttachmentEditorState.");
                     AttachmentEditorState var13 = AttachmentEditorState.checkInstance();
                     this.RedirectState = var13;
                     return GameStateMachine.StateAction.Yield;
                  }

                  this.showAnimationViewer = false;
                  DebugLog.General.debugln("Activating AnimationViewerState.");
                  AnimationViewerState var12 = AnimationViewerState.checkInstance();
                  this.RedirectState = var12;
                  return GameStateMachine.StateAction.Yield;
               }

               this.showChunkDebugger = false;
               DebugLog.General.debugln("Activating DebugChunkState.");
               this.RedirectState = DebugChunkState.checkInstance();
               return GameStateMachine.StateAction.Yield;
            }
         }

         if (Core.bDebug) {
            this.debugTimes.add(System.nanoTime());
         }

         ++this.timesincelastinsanity;
         if (Core.bDebug) {
            this.debugTimes.add(System.nanoTime());
         }

         try {
            if (!GameServer.bServer && IsoPlayer.getInstance() != null && IsoPlayer.allPlayersDead()) {
               if (IsoPlayer.getInstance() != null) {
                  UIManager.getSpeedControls().SetCurrentGameSpeed(1);
               }

               IsoCamera.update();
            }

            this.alt = !this.alt;
            if (!GameServer.bServer) {
               WaitMul = 1;
               if (UIManager.getSpeedControls() != null) {
                  if (UIManager.getSpeedControls().getCurrentGameSpeed() == 2) {
                     WaitMul = 15;
                  }

                  if (UIManager.getSpeedControls().getCurrentGameSpeed() == 3) {
                     WaitMul = 30;
                  }
               }
            }

            if (Core.bDebug) {
               this.debugTimes.add(System.nanoTime());
            }

            if (GameServer.bServer) {
               this.Paused = GameServer.Players.isEmpty() && ServerOptions.instance.PauseEmpty.getValue() && ZombiePopulationManager.instance.readyToPause();
            }

            if (!this.Paused || GameClient.bClient) {
               try {
                  if (IsoCamera.getCameraCharacter() != null && IsoWorld.instance.bDoChunkMapUpdate) {
                     for(var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
                        if (IsoPlayer.players[var1] != null && !IsoWorld.instance.CurrentCell.ChunkMap[var1].ignore) {
                           if (!GameServer.bServer) {
                              IsoCamera.setCameraCharacter(IsoPlayer.players[var1]);
                              IsoPlayer.setInstance(IsoPlayer.players[var1]);
                           }

                           if (!GameServer.bServer) {
                              IsoWorld.instance.CurrentCell.ChunkMap[var1].ProcessChunkPos(IsoCamera.getCameraCharacter());
                           }
                        }
                     }
                  }

                  if (Core.bDebug) {
                     this.debugTimes.add(System.nanoTime());
                  }

                  IsoWorld.instance.update();
                  CompletableFuture var19 = null;
                  if (DebugOptions.instance.ThreadAmbient.getValue() && !GameServer.bServer) {
                     ObjectAmbientEmitters var10000 = ObjectAmbientEmitters.getInstance();
                     Objects.requireNonNull(var10000);
                     var19 = CompletableFuture.runAsync(var10000::update, PZForkJoinPool.commonPool());
                  }

                  GameProfiler.getInstance().invokeAndMeasure("GEM Update", GameEntityManager::Update);
                  GameProfiler.getInstance().invokeAndMeasure("Animal", AnimalController.getInstance(), AnimalController::update);
                  if (Core.bDebug) {
                     this.debugTimes.add(System.nanoTime());
                  }

                  GameProfiler.getInstance().invokeAndMeasure("Radio", ZomboidRadio.getInstance(), ZomboidRadio::update);
                  GameProfiler.getInstance().invokeAndMeasure("Stuff", this::UpdateStuff);
                  GameProfiler.getInstance().invokeAndMeasure("On Tick", this, IngameState::onTick);

                  try {
                     FMODAmbientWalls.getInstance().update();
                  } catch (Throwable var9) {
                     ExceptionLogger.logException(var9);
                  }

                  this.TickMusicDirector();
                  if (var19 != null) {
                     GameProfiler var22 = GameProfiler.getInstance();
                     Objects.requireNonNull(var19);
                     var22.invokeAndMeasure("ObjectAmbientEmitters.update", var19::join);
                  } else {
                     ObjectAmbientEmitters.getInstance().update();
                  }

                  this.numberTicks = Math.max(this.numberTicks + 1L, 0L);
               } catch (Exception var10) {
                  ExceptionLogger.logException(var10);
                  if (!GameServer.bServer) {
                     if (GameClient.bClient) {
                        for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
                           IsoPlayer var3 = IsoPlayer.players[var2];
                           if (var3 != null) {
                              ClientPlayerDB.getInstance().clientSendNetworkPlayerInt(var3);
                           }
                        }

                        WorldStreamer.instance.stop();
                     }

                     String var16 = Core.GameSaveWorld;
                     createWorld(Core.GameSaveWorld + "_crash");
                     copyWorld(var16, Core.GameSaveWorld);
                     if (GameClient.bClient) {
                        if (PlayerDB.isAllow()) {
                           PlayerDB.getInstance().saveLocalPlayersForce();
                           PlayerDB.getInstance().m_canSavePlayers = false;
                        }

                        if (ClientPlayerDB.isAllow()) {
                           ClientPlayerDB.getInstance().canSavePlayers = false;
                        }
                     }

                     try {
                        GameWindow.save(true);
                     } catch (Throwable var8) {
                        ExceptionLogger.logException(var8);
                     }

                     if (GameClient.bClient) {
                        try {
                           LuaEventManager.triggerEvent("OnPostSave");
                        } catch (Exception var7) {
                           ExceptionLogger.logException(var7);
                        }

                        if (ClientPlayerDB.isAllow()) {
                           ClientPlayerDB.getInstance().close();
                        }
                     }
                  }

                  if (GameClient.bClient) {
                     GameClient.instance.doDisconnect("crash");
                  }

                  return GameStateMachine.StateAction.Continue;
               }
            }
         } catch (Exception var11) {
            System.err.println("IngameState.update caught an exception.");
            ExceptionLogger.logException(var11);
         }

         if (Core.bDebug) {
            this.debugTimes.add(System.nanoTime());
         }

         if (!GameServer.bServer || ServerGUI.isCreated()) {
            GameProfiler.getInstance().invokeAndMeasure("Update Model", ModelManager.instance, ModelManager::update);
         }

         if (Core.bDebug && FPSGraph.instance != null) {
            FPSGraph.instance.addUpdate(System.currentTimeMillis());
            FPSGraph.instance.update();
         }

         if (GameClient.bClient || GameServer.bServer) {
            GameProfiler.getInstance().invokeAndMeasure("Update Managers", IngameState::updateManagers);
         }

         return GameStateMachine.StateAction.Remain;
      }
   }

   private void onTick() {
      LuaEventManager.triggerEvent("OnTick", (double)this.numberTicks);
   }

   private static void updateManagers() {
      TransactionManager.update();
      ActionManager.update();
      MPStatistics.Update();
   }

   private static class s_performance {
      static final PerformanceProfileProbe render = new PerformanceProfileProbe("IngameState.render");
      static final PerformanceProfileProbe renderFrame = new PerformanceProfileProbe("IngameState.renderFrame");
      static final PerformanceProfileProbe renderFrameText = new PerformanceProfileProbe("IngameState.renderFrameText");
      static final PerformanceProfileProbe renderFrameUI = new PerformanceProfileProbe("IngameState.renderFrameUI");
      static final PerformanceProfileProbe update = new PerformanceProfileProbe("IngameState.update");

      private s_performance() {
      }
   }
}
