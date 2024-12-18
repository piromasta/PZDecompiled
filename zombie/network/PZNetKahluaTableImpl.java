package zombie.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import se.krka.kahlua.vm.LuaCallFrame;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.MultiStageBuilding;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.fields.AnimalID;
import zombie.network.fields.ContainerID;
import zombie.network.fields.NetObject;
import zombie.network.fields.PlayerID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Recipe;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

public class PZNetKahluaTableImpl implements KahluaTable {
   public final Map<Object, Object> delegate;
   private KahluaTable metatable;
   private KahluaTable reloadReplace;
   private static final byte PZNKTI_NO_SAVE = -1;
   private static final byte PZNKTI_INTEGER = 0;
   private static final byte PZNKTI_STRING = 1;
   private static final byte PZNKTI_DOUBLE = 2;
   private static final byte PZNKTI_TABLE = 3;
   private static final byte PZNKTI_PZTABLE = 4;
   private static final byte PZNKTI_BOOLEAN = 5;
   private static final byte PZNKTI_InventoryItem = 6;
   private static final byte PZNKTI_IsoObject = 7;
   private static final byte PZNKTI_IsoPlayer = 8;
   private static final byte PZNKTI_Container = 9;
   private static final byte PZNKTI_BodyPart = 10;
   private static final byte PZNKTI_VehiclePart = 11;
   private static final byte PZNKTI_BaseVehicle = 12;
   private static final byte PZNKTI_IsoGridSquare = 13;
   private static final byte PZNKTI_Recipe = 14;
   private static final byte PZNKTI_BloodBodyPartType = 15;
   private static final byte PZNKTI_DeadBody = 16;
   private static final byte PZNKTI_IsoAnimal = 17;
   private static final byte PZNKTI_ObjectInfo = 18;
   private static final byte PZNKTI_Resource = 19;
   private static final byte PZNKTI_NestBox = 20;
   private static final byte PZNKTI_MultiStageBuildingStage = 21;
   private static final byte PZNKTI_EvolvedRecipe = 22;
   private static final byte PZNKTI_FluidContainer = 23;
   private static final byte PZNKTI_VehicleWindow = 24;
   private static final byte PZNKTI_CraftRecipe = 25;
   private static final byte PZNKTI_ArrayList = 26;
   private static final byte PZNKTI_Null = 27;

   public PZNetKahluaTableImpl(Map<Object, Object> var1) {
      this.delegate = var1;
   }

   public void setMetatable(KahluaTable var1) {
      this.metatable = var1;
   }

   public KahluaTable getMetatable() {
      return this.metatable;
   }

   public int size() {
      return this.delegate.size();
   }

   public void rawset(Object var1, Object var2) {
      if (this.reloadReplace != null) {
         this.reloadReplace.rawset(var1, var2);
      }

      Object var3 = null;
      if (Core.bDebug && LuaManager.thread != null && LuaManager.thread.hasDataBreakpoint(this, var1)) {
         var3 = this.rawget(var1);
      }

      if (var2 == null) {
         if (Core.bDebug && LuaManager.thread != null && LuaManager.thread.hasDataBreakpoint(this, var1) && var3 != null) {
            UIManager.debugBreakpoint(LuaManager.thread.currentfile, (long)LuaManager.thread.lastLine);
         }

         this.delegate.remove(var1);
      } else {
         if (Core.bDebug && LuaManager.thread != null && LuaManager.thread.hasDataBreakpoint(this, var1) && !var2.equals(var3)) {
            int var4 = LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().pc;
            if (var4 < 0) {
               var4 = 0;
            }

            UIManager.debugBreakpoint(LuaManager.thread.currentfile, (long)(LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().closure.prototype.lines[var4] - 1));
         }

         this.delegate.put(var1, var2);
      }
   }

   public Object rawget(Object var1) {
      if (this.reloadReplace != null) {
         return this.reloadReplace.rawget(var1);
      } else if (var1 == null) {
         return null;
      } else {
         if (Core.bDebug && LuaManager.thread != null && LuaManager.thread.hasReadDataBreakpoint(this, var1)) {
            int var2 = LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().pc;
            if (var2 < 0) {
               var2 = 0;
            }

            UIManager.debugBreakpoint(LuaManager.thread.currentfile, (long)(LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().closure.prototype.lines[var2] - 1));
         }

         return !this.delegate.containsKey(var1) && this.metatable != null ? this.metatable.rawget(var1) : this.delegate.get(var1);
      }
   }

   public void rawset(int var1, Object var2) {
      this.rawset(KahluaUtil.toDouble((long)var1), var2);
   }

   public String rawgetStr(Object var1) {
      return (String)this.rawget(var1);
   }

   public int rawgetInt(Object var1) {
      return this.rawget(var1) instanceof Double ? ((Double)this.rawget(var1)).intValue() : -1;
   }

   public boolean rawgetBool(Object var1) {
      return this.rawget(var1) instanceof Boolean ? (Boolean)this.rawget(var1) : false;
   }

   public float rawgetFloat(Object var1) {
      return this.rawget(var1) instanceof Double ? ((Double)this.rawget(var1)).floatValue() : -1.0F;
   }

   public Object rawget(int var1) {
      return this.rawget(KahluaUtil.toDouble((long)var1));
   }

   public int len() {
      return KahluaUtil.len(this, 0, 2 * this.delegate.size());
   }

   public KahluaTableIterator iterator() {
      final Object[] var1 = this.delegate.isEmpty() ? null : this.delegate.keySet().toArray();
      return new KahluaTableIterator() {
         private Object curKey;
         private Object curValue;
         private int keyIndex;

         public int call(LuaCallFrame var1x, int var2) {
            return this.advance() ? var1x.push(this.getKey(), this.getValue()) : 0;
         }

         public boolean advance() {
            if (var1 != null && this.keyIndex < var1.length) {
               this.curKey = var1[this.keyIndex];
               this.curValue = PZNetKahluaTableImpl.this.delegate.get(this.curKey);
               ++this.keyIndex;
               return true;
            } else {
               this.curKey = null;
               this.curValue = null;
               return false;
            }
         }

         public Object getKey() {
            return this.curKey;
         }

         public Object getValue() {
            return this.curValue;
         }
      };
   }

   public boolean isEmpty() {
      return this.delegate.isEmpty();
   }

   public void wipe() {
      this.delegate.clear();
   }

   public String toString() {
      return "table 0x" + System.identityHashCode(this);
   }

   private void saveInventoryItem(ByteBuffer var1, InventoryItem var2) {
      ContainerID var3 = new ContainerID();
      var3.set(var2.getContainer());
      var3.write(var1);
      var1.putInt(var2.getID());
   }

   private void saveIsoObject(ByteBuffer var1, IsoObject var2) {
      NetObject var3 = new NetObject();
      var3.setObject(var2);
      var3.write(var1);
   }

   private void saveResource(ByteBuffer var1, Resource var2) {
      var1.putLong(var2.getGameEntity().getEntityNetID());
      GameWindow.WriteString(var1, var2.getId());
   }

   public void save(ByteBuffer var1) {
      KahluaTableIterator var2 = this.iterator();
      int var3 = 0;

      while(var2.advance()) {
         if (canSave(var2.getKey(), var2.getValue())) {
            ++var3;
         }
      }

      var2 = this.iterator();
      var1.putInt(var3);

      while(var2.advance()) {
         byte var4 = getKeyByte(var2.getKey());
         byte var5 = getValueByte(var2.getValue());
         if (var4 != -1 && var5 != -1) {
            this.save(var1, var4, var2.getKey());
            this.save(var1, var5, var2.getValue());
         }
      }

   }

   private void save(ByteBuffer var1, byte var2, Object var3) throws RuntimeException {
      var1.put(var2);
      if (var2 == 1) {
         if ("fireplace".equals(var3)) {
            boolean var4 = true;
         }

         GameWindow.WriteString(var1, (String)var3);
      } else if (var2 == 2) {
         var1.putDouble((Double)var3);
      } else if (var2 == 5) {
         var1.put((byte)((Boolean)var3 ? 1 : 0));
      } else if (var2 == 3) {
         ((KahluaTableImpl)var3).save(var1);
      } else if (var2 == 4) {
         ((PZNetKahluaTableImpl)var3).save(var1);
      } else if (var2 == 0) {
         var1.putInt((Integer)var3);
      } else if (var2 == 6) {
         this.saveInventoryItem(var1, (InventoryItem)var3);
      } else {
         PlayerID var7;
         if (var2 == 8) {
            var7 = new PlayerID();
            var7.set((IsoPlayer)var3);
            var7.write(var1);
         } else if (var2 == 7) {
            this.saveIsoObject(var1, (IsoObject)var3);
         } else if (var2 == 9) {
            ContainerID var8 = new ContainerID();
            var8.set((ItemContainer)var3);
            var8.write(var1);
         } else if (var2 == 10) {
            var7 = new PlayerID();
            var7.set((IsoPlayer)((BodyPart)var3).getParentChar());
            var7.write(var1);
            var1.putInt(((BodyPart)var3).getIndex());
         } else {
            VehiclePart var9;
            if (var2 == 11) {
               var9 = (VehiclePart)var3;
               var1.putShort(var9.getVehicle().VehicleID);
               GameWindow.WriteString(var1, var9.getId());
            } else if (var2 == 12) {
               BaseVehicle var11 = (BaseVehicle)var3;
               var1.putShort(var11.VehicleID);
            } else if (var2 == 13) {
               var1.putInt(((IsoGridSquare)var3).getX());
               var1.putInt(((IsoGridSquare)var3).getY());
               var1.put((byte)((IsoGridSquare)var3).getZ());
            } else if (var2 == 14) {
               GameWindow.WriteString(var1, ((Recipe)var3).getFullType());
            } else if (var2 == 22) {
               GameWindow.WriteString(var1, ((EvolvedRecipe)var3).getScriptObjectFullType());
            } else if (var2 == 16) {
               ((IsoDeadBody)var3).getObjectID().save(var1);
            } else if (var2 == 18) {
               GameWindow.WriteString(var1, ((SpriteConfigManager.ObjectInfo)var3).getName());
            } else if (var2 == 19) {
               this.saveResource(var1, (Resource)var3);
            } else if (var2 == 20) {
               var1.putInt(((IsoHutch.NestBox)var3).getIndex());
            } else if (var2 != 27) {
               if (var2 == 15) {
                  var1.putInt(((BloodBodyPartType)var3).index());
               } else if (var2 == 17) {
                  AnimalID var12 = new AnimalID();
                  var12.set((IsoAnimal)var3);
                  var12.write(var1);
               } else if (var2 == 21) {
                  GameWindow.WriteString(var1, ((MultiStageBuilding.Stage)var3).ID);
               } else if (var2 == 23) {
                  GameEntity var13 = ((FluidContainer)var3).getGameEntity();
                  if (var13 instanceof InventoryItem) {
                     InventoryItem var5 = (InventoryItem)var13;
                     var1.put((byte)6);
                     this.saveInventoryItem(var1, var5);
                  } else if (var13 instanceof IsoObject) {
                     IsoObject var6 = (IsoObject)var13;
                     var1.put((byte)7);
                     this.saveIsoObject(var1, var6);
                  }
               } else if (var2 == 24) {
                  var9 = ((VehicleWindow)var3).getPart();
                  var1.putShort(var9.getVehicle().VehicleID);
                  GameWindow.WriteString(var1, var9.getId());
               } else if (var2 == 25) {
                  GameWindow.WriteString(var1, ((CraftRecipe)var3).getName());
               } else {
                  if (var2 != 26) {
                     throw new RuntimeException("invalid lua table type " + var2);
                  }

                  ArrayList var14 = (ArrayList)var3;
                  var1.putInt(var14.size());
                  if (!var14.isEmpty()) {
                     byte var10 = getValueByte(var14.get(0));
                     if (var10 == -1) {
                        return;
                     }

                     var14.forEach((var3x) -> {
                        this.save(var1, var10, var3x);
                     });
                  }
               }
            }
         }
      }

   }

   public void save(DataOutputStream var1) throws IOException {
      throw new RuntimeException("The PZNetKahluaTableImpl.save function isn't implemented");
   }

   private InventoryItem loadInventoryItem(ByteBuffer var1, UdpConnection var2) {
      ContainerID var3 = new ContainerID();
      var3.parse(var1, var2);
      int var4 = var1.getInt();
      return var3.getContainer() != null ? var3.getContainer().getItemWithID(var4) : null;
   }

   private IsoObject loadIsoObject(ByteBuffer var1, UdpConnection var2) {
      NetObject var3 = new NetObject();
      var3.parse(var1, var2);
      return var3.getObject();
   }

   private Resource loadResource(ByteBuffer var1) {
      long var2 = var1.getLong();
      String var4 = GameWindow.ReadString(var1);
      GameEntity var5 = GameEntityManager.GetEntity(var2);
      Resources var6 = (Resources)var5.getComponent(ComponentType.Resources);
      return var6 != null ? var6.getResource(var4) : null;
   }

   public void load(ByteBuffer var1, int var2) {
      throw new RuntimeException("The PZNetKahluaTableImpl.load function isn't implemented");
   }

   public void load(ByteBuffer var1, UdpConnection var2) {
      int var3 = var1.getInt();
      this.wipe();

      for(int var4 = 0; var4 < var3; ++var4) {
         byte var5 = var1.get();
         Object var6 = this.load(var1, var2, var5);
         byte var7 = var1.get();
         Object var8 = this.load(var1, var2, var7);
         this.delegate.put(var6, var8);
      }

   }

   public Object load(ByteBuffer var1, UdpConnection var2, byte var3) throws RuntimeException {
      String var9;
      if (var3 == 1) {
         var9 = GameWindow.ReadString(var1);
         if ("fireplace".equals(var9)) {
            boolean var22 = true;
         }

         return var9;
      } else if (var3 == 2) {
         return var1.getDouble();
      } else if (var3 == 5) {
         return var1.get() == 1;
      } else if (var3 == 3) {
         KahluaTableImpl var26 = (KahluaTableImpl)LuaManager.platform.newTable();
         var26.load(var1, 219);
         return var26;
      } else if (var3 == 4) {
         PZNetKahluaTableImpl var25 = new PZNetKahluaTableImpl(new LinkedHashMap());
         var25.load(var1, var2);
         return var25;
      } else if (var3 == 0) {
         return var1.getInt();
      } else if (var3 == 6) {
         return this.loadInventoryItem(var1, var2);
      } else {
         PlayerID var23;
         if (var3 == 8) {
            var23 = new PlayerID();
            var23.parse(var1, var2);
            var23.parsePlayer(var2);
            return var23.getPlayer();
         } else if (var3 == 7) {
            return this.loadIsoObject(var1, var2);
         } else if (var3 == 9) {
            ContainerID var24 = new ContainerID();
            var24.parse(var1, var2);
            return var24.getContainer();
         } else {
            int var16;
            if (var3 == 10) {
               var23 = new PlayerID();
               var23.parse(var1, var2);
               var23.parsePlayer(var2);
               var16 = var1.getInt();
               return var23.getPlayer().getBodyDamage().getBodyParts().get(var16);
            } else {
               short var10;
               String var12;
               VehiclePart var19;
               if (var3 == 11) {
                  var10 = var1.getShort();
                  var12 = GameWindow.ReadString(var1);
                  var19 = VehicleManager.instance.getVehicleByID(var10).getPartById(var12);
                  return var19;
               } else if (var3 == 12) {
                  var10 = var1.getShort();
                  BaseVehicle var18 = VehicleManager.instance.getVehicleByID(var10);
                  return var18;
               } else {
                  int var4;
                  if (var3 == 13) {
                     var4 = var1.getInt();
                     var16 = var1.getInt();
                     byte var20 = var1.get();
                     IsoGridSquare var21 = ServerMap.instance.getGridSquare(var4, var16, var20);
                     return var21;
                  } else if (var3 == 14) {
                     var9 = GameWindow.ReadString(var1);
                     return ScriptManager.instance.getRecipe(var9);
                  } else if (var3 == 22) {
                     var9 = GameWindow.ReadString(var1);
                     return ScriptManager.instance.getEvolvedRecipe(var9);
                  } else if (var3 == 16) {
                     ObjectID var17 = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
                     var17.parse(var1, (UdpConnection)null);
                     return var17.getObject();
                  } else if (var3 == 18) {
                     var9 = GameWindow.ReadString(var1);
                     return SpriteConfigManager.GetObjectInfo(var9);
                  } else if (var3 == 19) {
                     return this.loadResource(var1);
                  } else if (var3 == 20) {
                     return var1.getInt();
                  } else if (var3 == 27) {
                     return null;
                  } else if (var3 == 15) {
                     return BloodBodyPartType.FromIndex(var1.getInt());
                  } else if (var3 == 17) {
                     AnimalID var13 = new AnimalID();
                     var13.parse(var1, var2);
                     return var13.getAnimal();
                  } else if (var3 == 21) {
                     var9 = GameWindow.ReadString(var1);
                     return MultiStageBuilding.getStage(var9);
                  } else if (var3 == 23) {
                     byte var11 = var1.get();
                     if (var11 == 6) {
                        InventoryItem var14 = this.loadInventoryItem(var1, var2);
                        if (var14 != null) {
                           return var14.getFluidContainer();
                        }
                     } else if (var11 == 7) {
                        IsoObject var15 = this.loadIsoObject(var1, var2);
                        if (var15 != null) {
                           return var15.getFluidContainer();
                        }
                     }

                     return null;
                  } else if (var3 == 24) {
                     var10 = var1.getShort();
                     var12 = GameWindow.ReadString(var1);
                     var19 = VehicleManager.instance.getVehicleByID(var10).getPartById(var12);
                     return var19.getWindow();
                  } else if (var3 == 25) {
                     var9 = GameWindow.ReadString(var1);
                     return ScriptManager.instance.getCraftRecipe(var9);
                  } else if (var3 != 26) {
                     throw new RuntimeException("invalid lua table type " + var3);
                  } else {
                     var4 = var1.getInt();
                     ArrayList var5 = new ArrayList();

                     for(int var6 = 0; var6 < var4; ++var6) {
                        byte var7 = var1.get();
                        if (var7 == -1) {
                           break;
                        }

                        Object var8 = this.load(var1, var2, var7);
                        if (var8 != null) {
                           var5.add(var8);
                        }
                     }

                     return var5;
                  }
               }
            }
         }
      }
   }

   public void load(DataInputStream var1, int var2) throws IOException {
      throw new RuntimeException("The PZNetKahluaTableImpl.load function isn't implemented");
   }

   public String getString(String var1) {
      return (String)this.rawget(var1);
   }

   public KahluaTableImpl getRewriteTable() {
      return (KahluaTableImpl)this.reloadReplace;
   }

   public void setRewriteTable(Object var1) {
      this.reloadReplace = (KahluaTableImpl)var1;
   }

   private static byte getKeyByte(Object var0) {
      if (var0 instanceof String) {
         return 1;
      } else {
         return (byte)(var0 instanceof Double ? 2 : -1);
      }
   }

   private static byte getValueByte(Object var0) {
      if (var0 instanceof String) {
         return 1;
      } else if (var0 instanceof Double) {
         return 2;
      } else if (var0 instanceof Boolean) {
         return 5;
      } else if (var0 instanceof KahluaTableImpl) {
         return 3;
      } else if (var0 instanceof PZNetKahluaTableImpl) {
         return 4;
      } else if (var0 instanceof InventoryItem) {
         return 6;
      } else if (var0 instanceof IsoAnimal) {
         return 17;
      } else if (var0 instanceof IsoPlayer) {
         return 8;
      } else if (var0 instanceof BaseVehicle) {
         return 12;
      } else if (var0 instanceof IsoDeadBody) {
         return 16;
      } else if (var0 instanceof Resource) {
         return 19;
      } else if (var0 instanceof SpriteConfigManager.ObjectInfo) {
         return 18;
      } else if (var0 instanceof IsoObject) {
         return 7;
      } else if (var0 instanceof Integer) {
         return 0;
      } else if (var0 instanceof ItemContainer) {
         return 9;
      } else if (var0 instanceof BodyPart) {
         return 10;
      } else if (var0 instanceof VehiclePart) {
         return 11;
      } else if (var0 instanceof IsoGridSquare) {
         return 13;
      } else if (var0 instanceof Recipe) {
         return 14;
      } else if (var0 instanceof CraftRecipe) {
         return 25;
      } else if (var0 instanceof EvolvedRecipe) {
         return 22;
      } else if (var0 instanceof BloodBodyPartType) {
         return 15;
      } else if (var0 instanceof IsoHutch.NestBox) {
         return 20;
      } else if (var0 instanceof MultiStageBuilding.Stage) {
         return 21;
      } else if (var0 instanceof FluidContainer) {
         return 23;
      } else if (var0 instanceof VehicleWindow) {
         return 24;
      } else if (var0 instanceof ArrayList) {
         return 26;
      } else {
         return (byte)(var0 instanceof PZNetKahluaNull ? 27 : -1);
      }
   }

   public static boolean canSave(Object var0, Object var1) {
      return getKeyByte(var0) != -1 && getValueByte(var1) != -1;
   }
}
