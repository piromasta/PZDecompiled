package zombie.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.core.random.Rand;

public final class Colors {
   private static final HashMap<String, ColNfo> infoMap = new HashMap();
   private static final ArrayList<ColNfo> infoList = new ArrayList();
   private static final ArrayList<Color> colors = new ArrayList();
   private static final HashMap<String, Color> colorMap = new HashMap();
   private static final ArrayList<String> colorNames = new ArrayList();
   private static final HashSet<String> colorSet = new HashSet();
   private static final HashMap<String, ColNfo> CB_infoMap = new HashMap();
   private static final ArrayList<Color> CB_colors = new ArrayList();
   private static final HashMap<String, Color> CB_colorMap = new HashMap();
   private static final ArrayList<String> CB_colorNames = new ArrayList();
   private static final HashSet<String> CB_colorSet = new HashSet();
   public static final Color UI_Background = AddGameColor("UI_Background", new Color(0.0F, 0.0F, 0.0F));
   public static final Color Black = AddGameColor("Black", new Color(0.0F, 0.0F, 0.0F));
   public static final Color CB_G0_SherwoodGreen = addColorCB("SherwoodGreen", new Color(0.0F, 0.239F, 0.188F));
   public static final Color CB_G1_PthaloGreen = addColorCB("PthaloGreen", new Color(0.0F, 0.341F, 0.271F));
   public static final Color CB_G2_TropicalRainForest = addColorCB("TropicalRainForest", new Color(0.0F, 0.451F, 0.361F));
   public static final Color CB_G3_Observatory = addColorCB("Observatory", new Color(0.0F, 0.569F, 0.459F));
   public static final Color CB_G4_JungleGreen = addColorCB("JungleGreen", new Color(0.0F, 0.686F, 0.557F));
   public static final Color CB_G5_Dali = addColorCB("Dali", new Color(0.0F, 0.796F, 0.655F));
   public static final Color CB_G6_AquaMarine = addColorCB("AquaMarine", new Color(0.0F, 0.922F, 0.757F));
   public static final Color CB_G7_LightAqua = addColorCB("LightAqua", new Color(0.525F, 1.0F, 0.871F));
   public static final Color CB_B0_Submerge = addColorCB("Submerge", new Color(0.0F, 0.188F, 0.435F));
   public static final Color CB_B1_Elvis = addColorCB("Elvis", new Color(0.0F, 0.282F, 0.62F));
   public static final Color CB_B2_FlatMediumBlue = addColorCB("FlatMediumBlue", new Color(0.0F, 0.373F, 0.8F));
   public static final Color CB_B3_ClearBlue = addColorCB("ClearBlue", new Color(0.0F, 0.475F, 0.98F));
   public static final Color CB_B4_Azure = addColorCB("Azure", new Color(0.0F, 0.624F, 0.98F));
   public static final Color CB_B5_SpiroDiscoBall = addColorCB("SpiroDiscoBall", new Color(0.0F, 0.761F, 0.976F));
   public static final Color CB_B6_AquaBlue = addColorCB("AquaBlue", new Color(0.0F, 0.898F, 0.973F));
   public static final Color CB_B7_LightBrilliantCyan = addColorCB("LightBrilliantCyan", new Color(0.486F, 1.0F, 0.98F));
   public static final Color CB_R0_DeepAmaranth = addColorCB("DeepAmaranth", new Color(0.373F, 0.0353F, 0.0784F));
   public static final Color CB_R1_HotChile = addColorCB("HotChile", new Color(0.525F, 0.0314F, 0.11F));
   public static final Color CB_R2_Smashing = addColorCB("Smashing", new Color(0.698F, 0.0275F, 0.145F));
   public static final Color CB_R3_GeraniumLake = addColorCB("GeraniumLake", new Color(0.871F, 0.051F, 0.18F));
   public static final Color CB_R4_RedOrange = addColorCB("RedOrange", new Color(1.0F, 0.259F, 0.208F));
   public static final Color CB_R5_Crusta = addColorCB("Crusta", new Color(1.0F, 0.529F, 0.208F));
   public static final Color CB_R6_GoldenYellow = addColorCB("GoldenYellow", new Color(1.0F, 0.725F, 0.208F));
   public static final Color CB_R7_BananaYellow = addColorCB("BananaYellow", new Color(1.0F, 0.886F, 0.224F));
   public static final Color CB_White = addColor("White", new Color(1.0F, 1.0F, 1.0F));
   public static final Color White = addColor("White", new Color(1.0F, 1.0F, 1.0F));
   public static final Color Silver = addColor("Silver", new Color(0.753F, 0.753F, 0.753F));
   public static final Color Gray = addColor("Gray", new Color(0.502F, 0.502F, 0.502F));
   public static final Color Red = addColor("Red", new Color(1.0F, 0.0F, 0.0F));
   public static final Color Maroon = addColor("Maroon", new Color(0.502F, 0.0F, 0.0F));
   public static final Color Yellow = addColor("Yellow", new Color(1.0F, 1.0F, 0.0F));
   public static final Color Olive = addColor("Olive", new Color(0.502F, 0.502F, 0.0F));
   public static final Color Lime = addColor("Lime", new Color(0.0F, 1.0F, 0.0F));
   public static final Color Green = addColor("Green", new Color(0.0F, 0.502F, 0.0F));
   public static final Color Cyan = addColor("Cyan", new Color(0.0F, 1.0F, 1.0F));
   public static final Color Teal = addColor("Teal", new Color(0.0F, 0.502F, 0.502F));
   public static final Color Blue = addColor("Blue", new Color(0.0F, 0.0F, 1.0F));
   public static final Color Navy = addColor("Navy", new Color(0.0F, 0.0F, 0.502F));
   public static final Color Magenta = addColor("Magenta", new Color(1.0F, 0.0F, 1.0F));
   public static final Color Purple = addColor("Purple", new Color(0.502F, 0.0F, 0.502F));
   public static final Color Orange = addColor("Orange", new Color(1.0F, 0.647F, 0.0F));
   public static final Color Pink = addColor("Pink", new Color(1.0F, 0.753F, 0.796F));
   public static final Color Brown = addColor("Brown", new Color(0.647F, 0.165F, 0.165F));
   public static final Color Gainsboro = addColor("Gainsboro", new Color(0.863F, 0.863F, 0.863F));
   public static final Color LightGray = addColor("LightGray", new Color(0.827F, 0.827F, 0.827F));
   public static final Color DarkGray = addColor("DarkGray", new Color(0.663F, 0.663F, 0.663F));
   public static final Color DimGray = addColor("DimGray", new Color(0.412F, 0.412F, 0.412F));
   public static final Color LightSlateGray = addColor("LightSlateGray", new Color(0.467F, 0.533F, 0.6F));
   public static final Color SlateGray = addColor("SlateGray", new Color(0.439F, 0.502F, 0.565F));
   public static final Color DarkSlateGray = addColor("DarkSlateGray", new Color(0.184F, 0.31F, 0.31F));
   public static final Color IndianRed = addColor("IndianRed", new Color(0.804F, 0.361F, 0.361F));
   public static final Color LightCoral = addColor("LightCoral", new Color(0.941F, 0.502F, 0.502F));
   public static final Color Salmon = addColor("Salmon", new Color(0.98F, 0.502F, 0.447F));
   public static final Color DarkSalmon = addColor("DarkSalmon", new Color(0.914F, 0.588F, 0.478F));
   public static final Color LightSalmon = addColor("LightSalmon", new Color(1.0F, 0.627F, 0.478F));
   public static final Color Crimson = addColor("Crimson", new Color(0.863F, 0.0784F, 0.235F));
   public static final Color FireBrick = addColor("FireBrick", new Color(0.698F, 0.133F, 0.133F));
   public static final Color DarkRed = addColor("DarkRed", new Color(0.545F, 0.0F, 0.0F));
   public static final Color LightPink = addColor("LightPink", new Color(1.0F, 0.714F, 0.757F));
   public static final Color HotPink = addColor("HotPink", new Color(1.0F, 0.412F, 0.706F));
   public static final Color DeepPink = addColor("DeepPink", new Color(1.0F, 0.0784F, 0.576F));
   public static final Color MediumVioletRed = addColor("MediumVioletRed", new Color(0.78F, 0.0824F, 0.522F));
   public static final Color PaleVioletRed = addColor("PaleVioletRed", new Color(0.859F, 0.439F, 0.576F));
   public static final Color Coral = addColor("Coral", new Color(1.0F, 0.498F, 0.314F));
   public static final Color Tomato = addColor("Tomato", new Color(1.0F, 0.388F, 0.278F));
   public static final Color OrangeRed = addColor("OrangeRed", new Color(1.0F, 0.271F, 0.0F));
   public static final Color DarkOrange = addColor("DarkOrange", new Color(1.0F, 0.549F, 0.0F));
   public static final Color Gold = addColor("Gold", new Color(1.0F, 0.843F, 0.0F));
   public static final Color LightYellow = addColor("LightYellow", new Color(1.0F, 1.0F, 0.878F));
   public static final Color LemonChiffon = addColor("LemonChiffon", new Color(1.0F, 0.98F, 0.804F));
   public static final Color LightGoldenrodYellow = addColor("LightGoldenrodYellow", new Color(0.98F, 0.98F, 0.824F));
   public static final Color PapayaWhip = addColor("PapayaWhip", new Color(1.0F, 0.937F, 0.835F));
   public static final Color Moccasin = addColor("Moccasin", new Color(1.0F, 0.894F, 0.71F));
   public static final Color PeachPuff = addColor("PeachPuff", new Color(1.0F, 0.855F, 0.725F));
   public static final Color PaleGoldenrod = addColor("PaleGoldenrod", new Color(0.933F, 0.91F, 0.667F));
   public static final Color Khaki = addColor("Khaki", new Color(0.941F, 0.902F, 0.549F));
   public static final Color DarkKhaki = addColor("DarkKhaki", new Color(0.741F, 0.718F, 0.42F));
   public static final Color Lavender = addColor("Lavender", new Color(0.902F, 0.902F, 0.98F));
   public static final Color Thistle = addColor("Thistle", new Color(0.847F, 0.749F, 0.847F));
   public static final Color Plum = addColor("Plum", new Color(0.867F, 0.627F, 0.867F));
   public static final Color Violet = addColor("Violet", new Color(0.933F, 0.51F, 0.933F));
   public static final Color Orchid = addColor("Orchid", new Color(0.855F, 0.439F, 0.839F));
   public static final Color MediumOrchid = addColor("MediumOrchid", new Color(0.729F, 0.333F, 0.827F));
   public static final Color MediumPurple = addColor("MediumPurple", new Color(0.576F, 0.439F, 0.859F));
   public static final Color RebeccaPurple = addColor("RebeccaPurple", new Color(0.4F, 0.2F, 0.6F));
   public static final Color BlueViolet = addColor("BlueViolet", new Color(0.541F, 0.169F, 0.886F));
   public static final Color DarkViolet = addColor("DarkViolet", new Color(0.58F, 0.0F, 0.827F));
   public static final Color DarkOrchid = addColor("DarkOrchid", new Color(0.6F, 0.196F, 0.8F));
   public static final Color DarkMagenta = addColor("DarkMagenta", new Color(0.545F, 0.0F, 0.545F));
   public static final Color Indigo = addColor("Indigo", new Color(0.294F, 0.0F, 0.51F));
   public static final Color SlateBlue = addColor("SlateBlue", new Color(0.416F, 0.353F, 0.804F));
   public static final Color DarkSlateBlue = addColor("DarkSlateBlue", new Color(0.282F, 0.239F, 0.545F));
   public static final Color MediumSlateBlue = addColor("MediumSlateBlue", new Color(0.482F, 0.408F, 0.933F));
   public static final Color GreenYellow = addColor("GreenYellow", new Color(0.678F, 1.0F, 0.184F));
   public static final Color Chartreuse = addColor("Chartreuse", new Color(0.498F, 1.0F, 0.0F));
   public static final Color LawnGreen = addColor("LawnGreen", new Color(0.486F, 0.988F, 0.0F));
   public static final Color LimeGreen = addColor("LimeGreen", new Color(0.196F, 0.804F, 0.196F));
   public static final Color PaleGreen = addColor("PaleGreen", new Color(0.596F, 0.984F, 0.596F));
   public static final Color LightGreen = addColor("LightGreen", new Color(0.565F, 0.933F, 0.565F));
   public static final Color MediumSpringGreen = addColor("MediumSpringGreen", new Color(0.0F, 0.98F, 0.604F));
   public static final Color SpringGreen = addColor("SpringGreen", new Color(0.0F, 1.0F, 0.498F));
   public static final Color MediumSeaGreen = addColor("MediumSeaGreen", new Color(0.235F, 0.702F, 0.443F));
   public static final Color SeaGreen = addColor("SeaGreen", new Color(0.18F, 0.545F, 0.341F));
   public static final Color ForestGreen = addColor("ForestGreen", new Color(0.133F, 0.545F, 0.133F));
   public static final Color DarkGreen = addColor("DarkGreen", new Color(0.0F, 0.392F, 0.0F));
   public static final Color YellowGreen = addColor("YellowGreen", new Color(0.604F, 0.804F, 0.196F));
   public static final Color OliveDrab = addColor("OliveDrab", new Color(0.42F, 0.557F, 0.137F));
   public static final Color DarkOliveGreen = addColor("DarkOliveGreen", new Color(0.333F, 0.42F, 0.184F));
   public static final Color MediumAquamarine = addColor("MediumAquamarine", new Color(0.4F, 0.804F, 0.667F));
   public static final Color DarkSeaGreen = addColor("DarkSeaGreen", new Color(0.561F, 0.737F, 0.545F));
   public static final Color LightSeaGreen = addColor("LightSeaGreen", new Color(0.125F, 0.698F, 0.667F));
   public static final Color DarkCyan = addColor("DarkCyan", new Color(0.0F, 0.545F, 0.545F));
   public static final Color LightCyan = addColor("LightCyan", new Color(0.878F, 1.0F, 1.0F));
   public static final Color PaleTurquoise = addColor("PaleTurquoise", new Color(0.686F, 0.933F, 0.933F));
   public static final Color Aquamarine = addColor("Aquamarine", new Color(0.498F, 1.0F, 0.831F));
   public static final Color Turquoise = addColor("Turquoise", new Color(0.251F, 0.878F, 0.816F));
   public static final Color MediumTurquoise = addColor("MediumTurquoise", new Color(0.282F, 0.82F, 0.8F));
   public static final Color DarkTurquoise = addColor("DarkTurquoise", new Color(0.0F, 0.808F, 0.82F));
   public static final Color CadetBlue = addColor("CadetBlue", new Color(0.373F, 0.62F, 0.627F));
   public static final Color SteelBlue = addColor("SteelBlue", new Color(0.275F, 0.51F, 0.706F));
   public static final Color LightSteelBlue = addColor("LightSteelBlue", new Color(0.69F, 0.769F, 0.871F));
   public static final Color PowderBlue = addColor("PowderBlue", new Color(0.69F, 0.878F, 0.902F));
   public static final Color LightBlue = addColor("LightBlue", new Color(0.678F, 0.847F, 0.902F));
   public static final Color SkyBlue = addColor("SkyBlue", new Color(0.529F, 0.808F, 0.922F));
   public static final Color LightSkyBlue = addColor("LightSkyBlue", new Color(0.529F, 0.808F, 0.98F));
   public static final Color DeepSkyBlue = addColor("DeepSkyBlue", new Color(0.0F, 0.749F, 1.0F));
   public static final Color DodgerBlue = addColor("DodgerBlue", new Color(0.118F, 0.565F, 1.0F));
   public static final Color CornFlowerBlue = addColor("CornFlowerBlue", new Color(0.392F, 0.584F, 0.929F));
   public static final Color RoyalBlue = addColor("RoyalBlue", new Color(0.255F, 0.412F, 0.882F));
   public static final Color MediumBlue = addColor("MediumBlue", new Color(0.0F, 0.0F, 0.804F));
   public static final Color DarkBlue = addColor("DarkBlue", new Color(0.0F, 0.0F, 0.545F));
   public static final Color MidnightBlue = addColor("MidnightBlue", new Color(0.098F, 0.098F, 0.439F));
   public static final Color Cornsilk = addColor("Cornsilk", new Color(1.0F, 0.973F, 0.863F));
   public static final Color BlanchedAlmond = addColor("BlanchedAlmond", new Color(1.0F, 0.922F, 0.804F));
   public static final Color Bisque = addColor("Bisque", new Color(1.0F, 0.894F, 0.769F));
   public static final Color NavajoWhite = addColor("NavajoWhite", new Color(1.0F, 0.871F, 0.678F));
   public static final Color Wheat = addColor("Wheat", new Color(0.961F, 0.871F, 0.702F));
   public static final Color BurlyWood = addColor("BurlyWood", new Color(0.871F, 0.722F, 0.529F));
   public static final Color Tan = addColor("Tan", new Color(0.824F, 0.706F, 0.549F));
   public static final Color RosyBrown = addColor("RosyBrown", new Color(0.737F, 0.561F, 0.561F));
   public static final Color SandyBrown = addColor("SandyBrown", new Color(0.957F, 0.643F, 0.376F));
   public static final Color Goldenrod = addColor("Goldenrod", new Color(0.855F, 0.647F, 0.125F));
   public static final Color DarkGoldenrod = addColor("DarkGoldenrod", new Color(0.722F, 0.525F, 0.0431F));
   public static final Color Peru = addColor("Peru", new Color(0.804F, 0.522F, 0.247F));
   public static final Color Chocolate = addColor("Chocolate", new Color(0.824F, 0.412F, 0.118F));
   public static final Color SaddleBrown = addColor("SaddleBrown", new Color(0.545F, 0.271F, 0.0745F));
   public static final Color Sienna = addColor("Sienna", new Color(0.627F, 0.322F, 0.176F));
   public static final Color Snow = addColor("Snow", new Color(1.0F, 0.98F, 0.98F));
   public static final Color HoneyDew = addColor("HoneyDew", new Color(0.941F, 1.0F, 0.941F));
   public static final Color MintCream = addColor("MintCream", new Color(0.961F, 1.0F, 0.98F));
   public static final Color Azure = addColor("Azure", new Color(0.941F, 1.0F, 1.0F));
   public static final Color AliceBlue = addColor("AliceBlue", new Color(0.941F, 0.973F, 1.0F));
   public static final Color GhostWhite = addColor("GhostWhite", new Color(0.973F, 0.973F, 1.0F));
   public static final Color WhiteSmoke = addColor("WhiteSmoke", new Color(0.961F, 0.961F, 0.961F));
   public static final Color SeaShell = addColor("SeaShell", new Color(1.0F, 0.961F, 0.933F));
   public static final Color Beige = addColor("Beige", new Color(0.961F, 0.961F, 0.863F));
   public static final Color OldLace = addColor("OldLace", new Color(0.992F, 0.961F, 0.902F));
   public static final Color FloralWhite = addColor("FloralWhite", new Color(1.0F, 0.98F, 0.941F));
   public static final Color Ivory = addColor("Ivory", new Color(1.0F, 1.0F, 0.941F));
   public static final Color AntiqueWhite = addColor("AntiqueWhite", new Color(0.98F, 0.922F, 0.843F));
   public static final Color Linen = addColor("Linen", new Color(0.98F, 0.941F, 0.902F));
   public static final Color LavenderBlush = addColor("LavenderBlush", new Color(1.0F, 0.941F, 0.961F));
   public static final Color MistyRose = addColor("MistyRose", new Color(1.0F, 0.894F, 0.882F));
   public static final Color Grenadine = addColor("Grenadine", new Color(0.674F, 0.329F, 0.368F));
   public static final Color Cola = addColor("Cola", new Color(0.235F, 0.184F, 0.137F));
   public static final Color Ginger = addColor("Ginger", new Color(0.69F, 0.396F, 0.0F));
   public static final Color FruitPunch = addColor("FruitPunch", new Color(0.807F, 0.239F, 0.282F));
   public static final Color Fuchsia;
   public static final Color Aqua;

   public Colors() {
   }

   private static Color addColor(String var0, Color var1) {
      ColNfo var2 = new ColNfo(var0, var1, Colors.ColorSet.Standard);
      infoMap.put(var0, var2);
      infoList.add(var2);
      colors.add(var1);
      colorMap.put(var0.toLowerCase(), var1);
      colorNames.add(var0);
      colorSet.add(var0.toLowerCase());
      return var1;
   }

   public static Color AddGameColor(String var0, Color var1) {
      ColNfo var2;
      if (infoMap.containsKey(var0)) {
         var2 = (ColNfo)infoMap.get(var0);
         if (var2.colorSet == Colors.ColorSet.Game) {
            var2.color.set(var1);
            return var1;
         }
      }

      var2 = new ColNfo(var0, var1, Colors.ColorSet.Game);
      infoMap.put(var0, var2);
      infoList.add(var2);
      colorMap.put(var0.toLowerCase(), var1);
      colorNames.add(var0);
      colorSet.add(var0.toLowerCase());
      return var1;
   }

   public static ColNfo GetColorInfo(String var0) {
      return (ColNfo)infoMap.get(var0);
   }

   public static Color GetRandomColor() {
      return (Color)colors.get(Rand.Next(0, colors.size() - 1));
   }

   public static Color GetColorFromIndex(int var0) {
      return (Color)colors.get(var0);
   }

   public static String GetColorNameFromIndex(int var0) {
      return (String)colorNames.get(var0);
   }

   public static int GetColorsCount() {
      return colors.size();
   }

   public static Color GetColorByName(String var0) {
      return (Color)colorMap.get(var0.toLowerCase());
   }

   public static ArrayList<String> GetColorNames() {
      return colorNames;
   }

   public static boolean ColorExists(String var0) {
      return colorSet.contains(var0.toLowerCase());
   }

   public static Color CB_GetRandomColor() {
      return (Color)CB_colors.get(Rand.Next(0, CB_colors.size() - 1));
   }

   public static Color CB_GetColorFromIndex(int var0) {
      return (Color)CB_colors.get(var0);
   }

   public static String CB_GetColorNameFromIndex(int var0) {
      return (String)CB_colorNames.get(var0);
   }

   public static int CB_GetColorsCount() {
      return CB_colors.size();
   }

   public static Color CB_GetColorByName(String var0) {
      return (Color)CB_colorMap.get(var0.toLowerCase());
   }

   public static ArrayList<String> CB_GetColorNames() {
      return CB_colorNames;
   }

   public static boolean CB_ColorExists(String var0) {
      return CB_colorSet.contains(var0.toLowerCase());
   }

   private static Color addColorCB(String var0, Color var1) {
      ColNfo var2 = new ColNfo(var0, var1, Colors.ColorSet.ColorBlind);
      infoList.add(var2);
      CB_infoMap.put(var0, var2);
      CB_colors.add(var1);
      CB_colorMap.put(var0.toLowerCase(), var1);
      CB_colorNames.add(var0);
      CB_colorSet.add(var0.toLowerCase());
      return var1;
   }

   static {
      Fuchsia = Magenta;
      Aqua = Cyan;
   }

   public static class ColNfo {
      private final ColorSet colorSet;
      private final String name;
      private final String hex;
      private final Color color;
      private final float r;
      private final float g;
      private final float b;
      private final int rInt;
      private final int gInt;
      private final int bInt;

      public ColNfo(String var1, Color var2, ColorSet var3) {
         this.colorSet = var3;
         this.name = var1;
         this.color = var2;
         this.r = var2.r;
         this.g = var2.g;
         this.b = var2.b;
         this.rInt = (int)this.r * 255;
         this.gInt = (int)this.g * 255;
         this.bInt = (int)this.b * 255;
         this.hex = String.format("#%02x%02x%02x", this.rInt, this.gInt, this.bInt);
      }

      public ColorSet getColorSet() {
         return this.colorSet;
      }

      public int getColorSetIndex() {
         return this.colorSet.index;
      }

      public String getName() {
         return this.name;
      }

      public String getHex() {
         return this.hex;
      }

      public Color getColor() {
         return this.color;
      }

      public float getR() {
         return this.r;
      }

      public float getG() {
         return this.g;
      }

      public float getB() {
         return this.b;
      }

      public int getRInt() {
         return this.rInt;
      }

      public int getGInt() {
         return this.gInt;
      }

      public int getBInt() {
         return this.bInt;
      }
   }

   public static enum ColorSet {
      Game(0),
      Standard(1),
      ColorBlind(2);

      final int index;

      private ColorSet(int var3) {
         this.index = var3;
      }

      public int getIndex() {
         return this.index;
      }
   }
}
