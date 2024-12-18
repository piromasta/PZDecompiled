package zombie.iso.objects;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.ThumpState;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public class IsoDoor extends IsoObject implements BarricadeAble, Thumpable {
   public int Health = 500;
   public boolean lockedByKey = false;
   private boolean haveKey = false;
   public boolean Locked = false;
   public int MaxHealth = 500;
   public int PushedMaxStrength = 0;
   public int PushedStrength = 0;
   public DoorType type;
   IsoSprite closedSprite;
   public boolean north;
   int gid;
   public boolean open;
   IsoSprite openSprite;
   private boolean destroyed;
   private boolean bHasCurtain;
   private boolean bCurtainInside;
   private boolean bCurtainOpen;
   private static final ColorInfo curtainColor = new ColorInfo();
   KahluaTable table;
   public static final Vector2 tempo = new Vector2();
   private IsoSpriteInstance curtainN;
   private IsoSpriteInstance curtainS;
   private IsoSpriteInstance curtainW;
   private IsoSpriteInstance curtainE;
   private IsoSpriteInstance curtainNopen;
   private IsoSpriteInstance curtainSopen;
   private IsoSpriteInstance curtainWopen;
   private IsoSpriteInstance curtainEopen;
   private static final int[] DoubleDoorNorthSpriteOffset = new int[]{5, 3, 4, 4};
   private static final int[] DoubleDoorWestSpriteOffset = new int[]{4, 4, 5, 3};
   private static final int[] DoubleDoorNorthClosedXOffset = new int[]{0, 1, 2, 3};
   private static final int[] DoubleDoorNorthOpenXOffset = new int[]{0, 0, 3, 3};
   private static final int[] DoubleDoorNorthClosedYOffset = new int[]{0, 0, 0, 0};
   private static final int[] DoubleDoorNorthOpenYOffset = new int[]{0, 1, 1, 0};
   private static final int[] DoubleDoorWestClosedXOffset = new int[]{0, 0, 0, 0};
   private static final int[] DoubleDoorWestOpenXOffset = new int[]{0, 1, 1, 0};
   private static final int[] DoubleDoorWestClosedYOffset = new int[]{0, -1, -2, -3};
   private static final int[] DoubleDoorWestOpenYOffset = new int[]{0, 0, -3, -3};

   public IsoDoor(IsoCell var1) {
      super(var1);
      this.type = IsoDoor.DoorType.WeakWooden;
      this.north = false;
      this.gid = -1;
      this.open = false;
      this.destroyed = false;
   }

   public String getObjectName() {
      return "Door";
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (!this.bHasCurtain) {
         super.render(var1, var2, var3, var4, var5, var6, var7);
      } else {
         this.initCurtainSprites();
         IsoDirections var8 = this.getSpriteEdge(false);
         this.prerender(var1, var2, var3, var4, var5, var6, var8);
         super.render(var1, var2, var3, var4, var5, var6, var7);
         this.postrender(var1, var2, var3, var4, var5, var6, var8);
      }
   }

   public void renderWallTile(IsoDirections var1, float var2, float var3, float var4, ColorInfo var5, boolean var6, boolean var7, Shader var8, Consumer<TextureDraw> var9) {
      this.sx = 0.0F;
      int var10 = getDoubleDoorIndex(this);
      if (var10 != -1) {
         IsoObject var11 = null;
         if (var10 == 2) {
            var11 = getDoubleDoorObject(this, 1);
         } else if (var10 == 3) {
            var11 = getDoubleDoorObject(this, 4);
         }

         if (var11 != null && var11.getSpriteModel() != null) {
            this.updateRenderInfoForObjectPicker(var2, var3, var4, var5);
            this.sx = 0.0F;
            return;
         }
      }

      if (!this.bHasCurtain) {
         super.renderWallTile(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      } else {
         this.initCurtainSprites();
         this.initCurtainColor();
         IsoDirections var12 = this.getSpriteEdge(false);
         this.prerender(var2, var3, var4, curtainColor, var6, var7, var12);
         super.renderWallTile(var1, var2, var3, var4, var5, var6, var7, var8, var9);
         this.postrender(var2, var3, var4, curtainColor, var6, var7, var12);
      }
   }

   private ColorInfo initCurtainColor() {
      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         return curtainColor.set(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         IsoGridSquare var1 = this.getSheetSquare();
         if (var1 == null) {
            return curtainColor.set(1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            ColorInfo var2 = var1.getLightInfo(IsoCamera.frameState.playerIndex);
            if (var2 == null) {
               return curtainColor.set(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
               curtainColor.set(var2);
               IsoDirections var3 = this.getSpriteEdge(false);
               float var10000;
               switch (var3) {
                  case N:
                     var10000 = 0.5F;
                     break;
                  case S:
                     var10000 = 0.5F;
                     break;
                  case W:
                     var10000 = 0.0F;
                     break;
                  case E:
                     var10000 = 1.0F;
                     break;
                  default:
                     var10000 = 0.0F;
               }

               float var4 = var10000;
               switch (var3) {
                  case N:
                     var10000 = 0.0F;
                     break;
                  case S:
                     var10000 = 1.0F;
                     break;
                  case W:
                     var10000 = 0.5F;
                     break;
                  case E:
                     var10000 = 1.5F;
                     break;
                  default:
                     var10000 = 0.0F;
               }

               float var5 = var10000;
               var1.interpolateLight(curtainColor, var4, var5);
               curtainColor.a = FBORenderCell.instance.calculateWindowTargetAlpha(IsoCamera.frameState.playerIndex, this, this.getOppositeSquare(), this.getNorth());
               return curtainColor;
            }
         }
      }
   }

   public void addToWorld() {
      super.addToWorld();
      IsoObjectAnimations.getInstance().addDancingDoor(this);
   }

   public void removeFromWorld() {
      super.removeFromWorld();
      IsoObjectAnimations.getInstance().removeDancingDoor(this);
   }

   public void checkKeyHighlight() {
      int var1 = IsoCamera.frameState.playerIndex;
      Key var2 = Key.highlightDoor[var1];
      if (var2 != null) {
         boolean var3 = this.square.isSeen(var1);
         if (!var3) {
            IsoGridSquare var4 = this.getOppositeSquare();
            var3 = var4 != null && var4.isSeen(var1);
         }

         if (var3) {
            this.checkKeyId();
            if (this.getKeyId() == var2.getKeyId()) {
               this.setHighlighted(true, false);
            }
         }
      }

   }

   private void prerender(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, IsoDirections var7) {
      if (Core.TileScale == 1) {
         switch (var7) {
            case N:
               this.prerender1xN(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case S:
               this.prerender1xS(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case W:
               this.prerender1xW(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case E:
               this.prerender1xE(var1, var2, var3, var4, var5, var6, (Shader)null);
         }

      } else {
         switch (var7) {
            case N:
               this.prerender2xN(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case S:
               this.prerender2xS(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case W:
               this.prerender2xW(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case E:
               this.prerender2xE(var1, var2, var3, var4, var5, var6, (Shader)null);
         }

      }
   }

   private void postrender(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, IsoDirections var7) {
      if (Core.TileScale == 1) {
         switch (var7) {
            case N:
               this.postrender1xN(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case S:
               this.postrender1xS(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case W:
               this.postrender1xW(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case E:
               this.postrender1xE(var1, var2, var3, var4, var5, var6, (Shader)null);
         }

      } else {
         switch (var7) {
            case N:
               this.postrender2xN(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case S:
               this.postrender2xS(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case W:
               this.postrender2xW(var1, var2, var3, var4, var5, var6, (Shader)null);
               break;
            case E:
               this.postrender2xE(var1, var2, var3, var4, var5, var6, (Shader)null);
         }

      }
   }

   private void prerender1xN(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (!this.north && this.open) {
            (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2 - 1.0F, var3, this.dir, this.offsetX + 3.0F, this.offsetY + (float)(this.bCurtainOpen ? -14 : -14), var4, true);
         }
      } else if (this.north && !this.open) {
         (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2 - 1.0F, var3, this.dir, this.offsetX - 1.0F - 1.0F, this.offsetY + -15.0F, var4, true);
      }

   }

   private void postrender1xN(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 10.0F - 1.0F, this.offsetY + -10.0F, var4, true);
         }
      } else if (!this.north && this.open) {
         (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 4.0F, this.offsetY + (float)(this.bCurtainOpen ? -10 : -10), var4, true);
      }

   }

   private void prerender1xS(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert !this.north && this.open;

      if (!this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX + (float)((this.bCurtainOpen ? -14 : -14) / 2), this.offsetY + (float)((this.bCurtainOpen ? -16 : -16) / 2), var4, true);
      }

   }

   private void postrender1xS(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert !this.north && this.open;

      if (this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2 + 1.0F, var3, this.dir, this.offsetX + (float)((this.bCurtainOpen ? -28 : -28) / 2), this.offsetY + (float)((this.bCurtainOpen ? -8 : -8) / 2), var4, true);
      }

   }

   private void prerender1xW(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (this.north && this.open) {
            (this.bCurtainOpen ? this.curtainEopen : this.curtainE).render((IsoObject)null, var1 - 1.0F, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -16 : -18), this.offsetY + (float)(this.bCurtainOpen ? -14 : -15), var4, true);
         }

         if (!this.north && this.open) {
            (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2 - 1.0F, var3, this.dir, this.offsetX + 3.0F, this.offsetY + (float)(this.bCurtainOpen ? -14 : -14), var4, true);
         }
      } else {
         if (this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2 - 1.0F, var3, this.dir, this.offsetX - 1.0F - 1.0F, this.offsetY + -15.0F, var4, true);
         }

         if (!this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainEopen : this.curtainE).render((IsoObject)null, var1 - 1.0F, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -12 : -14), this.offsetY + (float)(this.bCurtainOpen ? -14 : -15), var4, true);
         }
      }

   }

   private void postrender1xW(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 10.0F - 1.0F, this.offsetY + -10.0F, var4, true);
         }

         if (!this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainWopen : this.curtainW).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 2.0F - 1.0F, this.offsetY + -10.0F, var4, true);
         }
      } else {
         if (this.north && this.open) {
            (this.bCurtainOpen ? this.curtainWopen : this.curtainW).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 9.0F, this.offsetY + -10.0F, var4, true);
         }

         if (!this.north && this.open) {
            (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 4.0F, this.offsetY + (float)(this.bCurtainOpen ? -10 : -10), var4, true);
         }
      }

   }

   private void prerender1xE(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert this.north && this.open;

      if (!this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainEopen : this.curtainE).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX + (float)((this.bCurtainOpen ? -13 : -18) / 2), this.offsetY + (float)((this.bCurtainOpen ? -15 : -18) / 2), var4, true);
      }

   }

   private void postrender1xE(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert this.north && this.open;

      if (this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainWopen : this.curtainW).render((IsoObject)null, var1 + 1.0F, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? 0 : 0), this.offsetY + (float)(this.bCurtainOpen ? 0 : 0), var4, true);
      }

   }

   private void prerender2xN(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (!this.north && this.open) {
            (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2 - 1.0F, var3, this.dir, this.offsetX + 7.0F, this.offsetY + (float)(this.bCurtainOpen ? -28 : -28), var4, false);
         }
      } else if (this.north && !this.open) {
         (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2 - 1.0F, var3, this.dir, this.offsetX - 3.0F, this.offsetY + (float)(this.bCurtainOpen ? -30 : -30), var4, false);
      }

   }

   private void postrender2xN(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 20.0F, this.offsetY + (float)(this.bCurtainOpen ? -20 : -20), var4, false);
         }
      } else if (!this.north && this.open) {
         (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 8.0F, this.offsetY + (float)(this.bCurtainOpen ? -20 : -20), var4, false);
      }

   }

   private void prerender2xS(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert !this.north && this.open;

      if (!this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainSopen : this.curtainS).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -14 : -14), this.offsetY + (float)(this.bCurtainOpen ? -16 : -16), var4, false);
      }

   }

   private void postrender2xS(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert !this.north && this.open;

      if (this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainNopen : this.curtainN).render((IsoObject)null, var1, var2 + 1.0F, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -28 : -28), this.offsetY + (float)(this.bCurtainOpen ? -8 : -8), var4, false);
      }

   }

   private void prerender2xW(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (this.north && this.open) {
            (this.bCurtainOpen ? this.curtainEopen : this.curtainE).render((IsoObject)null, var1 - 1.0F, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -32 : -37), this.offsetY + (float)(this.bCurtainOpen ? -28 : -31), var4, false);
         }
      } else if (!this.north && !this.open) {
         (this.bCurtainOpen ? this.curtainEopen : this.curtainE).render((IsoObject)null, var1 - 1.0F, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -22 : -26), this.offsetY + (float)(this.bCurtainOpen ? -28 : -31), var4, false);
      }

   }

   private void postrender2xW(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.bCurtainInside) {
         if (!this.north && !this.open) {
            (this.bCurtainOpen ? this.curtainWopen : this.curtainW).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 5.0F, this.offsetY + (float)(this.bCurtainOpen ? -20 : -20), var4, false);
         }
      } else if (this.north && this.open) {
         (this.bCurtainOpen ? this.curtainWopen : this.curtainW).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX - 19.0F, this.offsetY + (float)(this.bCurtainOpen ? -20 : -20), var4, false);
      }

   }

   private void prerender2xE(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert this.north && this.open;

      if (!this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainEopen : this.curtainE).render((IsoObject)null, var1, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? -13 : -18), this.offsetY + (float)(this.bCurtainOpen ? -15 : -18), var4, false);
      }

   }

   private void postrender2xE(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      assert this.north && this.open;

      if (this.bCurtainInside) {
         (this.bCurtainOpen ? this.curtainWopen : this.curtainW).render((IsoObject)null, var1 + 1.0F, var2, var3, this.dir, this.offsetX + (float)(this.bCurtainOpen ? 0 : 0), this.offsetY + (float)(this.bCurtainOpen ? 0 : 0), var4, false);
      }

   }

   public IsoDirections getSpriteEdge(boolean var1) {
      if (this.open && !var1) {
         PropertyContainer var2 = this.getProperties();
         if (var2 != null && var2.Is("GarageDoor")) {
            return this.north ? IsoDirections.N : IsoDirections.W;
         } else if (var2 != null && var2.Is(IsoFlagType.attachedE)) {
            return IsoDirections.E;
         } else if (var2 != null && var2.Is(IsoFlagType.attachedS)) {
            return IsoDirections.S;
         } else {
            return this.north ? IsoDirections.W : IsoDirections.N;
         }
      } else {
         return this.north ? IsoDirections.N : IsoDirections.W;
      }
   }

   public IsoDoor(IsoCell var1, IsoGridSquare var2, IsoSprite var3, boolean var4) {
      this.type = IsoDoor.DoorType.WeakWooden;
      this.north = false;
      this.gid = -1;
      this.open = false;
      this.destroyed = false;
      this.open = var3.getProperties().Is(IsoFlagType.open);
      this.OutlineOnMouseover = true;
      this.PushedMaxStrength = this.PushedStrength = 2500;
      byte var5 = 2;
      if (var3.getProperties().Is("DoubleDoor")) {
         var5 = 4;
      }

      if (var3.getProperties().Is("GarageDoor")) {
         var5 = 8;
      }

      this.closedSprite = this.open ? IsoSprite.getSprite(IsoSpriteManager.instance, var3, -var5) : var3;
      this.openSprite = this.open ? var3 : IsoSprite.getSprite(IsoSpriteManager.instance, (IsoSprite)var3, var5);
      this.sprite = this.open ? this.openSprite : this.closedSprite;
      this.square = var2;
      this.north = var4;
      switch (this.type) {
         case WeakWooden:
            this.MaxHealth = this.Health = 500;
            break;
         case StrongWooden:
            this.MaxHealth = this.Health = 800;
      }

      if (this.getProperties().Is("forceLocked")) {
         this.MaxHealth = this.Health = 2000;
      }

      if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
         this.MaxHealth = this.Health = 100;
      }

      byte var6 = 69;
      if (SandboxOptions.instance.LockedHouses.getValue() == 1) {
         var6 = -1;
      } else if (SandboxOptions.instance.LockedHouses.getValue() == 2) {
         var6 = 5;
      } else if (SandboxOptions.instance.LockedHouses.getValue() == 3) {
         var6 = 10;
      } else if (SandboxOptions.instance.LockedHouses.getValue() == 4) {
         var6 = 50;
      } else if (SandboxOptions.instance.LockedHouses.getValue() == 5) {
         var6 = 60;
      } else if (SandboxOptions.instance.LockedHouses.getValue() == 6) {
         var6 = 70;
      }

      if (var6 > -1) {
         this.Locked = Rand.Next(100) < var6;
         if (this.Locked && Rand.Next(3) == 0) {
            this.lockedByKey = true;
         }
      }

      if (this.getProperties().Is("forceLocked")) {
         this.Locked = true;
         this.lockedByKey = true;
      }

      if (this.open) {
         this.Locked = false;
         this.lockedByKey = false;
      }

   }

   public IsoDoor(IsoCell var1, IsoGridSquare var2, String var3, boolean var4) {
      this.type = IsoDoor.DoorType.WeakWooden;
      this.north = false;
      this.gid = -1;
      this.open = false;
      this.destroyed = false;
      this.OutlineOnMouseover = true;
      this.PushedMaxStrength = this.PushedStrength = 2500;
      this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, 0);
      this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, 2);
      this.sprite = this.closedSprite;
      String var5 = this.closedSprite.getProperties().Val("GarageDoor");
      if (var5 != null) {
         int var6 = Integer.parseInt(var5);
         if (var6 <= 3) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, 8);
         } else {
            this.openSprite = this.sprite;
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, -8);
         }
      }

      this.square = var2;
      this.north = var4;
      switch (this.type) {
         case WeakWooden:
            this.MaxHealth = this.Health = 500;
            break;
         case StrongWooden:
            this.MaxHealth = this.Health = 800;
      }

      if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
         this.MaxHealth = this.Health = 100;
      }

   }

   public IsoDoor(IsoCell var1, IsoGridSquare var2, String var3, boolean var4, KahluaTable var5) {
      this.type = IsoDoor.DoorType.WeakWooden;
      this.north = false;
      this.gid = -1;
      this.open = false;
      this.destroyed = false;
      this.OutlineOnMouseover = true;
      this.PushedMaxStrength = this.PushedStrength = 2500;
      this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, 0);
      this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, 2);
      this.table = var5;
      this.sprite = this.closedSprite;
      String var6 = this.sprite.getProperties().Val("GarageDoor");
      if (var6 != null) {
         int var7 = Integer.parseInt(var6);
         if (var7 <= 3) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, 8);
         } else {
            this.openSprite = this.sprite;
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var3, -8);
         }
      }

      this.square = var2;
      this.north = var4;
      switch (this.type) {
         case WeakWooden:
            this.MaxHealth = this.Health = 500;
            break;
         case StrongWooden:
            this.MaxHealth = this.Health = 800;
      }

      if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
         this.MaxHealth = this.Health = 100;
      }

   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.open = var1.get() == 1;
      this.Locked = var1.get() == 1;
      this.north = var1.get() == 1;
      this.Health = var1.getInt();
      this.MaxHealth = var1.getInt();
      this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, var1.getInt());
      this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, var1.getInt());
      this.OutlineOnMouseover = true;
      this.PushedMaxStrength = this.PushedStrength = 2500;
      this.keyId = var1.getInt();
      this.lockedByKey = var1.get() == 1;
      byte var4 = var1.get();
      if ((var4 & 1) != 0) {
         this.bHasCurtain = true;
         this.bCurtainOpen = (var4 & 2) != 0;
         this.bCurtainInside = (var4 & 4) != 0;
      }

   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)(this.open ? 1 : 0));
      var1.put((byte)(this.Locked ? 1 : 0));
      var1.put((byte)(this.north ? 1 : 0));
      var1.putInt(this.Health);
      var1.putInt(this.MaxHealth);
      var1.putInt(this.closedSprite.ID);
      var1.putInt(this.openSprite.ID);
      var1.putInt(this.getKeyId());
      var1.put((byte)(this.isLockedByKey() ? 1 : 0));
      byte var3 = 0;
      if (this.bHasCurtain) {
         var3 = (byte)(var3 | 1);
         if (this.bCurtainOpen) {
            var3 = (byte)(var3 | 2);
         }

         if (this.bCurtainInside) {
            var3 = (byte)(var3 | 4);
         }
      }

      var1.put(var3);
   }

   public void saveState(ByteBuffer var1) throws IOException {
      var1.put((byte)(this.open ? 1 : 0));
      var1.put((byte)(this.Locked ? 1 : 0));
      var1.put((byte)(this.lockedByKey ? 1 : 0));
   }

   public void loadState(ByteBuffer var1) throws IOException {
      boolean var2 = var1.get() == 1;
      boolean var3 = var1.get() == 1;
      boolean var4 = var1.get() == 1;
      if (var2 != this.open) {
         this.open = var2;
         this.sprite = var2 ? this.openSprite : this.closedSprite;
      }

      if (var3 != this.Locked) {
         this.Locked = var3;
      }

      if (var4 != this.lockedByKey) {
         this.lockedByKey = var4;
      }

   }

   public boolean isDestroyed() {
      return this.destroyed;
   }

   public boolean IsOpen() {
      return this.open;
   }

   public boolean IsStrengthenedByPushedItems() {
      return false;
   }

   public boolean onMouseLeftClick(int var1, int var2) {
      return false;
   }

   public boolean TestPathfindCollide(IsoMovingObject var1, IsoGridSquare var2, IsoGridSquare var3) {
      boolean var4 = this.north;
      if (!this.isBarricaded()) {
         return false;
      } else if (var1 instanceof IsoSurvivor && ((IsoSurvivor)var1).getInventory().contains("Hammer")) {
         return false;
      } else {
         if (this.open) {
            var4 = !var4;
         }

         if (var2 == this.square) {
            if (var4 && var3.getY() < var2.getY()) {
               return true;
            }

            if (!var4 && var3.getX() < var2.getX()) {
               return true;
            }
         } else {
            if (var4 && var3.getY() > var2.getY()) {
               return true;
            }

            if (!var4 && var3.getX() > var2.getX()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean TestCollide(IsoMovingObject var1, IsoGridSquare var2, IsoGridSquare var3) {
      boolean var4 = this.north;
      if (this.open) {
         return false;
      } else {
         if (var2 == this.square) {
            if (var4 && var3.getY() < var2.getY()) {
               if (var1 != null) {
                  var1.collideWith(this);
               }

               return true;
            }

            if (!var4 && var3.getX() < var2.getX()) {
               if (var1 != null) {
                  var1.collideWith(this);
               }

               return true;
            }
         } else {
            if (var4 && var3.getY() > var2.getY()) {
               if (var1 != null) {
                  var1.collideWith(this);
               }

               return true;
            }

            if (!var4 && var3.getX() > var2.getX()) {
               if (var1 != null) {
                  var1.collideWith(this);
               }

               return true;
            }
         }

         return false;
      }
   }

   public IsoObject.VisionResult TestVision(IsoGridSquare var1, IsoGridSquare var2) {
      boolean var3 = this.sprite != null && this.sprite.getProperties().Is("doorTrans");
      if (this.sprite != null && this.sprite.getProperties().Is("GarageDoor") && this.open) {
         var3 = true;
      }

      if (this.open) {
         var3 = true;
      } else if (this.bHasCurtain && !this.bCurtainOpen) {
         var3 = false;
      }

      boolean var4 = this.north;
      if (this.open) {
         var4 = !var4;
      }

      if (var2.getZ() != var1.getZ()) {
         return IsoObject.VisionResult.NoEffect;
      } else {
         if (var1 == this.square) {
            if (var4 && var2.getY() < var1.getY()) {
               if (var3) {
                  return IsoObject.VisionResult.Unblocked;
               }

               return IsoObject.VisionResult.Blocked;
            }

            if (!var4 && var2.getX() < var1.getX()) {
               if (var3) {
                  return IsoObject.VisionResult.Unblocked;
               }

               return IsoObject.VisionResult.Blocked;
            }
         } else {
            if (var4 && var2.getY() > var1.getY()) {
               if (var3) {
                  return IsoObject.VisionResult.Unblocked;
               }

               return IsoObject.VisionResult.Blocked;
            }

            if (!var4 && var2.getX() > var1.getX()) {
               if (var3) {
                  return IsoObject.VisionResult.Unblocked;
               }

               return IsoObject.VisionResult.Blocked;
            }
         }

         return IsoObject.VisionResult.NoEffect;
      }
   }

   public void Thump(IsoMovingObject var1) {
      if (!this.isDestroyed()) {
         if (var1 instanceof IsoGameCharacter) {
            Thumpable var2 = this.getThumpableFor((IsoGameCharacter)var1);
            if (var2 == null) {
               return;
            }

            if (var2 != this) {
               var2.Thump(var1);
               return;
            }
         }

         if (var1 instanceof IsoZombie) {
            if (((IsoZombie)var1).cognition == 1 && !this.open && (!this.Locked || var1.getCurrentSquare() != null && !var1.getCurrentSquare().Is(IsoFlagType.exterior))) {
               this.ToggleDoor((IsoGameCharacter)var1);
               if (this.open) {
                  return;
               }
            }

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

            int var3 = ThumpState.getFastForwardDamageMultiplier();
            int var4 = ((IsoZombie)var1).strength;
            if (var5 >= 2) {
               this.DirtySlice();
               this.Damage(((IsoZombie)var1).strength * var3);
               if (SandboxOptions.instance.Lore.Strength.getValue() == 1) {
                  this.Damage(var5 * 2 * var3);
               }
            }

            if (Core.GameMode.equals("LastStand")) {
               this.Damage(1 * var3);
            }

            WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
            this.setRenderEffect(RenderEffectType.Hit_Door, true);
         }

         if (this.Health <= 0) {
            if (this.getSquare().getBuilding() != null) {
               this.getSquare().getBuilding().forceAwake();
            }

            this.playDoorSound(((IsoGameCharacter)var1).getEmitter(), "Break");
            if (GameServer.bServer) {
               GameServer.PlayWorldSoundServer((IsoGameCharacter)var1, "BreakDoor", false, var1.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
            }

            WorldSoundManager.instance.addSound((Object)null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
            var1.setThumpTarget((Thumpable)null);
            if (destroyDoubleDoor(this)) {
               return;
            }

            if (destroyGarageDoor(this)) {
               return;
            }

            this.destroy();
         }

      }
   }

   public Thumpable getThumpableFor(IsoGameCharacter var1) {
      IsoBarricade var2 = this.getBarricadeForCharacter(var1);
      if (var2 != null) {
         return var2;
      } else {
         var2 = this.getBarricadeOppositeCharacter(var1);
         if (var2 != null) {
            return var2;
         } else {
            return !this.isDestroyed() && !this.IsOpen() ? this : null;
         }
      }
   }

   public float getThumpCondition() {
      return this.getMaxHealth() <= 0 ? 0.0F : (float)PZMath.clamp(this.getHealth(), 0, this.getMaxHealth()) / (float)this.getMaxHealth();
   }

   public void WeaponHit(IsoGameCharacter var1, HandWeapon var2) {
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (GameClient.bClient) {
         if (var3 != null) {
            GameClient.instance.sendWeaponHit(var3, var2, this);
         }

         this.setRenderEffect(RenderEffectType.Hit_Door, true);
      } else {
         Thumpable var4 = this.getThumpableFor(var1);
         if (var4 != null) {
            if (var4 instanceof IsoBarricade) {
               ((IsoBarricade)var4).WeaponHit(var1, var2);
            } else {
               LuaEventManager.triggerEvent("OnWeaponHitThumpable", var1, var2, this);
               if (!this.open) {
                  if (!this.isDestroyed()) {
                     int var5 = var1.getPerkLevel(PerkFactory.Perks.Strength);
                     float var6 = 1.0F;
                     if (var5 == 0) {
                        var6 = 0.5F;
                     } else if (var5 == 1) {
                        var6 = 0.63F;
                     } else if (var5 == 2) {
                        var6 = 0.76F;
                     } else if (var5 == 3) {
                        var6 = 0.89F;
                     } else if (var5 == 4) {
                        var6 = 1.02F;
                     }

                     if (var5 == 6) {
                        var6 = 1.15F;
                     } else if (var5 == 7) {
                        var6 = 1.27F;
                     } else if (var5 == 8) {
                        var6 = 1.3F;
                     } else if (var5 == 9) {
                        var6 = 1.45F;
                     } else if (var5 == 10) {
                        var6 = 1.7F;
                     }

                     this.Damage((int)((float)var2.getDoorDamage() * 2.0F * var6));
                     this.setRenderEffect(RenderEffectType.Hit_Door, true);
                     if (Rand.Next(10) == 0) {
                        this.Damage((int)((float)var2.getDoorDamage() * 6.0F * var6));
                     }

                     float var7 = GameTime.getInstance().getThirtyFPSMultiplier();
                     switch (var1.getPerkLevel(PerkFactory.Perks.Fitness)) {
                        case 0:
                           var1.exert(0.01F * var7);
                           break;
                        case 1:
                           var1.exert(0.007F * var7);
                           break;
                        case 2:
                           var1.exert(0.0065F * var7);
                           break;
                        case 3:
                           var1.exert(0.006F * var7);
                           break;
                        case 4:
                           var1.exert(0.005F * var7);
                           break;
                        case 5:
                           var1.exert(0.004F * var7);
                           break;
                        case 6:
                           var1.exert(0.0035F * var7);
                           break;
                        case 7:
                           var1.exert(0.003F * var7);
                           break;
                        case 8:
                           var1.exert(0.0025F * var7);
                           break;
                        case 9:
                           var1.exert(0.002F * var7);
                     }

                     this.DirtySlice();
                     if (var2.getDoorHitSound() != null) {
                        if (var3 != null) {
                           var3.setMeleeHitSurface(this.getSoundPrefix());
                        }

                        var1.getEmitter().playSound(var2.getDoorHitSound(), this);
                        if (GameServer.bServer) {
                           GameServer.PlayWorldSoundServer(var1, var2.getDoorHitSound(), false, this.getSquare(), 1.0F, 20.0F, 2.0F, false);
                        }
                     }

                     WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                     if (!this.IsStrengthenedByPushedItems() && this.Health <= 0 || this.IsStrengthenedByPushedItems() && this.Health <= -this.PushedMaxStrength) {
                        this.playDoorSound(var1.getEmitter(), "Break");
                        if (GameServer.bServer) {
                           GameServer.PlayWorldSoundServer(var1, "BreakDoor", false, this.getSquare(), 0.2F, 20.0F, 1.1F, true);
                        }

                        WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                        if (destroyDoubleDoor(this)) {
                           return;
                        }

                        if (destroyGarageDoor(this)) {
                           return;
                        }

                        this.destroy();
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                     }

                  }
               }
            }
         }
      }
   }

   public void destroy() {
      if (this.sprite != null && this.sprite.getProperties().Is("GarageDoor")) {
         this.destroyed = true;
         this.square.transmitRemoveItemFromSquare(this);
      } else {
         PropertyContainer var1 = this.getProperties();
         if (var1 != null) {
            String var2 = var1.Val("Material");
            String var3 = var1.Val("Material2");
            String var4 = var1.Val("Material3");
            int var6;
            if (StringUtils.isNullOrEmpty(var2) && StringUtils.isNullOrEmpty(var3) && StringUtils.isNullOrEmpty(var4)) {
               int var5 = Rand.Next(2) + 1;

               for(var6 = 0; var6 < var5; ++var6) {
                  this.square.AddWorldInventoryItem("Base.Plank", 0.0F, 0.0F, 0.0F);
               }
            } else {
               this.addItemsFromProperties();
            }

            InventoryItem var8 = InventoryItemFactory.CreateItem("Base.Doorknob");
            var8.setKeyId(this.checkKeyId());
            this.square.AddWorldInventoryItem(var8, 0.0F, 0.0F, 0.0F);
            var6 = Rand.Next(3);

            for(int var7 = 0; var7 < var6; ++var7) {
               this.square.AddWorldInventoryItem("Base.Hinge", 0.0F, 0.0F, 0.0F);
            }

            if (this.bHasCurtain) {
               this.square.AddWorldInventoryItem("Base.Sheet", 0.0F, 0.0F, 0.0F);
            }

            this.destroyed = true;
            this.square.transmitRemoveItemFromSquare(this);
         }
      }
   }

   public IsoGridSquare getOtherSideOfDoor(IsoGameCharacter var1) {
      if (this.north) {
         return var1.getCurrentSquare().getRoom() == this.square.getRoom() ? IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ()) : IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
      } else {
         return var1.getCurrentSquare().getRoom() == this.square.getRoom() ? IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ()) : IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
      }
   }

   /** @deprecated */
   @Deprecated
   public boolean isExteriorDoor(IsoGameCharacter var1) {
      return this.isExterior();
   }

   public boolean isExterior() {
      IsoGridSquare var1 = this.getSquare();
      IsoGridSquare var2 = this.getOppositeSquare();
      if (var2 == null) {
         return false;
      } else if (var1.Is(IsoFlagType.exterior) && var2.getBuilding() != null && var2.getBuilding().getDef() != null) {
         return true;
      } else {
         return var1.getBuilding() != null && var1.getBuilding().getDef() != null && var2.Is(IsoFlagType.exterior);
      }
   }

   public boolean isHoppable() {
      if (this.IsOpen()) {
         return false;
      } else if (this.closedSprite == null) {
         return false;
      } else {
         PropertyContainer var1 = this.closedSprite.getProperties();
         return var1.Is(IsoFlagType.HoppableN) || var1.Is(IsoFlagType.HoppableW);
      }
   }

   public boolean canClimbOver(IsoGameCharacter var1) {
      if (this.square == null) {
         return false;
      } else if (!this.isHoppable()) {
         return false;
      } else {
         return var1 == null || IsoWindow.canClimbThroughHelper(var1, this.getSquare(), this.getOppositeSquare(), this.north);
      }
   }

   public boolean couldBeOpen(IsoGameCharacter var1) {
      if (var1 instanceof IsoAnimal) {
         return false;
      } else if (this.isBarricaded()) {
         return false;
      } else if (this.isLockedByKey() && var1 instanceof IsoPlayer && (var1.getCurrentSquare().Is(IsoFlagType.exterior) || this.getProperties().Is("forceLocked") || this.getModData().rawget("CustomLock") != null && this.getModData().rawget("CustomLock") instanceof Boolean && (Boolean)this.getModData().rawget("CustomLock")) && var1.getInventory().haveThisKeyId(this.getKeyId()) == null) {
         return false;
      } else if (this.isObstructed()) {
         return false;
      } else {
         return !"Tutorial".equals(Core.getInstance().getGameMode()) || !this.isLockedByKey();
      }
   }

   public void ToggleDoorActual(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (!(var1 instanceof IsoAnimal)) {
         if (Core.bDebug && DebugOptions.instance.Cheat.Door.Unlock.getValue()) {
            this.Locked = false;
            this.setLockedByKey(false);
         }

         if (this.isHoppable()) {
            this.Locked = false;
            this.setLockedByKey(false);
         }

         if (this.isBarricaded()) {
            if (var1 != null) {
               this.playDoorSound(var1.getEmitter(), "Blocked");
               var1.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBarricaded"), 255, 255, 255, 256.0F);
               this.setRenderEffect(RenderEffectType.Hit_Door, true);
            }

         } else {
            this.checkKeyId();
            if (this.Locked && !this.lockedByKey && this.getKeyId() != -1) {
               this.lockedByKey = true;
            }

            if (var1 instanceof IsoPlayer) {
               if (!this.open) {
                  ((IsoPlayer)var1).TimeSinceOpenDoor = 0.0F;
               } else {
                  ((IsoPlayer)var1).TimeSinceCloseDoor = 0.0F;
               }
            }

            this.DirtySlice();
            IsoGridSquare.RecalcLightTime = -1.0F;
            ++Core.dirtyGlobalLightsCount;
            GameTime.instance.lightSourceUpdate = 100.0F;
            this.square.InvalidateSpecialObjectPaths();
            if (this.isLockedByKey() && var1 != null && var1 instanceof IsoPlayer && (var1.getCurrentSquare().Is(IsoFlagType.exterior) || this.getProperties().Is("forceLocked") || this.getModData().rawget("CustomLock") != null && this.getModData().rawget("CustomLock") instanceof Boolean && (Boolean)this.getModData().rawget("CustomLock")) && !this.open) {
               if (var1.getInventory().haveThisKeyId(this.getKeyId()) == null) {
                  this.playDoorSound(var1.getEmitter(), "Locked");
                  this.setRenderEffect(RenderEffectType.Hit_Door, true);
                  return;
               }

               this.playDoorSound(var1.getEmitter(), "Unlock");
               this.playDoorSound(var1.getEmitter(), "Open");
               this.Locked = false;
               this.setLockedByKey(false);
            }

            boolean var3 = var1 instanceof IsoPlayer && !var1.getCurrentSquare().isOutside();
            if ("Tutorial".equals(Core.getInstance().getGameMode()) && this.isLockedByKey()) {
               var3 = false;
            }

            boolean var4;
            if (var1 instanceof IsoPlayer && this.getSprite().getProperties().Is("GarageDoor")) {
               var4 = this.getSprite().getProperties().Is("InteriorSide");
               if (var4) {
                  var3 = this.north ? var1.getY() >= this.getY() : var1.getX() >= this.getX();
               } else {
                  var3 = this.north ? var1.getY() < this.getY() : var1.getX() < this.getX();
               }
            }

            if (this.Locked && !var3 && !this.open) {
               this.playDoorSound(var1.getEmitter(), "Locked");
               this.setRenderEffect(RenderEffectType.Hit_Door, true);
            } else if (this.getSprite().getProperties().Is("DoubleDoor")) {
               if (isDoubleDoorObstructed(this)) {
                  if (var1 != null) {
                     this.playDoorSound(var1.getEmitter(), "Blocked");
                     var1.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                  }

               } else {
                  var4 = this.open;
                  toggleDoubleDoor(this, true);
                  if (var4 != this.open) {
                     this.playDoorSound(var1.getEmitter(), this.open ? "Open" : "Close");
                     if (var2 != null && var2.isLocalPlayer()) {
                        var2.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                     }
                  }

               }
            } else if (this.getSprite().getProperties().Is("GarageDoor")) {
               if (isGarageDoorObstructed(this)) {
                  if (var1 != null) {
                     this.playDoorSound(var1.getEmitter(), "Blocked");
                     var1.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                  }

               } else {
                  var4 = this.open;
                  toggleGarageDoor(this, true);
                  if (var4 != this.open) {
                     this.playDoorSound(var1.getEmitter(), this.open ? "Open" : "Close");
                  }

                  if (var2 != null && var2.isLocalPlayer()) {
                     var2.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                  }

               }
            } else if (this.isObstructed()) {
               if (var1 != null) {
                  this.playDoorSound(var1.getEmitter(), "Blocked");
                  var1.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
               }

            } else {
               this.Locked = false;
               this.setLockedByKey(false);
               if (var1 instanceof IsoPlayer) {
                  for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
                     LosUtil.cachecleared[var5] = true;
                  }

                  IsoGridSquare.setRecalcLightTime(-1.0F);
               }

               this.open = !this.open;
               WeatherFxMask.forceMaskUpdateAll();
               this.sprite = this.closedSprite;
               if (this.open) {
                  if (var1 != null) {
                     this.playDoorSound(var1.getEmitter(), "Open");
                  }

                  this.sprite = this.openSprite;
               } else if (var1 != null) {
                  this.playDoorSound(var1.getEmitter(), "Close");
               }

               this.square.RecalcProperties();
               this.syncIsoObject(false, (byte)(this.open ? 1 : 0), (UdpConnection)null, (ByteBuffer)null);
               PolygonalMap2.instance.squareChanged(this.square);
               LuaEventManager.triggerEvent("OnContainerUpdate");
               this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
               if (var2 != null && var2.isLocalPlayer()) {
                  var2.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
               }

               SpriteModel var6 = this.getSpriteModel();
               if (var6 != null && var6.animationName != null) {
                  IsoObjectAnimations.getInstance().addObject(this, var6, this.open ? "Open" : "Close");
               }

               this.setAnimating(true);
            }
         }
      }
   }

   public void syncIsoObjectSend(ByteBufferWriter var1) {
      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      byte var2 = (byte)this.square.getObjects().indexOf(this);
      var1.putByte(var2);
      var1.putByte((byte)1);
      var1.putByte((byte)0);
      var1.putBoolean(this.open);
      var1.putBoolean(this.Locked);
      var1.putBoolean(this.lockedByKey);
   }

   public void syncIsoObject(boolean var1, byte var2, UdpConnection var3, ByteBuffer var4) {
      if (this.square == null) {
         System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
      } else if (this.getObjectIndex() == -1) {
         PrintStream var10000 = System.out;
         String var10001 = this.getClass().getSimpleName();
         var10000.println("ERROR: " + var10001 + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
      } else {
         boolean var5 = var4 != null && var4.get() == 1;
         boolean var6 = var4 != null && var4.get() == 1;
         boolean var7 = var4 != null && var4.get() == 1;
         short var8 = -1;
         if ((GameServer.bServer || GameClient.bClient) && var4 != null) {
            var8 = var4.getShort();
         }

         if (GameClient.bClient && !var1) {
            var8 = IsoPlayer.getInstance().getOnlineID();
            ByteBufferWriter var13 = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(var13);
            this.syncIsoObjectSend(var13);
            var13.putShort(var8);
            PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
         } else {
            UdpConnection var10;
            ByteBufferWriter var11;
            Iterator var12;
            if (GameServer.bServer && !var1) {
               var12 = GameServer.udpEngine.connections.iterator();

               while(var12.hasNext()) {
                  var10 = (UdpConnection)var12.next();
                  var11 = var10.startPacket();
                  PacketTypes.PacketType.SyncIsoObject.doPacket(var11);
                  this.syncIsoObjectSend(var11);
                  var11.putShort(var8);
                  PacketTypes.PacketType.SyncIsoObject.send(var10);
               }
            } else if (var1) {
               if (GameClient.bClient && var8 != -1) {
                  IsoPlayer var9 = (IsoPlayer)GameClient.IDToPlayerMap.get(var8);
                  if (var9 != null) {
                     var9.networkAI.setNoCollision(1000L);
                  }
               }

               if (getDoubleDoorIndex(this) != -1) {
                  if (var5 != this.open) {
                     toggleDoubleDoor(this, false);
                  }
               } else if (var5) {
                  this.open = true;
                  this.sprite = this.openSprite;
               } else {
                  this.open = false;
                  this.sprite = this.closedSprite;
               }

               this.Locked = var6;
               this.lockedByKey = var7;
               if (GameServer.bServer) {
                  var12 = GameServer.udpEngine.connections.iterator();

                  label79:
                  while(true) {
                     do {
                        if (!var12.hasNext()) {
                           break label79;
                        }

                        var10 = (UdpConnection)var12.next();
                     } while((var3 == null || var10.getConnectedGUID() == var3.getConnectedGUID()) && var3 != null);

                     var11 = var10.startPacket();
                     PacketTypes.PacketType.SyncIsoObject.doPacket(var11);
                     this.syncIsoObjectSend(var11);
                     var11.putShort(var8);
                     PacketTypes.PacketType.SyncIsoObject.send(var10);
                  }
               }
            }
         }

         this.square.InvalidateSpecialObjectPaths();
         this.square.RecalcProperties();
         this.square.RecalcAllWithNeighbours(true);

         for(int var14 = 0; var14 < IsoPlayer.numPlayers; ++var14) {
            LosUtil.cachecleared[var14] = true;
         }

         IsoGridSquare.setRecalcLightTime(-1.0F);
         GameTime.instance.lightSourceUpdate = 100.0F;
         LuaEventManager.triggerEvent("OnContainerUpdate");
         WeatherFxMask.forceMaskUpdateAll();
      }
   }

   public void ToggleDoor(IsoGameCharacter var1) {
      this.ToggleDoorActual(var1);
   }

   public void ToggleDoorSilent() {
      if (!this.isBarricaded()) {
         this.square.InvalidateSpecialObjectPaths();

         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            LosUtil.cachecleared[var1] = true;
         }

         IsoGridSquare.setRecalcLightTime(-1.0F);
         this.open = !this.open;
         this.sprite = this.closedSprite;
         if (this.open) {
            this.sprite = this.openSprite;
         }

      }
   }

   void Damage(int var1) {
      this.DirtySlice();
      this.Health -= var1;
   }

   public IsoBarricade getBarricadeOnSameSquare() {
      return IsoBarricade.GetBarricadeOnSquare(this.square, this.north ? IsoDirections.N : IsoDirections.W);
   }

   public IsoBarricade getBarricadeOnOppositeSquare() {
      return IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.north ? IsoDirections.S : IsoDirections.E);
   }

   public boolean isBarricaded() {
      IsoBarricade var1 = this.getBarricadeOnSameSquare();
      if (var1 == null) {
         var1 = this.getBarricadeOnOppositeSquare();
      }

      return var1 != null;
   }

   public boolean isBarricadeAllowed() {
      return this.getSprite() != null && !this.getSprite().getProperties().Is("DoubleDoor") && !this.getSprite().getProperties().Is("GarageDoor");
   }

   public IsoBarricade getBarricadeForCharacter(IsoGameCharacter var1) {
      return IsoBarricade.GetBarricadeForCharacter(this, var1);
   }

   public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter var1) {
      return IsoBarricade.GetBarricadeOppositeCharacter(this, var1);
   }

   public boolean isLocked() {
      return this.Locked;
   }

   public void setLocked(boolean var1) {
      this.Locked = var1;
   }

   public boolean getNorth() {
      return this.north;
   }

   public Vector2 getFacingPosition(Vector2 var1) {
      if (this.square == null) {
         return var1.set(0.0F, 0.0F);
      } else {
         return this.north ? var1.set(this.getX() + 0.5F, this.getY()) : var1.set(this.getX(), this.getY() + 0.5F);
      }
   }

   public Vector2 getFacingPositionAlt(Vector2 var1) {
      if (this.square == null) {
         return var1.set(0.0F, 0.0F);
      } else {
         switch (this.getSpriteEdge(false)) {
            case N:
               return var1.set(this.getX() + 0.5F, this.getY());
            case S:
               return var1.set(this.getX() + 0.5F, this.getY() + 1.0F);
            case W:
               return var1.set(this.getX(), this.getY() + 0.5F);
            case E:
               return var1.set(this.getX() + 1.0F, this.getY() + 0.5F);
            default:
               throw new IllegalStateException();
         }
      }
   }

   public void setIsLocked(boolean var1) {
      this.Locked = var1;
   }

   public IsoSprite getOpenSprite() {
      return this.openSprite;
   }

   public void setOpenSprite(IsoSprite var1) {
      this.openSprite = var1;
   }

   public int getKeyId() {
      return this.keyId;
   }

   public void syncDoorKey() {
      ByteBufferWriter var1 = GameClient.connection.startPacket();
      PacketTypes.PacketType.SyncDoorKey.doPacket(var1);
      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      byte var2 = (byte)this.square.getObjects().indexOf(this);
      if (var2 == -1) {
         PrintStream var10000 = System.out;
         int var10001 = this.square.getX();
         var10000.println("ERROR: Door not found on square " + var10001 + ", " + this.square.getY() + ", " + this.square.getZ());
         GameClient.connection.cancelPacket();
      } else {
         var1.putByte(var2);
         var1.putInt(this.getKeyId());
         PacketTypes.PacketType.SyncDoorKey.send(GameClient.connection);
      }
   }

   public void syncDoorGarage(ArrayList<IsoObject> var1) {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.SyncDoorGarage, var1, this.open);
      } else if (GameServer.bServer) {
         INetworkPacket.sendToAll(PacketTypes.PacketType.SyncDoorGarage, (UdpConnection)null, var1, this.open);
      }

   }

   public void setKeyId(int var1) {
      if (this.keyId != var1 && GameClient.bClient) {
         this.keyId = var1;
         this.syncDoorKey();
      } else {
         this.keyId = var1;
      }

   }

   public boolean isLockedByKey() {
      return this.lockedByKey;
   }

   public void setLockedByKey(boolean var1) {
      boolean var2 = var1 != this.lockedByKey;
      this.lockedByKey = var1;
      this.Locked = var1;
      if (var2) {
         if (var1) {
            this.syncIsoObject(false, (byte)3, (UdpConnection)null, (ByteBuffer)null);
         } else {
            this.syncIsoObject(false, (byte)4, (UdpConnection)null, (ByteBuffer)null);
         }
      }

   }

   public boolean haveKey() {
      return this.haveKey;
   }

   public void setHaveKey(boolean var1) {
      this.haveKey = var1;
      if (!GameServer.bServer) {
         if (var1) {
            this.syncIsoObject(false, (byte)-1, (UdpConnection)null, (ByteBuffer)null);
         } else {
            this.syncIsoObject(false, (byte)-2, (UdpConnection)null, (ByteBuffer)null);
         }

      }
   }

   public IsoGridSquare getOppositeSquare() {
      return this.getNorth() ? this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ()) : this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
   }

   public boolean isAdjacentToSquare(IsoGridSquare var1) {
      IsoGridSquare var2 = this.getSquare();
      if (var2 != null && var1 != null) {
         boolean var3 = !this.IsOpen();
         IsoGridSquare var4 = var2.getAdjacentSquare(IsoDirections.NW);
         IsoGridSquare var5 = var2.getAdjacentSquare(IsoDirections.N);
         IsoGridSquare var6 = var2.getAdjacentSquare(IsoDirections.NE);
         IsoGridSquare var7 = var2.getAdjacentSquare(IsoDirections.W);
         IsoGridSquare var8 = var2.getAdjacentSquare(IsoDirections.E);
         IsoGridSquare var9 = var2.getAdjacentSquare(IsoDirections.SW);
         IsoGridSquare var10 = var2.getAdjacentSquare(IsoDirections.S);
         IsoGridSquare var11 = var2.getAdjacentSquare(IsoDirections.SE);
         switch (this.getSpriteEdge(false)) {
            case N:
               if (var1 == var4) {
                  if (!var4.isWallTo(var5) && !var4.isWindowTo(var5) && !var4.hasDoorOnEdge(IsoDirections.E, false) && !var5.hasDoorOnEdge(IsoDirections.W, false)) {
                     if (var5.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                     }

                     if (this.IsOpen() && var2.hasClosedDoorOnEdge(IsoDirections.N)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var5) {
                  if (var5.hasDoorOnEdge(IsoDirections.S, false)) {
                     return false;
                  }

                  if (this.IsOpen() && var2.hasClosedDoorOnEdge(IsoDirections.N)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var6) {
                  if (!var6.isWallTo(var5) && !var6.isWindowTo(var5) && !var6.hasDoorOnEdge(IsoDirections.W, false) && !var5.hasDoorOnEdge(IsoDirections.E, false)) {
                     if (var5.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                     }

                     if (this.IsOpen() && var2.hasClosedDoorOnEdge(IsoDirections.N)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var7) {
                  if (!var7.isWallTo(var2) && !var7.isWindowTo(var2) && !var7.hasDoorOnEdge(IsoDirections.E, false) && !var2.hasDoorOnEdge(IsoDirections.W, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.N)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var2) {
                  if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.N)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var8) {
                  if (!var8.isWallTo(var2) && !var8.isWindowTo(var2) && !var8.hasDoorOnEdge(IsoDirections.W, false) && !var2.hasDoorOnEdge(IsoDirections.E, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.N)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }
               break;
            case S:
               if (var1 == var7) {
                  if (!var7.isWallTo(var2) && !var7.isWindowTo(var2) && !var7.hasDoorOnEdge(IsoDirections.E, false) && !var2.hasDoorOnEdge(IsoDirections.W, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.S)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var2) {
                  if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.S)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var8) {
                  if (!var8.isWallTo(var2) && !var8.isWindowTo(var2) && !var8.hasDoorOnEdge(IsoDirections.W, false) && !var2.hasDoorOnEdge(IsoDirections.E, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.S)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var9) {
                  if (!var9.isWallTo(var10) && !var9.isWindowTo(var10) && !var9.hasDoorOnEdge(IsoDirections.E, false) && !var10.hasDoorOnEdge(IsoDirections.W, false)) {
                     if (var10.hasDoorOnEdge(IsoDirections.N, false)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var10) {
                  if (var10.hasDoorOnEdge(IsoDirections.N, false)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var11) {
                  if (!var11.isWallTo(var10) && !var11.isWindowTo(var10) && !var11.hasDoorOnEdge(IsoDirections.W, false) && !var10.hasDoorOnEdge(IsoDirections.E, false)) {
                     if (var10.hasDoorOnEdge(IsoDirections.N, false)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }
               break;
            case W:
               if (var1 == var4) {
                  if (!var4.isWallTo(var7) && !var4.isWindowTo(var7) && !var4.hasDoorOnEdge(IsoDirections.S, false) && !var7.hasDoorOnEdge(IsoDirections.N, false)) {
                     if (var3 && var7.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                     }

                     if (this.IsOpen() && var2.hasClosedDoorOnEdge(IsoDirections.W)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var7) {
                  if (var3 && var7.hasDoorOnEdge(IsoDirections.E, false)) {
                     return false;
                  }

                  if (this.IsOpen() && var2.hasClosedDoorOnEdge(IsoDirections.W)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var9) {
                  if (!var9.isWallTo(var7) && !var9.isWindowTo(var7) && !var9.hasDoorOnEdge(IsoDirections.N, false) && !var7.hasDoorOnEdge(IsoDirections.S, false)) {
                     if (var3 && var7.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                     }

                     if (this.IsOpen() && var2.hasClosedDoorOnEdge(IsoDirections.W)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var5) {
                  if (!var5.isWallTo(var2) && !var5.isWindowTo(var2) && !var5.hasDoorOnEdge(IsoDirections.S, false) && !var2.hasDoorOnEdge(IsoDirections.N, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.W)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var2) {
                  if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.W)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var10) {
                  if (!var10.isWallTo(var2) && !var10.isWindowTo(var2) && !var10.hasDoorOnEdge(IsoDirections.N, false) && !var2.hasDoorOnEdge(IsoDirections.S, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.W)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }
               break;
            case E:
               if (var1 == var5) {
                  if (!var5.isWallTo(var2) && !var5.isWindowTo(var2) && !var5.hasDoorOnEdge(IsoDirections.S, false) && !var2.hasDoorOnEdge(IsoDirections.N, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.E)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var2) {
                  if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.E)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var10) {
                  if (!var10.isWallTo(var2) && !var10.isWindowTo(var2) && !var10.hasDoorOnEdge(IsoDirections.N, false) && !var2.hasDoorOnEdge(IsoDirections.S, false)) {
                     if (var3 && var2.hasOpenDoorOnEdge(IsoDirections.E)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var6) {
                  if (!var6.isWallTo(var8) && !var6.isWindowTo(var8) && !var6.hasDoorOnEdge(IsoDirections.S, false) && !var7.hasDoorOnEdge(IsoDirections.N, false)) {
                     if (var8.hasDoorOnEdge(IsoDirections.W, false)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }

               if (var1 == var8) {
                  if (var8.hasDoorOnEdge(IsoDirections.W, false)) {
                     return false;
                  }

                  return true;
               }

               if (var1 == var11) {
                  if (!var11.isWallTo(var8) && !var11.isWindowTo(var8) && !var11.hasDoorOnEdge(IsoDirections.N, false) && !var8.hasDoorOnEdge(IsoDirections.S, false)) {
                     if (var8.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                     }

                     return true;
                  }

                  return false;
               }
               break;
            default:
               return false;
         }

         return false;
      } else {
         return false;
      }
   }

   public int checkKeyId() {
      if (this.getKeyId() != -1) {
         return this.getKeyId();
      } else {
         IsoGridSquare var1 = this.getSquare();
         IsoGridSquare var2 = this.getOppositeSquare();
         if (var1 != null && var2 != null) {
            BuildingDef var3 = var1.getBuilding() == null ? null : var1.getBuilding().getDef();
            BuildingDef var4 = var2.getBuilding() == null ? null : var2.getBuilding().getDef();
            if (var3 == null && var4 != null) {
               this.setKeyId(var4.getKeyId());
            } else if (var3 != null && var4 == null) {
               this.setKeyId(var3.getKeyId());
            } else if (this.getProperties().Is("forceLocked") && var3 != null) {
               this.setKeyId(var3.getKeyId());
            }

            if (this.Locked && !this.lockedByKey && this.getKeyId() != -1) {
               this.lockedByKey = true;
            }

            return this.getKeyId();
         } else {
            return -1;
         }
      }
   }

   public void setHealth(int var1) {
      this.Health = var1;
   }

   private void initCurtainSprites() {
      if (this.curtainN == null) {
         this.curtainW = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_16").newInstance();
         this.curtainW.setScale(0.8F, 0.8F);
         this.curtainWopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_20").newInstance();
         this.curtainWopen.setScale(0.8F, 0.8F);
         this.curtainE = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_17").newInstance();
         this.curtainE.setScale(0.8F, 0.8F);
         this.curtainEopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_21").newInstance();
         this.curtainEopen.setScale(0.8F, 0.8F);
         this.curtainN = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_18").newInstance();
         this.curtainN.setScale(0.8F, 0.8F);
         this.curtainNopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_22").newInstance();
         this.curtainNopen.setScale(0.8F, 0.8F);
         this.curtainS = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_19").newInstance();
         this.curtainS.setScale(0.8F, 0.8F);
         this.curtainSopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_23").newInstance();
         this.curtainSopen.setScale(0.8F, 0.8F);
      }
   }

   public IsoDoor HasCurtains() {
      return this.bHasCurtain ? this : null;
   }

   public boolean isCurtainOpen() {
      return this.bHasCurtain && this.bCurtainOpen;
   }

   public void setCurtainOpen(boolean var1) {
      if (this.bHasCurtain) {
         this.bCurtainOpen = var1;
         if (!GameServer.bServer) {
            for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
               LosUtil.cachecleared[var2] = true;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            IsoGridSquare.setRecalcLightTime(-1.0F);
            if (this.square != null) {
               this.square.RecalcProperties();
               this.square.RecalcAllWithNeighbours(true);
            }
         }

      }
   }

   public void transmitSetCurtainOpen(boolean var1) {
      if (this.bHasCurtain) {
         if (GameServer.bServer) {
            this.sendObjectChange("setCurtainOpen", new Object[]{"open", var1});
         }

         if (GameClient.bClient) {
            GameClient.instance.sendClientCommandV((IsoPlayer)null, "object", "openCloseCurtain", "x", this.getX(), "y", this.getY(), "z", this.getZ(), "index", this.getObjectIndex(), "open", !this.bCurtainOpen);
         }

      }
   }

   public void toggleCurtain() {
      if (this.bHasCurtain) {
         if (GameClient.bClient) {
            this.transmitSetCurtainOpen(!this.isCurtainOpen());
         } else {
            this.setCurtainOpen(!this.isCurtainOpen());
            if (GameServer.bServer) {
               this.transmitSetCurtainOpen(this.isCurtainOpen());
            }
         }

      }
   }

   public void addSheet(IsoGameCharacter var1) {
      if (!this.bHasCurtain && var1 != null && var1.getCurrentSquare() != null) {
         IsoGridSquare var2 = var1.getCurrentSquare();
         IsoGridSquare var3 = this.getSquare();
         boolean var4;
         switch (this.getSpriteEdge(false)) {
            case N:
               var4 = this.north == var2.getY() >= var3.getY();
               break;
            case S:
               var4 = var2.getY() > var3.getY();
               break;
            case W:
               var4 = this.north == var2.getX() < var3.getX();
               break;
            case E:
               var4 = var2.getX() > var3.getX();
               break;
            default:
               throw new IllegalStateException();
         }

         this.addSheet(var4, var1);
      }
   }

   public void addSheet(boolean var1, IsoGameCharacter var2) {
      if (!this.bHasCurtain) {
         this.bHasCurtain = true;
         this.bCurtainInside = var1;
         this.bCurtainOpen = true;
         if (!GameClient.bClient) {
            InventoryItem var3 = var2.getInventory().FindAndReturn("Sheet");
            var2.getInventory().Remove(var3);
            if (GameServer.bServer) {
               GameServer.sendRemoveItemFromContainer(var2.getInventory(), var3);
            }
         }

         if (GameServer.bServer) {
            this.sendObjectChange("addSheet", new Object[]{"inside", var1});
         } else if (var2 != null) {
            for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
               LosUtil.cachecleared[var4] = true;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            IsoGridSquare.setRecalcLightTime(-1.0F);
            if (this.square != null) {
               this.square.RecalcProperties();
            }
         }

      }
   }

   public void removeSheet(IsoGameCharacter var1) {
      if (this.bHasCurtain) {
         this.bHasCurtain = false;
         InventoryItem var2 = var1.getInventory().AddItem("Base.Sheet");
         if (GameServer.bServer) {
            GameServer.sendAddItemToContainer(var1.getInventory(), var2);
         }

         if (GameServer.bServer) {
            this.sendObjectChange("removeSheet");
         } else if (var1 != null) {
            for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
               LosUtil.cachecleared[var3] = true;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            IsoGridSquare.setRecalcLightTime(-1.0F);
            if (this.square != null) {
               this.square.RecalcProperties();
            }
         }

      }
   }

   public IsoGridSquare getAddSheetSquare(IsoGameCharacter var1) {
      if (var1 != null && var1.getCurrentSquare() != null) {
         IsoGridSquare var2 = var1.getCurrentSquare();
         IsoGridSquare var3 = this.getSquare();
         switch (this.getSpriteEdge(false)) {
            case N:
               return var2.getY() >= var3.getY() ? var3 : this.getCell().getGridSquare(var3.x, var3.y - 1, var3.z);
            case S:
               return var2.getY() <= var3.getY() ? var3 : this.getCell().getGridSquare(var3.x, var3.y + 1, var3.z);
            case W:
               return var2.getX() >= var3.getX() ? var3 : this.getCell().getGridSquare(var3.x - 1, var3.y, var3.z);
            case E:
               return var2.getX() <= var3.getX() ? var3 : this.getCell().getGridSquare(var3.x + 1, var3.y, var3.z);
            default:
               throw new IllegalStateException();
         }
      } else {
         return null;
      }
   }

   public IsoGridSquare getSheetSquare() {
      if (!this.bHasCurtain) {
         return null;
      } else {
         switch (this.getSpriteEdge(false)) {
            case N:
               if (this.open) {
                  return this.bCurtainInside ? this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ()) : this.getSquare();
               }

               return this.bCurtainInside ? this.getSquare() : this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ());
            case S:
               return this.bCurtainInside ? this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ()) : this.getSquare();
            case W:
               if (this.open) {
                  return this.bCurtainInside ? this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ()) : this.getSquare();
               }

               return this.bCurtainInside ? this.getSquare() : this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
            case E:
               return this.bCurtainInside ? this.getCell().getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ()) : this.getSquare();
            default:
               throw new IllegalStateException();
         }
      }
   }

   public int getHealth() {
      return this.Health;
   }

   public int getMaxHealth() {
      return this.MaxHealth;
   }

   public boolean isFacingSheet(IsoGameCharacter var1) {
      if (this.bHasCurtain && var1 != null && var1.getCurrentSquare() == this.getSheetSquare()) {
         IsoDirections var2;
         if (this.bCurtainInside) {
            if (this.open) {
               if (this.north) {
                  var2 = IsoDirections.E;
               } else {
                  var2 = IsoDirections.S;
               }
            } else if (this.north) {
               var2 = IsoDirections.N;
            } else {
               var2 = IsoDirections.W;
            }
         } else if (this.open) {
            if (this.north) {
               var2 = IsoDirections.W;
            } else {
               var2 = IsoDirections.N;
            }
         } else if (this.north) {
            var2 = IsoDirections.S;
         } else {
            var2 = IsoDirections.E;
         }

         IsoDirections var3 = this.getSpriteEdge(false);
         if (var3 == IsoDirections.E) {
            var2 = this.bCurtainInside ? IsoDirections.W : IsoDirections.E;
         }

         if (var3 == IsoDirections.S) {
            var2 = this.bCurtainInside ? IsoDirections.N : IsoDirections.S;
         }

         return var1.getDir() == var2 || var1.getDir() == IsoDirections.RotLeft(var2) || var1.getDir() == IsoDirections.RotRight(var2);
      } else {
         return false;
      }
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      if ("addSheet".equals(var1)) {
         if (var2 != null && var2.rawget("inside") instanceof Boolean) {
            var3.put((byte)((Boolean)var2.rawget("inside") ? 1 : 0));
         }
      } else if (!"removeSheet".equals(var1)) {
         if ("setCurtainOpen".equals(var1)) {
            if (var2 != null && var2.rawget("open") instanceof Boolean) {
               var3.put((byte)((Boolean)var2.rawget("open") ? 1 : 0));
            }
         } else {
            super.saveChange(var1, var2, var3);
         }
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      if ("addSheet".equals(var1)) {
         this.addSheet(var2.get() == 1, (IsoGameCharacter)null);
      } else if ("removeSheet".equals(var1)) {
         this.removeSheet((IsoGameCharacter)null);
      } else if ("setCurtainOpen".equals(var1)) {
         this.setCurtainOpen(var2.get() == 1);
      } else {
         super.loadChange(var1, var2);
      }

   }

   public void addRandomBarricades() {
      IsoGridSquare var1 = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
      if (var1 != null && var1.getRoom() == null) {
         boolean var2 = var1 != this.square;
         IsoBarricade var3 = IsoBarricade.AddBarricadeToObject(this, var2);
         if (var3 != null) {
            int var4 = Rand.Next(1, 4);

            for(int var5 = 0; var5 < var4; ++var5) {
               var3.addPlank((IsoGameCharacter)null, (InventoryItem)null);
            }
         }
      }

   }

   public boolean isObstructed() {
      return isDoorObstructed(this);
   }

   public static boolean isDoorObstructed(IsoObject var0) {
      IsoDoor var1 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
      IsoThumpable var2 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
      if (var1 == null && var2 == null) {
         return false;
      } else {
         IsoGridSquare var3 = var0.getSquare();
         if (var3 == null) {
            return false;
         } else if (!var3.isSolid() && !var3.isSolidTrans() && !var3.Has(IsoObjectType.tree)) {
            int var4 = (var3.x - 1) / 8;
            int var5 = (var3.y - 1) / 8;
            int var6 = (int)Math.ceil((double)(((float)var3.x + 1.0F) / 8.0F));
            int var7 = (int)Math.ceil((double)(((float)var3.y + 1.0F) / 8.0F));

            for(int var8 = var5; var8 <= var7; ++var8) {
               for(int var9 = var4; var9 <= var6; ++var9) {
                  IsoChunk var10 = GameServer.bServer ? ServerMap.instance.getChunk(var9, var8) : IsoWorld.instance.CurrentCell.getChunk(var9, var8);
                  if (var10 != null) {
                     for(int var11 = 0; var11 < var10.vehicles.size(); ++var11) {
                        BaseVehicle var12 = (BaseVehicle)var10.vehicles.get(var11);
                        if (var12.isIntersectingSquareWithShadow(var3.x, var3.y, var3.z)) {
                           return true;
                        }
                     }
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      }
   }

   public static void toggleDoubleDoor(IsoObject var0, boolean var1) {
      int var2 = getDoubleDoorIndex(var0);
      if (var2 != -1) {
         IsoDoor var3 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var4 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var10000;
         if (var3 == null) {
            var10000 = var4.north;
         } else {
            var10000 = var3.north;
         }

         boolean var6 = var3 == null ? var4.open : var3.open;
         IsoObject var7 = getDoubleDoorObject(var0, 1);
         IsoObject var8 = getDoubleDoorObject(var0, 2);
         IsoObject var9 = getDoubleDoorObject(var0, 3);
         IsoObject var10 = getDoubleDoorObject(var0, 4);
         if (var7 != null) {
            toggleDoubleDoorObject(var7);
         }

         if (var8 != null) {
            toggleDoubleDoorObject(var8);
         }

         if (var9 != null) {
            toggleDoubleDoorObject(var9);
         }

         if (var10 != null) {
            toggleDoubleDoorObject(var10);
         }

         LuaEventManager.triggerEvent("OnContainerUpdate");
         if (var1) {
            if (var7 != null) {
               var7.syncIsoObject(false, (byte)(var6 ? 1 : 0), (UdpConnection)null, (ByteBuffer)null);
            } else if (var10 != null) {
               var10.syncIsoObject(false, (byte)(var6 ? 1 : 0), (UdpConnection)null, (ByteBuffer)null);
            }
         }

         SpriteModel var11;
         if (var7 != null) {
            var11 = var7.getSpriteModel();
            if (var11 != null && var11.animationName != null) {
               IsoObjectAnimations.getInstance().addObject(var7, var11, !var6 ? "Open" : "Close");
               var7.setAnimating(true);
            }
         }

         if (var10 != null) {
            var11 = var10.getSpriteModel();
            if (var11 != null && var11.animationName != null) {
               IsoObjectAnimations.getInstance().addObject(var10, var11, !var6 ? "Open" : "Close");
               var10.setAnimating(true);
            }
         }

      }
   }

   private static void toggleDoubleDoorObject(IsoObject var0) {
      int var1 = getDoubleDoorIndex(var0);
      if (var1 != -1) {
         IsoDoor var2 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var3 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var4 = var2 == null ? var3.north : var2.north;
         boolean var5 = var2 == null ? var3.open : var2.open;
         int var6 = -1;
         if (var2 != null) {
            var2.open = !var5;
            var2.setLockedByKey(false);
            var6 = var2.checkKeyId();
         }

         if (var3 != null) {
            var3.open = !var5;
            var3.setLockedByKey(false);
            var6 = var3.getKeyId();
         }

         IsoSprite var7 = var0.getSprite();
         int var8 = var4 ? DoubleDoorNorthSpriteOffset[var1 - 1] : DoubleDoorWestSpriteOffset[var1 - 1];
         if (var5) {
            var8 *= -1;
         }

         var0.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, var7.getName(), var8);
         var0.getSquare().RecalcAllWithNeighbours(true);
         if (var1 != 2 && var1 != 3) {
            PolygonalMap2.instance.squareChanged(var0.getSquare());
            var0.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
         } else {
            IsoGridSquare var9 = var0.getSquare();
            int[] var10;
            int[] var11;
            int[] var12;
            int[] var13;
            if (var4) {
               if (var5) {
                  var10 = DoubleDoorNorthOpenXOffset;
                  var11 = DoubleDoorNorthOpenYOffset;
                  var12 = DoubleDoorNorthClosedXOffset;
                  var13 = DoubleDoorNorthClosedYOffset;
               } else {
                  var10 = DoubleDoorNorthClosedXOffset;
                  var11 = DoubleDoorNorthClosedYOffset;
                  var12 = DoubleDoorNorthOpenXOffset;
                  var13 = DoubleDoorNorthOpenYOffset;
               }
            } else if (var5) {
               var10 = DoubleDoorWestOpenXOffset;
               var11 = DoubleDoorWestOpenYOffset;
               var12 = DoubleDoorWestClosedXOffset;
               var13 = DoubleDoorWestClosedYOffset;
            } else {
               var10 = DoubleDoorWestClosedXOffset;
               var11 = DoubleDoorWestClosedYOffset;
               var12 = DoubleDoorWestOpenXOffset;
               var13 = DoubleDoorWestOpenYOffset;
            }

            int var14 = var9.getX() - var10[var1 - 1];
            int var15 = var9.getY() - var11[var1 - 1];
            int var16 = var14 + var12[var1 - 1];
            int var17 = var15 + var13[var1 - 1];
            var9.RemoveTileObject(var0);
            PolygonalMap2.instance.squareChanged(var9);
            var9 = IsoWorld.instance.CurrentCell.getGridSquare(var16, var17, var9.getZ());
            if (var9 == null) {
               return;
            }

            if (var3 != null) {
               IsoThumpable var18 = new IsoThumpable(var9.getCell(), var9, var0.getSprite().getName(), var4, var3.getTable());
               var18.setModData(var3.getModData());
               var18.setCanBeLockByPadlock(var3.canBeLockByPadlock());
               var18.setCanBePlastered(var3.canBePlastered());
               var18.setIsHoppable(var3.isHoppable());
               var18.setIsDismantable(var3.isDismantable());
               var18.setName(var3.getName());
               var18.setIsDoor(true);
               var18.setIsThumpable(var3.isThumpable());
               var18.setThumpDmg(var3.getThumpDmg());
               var18.setThumpSound(var3.getThumpSound());
               var18.open = !var5;
               var18.keyId = var6;
               var9.AddSpecialObject(var18);
            } else {
               IsoDoor var19 = new IsoDoor(var9.getCell(), var9, var0.getSprite().getName(), var4);
               var19.open = !var5;
               var19.keyId = var6;
               var9.getObjects().add(var19);
               var9.getSpecialObjects().add(var19);
               var9.RecalcProperties();
               var9.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_ADD);
            }

            if (!GameClient.bClient) {
               var9.restackSheetRope();
            }

            PolygonalMap2.instance.squareChanged(var9);
         }

      }
   }

   public static int getDoubleDoorIndex(IsoObject var0) {
      if (var0 != null && var0.getSquare() != null) {
         PropertyContainer var1 = var0.getProperties();
         if (var1 != null && var1.Is("DoubleDoor")) {
            int var2 = Integer.parseInt(var1.Val("DoubleDoor"));
            if (var2 >= 1 && var2 <= 8) {
               IsoDoor var3 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
               IsoThumpable var4 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
               if (var3 == null && var4 == null) {
                  return -1;
               } else {
                  boolean var5 = var3 == null ? var4.open : var3.open;
                  if (var5) {
                     return var2 >= 5 ? var2 - 4 : -1;
                  } else {
                     return var2;
                  }
               }
            } else {
               return -1;
            }
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public static IsoObject getDoubleDoorObject(IsoObject var0, int var1) {
      int var2 = getDoubleDoorIndex(var0);
      if (var2 != -1 && var1 > 0) {
         IsoDoor var3 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var4 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var5 = var3 == null ? var4.north : var3.north;
         boolean var6 = var3 == null ? var4.open : var3.open;
         IsoGridSquare var7 = var0.getSquare();
         int[] var8;
         int[] var9;
         if (var5) {
            if (var6) {
               var8 = DoubleDoorNorthOpenXOffset;
               var9 = DoubleDoorNorthOpenYOffset;
            } else {
               var8 = DoubleDoorNorthClosedXOffset;
               var9 = DoubleDoorNorthClosedYOffset;
            }
         } else if (var6) {
            var8 = DoubleDoorWestOpenXOffset;
            var9 = DoubleDoorWestOpenYOffset;
         } else {
            var8 = DoubleDoorWestClosedXOffset;
            var9 = DoubleDoorWestClosedYOffset;
         }

         int var10 = var7.getX() - var8[var2 - 1];
         int var11 = var7.getY() - var9[var2 - 1];
         int var12 = var10 + var8[var1 - 1];
         int var13 = var11 + var9[var1 - 1];
         var7 = IsoWorld.instance.CurrentCell.getGridSquare(var12, var13, var7.getZ());
         if (var7 == null) {
            return null;
         } else {
            ArrayList var14 = var7.getSpecialObjects();
            int var15;
            IsoObject var16;
            if (var3 != null) {
               for(var15 = 0; var15 < var14.size(); ++var15) {
                  var16 = (IsoObject)var14.get(var15);
                  if (var16 instanceof IsoDoor && ((IsoDoor)var16).north == var5 && getDoubleDoorIndex(var16) == var1) {
                     return var16;
                  }
               }
            }

            if (var4 != null) {
               for(var15 = 0; var15 < var14.size(); ++var15) {
                  var16 = (IsoObject)var14.get(var15);
                  if (var16 instanceof IsoThumpable && ((IsoThumpable)var16).north == var5 && getDoubleDoorIndex(var16) == var1) {
                     return var16;
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean isDoubleDoorObstructed(IsoObject var0) {
      int var1 = getDoubleDoorIndex(var0);
      if (var1 == -1) {
         return false;
      } else {
         IsoDoor var2 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var3 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var4 = var2 == null ? var3.north : var2.north;
         boolean var5 = var2 == null ? var3.open : var2.open;
         IsoGridSquare var6 = var0.getSquare();
         int[] var7;
         int[] var8;
         if (var4) {
            if (var5) {
               var7 = DoubleDoorNorthOpenXOffset;
               var8 = DoubleDoorNorthOpenYOffset;
            } else {
               var7 = DoubleDoorNorthClosedXOffset;
               var8 = DoubleDoorNorthClosedYOffset;
            }
         } else if (var5) {
            var7 = DoubleDoorWestOpenXOffset;
            var8 = DoubleDoorWestOpenYOffset;
         } else {
            var7 = DoubleDoorWestClosedXOffset;
            var8 = DoubleDoorWestClosedYOffset;
         }

         boolean var9 = getDoubleDoorObject(var0, 1) != null;
         boolean var10 = getDoubleDoorObject(var0, 4) != null;
         int var11 = var6.getX() - var7[var1 - 1];
         int var12 = var6.getY() - var8[var1 - 1];
         int var13 = var11;
         int var14 = var12;
         int var17 = var6.getZ();
         int var15;
         int var16;
         if (var4) {
            var15 = var11 + 4;
            var16 = var12 + 2;
            if (var9 && hasSomething4x4(var11, var12, var11 + 1, var12 + 1, var17)) {
               return true;
            }

            if (var10 && hasSomething4x4(var11 + 2, var12, var11 + 3, var12 + 1, var17)) {
               return true;
            }

            if (!var9) {
               var13 = var11 + 2;
            }

            if (!var10) {
               var15 -= 2;
            }
         } else {
            var14 = var12 - 3;
            var15 = var11 + 2;
            var16 = var14 + 4;
            if (var9 && hasSomething4x4(var11, var12 - 1, var11 + 1, var12, var17)) {
               return true;
            }

            if (var10 && hasSomething4x4(var11, var12 - 3, var11 + 1, var12 - 2, var17)) {
               return true;
            }

            if (!var9) {
               var16 -= 2;
            }

            if (!var10) {
               var14 += 2;
            }
         }

         int var18 = PZMath.fastfloor(((float)var13 - 4.0F) / 8.0F);
         int var19 = PZMath.fastfloor(((float)var14 - 4.0F) / 8.0F);
         int var20 = (int)Math.ceil((double)(((float)var15 + 4.0F) / 8.0F));
         int var21 = (int)Math.ceil((double)(((float)var16 + 4.0F) / 8.0F));

         for(int var22 = var19; var22 <= var21; ++var22) {
            for(int var23 = var18; var23 <= var20; ++var23) {
               IsoChunk var24 = GameServer.bServer ? ServerMap.instance.getChunk(var23, var22) : IsoWorld.instance.CurrentCell.getChunk(var23, var22);
               if (var24 != null) {
                  for(int var25 = 0; var25 < var24.vehicles.size(); ++var25) {
                     BaseVehicle var26 = (BaseVehicle)var24.vehicles.get(var25);

                     for(int var27 = var14; var27 < var16; ++var27) {
                        for(int var28 = var13; var28 < var15; ++var28) {
                           if (var26.isIntersectingSquare(var28, var27, var17)) {
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private static boolean hasSolidObjects(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else {
         return var0.isSolid() || var0.isSolidTrans() || var0.Has(IsoObjectType.tree);
      }
   }

   private static boolean isSomethingTo(IsoGridSquare var0, IsoGridSquare var1) {
      if (var0 != null && var1 != null) {
         if (var1 != var0.getAdjacentSquare(IsoDirections.E) || !var0.hasDoorOnEdge(IsoDirections.E, false) && !var1.hasDoorOnEdge(IsoDirections.W, false) && !var1.getProperties().Is(IsoFlagType.DoorWallW)) {
            if (var1 != var0.getAdjacentSquare(IsoDirections.SE) || !var0.hasDoorOnEdge(IsoDirections.E, false) && !var0.hasDoorOnEdge(IsoDirections.S, false) && !var1.hasDoorOnEdge(IsoDirections.W, false) && !var1.hasDoorOnEdge(IsoDirections.N, false) && !var1.getProperties().Is(IsoFlagType.DoorWallN) && !var1.getProperties().Is(IsoFlagType.DoorWallW)) {
               if (var1 == var0.getAdjacentSquare(IsoDirections.S) && (var0.hasDoorOnEdge(IsoDirections.S, false) || var1.hasDoorOnEdge(IsoDirections.N, false) || var1.getProperties().Is(IsoFlagType.DoorWallN))) {
                  return true;
               } else {
                  return var0.isWallTo(var1) || var0.isWindowTo(var1);
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean hasSomething4x4(int var0, int var1, int var2, int var3, int var4) {
      IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var0, var1, var4);
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var1, var4);
      IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var0, var3, var4);
      if (!hasSolidObjects(var5) && !hasSolidObjects(var6) && !hasSolidObjects(var7) && !hasSolidObjects(var8)) {
         if (isSomethingTo(var5, var6)) {
            return true;
         } else if (isSomethingTo(var5, var7)) {
            return true;
         } else {
            return isSomethingTo(var5, var8);
         }
      } else {
         return true;
      }
   }

   public static boolean destroyDoubleDoor(IsoObject var0) {
      int var1 = getDoubleDoorIndex(var0);
      if (var1 == -1) {
         return false;
      } else {
         if (var1 == 1 || var1 == 4) {
            IsoObject var2 = getDoubleDoorObject(var0, var1 == 1 ? 2 : 3);
            if (var2 instanceof IsoDoor) {
               ((IsoDoor)var2).destroy();
            } else if (var2 instanceof IsoThumpable) {
               ((IsoThumpable)var2).destroy();
            }
         }

         if (var0 instanceof IsoDoor) {
            ((IsoDoor)var0).destroy();
         } else if (var0 instanceof IsoThumpable) {
            ((IsoThumpable)var0).destroy();
         }

         LuaEventManager.triggerEvent("OnContainerUpdate");
         return true;
      }
   }

   public static int getGarageDoorIndex(IsoObject var0) {
      if (var0 != null && var0.getSquare() != null) {
         PropertyContainer var1 = var0.getProperties();
         if (var1 != null && var1.Is("GarageDoor")) {
            int var2 = Integer.parseInt(var1.Val("GarageDoor"));
            if (var2 >= 1 && var2 <= 6) {
               IsoDoor var3 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
               IsoThumpable var4 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
               if (var3 == null && var4 == null) {
                  return -1;
               } else {
                  boolean var5 = var3 == null ? var4.open : var3.open;
                  if (var5) {
                     return var2 >= 4 ? var2 - 3 : -1;
                  } else {
                     return var2;
                  }
               }
            } else {
               return -1;
            }
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public static IsoObject getGarageDoorPrev(IsoObject var0) {
      int var1 = getGarageDoorIndex(var0);
      if (var1 == -1) {
         return null;
      } else if (var1 == 1) {
         return null;
      } else {
         IsoDoor var2 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var3 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var4 = var2 == null ? var3.north : var2.north;
         IsoGridSquare var5 = var0.getSquare();
         int var6 = var5.x - (var4 ? 1 : 0);
         int var7 = var5.y + (var4 ? 0 : 1);
         var5 = IsoWorld.instance.CurrentCell.getGridSquare(var6, var7, var5.getZ());
         if (var5 == null) {
            return null;
         } else {
            ArrayList var8 = var5.getSpecialObjects();
            int var9;
            IsoObject var10;
            if (var2 != null) {
               for(var9 = 0; var9 < var8.size(); ++var9) {
                  var10 = (IsoObject)var8.get(var9);
                  if (var10 instanceof IsoDoor && ((IsoDoor)var10).north == var4 && getGarageDoorIndex(var10) <= var1) {
                     return var10;
                  }
               }
            }

            if (var3 != null) {
               for(var9 = 0; var9 < var8.size(); ++var9) {
                  var10 = (IsoObject)var8.get(var9);
                  if (var10 instanceof IsoThumpable && ((IsoThumpable)var10).north == var4 && getGarageDoorIndex(var10) <= var1) {
                     return var10;
                  }
               }
            }

            return null;
         }
      }
   }

   public static IsoObject getGarageDoorNext(IsoObject var0) {
      int var1 = getGarageDoorIndex(var0);
      if (var1 == -1) {
         return null;
      } else if (var1 == 3) {
         return null;
      } else {
         IsoDoor var2 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var3 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var4 = var2 == null ? var3.north : var2.north;
         IsoGridSquare var5 = var0.getSquare();
         int var6 = var5.x + (var4 ? 1 : 0);
         int var7 = var5.y - (var4 ? 0 : 1);
         var5 = IsoWorld.instance.CurrentCell.getGridSquare(var6, var7, var5.getZ());
         if (var5 == null) {
            return null;
         } else {
            ArrayList var8 = var5.getSpecialObjects();
            int var9;
            IsoObject var10;
            if (var2 != null) {
               for(var9 = 0; var9 < var8.size(); ++var9) {
                  var10 = (IsoObject)var8.get(var9);
                  if (var10 instanceof IsoDoor && ((IsoDoor)var10).north == var4 && getGarageDoorIndex(var10) >= var1) {
                     return var10;
                  }
               }
            }

            if (var3 != null) {
               for(var9 = 0; var9 < var8.size(); ++var9) {
                  var10 = (IsoObject)var8.get(var9);
                  if (var10 instanceof IsoThumpable && ((IsoThumpable)var10).north == var4 && getGarageDoorIndex(var10) >= var1) {
                     return var10;
                  }
               }
            }

            return null;
         }
      }
   }

   public static IsoObject getGarageDoorFirst(IsoObject var0) {
      int var1 = getGarageDoorIndex(var0);
      if (var1 == -1) {
         return null;
      } else if (var1 == 1) {
         return var0;
      } else {
         for(IsoObject var2 = getGarageDoorPrev(var0); var2 != null; var2 = getGarageDoorPrev(var2)) {
            if (getGarageDoorIndex(var2) == 1) {
               return var2;
            }
         }

         return var0;
      }
   }

   public void changeSprite(IsoDoor var1) {
      var1.sprite = var1.open ? var1.openSprite : var1.closedSprite;
   }

   private static void toggleGarageDoorObject(IsoObject var0) {
      int var1 = getGarageDoorIndex(var0);
      if (var1 != -1) {
         IsoDoor var2 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var3 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var4 = var2 == null ? var3.open : var2.open;
         if (var2 != null) {
            var2.open = !var4;
            var2.setLockedByKey(false);
            var2.sprite = var2.open ? var2.openSprite : var2.closedSprite;
         }

         if (var3 != null) {
            var3.open = !var4;
            var3.setLockedByKey(false);
            var3.sprite = var3.open ? var3.openSprite : var3.closedSprite;
         }

         var0.getSquare().RecalcAllWithNeighbours(true);
         PolygonalMap2.instance.squareChanged(var0.getSquare());
         var0.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
      }
   }

   public static void toggleGarageDoor(IsoObject var0, boolean var1) {
      int var2 = getGarageDoorIndex(var0);
      ArrayList var3 = new ArrayList();
      var3.add(var0);
      IsoDoor var4 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
      if (var0 instanceof IsoThumpable var10000) {
         ;
      } else {
         var10000 = null;
      }

      toggleGarageDoorObject(var0);

      for(IsoObject var6 = getGarageDoorPrev(var0); var6 != null; var6 = getGarageDoorPrev(var6)) {
         toggleGarageDoorObject(var6);
         var3.add(var6);
      }

      for(IsoObject var7 = getGarageDoorNext(var0); var7 != null; var7 = getGarageDoorNext(var7)) {
         toggleGarageDoorObject(var7);
         var3.add(var7);
      }

      if (GameClient.bClient || GameServer.bServer) {
         var4.syncDoorGarage(var3);
      }

      for(int var8 = 0; var8 < IsoPlayer.numPlayers; ++var8) {
         LosUtil.cachecleared[var8] = true;
      }

      IsoGridSquare.setRecalcLightTime(-1.0F);
      LuaEventManager.triggerEvent("OnContainerUpdate");
   }

   private static boolean isGarageDoorObstructed(IsoObject var0) {
      int var1 = getGarageDoorIndex(var0);
      if (var1 == -1) {
         return false;
      } else {
         IsoDoor var2 = var0 instanceof IsoDoor ? (IsoDoor)var0 : null;
         IsoThumpable var3 = var0 instanceof IsoThumpable ? (IsoThumpable)var0 : null;
         boolean var4 = var2 == null ? var3.north : var2.north;
         boolean var5 = var2 == null ? var3.open : var2.open;
         if (!var5) {
            return false;
         } else {
            int var6 = var0.square.x;
            int var7 = var0.square.y;
            int var8 = var6;
            int var9 = var7;
            IsoObject var10;
            IsoObject var11;
            if (var4) {
               for(var10 = getGarageDoorPrev(var0); var10 != null; var10 = getGarageDoorPrev(var10)) {
                  --var6;
               }

               for(var11 = getGarageDoorNext(var0); var11 != null; var11 = getGarageDoorNext(var11)) {
                  ++var8;
               }
            } else {
               for(var10 = getGarageDoorPrev(var0); var10 != null; var10 = getGarageDoorPrev(var10)) {
                  ++var9;
               }

               for(var11 = getGarageDoorNext(var0); var11 != null; var11 = getGarageDoorNext(var11)) {
                  --var7;
               }
            }

            int var22 = (var6 - 4) / 8;
            int var23 = (var7 - 4) / 8;
            int var12 = (int)Math.ceil((double)((var8 + 4) / 8));
            int var13 = (int)Math.ceil((double)((var9 + 4) / 8));
            int var14 = var0.square.z;

            for(int var15 = var23; var15 <= var13; ++var15) {
               for(int var16 = var22; var16 <= var12; ++var16) {
                  IsoChunk var17 = GameServer.bServer ? ServerMap.instance.getChunk(var16, var15) : IsoWorld.instance.CurrentCell.getChunk(var16, var15);
                  if (var17 != null) {
                     for(int var18 = 0; var18 < var17.vehicles.size(); ++var18) {
                        BaseVehicle var19 = (BaseVehicle)var17.vehicles.get(var18);

                        for(int var20 = var7; var20 <= var9; ++var20) {
                           for(int var21 = var6; var21 <= var8; ++var21) {
                              if (var19.isIntersectingSquare(var21, var20, var14) && var19.isIntersectingSquare(var21 - (var4 ? 0 : 1), var20 - (var4 ? 1 : 0), var14)) {
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   public static boolean destroyGarageDoor(IsoObject var0) {
      int var1 = getGarageDoorIndex(var0);
      if (var1 == -1) {
         return false;
      } else {
         IsoObject var3;
         for(IsoObject var2 = getGarageDoorPrev(var0); var2 != null; var2 = var3) {
            var3 = getGarageDoorPrev(var2);
            if (var2 instanceof IsoDoor) {
               ((IsoDoor)var2).destroy();
            } else if (var2 instanceof IsoThumpable) {
               ((IsoThumpable)var2).destroy();
            }
         }

         IsoObject var4;
         for(var3 = getGarageDoorNext(var0); var3 != null; var3 = var4) {
            var4 = getGarageDoorNext(var3);
            if (var3 instanceof IsoDoor) {
               ((IsoDoor)var3).destroy();
            } else if (var3 instanceof IsoThumpable) {
               ((IsoThumpable)var3).destroy();
            }
         }

         if (var0 instanceof IsoDoor) {
            ((IsoDoor)var0).destroy();
         } else if (var0 instanceof IsoThumpable) {
            ((IsoThumpable)var0).destroy();
         }

         LuaEventManager.triggerEvent("OnContainerUpdate");
         return true;
      }
   }

   public IsoObject getRenderEffectMaster() {
      int var1 = getDoubleDoorIndex(this);
      IsoObject var2;
      if (var1 != -1) {
         var2 = null;
         if (var1 == 2) {
            var2 = getDoubleDoorObject(this, 1);
         } else if (var1 == 3) {
            var2 = getDoubleDoorObject(this, 4);
         }

         if (var2 != null) {
            return var2;
         }
      } else {
         var2 = getGarageDoorFirst(this);
         if (var2 != null) {
            return var2;
         }
      }

      return this;
   }

   public int getRenderEffectObjectCount() {
      int var1 = getDoubleDoorIndex(this);
      if (var1 != -1) {
         return 2;
      } else {
         int var2 = getGarageDoorIndex(this);
         if (var2 == -1) {
            return 1;
         } else {
            Object var3 = this;

            int var4;
            for(var4 = 1; (var3 = getGarageDoorPrev((IsoObject)var3)) != null; ++var4) {
            }

            for(var3 = this; (var3 = getGarageDoorNext((IsoObject)var3)) != null; ++var4) {
            }

            return var4;
         }
      }
   }

   public IsoObject getRenderEffectObjectByIndex(int var1) {
      int var2 = getDoubleDoorIndex(this);
      if (var2 != -1) {
         return getDoubleDoorObject(this, var1);
      } else {
         int var3 = getGarageDoorIndex(this);
         if (var3 == -1) {
            return this;
         } else {
            IsoObject var4 = getGarageDoorFirst(this);

            for(int var5 = 0; var4 != null && var5 != var1; ++var5) {
               var4 = getGarageDoorNext(var4);
            }

            return var4;
         }
      }
   }

   public String getThumpSound() {
      switch (this.getSoundPrefix()) {
         case "MetalGate":
            return "ZombieThumpWireFence";
         case "MetalPoleGate":
         case "MetalPoleGateDouble":
            return "ZombieThumpMetalPoleGate";
         case "GarageDoor":
            return "ZombieThumpGarageDoor";
         case "MetalDoor":
         case "PrisonMetalDoor":
            return "ZombieThumpMetal";
         case "SlidingGlassDoor":
            return "ZombieThumpWindow";
         default:
            return "ZombieThumpGeneric";
      }
   }

   private String getSoundPrefix() {
      if (this.closedSprite == null) {
         return "WoodDoor";
      } else {
         PropertyContainer var1 = this.closedSprite.getProperties();
         return var1.Is("DoorSound") ? var1.Val("DoorSound") : "WoodDoor";
      }
   }

   private void playDoorSound(BaseCharacterSoundEmitter var1, String var2) {
      var1.playSound(this.getSoundPrefix() + var2, this);
   }

   public SpriteModel getSpriteModel() {
      if (this.bHasCurtain) {
         return null;
      } else {
         int var1 = getDoubleDoorIndex(this);
         return (var1 == 1 || var1 == 4) && getDoubleDoorObject(this, var1 == 1 ? 2 : 3) == null ? null : super.getSpriteModel();
      }
   }

   public static enum DoorType {
      WeakWooden,
      StrongWooden;

      private DoorType() {
      }
   }
}
