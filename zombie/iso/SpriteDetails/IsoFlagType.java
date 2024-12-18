package zombie.iso.SpriteDetails;

import java.util.HashMap;

public enum IsoFlagType {
   collideW(0),
   collideN(1),
   solidfloor(2),
   noStart(3),
   windowW(4),
   windowN(5),
   hidewalls(6),
   exterior(7),
   NoWallLighting(8),
   doorW(9),
   doorN(10),
   transparentW(11),
   transparentN(12),
   WallOverlay(13),
   FloorOverlay(14),
   vegitation(15),
   burning(16),
   burntOut(17),
   unflamable(18),
   cutW(19),
   cutN(20),
   tableN(21),
   tableNW(22),
   tableW(23),
   tableSW(24),
   tableS(25),
   tableSE(26),
   tableE(27),
   tableNE(28),
   halfheight(29),
   HasRainSplashes(30),
   HasRaindrop(31),
   solid(32),
   trans(33),
   pushable(34),
   solidtrans(35),
   invisible(36),
   floorS(37),
   floorE(38),
   shelfS(39),
   shelfE(40),
   alwaysDraw(41),
   ontable(42),
   transparentFloor(43),
   climbSheetW(44),
   climbSheetN(45),
   climbSheetTopN(46),
   climbSheetTopW(47),
   attachtostairs(48),
   sheetCurtains(49),
   waterPiped(50),
   HoppableN(51),
   HoppableW(52),
   bed(53),
   blueprint(54),
   canPathW(55),
   canPathN(56),
   blocksight(57),
   climbSheetE(58),
   climbSheetS(59),
   climbSheetTopE(60),
   climbSheetTopS(61),
   makeWindowInvincible(62),
   water(63),
   canBeCut(64),
   canBeRemoved(65),
   taintedWater(66),
   smoke(67),
   attachedN(68),
   attachedS(69),
   attachedE(70),
   attachedW(71),
   attachedFloor(72),
   attachedSurface(73),
   attachedCeiling(74),
   attachedNW(75),
   ForceAmbient(76),
   WallSE(77),
   WindowN(78),
   WindowW(79),
   FloorHeightOneThird(80),
   FloorHeightTwoThirds(81),
   CantClimb(82),
   diamondFloor(83),
   attachedSE(84),
   TallHoppableW(85),
   WallWTrans(86),
   TallHoppableN(87),
   WallNTrans(88),
   container(89),
   DoorWallW(90),
   DoorWallN(91),
   WallW(92),
   WallN(93),
   WallNW(94),
   SpearOnlyAttackThrough(95),
   forceRender(96),
   open(97),
   SpriteConfig(98),
   BlockRain(99),
   EntityScript(100),
   isEave(101),
   openAir(102),
   HasLightOnSprite(103),
   unlit(104),
   NeverCutaway(105),
   DoubleDoor1(106),
   DoubleDoor2(107),
   MAX(108);

   private final int index;
   private static final IsoFlagType[] EnumConstants = (IsoFlagType[])IsoFlagType.class.getEnumConstants();
   private static final HashMap<String, IsoFlagType> fromStringMap = new HashMap();

   private IsoFlagType(int var3) {
      this.index = var3;
   }

   public int index() {
      return this.index;
   }

   public static IsoFlagType fromIndex(int var0) {
      return EnumConstants[var0];
   }

   public static IsoFlagType FromString(String var0) {
      return (IsoFlagType)fromStringMap.getOrDefault(var0, MAX);
   }

   static {
      IsoFlagType[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         IsoFlagType var3 = var0[var2];
         if (var3 == MAX) {
            break;
         }

         fromStringMap.put(var3.name(), var3);
      }

   }
}
