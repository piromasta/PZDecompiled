package zombie.iso;

import fmod.fmod.FMODManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL20;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.ThumpState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODAmbientWalls;
import zombie.audio.ObjectAmbientEmitters;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.GameEntityType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStackedWasherDryer;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWheelieBin;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.AddItemToMapPacket;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.spnetwork.SinglePlayerServer;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZArrayList;
import zombie.vehicles.BaseVehicle;
import zombie.world.WorldDictionary;

public class IsoObject extends GameEntity implements Serializable, Thumpable {
   public static final byte OBF_Highlighted = 1;
   public static final byte OBF_HighlightRenderOnce = 2;
   public static final byte OBF_Blink = 4;
   public static final byte OBF_SatChair = 8;
   public static final int MAX_WALL_SPLATS = 32;
   private static final String PropMoveWithWind = "MoveWithWind";
   public static IsoObject lastRendered = null;
   public static IsoObject lastRenderedRendered = null;
   private static final ColorInfo stCol = new ColorInfo();
   public static float rmod;
   public static float gmod;
   public static float bmod;
   public static boolean LowLightingQualityHack = false;
   private static int DefaultCondition = 0;
   private static final ColorInfo stCol2 = new ColorInfo();
   private static final ColorInfo colFxMask = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
   public byte highlightFlags;
   public int keyId;
   public BaseSoundEmitter emitter;
   public float sheetRopeHealth;
   public boolean sheetRope;
   public boolean bNeverDoneAlpha;
   public boolean bAlphaForced;
   public ArrayList<IsoSpriteInstance> AttachedAnimSprite;
   public ArrayList<IsoWallBloodSplat> wallBloodSplats;
   public ItemContainer container;
   public IsoDirections dir;
   public short Damage;
   public float partialThumpDmg;
   public boolean NoPicking;
   public float offsetX;
   public float offsetY;
   public boolean OutlineOnMouseover;
   public IsoObject rerouteMask;
   public IsoSprite sprite;
   public IsoSprite overlaySprite;
   public ColorInfo overlaySpriteColor;
   public IsoGridSquare square;
   public final float[] alpha;
   protected final float[] targetAlpha;
   protected final ObjectRenderInfo[] renderInfo;
   public IsoObject rerouteCollide;
   public KahluaTable table;
   public String name;
   public float tintr;
   public float tintg;
   public float tintb;
   public String spriteName;
   public float sx;
   public float sy;
   public boolean doNotSync;
   protected ObjectRenderEffects windRenderEffects;
   protected ObjectRenderEffects objectRenderEffects;
   protected IsoObject externalWaterSource;
   protected boolean usesExternalWaterSource;
   ArrayList<IsoObject> Children;
   String tile;
   private boolean specialTooltip;
   private ColorInfo highlightColor;
   private ArrayList<ItemContainer> secondaryContainers;
   private ColorInfo customColor;
   private float renderYOffset;
   protected byte isOutlineHighlight;
   protected byte isOutlineHlAttached;
   protected byte isOutlineHlBlink;
   protected final int[] outlineHighlightCol;
   private float outlineThickness;
   protected boolean bMovedThumpable;
   protected String spriteModelName;
   protected SpriteModel spriteModel;
   protected IsoSprite spriteModelInit;
   protected boolean bAnimating;
   private IsoLightSource lightSource;
   private IsoSpriteInstance onOverlay;
   public ColorInfo fireColor;
   private static final Map<Byte, IsoObjectFactory> byteToObjectMap = new HashMap();
   private static final Map<Integer, IsoObjectFactory> hashCodeToObjectMap = new HashMap();
   private static final Map<String, IsoObjectFactory> nameToObjectMap = new HashMap();
   private static IsoObjectFactory factoryIsoObject;
   private static IsoObjectFactory factoryVehicle;
   private boolean bRemoveFromWorldToMeta;
   private long isoEntityNetID;
   private int lastObjectIndex;
   public IsoGridSquare renderSquareOverride;
   public IsoGridSquare renderSquareOverride2;
   public float renderDepthAdjust;

   public IsoObject(IsoCell var1) {
      this();
   }

   public IsoObject() {
      this.keyId = -1;
      this.sheetRopeHealth = 100.0F;
      this.sheetRope = false;
      this.bNeverDoneAlpha = true;
      this.bAlphaForced = false;
      this.container = null;
      this.dir = IsoDirections.N;
      this.Damage = 100;
      this.partialThumpDmg = 0.0F;
      this.NoPicking = false;
      this.offsetX = (float)(32 * Core.TileScale);
      this.offsetY = (float)(96 * Core.TileScale);
      this.OutlineOnMouseover = false;
      this.rerouteMask = null;
      this.sprite = null;
      this.overlaySprite = null;
      this.overlaySpriteColor = null;
      this.alpha = new float[4];
      this.targetAlpha = new float[4];
      this.renderInfo = new ObjectRenderInfo[4];
      this.rerouteCollide = null;
      this.table = null;
      this.name = null;
      this.tintr = 1.0F;
      this.tintg = 1.0F;
      this.tintb = 1.0F;
      this.spriteName = null;
      this.doNotSync = false;
      this.externalWaterSource = null;
      this.usesExternalWaterSource = false;
      this.specialTooltip = false;
      this.highlightColor = new ColorInfo(0.9F, 1.0F, 0.0F, 1.0F);
      this.customColor = null;
      this.renderYOffset = 0.0F;
      this.isOutlineHighlight = 0;
      this.isOutlineHlAttached = 0;
      this.isOutlineHlBlink = 0;
      this.outlineHighlightCol = new int[4];
      this.outlineThickness = 1.0F;
      this.bMovedThumpable = false;
      this.spriteModelName = null;
      this.spriteModel = null;
      this.spriteModelInit = null;
      this.bAnimating = false;
      this.lightSource = null;
      this.onOverlay = null;
      this.fireColor = new ColorInfo(0.51F, 0.27F, 0.18F, 1.0F);
      this.bRemoveFromWorldToMeta = false;
      this.isoEntityNetID = -1L;
      this.lastObjectIndex = -1;
      this.renderSquareOverride = null;
      this.renderSquareOverride2 = null;
      this.renderDepthAdjust = 0.0F;

      for(int var1 = 0; var1 < 4; ++var1) {
         this.setAlphaAndTarget(var1, 1.0F);
         this.outlineHighlightCol[var1] = -1;
         this.renderInfo[var1] = new ObjectRenderInfo(this);
      }

   }

   public IsoObject(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      this();
      this.sprite = var3;
      this.square = var2;
   }

   public IsoObject(IsoCell var1, IsoGridSquare var2, String var3) {
      this();
      this.sprite = IsoSpriteManager.instance.getSprite(var3);
      this.square = var2;
      this.tile = var3;
   }

   public IsoObject(IsoGridSquare var1, String var2, String var3) {
      this();
      this.sprite = IsoSpriteManager.instance.getSprite(var2);
      this.square = var1;
      this.tile = var2;
      this.spriteName = var2;
      this.name = var3;
   }

   public IsoObject(IsoGridSquare var1, String var2, String var3, boolean var4) {
      this();
      if (var4) {
         this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
         this.sprite.LoadFramesNoDirPageSimple(var2);
      } else {
         this.sprite = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var2);
      }

      this.tile = var2;
      this.square = var1;
      this.name = var3;
   }

   public boolean isFloor() {
      return this.getProperties() != null ? this.getProperties().Is(IsoFlagType.solidfloor) : false;
   }

   public IsoObject(IsoGridSquare var1, String var2, boolean var3) {
      this();
      if (var3) {
         this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
         this.sprite.LoadFramesNoDirPageSimple(var2);
      } else {
         this.sprite = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var2);
      }

      this.tile = var2;
      this.square = var1;
   }

   public IsoObject(IsoGridSquare var1, String var2) {
      this();
      this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
      this.sprite.LoadFramesNoDirPageSimple(var2);
      this.square = var1;
   }

   public static IsoObject getNew(IsoGridSquare var0, String var1, String var2, boolean var3) {
      IsoObject var4 = null;
      synchronized(CellLoader.isoObjectCache) {
         if (CellLoader.isoObjectCache.isEmpty()) {
            var4 = new IsoObject(var0, var1, var2, var3);
         } else {
            var4 = (IsoObject)CellLoader.isoObjectCache.pop();
            var4.reset();
            var4.tile = var1;
         }
      }

      if (var3) {
         var4.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
         var4.sprite.LoadFramesNoDirPageSimple(var4.tile);
      } else {
         var4.sprite = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var4.tile);
      }

      var4.square = var0;
      var4.name = var2;
      return var4;
   }

   public static IsoObject getLastRendered() {
      return lastRendered;
   }

   public static void setLastRendered(IsoObject var0) {
      lastRendered = var0;
   }

   public static IsoObject getLastRenderedRendered() {
      return lastRenderedRendered;
   }

   public static void setLastRenderedRendered(IsoObject var0) {
      lastRenderedRendered = var0;
   }

   public static void setDefaultCondition(int var0) {
      DefaultCondition = var0;
   }

   public static IsoObject getNew() {
      synchronized(CellLoader.isoObjectCache) {
         return CellLoader.isoObjectCache.isEmpty() ? new IsoObject() : (IsoObject)CellLoader.isoObjectCache.pop();
      }
   }

   private static IsoObjectFactory addIsoObjectFactory(IsoObjectFactory var0) {
      if (byteToObjectMap.containsKey(var0.classID)) {
         throw new RuntimeException("Class id already exists, " + var0.objectName);
      } else {
         byteToObjectMap.put(var0.classID, var0);
         if (hashCodeToObjectMap.containsKey(var0.hashCode)) {
            throw new RuntimeException("Hashcode already exists, " + var0.objectName);
         } else {
            hashCodeToObjectMap.put(var0.hashCode, var0);
            if (nameToObjectMap.containsKey(var0.objectName)) {
               throw new RuntimeException("ObjectName already exists, " + var0.objectName);
            } else {
               nameToObjectMap.put(var0.objectName, var0);
               return var0;
            }
         }
      }
   }

   public static IsoObjectFactory getFactoryVehicle() {
      return factoryVehicle;
   }

   private static void initFactory() {
      factoryIsoObject = addIsoObjectFactory(new IsoObjectFactory(0, "IsoObject") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            IsoObject var2 = IsoObject.getNew();
            var2.sx = 0.0F;
            return var2;
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(1, "Player") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoPlayer(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(2, "Survivor") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoSurvivor(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(3, "Zombie") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoZombie(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(4, "Pushable") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoPushableObject(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(5, "WheelieBin") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoWheelieBin(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(6, "WorldInventoryItem") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoWorldInventoryObject(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(7, "Jukebox") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoJukebox(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(8, "Curtain") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoCurtain(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(9, "Radio") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoRadio(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(10, "Television") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoTelevision(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(11, "DeadBody") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoDeadBody(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(12, "Barbecue") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoBarbecue(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(13, "ClothingDryer") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoClothingDryer(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(14, "ClothingWasher") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoClothingWasher(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(15, "Fireplace") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoFireplace(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(16, "Stove") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoStove(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(17, "Door") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoDoor(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(18, "Thumpable") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoThumpable(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(19, "IsoTrap") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoTrap(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(20, "IsoBrokenGlass") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoBrokenGlass(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(21, "IsoCarBatteryCharger") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoCarBatteryCharger(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(22, "IsoGenerator") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoGenerator(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(23, "IsoCompost") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoCompost(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(24, "Mannequin") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoMannequin(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(26, "Window") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoWindow(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(27, "Barricade") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoBarricade(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(28, "Tree") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return IsoTree.getNew();
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(29, "LightSwitch") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoLightSwitch(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(30, "ZombieGiblets") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoZombieGiblets(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(31, "MolotovCocktail") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoMolotovCocktail(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(32, "Fire") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoFire(var1);
         }
      });
      factoryVehicle = addIsoObjectFactory(new IsoObjectFactory(33, "Vehicle") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new BaseVehicle(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(34, "CombinationWasherDryer") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoCombinationWasherDryer(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(35, "StackedWasherDryer") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoStackedWasherDryer(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(36, "Animal") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoAnimal(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(37, "FeedingTrough") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoFeedingTrough(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(38, "IsoHutch") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoHutch(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(39, "IsoAnimalTrack") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoAnimalTrack(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(40, "ButcherHook") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoButcherHook(var1);
         }
      });
      addIsoObjectFactory(new IsoObjectFactory(41, "IsoWindowFrame") {
         protected IsoObject InstantiateObject(IsoCell var1) {
            return new IsoWindowFrame(var1);
         }
      });
   }

   public static byte factoryGetClassID(String var0) {
      IsoObjectFactory var1 = (IsoObjectFactory)hashCodeToObjectMap.get(var0.hashCode());
      return var1 != null ? var1.classID : factoryIsoObject.classID;
   }

   public static IsoObject factoryFromFileInput(IsoCell var0, byte var1) {
      IsoObjectFactory var2 = (IsoObjectFactory)byteToObjectMap.get(var1);
      if (var2 == null || var2.objectName.equals("Vehicle") && GameClient.bClient) {
         if (var2 == null && Core.bDebug) {
            throw new RuntimeException("Cannot get IsoObject from classID: " + var1);
         } else {
            IsoObject var3 = new IsoObject(var0);
            return var3;
         }
      } else {
         return var2.InstantiateObject(var0);
      }
   }

   /** @deprecated */
   @Deprecated
   public static IsoObject factoryFromFileInput_OLD(IsoCell var0, int var1) {
      IsoObject var2;
      if (var1 == "IsoObject".hashCode()) {
         var2 = getNew();
         var2.sx = 0.0F;
         return var2;
      } else if (var1 == "Player".hashCode()) {
         return new IsoPlayer(var0);
      } else if (var1 == "Survivor".hashCode()) {
         return new IsoSurvivor(var0);
      } else if (var1 == "Zombie".hashCode()) {
         return new IsoZombie(var0);
      } else if (var1 == "Pushable".hashCode()) {
         return new IsoPushableObject(var0);
      } else if (var1 == "WheelieBin".hashCode()) {
         return new IsoWheelieBin(var0);
      } else if (var1 == "WorldInventoryItem".hashCode()) {
         return new IsoWorldInventoryObject(var0);
      } else if (var1 == "Jukebox".hashCode()) {
         return new IsoJukebox(var0);
      } else if (var1 == "Curtain".hashCode()) {
         return new IsoCurtain(var0);
      } else if (var1 == "Radio".hashCode()) {
         return new IsoRadio(var0);
      } else if (var1 == "Television".hashCode()) {
         return new IsoTelevision(var0);
      } else if (var1 == "DeadBody".hashCode()) {
         return new IsoDeadBody(var0);
      } else if (var1 == "Barbecue".hashCode()) {
         return new IsoBarbecue(var0);
      } else if (var1 == "ClothingDryer".hashCode()) {
         return new IsoClothingDryer(var0);
      } else if (var1 == "ClothingWasher".hashCode()) {
         return new IsoClothingWasher(var0);
      } else if (var1 == "Fireplace".hashCode()) {
         return new IsoFireplace(var0);
      } else if (var1 == "Stove".hashCode()) {
         return new IsoStove(var0);
      } else if (var1 == "Door".hashCode()) {
         return new IsoDoor(var0);
      } else if (var1 == "Thumpable".hashCode()) {
         return new IsoThumpable(var0);
      } else if (var1 == "IsoTrap".hashCode()) {
         return new IsoTrap(var0);
      } else if (var1 == "IsoBrokenGlass".hashCode()) {
         return new IsoBrokenGlass(var0);
      } else if (var1 == "IsoCarBatteryCharger".hashCode()) {
         return new IsoCarBatteryCharger(var0);
      } else if (var1 == "IsoGenerator".hashCode()) {
         return new IsoGenerator(var0);
      } else if (var1 == "IsoCompost".hashCode()) {
         return new IsoCompost(var0);
      } else if (var1 == "Mannequin".hashCode()) {
         return new IsoMannequin(var0);
      } else if (var1 == "Window".hashCode()) {
         return new IsoWindow(var0);
      } else if (var1 == "Barricade".hashCode()) {
         return new IsoBarricade(var0);
      } else if (var1 == "Tree".hashCode()) {
         return IsoTree.getNew();
      } else if (var1 == "LightSwitch".hashCode()) {
         return new IsoLightSwitch(var0);
      } else if (var1 == "ZombieGiblets".hashCode()) {
         return new IsoZombieGiblets(var0);
      } else if (var1 == "MolotovCocktail".hashCode()) {
         return new IsoMolotovCocktail(var0);
      } else if (var1 == "Fire".hashCode()) {
         return new IsoFire(var0);
      } else if (var1 == "Vehicle".hashCode() && !GameClient.bClient) {
         return new BaseVehicle(var0);
      } else {
         var2 = new IsoObject(var0);
         return var2;
      }
   }

   /** @deprecated */
   @Deprecated
   public static Class factoryClassFromFileInput(IsoCell var0, int var1) {
      if (var1 == "IsoObject".hashCode()) {
         return IsoObject.class;
      } else if (var1 == "Player".hashCode()) {
         return IsoPlayer.class;
      } else if (var1 == "Survivor".hashCode()) {
         return IsoSurvivor.class;
      } else if (var1 == "Zombie".hashCode()) {
         return IsoZombie.class;
      } else if (var1 == "Pushable".hashCode()) {
         return IsoPushableObject.class;
      } else if (var1 == "WheelieBin".hashCode()) {
         return IsoWheelieBin.class;
      } else if (var1 == "WorldInventoryItem".hashCode()) {
         return IsoWorldInventoryObject.class;
      } else if (var1 == "Jukebox".hashCode()) {
         return IsoJukebox.class;
      } else if (var1 == "Curtain".hashCode()) {
         return IsoCurtain.class;
      } else if (var1 == "Radio".hashCode()) {
         return IsoRadio.class;
      } else if (var1 == "Television".hashCode()) {
         return IsoTelevision.class;
      } else if (var1 == "DeadBody".hashCode()) {
         return IsoDeadBody.class;
      } else if (var1 == "Barbecue".hashCode()) {
         return IsoBarbecue.class;
      } else if (var1 == "ClothingDryer".hashCode()) {
         return IsoClothingDryer.class;
      } else if (var1 == "ClothingWasher".hashCode()) {
         return IsoClothingWasher.class;
      } else if (var1 == "Fireplace".hashCode()) {
         return IsoFireplace.class;
      } else if (var1 == "Stove".hashCode()) {
         return IsoStove.class;
      } else if (var1 == "Mannequin".hashCode()) {
         return IsoMannequin.class;
      } else if (var1 == "Door".hashCode()) {
         return IsoDoor.class;
      } else if (var1 == "Thumpable".hashCode()) {
         return IsoThumpable.class;
      } else if (var1 == "Window".hashCode()) {
         return IsoWindow.class;
      } else if (var1 == "Barricade".hashCode()) {
         return IsoBarricade.class;
      } else if (var1 == "Tree".hashCode()) {
         return IsoTree.class;
      } else if (var1 == "LightSwitch".hashCode()) {
         return IsoLightSwitch.class;
      } else if (var1 == "ZombieGiblets".hashCode()) {
         return IsoZombieGiblets.class;
      } else if (var1 == "MolotovCocktail".hashCode()) {
         return IsoMolotovCocktail.class;
      } else {
         return var1 == "Vehicle".hashCode() ? BaseVehicle.class : IsoObject.class;
      }
   }

   /** @deprecated */
   @Deprecated
   static IsoObject factoryFromFileInput(IsoCell var0, DataInputStream var1) throws IOException {
      boolean var2 = var1.readBoolean();
      if (!var2) {
         return null;
      } else {
         byte var3 = var1.readByte();
         IsoObject var4 = factoryFromFileInput(var0, var3);
         return var4;
      }
   }

   public static IsoObject factoryFromFileInput(IsoCell var0, ByteBuffer var1) {
      boolean var2 = var1.get() != 0;
      if (!var2) {
         return null;
      } else {
         byte var3 = var1.get();
         IsoObject var4 = factoryFromFileInput(var0, var3);
         return var4;
      }
   }

   public void sync() {
      this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
   }

   public void syncIsoObject(boolean var1, byte var2, UdpConnection var3, ByteBuffer var4) {
      if (this.square == null) {
         System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
      } else if (this.getObjectIndex() == -1) {
         PrintStream var10000 = System.out;
         String var10001 = this.getClass().getSimpleName();
         var10000.println("ERROR: " + var10001 + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
      } else {
         if (GameClient.bClient && !var1) {
            ByteBufferWriter var8 = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(var8);
            this.syncIsoObjectSend(var8);
            PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
         } else {
            Iterator var5;
            UdpConnection var6;
            ByteBufferWriter var7;
            if (GameServer.bServer && !var1) {
               var5 = GameServer.udpEngine.connections.iterator();

               while(var5.hasNext()) {
                  var6 = (UdpConnection)var5.next();
                  var7 = var6.startPacket();
                  PacketTypes.PacketType.SyncIsoObject.doPacket(var7);
                  this.syncIsoObjectSend(var7);
                  PacketTypes.PacketType.SyncIsoObject.send(var6);
               }
            } else if (var1) {
               this.syncIsoObjectReceive(var4);
               if (GameServer.bServer) {
                  var5 = GameServer.udpEngine.connections.iterator();

                  while(var5.hasNext()) {
                     var6 = (UdpConnection)var5.next();
                     if (var3 != null && var6.getConnectedGUID() != var3.getConnectedGUID()) {
                        var7 = var6.startPacket();
                        PacketTypes.PacketType.SyncIsoObject.doPacket(var7);
                        this.syncIsoObjectSend(var7);
                        PacketTypes.PacketType.SyncIsoObject.send(var6);
                     }
                  }
               }
            }
         }

      }
   }

   public void syncIsoObjectSend(ByteBufferWriter var1) {
      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      var1.putByte((byte)this.square.getObjects().indexOf(this));
      var1.putByte((byte)1);
      var1.putByte((byte)0);
      var1.putInt(this.getWaterAmount());
      FluidContainer var2 = (FluidContainer)this.getComponent(ComponentType.FluidContainer);
      if (var2 != null) {
         try {
            var2.save(var1.bb);
         } catch (IOException var4) {
            throw new RuntimeException(var4);
         }
      }

   }

   public void syncIsoObjectReceive(ByteBuffer var1) {
      this.setWaterAmount(var1.getInt());
      FluidContainer var2 = (FluidContainer)this.getComponent(ComponentType.FluidContainer);
      if (var2 != null) {
         try {
            var2.load(var1, IsoWorld.getWorldVersion());
         } catch (IOException var4) {
            throw new RuntimeException(var4);
         }
      }

   }

   public String getTextureName() {
      return this.sprite == null ? null : this.sprite.name;
   }

   public boolean Serialize() {
      return true;
   }

   public KahluaTable getModData() {
      if (this.table == null) {
         this.table = LuaManager.platform.newTable();
      }

      return this.table;
   }

   public void setModData(KahluaTable var1) {
      this.table = var1;
   }

   public boolean hasModData() {
      return this.table != null && !this.table.isEmpty();
   }

   public IsoGridSquare getSquare() {
      return this.square;
   }

   public void setSquare(IsoGridSquare var1) {
      this.square = var1;
   }

   public IsoChunk getChunk() {
      IsoGridSquare var1 = this.getSquare();
      return var1 == null ? null : var1.getChunk();
   }

   public void update() {
   }

   public void renderlast() {
   }

   public void DirtySlice() {
   }

   public String getObjectName() {
      if (this.name != null) {
         return this.name;
      } else {
         return this.sprite != null && this.sprite.getParentObjectName() != null ? this.sprite.getParentObjectName() : "IsoObject";
      }
   }

   public final void load(ByteBuffer var1, int var2) throws IOException {
      this.load(var1, var2, false);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      int var4 = var1.getInt();
      var4 = IsoChunk.Fix2x(this.square, var4);
      this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, var4);
      if (var4 == -1) {
         this.sprite = IsoSpriteManager.instance.getSprite("");

         assert this.sprite != null;

         assert this.sprite.ID == -1;
      }

      BitHeaderRead var5 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      if (!var5.equals(0)) {
         String var7;
         int var8;
         if (var5.hasFlags(1)) {
            int var6;
            if (var5.hasFlags(2)) {
               var6 = 1;
            } else {
               var6 = var1.get() & 255;
            }

            if (var3) {
               var7 = GameWindow.ReadStringUTF(var1);
               DebugLog.log(var7 + ", read = " + var6);
            }

            for(int var22 = 0; var22 < var6; ++var22) {
               if (this.AttachedAnimSprite == null) {
                  this.AttachedAnimSprite = new ArrayList();
               }

               var8 = var1.getInt();
               IsoSprite var9 = IsoSprite.getSprite(IsoSpriteManager.instance, var8);
               IsoSpriteInstance var10 = null;
               if (var9 != null) {
                  var10 = var9.newInstance();
               } else if (Core.bDebug) {
                  DebugLog.General.warn("discarding attached sprite because it has no tile properties");
               }

               byte var11 = var1.get();
               boolean var12 = false;
               boolean var13 = false;
               if ((var11 & 2) != 0) {
                  var12 = true;
               }

               if ((var11 & 4) != 0 && var10 != null) {
                  var10.Flip = true;
               }

               if ((var11 & 8) != 0 && var10 != null) {
                  var10.bCopyTargetAlpha = true;
               }

               if ((var11 & 16) != 0) {
                  var13 = true;
                  if (var10 != null) {
                     var10.bMultiplyObjectAlpha = true;
                  }
               }

               float var14;
               if (var12) {
                  var14 = var1.getFloat();
                  float var15 = var1.getFloat();
                  float var16 = var1.getFloat();
                  float var17 = Bits.unpackByteToFloatUnit(var1.get());
                  float var18 = Bits.unpackByteToFloatUnit(var1.get());
                  float var19 = Bits.unpackByteToFloatUnit(var1.get());
                  if (var10 != null) {
                     var10.offX = var14;
                     var10.offY = var15;
                     var10.offZ = var16;
                     var10.tintr = var17;
                     var10.tintg = var18;
                     var10.tintb = var19;
                  }
               } else if (var10 != null) {
                  var10.offX = 0.0F;
                  var10.offY = 0.0F;
                  var10.offZ = 0.0F;
                  var10.tintr = 1.0F;
                  var10.tintg = 1.0F;
                  var10.tintb = 1.0F;
                  var10.alpha = 1.0F;
                  var10.targetAlpha = 1.0F;
               }

               if (var13) {
                  var14 = var1.getFloat();
                  if (var10 != null) {
                     var10.alpha = var14;
                  }
               }

               if (var9 != null) {
                  if (var9.name != null && var9.name.startsWith("overlay_blood_")) {
                     var14 = (float)GameTime.getInstance().getWorldAgeHours();
                     IsoWallBloodSplat var34 = new IsoWallBloodSplat(var14, var9);
                     if (this.wallBloodSplats == null) {
                        this.wallBloodSplats = new ArrayList();
                     }

                     this.wallBloodSplats.add(var34);
                  } else {
                     this.AttachedAnimSprite.add(var10);
                  }
               }
            }
         }

         if (var5.hasFlags(4)) {
            if (var3) {
               String var21 = GameWindow.ReadStringUTF(var1);
               DebugLog.log(var21);
            }

            byte var23 = var1.get();
            if ((var23 & 2) != 0) {
               this.name = "Grass";
            } else if ((var23 & 4) != 0) {
               this.name = WorldDictionary.getObjectNameFromID(var1.get());
            } else if ((var23 & 8) != 0) {
               this.name = GameWindow.ReadString(var1);
            }

            if ((var23 & 16) != 0) {
               this.spriteName = WorldDictionary.getSpriteNameFromID(var1.getInt());
            } else if ((var23 & 32) != 0) {
               this.spriteName = GameWindow.ReadString(var1);
            }
         }

         float var25;
         float var26;
         if (var5.hasFlags(8)) {
            float var24 = Bits.unpackByteToFloatUnit(var1.get());
            var25 = Bits.unpackByteToFloatUnit(var1.get());
            var26 = Bits.unpackByteToFloatUnit(var1.get());
            this.customColor = new ColorInfo(var24, var25, var26, 1.0F);
         }

         this.doNotSync = var5.hasFlags(16);
         this.setOutlineOnMouseover(var5.hasFlags(32));
         if (var5.hasFlags(64)) {
            BitHeaderRead var27 = BitHeader.allocRead(BitHeader.HeaderSize.Short, var1);
            byte var28;
            float var29;
            if (var27.hasFlags(1)) {
               var28 = var1.get();
               if (var28 > 0) {
                  if (this.wallBloodSplats == null) {
                     this.wallBloodSplats = new ArrayList();
                  }

                  var8 = SandboxOptions.getInstance().BloodSplatLifespanDays.getValue();
                  var29 = (float)GameTime.getInstance().getWorldAgeHours();

                  for(int var32 = 0; var32 < var28; ++var32) {
                     IsoWallBloodSplat var31 = new IsoWallBloodSplat();
                     var31.load(var1, var2);
                     if (var31.worldAge > var29) {
                        var31.worldAge = var29;
                     }

                     if (var8 <= 0 || !(var29 - var31.worldAge >= (float)(var8 * 24))) {
                        this.wallBloodSplats.add(var31);
                     }
                  }
               }
            }

            if (var27.hasFlags(2)) {
               if (var3) {
                  var7 = GameWindow.ReadStringUTF(var1);
                  DebugLog.log(var7);
               }

               var28 = var1.get();

               for(var8 = 0; var8 < var28; ++var8) {
                  try {
                     ItemContainer var30 = new ItemContainer();
                     var30.ID = 0;
                     var30.parent = this;
                     var30.parent.square = this.square;
                     var30.SourceGrid = this.square;
                     var30.load(var1, var2);
                     if (var8 == 0) {
                        if (this instanceof IsoDeadBody) {
                           var30.Capacity = 8;
                        }

                        this.container = var30;
                     } else {
                        this.addSecondaryContainer(var30);
                     }
                  } catch (Exception var20) {
                     if (this.container != null) {
                        DebugLog.log("Failed to stream in container ID: " + this.container.ID);
                     }

                     throw new RuntimeException(var20);
                  }
               }
            }

            if (var27.hasFlags(4)) {
               if (this.table == null) {
                  this.table = LuaManager.platform.newTable();
               }

               this.table.load(var1, var2);
            }

            this.setSpecialTooltip(var27.hasFlags(8));
            if (var27.hasFlags(16)) {
               this.keyId = var1.getInt();
            }

            this.usesExternalWaterSource = var27.hasFlags(32);
            if (var27.hasFlags(64)) {
               this.sheetRope = true;
               this.sheetRopeHealth = var1.getFloat();
            } else {
               this.sheetRope = false;
            }

            if (var27.hasFlags(128)) {
               this.renderYOffset = var1.getFloat();
            }

            if (var27.hasFlags(256)) {
               var7 = null;
               if (var27.hasFlags(512)) {
                  var7 = GameWindow.ReadString(var1);
               } else {
                  var7 = WorldDictionary.getSpriteNameFromID(var1.getInt());
               }

               if (var7 != null && !var7.isEmpty()) {
                  this.overlaySprite = IsoSpriteManager.instance.getSprite(var7);
                  this.overlaySprite.name = var7;
               }
            }

            if (var27.hasFlags(1024)) {
               var25 = Bits.unpackByteToFloatUnit(var1.get());
               var26 = Bits.unpackByteToFloatUnit(var1.get());
               var29 = Bits.unpackByteToFloatUnit(var1.get());
               float var33 = Bits.unpackByteToFloatUnit(var1.get());
               if (this.overlaySprite != null) {
                  this.setOverlaySpriteColor(var25, var26, var29, var33);
               }
            }

            this.setMovedThumpable(var27.hasFlags(2048));
            if (var27.hasFlags(4096)) {
               this.loadEntity(var1, var2);
            }

            if (var27.hasFlags(8192)) {
               this.spriteModelName = GameWindow.ReadStringUTF(var1);
            }

            var27.release();
         }
      }

      var5.release();
      if (this.sprite == null) {
         this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
         this.sprite.LoadSingleTexture(this.spriteName);
      }

   }

   public final void save(ByteBuffer var1) throws IOException {
      this.save(var1, false);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      var1.put((byte)(this.Serialize() ? 1 : 0));
      if (this.Serialize()) {
         var1.put(factoryGetClassID(this.getObjectName()));
         var1.putInt(this.sprite == null ? -1 : this.sprite.ID);
         BitHeaderWrite var3 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
         int var5;
         int var7;
         if (this.AttachedAnimSprite != null) {
            var3.addFlags(1);
            if (this.AttachedAnimSprite.size() == 1) {
               var3.addFlags(2);
            }

            int var4 = this.AttachedAnimSprite.size() > 255 ? 255 : this.AttachedAnimSprite.size();
            if (var4 != 1) {
               var1.put((byte)var4);
            }

            if (var2) {
               GameWindow.WriteString(var1, "Writing attached sprites (" + var4 + ")");
            }

            for(var5 = 0; var5 < var4; ++var5) {
               IsoSpriteInstance var6 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var5);
               var1.putInt(var6.getID());
               var7 = 0;
               boolean var8 = false;
               if (var6.offX != 0.0F || var6.offY != 0.0F || var6.offZ != 0.0F || var6.tintr != 1.0F || var6.tintg != 1.0F || var6.tintb != 1.0F) {
                  var7 = (byte)(var7 | 2);
                  var8 = true;
               }

               if (var6.Flip) {
                  var7 = (byte)(var7 | 4);
               }

               if (var6.bCopyTargetAlpha) {
                  var7 = (byte)(var7 | 8);
               }

               if (var6.bMultiplyObjectAlpha) {
                  var7 = (byte)(var7 | 16);
               }

               var1.put((byte)var7);
               if (var8) {
                  var1.putFloat(var6.offX);
                  var1.putFloat(var6.offY);
                  var1.putFloat(var6.offZ);
                  var1.put(Bits.packFloatUnitToByte(var6.tintr));
                  var1.put(Bits.packFloatUnitToByte(var6.tintg));
                  var1.put(Bits.packFloatUnitToByte(var6.tintb));
               }

               if (var6.bMultiplyObjectAlpha) {
                  var1.putFloat(var6.alpha);
               }
            }
         }

         int var11;
         if (this.name != null || this.spriteName != null) {
            var3.addFlags(4);
            if (var2) {
               GameWindow.WriteString(var1, "Writing name");
            }

            byte var9 = 0;
            byte var10 = -1;
            var11 = -1;
            if (this.name != null) {
               if (this.name.equals("Grass")) {
                  var9 = (byte)(var9 | 2);
               } else {
                  var10 = WorldDictionary.getIdForObjectName(this.name);
                  if (var10 >= 0) {
                     var9 = (byte)(var9 | 4);
                  } else {
                     var9 = (byte)(var9 | 8);
                  }
               }
            }

            if (this.spriteName != null) {
               var11 = WorldDictionary.getIdForSpriteName(this.spriteName);
               if (var11 >= 0) {
                  var9 = (byte)(var9 | 16);
               } else {
                  var9 = (byte)(var9 | 32);
               }
            }

            var1.put(var9);
            if (this.name != null && !this.name.equals("Grass")) {
               if (var10 >= 0) {
                  var1.put(var10);
               } else {
                  GameWindow.WriteString(var1, this.name);
               }
            }

            if (this.spriteName != null) {
               if (var11 >= 0) {
                  var1.putInt(var11);
               } else {
                  GameWindow.WriteString(var1, this.spriteName);
               }
            }
         }

         if (this.customColor != null) {
            var3.addFlags(8);
            var1.put(Bits.packFloatUnitToByte(this.customColor.r));
            var1.put(Bits.packFloatUnitToByte(this.customColor.g));
            var1.put(Bits.packFloatUnitToByte(this.customColor.b));
         }

         if (this.doNotSync) {
            var3.addFlags(16);
         }

         if (this.isOutlineOnMouseover()) {
            var3.addFlags(32);
         }

         BitHeaderWrite var12 = BitHeader.allocWrite(BitHeader.HeaderSize.Short, var1);
         if (this.wallBloodSplats != null) {
            var12.addFlags(1);
            var5 = Math.min(this.wallBloodSplats.size(), 32);
            var11 = this.wallBloodSplats.size() - var5;
            var1.put((byte)var5);

            for(var7 = var11; var7 < this.wallBloodSplats.size(); ++var7) {
               ((IsoWallBloodSplat)this.wallBloodSplats.get(var7)).save(var1);
            }
         }

         if (this.getContainerCount() > 0) {
            var12.addFlags(2);
            if (var2) {
               GameWindow.WriteString(var1, "Writing container");
            }

            var1.put((byte)this.getContainerCount());

            for(var5 = 0; var5 < this.getContainerCount(); ++var5) {
               this.getContainerByIndex(var5).save(var1);
            }
         }

         if (this.table != null && !this.table.isEmpty()) {
            var12.addFlags(4);
            this.table.save(var1);
         }

         if (this.haveSpecialTooltip()) {
            var12.addFlags(8);
         }

         if (this.getKeyId() != -1) {
            var12.addFlags(16);
            var1.putInt(this.getKeyId());
         }

         if (this.usesExternalWaterSource) {
            var12.addFlags(32);
         }

         if (this.sheetRope) {
            var12.addFlags(64);
            var1.putFloat(this.sheetRopeHealth);
         }

         if (this.renderYOffset != 0.0F) {
            var12.addFlags(128);
            var1.putFloat(this.renderYOffset);
         }

         if (this.getOverlaySprite() != null) {
            var12.addFlags(256);
            var5 = WorldDictionary.getIdForSpriteName(this.getOverlaySprite().name);
            if (var5 < 0) {
               var12.addFlags(512);
               GameWindow.WriteString(var1, this.getOverlaySprite().name);
            } else {
               var1.putInt(var5);
            }

            if (this.getOverlaySpriteColor() != null) {
               var12.addFlags(1024);
               var1.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().r));
               var1.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().g));
               var1.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().b));
               var1.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().a));
            }
         }

         if (this.isMovedThumpable()) {
            var12.addFlags(2048);
         }

         if (this.requiresEntitySave()) {
            var12.addFlags(4096);
            this.saveEntity(var1);
         }

         if (this.spriteModelName != null) {
            var12.addFlags(8192);
            GameWindow.WriteStringUTF(var1, this.spriteModelName);
         }

         if (!var12.equals(0)) {
            var3.addFlags(64);
            var12.write();
         } else {
            var1.position(var12.getStartPosition());
         }

         var3.write();
         var3.release();
         var12.release();
      }
   }

   public void saveState(ByteBuffer var1) throws IOException {
   }

   public void loadState(ByteBuffer var1) throws IOException {
   }

   public void softReset() {
      if (this.container != null) {
         this.container.Items.clear();
         this.container.bExplored = false;
         this.setOverlaySprite((String)null, -1.0F, -1.0F, -1.0F, -1.0F, false);
      }

      if (this.AttachedAnimSprite != null && !this.AttachedAnimSprite.isEmpty()) {
         for(int var1 = 0; var1 < this.AttachedAnimSprite.size(); ++var1) {
            IsoSprite var2 = ((IsoSpriteInstance)this.AttachedAnimSprite.get(var1)).parentSprite;
            if (var2.name != null && var2.name.contains("blood")) {
               this.AttachedAnimSprite.remove(var1);
               --var1;
            }
         }
      }

   }

   public void AttackObject(IsoGameCharacter var1) {
      this.Damage = (short)(this.Damage - 10);
      HandWeapon var2 = (HandWeapon)var1.getPrimaryHandItem();
      SoundManager.instance.PlaySound(var2.getDoorHitSound(), false, 2.0F);
      WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
      if (this.Damage <= 0) {
         this.square.getObjects().remove(this);
         this.square.RecalcAllWithNeighbours(true);
         if (this.getType() == IsoObjectType.stairsBN || this.getType() == IsoObjectType.stairsMN || this.getType() == IsoObjectType.stairsTN || this.getType() == IsoObjectType.stairsBW || this.getType() == IsoObjectType.stairsMW || this.getType() == IsoObjectType.stairsTW) {
            this.square.RemoveAllWith(IsoFlagType.attachtostairs);
         }

         byte var3 = 1;

         for(int var4 = 0; var4 < var3; ++var4) {
            InventoryItem var5 = this.square.AddWorldInventoryItem("Base.Plank", Rand.Next(-1.0F, 1.0F), Rand.Next(-1.0F, 1.0F), 0.0F);
         }
      }

   }

   public void onMouseRightClick(int var1, int var2) {
   }

   public void onMouseRightReleased() {
   }

   public void Hit(Vector2 var1, IsoObject var2, float var3) {
      if (var2 instanceof BaseVehicle) {
         this.HitByVehicle((BaseVehicle)var2, var3);
         if (this.Damage <= 0 && BrokenFences.getInstance().isBreakableObject(this)) {
            PropertyContainer var5 = this.getProperties();
            IsoDirections var4;
            if (var5.Is(IsoFlagType.collideN) && var5.Is(IsoFlagType.collideW)) {
               var4 = var2.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
            } else if (var5.Is(IsoFlagType.collideN)) {
               var4 = var2.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
            } else {
               var4 = var2.getX() >= this.getX() ? IsoDirections.W : IsoDirections.E;
            }

            BrokenFences.getInstance().destroyFence(this, var4);
         }
      }

   }

   public void Damage(float var1) {
      this.Damage = (short)((int)((double)this.Damage - (double)var1 * 0.1));
   }

   public void HitByVehicle(BaseVehicle var1, float var2) {
      short var3 = this.Damage;
      this.Damage = (short)((int)((double)this.Damage - (double)var2 * 0.1));
      BaseSoundEmitter var4 = IsoWorld.instance.getFreeEmitter((float)this.square.x + 0.5F, (float)this.square.y + 0.5F, (float)this.square.z);
      long var5 = var4.playSound("VehicleHitObject");
      var4.setParameterValue(var5, FMODManager.instance.getParameterDescription("VehicleSpeed"), var1.getCurrentSpeedKmHour());
      WorldSoundManager.instance.addSound((Object)null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
      if (this.getProperties().Is("HitByCar") && this.getSprite().getProperties().Val("DamagedSprite") != null && !this.getSprite().getProperties().Val("DamagedSprite").equals("") && this.Damage <= 90 && var3 > 90) {
         this.setSprite(IsoSpriteManager.instance.getSprite(this.getSprite().getProperties().Val("DamagedSprite")));
         if (this.getSprite().getProperties().Is("StopCar")) {
            this.getSprite().setType(IsoObjectType.isMoveAbleObject);
         } else {
            this.getSprite().setType(IsoObjectType.MAX);
         }

         if (this instanceof IsoThumpable) {
            ((IsoThumpable)this).setBlockAllTheSquare(false);
         }

         if (GameServer.bServer) {
            this.transmitUpdatedSpriteToClients();
         }

         this.getSquare().RecalcProperties();
         this.Damage = 50;
      }

      if (this.Damage <= 40 && this.getProperties().Is("HitByCar") && !BrokenFences.getInstance().isBreakableObject(this)) {
         this.getSquare().transmitRemoveItemFromSquare(this);
      }

      IsoPlayer var7 = (IsoPlayer)Type.tryCastTo(var1.getDriver(), IsoPlayer.class);
      if (var7 != null && var7.isLocalPlayer()) {
         var7.triggerMusicIntensityEvent("VehicleHitObject");
      }

   }

   public void Collision(Vector2 var1, IsoObject var2) {
      if (var2 instanceof BaseVehicle) {
         if (this.getProperties().Is("CarSlowFactor")) {
            int var3 = Integer.parseInt(this.getProperties().Val("CarSlowFactor"));
            BaseVehicle var4 = (BaseVehicle)var2;
            var4.ApplyImpulse(this, Math.abs(var4.getFudgedMass() * var4.getCurrentSpeedKmHour() * (float)var3 / 100.0F));
         }

         if (this.getProperties().Is("HitByCar")) {
            BaseVehicle var7 = (BaseVehicle)var2;
            String var8 = this.getSprite().getProperties().Val("MinimumCarSpeedDmg");
            if (var8 == null) {
               var8 = "150";
            }

            if (Math.abs(var7.getCurrentSpeedKmHour()) > (float)Integer.parseInt(var8)) {
               this.HitByVehicle(var7, Math.abs(var7.getFudgedMass() * var7.getCurrentSpeedKmHour()) / 300.0F);
               if (this.Damage <= 0 && BrokenFences.getInstance().isBreakableObject(this)) {
                  PropertyContainer var6 = this.getProperties();
                  IsoDirections var5;
                  if (var6.Is(IsoFlagType.collideN) && var6.Is(IsoFlagType.collideW)) {
                     var5 = var7.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                  } else if (var6.Is(IsoFlagType.collideN)) {
                     var5 = var7.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                  } else {
                     var5 = var7.getX() >= this.getX() ? IsoDirections.W : IsoDirections.E;
                  }

                  BrokenFences.getInstance().destroyFence(this, var5);
               }
            } else if (!this.square.getProperties().Is(IsoFlagType.collideN) && !this.square.getProperties().Is(IsoFlagType.collideW)) {
               var7.ApplyImpulse(this, Math.abs(var7.getFudgedMass() * var7.getCurrentSpeedKmHour() * 10.0F / 200.0F));
               if (var7.getCurrentSpeedKmHour() > 3.0F) {
                  var7.ApplyImpulse(this, Math.abs(var7.getFudgedMass() * var7.getCurrentSpeedKmHour() * 10.0F / 150.0F));
               }

               var7.jniSpeed = 0.0F;
            }
         }
      }

   }

   public void UnCollision(IsoObject var1) {
   }

   public float GetVehicleSlowFactor(BaseVehicle var1) {
      if (this.getProperties().Is("CarSlowFactor")) {
         int var2 = Integer.parseInt(this.getProperties().Val("CarSlowFactor"));
         return 33.0F - (float)(10 - var2);
      } else {
         return 0.0F;
      }
   }

   public IsoObject getRerouteCollide() {
      return this.rerouteCollide;
   }

   public void setRerouteCollide(IsoObject var1) {
      this.rerouteCollide = var1;
   }

   public KahluaTable getTable() {
      return this.table;
   }

   public void setTable(KahluaTable var1) {
      this.table = var1;
   }

   public void setAlpha(float var1) {
      this.setAlpha(IsoPlayer.getPlayerIndex(), var1);
   }

   public void setAlpha(int var1, float var2) {
      if (this instanceof IsoAnimal && var2 < 1.0F) {
         System.out.println("set Alpha " + var2 + "  " + var1);
      }

      this.alpha[var1] = PZMath.clamp(var2, 0.0F, 1.0F);
   }

   public void setAlphaToTarget(int var1) {
      this.setAlpha(var1, this.getTargetAlpha(var1));
   }

   public void setAlphaAndTarget(float var1) {
      int var2 = IsoPlayer.getPlayerIndex();
      this.setAlphaAndTarget(var2, var1);
   }

   public void setAlphaAndTarget(int var1, float var2) {
      this.setAlpha(var1, var2);
      this.setTargetAlpha(var1, var2);
   }

   public float getAlpha() {
      return this.getAlpha(IsoPlayer.getPlayerIndex());
   }

   public float getAlpha(int var1) {
      return this.alpha[var1];
   }

   public ArrayList<IsoSpriteInstance> getAttachedAnimSprite() {
      return this.AttachedAnimSprite;
   }

   public void setAttachedAnimSprite(ArrayList<IsoSpriteInstance> var1) {
      this.AttachedAnimSprite = var1;
   }

   public int getAttachedAnimSpriteCount() {
      return this.AttachedAnimSprite == null ? 0 : this.AttachedAnimSprite.size();
   }

   public boolean hasAttachedAnimSprites() {
      return this.AttachedAnimSprite != null && !this.AttachedAnimSprite.isEmpty();
   }

   public void addAttachedAnimSpriteInstance(IsoSpriteInstance var1) {
      if (var1 != null) {
         if (this.getAttachedAnimSprite() == null) {
            this.setAttachedAnimSprite(new ArrayList());
         }

         this.getAttachedAnimSprite().add(var1);
         if (PerformanceSettings.FBORenderChunk && Thread.currentThread() == GameWindow.GameThread) {
            this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
         }

      }
   }

   public void addAttachedAnimSprite(IsoSprite var1) {
      if (var1 != null) {
         IsoSpriteInstance var2 = IsoSpriteInstance.get(var1);
         this.addAttachedAnimSpriteInstance(var2);
      }
   }

   public void addAttachedAnimSpriteByName(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         IsoSprite var2 = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var1, 0);
         this.addAttachedAnimSprite(var2);
      }
   }

   public IsoCell getCell() {
      return IsoWorld.instance.CurrentCell;
   }

   public ArrayList<IsoSpriteInstance> getChildSprites() {
      return this.AttachedAnimSprite;
   }

   public void setChildSprites(ArrayList<IsoSpriteInstance> var1) {
      this.AttachedAnimSprite = var1;
   }

   public void clearAttachedAnimSprite() {
      this.RemoveAttachedAnims();
   }

   public ItemContainer getContainer() {
      return this.container;
   }

   public void setContainer(ItemContainer var1) {
      var1.parent = this;
      this.container = var1;
   }

   public IsoDirections getDir() {
      return this.dir;
   }

   public void setDir(IsoDirections var1) {
      this.dir = var1;
   }

   public void setDir(int var1) {
      this.dir = IsoDirections.fromIndex(var1);
   }

   public short getDamage() {
      return this.Damage;
   }

   public void setDamage(short var1) {
      this.Damage = var1;
   }

   public boolean isNoPicking() {
      return this.NoPicking;
   }

   public void setNoPicking(boolean var1) {
      this.NoPicking = var1;
   }

   public boolean isOutlineOnMouseover() {
      return this.OutlineOnMouseover;
   }

   public void setOutlineOnMouseover(boolean var1) {
      this.OutlineOnMouseover = var1;
   }

   public IsoObject getRerouteMask() {
      return this.rerouteMask;
   }

   public void setRerouteMask(IsoObject var1) {
      this.rerouteMask = var1;
   }

   public IsoSprite getSprite() {
      return this.sprite;
   }

   public void setSprite(IsoSprite var1) {
      this.sprite = var1;
      this.windRenderEffects = null;
      this.checkMoveWithWind();
      if (Thread.currentThread() == GameWindow.GameThread) {
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
      }

   }

   public void setSprite(String var1) {
      IsoSprite var2 = IsoSprite.CreateSprite(IsoSpriteManager.instance);
      var2.LoadSingleTexture(var1);
      this.setSprite(var2);
      this.tile = var1;
      this.spriteName = var1;
   }

   public void setSpriteFromName(String var1) {
      IsoSprite var2 = IsoSpriteManager.instance.getSprite(var1);
      this.setSprite(var2);
   }

   public IsoSpriteGrid getSpriteGrid() {
      IsoSprite var1 = this.getSprite();
      return var1 == null ? null : var1.getSpriteGrid();
   }

   public boolean hasSpriteGrid() {
      return this.getSpriteGrid() != null;
   }

   public float getTargetAlpha() {
      return this.getTargetAlpha(IsoPlayer.getPlayerIndex());
   }

   public void setTargetAlpha(float var1) {
      this.setTargetAlpha(IsoPlayer.getPlayerIndex(), var1);
   }

   public void setTargetAlpha(int var1, float var2) {
      this.targetAlpha[var1] = PZMath.clamp(var2, 0.0F, 1.0F);
   }

   public float getTargetAlpha(int var1) {
      return this.targetAlpha[var1];
   }

   public boolean isAlphaAndTargetZero() {
      int var1 = IsoPlayer.getPlayerIndex();
      return this.isAlphaAndTargetZero(var1);
   }

   public boolean isAlphaAndTargetZero(int var1) {
      return this.isAlphaZero(var1) && this.isTargetAlphaZero(var1);
   }

   public boolean isAlphaZero() {
      int var1 = IsoPlayer.getPlayerIndex();
      return this.isAlphaZero(var1);
   }

   public boolean isAlphaZero(int var1) {
      return this.alpha[var1] <= 0.001F;
   }

   public boolean isTargetAlphaZero(int var1) {
      return this.targetAlpha[var1] <= 0.001F;
   }

   public IsoObjectType getType() {
      return this.sprite == null ? IsoObjectType.MAX : this.sprite.getType();
   }

   public void setType(IsoObjectType var1) {
      if (this.sprite != null) {
         this.sprite.setType(var1);
      }

   }

   public void addChild(IsoObject var1) {
      if (this.Children == null) {
         this.Children = new ArrayList(4);
      }

      this.Children.add(var1);
   }

   public void debugPrintout() {
      System.out.println(this.getClass().toString());
      System.out.println(this.getObjectName());
   }

   protected void checkMoveWithWind() {
      this.checkMoveWithWind(this.sprite != null && this.sprite.isBush);
   }

   protected void checkMoveWithWind(boolean var1) {
      if (!GameServer.bServer) {
         if (this.sprite != null && this.windRenderEffects == null && this.sprite.moveWithWind) {
            if (this.getSquare() != null) {
               IsoGridSquare var2 = this.getCell().getGridSquare(this.getSquare().x - 1, this.getSquare().y, this.getSquare().z);
               IsoGridSquare var3;
               if (var2 != null) {
                  var3 = this.getCell().getGridSquare(var2.x, var2.y + 1, var2.z);
                  if (var3 != null && !var3.isExteriorCache && var3.getWall(true) != null) {
                     this.windRenderEffects = null;
                     return;
                  }
               }

               var3 = this.getCell().getGridSquare(this.getSquare().x, this.getSquare().y - 1, this.getSquare().z);
               if (var3 != null) {
                  IsoGridSquare var4 = this.getCell().getGridSquare(var3.x + 1, var3.y, var3.z);
                  if (var4 != null && !var4.isExteriorCache && var4.getWall(false) != null) {
                     this.windRenderEffects = null;
                     return;
                  }
               }
            }

            this.windRenderEffects = ObjectRenderEffects.getNextWindEffect(this.sprite.windType, var1);
         } else {
            if (this.windRenderEffects != null && (this.sprite == null || !this.sprite.moveWithWind)) {
               this.windRenderEffects = null;
            }

         }
      }
   }

   public void reset() {
      super.reset();
      this.tintr = 1.0F;
      this.tintg = 1.0F;
      this.tintb = 1.0F;
      this.name = null;
      this.table = null;
      this.rerouteCollide = null;
      int var1;
      if (this.AttachedAnimSprite != null) {
         for(var1 = 0; var1 < this.AttachedAnimSprite.size(); ++var1) {
            IsoSpriteInstance var2 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var1);
            IsoSpriteInstance.add(var2);
         }

         this.AttachedAnimSprite.clear();
      }

      if (this.wallBloodSplats != null) {
         this.wallBloodSplats.clear();
      }

      this.overlaySprite = null;
      this.overlaySpriteColor = null;
      this.customColor = null;
      if (this.container != null) {
         this.container.Items.clear();
         this.container.IncludingObsoleteItems.clear();
         this.container.setParent((IsoObject)null);
         this.container.setSourceGrid((IsoGridSquare)null);
         this.container.vehiclePart = null;
      }

      this.container = null;
      this.dir = IsoDirections.N;
      this.Damage = 100;
      this.partialThumpDmg = 0.0F;
      this.NoPicking = false;
      this.offsetX = (float)(32 * Core.TileScale);
      this.offsetY = (float)(96 * Core.TileScale);
      this.OutlineOnMouseover = false;
      this.rerouteMask = null;
      this.sprite = null;
      this.square = null;

      for(var1 = 0; var1 < 4; ++var1) {
         this.setAlphaAndTarget(var1, 1.0F);
         this.renderInfo[var1].m_layer = ObjectRenderLayer.None;
         this.renderInfo[var1].m_targetAlpha = 0.0F;
      }

      this.bNeverDoneAlpha = true;
      this.bAlphaForced = false;
      this.highlightFlags = 0;
      this.tile = null;
      this.spriteName = null;
      this.specialTooltip = false;
      this.usesExternalWaterSource = false;
      this.externalWaterSource = null;
      if (this.secondaryContainers != null) {
         for(var1 = 0; var1 < this.secondaryContainers.size(); ++var1) {
            ItemContainer var3 = (ItemContainer)this.secondaryContainers.get(var1);
            var3.Items.clear();
            var3.IncludingObsoleteItems.clear();
            var3.setParent((IsoObject)null);
            var3.setSourceGrid((IsoGridSquare)null);
            var3.vehiclePart = null;
         }

         this.secondaryContainers.clear();
      }

      this.renderYOffset = 0.0F;
      this.sx = 0.0F;
      this.windRenderEffects = null;
      this.objectRenderEffects = null;
      this.sheetRope = false;
      this.sheetRopeHealth = 100.0F;
      this.bMovedThumpable = false;
      this.isoEntityNetID = -1L;
      this.spriteModelName = null;
      this.spriteModel = null;
      this.spriteModelInit = null;
      this.lightSource = null;
      this.onOverlay = null;
   }

   public long customHashCode() {
      if (this.doNotSync) {
         return 0L;
      } else {
         try {
            long var1 = 1L;
            if (this.getObjectName() != null) {
               var1 = var1 * 3L + (long)this.getObjectName().hashCode();
            }

            if (this.name != null) {
               var1 = var1 * 2L + (long)this.name.hashCode();
            }

            if (this.container != null) {
               ++var1;
               var1 += (long)this.container.Items.size();

               for(int var3 = 0; var3 < this.container.Items.size(); ++var3) {
                  var1 += (long)(((InventoryItem)this.container.Items.get(var3)).getModule().hashCode() + ((InventoryItem)this.container.Items.get(var3)).getType().hashCode() + ((InventoryItem)this.container.Items.get(var3)).id);
               }
            }

            var1 += (long)this.square.getObjects().indexOf(this);
            return var1;
         } catch (Throwable var4) {
            DebugLog.log("ERROR: " + var4.getMessage());
            return 0L;
         }
      }
   }

   public void SetName(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getSpriteName() {
      return this.spriteName == null && this.getSprite() != null && this.getSprite().getName() != null ? this.getSprite().getName() : this.spriteName;
   }

   public String getTile() {
      return this.tile;
   }

   public boolean isCharacter() {
      return this instanceof IsoLivingCharacter;
   }

   public boolean isZombie() {
      return false;
   }

   public String getScriptName() {
      return "none";
   }

   public void AttachAnim(String var1, String var2, int var3, float var4, int var5, int var6, boolean var7, int var8, boolean var9, float var10, ColorInfo var11) {
      IsoSprite var12 = IsoSprite.CreateSpriteUsingCache(var1, var2, var3);
      var12.TintMod.set(var11);
      var12.soffX = (short)(-var5);
      var12.soffY = (short)(-var6);
      var12.Animate = var3 > 1;
      var12.Loop = var7;
      var12.DeleteWhenFinished = var9;
      var12.PlayAnim(var2);
      IsoSpriteInstance var13 = var12.def;
      var13.AnimFrameIncrease = var4;
      var13.Frame = 0.0F;
      this.addAttachedAnimSpriteInstance(var13);
   }

   public void AttachExistingAnim(IsoSprite var1, int var2, int var3, boolean var4, int var5, boolean var6, float var7, ColorInfo var8) {
      var1.TintMod.r = var8.r;
      var1.TintMod.g = var8.g;
      var1.TintMod.b = var8.b;
      var1.TintMod.a = var8.a;
      Integer var10 = var2;
      Integer var11 = var3;
      var1.soffX = (short)(-var10);
      var1.soffY = (short)(-var11);
      var1.Animate = var1.hasAnimation() && var1.CurrentAnim.Frames.size() > 1;
      var1.Loop = var4;
      var1.DeleteWhenFinished = var6;
      this.addAttachedAnimSprite(var1);
   }

   public void AttachExistingAnim(IsoSprite var1, int var2, int var3, boolean var4, int var5, boolean var6, float var7) {
      this.AttachExistingAnim(var1, var2, var3, var4, var5, var6, var7, new ColorInfo());
   }

   public void DoTooltip(ObjectTooltip var1) {
   }

   public void DoSpecialTooltip(ObjectTooltip var1, IsoGridSquare var2) {
      if (this.haveSpecialTooltip()) {
         var1.setHeight(0.0);
         LuaEventManager.triggerEvent("DoSpecialTooltip", var1, var2);
         if (var1.getHeight() == 0.0) {
            var1.hide();
         }
      }

   }

   public ItemContainer getItemContainer() {
      return this.container;
   }

   public float getOffsetX() {
      return this.offsetX;
   }

   public void setOffsetX(float var1) {
      this.offsetX = var1;
   }

   public float getOffsetY() {
      return this.offsetY;
   }

   public void setOffsetY(float var1) {
      this.offsetY = var1;
   }

   public IsoObject getRerouteMaskObject() {
      return this.rerouteMask;
   }

   public boolean HasTooltip() {
      return false;
   }

   public boolean getUsesExternalWaterSource() {
      return this.usesExternalWaterSource;
   }

   public void setUsesExternalWaterSource(boolean var1) {
      this.usesExternalWaterSource = var1;
   }

   public boolean hasExternalWaterSource() {
      return this.externalWaterSource != null;
   }

   public void doFindExternalWaterSource() {
      this.externalWaterSource = FindExternalWaterSource(this.getSquare());
   }

   public static IsoObject FindExternalWaterSource(IsoGridSquare var0) {
      return var0 == null ? null : FindExternalWaterSource(var0.getX(), var0.getY(), var0.getZ());
   }

   public static IsoObject FindExternalWaterSource(int var0, int var1, int var2) {
      IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var0, var1, var2 + 1);
      IsoObject var4 = null;
      IsoObject var5 = FindWaterSourceOnSquare(var3);
      if (var5 != null) {
         if (var5.hasWater()) {
            return var5;
         }

         var4 = var5;
      }

      for(int var6 = -1; var6 <= 1; ++var6) {
         for(int var7 = -1; var7 <= 1; ++var7) {
            if (var7 != 0 || var6 != 0) {
               var3 = IsoWorld.instance.CurrentCell.getGridSquare(var0 + var7, var1 + var6, var2 + 1);
               var5 = FindWaterSourceOnSquare(var3);
               if (var5 != null) {
                  if (var5.hasWater()) {
                     return var5;
                  }

                  if (var4 == null) {
                     var4 = var5;
                  }
               }
            }
         }
      }

      return var4;
   }

   public static IsoObject FindWaterSourceOnSquare(IsoGridSquare var0) {
      if (var0 == null) {
         return null;
      } else {
         PZArrayList var1 = var0.getObjects();

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            IsoObject var3 = (IsoObject)var1.get(var2);
            if (var3 instanceof IsoThumpable && (var3.getSprite() == null || !var3.getSprite().solidfloor) && !var3.getUsesExternalWaterSource() && var3.getWaterMax() > 0) {
               return var3;
            }
         }

         return null;
      }
   }

   public int getPipedFuelAmount() {
      if (this.sprite == null) {
         return 0;
      } else {
         double var1 = -1.0;
         if (this.hasModData() && !this.getModData().isEmpty()) {
            Object var3 = this.getModData().rawget("fuelAmount");
            if (var3 != null) {
               var1 = (Double)var3;
            }
         }

         if (this.sprite.getProperties().Is("fuelAmount")) {
            if (SandboxOptions.getInstance().FuelStationGasInfinite.getValue()) {
               return 1000;
            }

            if (var1 == -1.0 && (SandboxOptions.getInstance().AllowExteriorGenerator.getValue() && this.getSquare().haveElectricity() || IsoWorld.instance.isHydroPowerOn())) {
               float var12 = (float)SandboxOptions.getInstance().FuelStationGasMin.getValue();
               float var4 = (float)SandboxOptions.getInstance().FuelStationGasMax.getValue();
               if (var12 > var4) {
                  var12 = var4;
               }

               var1 = (double)((int)Rand.Next((float)Integer.parseInt(this.sprite.getProperties().Val("fuelAmount")) * var12, (float)Integer.parseInt(this.sprite.getProperties().Val("fuelAmount")) * var4));
               if (this.getSquare().isNoGas() || Rand.Next(100) < SandboxOptions.getInstance().FuelStationGasEmptyChance.getValue()) {
                  var1 = 0.0;
               }

               if (var1 == 0.0 && Rand.NextBool(2)) {
                  IsoDirections var5 = this.getFacing();
                  if (var5 == null) {
                     var5 = IsoDirections.E;
                  }

                  if (Rand.NextBool(2)) {
                     if (var5 == IsoDirections.E) {
                        var5 = IsoDirections.W;
                     }

                     if (var5 == IsoDirections.S) {
                        var5 = IsoDirections.N;
                     }
                  }

                  IsoGridSquare var6 = this.square.getAdjacentSquare(var5);
                  boolean var7 = var6 != null && var6.getObjects().size() < 2;
                  if (var7 && (var5 == IsoDirections.E || var5 == IsoDirections.W)) {
                     var7 = var6.getAdjacentSquare(IsoDirections.N) != null && var6.getAdjacentSquare(IsoDirections.N).getObjects().size() < 2 && var6.getAdjacentSquare(IsoDirections.S) != null && var6.getAdjacentSquare(IsoDirections.S).getObjects().size() < 2;
                  }

                  if (var7 && (var5 == IsoDirections.S || var5 == IsoDirections.N)) {
                     var7 = var6.getAdjacentSquare(IsoDirections.E) != null && var6.getAdjacentSquare(IsoDirections.E).getObjects().size() < 2 && var6.getAdjacentSquare(IsoDirections.W) != null && var6.getAdjacentSquare(IsoDirections.W).getObjects().size() < 2;
                  }

                  if (var7) {
                     String var8 = "signs_one-off_07_8";
                     int var9 = 5;
                     boolean var10 = Objects.equals(this.square.getZombiesType(), "Gas2Go");
                     boolean var11 = Objects.equals(this.square.getZombiesType(), "Fossoil");
                     if (Objects.equals(this.square.getZombiesType(), "Fossoil")) {
                        var8 = "location_shop_fossoil_01_6";
                        var9 = 8;
                     } else if (Objects.equals(this.square.getZombiesType(), "Gas2Go")) {
                        var8 = "location_shop_gas2go_01_3";
                        var9 = 8;
                     }

                     if (var5 == IsoDirections.E || var5 == IsoDirections.W) {
                        ++var9;
                     }

                     var8 = var8 + var9;
                     var6.addTileObject(var8);
                  }
               }

               this.getModData().rawset("fuelAmount", var1);
               this.transmitModData();
               return (int)var1;
            }
         }

         return (int)var1;
      }
   }

   public void setPipedFuelAmount(int var1) {
      var1 = Math.max(0, var1);
      int var2 = this.getPipedFuelAmount();
      if (var1 != var2) {
         this.getModData().rawset("fuelAmount", (double)var1);
         this.transmitModData();
      }

   }

   private boolean isWaterInfinite() {
      if (this.sprite == null) {
         return false;
      } else if (this.square != null && this.square.getRoom() != null) {
         if (!this.sprite.getProperties().Is(IsoFlagType.waterPiped)) {
            return false;
         } else if ((float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) >= (float)SandboxOptions.instance.getWaterShutModifier()) {
            return false;
         } else {
            return !this.hasModData() || !(this.getModData().rawget("canBeWaterPiped") instanceof Boolean) || !(Boolean)this.getModData().rawget("canBeWaterPiped");
         }
      } else {
         return false;
      }
   }

   private IsoObject checkExternalWaterSource() {
      if (!this.usesExternalWaterSource) {
         return null;
      } else {
         if (this.externalWaterSource == null || !this.externalWaterSource.hasWater()) {
            this.doFindExternalWaterSource();
         }

         return this.externalWaterSource;
      }
   }

   public int getWaterAmount() {
      if (this.sprite == null) {
         return 0;
      } else if (this.usesExternalWaterSource) {
         if (this.isWaterInfinite()) {
            return 10000;
         } else {
            IsoObject var3 = this.checkExternalWaterSource();
            return var3 == null ? 0 : var3.getWaterAmount();
         }
      } else if (this.isWaterInfinite()) {
         return 10000;
      } else {
         if (this.hasModData() && !this.getModData().isEmpty()) {
            Object var1 = this.getModData().rawget("waterAmount");
            if (var1 != null) {
               if (var1 instanceof Double) {
                  return (int)Math.max(0.0, (Double)var1);
               }

               if (var1 instanceof String) {
                  return Math.max(0, Integer.parseInt((String)var1));
               }

               return 0;
            }
         }

         if (this.square != null && !this.square.getProperties().Is(IsoFlagType.water) && this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.solidfloor) && this.square.getPuddlesInGround() > 0.09F) {
            return (int)(this.square.getPuddlesInGround() * 10.0F);
         } else if (!this.sprite.Properties.Is("waterAmount")) {
            return 0;
         } else {
            int var2 = Integer.parseInt(this.sprite.getProperties().Val("waterAmount"));
            return var2;
         }
      }
   }

   public void setWaterAmount(int var1) {
      if (this.usesExternalWaterSource) {
         if (!this.isWaterInfinite()) {
            IsoObject var4 = this.checkExternalWaterSource();
            if (var4 != null) {
               var4.setWaterAmount(var1);
            }

         }
      } else {
         var1 = Math.max(0, var1);
         int var2 = this.getWaterAmount();
         if (var1 != var2) {
            boolean var3 = true;
            if (this.hasModData() && !this.getModData().isEmpty()) {
               var3 = this.getModData().rawget("waterAmount") == null;
            }

            if (var3) {
               this.getModData().rawset("waterMax", (double)var2);
            }

            this.getModData().rawset("waterAmount", (double)var1);
            if (var1 <= 0) {
               this.setTaintedWater(false);
            }

            LuaEventManager.triggerEvent("OnWaterAmountChange", this, var2);
         }

      }
   }

   public int getWaterMax() {
      if (this.sprite == null) {
         return 0;
      } else if (this.usesExternalWaterSource) {
         if (this.isWaterInfinite()) {
            return 10000;
         } else {
            IsoObject var2 = this.checkExternalWaterSource();
            return var2 != null ? var2.getWaterMax() : 0;
         }
      } else if (this.isWaterInfinite()) {
         return 10000;
      } else {
         if (this.hasModData() && !this.getModData().isEmpty()) {
            Object var1 = this.getModData().rawget("waterMax");
            if (var1 != null) {
               if (var1 instanceof Double) {
                  return (int)Math.max(0.0, (Double)var1);
               }

               if (var1 instanceof String) {
                  return Math.max(0, Integer.parseInt((String)var1));
               }

               return 0;
            }
         }

         if (this.square != null && !this.square.getProperties().Is(IsoFlagType.water) && this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.solidfloor) && this.square.getPuddlesInGround() > 0.09F) {
            return (int)(this.square.getPuddlesInGround() * 10.0F);
         } else if (this.sprite.Properties.Is("waterMaxAmount")) {
            return Integer.parseInt(this.sprite.getProperties().Val("waterMaxAmount"));
         } else {
            return this.sprite.Properties.Is("waterAmount") ? Integer.parseInt(this.sprite.getProperties().Val("waterAmount")) : 0;
         }
      }
   }

   public int useWater(int var1) {
      if (this.sprite == null) {
         return 0;
      } else {
         int var2 = this.getWaterAmount();
         boolean var3 = false;
         int var4;
         if (var2 >= var1) {
            var4 = var1;
         } else {
            var4 = var2;
         }

         if (this.square != null && this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.solidfloor) && this.square.getPuddlesInGround() > 0.09F) {
            return var4;
         } else {
            if (!this.usesExternalWaterSource) {
               if (this.sprite.getProperties().Is(IsoFlagType.water)) {
                  return var4;
               }

               if (this.isWaterInfinite()) {
                  return var4;
               }
            }

            this.setWaterAmount(var2 - var4);
            return var4;
         }
      }
   }

   public boolean hasWater() {
      if (this.square != null && this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.solidfloor) && this.square.getPuddlesInGround() > 0.09F) {
         return true;
      } else {
         return this.getWaterAmount() > 0;
      }
   }

   public boolean isTaintedWater() {
      if (this.square != null && this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.solidfloor) && this.square.getPuddlesInGround() > 0.09F) {
         return true;
      } else {
         if (this.hasModData()) {
            Object var1 = this.getModData().rawget("taintedWater");
            if (var1 instanceof Boolean) {
               return (Boolean)var1;
            }
         }

         if (this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.taintedWater)) {
            return true;
         } else {
            return this.getFluidContainer() != null && this.getFluidContainer().getPrimaryFluid() == Fluid.TaintedWater;
         }
      }
   }

   public void setTaintedWater(boolean var1) {
      this.getModData().rawset("taintedWater", var1);
   }

   public InventoryItem replaceItem(InventoryItem var1) {
      String var2 = null;
      InventoryItem var3 = null;
      if (var1 != null) {
         if (var1.hasReplaceType(this.getObjectName())) {
            var2 = var1.getReplaceType(this.getObjectName());
         } else if (var1.hasReplaceType("WaterSource")) {
            var2 = var1.getReplaceType("WaterSource");
         }
      }

      if (var2 != null) {
         var3 = var1.getContainer().AddItem(InventoryItemFactory.CreateItem(var2));
         if (var1.getContainer().getParent() instanceof IsoGameCharacter) {
            IsoGameCharacter var4 = (IsoGameCharacter)var1.getContainer().getParent();
            if (var4.getPrimaryHandItem() == var1) {
               var4.setPrimaryHandItem(var3);
            }

            if (var4.getSecondaryHandItem() == var1) {
               var4.setSecondaryHandItem(var3);
            }
         }

         var1.getContainer().Remove(var1);
      }

      return var3;
   }

   /** @deprecated */
   @Deprecated
   public void useItemOn(InventoryItem var1) {
      String var2 = null;
      if (var1 != null) {
         if (var1.hasReplaceType(this.getObjectName())) {
            var2 = var1.getReplaceType(this.getObjectName());
         } else if (var1.hasReplaceType("WaterSource")) {
            var2 = var1.getReplaceType("WaterSource");
            this.useWater(10);
         }
      }

      if (var2 != null) {
         InventoryItem var3 = var1.getContainer().AddItem(InventoryItemFactory.CreateItem(var2));
         var1.setUses(var1.getUses() - 1);
         if (var1.getUses() <= 0 && var1.getContainer() != null) {
            var1.getContainer().Items.remove(var1);
         }
      }

   }

   public boolean isCanPath() {
      return this.square != null && this.sprite != null && (this.sprite.getProperties().Is(IsoFlagType.canPathW) || this.sprite.getProperties().Is(IsoFlagType.canPathN));
   }

   public float getX() {
      return (float)this.square.getX();
   }

   public float getY() {
      return (float)this.square.getY();
   }

   public float getZ() {
      return (float)this.square.getZ();
   }

   public boolean onMouseLeftClick(int var1, int var2) {
      return false;
   }

   public PropertyContainer getProperties() {
      return this.sprite == null ? null : this.sprite.getProperties();
   }

   public void RemoveAttachedAnims() {
      if (this.AttachedAnimSprite != null && !this.AttachedAnimSprite.isEmpty()) {
         for(int var1 = 0; var1 < this.AttachedAnimSprite.size(); ++var1) {
            IsoSpriteInstance var2 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var1);
            var2.Dispose();
            IsoSpriteInstance.add(var2);
         }

         this.AttachedAnimSprite.clear();
         if (PerformanceSettings.FBORenderChunk && Thread.currentThread() == GameWindow.GameThread) {
            this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
         }

      }
   }

   public void RemoveAttachedAnim(int var1) {
      if (this.AttachedAnimSprite != null) {
         if (var1 >= 0 && var1 < this.AttachedAnimSprite.size()) {
            ((IsoSpriteInstance)this.AttachedAnimSprite.get(var1)).Dispose();
            IsoSpriteInstance var2 = (IsoSpriteInstance)this.AttachedAnimSprite.remove(var1);
            IsoSpriteInstance.add(var2);
            if (PerformanceSettings.FBORenderChunk && Thread.currentThread() == GameWindow.GameThread) {
               this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
            }

         }
      }
   }

   public Vector2 getFacingPosition(Vector2 var1) {
      if (this.square == null) {
         return var1.set(0.0F, 0.0F);
      } else {
         PropertyContainer var2 = this.getProperties();
         if (var2 != null) {
            if (this.getType() == IsoObjectType.wall) {
               if (var2.Is(IsoFlagType.collideN) && var2.Is(IsoFlagType.collideW)) {
                  return var1.set(this.getX(), this.getY());
               }

               if (var2.Is(IsoFlagType.collideN)) {
                  return var1.set(this.getX() + 0.5F, this.getY());
               }

               if (var2.Is(IsoFlagType.collideW)) {
                  return var1.set(this.getX(), this.getY() + 0.5F);
               }

               if (var2.Is(IsoFlagType.DoorWallN)) {
                  return var1.set(this.getX() + 0.5F, this.getY());
               }

               if (var2.Is(IsoFlagType.DoorWallW)) {
                  return var1.set(this.getX(), this.getY() + 0.5F);
               }
            } else {
               if (var2.Is(IsoFlagType.attachedN)) {
                  return var1.set(this.getX() + 0.5F, this.getY());
               }

               if (var2.Is(IsoFlagType.attachedS)) {
                  return var1.set(this.getX() + 0.5F, this.getY() + 1.0F);
               }

               if (var2.Is(IsoFlagType.attachedW)) {
                  return var1.set(this.getX(), this.getY() + 0.5F);
               }

               if (var2.Is(IsoFlagType.attachedE)) {
                  return var1.set(this.getX() + 1.0F, this.getY() + 0.5F);
               }
            }
         }

         return var1.set(this.getX() + 0.5F, this.getY() + 0.5F);
      }
   }

   public Vector2 getFacingPositionAlt(Vector2 var1) {
      return this.getFacingPosition(var1);
   }

   public float getRenderYOffset() {
      return this.renderYOffset;
   }

   public void setRenderYOffset(float var1) {
      this.renderYOffset = var1;
      this.sx = 0.0F;
   }

   public boolean isTableSurface() {
      PropertyContainer var1 = this.getProperties();
      return var1 != null ? var1.isTable() : false;
   }

   public boolean isTableTopObject() {
      PropertyContainer var1 = this.getProperties();
      return var1 != null ? var1.isTableTop() : false;
   }

   public boolean getIsSurfaceNormalOffset() {
      PropertyContainer var1 = this.getProperties();
      return var1 != null ? var1.isSurfaceOffset() : false;
   }

   public float getSurfaceNormalOffset() {
      float var1 = 0.0F;
      PropertyContainer var2 = this.getProperties();
      if (var2.isSurfaceOffset()) {
         var1 = (float)var2.getSurface();
      }

      return var1;
   }

   public float getSurfaceOffsetNoTable() {
      float var1 = 0.0F;
      int var2 = 0;
      PropertyContainer var3 = this.getProperties();
      if (var3 != null) {
         var1 = (float)var3.getSurface();
         var2 = var3.getItemHeight();
      }

      return var1 + this.getRenderYOffset() + (float)var2;
   }

   public float getSurfaceOffset() {
      float var1 = 0.0F;
      if (this.isTableSurface()) {
         PropertyContainer var2 = this.getProperties();
         if (var2 != null) {
            var1 = (float)var2.getSurface();
         }
      }

      return var1;
   }

   public boolean isStairsNorth() {
      return this.getType() == IsoObjectType.stairsTN || this.getType() == IsoObjectType.stairsMN || this.getType() == IsoObjectType.stairsBN;
   }

   public boolean isStairsWest() {
      return this.getType() == IsoObjectType.stairsTW || this.getType() == IsoObjectType.stairsMW || this.getType() == IsoObjectType.stairsBW;
   }

   public boolean isStairsObject() {
      return this.isStairsNorth() || this.isStairsWest();
   }

   public boolean isHoppable() {
      return this.sprite != null && (this.sprite.getProperties().Is(IsoFlagType.HoppableN) || this.sprite.getProperties().Is(IsoFlagType.HoppableW));
   }

   public boolean isNorthHoppable() {
      return this.sprite != null && this.isHoppable() && this.sprite.getProperties().Is(IsoFlagType.HoppableN);
   }

   public boolean haveSheetRope() {
      return IsoWindow.isTopOfSheetRopeHere(this.square, this.isNorthHoppable());
   }

   public int countAddSheetRope() {
      return IsoWindow.countAddSheetRope(this.square, this.isNorthHoppable());
   }

   public boolean canAddSheetRope() {
      return IsoWindow.canAddSheetRope(this.square, this.isNorthHoppable());
   }

   public boolean addSheetRope(IsoPlayer var1, String var2) {
      return !this.canAddSheetRope() ? false : IsoWindow.addSheetRope(var1, this.square, this.isNorthHoppable(), var2);
   }

   public boolean removeSheetRope(IsoPlayer var1) {
      return this.haveSheetRope() ? IsoWindow.removeSheetRope(var1, this.square, this.isNorthHoppable()) : false;
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.renderModel(var1 + 0.5F, var2 + 0.5F, var3, var4)) {
         this.updateRenderInfoForObjectPicker(var1, var2, var3, var4);
      } else if (!this.isSpriteInvisible()) {
         this.prepareToRender(var4);
         int var8 = IsoCamera.frameState.playerIndex;
         int var9;
         float var10;
         float var11;
         float var12;
         Texture var13;
         if (this.shouldDrawMainSprite()) {
            this.sprite.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink());
            if (!PerformanceSettings.FBORenderChunk && this.isOutlineHighlight(var8) && !this.isOutlineHlAttached(var8) && IsoObject.OutlineShader.instance.StartShader()) {
               var9 = this.outlineHighlightCol[var8];
               var10 = Color.getRedChannelFromABGR(var9);
               var11 = Color.getGreenChannelFromABGR(var9);
               var12 = Color.getBlueChannelFromABGR(var9);
               IsoObject.OutlineShader.instance.setOutlineColor(var10, var11, var12, this.isOutlineHlBlink(var8) ? Core.blinkAlpha : 1.0F);
               var13 = this.sprite.getTextureForCurrentFrame(this.dir);
               if (var13 != null) {
                  IsoObject.OutlineShader.instance.setStepSize(this.outlineThickness, var13.getWidthHW(), var13.getHeightHW());
               }

               this.sprite.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink());
               IndieGL.EndShader();
            }
         }

         if (this.isSatChair()) {
            var5 = false;
         }

         this.renderAttachedAndOverlaySpritesInternal(this.dir, var1, var2, var3, var4, var5, var6, var7, (Consumer)null);
         if (!PerformanceSettings.FBORenderChunk && this.isOutlineHighlight(var8) && this.isOutlineHlAttached(var8) && IsoObject.OutlineShader.instance.StartShader()) {
            var9 = this.outlineHighlightCol[var8];
            var10 = Color.getRedChannelFromABGR(var9);
            var11 = Color.getGreenChannelFromABGR(var9);
            var12 = Color.getBlueChannelFromABGR(var9);
            IsoObject.OutlineShader.instance.setOutlineColor(var10, var11, var12, this.isOutlineHlBlink(var8) ? Core.blinkAlpha : 1.0F);
            var13 = this.sprite.getTextureForCurrentFrame(this.dir);
            if (var13 != null) {
               IsoObject.OutlineShader.instance.setStepSize(this.outlineThickness, var13.getWidthHW(), var13.getHeightHW());
            }

            if (this.getProperties().Is(IsoFlagType.unlit)) {
               stCol.r = 1.0F;
               stCol.g = 1.0F;
               stCol.b = 1.0F;
            }

            if (this.shouldDrawMainSprite()) {
               this.sprite.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink());
            }

            this.renderAttachedAndOverlaySpritesInternal(this.dir, var1, var2, var3, var4, var5, var6, var7, (Consumer)null);
            IndieGL.EndShader();
         }

         if (!this.bAlphaForced && this.isUpdateAlphaDuringRender()) {
            this.updateAlpha(var8);
         }

         this.debugRenderItemHeight(var1, var2, var3);
         this.debugRenderSurface(var1, var2, var3);
      }
   }

   private void debugRenderItemHeight(float var1, float var2, float var3) {
      if (DebugOptions.instance.IsoSprite.ItemHeight.getValue()) {
         if (this.square != null && IsoCamera.frameState.CamCharacterSquare != null && this.square.z == IsoCamera.frameState.CamCharacterSquare.z) {
            int var4 = this.sprite.getProperties().getItemHeight();
            if (var4 > 0) {
               int var5 = 0;
               if (this.sprite != null && this.sprite.getProperties().getSurface() > 0 && this.sprite.getProperties().isSurfaceOffset()) {
                  var5 = this.sprite.getProperties().getSurface();
               }

               LineDrawer.addRectYOffset(var1, var2, var3, 1.0F, 1.0F, (int)this.getRenderYOffset() + var5 + var4, 0.66F, 0.66F, 0.66F);
            }

         }
      }
   }

   private void debugRenderSurface(float var1, float var2, float var3) {
      if (DebugOptions.instance.IsoSprite.Surface.getValue()) {
         if (this.square != null && IsoCamera.frameState.CamCharacterSquare != null && this.square.z == IsoCamera.frameState.CamCharacterSquare.z) {
            int var4 = 0;
            if (this.sprite != null && this.sprite.getProperties().getSurface() > 0 && !this.sprite.getProperties().isSurfaceOffset()) {
               var4 = this.sprite.getProperties().getSurface();
            }

            if (var4 > 0) {
               LineDrawer.addRectYOffset(var1, var2, var3, 1.0F, 1.0F, (int)this.getRenderYOffset() + var4, 1.0F, 1.0F, 1.0F);
            }

         }
      }
   }

   public void renderFloorTile(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7, Consumer<TextureDraw> var8, Consumer<TextureDraw> var9) {
      if (this.renderModel(var1 + 0.5F, var2 + 0.5F, var3, var4)) {
         this.updateRenderInfoForObjectPicker(var1, var2, var3, var4);
      } else if (!this.isSpriteInvisible()) {
         this.prepareToRender(var4);
         boolean var10 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
         FloorShaper var11 = (FloorShaper)Type.tryCastTo(var8, FloorShaper.class);
         FloorShaper var12 = (FloorShaper)Type.tryCastTo(var9, FloorShaper.class);
         if ((var11 != null || var12 != null) && var10 && this.getHighlightColor() != null) {
            ColorInfo var13 = this.getHighlightColor();
            float var14 = var13.a * (this.isBlink() ? Core.blinkAlpha : 1.0F);
            int var15 = Color.colorToABGR(var13.r, var13.g, var13.b, var14);
            if (var11 != null) {
               var11.setTintColor(var15);
            }

            if (var12 != null) {
               var12.setTintColor(var15);
            }
         }

         if (this.shouldDrawMainSprite()) {
            if (this == this.square.getFloor()) {
               FBORenderCell.instance.renderSeamFix1_Floor(this, var1, var2, var3, stCol, var8);
               FBORenderCell.instance.renderSeamFix2_Floor(this, var1, var2, var3, stCol, var8);
            }

            if (!PerformanceSettings.FBORenderChunk && this.square.getWater() != null && this.square.getWater().isbShore()) {
               IndieGL.glBlendFunc(770, 771);
            }

            this.sprite.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink(), var8);
         }

         this.renderAttachedAndOverlaySpritesInternal(this.dir, var1, var2, var3, var4, var5, var6, var7, var9);
         if (var11 != null) {
            var11.setTintColor(0);
         }

         if (var12 != null) {
            var12.setTintColor(0);
         }

      }
   }

   public void renderWallTile(IsoDirections var1, float var2, float var3, float var4, ColorInfo var5, boolean var6, boolean var7, Shader var8, Consumer<TextureDraw> var9) {
      if (this.renderModel(var2 + 0.5F, var3 + 0.5F, var4, var5)) {
         this.updateRenderInfoForObjectPicker(var2, var3, var4, var5);
         this.sx = 0.0F;
      } else if (!this.isSpriteInvisible()) {
         this.renderWallTileOnly(var1, var2, var3, var4, var5, var8, var9);
         this.renderAttachedAndOverlaySpritesInternal(var1, var2, var3, var4, var5, var6, var7, var8, var9);
         int var10 = IsoCamera.frameState.playerIndex;
         if (!PerformanceSettings.FBORenderChunk && this.isOutlineHighlight(var10) && !this.isOutlineHlAttached(var10) && IsoObject.OutlineShader.instance.StartShader()) {
            int var11 = this.outlineHighlightCol[var10];
            float var12 = Color.getRedChannelFromABGR(var11);
            float var13 = Color.getGreenChannelFromABGR(var11);
            float var14 = Color.getBlueChannelFromABGR(var11);
            IsoObject.OutlineShader.instance.setOutlineColor(var12, var13, var14, this.isOutlineHlBlink(var10) ? Core.blinkAlpha : 1.0F);
            Texture var15 = this.sprite.getTextureForCurrentFrame(this.dir);
            if (var15 != null) {
               IsoObject.OutlineShader.instance.setStepSize(this.outlineThickness, var15.getWidthHW(), var15.getHeightHW());
            }

            this.sprite.render(this, var2, var3, var4, var1, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink());
            IndieGL.EndShader();
         }

      }
   }

   public void renderWallTileDepth(IsoDirections var1, boolean var2, boolean var3, boolean var4, int var5, float var6, float var7, float var8, ColorInfo var9, Shader var10, Consumer<TextureDraw> var11) {
      if (!this.isSpriteInvisible()) {
         this.prepareToRender(var9);
         boolean var12 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
         WallShaper var13 = (WallShaper)Type.tryCastTo(var11, WallShaper.class);
         if (var13 != null && var12 && this.getHighlightColor() != null) {
            ColorInfo var14 = this.getHighlightColor();
            float var15 = var14.a * (this.isBlink() ? Core.blinkAlpha : 1.0F);
            int var16 = Color.colorToABGR(var14.r, var14.g, var14.b, var15);
            var13.setTintColor(var16);
         }

         if (this.shouldDrawMainSprite()) {
            this.sprite.renderDepth(this, var1, var2, var3, var4, var5, var6, var7, var8, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink(), var11);
         }

         if (var13 != null) {
            var13.setTintColor(0);
         }

      }
   }

   public void renderWallTileOnly(IsoDirections var1, float var2, float var3, float var4, ColorInfo var5, Shader var6, Consumer<TextureDraw> var7) {
      if (!this.isSpriteInvisible()) {
         this.prepareToRender(var5);
         boolean var8 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
         WallShaper var9 = (WallShaper)Type.tryCastTo(var7, WallShaper.class);
         float var11;
         if (var9 != null && var8 && this.getHighlightColor() != null) {
            ColorInfo var10 = this.getHighlightColor();
            var11 = var10.a * (this.isBlink() ? Core.blinkAlpha : 1.0F);
            int var12 = Color.colorToABGR(var10.r, var10.g, var10.b, var11);
            var9.setTintColor(var12);
         }

         if (this.shouldDrawMainSprite()) {
            if (var6 != null) {
               IndieGL.pushShader(var6);
               if (PerformanceSettings.FBORenderChunk && var6 == IsoGridSquare.CircleStencilShader.instance) {
                  float var16 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var2, var3, var4).depthStart;
                  var11 = var4 + 1.0F;
                  float var17 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var2 + 1.0F, var3 + 1.0F, var11).depthStart;
                  if (!FBORenderCell.instance.bRenderTranslucentOnly) {
                     byte var13 = 8;
                     IsoDepthHelper.Results var14 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var13), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var13), PZMath.fastfloor(var2 / (float)var13), PZMath.fastfloor(var3 / (float)var13), PZMath.fastfloor(var4));
                     float var15 = var14.depthStart;
                     var16 -= var15;
                     var17 -= var15;
                  }

                  IndieGL.shaderSetValue(var6, "zDepthBlendZ", var17);
                  IndieGL.shaderSetValue(var6, "zDepthBlendToZ", var16);
               }
            }

            if (PerformanceSettings.FBORenderChunk && !FBORenderCell.instance.bRenderTranslucentOnly && var6 != IsoGridSquare.CircleStencilShader.instance) {
               FBORenderCell.instance.renderSeamFix1_Wall(this, var2, var3, var4, stCol, var7);
               FBORenderCell.instance.renderSeamFix2_Wall(this, var2, var3, var4, stCol, var7);
            }

            this.sprite.render(this, var2, var3, var4, var1, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, stCol, !this.isBlink(), var7);
            if (var6 != null) {
               IndieGL.popShader(var6);
            }
         }

         if (var9 != null) {
            var9.setTintColor(0);
         }

      }
   }

   private boolean shouldDrawMainSprite() {
      if (this.sprite == null) {
         return false;
      } else {
         return DebugOptions.instance.Terrain.RenderTiles.RenderSprites.getValue();
      }
   }

   public void renderAttachedAndOverlaySprites(IsoDirections var1, float var2, float var3, float var4, ColorInfo var5, boolean var6, boolean var7, Shader var8, Consumer<TextureDraw> var9) {
      if (!this.isSpriteInvisible()) {
         this.renderAttachedAndOverlaySpritesInternal(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      }
   }

   private void renderAttachedAndOverlaySpritesInternal(IsoDirections var1, float var2, float var3, float var4, ColorInfo var5, boolean var6, boolean var7, Shader var8, Consumer<TextureDraw> var9) {
      boolean var10 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
      if (var10) {
         var5 = stCol;
      }

      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue() && !FBORenderObjectHighlight.getInstance().isRendering()) {
         var5.set(1.0F, 1.0F, 1.0F, var5.a);
      }

      this.renderOverlaySprites(var2, var3, var4, var5, var8, var9);
      if (var6) {
         this.renderAttachedSprites(var1, var2, var3, var4, var5, var7, var8, var9);
      }

   }

   private void prepareToRender(ColorInfo var1) {
      stCol.set(var1);
      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         stCol.set(1.0F, 1.0F, 1.0F, stCol.a);
      }

      boolean var2 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
      ColorInfo var10000;
      if (var2) {
         stCol.set(this.getHighlightColor());
         if (this.isBlink()) {
            var10000 = stCol;
            var10000.a *= Core.blinkAlpha;
         }

         stCol.r = var1.r * (1.0F - stCol.a) + this.getHighlightColor().r * stCol.a;
         stCol.g = var1.g * (1.0F - stCol.a) + this.getHighlightColor().g * stCol.a;
         stCol.b = var1.b * (1.0F - stCol.a) + this.getHighlightColor().b * stCol.a;
         stCol.a = var1.a;
      }

      float var3;
      if (this.customColor != null) {
         var3 = this.square != null ? this.square.getDarkMulti(IsoPlayer.getPlayerIndex()) : 1.0F;
         if (var2) {
            var10000 = stCol;
            var10000.r *= this.customColor.r * var3;
            var10000 = stCol;
            var10000.g *= this.customColor.g * var3;
            var10000 = stCol;
            var10000.b *= this.customColor.b * var3;
         } else {
            stCol.r = this.customColor.r * var3;
            stCol.g = this.customColor.g * var3;
            stCol.b = this.customColor.b * var3;
         }
      }

      float var4;
      float var5;
      if (this.sprite != null && this.sprite.forceAmbient && !DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         var3 = rmod * this.tintr;
         var4 = gmod * this.tintg;
         var5 = bmod * this.tintb;
         if (!var2) {
            stCol.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * var3;
            stCol.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * var4;
            stCol.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * var5;
         }
      }

      int var14 = IsoPlayer.getPlayerIndex();
      var4 = IsoCamera.frameState.CamCharacterX;
      var5 = IsoCamera.frameState.CamCharacterY;
      float var6 = IsoCamera.frameState.CamCharacterZ;
      if (IsoWorld.instance.CurrentCell.IsPlayerWindowPeeking(var14)) {
         IsoPlayer var7 = IsoPlayer.players[var14];
         IsoDirections var8 = IsoDirections.fromAngle(var7.getForwardDirection());
         if (var8 == IsoDirections.N || var8 == IsoDirections.NW) {
            --var5;
         }

         if (var8 == IsoDirections.W || var8 == IsoDirections.NW) {
            --var4;
         }
      }

      if (this == IsoCamera.getCameraCharacter()) {
         this.setAlphaAndTarget(var14, 1.0F);
      }

      lastRenderedRendered = lastRendered;
      lastRendered = this;
      if (this.sprite != null && !(this instanceof IsoPhysicsObject) && IsoCamera.getCameraCharacter() != null) {
         boolean var15 = this instanceof IsoWindow || this.sprite.getType() == IsoObjectType.doorW || this.sprite.getType() == IsoObjectType.doorN;
         if (this.sprite.getProperties().Is("GarageDoor")) {
            var15 = false;
         }

         if (!var15 && ((float)this.square.getX() > var4 || (float)this.square.getY() > var5) && PZMath.fastfloor(var6) <= this.square.getZ()) {
            boolean var16 = false;
            float var9 = 0.2F;
            boolean var10 = (this.sprite.cutW || this.sprite.getProperties().Is(IsoFlagType.doorW)) && (float)this.square.getX() > var4;
            boolean var11 = (this.sprite.cutN || this.sprite.getProperties().Is(IsoFlagType.doorN)) && (float)this.square.getY() > var5;
            if (var10 && this.square.getProperties().Is(IsoFlagType.WallSE) && (float)this.square.getY() <= var5) {
               var10 = false;
            }

            if (!var10 && !var11) {
               boolean var12 = this.getType() == IsoObjectType.WestRoofB || this.getType() == IsoObjectType.WestRoofM || this.getType() == IsoObjectType.WestRoofT;
               boolean var13 = var12 && PZMath.fastfloor(var6) == this.square.getZ() && this.square.getBuilding() == null;
               if (var13 && IsoWorld.instance.CurrentCell.CanBuildingSquareOccludePlayer(this.square, var14)) {
                  var16 = true;
                  var9 = 0.05F;
               }
            } else {
               var16 = true;
            }

            if (this.sprite.getProperties().Is(IsoFlagType.halfheight)) {
               var16 = false;
            }

            if (var16) {
               if (var11 && this.sprite.getProperties().Is(IsoFlagType.HoppableN)) {
                  var9 = 0.25F;
               }

               if (var10 && this.sprite.getProperties().Is(IsoFlagType.HoppableW)) {
                  var9 = 0.25F;
               }

               if (!PerformanceSettings.FBORenderChunk) {
                  if (this.bAlphaForced) {
                     if (this.getTargetAlpha(var14) == 1.0F) {
                        this.setAlphaAndTarget(var14, 0.99F);
                     }
                  } else {
                     this.setTargetAlpha(var14, var9);
                  }
               }

               LowLightingQualityHack = true;
               this.NoPicking = this.rerouteMask == null && !(this instanceof IsoThumpable) && !(this instanceof IsoWindowFrame) && !this.sprite.getProperties().Is(IsoFlagType.doorN) && !this.sprite.getProperties().Is(IsoFlagType.doorW) && !this.sprite.getProperties().Is(IsoFlagType.HoppableN) && !this.sprite.getProperties().Is(IsoFlagType.HoppableW);
            } else {
               this.NoPicking = false;
            }
         } else {
            this.NoPicking = false;
         }
      }

      if (this == IsoCamera.getCameraCharacter()) {
         this.setTargetAlpha(var14, 1.0F);
      }

   }

   protected float getAlphaUpdateRateDiv() {
      float var1 = 14.0F;
      return var1;
   }

   protected float getAlphaUpdateRateMul() {
      float var1 = 0.25F;
      if (this.square != null && this.square.room != null) {
         var1 *= 2.0F;
      }

      return var1;
   }

   protected boolean isUpdateAlphaEnabled() {
      return true;
   }

   protected boolean isUpdateAlphaDuringRender() {
      return true;
   }

   protected final void updateAlpha() {
      if (!(this instanceof IsoAnimal)) {
         if (!GameServer.bServer) {
            for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
               if (IsoPlayer.players[var1] != null) {
                  this.updateAlpha(var1);
               }
            }

         }
      }
   }

   protected final void updateAlpha(int var1) {
      if (!GameServer.bServer) {
         float var2 = this.getAlphaUpdateRateMul();
         float var3 = this.getAlphaUpdateRateDiv();
         this.updateAlpha(var1, var2, var3);
      }
   }

   protected void updateAlpha(int var1, float var2, float var3) {
      if (this.isUpdateAlphaEnabled()) {
         if (!DebugOptions.instance.Character.Debug.UpdateAlpha.getValue()) {
            this.setAlphaToTarget(var1);
         } else {
            if (this.bNeverDoneAlpha) {
               this.setAlpha(0.0F);
               this.bNeverDoneAlpha = false;
            }

            if (DebugOptions.instance.Character.Debug.UpdateAlphaEighthSpeed.getValue()) {
               var2 /= 8.0F;
               var3 *= 8.0F;
            }

            float var4 = GameTime.getInstance().getMultiplier();
            float var5 = var4 * 0.28F;
            float var6 = this.getAlpha(var1);
            float var7 = this.targetAlpha[var1];
            if (var6 < var7) {
               var6 += var5 * var2;
               if (var6 > var7) {
                  var6 = var7;
               }
            } else if (var6 > var7) {
               var6 -= var5 / var3;
               if (var6 < var7) {
                  var6 = var7;
               }
            }

            this.setAlpha(var1, var6);
         }
      }
   }

   private void renderOverlaySprites(float var1, float var2, float var3, ColorInfo var4, Shader var5, Consumer<TextureDraw> var6) {
      if (this.getOverlaySprite() != null && DebugOptions.instance.Terrain.RenderTiles.OverlaySprites.getValue()) {
         if (PerformanceSettings.FBORenderChunk) {
            IndieGL.glDepthMask(true);
         }

         ColorInfo var7 = stCol2;
         var7.set(var4);
         if (this.overlaySpriteColor != null) {
            var7.set(this.overlaySpriteColor);
         }

         if (var6 != CutawayAttachedModifier.instance) {
            var6 = null;
         }

         if (var7.a != 1.0F && this.overlaySprite.def != null && this.overlaySprite.def.bCopyTargetAlpha) {
            int var8 = IsoPlayer.getPlayerIndex();
            float var9 = this.alpha[var8];
            float[] var10000 = this.alpha;
            var10000[var8] *= var7.a;
            this.getOverlaySprite().render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, var7, true, var6);
            this.alpha[var8] = var9;
         } else {
            this.getOverlaySprite().render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, var7, true, var6);
         }

         if (PerformanceSettings.FBORenderChunk) {
            IndieGL.glDepthMask(true);
         }

      }
   }

   private void renderAttachedSprites(IsoDirections var1, float var2, float var3, float var4, ColorInfo var5, boolean var6, Shader var7, Consumer<TextureDraw> var8) {
      if (PerformanceSettings.FBORenderChunk) {
         boolean var9 = this.sprite == null ? false : this.sprite.solidfloor;
         IndieGL.glDepthMask(!var9);
      }

      int var10;
      int var15;
      if (this.AttachedAnimSprite != null && DebugOptions.instance.Terrain.RenderTiles.AttachedAnimSprites.getValue()) {
         var15 = this.AttachedAnimSprite.size();

         for(var10 = 0; var10 < var15; ++var10) {
            IsoSpriteInstance var11 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var10);
            if (!var6 || !var11.parentSprite.Properties.Is(IsoFlagType.NoWallLighting)) {
               float var12 = var5.a;
               IndieGL.shaderSetValue(var7, "floorLayer", 1);
               var5.a = var11.alpha;
               Object var13 = var8;
               if (var8 == WallShaperW.instance) {
                  if (var11.parentSprite.getProperties().Is(IsoFlagType.attachedN)) {
                     Texture var14 = var11.parentSprite.getTextureForCurrentFrame(var1);
                     if (var14 != null && var14.getWidth() < 32 * Core.TileScale) {
                        continue;
                     }
                  }

                  if (var11.parentSprite.getProperties().Is(IsoFlagType.attachedW)) {
                     var13 = WallShaperWhole.instance;
                  }
               } else if (var8 == WallShaperN.instance) {
                  if (var11.parentSprite.getProperties().Is(IsoFlagType.attachedW)) {
                     continue;
                  }

                  if (var11.parentSprite.getProperties().Is(IsoFlagType.attachedN)) {
                     var13 = WallShaperWhole.instance;
                  }
               }

               var11.parentSprite.render(var11, this, var2, var3, var4, var1, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, var5, true, (Consumer)var13);
               var5.a = var12;
               var11.update();
            }
         }
      }

      if (this.Children != null && DebugOptions.instance.Terrain.RenderTiles.AttachedChildren.getValue()) {
         var15 = this.Children.size();

         for(var10 = 0; var10 < var15; ++var10) {
            IsoObject var16 = (IsoObject)this.Children.get(var10);
            if (var16 instanceof IsoMovingObject) {
               IndieGL.shaderSetValue(var7, "floorLayer", 1);
               var16.render(((IsoMovingObject)var16).getX(), ((IsoMovingObject)var16).getY(), ((IsoMovingObject)var16).getZ(), var5, true, false, (Shader)null);
            }
         }
      }

      if (this.wallBloodSplats != null && DebugOptions.instance.Terrain.RenderTiles.AttachedWallBloodSplats.getValue()) {
         if (Core.getInstance().getOptionBloodDecals() == 0) {
            if (PerformanceSettings.FBORenderChunk) {
               IndieGL.glDepthMask(true);
            }

            return;
         }

         IndieGL.shaderSetValue(var7, "floorLayer", 0);

         for(var15 = 0; var15 < this.wallBloodSplats.size(); ++var15) {
            ((IsoWallBloodSplat)this.wallBloodSplats.get(var15)).render(var2, var3, var4, var5);
         }
      }

      if (PerformanceSettings.FBORenderChunk) {
         IndieGL.glDepthMask(true);
      }

   }

   public boolean isSpriteInvisible() {
      return this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.invisible);
   }

   public void renderFxMask(float var1, float var2, float var3, boolean var4) {
      if (this.sprite != null) {
         if (this.getType() == IsoObjectType.wall) {
         }

         this.sprite.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, colFxMask, false);
      }

      if (this.getOverlaySprite() != null) {
         this.getOverlaySprite().render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, colFxMask, false);
      }

      if (var4) {
         int var5;
         int var6;
         if (this.AttachedAnimSprite != null) {
            var5 = this.AttachedAnimSprite.size();

            for(var6 = 0; var6 < var5; ++var6) {
               IsoSpriteInstance var7 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var6);
               var7.render(this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.renderYOffset * (float)Core.TileScale, colFxMask);
            }
         }

         if (this.Children != null) {
            var5 = this.Children.size();

            for(var6 = 0; var6 < var5; ++var6) {
               IsoObject var8 = (IsoObject)this.Children.get(var6);
               if (var8 instanceof IsoMovingObject) {
                  var8.render(((IsoMovingObject)var8).getX(), ((IsoMovingObject)var8).getY(), ((IsoMovingObject)var8).getZ(), colFxMask, var4, false, (Shader)null);
               }
            }
         }

         if (this.wallBloodSplats != null) {
            if (Core.getInstance().getOptionBloodDecals() == 0) {
               return;
            }

            for(var5 = 0; var5 < this.wallBloodSplats.size(); ++var5) {
               ((IsoWallBloodSplat)this.wallBloodSplats.get(var5)).render(var1, var2, var3, colFxMask);
            }
         }
      }

   }

   public void renderObjectPicker(float var1, float var2, float var3, ColorInfo var4) {
      if (this.sprite != null) {
         if (!this.sprite.getProperties().Is(IsoFlagType.invisible)) {
            this.sprite.renderObjectPicker(this.sprite.def, this, this.dir);
         }
      }
   }

   public boolean TestPathfindCollide(IsoMovingObject var1, IsoGridSquare var2, IsoGridSquare var3) {
      return false;
   }

   public boolean TestCollide(IsoMovingObject var1, IsoGridSquare var2, IsoGridSquare var3) {
      return false;
   }

   public VisionResult TestVision(IsoGridSquare var1, IsoGridSquare var2) {
      return IsoObject.VisionResult.Unblocked;
   }

   public Texture getCurrentFrameTex() {
      return this.sprite == null ? null : this.sprite.getTextureForCurrentFrame(this.dir);
   }

   public boolean isMaskClicked(int var1, int var2) {
      return this.sprite == null ? false : this.sprite.isMaskClicked(this.dir, var1, var2);
   }

   public boolean isMaskClicked(int var1, int var2, boolean var3) {
      if (this.sprite == null) {
         return false;
      } else {
         return this.overlaySprite != null && this.overlaySprite.isMaskClicked(this.dir, var1, var2, var3) ? true : this.sprite.isMaskClicked(this.dir, var1, var2, var3);
      }
   }

   public float getMaskClickedY(int var1, int var2, boolean var3) {
      return this.sprite == null ? 10000.0F : this.sprite.getMaskClickedY(this.dir, var1, var2, var3);
   }

   public ColorInfo getCustomColor() {
      return this.customColor;
   }

   public void setCustomColor(ColorInfo var1) {
      this.customColor = var1;
      if (Thread.currentThread() == GameWindow.GameThread) {
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
      }

   }

   public void setCustomColor(float var1, float var2, float var3, float var4) {
      ColorInfo var5 = new ColorInfo(var1, var2, var3, var4);
      this.setCustomColor(var5);
   }

   public void loadFromRemoteBuffer(ByteBuffer var1) {
      this.loadFromRemoteBuffer(var1, true);
   }

   public void loadFromRemoteBuffer(ByteBuffer var1, boolean var2) {
      try {
         this.load(var1, 219);
      } catch (IOException var12) {
         var12.printStackTrace();
         return;
      }

      if (this instanceof IsoWorldInventoryObject && ((IsoWorldInventoryObject)this).getItem() == null) {
         DebugLog.log("loadFromRemoteBuffer() failed due to an unknown item type");
      } else {
         int var3 = var1.getInt();
         int var4 = var1.getInt();
         int var5 = var1.getInt();
         int var6 = var1.getInt();
         boolean var7 = var1.get() != 0;
         boolean var8 = var1.get() != 0;
         IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var3, var4, var5);
         this.square = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
         if (this.square != null) {
            if (GameServer.bServer && !(this instanceof IsoWorldInventoryObject)) {
               IsoRegions.setPreviousFlags(this.square);
            }

            if (var7) {
               this.square.getSpecialObjects().add(this);
            }

            if (var8 && this instanceof IsoWorldInventoryObject) {
               this.square.getWorldObjects().add((IsoWorldInventoryObject)this);
               this.square.chunk.recalcHashCodeObjects();
            }

            if (var2) {
               if (var6 != -1 && var6 >= 0 && var6 <= this.square.getObjects().size()) {
                  this.square.getObjects().add(var6, this);
               } else {
                  this.square.getObjects().add(this);
               }
            }

            int var9;
            for(var9 = 0; var9 < this.getContainerCount(); ++var9) {
               ItemContainer var10 = this.getContainerByIndex(var9);
               var10.parent = this;
               var10.parent.square = this.square;
               var10.SourceGrid = this.square;
            }

            for(var9 = -1; var9 <= 1; ++var9) {
               for(int var13 = -1; var13 <= 1; ++var13) {
                  IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare(var9 + var3, var13 + var4, var5);
                  if (var11 != null) {
                     var11.RecalcAllWithNeighbours(true);
                  }
               }
            }

         }
      }
   }

   protected boolean hasObjectAmbientEmitter() {
      IsoChunk var1 = this.getChunk();
      return var1 == null ? false : var1.hasObjectAmbientEmitter(this);
   }

   protected void addObjectAmbientEmitter(ObjectAmbientEmitters.PerObjectLogic var1) {
      IsoChunk var2 = this.getChunk();
      if (var2 != null) {
         var2.addObjectAmbientEmitter(this, var1);
      }
   }

   public void addToWorld() {
      super.addToWorld();
      this.createContainersFromSpriteProperties();

      ItemContainer var2;
      for(int var1 = 0; var1 < this.getContainerCount(); ++var1) {
         var2 = this.getContainerByIndex(var1);
         var2.addItemsToProcessItems();
      }

      if (!GameServer.bServer) {
         String var4 = null;
         var2 = this.getContainerByEitherType("fridge", "freezer");
         if (var2 != null && var2.isPowered()) {
            this.addObjectAmbientEmitter((new ObjectAmbientEmitters.FridgeHumLogic()).init(this));
            var4 = "FridgeHum";
            IsoWorld.instance.getCell().addToProcessIsoObject(this);
         } else if (this.sprite != null && this.sprite.getProperties().Is(IsoFlagType.waterPiped) && (float)this.getWaterAmount() > 0.0F && Rand.Next(15) == 0) {
            this.addObjectAmbientEmitter((new ObjectAmbientEmitters.WaterDripLogic()).init(this));
            var4 = "WaterDrip";
         } else if (this.sprite != null && this.sprite.getName() != null && this.sprite.getName().startsWith("camping_01") && (this.sprite.tileSheetIndex == 0 || this.sprite.tileSheetIndex == 3)) {
            this.addObjectAmbientEmitter((new ObjectAmbientEmitters.TentAmbianceLogic()).init(this));
            var4 = "TentAmbiance";
         } else if (this instanceof IsoDoor) {
            if (!FMODAmbientWalls.ENABLE && ((IsoDoor)this).isExterior()) {
               this.addObjectAmbientEmitter((new ObjectAmbientEmitters.DoorLogic()).init(this));
            }
         } else if (this instanceof IsoWindow) {
            if (!FMODAmbientWalls.ENABLE && ((IsoWindow)this).isExterior()) {
               this.addObjectAmbientEmitter((new ObjectAmbientEmitters.WindowLogic()).init(this));
            }
         } else if (this instanceof IsoTree && Rand.Next(40) == 0) {
            this.addObjectAmbientEmitter((new ObjectAmbientEmitters.TreeAmbianceLogic()).init(this));
            var4 = "TreeAmbiance";
         }

         PropertyContainer var3 = this.getProperties();
         if (var3 != null && var3.Is("AmbientSound")) {
            this.addObjectAmbientEmitter((new ObjectAmbientEmitters.AmbientSoundLogic()).init(this));
            var4 = var3.Val("AmbientSound");
         }

         this.checkMoveWithWind();
         this.addLightSourceToWorld();
      }
   }

   public void removeFromWorld() {
      super.removeFromWorld(this.bRemoveFromWorldToMeta);
      IsoCell var1 = this.getCell();
      var1.addToProcessIsoObjectRemove(this);
      var1.getStaticUpdaterObjectList().remove(this);

      for(int var2 = 0; var2 < this.getContainerCount(); ++var2) {
         ItemContainer var3 = this.getContainerByIndex(var2);
         var3.removeItemsFromProcessItems();
      }

      if (this.emitter != null) {
         this.emitter.stopAll();
         this.emitter = null;
      }

      if (this.getChunk() != null) {
         this.getChunk().removeObjectAmbientEmitter(this);
      }

      this.removeLightSourceFromWorld();
      this.clearOnOverlay();
      if (PerformanceSettings.FBORenderChunk) {
         FBORenderObjectHighlight.getInstance().unregisterObject(this);
         FBORenderObjectOutline.getInstance().unregisterObject(this);
      }

   }

   public final void removeFromWorldToMeta() {
      try {
         this.bRemoveFromWorldToMeta = true;
         this.removeFromWorld();
      } finally {
         this.bRemoveFromWorldToMeta = false;
      }

   }

   public void reuseGridSquare() {
   }

   public void removeFromSquare() {
      if (this.square != null) {
         this.square.getObjects().remove(this);
         this.square.getSpecialObjects().remove(this);
      }

   }

   public void transmitCustomColorToClients() {
      if (GameServer.bServer && this.getCustomColor() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.SendCustomColor, (float)this.getSquare().x, (float)this.getSquare().y, this);
      }

   }

   public void transmitCompleteItemToClients() {
      if (GameServer.bServer) {
         if (GameServer.udpEngine == null) {
            return;
         }

         INetworkPacket.sendToRelative(PacketTypes.PacketType.AddItemToMap, (float)this.square.x, (float)this.square.y, this);
      }

   }

   public void transmitUpdatedSpriteToClients(UdpConnection var1) {
      if (GameServer.bServer) {
         for(int var2 = 0; var2 < GameServer.udpEngine.connections.size(); ++var2) {
            UdpConnection var3 = (UdpConnection)GameServer.udpEngine.connections.get(var2);
            if (var3 != null && this.square != null && (var1 == null || var3.getConnectedGUID() != var1.getConnectedGUID()) && var3.RelevantTo((float)this.square.x, (float)this.square.y)) {
               ByteBufferWriter var4 = var3.startPacket();
               PacketTypes.PacketType.UpdateItemSprite.doPacket(var4);
               var4.putInt(this.getSprite().ID);
               GameWindow.WriteStringUTF(var4.bb, this.spriteName);
               var4.putInt(this.getSquare().getX());
               var4.putInt(this.getSquare().getY());
               var4.putInt(this.getSquare().getZ());
               var4.putInt(this.getSquare().getObjects().indexOf(this));
               if (this.AttachedAnimSprite != null) {
                  var4.putByte((byte)this.AttachedAnimSprite.size());

                  for(int var5 = 0; var5 < this.AttachedAnimSprite.size(); ++var5) {
                     IsoSpriteInstance var6 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var5);
                     var4.putInt(var6.parentSprite.ID);
                  }
               } else {
                  var4.putByte((byte)0);
               }

               PacketTypes.PacketType.UpdateItemSprite.send(var3);
            }
         }
      }

   }

   public void transmitUpdatedSpriteToClients() {
      this.transmitUpdatedSpriteToClients((UdpConnection)null);
   }

   public void transmitUpdatedSprite() {
      if (GameClient.bClient) {
         this.transmitUpdatedSpriteToServer();
      }

      if (GameServer.bServer) {
         this.transmitUpdatedSpriteToClients();
      }

   }

   public void sendObjectChange(String var1) {
      if (GameServer.bServer) {
         GameServer.sendObjectChange(this, var1, (KahluaTable)null);
      } else if (GameClient.bClient) {
         DebugLog.log("sendObjectChange() can only be called on the server");
      } else {
         SinglePlayerServer.sendObjectChange(this, var1, (KahluaTable)null);
      }

   }

   public void sendObjectChange(String var1, KahluaTable var2) {
      if (GameServer.bServer) {
         GameServer.sendObjectChange(this, var1, var2);
      } else if (GameClient.bClient) {
         DebugLog.log("sendObjectChange() can only be called on the server");
      } else {
         SinglePlayerServer.sendObjectChange(this, var1, var2);
      }

   }

   public void sendObjectChange(String var1, Object... var2) {
      if (GameServer.bServer) {
         GameServer.sendObjectChange(this, var1, var2);
      } else if (GameClient.bClient) {
         DebugLog.log("sendObjectChange() can only be called on the server");
      } else {
         SinglePlayerServer.sendObjectChange(this, var1, var2);
      }

   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      if ("containers".equals(var1)) {
         var3.put((byte)this.getContainerCount());

         for(int var4 = 0; var4 < this.getContainerCount(); ++var4) {
            ItemContainer var5 = this.getContainerByIndex(var4);

            try {
               var5.save(var3);
            } catch (Throwable var8) {
               ExceptionLogger.logException(var8);
            }
         }
      } else if ("container.customTemperature".equals(var1)) {
         if (this.getContainer() != null) {
            var3.putFloat(this.getContainer().getCustomTemperature());
         } else {
            var3.putFloat(0.0F);
         }
      } else if ("name".equals(var1)) {
         GameWindow.WriteStringUTF(var3, this.getName());
      } else if ("replaceWith".equals(var1)) {
         if (var2 != null && var2.rawget("object") instanceof IsoObject) {
            IsoObject var9 = (IsoObject)var2.rawget("object");

            try {
               var9.save(var3);
            } catch (IOException var7) {
               var7.printStackTrace();
            }
         }
      } else if ("usesExternalWaterSource".equals(var1)) {
         boolean var10 = var2 != null && Boolean.TRUE.equals(var2.rawget("value"));
         var3.put((byte)(var10 ? 1 : 0));
      } else if ("sprite".equals(var1)) {
         if (this.sprite == null) {
            var3.putInt(0);
         } else {
            var3.putInt(this.sprite.ID);
            GameWindow.WriteStringUTF(var3, this.spriteName);
         }
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      int var3;
      int var11;
      if ("containers".equals(var1)) {
         for(var3 = 0; var3 < this.getContainerCount(); ++var3) {
            ItemContainer var4 = this.getContainerByIndex(var3);
            var4.removeItemsFromProcessItems();
            var4.removeAllItems();
         }

         this.removeAllContainers();
         byte var9 = var2.get();

         for(var11 = 0; var11 < var9; ++var11) {
            ItemContainer var5 = new ItemContainer();
            var5.ID = 0;
            var5.parent = this;
            var5.SourceGrid = this.square;

            try {
               var5.load(var2, 219);
               if (var11 == 0) {
                  if (this instanceof IsoDeadBody) {
                     var5.Capacity = 8;
                  }

                  this.container = var5;
               } else {
                  this.addSecondaryContainer(var5);
               }
            } catch (Throwable var7) {
               ExceptionLogger.logException(var7);
            }
         }
      } else if ("container.customTemperature".equals(var1)) {
         float var10 = var2.getFloat();
         if (this.getContainer() != null) {
            this.getContainer().setCustomTemperature(var10);
         }
      } else if ("name".equals(var1)) {
         String var12 = GameWindow.ReadStringUTF(var2);
         this.setName(var12);
      } else if ("replaceWith".equals(var1)) {
         try {
            var3 = this.getObjectIndex();
            if (var3 >= 0) {
               IsoObject var13 = factoryFromFileInput(this.getCell(), var2);
               var13.load(var2, 219);
               var13.setSquare(this.square);
               this.square.getObjects().set(var3, var13);
               this.square.getSpecialObjects().remove(this);
               this.square.RecalcAllWithNeighbours(true);
               if (this.getContainerCount() > 0) {
                  for(int var14 = 0; var14 < this.getContainerCount(); ++var14) {
                     ItemContainer var6 = this.getContainerByIndex(var14);
                     var6.removeItemsFromProcessItems();
                  }

                  LuaEventManager.triggerEvent("OnContainerUpdate");
               }
            }
         } catch (IOException var8) {
            var8.printStackTrace();
         }
      } else if ("usesExternalWaterSource".equals(var1)) {
         this.usesExternalWaterSource = var2.get() == 1;
      } else if ("sprite".equals(var1)) {
         var3 = var2.getInt();
         if (var3 == 0) {
            this.sprite = null;
            this.spriteName = null;
            this.tile = null;
         } else {
            this.spriteName = GameWindow.ReadString(var2);
            this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, var3);
            if (this.sprite == null) {
               this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
               this.sprite.LoadFramesNoDirPageSimple(this.spriteName);
            }
         }
      } else if ("emptyTrash".equals(var1)) {
         ItemContainer var15 = this.getContainer();

         for(var11 = 0; var11 < var15.getItems().size(); ++var11) {
            InventoryItem var16 = (InventoryItem)var15.getItems().get(var11);
            var15.DoRemoveItem(var16);
         }

         this.getContainer().clear();
         if (this.getOverlaySprite() != null) {
            ItemPickerJava.updateOverlaySprite(this);
         }
      }

      this.checkMoveWithWind();
   }

   /** @deprecated */
   @Deprecated
   public void transmitUpdatedSpriteToServer() {
      if (GameClient.bClient) {
         ByteBufferWriter var1 = GameClient.connection.startPacket();
         PacketTypes.PacketType.UpdateItemSprite.doPacket(var1);
         var1.putInt(this.getSprite().ID);
         GameWindow.WriteStringUTF(var1.bb, this.spriteName);
         var1.putInt(this.getSquare().getX());
         var1.putInt(this.getSquare().getY());
         var1.putInt(this.getSquare().getZ());
         var1.putInt(this.getSquare().getObjects().indexOf(this));
         if (this.AttachedAnimSprite != null) {
            var1.putByte((byte)this.AttachedAnimSprite.size());

            for(int var2 = 0; var2 < this.AttachedAnimSprite.size(); ++var2) {
               IsoSpriteInstance var3 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var2);
               var1.putInt(var3.parentSprite.ID);
            }
         } else {
            var1.putByte((byte)0);
         }

         PacketTypes.PacketType.UpdateItemSprite.send(GameClient.connection);
         DebugLog.General.warn("Special for the MP branch: The deprecated function was called: transmitUpdatedSpriteToServer");
      }

   }

   /** @deprecated */
   @Deprecated
   public void transmitCompleteItemToServer() {
      if (GameClient.bClient) {
         AddItemToMapPacket var1 = new AddItemToMapPacket();
         var1.set(this);
         ByteBufferWriter var2 = GameClient.connection.startPacket();
         PacketTypes.PacketType.AddItemToMap.doPacket(var2);
         var1.write(var2);
         PacketTypes.PacketType.AddItemToMap.send(GameClient.connection);
         DebugLog.General.warn("Special for the MP branch: The deprecated function was called: transmitCompleteItemToServer");
      }

   }

   public void transmitModData() {
      if (this.square != null) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.ObjectModData, this);
         } else if (GameServer.bServer) {
            GameServer.sendObjectModData(this);
         }

      }
   }

   public void writeToRemoteBuffer(ByteBufferWriter var1) {
      try {
         this.save(var1.bb);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      var1.putInt(this.getObjectIndex());
      var1.putBoolean(this.square.getSpecialObjects().contains(this));
      var1.putBoolean(this.square.getWorldObjects().contains(this));
   }

   public int getObjectIndex() {
      return this.square == null ? -1 : this.square.getObjects().indexOf(this);
   }

   public int getMovingObjectIndex() {
      return this.square == null ? -1 : this.square.getMovingObjects().indexOf(this);
   }

   public int getSpecialObjectIndex() {
      return this.square == null ? -1 : this.square.getSpecialObjects().indexOf(this);
   }

   public int getStaticMovingObjectIndex() {
      return this.square == null ? -1 : this.square.getStaticMovingObjects().indexOf(this);
   }

   public int getWorldObjectIndex() {
      return this.square == null ? -1 : this.square.getWorldObjects().indexOf(this);
   }

   public IsoSprite getOverlaySprite() {
      return this.overlaySprite;
   }

   public void setOverlaySprite(String var1) {
      this.setOverlaySprite(var1, -1.0F, -1.0F, -1.0F, -1.0F, true);
   }

   public void setOverlaySprite(String var1, boolean var2) {
      this.setOverlaySprite(var1, -1.0F, -1.0F, -1.0F, -1.0F, var2);
   }

   public void setOverlaySpriteColor(float var1, float var2, float var3, float var4) {
      this.overlaySpriteColor = new ColorInfo(var1, var2, var3, var4);
   }

   public ColorInfo getOverlaySpriteColor() {
      return this.overlaySpriteColor;
   }

   public void setOverlaySprite(String var1, float var2, float var3, float var4, float var5) {
      this.setOverlaySprite(var1, var2, var3, var4, var5, true);
   }

   public boolean setOverlaySprite(String var1, float var2, float var3, float var4, float var5, boolean var6) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         if (this.overlaySprite == null) {
            return false;
         }

         this.overlaySprite = null;
         var1 = "";
      } else {
         boolean var7;
         if (!(var2 > -1.0F)) {
            var7 = this.overlaySpriteColor == null;
         } else {
            var7 = this.overlaySpriteColor != null && this.overlaySpriteColor.r == var2 && this.overlaySpriteColor.g == var3 && this.overlaySpriteColor.b == var4 && this.overlaySpriteColor.a == var5;
         }

         if (this.overlaySprite != null && var1.equals(this.overlaySprite.name) && var7) {
            return false;
         }

         this.overlaySprite = IsoSpriteManager.instance.getSprite(var1);
         this.overlaySprite.name = var1;
      }

      if (var2 > -1.0F) {
         this.overlaySpriteColor = new ColorInfo(var2, var3, var4, var5);
      } else {
         this.overlaySpriteColor = null;
      }

      if (PerformanceSettings.FBORenderChunk && !GameServer.bServer && Thread.currentThread() == GameWindow.GameThread) {
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
      }

      if (!var6) {
         return true;
      } else {
         if (GameServer.bServer) {
            GameServer.updateOverlayForClients(this, var1, var2, var3, var4, var5, (UdpConnection)null);
         } else if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.UpdateOverlaySprite, this, var1, var2, var3, var4, var5);
         }

         return true;
      }
   }

   public boolean hasOverlaySprite() {
      return this.getOverlaySprite() != null;
   }

   public boolean haveSpecialTooltip() {
      return this.specialTooltip;
   }

   public void setSpecialTooltip(boolean var1) {
      this.specialTooltip = var1;
   }

   public int getKeyId() {
      return this.keyId;
   }

   public void setKeyId(int var1) {
      this.keyId = var1;
   }

   public boolean isHighlighted() {
      return (this.highlightFlags & 1) != 0;
   }

   public void setHighlighted(boolean var1) {
      this.setHighlighted(var1, true);
   }

   public void setHighlighted(boolean var1, boolean var2) {
      if (var1) {
         this.highlightFlags = (byte)(this.highlightFlags | 1);
         if (PerformanceSettings.FBORenderChunk) {
            FBORenderObjectHighlight.getInstance().registerObject(this);
         }
      } else {
         this.highlightFlags &= -2;
         if (PerformanceSettings.FBORenderChunk) {
            FBORenderObjectHighlight.getInstance().unregisterObject(this);
         }
      }

      if (var2) {
         this.highlightFlags = (byte)(this.highlightFlags | 2);
      } else {
         this.highlightFlags &= -3;
      }

   }

   public ColorInfo getHighlightColor() {
      return this.highlightColor;
   }

   public void setHighlightColor(ColorInfo var1) {
      this.highlightColor.set(var1);
   }

   public void setHighlightColor(float var1, float var2, float var3, float var4) {
      if (this.highlightColor == null) {
         this.highlightColor = new ColorInfo(var1, var2, var3, var4);
      } else {
         this.highlightColor.set(var1, var2, var3, var4);
      }

   }

   public boolean isBlink() {
      return (this.highlightFlags & 4) != 0;
   }

   public void setBlink(boolean var1) {
      if (var1) {
         this.highlightFlags = (byte)(this.highlightFlags | 4);
      } else {
         this.highlightFlags &= -5;
      }

   }

   public boolean isSatChair() {
      return (this.highlightFlags & 8) != 0;
   }

   public void setSatChair(boolean var1) {
      if (var1) {
         this.highlightFlags = (byte)(this.highlightFlags | 8);
      } else {
         this.highlightFlags &= -9;
      }

      this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
   }

   public void checkHaveElectricity() {
      if (!GameServer.bServer) {
         ItemContainer var1 = this.getContainerByEitherType("fridge", "freezer");
         if (var1 != null && var1.isPowered()) {
            IsoWorld.instance.getCell().addToProcessIsoObject(this);
            if (this.getChunk() != null && !this.hasObjectAmbientEmitter()) {
               this.getChunk().addObjectAmbientEmitter(this, (new ObjectAmbientEmitters.FridgeHumLogic()).init(this));
            }
         }

         this.checkAmbientSound();
         this.checkLightSourceActive();
      }
   }

   public void checkAmbientSound() {
      PropertyContainer var1 = this.getProperties();
      if (var1 != null && var1.Is("AmbientSound") && this.getChunk() != null && !this.hasObjectAmbientEmitter()) {
         this.getChunk().addObjectAmbientEmitter(this, (new ObjectAmbientEmitters.AmbientSoundLogic()).init(this));
      }

   }

   public int getContainerCount() {
      int var1 = this.container == null ? 0 : 1;
      int var2 = this.secondaryContainers == null ? 0 : this.secondaryContainers.size();
      return var1 + var2;
   }

   public ItemContainer getContainerByIndex(int var1) {
      if (this.container != null) {
         if (var1 == 0) {
            return this.container;
         } else if (this.secondaryContainers == null) {
            return null;
         } else {
            return var1 >= 1 && var1 <= this.secondaryContainers.size() ? (ItemContainer)this.secondaryContainers.get(var1 - 1) : null;
         }
      } else if (this.secondaryContainers == null) {
         return null;
      } else {
         return var1 >= 0 && var1 < this.secondaryContainers.size() ? (ItemContainer)this.secondaryContainers.get(var1) : null;
      }
   }

   public ItemContainer getContainerByType(String var1) {
      for(int var2 = 0; var2 < this.getContainerCount(); ++var2) {
         ItemContainer var3 = this.getContainerByIndex(var2);
         if (var3.getType().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public ItemContainer getContainerByEitherType(String var1, String var2) {
      for(int var3 = 0; var3 < this.getContainerCount(); ++var3) {
         ItemContainer var4 = this.getContainerByIndex(var3);
         if (var4.getType().equals(var1) || var4.getType().equals(var2)) {
            return var4;
         }
      }

      return null;
   }

   public void addSecondaryContainer(ItemContainer var1) {
      if (this.secondaryContainers == null) {
         this.secondaryContainers = new ArrayList();
      }

      this.secondaryContainers.add(var1);
      var1.parent = this;
   }

   public int getContainerIndex(ItemContainer var1) {
      if (var1 == this.container) {
         return 0;
      } else if (this.secondaryContainers == null) {
         return -1;
      } else {
         for(int var2 = 0; var2 < this.secondaryContainers.size(); ++var2) {
            if (this.secondaryContainers.get(var2) == var1) {
               return (this.container == null ? 0 : 1) + var2;
            }
         }

         return -1;
      }
   }

   public void removeAllContainers() {
      this.container = null;
      if (this.secondaryContainers != null) {
         this.secondaryContainers.clear();
      }

   }

   public void createContainersFromSpriteProperties() {
      if (this.sprite != null) {
         if (this.container == null) {
            if (this.sprite.getProperties().Is(IsoFlagType.container) && this.container == null) {
               this.container = new ItemContainer(this.sprite.getProperties().Val("container"), this.square, this);
               this.container.parent = this;
               this.OutlineOnMouseover = true;
               if (this.sprite.getProperties().Is("ContainerCapacity")) {
                  this.container.Capacity = Integer.parseInt(this.sprite.getProperties().Val("ContainerCapacity"));
               }

               if (this.sprite.getProperties().Is("ContainerPosition")) {
                  this.container.setContainerPosition(this.sprite.getProperties().Val("ContainerPosition"));
               }
            }

            if (this.getSprite().getProperties().Is("Freezer")) {
               ItemContainer var1 = new ItemContainer("freezer", this.square, this);
               if (this.getSprite().getProperties().Is("FreezerCapacity")) {
                  var1.Capacity = Integer.parseInt(this.sprite.getProperties().Val("FreezerCapacity"));
               } else {
                  var1.Capacity = 15;
               }

               if (this.container == null) {
                  this.container = var1;
                  this.container.parent = this;
               } else {
                  this.addSecondaryContainer(var1);
               }

               if (this.sprite.getProperties().Is("FreezerPosition")) {
                  var1.setFreezerPosition(this.sprite.getProperties().Val("FreezerPosition"));
               }
            }

         }
      }
   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      return true;
   }

   public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2) {
      return true;
   }

   public void cleanWallBlood() {
      this.square.removeBlood(false, true);
   }

   public ObjectRenderEffects getWindRenderEffects() {
      return this.windRenderEffects;
   }

   public ObjectRenderEffects getObjectRenderEffects() {
      return this.objectRenderEffects;
   }

   public void setRenderEffect(RenderEffectType var1) {
      this.setRenderEffect(var1, false);
   }

   public IsoObject getRenderEffectMaster() {
      return this;
   }

   public int getRenderEffectObjectCount() {
      return 1;
   }

   public IsoObject getRenderEffectObjectByIndex(int var1) {
      return this;
   }

   public void setRenderEffect(RenderEffectType var1, boolean var2) {
      if (!GameServer.bServer) {
         IsoObject var3 = this.getRenderEffectMaster();
         ObjectRenderEffects var4 = var3.objectRenderEffects;
         if (var3.objectRenderEffects == null || var2) {
            var3.objectRenderEffects = ObjectRenderEffects.getNew(this, var1, var2);
         }

         if (PerformanceSettings.FBORenderChunk && var3.objectRenderEffects != var4) {
            for(int var5 = 0; var5 < this.getRenderEffectObjectCount(); ++var5) {
               IsoObject var6 = this.getRenderEffectObjectByIndex(var5);
               if (var6 != null) {
                  var6.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
               }
            }
         }

      }
   }

   public void removeRenderEffect(ObjectRenderEffects var1) {
      IsoObject var2 = this.getRenderEffectMaster();
      if (var2.objectRenderEffects != null && var2.objectRenderEffects == var1) {
         var2.objectRenderEffects = null;
         if (PerformanceSettings.FBORenderChunk) {
            for(int var3 = 0; var3 < this.getRenderEffectObjectCount(); ++var3) {
               IsoObject var4 = this.getRenderEffectObjectByIndex(var3);
               if (var4 != null) {
                  var4.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
               }
            }
         }
      }

   }

   public ObjectRenderEffects getObjectRenderEffectsToApply() {
      IsoObject var1 = this.getRenderEffectMaster();
      if (var1.objectRenderEffects != null) {
         return var1.objectRenderEffects;
      } else {
         return Core.getInstance().getOptionDoWindSpriteEffects() && var1.windRenderEffects != null ? var1.windRenderEffects : null;
      }
   }

   public void destroyFence(IsoDirections var1) {
      BrokenFences.getInstance().destroyFence(this, var1);
   }

   public void getSpriteGridObjects(ArrayList<IsoObject> var1) {
      this.getSpriteGridObjects(var1, false);
   }

   public void getSpriteGridObjectsExcludingSelf(ArrayList<IsoObject> var1) {
      this.getSpriteGridObjects(var1, false);
   }

   public void getSpriteGridObjectsIncludingSelf(ArrayList<IsoObject> var1) {
      this.getSpriteGridObjects(var1, true);
   }

   public void getSpriteGridObjects(ArrayList<IsoObject> var1, boolean var2) {
      var1.clear();
      if (var2) {
         var1.add(this);
      }

      IsoSprite var3 = this.getSprite();
      if (var3 != null) {
         IsoSpriteGrid var4 = var3.getSpriteGrid();
         if (var4 != null) {
            int var5 = var4.getSpriteGridPosX(var3);
            int var6 = var4.getSpriteGridPosY(var3);
            int var7 = var4.getSpriteGridPosZ(var3);
            int var8 = this.getSquare().getX();
            int var9 = this.getSquare().getY();
            int var10 = this.getSquare().getZ();

            for(int var11 = var10 - var7; var11 < var10 - var7 + var4.getLevels(); ++var11) {
               for(int var12 = var9 - var6; var12 < var9 - var6 + var4.getHeight(); ++var12) {
                  for(int var13 = var8 - var5; var13 < var8 - var5 + var4.getWidth(); ++var13) {
                     IsoGridSquare var14 = this.getCell().getGridSquare(var13, var12, var11);
                     if (var14 != null) {
                        for(int var15 = 0; var15 < var14.getObjects().size(); ++var15) {
                           IsoObject var16 = (IsoObject)var14.getObjects().get(var15);
                           if ((var16 != this || var2) && var16.getSpriteGrid() == var4) {
                              var1.add(var16);
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   public boolean isConnectedSpriteGridObject(IsoObject var1) {
      if (var1 == null) {
         return false;
      } else {
         IsoSprite var2 = this.getSprite();
         IsoSprite var3 = var1.getSprite();
         if (var2 != null && var3 != null) {
            IsoSpriteGrid var4 = var2.getSpriteGrid();
            IsoSpriteGrid var5 = var3.getSpriteGrid();
            if (var4 != null && var4 == var5) {
               int var6 = this.getSquare().getX() - var4.getSpriteGridPosX(var2);
               int var7 = this.getSquare().getY() - var4.getSpriteGridPosY(var2);
               int var8 = this.getSquare().getZ() - var4.getSpriteGridPosZ(var2);
               int var9 = var1.getSquare().getX() - var5.getSpriteGridPosX(var3);
               int var10 = var1.getSquare().getY() - var5.getSpriteGridPosY(var3);
               int var11 = var1.getSquare().getZ() - var5.getSpriteGridPosZ(var3);
               return var6 == var9 && var7 == var10 && var8 == var11;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public boolean isOnScreen() {
      return this.getSquare() != null && this.getSquare().IsOnScreen();
   }

   public final int getOutlineHighlightCol() {
      return this.outlineHighlightCol[0];
   }

   public final void setOutlineHighlightCol(ColorInfo var1) {
      if (var1 != null) {
         for(int var2 = 0; var2 < this.outlineHighlightCol.length; ++var2) {
            this.outlineHighlightCol[var2] = Color.colorToABGR(var1.r, var1.g, var1.b, var1.a);
         }

      }
   }

   public final int getOutlineHighlightCol(int var1) {
      return this.outlineHighlightCol[var1];
   }

   public final void setOutlineHighlightCol(int var1, ColorInfo var2) {
      if (var2 != null) {
         this.outlineHighlightCol[var1] = Color.colorToABGR(var2.r, var2.g, var2.b, var2.a);
      }
   }

   public final void setOutlineHighlightCol(float var1, float var2, float var3, float var4) {
      for(int var5 = 0; var5 < this.outlineHighlightCol.length; ++var5) {
         this.outlineHighlightCol[var5] = Color.colorToABGR(var1, var2, var3, var4);
      }

   }

   public final void setOutlineHighlightCol(int var1, float var2, float var3, float var4, float var5) {
      this.outlineHighlightCol[var1] = Color.colorToABGR(var2, var3, var4, var5);
   }

   public final boolean isOutlineHighlight() {
      return this.isOutlineHighlight != 0;
   }

   public final boolean isOutlineHighlight(int var1) {
      return (this.isOutlineHighlight & 1 << var1) != 0;
   }

   public final void setOutlineHighlight(boolean var1) {
      this.isOutlineHighlight = (byte)(var1 ? -1 : 0);
      if (var1) {
         if (PerformanceSettings.FBORenderChunk) {
            FBORenderObjectOutline.getInstance().registerObject(this);
         }
      } else if (PerformanceSettings.FBORenderChunk) {
         FBORenderObjectOutline.getInstance().unregisterObject(this);
      }

   }

   public final void setOutlineHighlight(int var1, boolean var2) {
      if (var2) {
         this.isOutlineHighlight = (byte)(this.isOutlineHighlight | 1 << var1);
      } else {
         this.isOutlineHighlight = (byte)(this.isOutlineHighlight & ~(1 << var1));
      }

   }

   public final boolean isOutlineHlAttached() {
      return this.isOutlineHlAttached != 0;
   }

   public final boolean isOutlineHlAttached(int var1) {
      return (this.isOutlineHlAttached & 1 << var1) != 0;
   }

   public void setOutlineHlAttached(boolean var1) {
      this.isOutlineHlAttached = (byte)(var1 ? -1 : 0);
   }

   public final void setOutlineHlAttached(int var1, boolean var2) {
      if (var2) {
         this.isOutlineHlAttached = (byte)(this.isOutlineHlAttached | 1 << var1);
      } else {
         this.isOutlineHlAttached = (byte)(this.isOutlineHlAttached & ~(1 << var1));
      }

   }

   public boolean isOutlineHlBlink() {
      return this.isOutlineHlBlink != 0;
   }

   public final boolean isOutlineHlBlink(int var1) {
      return (this.isOutlineHlBlink & 1 << var1) != 0;
   }

   public void setOutlineHlBlink(boolean var1) {
      this.isOutlineHlBlink = (byte)(var1 ? -1 : 0);
   }

   public final void setOutlineHlBlink(int var1, boolean var2) {
      if (var2) {
         this.isOutlineHlBlink = (byte)(this.isOutlineHlBlink | 1 << var1);
      } else {
         this.isOutlineHlBlink = (byte)(this.isOutlineHlBlink & ~(1 << var1));
      }

   }

   public void unsetOutlineHighlight() {
      this.isOutlineHighlight = 0;
      this.isOutlineHlBlink = 0;
      this.isOutlineHlAttached = 0;
   }

   public float getOutlineThickness() {
      return this.outlineThickness;
   }

   public void setOutlineThickness(float var1) {
      this.outlineThickness = var1;
   }

   protected void addItemsFromProperties() {
      PropertyContainer var1 = this.getProperties();
      if (var1 != null) {
         String var2 = var1.Val("Material");
         String var3 = var1.Val("Material2");
         String var4 = var1.Val("Material3");
         if ("Wood".equals(var2) || "Wood".equals(var3) || "Wood".equals(var4)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            if (Rand.NextBool(5)) {
               this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }
         }

         if (("MetalBars".equals(var2) || "MetalBars".equals(var3) || "MetalBars".equals(var4)) && Rand.NextBool(2)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.MetalBar"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
         }

         if (("MetalPlates".equals(var2) || "MetalPlates".equals(var3) || "MetalPlates".equals(var4)) && Rand.NextBool(2)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.SheetMetal"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
         }

         if (("MetalPipe".equals(var2) || "MetalPipe".equals(var3) || "MetalPipe".equals(var4)) && Rand.NextBool(2)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.MetalPipe"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
         }

         if (("MetalWire".equals(var2) || "MetalWire".equals(var3) || "MetalWire".equals(var4)) && Rand.NextBool(3)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Wire"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
         }

         if (("Nails".equals(var2) || "Nails".equals(var3) || "Nails".equals(var4)) && Rand.NextBool(2)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Nails"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
         }

         if (("Screws".equals(var2) || "Screws".equals(var3) || "Screws".equals(var4)) && Rand.NextBool(2)) {
            this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Screws"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
         }

      }
   }

   public boolean isDestroyed() {
      return this.Damage <= 0;
   }

   public void Thump(IsoMovingObject var1) {
      IsoGameCharacter var2 = (IsoGameCharacter)Type.tryCastTo(var1, IsoGameCharacter.class);
      if (var2 != null) {
         Thumpable var3 = this.getThumpableFor(var2);
         if (var3 == null) {
            return;
         }

         if (var3 != this) {
            var3.Thump(var1);
            return;
         }
      }

      boolean var10 = BrokenFences.getInstance().isBreakableObject(this);
      byte var4 = 8;
      int var7;
      if (var1 instanceof IsoZombie) {
         int var5 = var1.getCurrentSquare().getMovingObjects().size();
         if (var1.getCurrentSquare().getW() != null) {
            var5 += var1.getCurrentSquare().getW().getMovingObjects().size();
         }

         if (var1.getCurrentSquare().getE() != null) {
            var5 += var1.getCurrentSquare().getE().getMovingObjects().size();
         }

         if (var1.getCurrentSquare().getS() != null) {
            var5 += var1.getCurrentSquare().getS().getMovingObjects().size();
         }

         if (var1.getCurrentSquare().getN() != null) {
            var5 += var1.getCurrentSquare().getN().getMovingObjects().size();
         }

         if (var5 >= var4) {
            var7 = 1 * ThumpState.getFastForwardDamageMultiplier();
            this.Damage = (short)(this.Damage - var7);
         } else {
            this.partialThumpDmg += (float)var5 / (float)var4 * (float)ThumpState.getFastForwardDamageMultiplier();
            if ((int)this.partialThumpDmg > 0) {
               var7 = (int)this.partialThumpDmg;
               this.Damage = (short)(this.Damage - var7);
               this.partialThumpDmg -= (float)var7;
            }
         }

         WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
      }

      if (this.Damage <= 0) {
         String var12 = "BreakObject";
         if (var2 != null) {
            var2.getEmitter().playSound(var12, this);
         }

         if (GameServer.bServer) {
            GameServer.PlayWorldSoundServer(var12, false, var1.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
         }

         WorldSoundManager.instance.addSound((Object)null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
         var1.setThumpTarget((Thumpable)null);
         if (var10) {
            PropertyContainer var13 = this.getProperties();
            IsoDirections var11;
            if (var13.Is(IsoFlagType.collideN) && var13.Is(IsoFlagType.collideW)) {
               var11 = var1.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
            } else if (var13.Is(IsoFlagType.collideN)) {
               var11 = var1.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
            } else {
               var11 = var1.getX() >= this.getX() ? IsoDirections.W : IsoDirections.E;
            }

            BrokenFences.getInstance().destroyFence(this, var11);
            return;
         }

         ArrayList var6 = new ArrayList();

         for(var7 = 0; var7 < this.getContainerCount(); ++var7) {
            ItemContainer var8 = this.getContainerByIndex(var7);
            var6.clear();
            var6.addAll(var8.getItems());
            var8.removeItemsFromProcessItems();
            var8.removeAllItems();

            for(int var9 = 0; var9 < var6.size(); ++var9) {
               this.getSquare().AddWorldInventoryItem((InventoryItem)var6.get(var9), 0.0F, 0.0F, 0.0F);
            }
         }

         this.square.transmitRemoveItemFromSquare(this);
      }

   }

   public void setMovedThumpable(boolean var1) {
      this.bMovedThumpable = var1;
   }

   public boolean isMovedThumpable() {
      return this.bMovedThumpable;
   }

   public void WeaponHit(IsoGameCharacter var1, HandWeapon var2) {
   }

   public Thumpable getThumpableFor(IsoGameCharacter var1) {
      if (this.isDestroyed()) {
         return null;
      } else if (this.isMovedThumpable()) {
         return this;
      } else if (!BrokenFences.getInstance().isBreakableObject(this)) {
         return null;
      } else {
         IsoZombie var2 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
         return var2 != null && var2.isCrawling() ? this : null;
      }
   }

   public boolean isExistInTheWorld() {
      return this.square != null ? this.square.getObjects().contains(this) : false;
   }

   public float getThumpCondition() {
      return (float)PZMath.clamp(this.getDamage(), 0, 100) / 100.0F;
   }

   public String toString() {
      String var10000 = this.getName();
      return var10000 + ":" + (this.getSpriteName() != null ? this.getSpriteName() : "null") + ":" + (this.getSprite() != null ? this.getSprite().getName() : "UNKNOWN") + ":" + super.toString();
   }

   public GameEntityType getGameEntityType() {
      return GameEntityType.IsoObject;
   }

   public long getEntityNetID() {
      if (this.getObjectIndex() == -1) {
         this.isoEntityNetID = -1L;
         return -1L;
      } else {
         if (this.isoEntityNetID == -1L || this.lastObjectIndex == -1 || this.lastObjectIndex != this.getObjectIndex()) {
            this.lastObjectIndex = this.getObjectIndex();
            long var1 = (long)this.lastObjectIndex << 40;
            long var3 = (long)this.square.getZ() << 32;
            long var5 = (long)this.square.getY() << 16;
            long var7 = (long)this.square.getX();
            long var9 = var7 + var5 + var3 + var1;
            GameEntityManager.checkEntityIDChange(this, this.isoEntityNetID, var9);
            this.isoEntityNetID = var9;
         }

         return this.isoEntityNetID;
      }
   }

   public boolean isEntityValid() {
      return true;
   }

   public IsoObject getMasterObject() {
      SpriteConfig var1 = this.getSpriteConfig();
      return var1 != null ? var1.getMultiSquareMaster() : this;
   }

   public boolean isTent() {
      if (this.getSprite().getProperties() != null && this.getSprite().getProperties().Is("CustomName") && (this.getSprite().getProperties().Val("CustomName").contains("Tent") || this.getSprite().getProperties().Val("CustomName").contains("Shelter"))) {
         return true;
      } else if (this.getSprite().getProperties() != null && this.getSprite().getProperties().Is("CustomName") && (this.getSprite().getProperties().Val("CustomName").contains("tent") || this.getSprite().getProperties().Val("CustomName").contains("shelter"))) {
         return true;
      } else if (this.getName() != null && (this.getName().contains("Tent") || this.getName().contains("Shelter"))) {
         return true;
      } else {
         return this.getName() != null && (this.getName().contains("tent") || this.getName().contains("shelter"));
      }
   }

   public IsoDirections getFacing() {
      IsoSprite var1 = this.getSprite();
      return var1 != null ? var1.getFacing() : null;
   }

   public String getTileName() {
      if (this.getProperties() != null) {
         PropertyContainer var1 = this.getProperties();
         if (var1 != null && var1.Is("CustomName")) {
            String var2 = "Moveable Object";
            if (var1.Is("CustomName")) {
               if (var1.Is("GroupName")) {
                  String var10000 = var1.Val("GroupName");
                  var2 = var10000 + " " + var1.Val("CustomName");
               } else {
                  var2 = var1.Val("CustomName");
               }
            }

            return Translator.getMoveableDisplayName(var2);
         }
      }

      return this.getName();
   }

   public InventoryItem addItemToObjectSurface(String var1) {
      return this.addItemToObjectSurface(var1, false);
   }

   public InventoryItem addItemToObjectSurface(String var1, boolean var2) {
      if (var1 == null) {
         return null;
      } else {
         IsoDirections var3 = this.getFacing();
         IsoGridSquare var4 = this.getSquare();
         if (var4 == null) {
            return null;
         } else {
            InventoryItem var5 = null;
            if (var3 != null) {
               if (var3 == IsoDirections.E) {
                  var5 = ItemSpawner.spawnItem(var1, var4, Rand.Next(0.4F, 0.42F), Rand.Next(0.34F, 0.74F), this.getSurfaceOffsetNoTable() / 96.0F);
               }

               if (var3 == IsoDirections.W) {
                  var5 = ItemSpawner.spawnItem(var1, var4, Rand.Next(0.6F, 0.64F), Rand.Next(0.34F, 0.74F), this.getSurfaceOffsetNoTable() / 96.0F);
               }

               if (var3 == IsoDirections.N) {
                  var5 = ItemSpawner.spawnItem(var1, var4, Rand.Next(0.44F, 0.64F), 0.67F, this.getSurfaceOffsetNoTable() / 96.0F);
               }

               if (var3 == IsoDirections.S) {
                  var5 = ItemSpawner.spawnItem(var1, var4, Rand.Next(0.44F, 0.64F), 0.42F, this.getSurfaceOffsetNoTable() / 96.0F);
               }
            } else {
               var5 = ItemSpawner.spawnItem(var1, var4, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), this.getSurfaceOffsetNoTable() / 96.0F);
            }

            if (var2) {
               var5.randomizeWorldZRotation();
            }

            return var5;
         }
      }
   }

   public ObjectRenderInfo getRenderInfo(int var1) {
      return this.renderInfo[var1];
   }

   public void invalidateRenderChunkLevel(long var1) {
      if (this.getSquare() != null) {
         this.getSquare().invalidateRenderChunkLevel(var1);
         IsoGridSquare var3 = this.getRenderSquare();
         if (var3 != null && var3 != this.getSquare()) {
            var3.invalidateRenderChunkLevel(var1);
         }

      }
   }

   public void invalidateVispolyChunkLevel() {
      if (this.getSquare() != null) {
         this.getSquare().invalidateVispolyChunkLevel();
      }
   }

   public boolean hasAnimatedAttachments() {
      boolean var1 = false;
      if (this.getSprite() != null && this.getProperties().Is(IsoFlagType.HasLightOnSprite)) {
         var1 = this.shouldShowOnOverlay();
         if (var1 && this.getOnOverlay() == null && this.sprite.tilesetName != null) {
            IsoSprite var2 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(this.sprite.tilesetName + "_on_" + this.sprite.tileSheetIndex);
            if (var2 != null) {
               this.setOnOverlay(IsoSpriteInstance.get(var2));
            }
         }
      }

      return var1 && this.getOnOverlay() != null;
   }

   public void renderAnimatedAttachments(float var1, float var2, float var3, ColorInfo var4) {
      if (this.getOnOverlay() != null) {
         float var5 = var4.r;
         float var6 = var4.g;
         float var7 = var4.b;
         float var8 = var4.a;
         var4.set(1.0F, 1.0F, 1.0F, var4.a);
         boolean var9 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
         if (var9) {
            var4.set(this.getHighlightColor());
            if (this.isBlink()) {
               var4.a *= Core.blinkAlpha;
            }

            var4.r = var5 * (1.0F - var4.a) + this.getHighlightColor().r * var4.a;
            var4.g = var6 * (1.0F - var4.a) + this.getHighlightColor().g * var4.a;
            var4.b = var7 * (1.0F - var4.a) + this.getHighlightColor().b * var4.a;
            var4.a = var8;
         }

         this.getOnOverlay().getParentSprite().render(this.getOnOverlay(), this, var1, var2, var3, this.dir, this.offsetX, this.offsetY + this.getRenderYOffset() * (float)Core.TileScale, var4, true);
         var4.set(var5, var6, var7, var4.a);
      }
   }

   public IsoGridSquare getRenderSquare() {
      return this.renderSquareOverride != null ? this.renderSquareOverride : this.getSquare();
   }

   public void setSpriteModelName(String var1) {
      this.spriteModelName = var1;
      this.spriteModel = null;
      this.spriteModelInit = null;
   }

   public SpriteModel getSpriteModel() {
      if (this.spriteModelName != null) {
         if (this.spriteModelInit != this.sprite) {
            this.spriteModelInit = this.sprite;
            this.spriteModel = ScriptManager.instance.getSpriteModel(this.spriteModelName);
         }

         return this.spriteModel;
      } else if (this.sprite != null && this.sprite.spriteModel != null) {
         if (this.spriteModelInit != this.sprite) {
            this.spriteModelInit = this.sprite;
            this.spriteModel = this.sprite.spriteModel;
         }

         return this.spriteModel;
      } else {
         return null;
      }
   }

   protected boolean renderModel(float var1, float var2, float var3, ColorInfo var4) {
      if (!PerformanceSettings.FBORenderChunk) {
         return false;
      } else {
         SpriteModel var5 = this.getSpriteModel();
         if (var5 == null) {
            return false;
         } else {
            float var6 = this.sprite.getProperties().isSurfaceOffset() ? (float)this.sprite.getProperties().getSurface() : 0.0F;
            int var7 = IsoCamera.frameState.playerIndex;
            boolean var8 = this instanceof IsoDoor;
            if (this instanceof IsoThumpable) {
               IsoThumpable var9 = (IsoThumpable)this;
               var8 |= var9.isDoor;
            }

            if (var8 && this.square != null && !DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
               var4 = this.square.getLightInfo(var7);
            }

            ColorInfo var16 = var4;
            float var10 = var4.a;
            float var11 = this.getTargetAlpha(IsoCamera.frameState.playerIndex);
            float var12 = this.getAlpha(IsoCamera.frameState.playerIndex);
            if (var12 < var11) {
               var12 += IsoSprite.alphaStep;
               if (var12 > var11) {
                  var12 = var11;
               }
            } else if (var12 > var11) {
               var12 -= IsoSprite.alphaStep;
               if (var12 < var11) {
                  var12 = var11;
               }
            }

            if (var12 < 0.0F) {
               var12 = 0.0F;
            }

            if (var12 > 1.0F) {
               var12 = 1.0F;
            }

            this.setAlpha(IsoCamera.frameState.playerIndex, var12);
            var4.a *= this.getAlpha(IsoCamera.frameState.playerIndex);
            if (DebugOptions.instance.FBORenderChunk.ForceAlphaAndTargetOne.getValue()) {
               var4.a = 1.0F;
            }

            boolean var17 = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
            if (var17) {
               stCol.set(this.getHighlightColor());
               if (this.isBlink()) {
                  ColorInfo var10000 = stCol;
                  var10000.a *= Core.blinkAlpha;
               }

               stCol.r = var4.r * (1.0F - stCol.a) + this.getHighlightColor().r * stCol.a;
               stCol.g = var4.g * (1.0F - stCol.a) + this.getHighlightColor().g * stCol.a;
               stCol.b = var4.b * (1.0F - stCol.a) + this.getHighlightColor().b * stCol.a;
               stCol.a = var4.a;
               var4 = stCol;
            }

            AnimationPlayer var18 = IsoObjectAnimations.getInstance().getAnimationPlayer(this);
            IsoObjectModelDrawer.RenderStatus var13;
            if (var18 == null) {
               ObjectRenderEffects var14 = this.getObjectRenderEffectsToApply();
               if (var14 == null) {
                  var13 = IsoObjectModelDrawer.renderMain(var5, var1, var2, var3, var4, this.getRenderYOffset() + var6);
                  if (this.isOutlineHighlight(var7)) {
                     ColorInfo var15 = stCol.setABGR(this.getOutlineHighlightCol(var7));
                     var15.a = this.isOutlineHlBlink(var7) ? Core.blinkAlpha : 1.0F;
                     IsoObjectModelDrawer.renderMainOutline(var5, var1, var2, var3, var15, this.getRenderYOffset() + var6);
                  }
               } else {
                  var13 = IsoObjectModelDrawer.renderMain(var5, var1 + (float)var14.x1 * 1.5F, var2 + (float)var14.y1 * 1.5F, var3, var4, this.getRenderYOffset() + var6);
               }
            } else {
               var13 = IsoObjectModelDrawer.renderMain(var5, var1, var2, var3, var4, this.getRenderYOffset() + var6, var18);
            }

            var16.a = var10;
            if (var13 == IsoObjectModelDrawer.RenderStatus.Loading && PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching()) {
               FBORenderCell.instance.handleDelayedLoading(this);
               return true;
            } else {
               return var13 == IsoObjectModelDrawer.RenderStatus.Ready;
            }
         }
      }
   }

   public boolean isAnimating() {
      return this.bAnimating;
   }

   public void setAnimating(boolean var1) {
      this.bAnimating = var1;
   }

   public void onAnimationFinished() {
      this.setAnimating(false);
   }

   protected void updateRenderInfoForObjectPicker(float var1, float var2, float var3, ColorInfo var4) {
      float var5 = var1;
      float var6 = var2;
      Texture var7 = this.sprite.getTextureForCurrentFrame(this.getDir());
      if (var7 != null) {
         IsoSpriteInstance var8 = this.sprite.def;
         if (Core.TileScale == 2 && var7.getWidthOrig() == 64 && var7.getHeightOrig() == 128) {
            var8.setScale(2.0F, 2.0F);
         }

         if (Core.TileScale == 2 && var8.scaleX == 2.0F && var8.scaleY == 2.0F && var7.getWidthOrig() == 128 && var7.getHeightOrig() == 256) {
            var8.setScale(1.0F, 1.0F);
         }

         float var9 = var8.scaleX;
         float var10 = var8.scaleY;
         if (FBORenderChunkManager.instance.isCaching()) {
            var1 = PZMath.coordmodulof(var1, 8);
            var2 = PZMath.coordmodulof(var2, 8);
            if (this.getSquare() != this.getRenderSquare()) {
               var1 = var5 - (float)(this.getRenderSquare().chunk.wx * 8);
               var2 = var6 - (float)(this.getRenderSquare().chunk.wy * 8);
            }

            this.sx = 0.0F;
         }

         if (this.sx == 0.0F) {
            this.sx = IsoUtils.XToScreen(var1 + var8.offX, var2 + var8.offY, var3 + var8.offZ, 0);
            this.sy = IsoUtils.YToScreen(var1 + var8.offX, var2 + var8.offY, var3 + var8.offZ, 0);
            this.sx -= this.offsetX;
            this.sy -= this.offsetY + this.renderYOffset * (float)Core.TileScale;
         }

         int var11 = IsoCamera.frameState.playerIndex;
         ObjectRenderInfo var12 = this.getRenderInfo(var11);
         if (!FBORenderObjectHighlight.getInstance().isRendering() && !FBORenderObjectOutline.getInstance().isRendering()) {
            if (FBORenderCell.instance.bRenderTranslucentOnly) {
               var12.m_renderX = this.sx - IsoCamera.frameState.OffX;
               var12.m_renderY = this.sy - IsoCamera.frameState.OffY;
            } else if (FBORenderChunkManager.instance.renderChunk.bHighRes) {
               var12.m_renderX = this.sx + FBORenderChunkManager.instance.getXOffset() - (float)FBORenderChunkManager.instance.renderChunk.w / 4.0F;
               var12.m_renderY = this.sy + FBORenderChunkManager.instance.getYOffset();
            } else {
               var12.m_renderX = this.sx + FBORenderChunkManager.instance.getXOffset();
               var12.m_renderY = this.sy + FBORenderChunkManager.instance.getYOffset();
            }
         } else {
            boolean var13 = true;
         }

         var12.m_renderWidth = (float)var7.getWidthOrig() * var9;
         var12.m_renderHeight = (float)var7.getHeightOrig() * var10;
         var12.m_renderScaleX = var9;
         var12.m_renderScaleY = var10;
         var12.m_renderAlpha = var4.a;
      }
   }

   public boolean isGrave() {
      if (this.getSprite() != null && this.getSprite().getProperties() != null && this.getSprite().getProperties().Is("CustomName") && (((String)Objects.requireNonNull(this.getSprite().getProperties().Val("CustomName"))).contains("Grave") || this.getSprite().getProperties().Val("CustomName").contains("grave"))) {
         return true;
      } else {
         return this.getSprite() != null && this.getSprite().getName() != null && this.getSprite().getName().contains("cemetary");
      }
   }

   public IsoSpriteInstance getOnOverlay() {
      return this.onOverlay;
   }

   public void setOnOverlay(IsoSpriteInstance var1) {
      this.onOverlay = var1;
   }

   public void clearOnOverlay() {
      if (this.getOnOverlay() != null) {
         IsoSpriteInstance.add(this.getOnOverlay());
         this.setOnOverlay((IsoSpriteInstance)null);
      }

   }

   public boolean shouldShowOnOverlay() {
      int var1 = IsoCamera.frameState.playerIndex;
      return this.getSquare() != null && this.getSquare().isSeen(var1) ? ItemContainer.isObjectPowered(this) : false;
   }

   public IsoLightSource getLightSource() {
      return this.lightSource;
   }

   public void setLightSource(IsoLightSource var1) {
      this.lightSource = var1;
   }

   protected boolean shouldLightSourceBeActive() {
      return ItemContainer.isObjectPowered(this);
   }

   protected void addLightSourceToWorld() {
      if (!GameServer.bServer) {
         IsoGridSquare var1 = this.getSquare();
         if (var1 != null) {
            if (!(this instanceof IsoFire)) {
               if (!(this instanceof IsoFireplace)) {
                  if (!(this instanceof IsoLightSwitch)) {
                     if (!(this instanceof IsoMovingObject)) {
                        if (!(this instanceof IsoTelevision)) {
                           PropertyContainer var2 = this.getProperties();
                           if (this.lightSource == null && var2 != null && var2.Is("lightR") && var2.Is("lightG") && var2.Is("lightB")) {
                              float var3 = Float.parseFloat(var2.Val("lightR")) / 255.0F;
                              float var4 = Float.parseFloat(var2.Val("lightG")) / 255.0F;
                              float var5 = Float.parseFloat(var2.Val("lightB")) / 255.0F;
                              int var6 = 10;
                              if (var2.Is("LightRadius") && Integer.parseInt(var2.Val("LightRadius")) > 0) {
                                 var6 = Integer.parseInt(this.sprite.getProperties().Val("LightRadius"));
                              }

                              this.lightSource = new IsoLightSource(var1.getX(), var1.getY(), var1.getZ(), var3, var4, var5, var6);
                              this.lightSource.bActive = this.shouldLightSourceBeActive();
                              this.lightSource.bHydroPowered = false;
                           }

                           if (this.lightSource != null) {
                              IsoWorld.instance.CurrentCell.addLamppost(this.lightSource);
                           }

                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected void removeLightSourceFromWorld() {
      if (this.lightSource != null) {
         IsoWorld.instance.CurrentCell.removeLamppost(this.lightSource);
         this.lightSource = null;
      }

   }

   public void checkLightSourceActive() {
      if (this.getLightSource() != null) {
         boolean var1 = this.shouldLightSourceBeActive();
         if (this.getLightSource().isActive() != var1) {
            this.getLightSource().setActive(var1);
            IsoGridSquare.setRecalcLightTime(-1.0F);
            GameTime.instance.lightSourceUpdate = 100.0F;
         }
      }
   }

   public boolean isGenericCraftingSurface() {
      if (this.getSprite() != null && this.getSprite().getProperties() != null) {
         PropertyContainer var1 = this.getSprite().getProperties();
         if (Objects.equals(var1.Val("GenericCraftingSurface"), "true")) {
            return true;
         } else if (Objects.equals(var1.Val("GenericCraftingSurface"), "false")) {
            return false;
         } else {
            return var1.getSurface() > 25 && var1.getSurface() < 35 && !var1.isSurfaceOffset();
         }
      } else {
         return false;
      }
   }

   public boolean isBush() {
      return this.sprite != null && "f_bushes_1".equals(this.sprite.tilesetName);
   }

   public boolean isFascia() {
      PropertyContainer var1 = this.getProperties();
      if (var1 == null) {
         return false;
      } else {
         String var2 = var1.Val("FasciaEdge");
         if (var2 == null) {
            return false;
         } else {
            IsoGridSquare var3 = this.getSquare();
            IsoGridSquare var6;
            switch (var2) {
               case "North":
                  if (var3.getWallExcludingObject(true, this) != null) {
                     return false;
                  }
                  break;
               case "South":
                  var6 = var3.getAdjacentSquare(IsoDirections.S);
                  if (var6 != null && var6.getWallExcludingObject(true, this) == null) {
                     break;
                  }

                  return false;
               case "East":
                  var6 = var3.getAdjacentSquare(IsoDirections.E);
                  if (var6 != null && var6.getWallExcludingObject(false, this) == null) {
                     break;
                  }

                  return false;
               case "West":
                  if (var3.getWallExcludingObject(false, this) != null) {
                     return false;
                  }
               case "NorthEastCorner":
               case "SouthEastCorner":
               case "SouthWestCorner":
               case "NorthWestCorner":
            }

            return true;
         }
      }
   }

   public IsoGridSquare getFasciaAttachedSquare() {
      IsoCell var1 = IsoWorld.instance.CurrentCell;
      IsoGridSquare var2 = this.getSquare();
      PropertyContainer var3 = this.getProperties();
      String var4 = var3.Val("FasciaEdge");
      boolean var5 = StringUtils.equals(var3.Val("FasciaEdgeReversible"), "true");
      IsoGridSquare var8;
      switch (var4) {
         case "North":
            var8 = var1.getGridSquare(var2.getX(), var2.getY() - 1, var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }

            if (var5) {
               var8 = var1.getGridSquare(var2.getX(), var2.getY(), var2.getZ() + 1);
               if (var8 != null) {
                  return var8;
               }
            }
            break;
         case "South":
            var8 = var1.getGridSquare(var2.getX(), var2.getY() + 1, var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
            break;
         case "East":
            var8 = var1.getGridSquare(var2.getX() + 1, var2.getY(), var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
            break;
         case "West":
            var8 = var1.getGridSquare(var2.getX() - 1, var2.getY(), var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }

            if (var5) {
               var8 = var1.getGridSquare(var2.getX(), var2.getY(), var2.getZ() + 1);
               if (var8 != null) {
                  return var8;
               }
            }
            break;
         case "NorthEastCorner":
            var8 = var1.getGridSquare(var2.getX() + 1, var2.getY() - 1, var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
            break;
         case "SouthEastCorner":
            var8 = var1.getGridSquare(var2.getX() + 1, var2.getY() + 1, var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
            break;
         case "SouthWestCorner":
            var8 = var1.getGridSquare(var2.getX() - 1, var2.getY() + 1, var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
            break;
         case "NorthWestCorner":
            var8 = var1.getGridSquare(var2.getX() - 1, var2.getY() - 1, var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
            break;
         case "NorthAndEast":
         case "SouthAndEast":
         case "SouthAndWest":
         case "NorthAndWest":
            var8 = var1.getGridSquare(var2.getX(), var2.getY(), var2.getZ() + 1);
            if (var8 != null) {
               return var8;
            }
      }

      return null;
   }

   public void setExplored(boolean var1) {
      if (this.getContainer() != null) {
         this.getContainer().setExplored(var1);
      }

      if (this.secondaryContainers != null) {
         for(int var2 = 0; var2 < this.secondaryContainers.size(); ++var2) {
            ItemContainer var3 = (ItemContainer)this.secondaryContainers.get(var2);
            var3.setExplored(var1);
         }
      }

   }

   static {
      initFactory();
   }

   public static class IsoObjectFactory {
      private final byte classID;
      private final String objectName;
      private final int hashCode;

      public IsoObjectFactory(byte var1, String var2) {
         this.classID = var1;
         this.objectName = var2;
         this.hashCode = var2.hashCode();
      }

      protected IsoObject InstantiateObject(IsoCell var1) {
         return new IsoObject(var1);
      }

      public byte getClassID() {
         return this.classID;
      }

      public String getObjectName() {
         return this.objectName;
      }
   }

   public static class OutlineShader {
      public static final OutlineShader instance = new OutlineShader();
      private ShaderProgram shaderProgram;
      private int stepSize;
      private int outlineColor;

      public OutlineShader() {
      }

      public void initShader() {
         this.shaderProgram = ShaderProgram.createShaderProgram("outline", false, false, true);
         if (this.shaderProgram.isCompiled()) {
            this.stepSize = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "stepSize");
            this.outlineColor = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "outlineColor");
            ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
            GL20.glUniform2f(this.stepSize, 0.001F, 0.001F);
            ShaderHelper.glUseProgramObjectARB(0);
         }

      }

      public void setOutlineColor(float var1, float var2, float var3, float var4) {
         SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.outlineColor, var1, var2, var3, var4);
      }

      public void setStepSize(float var1, int var2, int var3) {
         SpriteRenderer.instance.ShaderUpdate2f(this.shaderProgram.getShaderID(), this.stepSize, var1 / (float)var2, var1 / (float)var3);
      }

      public boolean StartShader() {
         if (this.shaderProgram == null) {
            RenderThread.invokeOnRenderContext(this::initShader);
         }

         if (this.shaderProgram.isCompiled()) {
            IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
            return true;
         } else {
            return false;
         }
      }
   }

   public static enum VisionResult {
      NoEffect,
      Blocked,
      Unblocked;

      private VisionResult() {
      }
   }
}
