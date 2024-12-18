package zombie.ui;

public interface UIElementInterface {
   Boolean isIgnoreLossControl();

   Boolean isFollowGameWorld();

   Boolean isDefaultDraw();

   void render();

   Boolean isVisible();

   Boolean isCapture();

   Double getMaxDrawHeight();

   Double getX();

   Double getY();

   Double getWidth();

   Double getHeight();

   boolean isOverElement(double var1, double var3);

   UIElementInterface getParent();

   boolean onConsumeMouseButtonDown(int var1, double var2, double var4);

   boolean onConsumeMouseButtonUp(int var1, double var2, double var4);

   void onMouseButtonDownOutside(int var1, double var2, double var4);

   void onMouseButtonUpOutside(int var1, double var2, double var4);

   Boolean onConsumeMouseWheel(double var1, double var3, double var5);

   Boolean isPointOver(double var1, double var3);

   Boolean onConsumeMouseMove(double var1, double var3, double var5, double var7);

   void onExtendMouseMoveOutside(double var1, double var3, double var5, double var7);

   void update();

   Boolean isMouseOver();

   boolean isWantKeyEvents();

   boolean onConsumeKeyPress(int var1);

   boolean onConsumeKeyRepeat(int var1);

   boolean onConsumeKeyRelease(int var1);

   boolean isForceCursorVisible();

   int getRenderThisPlayerOnly();

   boolean isAlwaysOnTop();

   boolean isBackMost();
}
