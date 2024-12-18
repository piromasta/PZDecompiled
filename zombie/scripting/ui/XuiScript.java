package zombie.scripting.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.ui.UIFont;

public class XuiScript {
   private static final String xui_prefix = "xui_";
   protected HashMap<String, XuiVar> varsMap;
   protected ArrayList<XuiVar> vars;
   protected final ArrayList<XuiScript> children;
   protected XuiSkin xuiSkin;
   protected final boolean readAltKeys;
   protected final XuiScriptType scriptType;
   protected final String xuiLayoutName;
   private XuiScript defaultStyle;
   private XuiScript style;
   public final String xuiUUID;
   public final XuiString xuiKey;
   public final XuiString xuiLuaClass;
   public final XuiString xuiStyle;
   public final XuiString xuiCustomDebug;
   public final XuiUnit x;
   public final XuiUnit y;
   public final XuiUnit width;
   public final XuiUnit height;
   public final XuiVector vector;
   public final XuiVectorPosAlign posAlign;
   public final XuiFloat minimumWidth;
   public final XuiFloat minimumHeight;
   public final XuiFloat maximumWidth;
   public final XuiFloat maximumHeight;
   public final XuiUnit paddingTop;
   public final XuiUnit paddingRight;
   public final XuiUnit paddingBottom;
   public final XuiUnit paddingLeft;
   public final XuiSpacing padding;
   public final XuiUnit marginTop;
   public final XuiUnit marginRight;
   public final XuiUnit marginBottom;
   public final XuiUnit marginLeft;
   public final XuiSpacing margin;
   public final XuiTranslateString title;
   public final XuiTranslateString name;
   public final XuiFontType font;
   public final XuiFontType font2;
   public final XuiFontType font3;
   public final XuiTexture icon;
   public final XuiUnit icon_x;
   public final XuiUnit icon_y;
   public final XuiUnit icon_width;
   public final XuiUnit icon_height;
   public final XuiVector icon_vector;
   public final XuiTexture image;
   public final XuiUnit image_x;
   public final XuiUnit image_y;
   public final XuiUnit image_width;
   public final XuiUnit image_height;
   public final XuiVector image_vector;
   public final XuiBoolean anchorLeft;
   public final XuiBoolean anchorRight;
   public final XuiBoolean anchorTop;
   public final XuiBoolean anchorBottom;
   public final XuiStringList animationList;
   public final XuiFloat animationTime;
   public final XuiTexture textureBackground;
   public final XuiTexture texture;
   public final XuiTexture textureOverride;
   public final XuiTexture tickTexture;
   public final XuiColor textColor;
   public final XuiColor backgroundColor;
   public final XuiColor backgroundColorMouseOver;
   public final XuiColor borderColor;
   public final XuiColor textureColor;
   public final XuiColor choicesColor;
   public final XuiColor gridColor;
   public final XuiBoolean displayBackground;
   public final XuiBoolean background;
   public final XuiBoolean drawGrid;
   public final XuiBoolean drawBackground;
   public final XuiBoolean drawBorder;
   public final XuiTranslateString tooltip;
   public final XuiColor hsbFactor;
   public final XuiBoolean moveWithMouse;
   public final XuiBoolean mouseOver;
   public final XuiTranslateString mouseOverText;
   public final XuiTextAlign textAlign;
   public final XuiBoolean doHighlight;
   public final XuiColor backgroundColorHL;
   public final XuiColor borderColorHL;
   public final XuiBoolean doValidHighlight;
   public final XuiColor backgroundColorHLVal;
   public final XuiColor borderColorHLVal;
   public final XuiBoolean doInvalidHighlight;
   public final XuiColor backgroundColorHLInv;
   public final XuiColor borderColorHLInv;
   public final XuiBoolean storeItem;
   public final XuiBoolean doBackDropTex;
   public final XuiColor backDropTexCol;
   public final XuiBoolean doToolTip;
   public final XuiBoolean mouseEnabled;
   public final XuiBoolean allowDropAlways;
   public final XuiTranslateString toolTipTextItem;
   public final XuiTranslateString toolTipTextLocked;
   public final XuiColor backgroundEmpty;
   public final XuiColor backgroundHover;
   public final XuiColor borderInput;
   public final XuiColor borderOutput;
   public final XuiColor borderValid;
   public final XuiColor borderInvalid;
   public final XuiColor borderLocked;
   public final XuiBoolean doBorderLocked;
   public final XuiBoolean pin;
   public final XuiBoolean resizable;
   public final XuiBoolean enableHeader;
   public final XuiFloat scaledWidth;
   public final XuiFloat scaledHeight;

   public XuiScript(String var1, boolean var2, String var3) {
      this(var1, var2, var3, XuiScriptType.Layout);
   }

   public XuiScript(String var1, boolean var2, String var3, XuiScriptType var4) {
      this.varsMap = new HashMap();
      this.vars = new ArrayList();
      this.children = new ArrayList();
      this.xuiLayoutName = var1;
      this.readAltKeys = var2;
      this.scriptType = var4;
      this.xuiUUID = UUID.randomUUID().toString();
      int var5 = 9000;
      this.xuiLuaClass = (XuiString)this.addVar(new XuiString(this, "xuiLuaClass", "ISUIElement"));
      if (var3 != null) {
         this.xuiLuaClass.setValue(var3);
      }

      this.xuiLuaClass.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.xuiLuaClass.setIgnoreStyling(true);
      --var5;
      var5 = this.xuiLuaClass.setUiOrder(var5);
      this.xuiKey = (XuiString)this.addVar(new XuiString(this, "xuiKey", UUID.randomUUID().toString()));
      this.xuiKey.setAutoApplyMode(XuiAutoApply.Forbidden);
      --var5;
      var5 = this.xuiKey.setUiOrder(var5);
      this.xuiStyle = (XuiString)this.addVar(new XuiString(this, "xuiStyle"));
      this.xuiStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.xuiStyle.setScriptLoadEnabled(false);
      this.xuiStyle.setIgnoreStyling(true);
      --var5;
      var5 = this.xuiStyle.setUiOrder(var5);
      this.xuiCustomDebug = (XuiString)this.addVar(new XuiString(this, "xuiCustomDebug"));
      this.xuiCustomDebug.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.xuiCustomDebug.setIgnoreStyling(true);
      --var5;
      var5 = this.xuiCustomDebug.setUiOrder(var5);
      this.x = (XuiUnit)this.addVar(new XuiUnit(this, "x", 0.0F));
      this.x.setAutoApplyMode(XuiAutoApply.Forbidden);
      --var5;
      var5 = this.x.setUiOrder(var5);
      this.y = (XuiUnit)this.addVar(new XuiUnit(this, "y", 0.0F));
      this.y.setAutoApplyMode(XuiAutoApply.Forbidden);
      --var5;
      var5 = this.y.setUiOrder(var5);
      this.width = (XuiUnit)this.addVar(new XuiUnit(this, "width", 0.0F));
      this.width.setAutoApplyMode(XuiAutoApply.No);
      --var5;
      var5 = this.width.setUiOrder(var5);
      this.height = (XuiUnit)this.addVar(new XuiUnit(this, "height", 0.0F));
      this.height.setAutoApplyMode(XuiAutoApply.No);
      --var5;
      var5 = this.height.setUiOrder(var5);
      this.vector = (XuiVector)this.addVar(new XuiVector(this, "vector", this.x, this.y, this.width, this.height));
      --var5;
      var5 = this.vector.setUiOrder(var5);
      this.posAlign = (XuiVectorPosAlign)this.addVar(new XuiVectorPosAlign(this, "vectorPosAlign", VectorPosAlign.TopLeft));
      this.posAlign.setAutoApplyMode(XuiAutoApply.Always);
      --var5;
      this.posAlign.setUiOrder(var5);
      this.textAlign = (XuiTextAlign)this.addVar(new XuiTextAlign(this, "textAlign"));
      this.textAlign.setAutoApplyMode(XuiAutoApply.No);
      this.minimumWidth = (XuiFloat)this.addVar(new XuiFloat(this, "minimumWidth"));
      this.minimumHeight = (XuiFloat)this.addVar(new XuiFloat(this, "minimumHeight"));
      this.maximumWidth = (XuiFloat)this.addVar(new XuiFloat(this, "maximumWidth"));
      this.maximumHeight = (XuiFloat)this.addVar(new XuiFloat(this, "maximumHeight"));
      this.scaledWidth = (XuiFloat)this.addVar(new XuiFloat(this, "scaledWidth"));
      this.scaledHeight = (XuiFloat)this.addVar(new XuiFloat(this, "scaledHeight"));
      this.addVar(new XuiUnit(this, "maximumHeightPercent", -1.0F));
      this.paddingTop = (XuiUnit)this.addVar(new XuiUnit(this, "paddingTop", 0.0F));
      this.paddingRight = (XuiUnit)this.addVar(new XuiUnit(this, "paddingRight", 0.0F));
      this.paddingBottom = (XuiUnit)this.addVar(new XuiUnit(this, "paddingBottom", 0.0F));
      this.paddingLeft = (XuiUnit)this.addVar(new XuiUnit(this, "paddingLeft", 0.0F));
      this.padding = (XuiSpacing)this.addVar(new XuiSpacing(this, "padding", this.paddingTop, this.paddingRight, this.paddingBottom, this.paddingLeft));
      this.marginTop = (XuiUnit)this.addVar(new XuiUnit(this, "marginTop", 0.0F));
      this.marginRight = (XuiUnit)this.addVar(new XuiUnit(this, "marginRight", 0.0F));
      this.marginBottom = (XuiUnit)this.addVar(new XuiUnit(this, "marginBottom", 0.0F));
      this.marginLeft = (XuiUnit)this.addVar(new XuiUnit(this, "marginLeft", 0.0F));
      this.margin = (XuiSpacing)this.addVar(new XuiSpacing(this, "margin", this.marginTop, this.marginRight, this.marginBottom, this.marginLeft));
      this.icon = (XuiTexture)this.addVar(new XuiTexture(this, "icon"));
      this.icon_x = (XuiUnit)this.addVar(new XuiUnit(this, "icon_x", 0.0F));
      this.icon_x.setAutoApplyMode(XuiAutoApply.No);
      this.icon_y = (XuiUnit)this.addVar(new XuiUnit(this, "icon_y", 0.0F));
      this.icon_y.setAutoApplyMode(XuiAutoApply.No);
      this.icon_width = (XuiUnit)this.addVar(new XuiUnit(this, "icon_width", 0.0F));
      this.icon_width.setAutoApplyMode(XuiAutoApply.No);
      this.icon_height = (XuiUnit)this.addVar(new XuiUnit(this, "icon_height", 0.0F));
      this.icon_height.setAutoApplyMode(XuiAutoApply.No);
      this.icon_vector = (XuiVector)this.addVar(new XuiVector(this, "icon_vector", this.icon_x, this.icon_y, this.icon_width, this.icon_height));
      this.image = (XuiTexture)this.addVar(new XuiTexture(this, "image"));
      this.image_x = (XuiUnit)this.addVar(new XuiUnit(this, "image_x", 0.0F));
      this.image_x.setAutoApplyMode(XuiAutoApply.No);
      this.image_y = (XuiUnit)this.addVar(new XuiUnit(this, "image_y", 0.0F));
      this.image_y.setAutoApplyMode(XuiAutoApply.No);
      this.image_width = (XuiUnit)this.addVar(new XuiUnit(this, "image_width", 0.0F));
      this.image_width.setAutoApplyMode(XuiAutoApply.No);
      this.image_height = (XuiUnit)this.addVar(new XuiUnit(this, "image_height", 0.0F));
      this.image_height.setAutoApplyMode(XuiAutoApply.No);
      this.image_vector = (XuiVector)this.addVar(new XuiVector(this, "image_vector", this.image_x, this.image_y, this.image_width, this.image_height));
      this.anchorLeft = (XuiBoolean)this.addVar(new XuiBoolean(this, "anchorLeft"));
      this.anchorRight = (XuiBoolean)this.addVar(new XuiBoolean(this, "anchorRight"));
      this.anchorTop = (XuiBoolean)this.addVar(new XuiBoolean(this, "anchorTop"));
      this.anchorBottom = (XuiBoolean)this.addVar(new XuiBoolean(this, "anchorBottom"));
      this.animationList = (XuiStringList)this.addVar(new XuiStringList(this, "animationList"));
      this.addVar(new XuiFloat(this, "r", 1.0F));
      this.addVar(new XuiFloat(this, "g", 1.0F));
      this.addVar(new XuiFloat(this, "b", 1.0F));
      this.addVar(new XuiFloat(this, "a", 1.0F));
      this.addVar(new XuiFloat(this, "textR", 1.0F));
      this.addVar(new XuiFloat(this, "textG", 1.0F));
      this.addVar(new XuiFloat(this, "textB", 1.0F));
      this.animationTime = (XuiFloat)this.addVar(new XuiFloat(this, "animationTime", 1.0F));
      this.addVar(new XuiFloat(this, "boxSize", 16.0F));
      this.addVar(new XuiFloat(this, "bubblesAlpha", 1.0F));
      this.addVar(new XuiFloat(this, "contentTransparency", 1.0F));
      this.addVar(new XuiFloat(this, "currentValue", 1.0F));
      this.addVar(new XuiFloat(this, "differenceAlpha", 1.0F));
      this.addVar(new XuiFloat(this, "gradientAlpha", 1.0F));
      this.addVar(new XuiFloat(this, "itemGap", 4.0F));
      this.addVar(new XuiFloat(this, "itemheight", 30.0F));
      this.addVar(new XuiFloat(this, "itemHgt", 30.0F));
      this.addVar(new XuiFloat(this, "itemPadY", 10.0F));
      this.addVar(new XuiFloat(this, "ledBlinkSpeed", 0.0F));
      this.addVar(new XuiFloat(this, "leftMargin", 0.0F));
      this.addVar(new XuiFloat(this, "minValue", 0.0F));
      this.addVar(new XuiInteger(this, "maxLength", 1));
      this.addVar(new XuiInteger(this, "maxLines", 1));
      this.addVar(new XuiFloat(this, "maxValue", 0.0F));
      this.addVar(new XuiFloat(this, "scrollX", 0.0F));
      this.addVar(new XuiFloat(this, "shiftValue", 0.0F));
      this.addVar(new XuiFloat(this, "stepValue", 0.0F));
      this.addVar(new XuiFloat(this, "tabHeight", 0.0F));
      this.addVar(new XuiFloat(this, "tabPadX", 20.0F));
      this.addVar(new XuiFloat(this, "tabTransparency", 1.0F));
      this.addVar(new XuiFloat(this, "textTransparency", 1.0F));
      this.addVar(new XuiFloat(this, "textGap", 4.0F));
      this.addVar(new XuiFloat(this, "triangleWidth", 1.0F));
      this.addVar(new XuiFontType(this, "defaultFont", UIFont.NewSmall));
      this.font = (XuiFontType)this.addVar(new XuiFontType(this, "font", UIFont.Small));
      this.font2 = (XuiFontType)this.addVar(new XuiFontType(this, "font2", UIFont.Small));
      this.font3 = (XuiFontType)this.addVar(new XuiFontType(this, "font3", UIFont.Small));
      this.addVar(new XuiFontType(this, "titleFont", UIFont.Small));
      this.addVar(new XuiTexture(this, "bubblesTex"));
      this.addVar(new XuiTexture(this, "closeButtonTexture"));
      this.addVar(new XuiTexture(this, "collapseButtonTexture"));
      this.addVar(new XuiTexture(this, "gradientTex"));
      this.addVar(new XuiTexture(this, "infoBtn"));
      this.addVar(new XuiTexture(this, "invbasic"));
      this.addVar(new XuiTexture(this, "lcdback"));
      this.addVar(new XuiTexture(this, "lcdfont"));
      this.addVar(new XuiTexture(this, "ledBackTexture"));
      this.addVar(new XuiTexture(this, "ledTexture"));
      this.addVar(new XuiTexture(this, "resizeimage"));
      this.addVar(new XuiTexture(this, "pinButtonTexture"));
      this.addVar(new XuiTexture(this, "progressTexture"));
      this.addVar(new XuiTexture(this, "statusbarbkg"));
      this.texture = (XuiTexture)this.addVar(new XuiTexture(this, "texture"));
      this.textureBackground = (XuiTexture)this.addVar(new XuiTexture(this, "textureBackground"));
      this.addVar(new XuiTexture(this, "texBtnLeft"));
      this.addVar(new XuiTexture(this, "texBtnRight"));
      this.textureOverride = (XuiTexture)this.addVar(new XuiTexture(this, "textureOverride"));
      this.tickTexture = (XuiTexture)this.addVar(new XuiTexture(this, "tickTexture"));
      this.addVar(new XuiTexture(this, "titlebarbkg"));
      this.addVar(new XuiColor(this, "altBgColor"));
      this.backDropTexCol = (XuiColor)this.addVar(new XuiColor(this, "backDropTexCol"));
      this.backgroundColor = (XuiColor)this.addVar(new XuiColor(this, "backgroundColor"));
      this.backgroundColorHL = (XuiColor)this.addVar(new XuiColor(this, "backgroundColorHL"));
      this.backgroundColorHLInv = (XuiColor)this.addVar(new XuiColor(this, "backgroundColorHLInv"));
      this.backgroundColorHLVal = (XuiColor)this.addVar(new XuiColor(this, "backgroundColorHLVal"));
      this.backgroundColorMouseOver = (XuiColor)this.addVar(new XuiColor(this, "backgroundColorMouseOver"));
      this.backgroundEmpty = (XuiColor)this.addVar(new XuiColor(this, "backgroundEmpty"));
      this.backgroundHover = (XuiColor)this.addVar(new XuiColor(this, "backgroundHover"));
      this.borderColor = (XuiColor)this.addVar(new XuiColor(this, "borderColor"));
      this.borderColorHL = (XuiColor)this.addVar(new XuiColor(this, "borderColorHL"));
      this.borderColorHLInv = (XuiColor)this.addVar(new XuiColor(this, "borderColorHLInv "));
      this.borderColorHLVal = (XuiColor)this.addVar(new XuiColor(this, "borderColorHLVal"));
      this.borderInput = (XuiColor)this.addVar(new XuiColor(this, "borderInput"));
      this.borderInvalid = (XuiColor)this.addVar(new XuiColor(this, "borderInvalid"));
      this.borderLocked = (XuiColor)this.addVar(new XuiColor(this, "borderLocked"));
      this.borderOutput = (XuiColor)this.addVar(new XuiColor(this, "borderOutput"));
      this.borderValid = (XuiColor)this.addVar(new XuiColor(this, "borderValid"));
      this.addVar(new XuiColor(this, "buttonColor"));
      this.addVar(new XuiColor(this, "buttonMouseOverColor"));
      this.choicesColor = (XuiColor)this.addVar(new XuiColor(this, "choicesColor"));
      this.addVar(new XuiColor(this, "detailInnerColor"));
      this.addVar(new XuiColor(this, "greyCol"));
      this.gridColor = (XuiColor)this.addVar(new XuiColor(this, "gridColor"));
      this.hsbFactor = (XuiColor)this.addVar(new XuiColor(this, "hsbFactor"));
      this.hsbFactor.setAutoApplyMode(XuiAutoApply.No);
      this.addVar(new XuiColor(this, "ledCol"));
      this.addVar(new XuiColor(this, "ledColor"));
      this.addVar(new XuiColor(this, "ledColOff"));
      this.addVar(new XuiColor(this, "ledTextColor"));
      this.addVar(new XuiColor(this, "listHeaderColor"));
      this.addVar(new XuiColor(this, "progressColor"));
      this.addVar(new XuiColor(this, "sliderColor"));
      this.addVar(new XuiColor(this, "sliderBarBorderColor"));
      this.addVar(new XuiColor(this, "sliderBarColor"));
      this.addVar(new XuiColor(this, "sliderBorderColor"));
      this.addVar(new XuiColor(this, "sliderMouseOverColor"));
      this.textColor = (XuiColor)this.addVar(new XuiColor(this, "textColor"));
      this.addVar(new XuiColor(this, "textBackColor"));
      this.textureColor = (XuiColor)this.addVar(new XuiColor(this, "textureColor"));
      this.addVar(new XuiColor(this, "widgetTextureColor"));
      this.addVar(new XuiBoolean(this, "allowDraggingTabs"));
      this.allowDropAlways = (XuiBoolean)this.addVar(new XuiBoolean(this, "allowDropAlways"));
      this.addVar(new XuiBoolean(this, "allowTornOffTabs"));
      this.addVar(new XuiBoolean(this, "autoScale"));
      this.addVar(new XuiBoolean(this, "autosetheight"));
      this.background = (XuiBoolean)this.addVar(new XuiBoolean(this, "background"));
      this.addVar(new XuiBoolean(this, "center"));
      this.addVar(new XuiBoolean(this, "centerTabs"));
      this.addVar(new XuiBoolean(this, "clearStentil"));
      this.addVar(new XuiBoolean(this, "clip"));
      this.displayBackground = (XuiBoolean)this.addVar(new XuiBoolean(this, "displayBackground"));
      this.doBackDropTex = (XuiBoolean)this.addVar(new XuiBoolean(this, "doBackDropTex"));
      this.doBorderLocked = (XuiBoolean)this.addVar(new XuiBoolean(this, "doBorderLocked"));
      this.addVar(new XuiBoolean(this, "doButtons"));
      this.doHighlight = (XuiBoolean)this.addVar(new XuiBoolean(this, "doHighlight"));
      this.doInvalidHighlight = (XuiBoolean)this.addVar(new XuiBoolean(this, "doInvalidHighlight"));
      this.addVar(new XuiBoolean(this, "doLedBlink"));
      this.addVar(new XuiBoolean(this, "doScroll"));
      this.addVar(new XuiBoolean(this, "doTextBackdrop"));
      this.doToolTip = (XuiBoolean)this.addVar(new XuiBoolean(this, "doToolTip"));
      this.doValidHighlight = (XuiBoolean)this.addVar(new XuiBoolean(this, "doValidHighlight"));
      this.addVar(new XuiBoolean(this, "dragInside"));
      this.addVar(new XuiBoolean(this, "drawFrame"));
      this.drawGrid = (XuiBoolean)this.addVar(new XuiBoolean(this, "drawGrid"));
      this.drawBackground = (XuiBoolean)this.addVar(new XuiBoolean(this, "drawBackground"));
      this.drawBorder = (XuiBoolean)this.addVar(new XuiBoolean(this, "drawBorder"));
      this.addVar(new XuiBoolean(this, "drawMeasures"));
      this.addVar(new XuiBoolean(this, "editable"));
      this.addVar(new XuiBoolean(this, "enable"));
      this.enableHeader = (XuiBoolean)this.addVar(new XuiBoolean(this, "enableHeader"));
      this.addVar(new XuiBoolean(this, "equalTabWidth"));
      this.addVar(new XuiBoolean(this, "keeplog"));
      this.addVar(new XuiBoolean(this, "isOn", false));
      this.addVar(new XuiBoolean(this, "isVertical", false));
      this.addVar(new XuiBoolean(this, "ledIsOn", false));
      this.addVar(new XuiBoolean(this, "left", false));
      this.moveWithMouse = (XuiBoolean)this.addVar(new XuiBoolean(this, "moveWithMouse", false));
      this.mouseEnabled = (XuiBoolean)this.addVar(new XuiBoolean(this, "mouseEnabled"));
      this.mouseOver = (XuiBoolean)this.addVar(new XuiBoolean(this, "mouseover", false));
      this.pin = (XuiBoolean)this.addVar(new XuiBoolean(this, "pin"));
      this.resizable = (XuiBoolean)this.addVar(new XuiBoolean(this, "resizable"));
      this.storeItem = (XuiBoolean)this.addVar(new XuiBoolean(this, "storeItem"));
      this.addVar(new XuiTranslateString(this, "description"));
      this.addVar(new XuiTranslateString(this, "footNote"));
      this.mouseOverText = (XuiTranslateString)this.addVar(new XuiTranslateString(this, "mouseovertext"));
      this.name = (XuiTranslateString)this.addVar(new XuiTranslateString(this, "name", "NAME_NOT_SET"));
      this.title = (XuiTranslateString)this.addVar(new XuiTranslateString(this, "title", "TITLE_NOT_SET"));
      this.tooltip = (XuiTranslateString)this.addVar(new XuiTranslateString(this, "tooltip"));
      this.addVar(new XuiTranslateString(this, "toolTipText"));
      this.toolTipTextItem = (XuiTranslateString)this.addVar(new XuiTranslateString(this, "toolTipTextItem"));
      this.toolTipTextLocked = (XuiTranslateString)this.addVar(new XuiTranslateString(this, "toolTipTextLocked"));
      this.addVar(new XuiTranslateString(this, "translation"));
   }

   public String getXuiUUID() {
      return this.xuiUUID;
   }

   public String getXuiKey() {
      return (String)this.xuiKey.value();
   }

   public XuiScript setXuiKey(String var1) {
      this.xuiKey.setValue(var1);
      return this;
   }

   public String getXuiLuaClass() {
      return (String)this.xuiLuaClass.value();
   }

   public XuiScript setXuiLuaClass(String var1) {
      this.xuiLuaClass.setValue(var1);
      return this;
   }

   public String getXuiStyle() {
      return (String)this.xuiStyle.value();
   }

   public XuiScript setXuiStyle(String var1) {
      this.xuiStyle.setValue(var1);
      return this;
   }

   public String getXuiCustomDebug() {
      return (String)this.xuiCustomDebug.value();
   }

   public XuiVector getVector() {
      return this.vector;
   }

   public XuiSpacing getPadding() {
      return this.padding;
   }

   public XuiSpacing getMargin() {
      return this.margin;
   }

   public XuiVectorPosAlign getPosAlign() {
      return this.posAlign;
   }

   public XuiFloat getMinimumWidth() {
      return this.minimumWidth;
   }

   public XuiFloat getMinimumHeight() {
      return this.minimumHeight;
   }

   public XuiTranslateString getTitle() {
      return this.title;
   }

   public XuiTranslateString getName() {
      return this.name;
   }

   public XuiFontType getFont() {
      return this.font;
   }

   public XuiFontType getFont2() {
      return this.font2;
   }

   public XuiFontType getFont3() {
      return this.font3;
   }

   public XuiTexture getIcon() {
      return this.icon;
   }

   public XuiVector getIconVector() {
      return this.icon_vector;
   }

   public XuiBoolean getAnchorLeft() {
      return this.anchorLeft;
   }

   public XuiBoolean getAnchorRight() {
      return this.anchorRight;
   }

   public XuiBoolean getAnchorTop() {
      return this.anchorTop;
   }

   public XuiBoolean getAnchorBottom() {
      return this.anchorBottom;
   }

   public XuiStringList getAnimationList() {
      return this.animationList;
   }

   public XuiFloat getAnimationTime() {
      return this.animationTime;
   }

   public XuiTexture getTextureBackground() {
      return this.textureBackground;
   }

   public XuiTexture getTexture() {
      return this.texture;
   }

   public XuiTexture getTextureOverride() {
      return this.textureOverride;
   }

   public XuiTexture getTickTexture() {
      return this.tickTexture;
   }

   public XuiColor getTextColor() {
      return this.textColor;
   }

   public XuiColor getBackgroundColor() {
      return this.backgroundColor;
   }

   public XuiColor getBackgroundColorMouseOver() {
      return this.backgroundColorMouseOver;
   }

   public XuiColor getBorderColor() {
      return this.borderColor;
   }

   public XuiColor getTextureColor() {
      return this.textureColor;
   }

   public XuiColor getChoicesColor() {
      return this.choicesColor;
   }

   public XuiColor getGridColor() {
      return this.gridColor;
   }

   public XuiBoolean getDisplayBackground() {
      return this.displayBackground;
   }

   public XuiBoolean getBackground() {
      return this.background;
   }

   public XuiBoolean getDrawGrid() {
      return this.drawGrid;
   }

   public XuiBoolean getDrawBackground() {
      return this.drawBackground;
   }

   public XuiBoolean getDrawBorder() {
      return this.drawBorder;
   }

   public XuiTranslateString getTooltip() {
      return this.tooltip;
   }

   public XuiTranslateString getMouseOverText() {
      return this.mouseOverText;
   }

   public XuiColor getHsbFactor() {
      return this.hsbFactor;
   }

   public XuiBoolean getMoveWithMouse() {
      return this.moveWithMouse;
   }

   public XuiTextAlign getTextAlign() {
      return this.textAlign;
   }

   public XuiBoolean getDoHighlight() {
      return this.doHighlight;
   }

   public XuiColor getBackgroundColorHL() {
      return this.backgroundColorHL;
   }

   public XuiColor getBorderColorHL() {
      return this.borderColorHL;
   }

   public XuiBoolean getDoValidHighlight() {
      return this.doValidHighlight;
   }

   public XuiColor getBackgroundColorHLVal() {
      return this.backgroundColorHLVal;
   }

   public XuiColor getBorderColorHLVal() {
      return this.borderColorHLVal;
   }

   public XuiBoolean getDoInvalidHighlight() {
      return this.doInvalidHighlight;
   }

   public XuiColor getBackgroundColorHLInv() {
      return this.backgroundColorHLInv;
   }

   public XuiColor getBorderColorHLInv() {
      return this.borderColorHLInv;
   }

   public XuiBoolean getStoreItem() {
      return this.storeItem;
   }

   public XuiBoolean getDoBackDropTex() {
      return this.doBackDropTex;
   }

   public XuiColor getBackDropTexCol() {
      return this.backDropTexCol;
   }

   public XuiBoolean getDoToolTip() {
      return this.doToolTip;
   }

   public XuiBoolean getMouseEnabled() {
      return this.mouseEnabled;
   }

   public XuiBoolean getAllowDropAlways() {
      return this.allowDropAlways;
   }

   public XuiTranslateString getToolTipTextItem() {
      return this.toolTipTextItem;
   }

   public XuiTranslateString getToolTipTextLocked() {
      return this.toolTipTextLocked;
   }

   public XuiColor getBackgroundEmpty() {
      return this.backgroundEmpty;
   }

   public XuiColor getBackgroundHover() {
      return this.backgroundHover;
   }

   public XuiColor getBorderInput() {
      return this.borderInput;
   }

   public XuiColor getBorderOutput() {
      return this.borderOutput;
   }

   public XuiColor getBorderValid() {
      return this.borderValid;
   }

   public XuiColor getBorderInvalid() {
      return this.borderInvalid;
   }

   public XuiColor getBorderLocked() {
      return this.borderLocked;
   }

   public XuiBoolean getDoBorderLocked() {
      return this.doBorderLocked;
   }

   public String getXuiLayoutName() {
      return this.xuiLayoutName != null ? this.xuiLayoutName : "null";
   }

   public String toString() {
      String var1 = super.toString();
      String var2 = this.getXuiLayoutName();
      String var3 = this.scriptType != null ? this.scriptType.toString() : "null";
      String var4 = this.xuiLuaClass != null && this.xuiLuaClass.value() != null ? (String)this.xuiLuaClass.value() : "null";
      String var5 = this.xuiKey != null && this.xuiKey.value() != null ? (String)this.xuiKey.value() : "null";
      return "XuiScript [config=" + var2 + ", type=" + var3 + ", class=" + var4 + ", key=" + var5 + ", u=" + var1 + "]";
   }

   protected void logWithInfo(String var1) {
      DebugLog.General.debugln(var1);
      this.logInfo();
   }

   protected void warnWithInfo(String var1) {
      DebugLog.General.debugln(var1);
      this.logInfo();
   }

   protected void errorWithInfo(String var1) {
      DebugLog.General.error(var1);
      this.logInfo();
   }

   private void logInfo() {
      DebugLog.log(this.toString());
   }

   public XuiScript getStyle() {
      return this.style;
   }

   public void setStyle(XuiScript var1) {
      if (var1 != null && !var1.isStyle()) {
         this.errorWithInfo("XuiScript is not a style.");
         DebugLog.log("StyleScript = " + var1);
      } else if (var1 == null || this.style != var1) {
         this.style = var1;
         Iterator var2 = this.vars.iterator();

         while(var2.hasNext()) {
            XuiVar var3 = (XuiVar)var2.next();
            if (var1 != null) {
               var3.style = var1.getVar(var3.getScriptKey());
            } else {
               var3.style = null;
            }
         }

      }
   }

   public XuiScript getDefaultStyle() {
      return this.defaultStyle;
   }

   public void setDefaultStyle(XuiScript var1) {
      if (var1 != null && !var1.isDefaultStyle()) {
         this.errorWithInfo("XuiScript is not style.");
         DebugLog.log("StyleScript = " + var1);
      } else if (var1 == null || this.defaultStyle != var1) {
         this.defaultStyle = var1;
         Iterator var2 = this.vars.iterator();

         while(var2.hasNext()) {
            XuiVar var3 = (XuiVar)var2.next();
            if (var1 != null) {
               var3.defaultStyle = var1.getVar(var3.getScriptKey());
            } else {
               var3.defaultStyle = null;
            }
         }

      }
   }

   public boolean isLayout() {
      return this.scriptType == XuiScriptType.Layout;
   }

   public boolean isAnyStyle() {
      return this.scriptType == XuiScriptType.Style || this.scriptType == XuiScriptType.DefaultStyle;
   }

   public boolean isStyle() {
      return this.scriptType == XuiScriptType.Style;
   }

   public boolean isDefaultStyle() {
      return this.scriptType == XuiScriptType.DefaultStyle;
   }

   public XuiScriptType getScriptType() {
      return this.scriptType;
   }

   protected <T extends XuiVar> T addVar(T var1) {
      if (this.varsMap.containsKey(var1.getScriptKey())) {
         this.logInfo();
         throw new RuntimeException("Double script key");
      } else {
         this.vars.add(var1);
         this.varsMap.put(var1.getScriptKey(), var1);
         return var1;
      }
   }

   public XuiVar getVar(String var1) {
      return (XuiVar)this.varsMap.get(var1);
   }

   public ArrayList<XuiVar> getVars() {
      return this.vars;
   }

   public void addChild(XuiScript var1) {
      this.children.add(var1);
   }

   public ArrayList<XuiScript> getChildren() {
      return this.children;
   }

   public static String ReadLuaClassValue(ScriptParser.Block var0) {
      Iterator var1 = var0.values.iterator();

      String var3;
      String var4;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         ScriptParser.Value var2 = (ScriptParser.Value)var1.next();
         var3 = var2.getKey().trim();
         var4 = var2.getValue().trim();
      } while(var3.isEmpty() || var4.isEmpty() || !var3.equalsIgnoreCase("xuiLuaClass"));

      return var4;
   }

   public static XuiScript CreateScriptForClass(String var0, String var1, boolean var2, XuiScriptType var3) {
      if (var1 != null) {
         switch (var1) {
            case "ISXuiTableLayout":
               return new XuiTableScript(var0, var2, var3);
            case "Reference":
               return new XuiReference(var0, var2);
            default:
               return new XuiScript(var0, var2, var1, var3);
         }
      } else {
         return new XuiScript(var0, var2, var1, var3);
      }
   }

   public void Load(ScriptParser.Block var1) {
      Iterator var2;
      ScriptParser.Value var3;
      String var4;
      String var5;
      if (this.isLayout()) {
         var2 = var1.values.iterator();

         while(var2.hasNext()) {
            var3 = (ScriptParser.Value)var2.next();
            var4 = var3.getKey().trim();
            var5 = var3.getValue().trim();
            if (!var4.isEmpty() && !var5.isEmpty()) {
               if (this.xuiStyle.acceptsKey(var4)) {
                  this.xuiStyle.fromString(var5);
               } else if (this.xuiLuaClass.acceptsKey(var4)) {
                  if (!this.xuiLuaClass.isValueSet()) {
                     this.xuiLuaClass.fromString(var5);
                  } else {
                     this.warnWithInfo("LuaClass defined in script but already set in constructor, class: " + (String)this.xuiLuaClass.value());
                  }
               }
            }
         }

         XuiScript var6 = XuiManager.GetStyle((String)this.xuiStyle.value());
         if (var6 != null) {
            this.setStyle(var6);
         }

         this.tryToSetDefaultStyle();
      }

      var2 = var1.values.iterator();

      while(var2.hasNext()) {
         var3 = (ScriptParser.Value)var2.next();
         var4 = var3.getKey().trim();
         var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            this.loadVar(var4, var5);
         }
      }

      var2 = var1.children.iterator();

      while(var2.hasNext()) {
         ScriptParser.Block var7 = (ScriptParser.Block)var2.next();
         if (this.isLayout() && var7.type.equalsIgnoreCase("xui")) {
            XuiScript var8 = CreateScriptForClass(this.xuiLayoutName, var7.id, this.readAltKeys, this.scriptType);
            var8.Load(var7);
            this.children.add(var8);
         }
      }

      this.postLoad();
   }

   public boolean loadVar(String var1, String var2) {
      return this.loadVar(var1, var2, true);
   }

   public boolean loadVar(String var1, String var2, boolean var3) {
      for(int var4 = 0; var4 < this.vars.size(); ++var4) {
         if (((XuiVar)this.vars.get(var4)).isScriptLoadEnabled()) {
            if (var2 != null && ((XuiVar)this.vars.get(var4)).acceptsKey(var1)) {
               return ((XuiVar)this.vars.get(var4)).load(var1, var2);
            }

            if (var2 == null && var3 && ((XuiVar)this.vars.get(var4)).acceptsKey(var1)) {
               ((XuiVar)this.vars.get(var4)).setValue((Object)null);
               return true;
            }
         }
      }

      return false;
   }

   protected void tryToSetDefaultStyle() {
      if (this.isLayout() && this.xuiLuaClass.value() != null) {
         XuiScript var1 = XuiManager.GetDefaultStyle((String)this.xuiLuaClass.value());
         if (var1 != null) {
            this.setDefaultStyle(var1);
         }
      }

   }

   protected void postLoad() {
      if (this.xuiLuaClass.value() != null) {
         if (this.backgroundColor.valueSet) {
            if ((((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISPanel") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISCollapsableWindow") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISCollapsableWindowJoypad")) && !this.background.valueSet) {
               this.background.setValue(true);
            }

            if ((((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayout") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutCell") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutRow") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutColumn")) && !this.drawBackground.valueSet) {
               this.drawBackground.setValue(true);
            }
         }

         if (this.borderColor.valueSet) {
            if (((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISPanel") && !this.background.valueSet) {
               this.background.setValue(true);
            }

            if ((((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayout") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutCell") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutRow") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutColumn")) && !this.drawBorder.valueSet) {
               this.drawBorder.setValue(true);
            }
         }
      }

   }

   public static class XuiString extends XuiVar<String, XuiString> {
      protected XuiString(XuiScript var1, String var2) {
         super(XuiVarType.String, var1, var2);
      }

      protected XuiString(XuiScript var1, String var2, String var3) {
         super(XuiVarType.String, var1, var2, var3);
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }
   }

   public abstract static class XuiVar<T, C extends XuiVar<?, ?>> {
      private int uiOrder;
      protected final XuiVarType type;
      protected final XuiScript parent;
      protected XuiVar<T, C> style;
      protected XuiVar<T, C> defaultStyle;
      protected boolean valueSet;
      private boolean scriptLoadEnabled;
      protected XuiAutoApply autoApply;
      protected T defaultValue;
      protected T value;
      protected final String luaTableKey;
      private boolean ignoreStyling;

      protected XuiVar(XuiVarType var1, XuiScript var2, String var3) {
         this(var1, var2, var3, (Object)null);
      }

      protected XuiVar(XuiVarType var1, XuiScript var2, String var3, T var4) {
         this.uiOrder = 1000;
         this.valueSet = false;
         this.scriptLoadEnabled = true;
         this.autoApply = XuiAutoApply.IfSet;
         this.ignoreStyling = false;
         this.type = (XuiVarType)Objects.requireNonNull(var1);
         this.parent = (XuiScript)Objects.requireNonNull(var2);
         this.luaTableKey = (String)Objects.requireNonNull(var3);
         this.defaultValue = var4;
      }

      public XuiVarType getType() {
         return this.type;
      }

      public int setUiOrder(int var1) {
         this.uiOrder = var1;
         return this.uiOrder;
      }

      public int getUiOrder() {
         return this.uiOrder;
      }

      public XuiVar<T, C> getStyle() {
         return this.style;
      }

      public XuiVar<T, C> getDefaultStyle() {
         return this.defaultStyle;
      }

      public boolean isStyle() {
         return this.parent.isAnyStyle();
      }

      public void setScriptLoadEnabled(boolean var1) {
         this.scriptLoadEnabled = var1;
      }

      public boolean isScriptLoadEnabled() {
         return this.scriptLoadEnabled;
      }

      protected void setIgnoreStyling(boolean var1) {
         this.ignoreStyling = var1;
      }

      public boolean isIgnoreStyling() {
         return this.ignoreStyling;
      }

      protected void setDefaultValue(T var1) {
         this.defaultValue = var1;
      }

      protected T getDefaultValue() {
         return this.defaultValue;
      }

      public void setValue(T var1) {
         this.value = var1;
         this.valueSet = true;
      }

      public void setAutoApplyMode(XuiAutoApply var1) {
         this.autoApply = var1;
      }

      public XuiAutoApply getAutoApplyMode() {
         return this.autoApply;
      }

      public String getLuaTableKey() {
         return this.luaTableKey;
      }

      protected String getScriptKey() {
         return this.luaTableKey;
      }

      public boolean isValueSet() {
         if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
            if (this.style != null && this.style.isValueSet()) {
               return true;
            }

            if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
               return true;
            }
         }

         return this.valueSet;
      }

      public XuiScriptType getValueType() {
         if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
            if (this.style != null && this.style.isValueSet()) {
               return XuiScriptType.Style;
            }

            if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
               return XuiScriptType.DefaultStyle;
            }
         }

         return this.parent.getScriptType();
      }

      public T value() {
         if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
            if (this.style != null && this.style.isValueSet()) {
               return this.style.value();
            }

            if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
               return this.defaultStyle.value();
            }
         }

         return this.valueSet ? this.value : this.defaultValue;
      }

      public String getValueString() {
         return this.value() != null ? this.value().toString() : "null";
      }

      protected boolean acceptsKey(String var1) {
         return this.luaTableKey.equalsIgnoreCase(var1);
      }

      protected abstract void fromString(String var1);

      protected boolean load(String var1, String var2) {
         if (this.acceptsKey(var1)) {
            this.fromString(var2);
            return true;
         } else {
            return false;
         }
      }
   }

   public static class XuiUnit extends XuiVar<Float, XuiUnit> {
      protected boolean isPercent = false;

      protected XuiUnit(XuiScript var1, String var2) {
         super(XuiVarType.Unit, var1, var2, 0.0F);
      }

      protected XuiUnit(XuiScript var1, String var2, float var3) {
         super(XuiVarType.Unit, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.isPercent = this.isPercent(var1);
            this.setValue(this.getNum(var1));
         } catch (Exception var3) {
            var3.printStackTrace();
         }

      }

      public void setValue(float var1, boolean var2) {
         this.isPercent = var2;
         this.setValue(var1);
      }

      public boolean isPercent() {
         if (this.parent.isLayout() && !this.valueSet && !this.isIgnoreStyling()) {
            if (this.style != null && this.style.isValueSet()) {
               return ((XuiUnit)this.style).isPercent;
            }

            if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
               return ((XuiUnit)this.defaultStyle).isPercent;
            }
         }

         return this.isPercent;
      }

      private boolean isPercent(String var1) {
         return var1.endsWith("%");
      }

      private float getNum(String var1) {
         try {
            boolean var2 = this.isPercent(var1);
            String var3 = var2 ? var1.substring(0, var1.length() - 1) : var1;
            var3 = var3.trim();
            float var4 = Float.parseFloat(var3);
            if (var2) {
               var4 /= 100.0F;
            }

            return var4;
         } catch (Exception var5) {
            this.parent.logInfo();
            var5.printStackTrace();
            return 0.0F;
         }
      }

      public String getValueString() {
         Object var10000 = this.value() != null ? (Serializable)this.value() : "null";
         return "" + var10000 + (this.isPercent() ? "%" : "");
      }
   }

   public static class XuiVector extends XuiVar<Float, XuiVector> {
      private final XuiUnit x;
      private final XuiUnit y;
      private final XuiUnit w;
      private final XuiUnit h;

      public XuiVector(XuiScript var1, String var2, XuiUnit var3, XuiUnit var4, XuiUnit var5, XuiUnit var6) {
         super(XuiVarType.Vector, var1, var2, 0.0F);
         this.x = var3;
         this.y = var4;
         this.w = var5;
         this.h = var6;
         this.setIgnoreStyling(true);
      }

      protected void fromString(String var1) {
         throw new RuntimeException("Not implemented for UIVector!");
      }

      protected boolean load(String var1, String var2) {
         try {
            if (this.acceptsKey(var1)) {
               String[] var3 = var2.split(":");

               for(int var4 = 0; var4 < var3.length; ++var4) {
                  String var5 = var3[var4].trim();
                  switch (var4) {
                     case 0:
                        this.x.fromString(var5);
                        break;
                     case 1:
                        this.y.fromString(var5);
                        break;
                     case 2:
                        this.w.fromString(var5);
                        break;
                     case 3:
                        this.h.fromString(var5);
                  }
               }

               return true;
            }
         } catch (Exception var6) {
            this.parent.logInfo();
            var6.printStackTrace();
         }

         return false;
      }

      public float getX() {
         return (Float)this.x.value();
      }

      public float getY() {
         return (Float)this.y.value();
      }

      public float getWidth() {
         return (Float)this.w.value();
      }

      public float getHeight() {
         return (Float)this.h.value();
      }

      public float getW() {
         return (Float)this.w.value();
      }

      public float getH() {
         return (Float)this.h.value();
      }

      public boolean isxPercent() {
         return this.x.isPercent();
      }

      public boolean isyPercent() {
         return this.y.isPercent();
      }

      public boolean iswPercent() {
         return this.w.isPercent();
      }

      public boolean ishPercent() {
         return this.h.isPercent();
      }

      public boolean isValueSet() {
         return this.x.isValueSet() || this.y.isValueSet() || this.w.isValueSet() || this.h.isValueSet();
      }

      public String getValueString() {
         String var10000 = this.x.getValueString();
         return var10000 + ", " + this.y.getValueString() + ", " + this.w.getValueString() + ", " + this.h.getValueString();
      }
   }

   public static class XuiVectorPosAlign extends XuiVar<VectorPosAlign, XuiVectorPosAlign> {
      protected XuiVectorPosAlign(XuiScript var1, String var2) {
         super(XuiVarType.VectorPosAlign, var1, var2, VectorPosAlign.None);
      }

      protected XuiVectorPosAlign(XuiScript var1, String var2, VectorPosAlign var3) {
         super(XuiVarType.VectorPosAlign, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(VectorPosAlign.valueOf(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiTextAlign extends XuiVar<TextAlign, XuiTextAlign> {
      protected XuiTextAlign(XuiScript var1, String var2) {
         super(XuiVarType.TextAlign, var1, var2, TextAlign.Left);
      }

      protected XuiTextAlign(XuiScript var1, String var2, TextAlign var3) {
         super(XuiVarType.TextAlign, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(TextAlign.valueOf(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiFloat extends XuiVar<Float, XuiFloat> {
      protected XuiFloat(XuiScript var1, String var2) {
         super(XuiVarType.Float, var1, var2, 0.0F);
      }

      protected XuiFloat(XuiScript var1, String var2, float var3) {
         super(XuiVarType.Float, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(Float.parseFloat(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiSpacing extends XuiVar<Float, XuiSpacing> {
      private final XuiUnit top;
      private final XuiUnit right;
      private final XuiUnit bottom;
      private final XuiUnit left;

      public XuiSpacing(XuiScript var1, String var2, XuiUnit var3, XuiUnit var4, XuiUnit var5, XuiUnit var6) {
         super(XuiVarType.Vector, var1, var2, 0.0F);
         this.top = var3;
         this.right = var4;
         this.bottom = var5;
         this.left = var6;
         this.setIgnoreStyling(true);
         this.setAutoApplyMode(XuiAutoApply.Forbidden);
         var3.setAutoApplyMode(XuiAutoApply.No);
         var4.setAutoApplyMode(XuiAutoApply.No);
         var5.setAutoApplyMode(XuiAutoApply.No);
         var6.setAutoApplyMode(XuiAutoApply.No);
      }

      protected void fromString(String var1) {
         throw new RuntimeException("Not implemented for XuiSpacing!");
      }

      protected boolean load(String var1, String var2) {
         try {
            if (this.acceptsKey(var1)) {
               String[] var3 = var2.split(":");

               for(int var4 = 0; var4 < var3.length; ++var4) {
                  String var5 = var3[var4].trim();
                  switch (var4) {
                     case 0:
                        if (var3.length == 1) {
                           this.top.fromString(var5);
                           this.right.fromString(var5);
                           this.bottom.fromString(var5);
                           this.left.fromString(var5);
                        } else if (var3.length == 2) {
                           this.top.fromString(var5);
                           this.bottom.fromString(var5);
                        } else {
                           this.top.fromString(var5);
                        }
                        break;
                     case 1:
                        if (var3.length != 2 && var3.length != 3) {
                           this.right.fromString(var5);
                           break;
                        }

                        this.right.fromString(var5);
                        this.left.fromString(var5);
                        break;
                     case 2:
                        this.bottom.fromString(var5);
                        break;
                     case 3:
                        this.left.fromString(var5);
                  }
               }

               return true;
            }
         } catch (Exception var6) {
            this.parent.logInfo();
            var6.printStackTrace();
         }

         return false;
      }

      public float getTop() {
         return (Float)this.top.value();
      }

      public float getRight() {
         return (Float)this.right.value();
      }

      public float getBottom() {
         return (Float)this.bottom.value();
      }

      public float getLeft() {
         return (Float)this.left.value();
      }

      public boolean isTopPercent() {
         return this.top.isPercent();
      }

      public boolean isRightPercent() {
         return this.right.isPercent();
      }

      public boolean isBottomPercent() {
         return this.bottom.isPercent();
      }

      public boolean isLeftPercent() {
         return this.left.isPercent();
      }

      public boolean isValueSet() {
         return this.top.isValueSet() || this.right.isValueSet() || this.bottom.isValueSet() || this.left.isValueSet();
      }

      public String getValueString() {
         String var10000 = this.top.getValueString();
         return var10000 + ", " + this.right.getValueString() + ", " + this.bottom.getValueString() + ", " + this.left.getValueString();
      }
   }

   public static class XuiTexture extends XuiVar<String, XuiTexture> {
      protected XuiTexture(XuiScript var1, String var2) {
         super(XuiVarType.Texture, var1, var2);
      }

      protected XuiTexture(XuiScript var1, String var2, String var3) {
         super(XuiVarType.Texture, var1, var2, var3);
      }

      public Texture getTexture() {
         if (this.value() != null) {
            Texture var1 = Texture.getSharedTexture((String)this.value());
            if (var1 != null) {
               return var1;
            }

            if (Core.bDebug) {
               DebugLog.General.warn("Could not find texture for: " + (String)this.value());
            }
         }

         return null;
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }
   }

   public static class XuiBoolean extends XuiVar<Boolean, XuiBoolean> {
      protected XuiBoolean(XuiScript var1, String var2) {
         super(XuiVarType.Boolean, var1, var2, false);
      }

      protected XuiBoolean(XuiScript var1, String var2, boolean var3) {
         super(XuiVarType.Boolean, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(Boolean.parseBoolean(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiStringList extends XuiVar<ArrayList<String>, XuiStringList> {
      protected XuiStringList(XuiScript var1, String var2) {
         super(XuiVarType.StringList, var1, var2, new ArrayList());
      }

      protected XuiStringList(XuiScript var1, String var2, ArrayList<String> var3) {
         super(XuiVarType.StringList, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            String[] var2 = var1.split(":");
            ArrayList var3 = new ArrayList(var2.length);

            for(int var4 = 0; var4 < var2.length; ++var4) {
               var3.add(var2[var4].trim());
            }

            this.setValue(var3);
         } catch (Exception var5) {
            this.parent.logInfo();
            var5.printStackTrace();
         }

      }
   }

   public static class XuiInteger extends XuiVar<Integer, XuiInteger> {
      protected XuiInteger(XuiScript var1, String var2) {
         super(XuiVarType.Integer, var1, var2, 0);
      }

      protected XuiInteger(XuiScript var1, String var2, int var3) {
         super(XuiVarType.Integer, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(Integer.parseInt(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiFontType extends XuiVar<UIFont, XuiFontType> {
      protected XuiFontType(XuiScript var1, String var2) {
         super(XuiVarType.FontType, var1, var2, UIFont.Small);
      }

      protected XuiFontType(XuiScript var1, String var2, UIFont var3) {
         super(XuiVarType.FontType, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            if (var1.startsWith("UIFont.")) {
               var1 = var1.substring(var1.indexOf(".") + 1);
            }

            this.setValue(UIFont.valueOf(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiColor extends XuiVar<Color, XuiColor> {
      protected XuiColor(XuiScript var1, String var2) {
         super(XuiVarType.Color, var1, var2);
      }

      protected XuiColor(XuiScript var1, String var2, Color var3) {
         super(XuiVarType.Color, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            Color var2 = null;
            if (this.parent.xuiSkin != null) {
               var2 = this.parent.xuiSkin.color(var1);
            }

            if (var2 == null) {
               var2 = Colors.GetColorByName(var1);
            }

            if (var2 == null && var1.contains(":")) {
               var2 = new Color();
               String[] var3 = var1.split(":");
               if (var3.length < 3) {
                  this.parent.errorWithInfo("Warning color has <3 values. color: " + var1);
               }

               int var4;
               if (var3.length > 1 && var3[0].trim().equalsIgnoreCase("rgb")) {
                  for(var4 = 1; var4 < var3.length; ++var4) {
                     switch (var4) {
                        case 1:
                           var2.r = Float.parseFloat(var3[var4].trim()) / 255.0F;
                           break;
                        case 2:
                           var2.g = Float.parseFloat(var3[var4].trim()) / 255.0F;
                           break;
                        case 3:
                           var2.b = Float.parseFloat(var3[var4].trim()) / 255.0F;
                           break;
                        case 4:
                           var2.a = Float.parseFloat(var3[var4].trim()) / 255.0F;
                     }
                  }
               } else {
                  for(var4 = 0; var4 < var3.length; ++var4) {
                     switch (var4) {
                        case 0:
                           var2.r = Float.parseFloat(var3[var4].trim());
                           break;
                        case 1:
                           var2.g = Float.parseFloat(var3[var4].trim());
                           break;
                        case 2:
                           var2.b = Float.parseFloat(var3[var4].trim());
                           break;
                        case 3:
                           var2.a = Float.parseFloat(var3[var4].trim());
                     }
                  }
               }
            }

            if (var2 == null) {
               throw new Exception("Could not read color: " + var1);
            }

            this.setValue(var2);
         } catch (Exception var5) {
            this.parent.logInfo();
            var5.printStackTrace();
         }

      }

      public float getR() {
         return this.value() != null ? ((Color)this.value()).r : 1.0F;
      }

      public float getG() {
         return this.value() != null ? ((Color)this.value()).g : 1.0F;
      }

      public float getB() {
         return this.value() != null ? ((Color)this.value()).b : 1.0F;
      }

      public float getA() {
         return this.value() != null ? ((Color)this.value()).a : 1.0F;
      }

      public String getValueString() {
         float var10000 = this.getR();
         return "" + var10000 + ", " + this.getG() + ", " + this.getB() + ", " + this.getA();
      }
   }

   public static class XuiTranslateString extends XuiVar<String, XuiTranslateString> {
      protected XuiTranslateString(XuiScript var1, String var2) {
         super(XuiVarType.TranslateString, var1, var2);
      }

      protected XuiTranslateString(XuiScript var1, String var2, String var3) {
         super(XuiVarType.TranslateString, var1, var2, var3);
      }

      public String value() {
         return super.value() == null ? null : Translator.getText((String)super.value());
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }

      public String getValueString() {
         return super.value() != null ? (String)super.value() : "null";
      }
   }

   public static class XuiDouble extends XuiVar<Double, XuiDouble> {
      protected XuiDouble(XuiScript var1, String var2) {
         super(XuiVarType.Double, var1, var2, 0.0);
      }

      protected XuiDouble(XuiScript var1, String var2, double var3) {
         super(XuiVarType.Double, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(Double.parseDouble(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }
   }

   public static class XuiFunction extends XuiVar<String, XuiFunction> {
      protected XuiFunction(XuiScript var1, String var2) {
         super(XuiVarType.Function, var1, var2);
      }

      protected XuiFunction(XuiScript var1, String var2, String var3) {
         super(XuiVarType.Function, var1, var2, var3);
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }
   }
}
