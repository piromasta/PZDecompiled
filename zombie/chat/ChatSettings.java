package zombie.chat;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.network.ByteBufferWriter;
import zombie.ui.UIFont;

public class ChatSettings {
   private boolean unique;
   private Color fontColor;
   private UIFont font;
   private FontSize fontSize;
   private boolean bold;
   private boolean allowImages;
   private boolean allowChatIcons;
   private boolean allowColors;
   private boolean allowFonts;
   private boolean allowBBcode;
   private boolean equalizeLineHeights;
   private boolean showAuthor;
   private boolean showTimestamp;
   private boolean showChatTitle;
   private boolean useOnlyActiveTab;
   private float range;
   private float zombieAttractionRange;
   public static final float infinityRange = -1.0F;

   public ChatSettings() {
      this.unique = true;
      this.fontColor = Color.white;
      this.font = UIFont.Dialogue;
      this.bold = true;
      this.showAuthor = true;
      this.showTimestamp = true;
      this.showChatTitle = true;
      this.range = -1.0F;
      this.zombieAttractionRange = -1.0F;
      this.useOnlyActiveTab = false;
      this.fontSize = ChatSettings.FontSize.Medium;
   }

   public ChatSettings(ByteBuffer var1) {
      this.unique = var1.get() == 1;
      this.fontColor = new Color(var1.getFloat(), var1.getFloat(), var1.getFloat(), var1.getFloat());
      this.font = UIFont.FromString(GameWindow.ReadString(var1));
      this.bold = var1.get() == 1;
      this.allowImages = var1.get() == 1;
      this.allowChatIcons = var1.get() == 1;
      this.allowColors = var1.get() == 1;
      this.allowFonts = var1.get() == 1;
      this.allowBBcode = var1.get() == 1;
      this.equalizeLineHeights = var1.get() == 1;
      this.showAuthor = var1.get() == 1;
      this.showTimestamp = var1.get() == 1;
      this.showChatTitle = var1.get() == 1;
      this.range = var1.getFloat();
      if (var1.get() == 1) {
         this.zombieAttractionRange = var1.getFloat();
      } else {
         this.zombieAttractionRange = this.range;
      }

      this.fontSize = ChatSettings.FontSize.Medium;
   }

   public boolean isUnique() {
      return this.unique;
   }

   public void setUnique(boolean var1) {
      this.unique = var1;
   }

   public Color getFontColor() {
      return this.fontColor;
   }

   public void setFontColor(Color var1) {
      this.fontColor = var1;
   }

   public void setFontColor(float var1, float var2, float var3, float var4) {
      this.fontColor = new Color(var1, var2, var3, var4);
   }

   public UIFont getFont() {
      return this.font;
   }

   public void setFont(UIFont var1) {
      this.font = var1;
   }

   public String getFontSize() {
      return this.fontSize.toString().toLowerCase();
   }

   public void setFontSize(String var1) {
      switch (var1) {
         case "small":
         case "Small":
            this.fontSize = ChatSettings.FontSize.Small;
            break;
         case "medium":
         case "Medium":
            this.fontSize = ChatSettings.FontSize.Medium;
            break;
         case "large":
         case "Large":
            this.fontSize = ChatSettings.FontSize.Large;
            break;
         default:
            this.fontSize = ChatSettings.FontSize.NotDefine;
      }

   }

   public boolean isBold() {
      return this.bold;
   }

   public void setBold(boolean var1) {
      this.bold = var1;
   }

   public boolean isShowAuthor() {
      return this.showAuthor;
   }

   public void setShowAuthor(boolean var1) {
      this.showAuthor = var1;
   }

   public boolean isShowTimestamp() {
      return this.showTimestamp;
   }

   public void setShowTimestamp(boolean var1) {
      this.showTimestamp = var1;
   }

   public boolean isShowChatTitle() {
      return this.showChatTitle;
   }

   public void setShowChatTitle(boolean var1) {
      this.showChatTitle = var1;
   }

   public boolean isAllowImages() {
      return this.allowImages;
   }

   public void setAllowImages(boolean var1) {
      this.allowImages = var1;
   }

   public boolean isAllowChatIcons() {
      return this.allowChatIcons;
   }

   public void setAllowChatIcons(boolean var1) {
      this.allowChatIcons = var1;
   }

   public boolean isAllowColors() {
      return this.allowColors;
   }

   public void setAllowColors(boolean var1) {
      this.allowColors = var1;
   }

   public boolean isAllowFonts() {
      return this.allowFonts;
   }

   public void setAllowFonts(boolean var1) {
      this.allowFonts = var1;
   }

   public boolean isAllowBBcode() {
      return this.allowBBcode;
   }

   public void setAllowBBcode(boolean var1) {
      this.allowBBcode = var1;
   }

   public boolean isEqualizeLineHeights() {
      return this.equalizeLineHeights;
   }

   public void setEqualizeLineHeights(boolean var1) {
      this.equalizeLineHeights = var1;
   }

   public float getRange() {
      return this.range;
   }

   public void setRange(float var1) {
      this.range = var1;
   }

   public float getZombieAttractionRange() {
      return this.zombieAttractionRange == -1.0F ? this.range : this.zombieAttractionRange;
   }

   public void setZombieAttractionRange(float var1) {
      this.zombieAttractionRange = var1;
   }

   public boolean isUseOnlyActiveTab() {
      return this.useOnlyActiveTab;
   }

   public void setUseOnlyActiveTab(boolean var1) {
      this.useOnlyActiveTab = var1;
   }

   public void pack(ByteBufferWriter var1) {
      var1.putBoolean(this.unique);
      var1.putFloat(this.fontColor.r);
      var1.putFloat(this.fontColor.g);
      var1.putFloat(this.fontColor.b);
      var1.putFloat(this.fontColor.a);
      var1.putUTF(this.font.toString());
      var1.putBoolean(this.bold);
      var1.putBoolean(this.allowImages);
      var1.putBoolean(this.allowChatIcons);
      var1.putBoolean(this.allowColors);
      var1.putBoolean(this.allowFonts);
      var1.putBoolean(this.allowBBcode);
      var1.putBoolean(this.equalizeLineHeights);
      var1.putBoolean(this.showAuthor);
      var1.putBoolean(this.showTimestamp);
      var1.putBoolean(this.showChatTitle);
      var1.putFloat(this.range);
      var1.putBoolean(this.range != this.zombieAttractionRange);
      if (this.range != this.zombieAttractionRange) {
         var1.putFloat(this.zombieAttractionRange);
      }

   }

   public static enum FontSize {
      NotDefine,
      Small,
      Medium,
      Large;

      private FontSize() {
      }
   }
}
