package zombie.ui;

import java.util.ArrayList;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.core.BoxedStaticValues;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.input.Mouse;

public class AtomUI implements UIElementInterface {
   static int StencilLevel = 0;
   boolean bStencil = false;
   AtomUI stencilNode = null;
   KahluaTable table;
   String uiname = "";
   final ArrayList<AtomUI> nodes = new ArrayList();
   AtomUI parentNode = null;
   boolean visible = true;
   boolean enabled = true;
   boolean alwaysOnTop = false;
   boolean alwaysBack = false;
   Double anchorLeft = null;
   Double anchorRight = null;
   Double anchorTop = null;
   Double anchorDown = null;
   double x = 0.0;
   double y = 0.0;
   double width = 0.0;
   double height = 0.0;
   double pivotX = 0.5;
   double pivotY = 0.5;
   double angle = 0.0;
   double scaleX = 1.0;
   double scaleY = 1.0;
   float colorR = 1.0F;
   float colorG = 1.0F;
   float colorB = 1.0F;
   float colorA = 1.0F;
   double sinA = 0.0;
   double cosA = 1.0;
   double leftSide = 0.0;
   double rightSide = 256.0;
   double topSide = 0.0;
   double downSide = 256.0;
   Object luaMouseButtonDown;
   Object luaMouseButtonUp;
   Object luaMouseButtonDownOutside;
   Object luaMouseButtonUpOutside;
   Object luaMouseWheel;
   Object luaMouseMove;
   Object luaMouseMoveOutside;
   Object luaUpdate;
   Object luaRenderUpdate;
   Object luaKeyPress;
   Object luaKeyRepeat;
   Object luaKeyRelease;
   Object luaResize;

   public AtomUI(KahluaTable var1) {
      this.table = var1;
   }

   public void init() {
      this.loadFromTable();
      this.updateInternalValues();
      this.updateSize();
   }

   public Boolean isIgnoreLossControl() {
      return false;
   }

   public Boolean isFollowGameWorld() {
      return false;
   }

   public Boolean isDefaultDraw() {
      return true;
   }

   public void render() {
      if (this.visible) {
         if (this.checkStencilCollision()) {
            if (this.luaRenderUpdate != null) {
               LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaRenderUpdate, this.table);
            }

            int var1;
            if (this.bStencil) {
               this.setStencilRect();

               for(var1 = 0; var1 < this.nodes.size(); ++var1) {
                  ((AtomUI)this.nodes.get(var1)).stencilNode = this;
                  ((AtomUI)this.nodes.get(var1)).render();
               }

               this.clearStencilRect();
            } else {
               for(var1 = 0; var1 < this.nodes.size(); ++var1) {
                  ((AtomUI)this.nodes.get(var1)).stencilNode = this.stencilNode;
                  ((AtomUI)this.nodes.get(var1)).render();
               }
            }

         }
      }
   }

   private boolean checkStencilCollision() {
      if (this.stencilNode == null) {
         return true;
      } else {
         double[] var1 = this.getAbsolutePosition(0.0, 0.0);
         double[] var2 = this.getAbsolutePosition(this.width, this.height);
         double[] var3 = this.stencilNode.getAbsolutePosition(0.0, 0.0);
         double[] var4 = this.stencilNode.getAbsolutePosition(this.stencilNode.width, this.stencilNode.height);
         return !(var1[0] > var4[0]) && !(var1[1] > var4[1]) && !(var2[0] < var3[0]) && !(var2[1] < var3[1]);
      }
   }

   public Boolean isVisible() {
      return this.visible ? Boolean.TRUE : Boolean.FALSE;
   }

   public void setVisible(boolean var1) {
      this.visible = var1;
   }

   public Boolean isCapture() {
      return false;
   }

   public Double getMaxDrawHeight() {
      return BoxedStaticValues.toDouble(-1.0);
   }

   public Double getX() {
      return BoxedStaticValues.toDouble(this.x);
   }

   public void setX(double var1) {
      this.x = var1;
   }

   public Double getY() {
      return BoxedStaticValues.toDouble(this.y);
   }

   public void setY(double var1) {
      this.y = var1;
   }

   public Double getWidth() {
      return BoxedStaticValues.toDouble(this.width);
   }

   public void setWidth(double var1) {
      this.width = var1;
      this.updateInternalValues();
      this.onResize();
   }

   public void setWidthSilent(double var1) {
      this.width = var1;
      this.table.rawset("width", var1);
      this.updateInternalValues();
   }

   public Double getHeight() {
      return BoxedStaticValues.toDouble(this.height);
   }

   public void setHeight(double var1) {
      this.height = var1;
      this.updateInternalValues();
      this.onResize();
   }

   public void setHeightSilent(double var1) {
      this.height = var1;
      this.table.rawset("height", var1);
      this.updateInternalValues();
   }

   public void bringToTop() {
      if (this.parentNode != null) {
         this.parentNode.bringToTop();
      } else {
         UIManager.pushToTop(this);
      }

   }

   public boolean isOverElement(double var1, double var3) {
      return !this.visible ? false : this.isOverElementLocal(var1 - this.x, var3 - this.y);
   }

   public UIElementInterface getParent() {
      return this.parentNode;
   }

   public boolean onConsumeMouseButtonDown(int var1, double var2, double var4) {
      if (!this.enabled) {
         return false;
      } else {
         double[] var6 = this.toLocalCoordinates(var2, var4);
         double var7 = var6[0];
         double var9 = var6[1];
         this.bringToTop();
         boolean var11 = false;

         for(int var12 = this.nodes.size() - 1; var12 >= 0; --var12) {
            AtomUI var13 = (AtomUI)this.nodes.get(var12);
            if (!var11 && var13.isOverElementLocal(var7 - var13.x, var9 - var13.y)) {
               if (var13.onConsumeMouseButtonDown(var1, var7 - var13.x, var9 - var13.y)) {
                  var11 = true;
               }
            } else {
               var13.onMouseButtonDownOutside(var1, var7 - var13.x, var9 - var13.y);
            }
         }

         if (!var11 && var7 >= this.leftSide && var9 >= this.topSide && var7 < this.rightSide && var9 < this.downSide && this.luaMouseButtonDown != null) {
            Boolean var14 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaMouseButtonDown, new Object[]{this.table, BoxedStaticValues.toDouble((double)var1), BoxedStaticValues.toDouble(var7), BoxedStaticValues.toDouble(var9)});
            return var14 == Boolean.TRUE;
         } else {
            return var11;
         }
      }
   }

   public boolean onConsumeMouseButtonUp(int var1, double var2, double var4) {
      if (!this.enabled) {
         return false;
      } else {
         double[] var6 = this.toLocalCoordinates(var2, var4);
         double var7 = var6[0];
         double var9 = var6[1];
         boolean var11 = false;

         for(int var12 = this.nodes.size() - 1; var12 >= 0; --var12) {
            AtomUI var13 = (AtomUI)this.nodes.get(var12);
            if (!var11 && var13.isOverElementLocal(var7 - var13.x, var9 - var13.y)) {
               if (var13.onConsumeMouseButtonUp(var1, var7 - var13.x, var9 - var13.y)) {
                  var11 = true;
               }
            } else {
               var13.onMouseButtonUpOutside(var1, var7 - var13.x, var9 - var13.y);
            }
         }

         if (!var11 && var7 >= this.leftSide && var9 >= this.topSide && var7 < this.rightSide && var9 < this.downSide && this.luaMouseButtonUp != null) {
            Boolean var14 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaMouseButtonUp, new Object[]{this.table, BoxedStaticValues.toDouble((double)var1), BoxedStaticValues.toDouble(var7), BoxedStaticValues.toDouble(var9)});
            return var14 == Boolean.TRUE;
         } else {
            return var11;
         }
      }
   }

   public void onMouseButtonDownOutside(int var1, double var2, double var4) {
      if (this.enabled) {
         double[] var6 = this.toLocalCoordinates(var2, var4);

         for(int var7 = this.nodes.size() - 1; var7 >= 0; --var7) {
            AtomUI var8 = (AtomUI)this.nodes.get(var7);
            var8.onMouseButtonDownOutside(var1, var6[0] - var8.x, var6[1] - var8.y);
         }

         if (this.luaMouseButtonDownOutside != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaMouseButtonDownOutside, new Object[]{this.table, BoxedStaticValues.toDouble((double)var1), BoxedStaticValues.toDouble(var6[0]), BoxedStaticValues.toDouble(var6[1])});
         }

      }
   }

   public void onMouseButtonUpOutside(int var1, double var2, double var4) {
      if (this.enabled) {
         double[] var6 = this.toLocalCoordinates(var2, var4);

         for(int var7 = this.nodes.size() - 1; var7 >= 0; --var7) {
            AtomUI var8 = (AtomUI)this.nodes.get(var7);
            var8.onMouseButtonUpOutside(var1, var6[0] - var8.x, var6[1] - var8.y);
         }

         if (this.luaMouseButtonUpOutside != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaMouseButtonUpOutside, new Object[]{this.table, BoxedStaticValues.toDouble((double)var1), BoxedStaticValues.toDouble(var6[0]), BoxedStaticValues.toDouble(var6[1])});
         }

      }
   }

   public Boolean onConsumeMouseWheel(double var1, double var3, double var5) {
      if (!this.enabled) {
         return false;
      } else {
         double[] var7 = this.toLocalCoordinates(var3, var5);
         double var8 = var7[0];
         double var10 = var7[1];
         boolean var12 = false;

         for(int var13 = this.nodes.size() - 1; var13 >= 0 && !var12; --var13) {
            AtomUI var14 = (AtomUI)this.nodes.get(var13);
            if (var14.isOverElementLocal(var8 - var14.x, var10 - var14.y) && var14.onConsumeMouseWheel(var1, var8 - var14.x, var10 - var14.y)) {
               var12 = true;
            }
         }

         if (!var12 && var8 >= this.leftSide && var10 >= this.topSide && var8 < this.rightSide && var10 < this.downSide && this.luaMouseWheel != null) {
            Boolean var15 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaMouseWheel, new Object[]{this.table, BoxedStaticValues.toDouble(var1), BoxedStaticValues.toDouble(var8), BoxedStaticValues.toDouble(var10)});
            return var15 == Boolean.TRUE;
         } else {
            return var12;
         }
      }
   }

   public Boolean isPointOver(double var1, double var3) {
      return this.isOverElement(var1, var3);
   }

   public Boolean onConsumeMouseMove(double var1, double var3, double var5, double var7) {
      if (!this.enabled) {
         return false;
      } else {
         double[] var9 = this.toLocalCoordinates(var5, var7);
         double var10 = var9[0];
         double var12 = var9[1];
         boolean var14 = false;

         for(int var15 = this.nodes.size() - 1; var15 >= 0; --var15) {
            AtomUI var16 = (AtomUI)this.nodes.get(var15);
            if (!var14 && var16.isOverElementLocal(var10 - var16.x, var12 - var16.y)) {
               if (var16.onConsumeMouseMove(var1, var3, var10 - var16.x, var12 - var16.y)) {
                  var14 = true;
               }
            } else {
               var16.onExtendMouseMoveOutside(var1, var3, var10 - var16.x, var12 - var16.y);
            }
         }

         if (!var14 && var10 >= this.leftSide && var12 >= this.topSide && var10 < this.rightSide && var12 < this.downSide && this.luaMouseMove != null) {
            Boolean var17 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaMouseMove, new Object[]{this.table, BoxedStaticValues.toDouble(var1), BoxedStaticValues.toDouble(var3)});
            return var17 == Boolean.TRUE;
         } else {
            return var14;
         }
      }
   }

   public void onExtendMouseMoveOutside(double var1, double var3, double var5, double var7) {
      if (this.enabled) {
         double[] var9 = this.toLocalCoordinates(var5, var7);

         for(int var10 = this.nodes.size() - 1; var10 >= 0; --var10) {
            AtomUI var11 = (AtomUI)this.nodes.get(var10);
            var11.onExtendMouseMoveOutside(var1, var3, var9[0] - var11.x, var9[1] - var11.y);
         }

         if (this.luaMouseMoveOutside != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaMouseMoveOutside, new Object[]{this.table, BoxedStaticValues.toDouble(var1), BoxedStaticValues.toDouble(var3)});
         }

      }
   }

   public void update() {
      if (this.enabled) {
         if (UIManager.doTick) {
            if (this.luaUpdate != null) {
               LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaUpdate, this.table);
            }

            for(int var1 = 0; var1 < this.nodes.size(); ++var1) {
               ((AtomUI)this.nodes.get(var1)).update();
            }

         }
      }
   }

   public Boolean isMouseOver() {
      return this.isPointOver((double)Mouse.getXA(), (double)Mouse.getYA()) ? Boolean.TRUE : Boolean.FALSE;
   }

   public boolean isWantKeyEvents() {
      return true;
   }

   public int getRenderThisPlayerOnly() {
      return -1;
   }

   public boolean onConsumeKeyPress(int var1) {
      if (!this.enabled) {
         return false;
      } else {
         for(int var2 = this.nodes.size() - 1; var2 >= 0; --var2) {
            AtomUI var3 = (AtomUI)this.nodes.get(var2);
            if (var3.onConsumeKeyPress(var1)) {
               return true;
            }
         }

         if (this.luaKeyPress != null) {
            Boolean var4 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaKeyPress, this.table, BoxedStaticValues.toDouble((double)var1));
            return var4 == Boolean.TRUE;
         } else {
            return false;
         }
      }
   }

   public boolean onConsumeKeyRepeat(int var1) {
      if (!this.enabled) {
         return false;
      } else {
         for(int var2 = this.nodes.size() - 1; var2 >= 0; --var2) {
            AtomUI var3 = (AtomUI)this.nodes.get(var2);
            if (var3.onConsumeKeyRepeat(var1)) {
               return true;
            }
         }

         if (this.luaKeyRepeat != null) {
            Boolean var4 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaKeyRepeat, this.table, BoxedStaticValues.toDouble((double)var1));
            return var4 == Boolean.TRUE;
         } else {
            return false;
         }
      }
   }

   public boolean onConsumeKeyRelease(int var1) {
      if (!this.enabled) {
         return false;
      } else {
         for(int var2 = this.nodes.size() - 1; var2 >= 0; --var2) {
            AtomUI var3 = (AtomUI)this.nodes.get(var2);
            if (var3.onConsumeKeyRelease(var1)) {
               return true;
            }
         }

         if (this.luaKeyRelease != null) {
            Boolean var4 = LuaManager.caller.protectedCallBoolean(UIManager.getDefaultThread(), this.luaKeyRelease, this.table, BoxedStaticValues.toDouble((double)var1));
            return var4 == Boolean.TRUE;
         } else {
            return false;
         }
      }
   }

   public boolean isForceCursorVisible() {
      return false;
   }

   public KahluaTable getLuaLocalPosition(double var1, double var3) {
      double[] var5 = this.getLocalPosition(var1, var3);
      KahluaTable var6 = LuaManager.platform.newTable();
      var6.rawset("x", var5[0]);
      var6.rawset("y", var5[1]);
      return var6;
   }

   public KahluaTable getLuaAbsolutePosition(double var1, double var3) {
      double[] var5 = this.getAbsolutePosition(var1, var3);
      KahluaTable var6 = LuaManager.platform.newTable();
      var6.rawset("x", var5[0]);
      var6.rawset("y", var5[1]);
      return var6;
   }

   public KahluaTable getLuaParentPosition(double var1, double var3) {
      var1 *= this.scaleX;
      var3 *= this.scaleY;
      double var5 = var1 * this.cosA + var3 * this.sinA;
      double var7 = -var1 * this.sinA + var3 * this.cosA;
      KahluaTable var9 = LuaManager.platform.newTable();
      var9.rawset("x", var5);
      var9.rawset("y", var7);
      return var9;
   }

   public AtomUI getParentNode() {
      return this.parentNode;
   }

   public void setParentNode(AtomUI var1) {
      this.parentNode = var1;
   }

   public void addNode(AtomUI var1) {
      this.nodes.add(var1);
      var1.setParentNode(this);
   }

   public void removeNode(AtomUI var1) {
      this.nodes.remove(var1);
      var1.setParentNode((AtomUI)null);
   }

   public ArrayList<AtomUI> getNodes() {
      return this.nodes;
   }

   public void setPivotX(double var1) {
      this.pivotX = var1;
      this.updateInternalValues();
   }

   public Double getPivotX() {
      return this.pivotX;
   }

   public void setPivotY(double var1) {
      this.pivotY = var1;
      this.updateInternalValues();
   }

   public Double getPivotY() {
      return this.pivotY;
   }

   public void setAngle(double var1) {
      this.angle = var1;
      this.updateInternalValues();
   }

   public Double getAngle() {
      return this.angle;
   }

   public void setScaleX(double var1) {
      this.scaleX = var1;
      this.onResize();
   }

   public Double getScaleX() {
      return this.scaleX;
   }

   public void setScaleY(double var1) {
      this.scaleY = var1;
      this.onResize();
   }

   public Double getScaleY() {
      return this.scaleY;
   }

   public void setColor(double var1, double var3, double var5, double var7) {
      this.colorR = (float)var1;
      this.colorG = (float)var3;
      this.colorB = (float)var5;
      this.colorA = (float)var7;
   }

   public KahluaTable getColor() {
      KahluaTable var1 = LuaManager.platform.newTable();
      var1.rawset("r", BoxedStaticValues.toDouble((double)this.colorR));
      var1.rawset("g", BoxedStaticValues.toDouble((double)this.colorG));
      var1.rawset("b", BoxedStaticValues.toDouble((double)this.colorB));
      var1.rawset("a", BoxedStaticValues.toDouble((double)this.colorA));
      return var1;
   }

   public KahluaTable getTable() {
      return this.table;
   }

   public Boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean var1) {
      this.enabled = var1;
   }

   public void setAlwaysOnTop(boolean var1) {
      this.alwaysOnTop = var1;
   }

   public boolean isAlwaysOnTop() {
      return this.alwaysOnTop;
   }

   public void setBackMost(boolean var1) {
      this.alwaysBack = var1;
   }

   public boolean isBackMost() {
      return this.alwaysBack;
   }

   public String getUIName() {
      return this.uiname;
   }

   public void setUIName(String var1) {
      this.uiname = var1;
   }

   void loadFromTable() {
      this.uiname = this.tryGetString("uiname", "");
      this.x = this.tryGetDouble("x", 0.0);
      this.y = this.tryGetDouble("y", 0.0);
      this.width = (double)((float)this.tryGetDouble("width", 256.0));
      this.height = (double)((float)this.tryGetDouble("height", 256.0));
      this.pivotX = this.tryGetDouble("pivotX", 0.0);
      this.pivotY = this.tryGetDouble("pivotY", 0.0);
      this.angle = this.tryGetDouble("angle", 0.0);
      this.scaleX = this.tryGetDouble("scaleX", 1.0);
      this.scaleY = this.tryGetDouble("scaleY", 1.0);
      this.colorR = (float)this.tryGetDouble("r", 1.0);
      this.colorG = (float)this.tryGetDouble("g", 1.0);
      this.colorB = (float)this.tryGetDouble("b", 1.0);
      this.colorA = (float)this.tryGetDouble("a", 1.0);
      this.visible = this.tryGetBoolean("visible", true);
      this.enabled = this.tryGetBoolean("enabled", true);
      this.bStencil = this.tryGetBoolean("isStencil", false);
      Object var1 = UIManager.tableget(this.table, "anchorLeft");
      this.anchorLeft = var1 instanceof Double ? (Double)var1 : null;
      var1 = UIManager.tableget(this.table, "anchorRight");
      this.anchorRight = var1 instanceof Double ? (Double)var1 : null;
      var1 = UIManager.tableget(this.table, "anchorTop");
      this.anchorTop = var1 instanceof Double ? (Double)var1 : null;
      var1 = UIManager.tableget(this.table, "anchorDown");
      this.anchorDown = var1 instanceof Double ? (Double)var1 : null;
      this.luaMouseButtonDown = this.tryGetClosure("onMouseButtonDown");
      this.luaMouseButtonUp = this.tryGetClosure("onMouseButtonUp");
      this.luaMouseButtonDownOutside = this.tryGetClosure("onMouseButtonDownOutside");
      this.luaMouseButtonUpOutside = this.tryGetClosure("onMouseButtonUpOutside");
      this.luaMouseWheel = this.tryGetClosure("onMouseWheel");
      this.luaMouseMove = this.tryGetClosure("onMouseMove");
      this.luaMouseMoveOutside = this.tryGetClosure("onMouseMoveOutside");
      this.luaUpdate = this.tryGetClosure("update");
      this.luaRenderUpdate = this.tryGetClosure("renderUpdate");
      this.luaKeyPress = this.tryGetClosure("onKeyPress");
      this.luaKeyRepeat = this.tryGetClosure("onKeyRepeat");
      this.luaKeyRelease = this.tryGetClosure("onKeyRelease");
      this.luaResize = this.tryGetClosure("onResize");
   }

   void updateInternalValues() {
      this.leftSide = -this.pivotX * this.width;
      this.topSide = -this.pivotY * this.height;
      this.rightSide = this.leftSide + this.width;
      this.downSide = this.topSide + this.height;
      double var1 = Math.toRadians(this.angle);
      this.cosA = Math.cos(var1);
      this.sinA = Math.sin(var1);
   }

   double[] toLocalCoordinates(double var1, double var3) {
      double var5 = var1 * this.cosA - var3 * this.sinA;
      double var7 = var1 * this.sinA + var3 * this.cosA;
      return new double[]{var5 / this.scaleX, var7 / this.scaleY};
   }

   double[] getLocalPosition(double var1, double var3) {
      double[] var5 = new double[]{var1, var3};
      if (this.parentNode != null) {
         var5 = this.parentNode.getLocalPosition(var1, var3);
      }

      return this.toLocalCoordinates(var5[0] - this.x, var5[1] - this.y);
   }

   double[] getAbsolutePosition(double var1, double var3) {
      var1 *= this.scaleX;
      var3 *= this.scaleY;
      double var5 = var1 * this.cosA + var3 * this.sinA + this.x;
      double var7 = -var1 * this.sinA + var3 * this.cosA + this.y;
      return this.parentNode != null ? this.parentNode.getAbsolutePosition(var5, var7) : new double[]{var5, var7};
   }

   boolean isOverElementLocal(double var1, double var3) {
      double[] var5 = this.toLocalCoordinates(var1, var3);
      double var6 = var5[0];
      double var8 = var5[1];
      if (var6 >= this.leftSide && var8 >= this.topSide && var6 < this.rightSide && var8 < this.downSide) {
         return true;
      } else if (this.bStencil && (var6 < this.leftSide || var8 < this.topSide || var6 >= this.rightSide || var8 >= this.downSide)) {
         return false;
      } else {
         for(int var10 = 0; var10 < this.nodes.size(); ++var10) {
            AtomUI var11 = (AtomUI)this.nodes.get(var10);
            if (var11.isOverElementLocal(var6 - var11.x, var8 - var11.y)) {
               return true;
            }
         }

         return false;
      }
   }

   void updateSize() {
      if (this.parentNode != null) {
         double var1 = this.parentNode.width;
         if (this.anchorLeft != null && this.anchorRight != null) {
            this.setWidthSilent((var1 - this.anchorLeft + this.anchorRight) / this.scaleX);
            this.setX(-this.parentNode.pivotX * var1 + this.anchorLeft + this.pivotX * this.width * this.scaleX);
         } else if (this.anchorLeft != null) {
            this.setX(-this.parentNode.pivotX * var1 + this.anchorLeft + this.pivotX * this.width * this.scaleX);
         } else if (this.anchorRight != null) {
            this.setX((1.0 - this.parentNode.pivotX) * var1 + this.anchorRight - (1.0 - this.pivotX) * this.width * this.scaleX);
         }

         double var3 = this.parentNode.height;
         if (this.anchorTop != null && this.anchorDown != null) {
            this.setHeightSilent((var3 - this.anchorTop + this.anchorDown) / this.scaleY);
            this.setY(-this.parentNode.pivotY * var3 + this.anchorTop + this.pivotY * this.height * this.scaleY);
         } else if (this.anchorTop != null) {
            this.setY(-this.parentNode.pivotY * var3 + this.anchorTop + this.pivotY * this.height * this.scaleY);
         } else if (this.anchorDown != null) {
            this.setY((1.0 - this.parentNode.pivotY) * var3 + this.anchorDown - (1.0 - this.pivotY) * this.height * this.scaleY);
         }

      }
   }

   void onResize() {
      this.updateSize();
      if (this.luaResize != null) {
         LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaResize, this.table, BoxedStaticValues.toDouble(this.width), BoxedStaticValues.toDouble(this.height));
      }

      for(int var1 = 0; var1 < this.nodes.size(); ++var1) {
         ((AtomUI)this.nodes.get(var1)).onResize();
      }

   }

   public void setStencilRect() {
      IndieGL.glStencilMask(255);
      IndieGL.enableStencilTest();
      IndieGL.enableAlphaTest();
      ++StencilLevel;
      IndieGL.glStencilFunc(519, StencilLevel, 255);
      IndieGL.glStencilOp(7680, 7680, 7681);
      double[] var1 = this.getAbsolutePosition(this.leftSide, this.topSide);
      double[] var2 = this.getAbsolutePosition(this.rightSide, this.topSide);
      double[] var3 = this.getAbsolutePosition(this.rightSide, this.downSide);
      double[] var4 = this.getAbsolutePosition(this.leftSide, this.downSide);
      IndieGL.glColorMask(false, false, false, false);
      SpriteRenderer.instance.render((Texture)null, var1[0], var1[1], var2[0], var2[1], var3[0], var3[1], var4[0], var4[1], 1.0F, 0.0F, 0.0F, 1.0F, (Consumer)null);
      IndieGL.glColorMask(true, true, true, true);
      IndieGL.glStencilOp(7680, 7680, 7680);
      IndieGL.glStencilFunc(514, StencilLevel, 255);
   }

   public void clearStencilRect() {
      if (StencilLevel > 0) {
         --StencilLevel;
      }

      if (StencilLevel > 0) {
         IndieGL.glStencilFunc(514, StencilLevel, 255);
      } else {
         IndieGL.glAlphaFunc(519, 0.0F);
         IndieGL.disableStencilTest();
         IndieGL.disableAlphaTest();
         IndieGL.glStencilFunc(519, 255, 255);
         IndieGL.glStencilOp(7680, 7680, 7680);
         IndieGL.glClear(1280);
      }

   }

   public void repaintStencilRect() {
      if (StencilLevel > 0) {
         double[] var1 = this.getAbsolutePosition(this.x, this.y);
         double[] var2 = this.getAbsolutePosition(this.x + this.width, this.y + this.height);
         IndieGL.glStencilFunc(519, StencilLevel, 255);
         IndieGL.glStencilOp(7680, 7680, 7681);
         double[] var3 = this.getAbsolutePosition(this.leftSide, this.topSide);
         double[] var4 = this.getAbsolutePosition(this.rightSide, this.topSide);
         double[] var5 = this.getAbsolutePosition(this.rightSide, this.downSide);
         double[] var6 = this.getAbsolutePosition(this.leftSide, this.downSide);
         IndieGL.glColorMask(false, false, false, false);
         SpriteRenderer.instance.render((Texture)null, var3[0], var3[1], var4[0], var4[1], var5[0], var5[1], var6[0], var6[1], 1.0F, 0.0F, 0.0F, 1.0F, (Consumer)null);
         IndieGL.glColorMask(true, true, true, true);
         IndieGL.glStencilOp(7680, 7680, 7680);
         IndieGL.glStencilFunc(514, StencilLevel, 255);
      }
   }

   double tryGetDouble(String var1, double var2) {
      Object var4 = UIManager.tableget(this.table, var1);
      return var4 instanceof Double ? (Double)var4 : var2;
   }

   boolean tryGetBoolean(String var1, boolean var2) {
      Object var3 = UIManager.tableget(this.table, var1);
      return var3 instanceof Boolean ? (Boolean)var3 : var2;
   }

   LuaClosure tryGetClosure(String var1) {
      Object var2 = UIManager.tableget(this.table, var1);
      return var2 instanceof LuaClosure ? (LuaClosure)var2 : null;
   }

   String tryGetString(String var1, String var2) {
      Object var3 = UIManager.tableget(this.table, var1);
      return var3 instanceof String ? (String)var3 : var2;
   }
}
