package zombie.audio.parameters;

import java.util.TreeMap;
import java.util.function.Consumer;
import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;
import zombie.iso.zones.RoomTone;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public final class ParameterRoomTypeEx extends FMODGlobalParameter {
   static ParameterRoomTypeEx instance;
   static RoomType roomType = null;

   public ParameterRoomTypeEx() {
      super("RoomTypeEx");
      instance = this;
   }

   public float calculateCurrentValue() {
      return (float)this.getRoomType().label;
   }

   private RoomType getRoomType() {
      if (roomType != null) {
         return roomType;
      } else {
         IsoGameCharacter var1 = this.getCharacter();
         if (var1 == null) {
            return ParameterRoomTypeEx.RoomType.Generic;
         } else {
            BuildingDef var2 = var1.getCurrentBuildingDef();
            if (var2 == null) {
               return ParameterRoomTypeEx.RoomType.Generic;
            } else {
               RoomDef var3 = var1.getCurrentRoomDef();
               int var4 = PZMath.fastfloor(var1.getX() / (float)IsoCell.CellSizeInSquares);
               int var5 = PZMath.fastfloor(var1.getY() / (float)IsoCell.CellSizeInSquares);

               for(int var6 = -1; var6 <= 1; ++var6) {
                  for(int var7 = -1; var7 <= 1; ++var7) {
                     RoomType var8 = this.getRoomType(var3, var4 + var7, var5 + var6);
                     if (var8 != null) {
                        return var8;
                     }
                  }
               }

               return ParameterRoomTypeEx.RoomType.Generic;
            }
         }
      }
   }

   private RoomType getRoomType(RoomDef var1, int var2, int var3) {
      IsoMetaGrid var4 = IsoWorld.instance.getMetaGrid();
      IsoMetaCell var5 = var4.getCellData(var2, var3);
      if (var5 != null && !var5.roomTones.isEmpty()) {
         RoomTone var6 = null;

         for(int var7 = 0; var7 < var5.roomTones.size(); ++var7) {
            RoomTone var8 = (RoomTone)var5.roomTones.get(var7);
            RoomDef var9 = var4.getRoomAt(var8.x, var8.y, var8.z);
            if (var9 != null) {
               if (var9 == var1) {
                  return (RoomType)ParameterRoomTypeEx.RoomType.lowercaseMap.getOrDefault(var8.enumValue, ParameterRoomTypeEx.RoomType.Generic);
               }

               if (var8.entireBuilding && var9.building == var1.getBuilding() && (var6 == null || var1.level < var6.z && var1.level >= var8.z)) {
                  var6 = var8;
               }
            }
         }

         if (var6 != null) {
            return (RoomType)ParameterRoomTypeEx.RoomType.lowercaseMap.getOrDefault(var6.enumValue, ParameterRoomTypeEx.RoomType.Generic);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private IsoGameCharacter getCharacter() {
      IsoPlayer var1 = null;

      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         IsoPlayer var3 = IsoPlayer.players[var2];
         if (var3 != null && (var1 == null || var1.isDead() && var3.isAlive() || var1.Traits.Deaf.isSet() && !var3.Traits.Deaf.isSet())) {
            var1 = var3;
         }
      }

      return var1;
   }

   public static void setRoomType(int var0) {
      try {
         roomType = ParameterRoomTypeEx.RoomType.values()[var0];
      } catch (ArrayIndexOutOfBoundsException var2) {
         roomType = null;
      }

   }

   public static void render(IsoPlayer var0) {
      if (instance != null) {
         if (var0 == instance.getCharacter()) {
            if (var0 == IsoCamera.frameState.CamCharacter) {
               RoomDef var1 = var0.getCurrentRoomDef();
               String var10001 = var1 == null ? "null" : var1.name;
               var0.drawDebugTextBelow("RoomDef.name : " + var10001 + "\nRoomTypeEx : " + instance.getRoomType().name());
            }
         }
      }
   }

   public static void renderRoomTones() {
      int var0 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)IsoCell.CellSizeInSquares);
      int var1 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)IsoCell.CellSizeInSquares);
      renderRoomTones(var0, var1);

      for(int var2 = 0; var2 < 8; ++var2) {
         IsoDirections var3 = IsoDirections.fromIndex(var2);
         renderRoomTones(var0 + var3.dx(), var1 + var3.dy());
      }

   }

   private static void renderRoomTones(int var0, int var1) {
      IsoMetaGrid var2 = IsoWorld.instance.getMetaGrid();
      IsoMetaCell var3 = var2.getCellData(var0, var1);
      if (var3 != null && !var3.roomTones.isEmpty()) {
         PlayerCamera var4 = IsoCamera.cameras[IsoCamera.frameState.playerIndex];

         for(int var5 = 0; var5 < var3.roomTones.size(); ++var5) {
            RoomTone var6 = (RoomTone)var3.roomTones.get(var5);
            float var7 = (float)TextManager.instance.getFontHeight(UIFont.Small);
            int var8 = TextManager.instance.MeasureStringX(UIFont.Small, var6.enumValue);
            int var9 = (int)var7;
            float var10 = IsoUtils.XToScreen((float)var6.x + 0.5F + var4.fixJigglyModelsSquareX * 0.0F, (float)var6.y + 0.5F + var4.fixJigglyModelsSquareY * 0.0F, (float)var6.z, 0);
            float var11 = IsoUtils.YToScreen((float)var6.x + 0.5F + var4.fixJigglyModelsSquareX * 0.0F, (float)var6.y + 0.5F + var4.fixJigglyModelsSquareY * 0.0F, (float)var6.z, 0);
            var11 -= (float)var9 / 2.0F;
            var10 -= var4.getOffX();
            var11 -= var4.getOffY();
            var10 /= var4.zoom;
            var11 /= var4.zoom;
            SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
            SpriteRenderer.instance.renderi((Texture)null, (int)(var10 - (float)(var8 / 2)), (int)(var11 - ((float)var9 - var7) / 2.0F), var8, var9, 0.0F, 0.0F, 0.0F, 0.5F, (Consumer)null);
            TextManager.instance.DrawStringCentre(UIFont.Small, (double)var10, (double)var11, var6.enumValue, 1.0, 1.0, 1.0, 1.0);
            SpriteRenderer.instance.EndShader();
         }

      }
   }

   private static enum RoomType {
      Generic(0),
      AbandonedGrainmill(1),
      Airport(2),
      AirstripOffice(3),
      AnimalsanctuaryKennels(4),
      AnimalsanctuaryMain(5),
      AnimalShelter(6),
      ApartmentBuilding(7),
      Arena01Derby(8),
      Arena02Stables(9),
      Arena04Publicbathroom(10),
      Arena07Offices(11),
      Arena08Changerooms(12),
      Arena10Broadcasting(13),
      Arena12Baseball(14),
      Arena13Indoors(15),
      Bakery(16),
      Bank(17),
      BarWood(18),
      BarStone(19),
      Bargnclothes(20),
      Barn(21),
      BasementApartmnetBuilding(22),
      BasementBank(23),
      BasementBar(24),
      BasementDestroyedBuilding(25),
      BasementChurch(26),
      BasementColdwarBunker(27),
      BasementConstruction01Factory(28),
      BasementDetentionCentre(29),
      BasementFactoryAbandoned(30),
      BasementFirestation(31),
      BasementGas(32),
      BasementGeneralstore(33),
      BasementGovernmentSecret(34),
      BasementGunclub(35),
      BasementHouse(36),
      BasementHouseEastereggSpiffo(37),
      BasementIndustrialMedium(38),
      BasementOffices(39),
      BasementPaddlewheeler(40),
      BasementPlaza(41),
      BasementPolice(42),
      BasementRestaurant(43),
      BasementSchool(44),
      BasementStorefront(45),
      Bedandbreakfast(46),
      Boatclub(47),
      Boathouse(48),
      Bookstore(49),
      Bowlingalley(50),
      Brewery(51),
      BrownbrickTownhouse(52),
      BuildingDestroyed(53),
      Busshelter(54),
      Cabin(55),
      CabinAbandoned(56),
      Cardealership(57),
      Carsupplystore(58),
      Cattleauction(59),
      Cattlelot(60),
      Changerooms(61),
      Church(62),
      ChurchAbandoned(63),
      ChurchBurnt(64),
      CivilianMedia(65),
      CivilianTent(66),
      Cleaningservices(67),
      Clothesstore(68),
      Coffeeshop(69),
      CaldwarBunker(70),
      CommunityCentre(71),
      ConvenienceStoreBurnt(72),
      ConventionHall(73),
      Cornerstore(74),
      Countryclub(75),
      CountryclubGolfcarts(76),
      CountryclubMaintenance(77),
      CountryclubMechanic(78),
      CountryclubClubhouse(79),
      CountryclubChangerooms(80),
      CountryclubSauna(81),
      CountryclubSnackbar(82),
      CountryclubGuardbooth(83),
      Departmentstore(84),
      Depository(85),
      Detentioncenter(86),
      Diner(87),
      Distillery(88),
      Dock(89),
      Emptystore(90),
      Factory(91),
      FarmStorage(92),
      Farmhouse(93),
      Farmhousing(94),
      Farmstore(95),
      Firestation(96),
      Fishbait(97),
      Fitnesscenter(98),
      Freight(99),
      Funeralhome(100),
      Furniturestore(101),
      Gallery(102),
      GarageBurnt(103),
      Garage(104),
      Garagestorage(105),
      Gardenstore(106),
      Gas(107),
      GasBurnt(108),
      Gatehouse(109),
      Generalstore(110),
      Government(111),
      Greenhouse(112),
      Grocery(113),
      Groundskeeper(114),
      Guardbooth(115),
      Gunclub(116),
      Gym(117),
      Hairdresser(118),
      Hanger(119),
      Hayshipping(120),
      HighriseMgCenter(121),
      HorseridingCentre(122),
      Hospital(123),
      Hotel(124),
      HouseAbandoned(125),
      HouseBurnt(126),
      HouseCountry(127),
      HouseDestroyed(128),
      HouseGarage(129),
      HouseGarageDamaged(130),
      HouseLake(131),
      HouseLarge(132),
      HouseMedium(133),
      HouseMediumDamaged(134),
      HouseSmall(135),
      HouseSmallDamaged(136),
      HouseSuburb(137),
      HouseSuburbGarage(138),
      HouseTrapper(139),
      IndustrialWarehouse(140),
      IndustryDorm(141),
      Kicthenshowroom(142),
      Lakehouse(143),
      Lasertag(144),
      Laundromat(145),
      LaundromatBurnt(146),
      Library(147),
      LibraryBurnt(148),
      Liquorstore(149),
      Lofts(150),
      Mall(151),
      Mansion(152),
      Mausoleum(153),
      Mechanic(154),
      Medical(155),
      MilitaryApartments(156),
      MilitaryBorder(157),
      MilitaryCommunications(158),
      MilitaryHouse(159),
      MilitaryTent(160),
      MilitaryTower(161),
      MilitaryTownhouse(162),
      MilitaryTrailer(163),
      Motel(164),
      MotelDestroyed(165),
      Movierental(166),
      MusicfestivalTower(167),
      MusicfestivalTrailer(168),
      Musicstore(169),
      Nursinghome(170),
      Offices(171),
      Officesupplies(172),
      Optometrist(173),
      Outhouse(174),
      Paddlewheeler(175),
      Paintshop(176),
      ParkTreehouse(177),
      ParkRestrooms(178),
      ParkPlayhouse(179),
      Pawnshop(180),
      Paybooth(181),
      Pharmacy(182),
      Photobooth(183),
      Pizzawhirled(184),
      PizaawhirledBurnt(185),
      Plaza(186),
      PlazaBurnt(187),
      Police(188),
      PoliceBurnt(189),
      Portacabin(190),
      Post(191),
      Prison(192),
      PrisonGuardbooth(193),
      PrisonGuardtower(194),
      Publicwashroom(195),
      RadioTowerHouse(196),
      RadioTowerStation(197),
      Radiostation(198),
      Rangerstation(199),
      Reccenter(200),
      Recordingstudio(201),
      RedbrickTownhouse(202),
      Refinery(203),
      Resort(204),
      ResortCabin(205),
      RestareaBathrooms(206),
      Restaurant(207),
      Rollerrink(208),
      Sanatorium(209),
      School(210),
      SchoolColege(211),
      SchoolDorms(212),
      ScrapyardGarage(213),
      Security(214),
      Shack(215),
      Shed(216),
      Shelter(217),
      Shootingrange(218),
      ShopCamping(219),
      ShopHunting(220),
      Singleapartment(221),
      Slaughterhouse(222),
      Spa(223),
      Speedway(224),
      Sportstore(225),
      Stables(226),
      StoreAbandoned(227),
      Storefront(228),
      Summercamp(229),
      Taxi(230),
      TaxiGarage(231),
      Theatre(232),
      Thermalpowerstation(233),
      Tobaccostore(234),
      Tollbooth(235),
      Toolstore(236),
      Tornadoshelter(237),
      Townhouse(238),
      Trailer(239),
      TrailerBurnt(240),
      TrailerNice(241),
      TrailerOffice(242),
      Traincar(243),
      Trainstation(244),
      TrainstationAbadoned(245),
      Trainyard(246),
      TrainyardOffice(247),
      Transportcompany(248),
      Treehouse(249),
      Truck(250),
      Tvstudio(251),
      Vetrenarian(252),
      WadsworthBasemenet(253),
      Warehousestorage(254),
      WaterpurificationCheckpoint(255),
      WaterpurificationMainBuilding(256),
      WaterpurificationOffice(257),
      Weddingstore(258),
      Weldingshop(259),
      Westpointcaboose(260),
      Wildwestoffice(261),
      WoodCabin(262),
      WreckingyardOffice(263),
      Zippee(264),
      Victorian(265);

      static final TreeMap<String, RoomType> lowercaseMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
      final int label;

      private RoomType(int var3) {
         this.label = var3;
      }

      static {
         RoomType[] var0 = values();
         int var1 = var0.length;

         for(int var2 = 0; var2 < var1; ++var2) {
            RoomType var3 = var0[var2];
            lowercaseMap.put(var3.name(), var3);
         }

      }
   }
}
