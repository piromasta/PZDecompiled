package zombie.core;

import fmod.FMOD_DriverInfo;
import fmod.javafmod;
import imgui.ImDrawData;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Vector3f;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Controller;
import org.lwjglx.input.Keyboard;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.PixelFormat;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MovingObjectUpdateScheduler;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorFactory;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.professions.ProfessionFactory;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.TraitFactory;
import zombie.config.ArrayConfigOption;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.MatrixStack;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.MultiTextureFBO2;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugContext;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.IngameState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.iso.BentFences;
import zombie.iso.BrokenFences;
import zombie.iso.ContainerOverlays;
import zombie.iso.IsoCamera;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.PlayerCamera;
import zombie.iso.TileOverlays;
import zombie.modding.ActiveMods;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.sandbox.CustomSandboxOptions;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.seams.SeamManager;
import zombie.seating.SeatingManager;
import zombie.spriteModel.SpriteModelManager;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryManager;
import zombie.ui.FPSGraph;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;
import zombie.ui.UIManager;
import zombie.ui.UITextEntryInterface;
import zombie.util.StringUtils;
import zombie.vehicles.VehicleType;
import zombie.worldMap.WorldMap;

public final class Core {
   public static final float PZWorldToBulletZScale = 2.46F;
   public static final float characterHeight = 0.6F;
   public static final boolean bDemo = false;
   public static boolean bTutorial;
   public static int dirtyGlobalLightsCount = 0;
   private static boolean fakefullscreen = false;
   private static final GameVersion gameVersion = new GameVersion(42, 0, "");
   private static final int buildVersion = 0;
   public String steamServerVersion = "1.0.0.0";
   public static boolean bAltMoveMethod = false;
   private final ColorInfo objectHighlitedColor = new ColorInfo(0.98F, 0.56F, 0.11F, 1.0F);
   private final ColorInfo workstationHighlitedColor = new ColorInfo(0.56F, 0.98F, 0.11F, 1.0F);
   private final ColorInfo goodHighlitedColor = new ColorInfo(0.0F, 1.0F, 0.0F, 1.0F);
   private final ColorInfo badHighlitedColor = new ColorInfo(1.0F, 0.0F, 0.0F, 1.0F);
   private boolean flashIsoCursor = false;
   private final ColorInfo targetColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
   private final ColorInfo noTargetColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
   private final ArrayList<ConfigOption> options = new ArrayList();
   private final HashMap<String, ConfigOption> optionByName = new HashMap();
   private final ArrayList<ConfigOption> fakeOptions = new ArrayList();
   private final HashMap<String, ConfigOption> fakeOptionByName = new HashMap();
   private String selectedMap;
   private final IntegerConfigOption OptionReticleMode = this.newOption("reticleMode", 0, 1, 0);
   private final BooleanConfigOption OptionShowAimTexture = this.newOption("showAimTexture", false);
   private final BooleanConfigOption OptionShowReticleTexture = this.newOption("showReticleTexture", true);
   private final BooleanConfigOption OptionShowValidTargetReticleTexture = this.newOption("showValidTargetReticleTexture", true);
   private final IntegerConfigOption OptionAimTextureIndex = this.newOption("aimTextureIndex", 0, 17, 0);
   private final IntegerConfigOption OptionReticleTextureIndex = this.newOption("reticleTextureIndex", 0, 6, 0);
   private final IntegerConfigOption OptionValidTargetReticleTextureIndex = this.newOption("validTargetReticleTextureIndex", 0, 6, 3);
   private final IntegerConfigOption OptionCrosshairTextureIndex = this.newOption("crosshairTextureIndex", 0, 2, 0);
   private final IntegerConfigOption OptionMaxCrosshairOffset = this.newOption("maxCrosshairOffset", 0, 11, 5);
   private final BooleanConfigOption OptionReticleCameraZoom = this.newOption("reticleCameraZoom", false);
   private final ArrayConfigOption OptionTargetColor = this.newOption("targetColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "1.0,1.0,1.0").setFixedSize(3);
   private final ArrayConfigOption OptionNoTargetColor = this.newOption("noTargetColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "1.0,1.0,1.0").setFixedSize(3);
   private final ArrayConfigOption OptionObjectHighlightColor = this.newOption("objHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.98,0.56,0.11").setFixedSize(3);
   private final ArrayConfigOption OptionWorkstationHighlightColor = this.newOption("workstationHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.56,0.98,0.11").setFixedSize(3);
   private final ArrayConfigOption OptionGoodHighlightColor = this.newOption("goodHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.0,1.0,0.0").setFixedSize(3);
   private final ArrayConfigOption OptionBadHighlightColor = this.newOption("badHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "1.0,0.0,0.0").setFixedSize(3);
   private final IntegerConfigOption isoCursorVisibility = this.newOption("iso_cursor", 0, 6, 5);
   private final BooleanConfigOption OptionShowCursorWhileAiming = this.newOption("showCursorWhileAiming", false);
   private boolean collideZombies = true;
   public final MultiTextureFBO2 OffscreenBuffer = new MultiTextureFBO2();
   private String saveFolder = null;
   private final BooleanConfigOption OptionZoom = this.newOption("zoom", true);
   public static boolean OptionModsEnabled = true;
   private final IntegerConfigOption OptionFontSize = this.newOption("fontSize", 1, 6, 6);
   private final IntegerConfigOption OptionMoodleSize = this.newOption("moodleSize", 1, 7, 7);
   private final IntegerConfigOption OptionActionProgressBarSize = this.newOption("actionProgressBarSize", 1, 4, 1);
   private final StringConfigOption OptionContextMenuFont = this.newOption("contextMenuFont", "Medium", new String[]{"Small", "Medium", "Large"});
   private final StringConfigOption OptionInventoryFont = this.newOption("inventoryFont", "Medium", new String[]{"Small", "Medium", "Large"});
   private final IntegerConfigOption OptionInventoryContainerSize = this.newOption("inventoryContainerSize", 1, 3, 1);
   private final StringConfigOption OptionTooltipFont = this.newOption("tooltipFont", "Small", new String[]{"Small", "Medium", "Large"});
   private final BooleanConfigOption OptionColorblindPatterns = this.newOption("colorblindPatterns", false);
   private final BooleanConfigOption OptionEnableDyslexicFont = this.newOption("enableDyslexicFont", false);
   private final BooleanConfigOption OptionLightSensitivity = this.newOption("enableLightSensitivity", false);
   private final StringConfigOption OptionMeasurementFormat = this.newOption("measurementsFormat", "Metric", new String[]{"Imperial", "Metric"});
   private final IntegerConfigOption OptionClockFormat = this.newOption("clockFormat", 1, 2, 1);
   private final IntegerConfigOption OptionClockSize = this.newOption("clockSize", 1, 2, 2);
   private final BooleanConfigOption OptionClock24Hour = this.newOption("clock24Hour", true);
   private final BooleanConfigOption OptionVSync = this.newOption("vsync", false);
   private final IntegerConfigOption OptionSoundVolume = this.newOption("soundVolume", 0, 10, 8);
   private final IntegerConfigOption OptionMusicVolume = this.newOption("musicVolume", 0, 10, 6);
   private final IntegerConfigOption OptionAmbientVolume = this.newOption("ambientVolume", 0, 10, 5);
   private final IntegerConfigOption OptionJumpScareVolume = this.newOption("jumpScareVolume", 0, 10, 10);
   private final IntegerConfigOption OptionMusicActionStyle = this.newOption("musicActionStyle", 1, 2, 1);
   private final IntegerConfigOption OptionMusicLibrary = this.newOption("musicLibrary", 1, 3, 1);
   private final BooleanConfigOption OptionVoiceEnable = this.newOption("voiceEnable", true);
   private final IntegerConfigOption OptionVoiceMode = this.newOption("voiceMode", 1, 3, 3);
   private final IntegerConfigOption OptionVoiceVADMode = this.newOption("voiceVADMode", 1, 4, 3);
   private final IntegerConfigOption OptionVoiceAGCMode = this.newOption("voiceAGCMode", 1, 3, 2);
   private final StringConfigOption OptionVoiceRecordDeviceName = this.newOption("voiceRecordDeviceName", "", 256);
   private final IntegerConfigOption OptionVoiceVolumeMic = this.newOption("voiceVolumeMic", 0, 10, 10);
   private final IntegerConfigOption OptionVoiceVolumePlayers = this.newOption("voiceVolumePlayers", 0, 10, 5);
   private final IntegerConfigOption OptionVehicleEngineVolume = this.newOption("vehicleEngineVolume", 0, 10, 5);
   private final BooleanConfigOption OptionStreamerMode = this.newOption("vehicleStreamerMode", false);
   private final IntegerConfigOption OptionReloadDifficulty = this.newOption("reloadDifficulty", 1, 3, 2);
   private final BooleanConfigOption OptionRackProgress = this.newOption("rackProgress", true);
   private final IntegerConfigOption OptionBloodDecals = this.newOption("bloodDecals", 0, 10, 10);
   private final BooleanConfigOption OptionFocusloss = this.newOption("focusloss", false);
   private final BooleanConfigOption OptionBorderlessWindow = this.newOption("borderless", false);
   private final BooleanConfigOption OptionLockCursorToWindow = this.newOption("lockCursorToWindow", false);
   private final BooleanConfigOption OptionTextureCompression = this.newOption("textureCompression", true);
   private final BooleanConfigOption OptionModelTextureMipmaps = this.newOption("modelTextureMipmaps", false);
   private final BooleanConfigOption OptionTexture2x = this.newOption("texture2x", true);
   private final BooleanConfigOption OptionHighResPlacedItems = this.newOption("highResPlacedItems", true);
   private final IntegerConfigOption OptionMaxTextureSize = this.newOption("maxTextureSize", 1, 4, 1);
   private final IntegerConfigOption OptionMaxVehicleTextureSize = this.newOption("maxVehicleTextureSize", 1, 4, 2);
   private final ArrayConfigOption OptionZoomLevels1x = this.newOption("zoomLevels1x", new IntegerConfigOption("element", 25, 250, 100), ";", "");
   private final ArrayConfigOption OptionZoomLevels2x = this.newOption("zoomLevels2x", new IntegerConfigOption("element", 25, 250, 100), ";", "");
   private final BooleanConfigOption OptionEnableContentTranslations = this.newOption("contentTranslationsEnabled", true);
   private final BooleanConfigOption OptionUIFBO = this.newOption("uiRenderOffscreen", true);
   private final IntegerConfigOption OptionUIRenderFPS = this.newOption("uiRenderFPS", 10, 30, 20);
   private final BooleanConfigOption OptionRadialMenuKeyToggle = this.newOption("radialMenuKeyToggle", true);
   private final BooleanConfigOption OptionReloadRadialInstant = this.newOption("reloadRadialInstant", false);
   private final BooleanConfigOption OptionPanCameraWhileAiming = this.newOption("panCameraWhileAiming", true);
   private final BooleanConfigOption OptionPanCameraWhileDriving = this.newOption("panCameraWhileDriving", false);
   private final BooleanConfigOption OptionShowChatTimestamp = this.newOption("showChatTimestamp", false);
   private final BooleanConfigOption OptionShowChatTitle = this.newOption("showChatTitle", false);
   private final StringConfigOption OptionChatFontSize = this.newOption("chatFontSize", "medium", new String[]{"small", "medium", "large"});
   private final DoubleConfigOption OptionMinChatOpaque = this.newOption("minChatOpaque", 0.0, 1.0, 1.0);
   private final DoubleConfigOption OptionMaxChatOpaque = this.newOption("maxChatOpaque", 0.0, 1.0, 1.0);
   private final DoubleConfigOption OptionChatFadeTime = this.newOption("chatFadeTime", 0.0, 10.0, 0.0);
   private final BooleanConfigOption OptionChatOpaqueOnFocus = this.newOption("chatOpaqueOnFocus", true);
   private final BooleanConfigOption OptionTemperatureDisplayCelsius = this.newOption("temperatureDisplayCelsius", false);
   private final BooleanConfigOption OptionDoVideoEffects = this.newOption("doVideoEffects", true);
   private final BooleanConfigOption OptionDoWindSpriteEffects = this.newOption("doWindSpriteEffects", true);
   private final BooleanConfigOption OptionDoDoorSpriteEffects = this.newOption("doDoorSpriteEffects", true);
   private final BooleanConfigOption OptionDoContainerOutline = this.newOption("doContainerOutline", true);
   private final BooleanConfigOption OptionRenderPrecipIndoors = this.newOption("renderPrecipIndoors", true);
   private final DoubleConfigOption OptionPrecipitationSpeedMultiplier = this.newOption("precipitationSpeedMultiplier", 0.01, 1.0, 1.0);
   private final BooleanConfigOption OptionAutoProneAtk = this.newOption("autoProneAtk", true);
   private final BooleanConfigOption Option3DGroundItem = this.newOption("3DGroundItem", true);
   private final IntegerConfigOption OptionRenderPrecipitation = this.newOption("renderPrecipitation", 1, 3, 1);
   private final BooleanConfigOption OptiondblTapJogToSprint = this.newOption("dblTapJogToSprint", false);
   private final BooleanConfigOption OptionMeleeOutline = this.newOption("meleeOutline", false);
   private final StringConfigOption OptionCycleContainerKey = this.newOption("cycleContainerKey", "shift", new String[]{"control", "shift", "control+shift"});
   private final BooleanConfigOption OptionDropItemsOnSquareCenter = this.newOption("dropItemsOnSquareCenter", false);
   private final BooleanConfigOption OptionTimedActionGameSpeedReset = this.newOption("timedActionGameSpeedReset", false);
   private final IntegerConfigOption OptionShoulderButtonContainerSwitch = this.newOption("shoulderButtonContainerSwitch", 1, 3, 1);
   private final IntegerConfigOption OptionControllerButtonStyle = this.newOption("controllerButtonStyle", 1, 2, 1);
   private final BooleanConfigOption OptionProgressBar = this.newOption("progressBar", false);
   private final StringConfigOption OptionLanguageName = this.newOption("language", "", 64);
   private final ArrayConfigOption OptionSingleContextMenu = this.newOption("singleContextMenu", new BooleanConfigOption("element", false), ",", "").setFixedSize(4);
   private final BooleanConfigOption OptionCorpseShadows = this.newOption("corpseShadows", true);
   private final IntegerConfigOption OptionSimpleClothingTextures = this.newOption("simpleClothingTextures", 1, 3, 1);
   private final BooleanConfigOption OptionSimpleWeaponTextures = this.newOption("simpleWeaponTextures", false);
   private final BooleanConfigOption OptionAutoDrink = this.newOption("autoDrink", true);
   private final BooleanConfigOption OptionLeaveKeyInIgnition = this.newOption("leaveKeyInIgnition", false);
   private final BooleanConfigOption OptionAutoWalkContainer = this.newOption("autoWalkContainer", false);
   private final IntegerConfigOption OptionSearchModeOverlayEffect = this.newOption("searchModeOverlayEffect", 1, 4, 1);
   private final IntegerConfigOption OptionIgnoreProneZombieRange = this.newOption("ignoreProneZombieRange", 1, 5, 2);
   private final BooleanConfigOption OptionShowItemModInfo = this.newOption("showItemModInfo", true);
   private final BooleanConfigOption OptionShowSurvivalGuide = this.newOption("showSurvivalGuide", true);
   private final BooleanConfigOption OptionShowFirstAnimalZoneInfo = this.newOption("showFirstAnimalZoneInfo", true);
   private final BooleanConfigOption OptionEnableLeftJoystickRadialMenu = this.newOption("enableLeftJoystickRadialMenu", true);
   private boolean showPing = true;
   private boolean forceSnow = false;
   private boolean zombieGroupSound = true;
   private String blinkingMoodle = null;
   private String poisonousBerry = null;
   private String poisonousMushroom = null;
   private static String difficulty = "Hardcore";
   public static int TileScale = 2;
   private boolean isSelectingAll = false;
   private final BooleanConfigOption showYourUsername = this.newOption("showYourUsername", true);
   private ColorInfo mpTextColor = null;
   private final ArrayConfigOption OptionMPTextColor = this.newOption("mpTextColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "").setFixedSize(3);
   private boolean isAzerty = false;
   private final StringConfigOption seenUpdateText = this.newOption("seenNews", "", 64);
   private final BooleanConfigOption toggleToAim = this.newOption("toggleToAim", false);
   private final BooleanConfigOption toggleToRun = this.newOption("toggleToRun", false);
   private final BooleanConfigOption toggleToSprint = this.newOption("toggleToSprint", true);
   private final BooleanConfigOption celsius = this.newOption("celsius", false);
   private boolean noSave = false;
   private boolean showFirstTimeVehicleTutorial = false;
   private boolean showFirstTimeWeatherTutorial = false;
   private boolean bAnimPopupDone = false;
   private boolean bModsPopupDone = false;
   public static float blinkAlpha = 1.0F;
   public static boolean blinkAlphaIncrease = false;
   private boolean bLoadedOptions = false;
   private static final HashMap<String, Object> optionsOnStartup = new HashMap();
   public boolean animalCheat = false;
   public boolean displayPlayerModel = true;
   public boolean displayCursor = true;
   public final MatrixStack projectionMatrixStack = new MatrixStack(5889);
   public final MatrixStack modelViewMatrixStack = new MatrixStack(5888);
   public static final Vector3f UnitVector3f = new Vector3f(1.0F, 1.0F, 1.0F);
   public static final Vector3f _UNIT_Z = new Vector3f(0.0F, 0.0F, 1.0F);
   private boolean bChallenge;
   public static int width = 1280;
   public static int height = 720;
   public static float initialWidth = 1280.0F;
   public static float initialHeight = 720.0F;
   public static int MaxJukeBoxesActive = 10;
   public static int NumJukeBoxesActive = 0;
   public static String GameMode = "Sandbox";
   public static String Preset = "Apocalypse";
   private static String glVersion;
   private static int glMajorVersion = -1;
   private static final Core core = new Core();
   public static boolean bDebug = false;
   public static boolean bUseViewports = false;
   public static boolean bUseGameViewport = true;
   public static boolean bImGui = false;
   public static UITextEntryInterface CurrentTextEntryBox = null;
   private Map<String, Integer> keyMaps = null;
   private Map<String, Integer> altKeyMaps = null;
   public final boolean bUseShaders = true;
   private int iPerfSkybox = 1;
   private final IntegerConfigOption iPerfSkybox_new = this.newOption("perfSkybox", 0, 2, 1);
   public static final int iPerfSkybox_High = 0;
   public static final int iPerfSkybox_Medium = 1;
   public static final int iPerfSkybox_Static = 2;
   private int iPerfPuddles = 0;
   private final IntegerConfigOption iPerfPuddles_new = this.newOption("perfPuddles", 0, 3, 0);
   public static final int iPerfPuddles_None = 3;
   public static final int iPerfPuddles_GroundOnly = 2;
   public static final int iPerfPuddles_GroundWithRuts = 1;
   public static final int iPerfPuddles_All = 0;
   private boolean bPerfReflections = true;
   private final BooleanConfigOption bPerfReflections_new = this.newOption("bPerfReflections", true);
   public int vidMem = 3;
   private boolean bSupportsFBO = true;
   public float UIRenderAccumulator = 0.0F;
   public boolean UIRenderThisFrame = true;
   public int version = 1;
   public int fileversion = 7;
   private final ArrayConfigOption OptionActiveControllerGUIDs = this.newFakeOption("controller", new StringConfigOption("element", "", 256), ",", "").setMultiLine(true);
   private final BooleanConfigOption OptionDoneNewSaveFolder = this.newFakeOption("doneNewSaveFolder", false);
   private final IntegerConfigOption OptionFogQuality = this.newFakeOption("fogQuality", 0, 2, 0);
   private final BooleanConfigOption OptionGotNewBelt = this.newFakeOption("gotNewBelt", false);
   private final IntegerConfigOption OptionLightingFPS = this.newFakeOption("lightFPS", 5, 60, 15);
   private final IntegerConfigOption OptionLightingFrameSkip = this.newFakeOption("lighting", 0, 3, 0);
   private final IntegerConfigOption OptionLockFPS = this.newFakeOption("frameRate", 24, 244, 60);
   private final IntegerConfigOption OptionPuddlesQuality = this.newFakeOption("puddles", 0, 2, 0);
   private final BooleanConfigOption OptionRiversideDone = this.newFakeOption("riversideDone", false);
   private final BooleanConfigOption OptionRosewoodSpawnDone = this.newFakeOption("rosewoodSpawnDone", false);
   private final IntegerConfigOption OptionScreenHeight = this.newFakeOption("height", 0, 16384, 720);
   private final IntegerConfigOption OptionScreenWidth = this.newFakeOption("width", 0, 16384, 1280);
   private final BooleanConfigOption OptionShowFirstTimeSearchTutorial = this.newFakeOption("showFirstTimeSearchTutorial", true);
   private final BooleanConfigOption OptionShowFirstTimeSneakTutorial = this.newFakeOption("showFirstTimeSneakTutorial", true);
   private final IntegerConfigOption OptionTermsOfServiceVersion = this.newFakeOption("termsOfServiceVersion", -1, 1000, -1);
   private final BooleanConfigOption OptionTieredZombieUpdates = this.newFakeOption("tieredZombieUpdates", true);
   private final BooleanConfigOption OptionTutorialDone = this.newFakeOption("tutorialDone", false);
   private final BooleanConfigOption OptionUpdateSneakButton = this.newFakeOption("updateSneakButton", true);
   private final BooleanConfigOption OptionUncappedFPS = this.newFakeOption("uncappedFPS", true);
   private final BooleanConfigOption OptionVehiclesWarningShow = this.newFakeOption("vehiclesWarningShow", false);
   private final IntegerConfigOption OptionWaterQuality = this.newFakeOption("water", 0, 2, 0);
   private final BooleanConfigOption fullScreen = this.newOption("fullScreen", false);
   private final ArrayConfigOption bAutoZoom = this.newOption("autozoom", new BooleanConfigOption("element", false), ",", "").setFixedSize(4);
   public static String GameMap = "DEFAULT";
   public static String GameSaveWorld = "";
   public static boolean SafeMode = false;
   public static boolean SafeModeForced = false;
   public static boolean SoundDisabled = false;
   public int frameStage = 0;
   private int stack = 0;
   public static int xx = 0;
   public static int yy = 0;
   public static int zz = 0;
   public final HashMap<Integer, Float> FloatParamMap = new HashMap();
   private final Matrix4f tempMatrix4f = new Matrix4f();
   private static final float isoAngle = 62.65607F;
   public static final float ModelScale = 1.5F;
   public static final float scale = (float)(Math.sqrt(2.0) / 2.0 / 10.0) / 1.5F;
   public static boolean bLastStand = false;
   public static String ChallengeID = null;
   public static boolean bExiting = false;
   private String m_delayResetLua_activeMods = null;
   private String m_delayResetLua_reason = null;
   private final String RN = "\r\n";

   public Core() {
   }

   private ArrayConfigOption newOption(String var1, ConfigOption var2, String var3, String var4) {
      ArrayConfigOption var5 = new ArrayConfigOption(var1, var2, var3, var4);
      this.options.add(var5);
      this.optionByName.put(var1, var5);
      return var5;
   }

   private BooleanConfigOption newOption(String var1, boolean var2) {
      BooleanConfigOption var3 = new BooleanConfigOption(var1, var2);
      this.options.add(var3);
      this.optionByName.put(var1, var3);
      return var3;
   }

   private DoubleConfigOption newOption(String var1, double var2, double var4, double var6) {
      DoubleConfigOption var8 = new DoubleConfigOption(var1, var2, var4, var6);
      this.options.add(var8);
      this.optionByName.put(var1, var8);
      return var8;
   }

   private IntegerConfigOption newOption(String var1, int var2, int var3, int var4) {
      IntegerConfigOption var5 = new IntegerConfigOption(var1, var2, var3, var4);
      this.options.add(var5);
      this.optionByName.put(var1, var5);
      return var5;
   }

   private StringConfigOption newOption(String var1, String var2, String[] var3) {
      StringConfigOption var4 = new StringConfigOption(var1, var2, var3);
      this.options.add(var4);
      this.optionByName.put(var1, var4);
      return var4;
   }

   private StringConfigOption newOption(String var1, String var2, int var3) {
      StringConfigOption var4 = new StringConfigOption(var1, var2, var3);
      this.options.add(var4);
      this.optionByName.put(var1, var4);
      return var4;
   }

   private ArrayConfigOption newFakeOption(String var1, ConfigOption var2, String var3, String var4) {
      ArrayConfigOption var5 = new ArrayConfigOption(var1, var2, var3, var4);
      this.fakeOptions.add(var5);
      this.fakeOptionByName.put(var1, var5);
      return var5;
   }

   private BooleanConfigOption newFakeOption(String var1, boolean var2) {
      BooleanConfigOption var3 = new BooleanConfigOption(var1, var2);
      this.fakeOptions.add(var3);
      this.fakeOptionByName.put(var1, var3);
      return var3;
   }

   private DoubleConfigOption newFakeOption(String var1, double var2, double var4, double var6) {
      DoubleConfigOption var8 = new DoubleConfigOption(var1, var2, var4, var6);
      this.fakeOptions.add(var8);
      this.fakeOptionByName.put(var1, var8);
      return var8;
   }

   private IntegerConfigOption newFakeOption(String var1, int var2, int var3, int var4) {
      IntegerConfigOption var5 = new IntegerConfigOption(var1, var2, var3, var4);
      this.fakeOptions.add(var5);
      this.fakeOptionByName.put(var1, var5);
      return var5;
   }

   private StringConfigOption newFakeOption(String var1, String var2, String[] var3) {
      StringConfigOption var4 = new StringConfigOption(var1, var2, var3);
      this.fakeOptions.add(var4);
      this.fakeOptionByName.put(var1, var4);
      return var4;
   }

   private StringConfigOption newFakeOption(String var1, String var2, int var3) {
      StringConfigOption var4 = new StringConfigOption(var1, var2, var3);
      this.fakeOptions.add(var4);
      this.fakeOptionByName.put(var1, var4);
      return var4;
   }

   public int getOptionCount() {
      return this.options.size();
   }

   public ConfigOption getOptionByIndex(int var1) {
      return (ConfigOption)this.options.get(var1);
   }

   public boolean isMultiThread() {
      return true;
   }

   public void setChallenge(boolean var1) {
      this.bChallenge = var1;
   }

   public boolean isChallenge() {
      return this.bChallenge;
   }

   public String getChallengeID() {
      return ChallengeID;
   }

   public boolean getOptionTieredZombieUpdates() {
      return this.OptionTieredZombieUpdates.getValue();
   }

   public void setOptionTieredZombieUpdates(boolean var1) {
      this.OptionTieredZombieUpdates.setValue(var1);
      MovingObjectUpdateScheduler.instance.setEnabled(var1);
   }

   public void setFramerate(int var1) {
      PerformanceSettings.instance.setFramerateUncapped(var1 == 1);
      switch (var1) {
         case 1:
            PerformanceSettings.setLockFPS(60);
            break;
         case 2:
            PerformanceSettings.setLockFPS(244);
            break;
         case 3:
            PerformanceSettings.setLockFPS(240);
            break;
         case 4:
            PerformanceSettings.setLockFPS(165);
            break;
         case 5:
            PerformanceSettings.setLockFPS(144);
            break;
         case 6:
            PerformanceSettings.setLockFPS(120);
            break;
         case 7:
            PerformanceSettings.setLockFPS(95);
            break;
         case 8:
            PerformanceSettings.setLockFPS(90);
            break;
         case 9:
            PerformanceSettings.setLockFPS(75);
            break;
         case 10:
            PerformanceSettings.setLockFPS(60);
            break;
         case 11:
            PerformanceSettings.setLockFPS(55);
            break;
         case 12:
            PerformanceSettings.setLockFPS(45);
            break;
         case 13:
            PerformanceSettings.setLockFPS(30);
            break;
         case 14:
            PerformanceSettings.setLockFPS(24);
      }

   }

   public void setMultiThread(boolean var1) {
   }

   public static boolean isUseGameViewport() {
      return bUseGameViewport && bDebug && bImGui;
   }

   public static boolean isImGui() {
      return bDebug && bImGui;
   }

   public static boolean isUseViewports() {
      return bDebug && bImGui && bUseViewports;
   }

   public boolean loadedShader() {
      return SceneShaderStore.WeatherShader != null;
   }

   public static int getGLMajorVersion() {
      if (glMajorVersion == -1) {
         getOpenGLVersions();
      }

      return glMajorVersion;
   }

   public boolean getUseShaders() {
      return true;
   }

   public int getPerfSkybox() {
      return this.iPerfSkybox_new.getValue();
   }

   public int getPerfSkyboxOnLoad() {
      return this.iPerfSkybox;
   }

   public void setPerfSkybox(int var1) {
      this.iPerfSkybox_new.setValue(var1);
   }

   public boolean getPerfReflections() {
      return this.bPerfReflections_new.getValue();
   }

   public boolean getPerfReflectionsOnLoad() {
      return this.bPerfReflections;
   }

   public void setPerfReflections(boolean var1) {
      this.bPerfReflections_new.setValue(var1);
   }

   public int getPerfPuddles() {
      return this.iPerfPuddles_new.getValue();
   }

   public int getPerfPuddlesOnLoad() {
      return this.iPerfPuddles;
   }

   public void setPerfPuddles(int var1) {
      this.iPerfPuddles_new.setValue(var1);
   }

   public int getVidMem() {
      return SafeMode ? 5 : this.vidMem;
   }

   public void setVidMem(int var1) {
      if (SafeMode) {
         this.vidMem = 5;
      }

      this.vidMem = var1;

      try {
         this.saveOptions();
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public void setUseShaders(boolean var1) {
   }

   public void shadersOptionChanged() {
      RenderThread.invokeOnRenderContext(() -> {
         if (!SafeModeForced) {
            SceneShaderStore.shaderOptionsChanged();
         }

      });
   }

   public void initGlobalShader() {
      SceneShaderStore.initGlobalShader();
   }

   public void initShaders() {
      SceneShaderStore.initShaders();
      IsoPuddles.getInstance();
      IsoWater.getInstance();
   }

   public static String getGLVersion() {
      if (glVersion == null) {
         getOpenGLVersions();
      }

      return glVersion;
   }

   public String getGameMode() {
      return GameMode;
   }

   public static Core getInstance() {
      return core;
   }

   public static void getOpenGLVersions() {
      glVersion = GL11.glGetString(7938);
      glMajorVersion = glVersion.charAt(0) - 48;
   }

   public boolean getDebug() {
      return bDebug;
   }

   public static void setFullScreen(boolean var0) {
      getInstance().fullScreen.setValue(var0);
   }

   public static int[] flipPixels(int[] var0, int var1, int var2) {
      int[] var3 = null;
      if (var0 != null) {
         var3 = new int[var1 * var2];

         for(int var4 = 0; var4 < var2; ++var4) {
            for(int var5 = 0; var5 < var1; ++var5) {
               var3[(var2 - var4 - 1) * var1 + var5] = var0[var4 * var1 + var5];
            }
         }
      }

      return var3;
   }

   public void TakeScreenshot() {
      this.TakeScreenshot(256, 256, 1028);
   }

   public void TakeScreenshot(int var1, int var2, int var3) {
      byte var4 = 0;
      int var5 = IsoCamera.getScreenWidth(var4);
      int var6 = IsoCamera.getScreenHeight(var4);
      var1 = PZMath.min(var1, var5);
      var2 = PZMath.min(var2, var6);
      int var7 = IsoCamera.getScreenLeft(var4) + var5 / 2 - var1 / 2;
      int var8 = IsoCamera.getScreenTop(var4) + var6 / 2 - var2 / 2;
      this.TakeScreenshot(var7, var8, var1, var2, var3);
   }

   public void TakeScreenshot(int var1, int var2, int var3, int var4, int var5) {
      GL11.glPixelStorei(3333, 1);
      GL11.glReadBuffer(var5);
      byte var6 = 3;
      ByteBuffer var7 = MemoryUtil.memAlloc(var3 * var4 * var6);
      GL11.glReadPixels(var1, var2, var3, var4, 6407, 5121, var7);
      int[] var8 = new int[var3 * var4];
      File var10 = ZomboidFileSystem.instance.getFileInCurrentSave("thumb.png");
      String var11 = "png";

      for(int var12 = 0; var12 < var8.length; ++var12) {
         int var9 = var12 * 3;
         var8[var12] = -16777216 | (var7.get(var9) & 255) << 16 | (var7.get(var9 + 1) & 255) << 8 | (var7.get(var9 + 2) & 255) << 0;
      }

      MemoryUtil.memFree(var7);
      var8 = flipPixels(var8, var3, var4);
      BufferedImage var15 = new BufferedImage(var3, var4, 2);
      var15.setRGB(0, 0, var3, var4, var8, 0, var3);

      try {
         ImageIO.write(var15, "png", var10);
      } catch (IOException var14) {
         var14.printStackTrace();
      }

      Texture.reload(ZomboidFileSystem.instance.getFileNameInCurrentSave("thumb.png"));
   }

   public void TakeFullScreenshot(String var1) {
      RenderThread.invokeOnRenderContext(var1, (var0) -> {
         GL11.glPixelStorei(3333, 1);
         GL11.glReadBuffer(1028);
         int var1 = Display.getDisplayMode().getWidth();
         int var2 = Display.getDisplayMode().getHeight();
         byte var3 = 0;
         byte var4 = 0;
         byte var5 = 3;
         ByteBuffer var6 = MemoryUtil.memAlloc(var1 * var2 * var5);
         GL11.glReadPixels(var3, var4, var1, var2, 6407, 5121, var6);
         int[] var7 = new int[var1 * var2];
         if (var0 == null) {
            SimpleDateFormat var9 = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            var0 = "screenshot_" + var9.format(Calendar.getInstance().getTime()) + ".png";
         }

         String var10002 = ZomboidFileSystem.instance.getScreenshotDir();
         File var13 = new File(var10002 + File.separator + var0);

         for(int var10 = 0; var10 < var7.length; ++var10) {
            int var8 = var10 * 3;
            var7[var10] = -16777216 | (var6.get(var8) & 255) << 16 | (var6.get(var8 + 1) & 255) << 8 | (var6.get(var8 + 2) & 255) << 0;
         }

         MemoryUtil.memFree(var6);
         var7 = flipPixels(var7, var1, var2);
         BufferedImage var14 = new BufferedImage(var1, var2, 2);
         var14.setRGB(0, 0, var1, var2, var7, 0, var1);

         try {
            ImageIO.write(var14, "png", var13);
         } catch (IOException var12) {
            var12.printStackTrace();
         }

      });
   }

   public static boolean supportNPTTexture() {
      return false;
   }

   public boolean supportsFBO() {
      if (SafeMode) {
         this.OffscreenBuffer.bZoomEnabled = false;
         return false;
      } else if (!this.bSupportsFBO) {
         return false;
      } else if (this.OffscreenBuffer.Current != null) {
         return true;
      } else {
         try {
            if (TextureFBO.checkFBOSupport() && this.setupMultiFBO()) {
               return true;
            } else {
               this.bSupportsFBO = false;
               SafeMode = true;
               this.OffscreenBuffer.bZoomEnabled = false;
               return false;
            }
         } catch (Exception var2) {
            var2.printStackTrace();
            this.bSupportsFBO = false;
            SafeMode = true;
            this.OffscreenBuffer.bZoomEnabled = false;
            return false;
         }
      }
   }

   private void sharedInit() {
      this.supportsFBO();
   }

   public void MoveMethodToggle() {
      bAltMoveMethod = !bAltMoveMethod;
   }

   public void EndFrameText(int var1) {
      if (!LuaManager.thread.bStep) {
         if (this.OffscreenBuffer.Current != null) {
         }

         IndieGL.glDoEndFrame();
         this.frameStage = 2;
      }
   }

   public void EndFrame(int var1) {
      if (!LuaManager.thread.bStep) {
         if (this.OffscreenBuffer.Current != null) {
            SpriteRenderer.instance.glBuffer(0, var1);
         }

         IndieGL.glDoEndFrame();
         this.frameStage = 2;
      }
   }

   public void EndFrame() {
      IndieGL.glDoEndFrame();
      if (this.OffscreenBuffer.Current != null) {
         SpriteRenderer.instance.glBuffer(0, 0);
      }

   }

   public void EndFrameUI() {
      if (!blinkAlphaIncrease) {
         blinkAlpha -= 0.07F * GameTime.getInstance().getThirtyFPSMultiplier();
         if (blinkAlpha < 0.15F) {
            blinkAlpha = 0.15F;
            blinkAlphaIncrease = true;
         }
      } else {
         blinkAlpha += 0.07F * GameTime.getInstance().getThirtyFPSMultiplier();
         if (blinkAlpha > 1.0F) {
            blinkAlpha = 1.0F;
            blinkAlphaIncrease = false;
         }
      }

      if (UIManager.useUIFBO && UIManager.UIFBO == null) {
         UIManager.CreateFBO(width, height);
      }

      if (LuaManager.thread != null && LuaManager.thread.bStep) {
         SpriteRenderer.instance.clearSprites();
      } else {
         ExceptionLogger.render();
         if (UIManager.useUIFBO) {
            if (this.UIRenderThisFrame) {
               UIManager.UITextureContentsValid = true;
               SpriteRenderer.instance.glBuffer(3, 0);
               IndieGL.glDoEndFrame();
               SpriteRenderer.instance.stopOffscreenUI();
               IndieGL.glDoStartFrame(width, height, 1.0F, -1);
               float var1 = (float)((int)(1.0F / (float)this.getOptionUIRenderFPS() * 100.0F)) / 100.0F;
               int var2 = (int)(this.UIRenderAccumulator / var1);
               this.UIRenderAccumulator -= (float)var2 * var1;
               if (FPSGraph.instance != null) {
                  FPSGraph.instance.addUI(System.currentTimeMillis());
               }
            }
         } else {
            UIManager.UITextureContentsValid = false;
         }

         if (UIManager.useUIFBO && UIManager.UITextureContentsValid) {
            SpriteRenderer.instance.setDoAdditive(true);
            SpriteRenderer.instance.renderi((Texture)UIManager.UIFBO.getTexture(), 0, height, width, -height, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
            SpriteRenderer.instance.setDoAdditive(false);
         } else if (UIManager.useUIFBO) {
            UIManager.renderFadeOverlay();
         }

         if (getInstance().getOptionLockCursorToWindow()) {
            Mouse.renderCursorTexture();
         }

         ImDrawData var3 = Display.imguiEndFrame();
         if (bDebug && var3 != null) {
            SpriteRenderer.instance.render(var3);
         }

         IndieGL.glDoEndFrame();
         RenderThread.Ready();
         this.frameStage = 0;
      }
   }

   public static void UnfocusActiveTextEntryBox() {
      if (CurrentTextEntryBox != null && !CurrentTextEntryBox.getUIName().contains("chat text entry")) {
         CurrentTextEntryBox.setDoingTextEntry(false);
         if (CurrentTextEntryBox.getFrame() != null) {
            CurrentTextEntryBox.getFrame().Colour = CurrentTextEntryBox.getStandardFrameColour();
         }

         CurrentTextEntryBox = null;
      }

   }

   public int getOffscreenWidth(int var1) {
      if (this.OffscreenBuffer == null) {
         return IsoPlayer.numPlayers > 1 ? this.getScreenWidth() / 2 : this.getScreenWidth();
      } else {
         return this.OffscreenBuffer.getWidth(var1);
      }
   }

   public int getOffscreenHeight(int var1) {
      if (this.OffscreenBuffer == null) {
         return IsoPlayer.numPlayers > 2 ? this.getScreenHeight() / 2 : this.getScreenHeight();
      } else {
         return this.OffscreenBuffer.getHeight(var1);
      }
   }

   public int getOffscreenTrueWidth() {
      return this.OffscreenBuffer != null && this.OffscreenBuffer.Current != null ? this.OffscreenBuffer.getTexture(0).getWidth() : this.getScreenWidth();
   }

   public int getOffscreenTrueHeight() {
      return this.OffscreenBuffer != null && this.OffscreenBuffer.Current != null ? this.OffscreenBuffer.getTexture(0).getHeight() : this.getScreenHeight();
   }

   public int getScreenHeight() {
      return height;
   }

   public int getScreenWidth() {
      return width;
   }

   public void setResolutionAndFullScreen(int var1, int var2, boolean var3) {
      if (!isUseGameViewport()) {
         setDisplayMode(var1, var2, var3);
         this.setScreenSize(Display.getWidth(), Display.getHeight());
      } else {
         this.setScreenSize(var1, var2);
      }

   }

   public void setResolution(String var1) {
      String[] var2 = var1.split("x");
      int var3 = Integer.parseInt(var2[0].trim());
      int var4 = Integer.parseInt(var2[1].trim());
      if (!isUseGameViewport()) {
         if (this.fullScreen.getValue()) {
            setDisplayMode(var3, var4, true);
         } else {
            setDisplayMode(var3, var4, false);
         }

         this.setScreenSize(Display.getWidth(), Display.getHeight());
      } else {
         this.setScreenSize(var3, var4);
      }

      try {
         this.saveOptions();
      } catch (IOException var6) {
         var6.printStackTrace();
      }

   }

   public boolean loadOptions_OLD() throws IOException {
      this.bLoadedOptions = false;
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var1 = new File(var10002 + File.separator + "options.ini");
      if (!var1.exists()) {
         this.initOptionsINI();
         return false;
      } else {
         this.bLoadedOptions = true;

         for(int var2 = 0; var2 < 4; ++var2) {
            this.setAutoZoom(var2, false);
         }

         this.OptionLanguageName.setValue("");
         BufferedReader var14 = new BufferedReader(new FileReader(var1));

         try {
            String var3;
            while((var3 = var14.readLine()) != null) {
               if (var3.startsWith("version=")) {
                  this.version = Integer.valueOf(var3.replaceFirst("version=", ""));
               } else if (var3.startsWith("width=")) {
                  width = Integer.valueOf(var3.replaceFirst("width=", ""));
               } else if (var3.startsWith("height=")) {
                  height = Integer.valueOf(var3.replaceFirst("height=", ""));
               } else if (var3.startsWith("fullScreen=")) {
                  this.fullScreen.parse(var3.replaceFirst("fullScreen=", ""));
               } else if (var3.startsWith("frameRate=")) {
                  PerformanceSettings.setLockFPS(Integer.parseInt(var3.replaceFirst("frameRate=", "")));
               } else if (var3.startsWith("uncappedFPS=")) {
                  PerformanceSettings.instance.setFramerateUncapped(Boolean.parseBoolean(var3.replaceFirst("uncappedFPS=", "")));
               } else if (var3.startsWith("iso_cursor=")) {
                  this.isoCursorVisibility.parse(var3.replaceFirst("iso_cursor=", ""));
               } else if (var3.startsWith("showCursorWhileAiming=")) {
                  this.OptionShowCursorWhileAiming.parse(var3.replaceFirst("showCursorWhileAiming=", ""));
               } else if (var3.startsWith("water=")) {
                  PerformanceSettings.WaterQuality = Integer.parseInt(var3.replaceFirst("water=", ""));
               } else if (var3.startsWith("puddles=")) {
                  PerformanceSettings.PuddlesQuality = Integer.parseInt(var3.replaceFirst("puddles=", ""));
               } else if (var3.startsWith("lighting=")) {
                  PerformanceSettings.LightingFrameSkip = Integer.parseInt(var3.replaceFirst("lighting=", ""));
               } else if (var3.startsWith("lightFPS=")) {
                  PerformanceSettings.instance.setLightingFPS(Integer.parseInt(var3.replaceFirst("lightFPS=", "")));
               } else if (var3.startsWith("perfSkybox=")) {
                  this.iPerfSkybox_new.parse(var3.replaceFirst("perfSkybox=", ""));
                  this.iPerfSkybox = this.iPerfSkybox_new.getValue();
               } else if (var3.startsWith("perfPuddles=")) {
                  this.iPerfPuddles_new.parse(var3.replaceFirst("perfPuddles=", ""));
                  this.iPerfPuddles = this.iPerfPuddles_new.getValue();
               } else if (var3.startsWith("bPerfReflections=")) {
                  this.bPerfReflections_new.parse(var3.replaceFirst("bPerfReflections=", ""));
                  this.bPerfReflections = this.bPerfReflections_new.getValue();
               } else if (var3.startsWith("language=")) {
                  this.OptionLanguageName.parse(var3.replaceFirst("language=", "").trim());
               } else if (var3.startsWith("zoom=")) {
                  this.OptionZoom.parse(var3.replaceFirst("zoom=", ""));
               } else if (var3.startsWith("autozoom=")) {
                  this.bAutoZoom.parse(var3.replaceFirst("autozoom=", "").trim());

                  for(int var17 = 0; var17 < 4; ++var17) {
                     this.setAutoZoom(var17, this.getAutoZoom(var17));
                  }
               } else if (var3.startsWith("fontSize=")) {
                  this.setOptionFontSize(Integer.parseInt(var3.replaceFirst("fontSize=", "").trim()));
               } else if (var3.startsWith("moodleSize=")) {
                  this.setOptionMoodleSize(Integer.parseInt(var3.replaceFirst("moodleSize=", "").trim()));
               } else if (var3.startsWith("contextMenuFont=")) {
                  this.OptionContextMenuFont.parse(var3.replaceFirst("contextMenuFont=", "").trim());
               } else if (var3.startsWith("inventoryFont=")) {
                  this.OptionInventoryFont.parse(var3.replaceFirst("inventoryFont=", "").trim());
               } else if (var3.startsWith("inventoryContainerSize=")) {
                  this.OptionInventoryContainerSize.parse(var3.replaceFirst("inventoryContainerSize=", ""));
               } else if (var3.startsWith("tooltipFont=")) {
                  this.OptionTooltipFont.parse(var3.replaceFirst("tooltipFont=", "").trim());
               } else if (var3.startsWith("measurementsFormat=")) {
                  this.OptionMeasurementFormat.parse(var3.replaceFirst("measurementsFormat=", "").trim());
               } else if (var3.startsWith("clockFormat=")) {
                  this.OptionClockFormat.parse(var3.replaceFirst("clockFormat=", ""));
               } else if (var3.startsWith("clockSize=")) {
                  this.OptionClockSize.parse(var3.replaceFirst("clockSize=", ""));
               } else if (var3.startsWith("clock24Hour=")) {
                  this.OptionClock24Hour.parse(var3.replaceFirst("clock24Hour=", ""));
               } else if (var3.startsWith("vsync=")) {
                  this.OptionVSync.parse(var3.replaceFirst("vsync=", ""));
               } else if (var3.startsWith("voiceEnable=")) {
                  this.OptionVoiceEnable.parse(var3.replaceFirst("voiceEnable=", ""));
               } else if (var3.startsWith("voiceMode=")) {
                  this.OptionVoiceMode.parse(var3.replaceFirst("voiceMode=", ""));
               } else if (var3.startsWith("voiceVADMode=")) {
                  this.OptionVoiceVADMode.parse(var3.replaceFirst("voiceVADMode=", ""));
               } else if (var3.startsWith("voiceAGCMode=")) {
                  this.OptionVoiceAGCMode.parse(var3.replaceFirst("voiceAGCMode=", ""));
               } else if (var3.startsWith("voiceVolumeMic=")) {
                  this.OptionVoiceVolumeMic.parse(var3.replaceFirst("voiceVolumeMic=", ""));
               } else if (var3.startsWith("voiceVolumePlayers=")) {
                  this.OptionVoiceVolumePlayers.parse(var3.replaceFirst("voiceVolumePlayers=", ""));
               } else if (var3.startsWith("voiceRecordDeviceName=")) {
                  this.OptionVoiceRecordDeviceName.parse(var3.replaceFirst("voiceRecordDeviceName=", "").trim());
               } else if (var3.startsWith("soundVolume=")) {
                  this.OptionSoundVolume.parse(var3.replaceFirst("soundVolume=", ""));
               } else if (var3.startsWith("musicVolume=")) {
                  this.OptionMusicVolume.parse(var3.replaceFirst("musicVolume=", ""));
               } else if (var3.startsWith("ambientVolume=")) {
                  this.OptionAmbientVolume.parse(var3.replaceFirst("ambientVolume=", ""));
               } else if (var3.startsWith("jumpScareVolume=")) {
                  this.OptionJumpScareVolume.parse(var3.replaceFirst("jumpScareVolume=", ""));
               } else if (var3.startsWith("musicActionStyle=")) {
                  this.OptionMusicActionStyle.parse(var3.replaceFirst("musicActionStyle=", ""));
               } else if (var3.startsWith("musicLibrary=")) {
                  this.OptionMusicLibrary.parse(var3.replaceFirst("musicLibrary=", ""));
               } else if (var3.startsWith("vehicleEngineVolume=")) {
                  this.OptionVehicleEngineVolume.parse(var3.replaceFirst("vehicleEngineVolume=", ""));
               } else if (var3.startsWith("streamerMode=")) {
                  this.OptionStreamerMode.parse(var3.replaceFirst("streamerMode=", ""));
               } else if (var3.startsWith("reloadDifficulty=")) {
                  this.OptionReloadDifficulty.parse(var3.replaceFirst("reloadDifficulty=", ""));
               } else if (var3.startsWith("rackProgress=")) {
                  this.OptionRackProgress.parse(var3.replaceFirst("rackProgress=", ""));
               } else {
                  String var15;
                  if (var3.startsWith("controller=")) {
                     var15 = var3.replaceFirst("controller=", "");
                     if (!var15.isEmpty()) {
                        JoypadManager.instance.setControllerActive(var15, true);
                     }
                  } else if (var3.startsWith("tutorialDone=")) {
                     this.OptionTutorialDone.parse(var3.replaceFirst("tutorialDone=", ""));
                  } else if (var3.startsWith("vehiclesWarningShow=")) {
                     this.OptionVehiclesWarningShow.parse(var3.replaceFirst("vehiclesWarningShow=", ""));
                  } else if (var3.startsWith("bloodDecals=")) {
                     this.setOptionBloodDecals(Integer.parseInt(var3.replaceFirst("bloodDecals=", "")));
                  } else if (var3.startsWith("focusloss=")) {
                     this.OptionFocusloss.parse(var3.replaceFirst("focusloss=", ""));
                  } else if (var3.startsWith("borderless=")) {
                     this.OptionBorderlessWindow.parse(var3.replaceFirst("borderless=", ""));
                  } else if (var3.startsWith("lockCursorToWindow=")) {
                     this.OptionLockCursorToWindow.parse(var3.replaceFirst("lockCursorToWindow=", ""));
                  } else if (var3.startsWith("textureCompression=")) {
                     this.OptionTextureCompression.parse(var3.replaceFirst("textureCompression=", ""));
                  } else if (var3.startsWith("modelTextureMipmaps=")) {
                     this.OptionModelTextureMipmaps.parse(var3.replaceFirst("modelTextureMipmaps=", ""));
                  } else if (var3.startsWith("texture2x=")) {
                     this.OptionTexture2x.parse(var3.replaceFirst("texture2x=", ""));
                  } else if (var3.startsWith("maxTextureSize=")) {
                     this.OptionMaxTextureSize.parse(var3.replaceFirst("maxTextureSize=", ""));
                  } else if (var3.startsWith("maxVehicleTextureSize=")) {
                     this.OptionMaxVehicleTextureSize.parse(var3.replaceFirst("maxVehicleTextureSize=", ""));
                  } else if (var3.startsWith("zoomLevels1x=")) {
                     this.OptionZoomLevels1x.parse(var3.replaceFirst("zoomLevels1x=", ""));
                  } else if (var3.startsWith("zoomLevels2x=")) {
                     this.OptionZoomLevels2x.parse(var3.replaceFirst("zoomLevels2x=", ""));
                  } else if (var3.startsWith("showChatTimestamp=")) {
                     this.OptionShowChatTimestamp.parse(var3.replaceFirst("showChatTimestamp=", ""));
                  } else if (var3.startsWith("showChatTitle=")) {
                     this.OptionShowChatTitle.parse(var3.replaceFirst("showChatTitle=", ""));
                  } else if (var3.startsWith("chatFontSize=")) {
                     this.OptionChatFontSize.parse(var3.replaceFirst("chatFontSize=", "").trim());
                  } else if (var3.startsWith("minChatOpaque=")) {
                     this.OptionMinChatOpaque.parse(var3.replaceFirst("minChatOpaque=", ""));
                  } else if (var3.startsWith("maxChatOpaque=")) {
                     this.OptionMaxChatOpaque.parse(var3.replaceFirst("maxChatOpaque=", ""));
                  } else if (var3.startsWith("chatFadeTime=")) {
                     this.OptionChatFadeTime.parse(var3.replaceFirst("chatFadeTime=", ""));
                  } else if (var3.startsWith("chatOpaqueOnFocus=")) {
                     this.OptionChatOpaqueOnFocus.parse(var3.replaceFirst("chatOpaqueOnFocus=", ""));
                  } else if (var3.startsWith("doneNewSaveFolder=")) {
                     this.OptionDoneNewSaveFolder.parse(var3.replaceFirst("doneNewSaveFolder=", ""));
                  } else if (var3.startsWith("contentTranslationsEnabled=")) {
                     this.OptionEnableContentTranslations.parse(var3.replaceFirst("contentTranslationsEnabled=", ""));
                  } else if (var3.startsWith("showYourUsername=")) {
                     this.showYourUsername.parse(var3.replaceFirst("showYourUsername=", ""));
                  } else if (var3.startsWith("riversideDone=")) {
                     this.OptionRiversideDone.parse(var3.replaceFirst("riversideDone=", ""));
                  } else if (var3.startsWith("rosewoodSpawnDone=")) {
                     this.OptionRosewoodSpawnDone.parse(var3.replaceFirst("rosewoodSpawnDone=", ""));
                  } else if (var3.startsWith("gotNewBelt=")) {
                     this.OptionGotNewBelt.parse(var3.replaceFirst("gotNewBelt=", ""));
                  } else {
                     float var16;
                     float var18;
                     float var19;
                     if (var3.startsWith("mpTextColor=")) {
                        var15 = var3.replaceFirst("mpTextColor=", "").trim();
                        this.OptionMPTextColor.parse(var15);
                        var16 = (float)((DoubleConfigOption)this.OptionMPTextColor.getElement(0)).getValue();
                        var18 = (float)((DoubleConfigOption)this.OptionMPTextColor.getElement(1)).getValue();
                        var19 = (float)((DoubleConfigOption)this.OptionMPTextColor.getElement(2)).getValue();
                        this.mpTextColor = new ColorInfo(var16, var18, var19, 1.0F);
                     } else if (var3.startsWith("objHighlightColor=")) {
                        var15 = var3.replaceFirst("objHighlightColor=", "").trim();
                        this.OptionObjectHighlightColor.parse(var15);
                        var16 = (float)((DoubleConfigOption)this.OptionObjectHighlightColor.getElement(0)).getValue();
                        var18 = (float)((DoubleConfigOption)this.OptionObjectHighlightColor.getElement(1)).getValue();
                        var19 = (float)((DoubleConfigOption)this.OptionObjectHighlightColor.getElement(2)).getValue();
                        this.objectHighlitedColor.set(var16, var18, var19, 1.0F);
                     } else if (var3.startsWith("workstationHighlightColor=")) {
                        var15 = var3.replaceFirst("workstationHighlightColor=", "").trim();
                        this.OptionWorkstationHighlightColor.parse(var15);
                        var16 = (float)((DoubleConfigOption)this.OptionWorkstationHighlightColor.getElement(0)).getValue();
                        var18 = (float)((DoubleConfigOption)this.OptionWorkstationHighlightColor.getElement(1)).getValue();
                        var19 = (float)((DoubleConfigOption)this.OptionWorkstationHighlightColor.getElement(2)).getValue();
                        this.workstationHighlitedColor.set(var16, var18, var19, 1.0F);
                     } else if (var3.startsWith("goodHighlightColor=")) {
                        var15 = var3.replaceFirst("goodHighlightColor=", "").trim();
                        this.OptionGoodHighlightColor.parse(var15);
                        var16 = (float)((DoubleConfigOption)this.OptionGoodHighlightColor.getElement(0)).getValue();
                        var18 = (float)((DoubleConfigOption)this.OptionGoodHighlightColor.getElement(1)).getValue();
                        var19 = (float)((DoubleConfigOption)this.OptionGoodHighlightColor.getElement(2)).getValue();
                        this.goodHighlitedColor.set(var16, var18, var19, 1.0F);
                     } else if (var3.startsWith("badHighlightColor=")) {
                        var15 = var3.replaceFirst("badHighlightColor=", "").trim();
                        this.OptionBadHighlightColor.parse(var15);
                        var16 = (float)((DoubleConfigOption)this.OptionBadHighlightColor.getElement(0)).getValue();
                        var18 = (float)((DoubleConfigOption)this.OptionBadHighlightColor.getElement(1)).getValue();
                        var19 = (float)((DoubleConfigOption)this.OptionBadHighlightColor.getElement(2)).getValue();
                        this.badHighlitedColor.set(var16, var18, var19, 1.0F);
                     } else if (var3.startsWith("seenNews=")) {
                        this.setSeenUpdateText(var3.replaceFirst("seenNews=", ""));
                     } else if (var3.startsWith("toggleToAim=")) {
                        this.setToggleToAim(Boolean.parseBoolean(var3.replaceFirst("toggleToAim=", "")));
                     } else if (var3.startsWith("toggleToRun=")) {
                        this.setToggleToRun(Boolean.parseBoolean(var3.replaceFirst("toggleToRun=", "")));
                     } else if (var3.startsWith("toggleToSprint=")) {
                        this.setToggleToSprint(Boolean.parseBoolean(var3.replaceFirst("toggleToSprint=", "")));
                     } else if (var3.startsWith("celsius=")) {
                        this.setCelsius(Boolean.parseBoolean(var3.replaceFirst("celsius=", "")));
                     } else if (!var3.startsWith("mapOrder=")) {
                        if (var3.startsWith("showFirstTimeSneakTutorial=")) {
                           this.setShowFirstTimeSneakTutorial(Boolean.parseBoolean(var3.replaceFirst("showFirstTimeSneakTutorial=", "")));
                        } else if (var3.startsWith("showFirstTimeSearchTutorial=")) {
                           this.setShowFirstTimeSearchTutorial(Boolean.parseBoolean(var3.replaceFirst("showFirstTimeSearchTutorial=", "")));
                        } else if (var3.startsWith("termsOfServiceVersion=")) {
                           this.OptionTermsOfServiceVersion.parse(var3.replaceFirst("termsOfServiceVersion=", ""));
                        } else if (var3.startsWith("uiRenderOffscreen=")) {
                           this.OptionUIFBO.parse(var3.replaceFirst("uiRenderOffscreen=", ""));
                        } else if (var3.startsWith("uiRenderFPS=")) {
                           this.OptionUIRenderFPS.parse(var3.replaceFirst("uiRenderFPS=", ""));
                        } else if (var3.startsWith("radialMenuKeyToggle=")) {
                           this.OptionRadialMenuKeyToggle.parse(var3.replaceFirst("radialMenuKeyToggle=", ""));
                        } else if (var3.startsWith("reloadRadialInstant=")) {
                           this.OptionReloadRadialInstant.parse(var3.replaceFirst("reloadRadialInstant=", ""));
                        } else if (var3.startsWith("panCameraWhileAiming=")) {
                           this.OptionPanCameraWhileAiming.parse(var3.replaceFirst("panCameraWhileAiming=", ""));
                        } else if (var3.startsWith("panCameraWhileDriving=")) {
                           this.OptionPanCameraWhileDriving.parse(var3.replaceFirst("panCameraWhileDriving=", ""));
                        } else if (var3.startsWith("temperatureDisplayCelsius=")) {
                           this.OptionTemperatureDisplayCelsius.parse(var3.replaceFirst("temperatureDisplayCelsius=", ""));
                        } else if (var3.startsWith("doVideoEffects=")) {
                           this.OptionDoVideoEffects.parse(var3.replaceFirst("doVideoEffects=", ""));
                        } else if (var3.startsWith("doWindSpriteEffects=")) {
                           this.OptionDoWindSpriteEffects.parse(var3.replaceFirst("doWindSpriteEffects=", ""));
                        } else if (var3.startsWith("doDoorSpriteEffects=")) {
                           this.OptionDoDoorSpriteEffects.parse(var3.replaceFirst("doDoorSpriteEffects=", ""));
                        } else if (var3.startsWith("doContainerOutline=")) {
                           this.OptionDoContainerOutline.parse(var3.replaceFirst("doContainerOutline=", ""));
                        } else if (var3.startsWith("updateSneakButton2=")) {
                           this.OptionUpdateSneakButton.setValue(true);
                        } else if (var3.startsWith("updateSneakButton=")) {
                           this.OptionUpdateSneakButton.parse(var3.replaceFirst("updateSneakButton=", ""));
                        } else if (var3.startsWith("dblTapJogToSprint=")) {
                           this.OptiondblTapJogToSprint.parse(var3.replaceFirst("dblTapJogToSprint=", ""));
                        } else if (var3.startsWith("meleeOutline=")) {
                           this.OptionMeleeOutline.parse(var3.replaceFirst("meleeOutline=", ""));
                        } else if (var3.startsWith("cycleContainerKey=")) {
                           this.OptionCycleContainerKey.parse(var3.replaceFirst("cycleContainerKey=", "").trim());
                        } else if (var3.startsWith("dropItemsOnSquareCenter=")) {
                           this.OptionDropItemsOnSquareCenter.parse(var3.replaceFirst("dropItemsOnSquareCenter=", ""));
                        } else if (var3.startsWith("timedActionGameSpeedReset=")) {
                           this.OptionTimedActionGameSpeedReset.parse(var3.replaceFirst("timedActionGameSpeedReset=", ""));
                        } else if (var3.startsWith("shoulderButtonContainerSwitch=")) {
                           this.OptionShoulderButtonContainerSwitch.parse(var3.replaceFirst("shoulderButtonContainerSwitch=", ""));
                        } else if (var3.startsWith("controllerButtonStyle=")) {
                           this.OptionControllerButtonStyle.parse(var3.replaceFirst("controllerButtonStyle=", ""));
                        } else if (var3.startsWith("singleContextMenu=")) {
                           this.OptionSingleContextMenu.parse(var3.replaceFirst("singleContextMenu=", ""));
                        } else if (var3.startsWith("renderPrecipIndoors=")) {
                           this.OptionRenderPrecipIndoors.parse(var3.replaceFirst("renderPrecipIndoors=", ""));
                        } else if (var3.startsWith("precipitationSpeedMultiplier=")) {
                           this.setOptionPrecipitationSpeedMultiplier(Float.parseFloat(var3.replaceFirst("precipitationSpeedMultiplier=", "")));
                        } else if (var3.startsWith("autoProneAtk=")) {
                           this.OptionAutoProneAtk.parse(var3.replaceFirst("autoProneAtk=", ""));
                        } else if (var3.startsWith("3DGroundItem=")) {
                           this.Option3DGroundItem.parse(var3.replaceFirst("3DGroundItem=", ""));
                        } else if (var3.startsWith("tieredZombieUpdates=")) {
                           this.setOptionTieredZombieUpdates(Boolean.parseBoolean(var3.replaceFirst("tieredZombieUpdates=", "")));
                        } else if (var3.startsWith("progressBar=")) {
                           this.setOptionProgressBar(Boolean.parseBoolean(var3.replaceFirst("progressBar=", "")));
                        } else if (var3.startsWith("corpseShadows=")) {
                           this.OptionCorpseShadows.parse(var3.replaceFirst("corpseShadows=", ""));
                        } else if (var3.startsWith("simpleClothingTextures=")) {
                           this.OptionSimpleClothingTextures.parse(var3.replaceFirst("simpleClothingTextures=", ""));
                        } else if (var3.startsWith("simpleWeaponTextures=")) {
                           this.OptionSimpleWeaponTextures.parse(var3.replaceFirst("simpleWeaponTextures=", ""));
                        } else if (var3.startsWith("autoDrink=")) {
                           this.OptionAutoDrink.parse(var3.replaceFirst("autoDrink=", ""));
                        } else if (var3.startsWith("leaveKeyInIgnition=")) {
                           this.OptionLeaveKeyInIgnition.parse(var3.replaceFirst("leaveKeyInIgnition=", ""));
                        } else if (var3.startsWith("autoWalkContainer=")) {
                           this.OptionAutoWalkContainer.parse(var3.replaceFirst("autoWalkContainer=", ""));
                        } else if (var3.startsWith("searchModeOverlayEffect=")) {
                           this.OptionSearchModeOverlayEffect.parse(var3.replaceFirst("searchModeOverlayEffect=", ""));
                        } else if (var3.startsWith("ignoreProneZombieRange=")) {
                           this.OptionIgnoreProneZombieRange.parse(var3.replaceFirst("ignoreProneZombieRange=", ""));
                        } else if (var3.startsWith("fogQuality=")) {
                           PerformanceSettings.FogQuality = Integer.parseInt(var3.replaceFirst("fogQuality=", ""));
                        } else if (var3.startsWith("renderPrecipitation=")) {
                           this.OptionRenderPrecipitation.parse(var3.replaceFirst("renderPrecipitation=", ""));
                        } else if (var3.startsWith("showItemModInfo=")) {
                           this.OptionShowItemModInfo.parse(var3.replaceFirst("showItemModInfo=", ""));
                        } else if (var3.startsWith("showSurvivalGuide=")) {
                           this.OptionShowSurvivalGuide.parse(var3.replaceFirst("showSurvivalGuide=", ""));
                        } else if (var3.startsWith("showFirstAnimalZoneInfo=")) {
                           this.OptionShowFirstAnimalZoneInfo.parse(var3.replaceFirst("showFirstAnimalZoneInfo=", ""));
                        } else if (var3.startsWith("enableLeftJoystickRadialMenu=")) {
                           this.OptionEnableLeftJoystickRadialMenu.parse(var3.replaceFirst("enableLeftJoystickRadialMenu=", ""));
                        } else if (var3.startsWith("enableDyslexicFont=")) {
                           this.OptionEnableDyslexicFont.parse(var3.replaceFirst("enableDyslexicFont=", ""));
                        }
                     } else {
                        if (this.version < 7) {
                           var3 = "mapOrder=";
                        }

                        String[] var4 = var3.replaceFirst("mapOrder=", "").split(";");
                        String[] var5 = var4;
                        int var6 = var4.length;

                        for(int var7 = 0; var7 < var6; ++var7) {
                           String var8 = var5[var7];
                           var8 = var8.trim();
                           if (!var8.isEmpty()) {
                              ActiveMods.getById("default").getMapOrder().add(var8);
                           }
                        }

                        ZomboidFileSystem.instance.saveModsFile();
                     }
                  }
               }
            }

            if (this.OptionLanguageName.getValue().isEmpty()) {
               this.OptionLanguageName.setValue(System.getProperty("user.language").toUpperCase());
            }

            if (!this.OptionDoneNewSaveFolder.getValue()) {
               this.handleNewSaveFolderFormat();
               this.OptionDoneNewSaveFolder.setValue(true);
            }
         } catch (Exception var12) {
            ExceptionLogger.logException(var12);
         } finally {
            var14.close();
         }

         this.saveOptions();
         return true;
      }
   }

   public boolean loadOptions() throws IOException {
      this.bLoadedOptions = false;
      File var1 = new File(ZomboidFileSystem.instance.getCacheDirSub("options.ini"));
      if (!var1.exists()) {
         this.initOptionsINI();
         return false;
      } else {
         this.bLoadedOptions = true;

         for(int var2 = 0; var2 < 4; ++var2) {
            this.setAutoZoom(var2, false);
         }

         this.OptionLanguageName.setValue("");

         try {
            Iterator var10 = this.options.iterator();

            while(var10.hasNext()) {
               ConfigOption var3 = (ConfigOption)var10.next();
               if (var3 instanceof ArrayConfigOption) {
                  ArrayConfigOption var4 = (ArrayConfigOption)var3;
                  if (var4.isMultiLine()) {
                     var4.clear();
                  }
               }
            }

            ConfigFile var11 = new ConfigFile();
            var11.setVersionString("version");
            if (var11.read(var1.getAbsolutePath())) {
               int var12 = var11.getVersion();
               this.version = var12;

               ConfigOption var5;
               String var6;
               int var13;
               for(var13 = 0; var13 < var11.getOptions().size(); ++var13) {
                  var5 = (ConfigOption)var11.getOptions().get(var13);
                  var6 = var5.getName();
                  String var7 = var5.getValueAsString();
                  var6 = this.upgradeOptionName(var6, var12);
                  var7 = this.upgradeOptionValue(var6, var7, var12);
                  ConfigOption var8 = (ConfigOption)this.optionByName.get(var6);
                  if (var8 == null) {
                     var8 = (ConfigOption)this.fakeOptionByName.get(var6);
                  }

                  if (var8 != null) {
                     var8.parse(var7);
                  }
               }

               width = this.OptionScreenWidth.getValue();
               height = this.OptionScreenHeight.getValue();
               PerformanceSettings.FogQuality = this.OptionFogQuality.getValue();
               PerformanceSettings.setLockFPS(this.OptionLockFPS.getValue());
               PerformanceSettings.instance.setFramerateUncapped(this.OptionUncappedFPS.getValue());
               PerformanceSettings.WaterQuality = this.OptionWaterQuality.getValue();
               PerformanceSettings.PuddlesQuality = this.OptionPuddlesQuality.getValue();
               PerformanceSettings.LightingFrameSkip = this.OptionLightingFrameSkip.getValue();
               PerformanceSettings.instance.setLightingFPS(this.OptionLightingFPS.getValue());
               this.iPerfSkybox = this.iPerfSkybox_new.getValue();
               this.iPerfPuddles = this.iPerfPuddles_new.getValue();
               this.bPerfReflections = this.bPerfReflections_new.getValue();

               for(var13 = 0; var13 < 4; ++var13) {
                  this.OffscreenBuffer.bAutoZoom[var13] = this.getAutoZoom(var13);
               }

               for(var13 = 0; var13 < this.OptionActiveControllerGUIDs.size(); ++var13) {
                  var5 = this.OptionActiveControllerGUIDs.getElement(var13);
                  var6 = var5.getValueAsString().trim();
                  if (!var6.isEmpty()) {
                     JoypadManager.instance.setControllerActive(var6, true);
                  }
               }

               MovingObjectUpdateScheduler.instance.setEnabled(this.OptionTieredZombieUpdates.getValue());
            }

            if (this.OptionLanguageName.getValue().isEmpty()) {
               this.OptionLanguageName.setValue(System.getProperty("user.language").toUpperCase());
            }

            if (!this.OptionDoneNewSaveFolder.getValue()) {
               this.handleNewSaveFolderFormat();
               this.OptionDoneNewSaveFolder.setValue(true);
            }
         } catch (Exception var9) {
            ExceptionLogger.logException(var9);
         }

         this.saveOptions();
         return true;
      }
   }

   private String upgradeOptionName(String var1, int var2) {
      return var1;
   }

   private String upgradeOptionValue(String var1, String var2, int var3) {
      return var2;
   }

   private void initOptionsINI() throws IOException {
      this.saveFolder = getMyDocumentFolder();
      File var1 = new File(this.saveFolder);
      var1.mkdir();
      this.copyPasteFolders("mods");
      this.setOptionLanguageName(System.getProperty("user.language").toUpperCase());
      if (Translator.getAzertyMap().contains(Translator.getLanguage().name())) {
         this.setAzerty(true);
      }

      if (!GameServer.bServer) {
         try {
            int var3 = 0;
            int var4 = 0;
            DisplayMode[] var2 = Display.getAvailableDisplayModes();
            int[] var5 = new int[1];
            int[] var6 = new int[1];
            int[] var7 = new int[1];
            int[] var8 = new int[1];
            GLFW.glfwGetMonitorWorkarea(GLFW.glfwGetPrimaryMonitor(), var5, var6, var7, var8);

            for(int var9 = 0; var9 < var2.length; ++var9) {
               if (var2[var9].getWidth() > var3 && var2[var9].getWidth() < var7[0] && var2[var9].getHeight() < var8[0]) {
                  var3 = var2[var9].getWidth();
                  var4 = var2[var9].getHeight();
               }
            }

            width = var3;
            height = var4;
         } catch (LWJGLException var10) {
            ExceptionLogger.logException(var10);
         }
      }

      this.setOptionZoomLevels2x("50;75;125;150;175;200");
      this.setOptionZoomLevels1x("50;75;125;150;175;200");
      this.saveOptions();
   }

   private void handleNewSaveFolderFormat() {
      File var1 = new File(ZomboidFileSystem.instance.getSaveDir());
      var1.mkdir();
      ArrayList var2 = new ArrayList();
      var2.add("Beginner");
      var2.add("Survival");
      var2.add("A Really CD DA");
      var2.add("LastStand");
      var2.add("Opening Hours");
      var2.add("Sandbox");
      var2.add("Tutorial");
      var2.add("Winter is Coming");
      var2.add("You Have One Day");
      File var3 = null;
      File var4 = null;

      try {
         Iterator var5 = var2.iterator();

         while(var5.hasNext()) {
            String var6 = (String)var5.next();
            String var10002 = ZomboidFileSystem.instance.getCacheDir();
            var3 = new File(var10002 + File.separator + var6);
            var10002 = ZomboidFileSystem.instance.getSaveDir();
            var4 = new File(var10002 + File.separator + var6);
            if (var3.exists()) {
               var4.mkdir();
               Files.move(var3.toPath(), var4.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
         }
      } catch (Exception var7) {
      }

   }

   public boolean isDefaultOptions() {
      return !this.bLoadedOptions;
   }

   public boolean isDedicated() {
      return GameServer.bServer;
   }

   private void copyPasteFolders(String var1) {
      File var2 = (new File(var1)).getAbsoluteFile();
      if (var2.exists()) {
         this.searchFolders(var2, var1);
      }

   }

   private void searchFolders(File var1, String var2) {
      if (var1.isDirectory()) {
         File var3 = new File(this.saveFolder + File.separator + var2);
         var3.mkdir();
         String[] var4 = var1.list();

         for(int var5 = 0; var5 < var4.length; ++var5) {
            String var10003 = var1.getAbsolutePath();
            this.searchFolders(new File(var10003 + File.separator + var4[var5]), var2 + File.separator + var4[var5]);
         }
      } else {
         this.copyPasteFile(var1, var2);
      }

   }

   private void copyPasteFile(File var1, String var2) {
      FileOutputStream var3 = null;
      FileInputStream var4 = null;

      try {
         File var5 = new File(this.saveFolder + File.separator + var2);
         var5.createNewFile();
         var3 = new FileOutputStream(var5);
         var4 = new FileInputStream(var1);
         var3.getChannel().transferFrom(var4.getChannel(), 0L, var1.length());
      } catch (Exception var14) {
         var14.printStackTrace();
      } finally {
         try {
            if (var4 != null) {
               var4.close();
            }

            if (var3 != null) {
               var3.close();
            }
         } catch (IOException var13) {
            var13.printStackTrace();
         }

      }

   }

   public static String getMyDocumentFolder() {
      return ZomboidFileSystem.instance.getCacheDir();
   }

   public void saveOptions_OLD() throws IOException {
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var1 = new File(var10002 + File.separator + "options.ini");
      if (!var1.exists()) {
         var1.createNewFile();
      }

      FileWriter var2 = new FileWriter(var1);

      try {
         var2.write("version=" + this.fileversion + "\r\n");
         var2.write("width=" + this.getScreenWidth() + "\r\n");
         var2.write("height=" + this.getScreenHeight() + "\r\n");
         var2.write("fullScreen=" + this.fullScreen.getValueAsString() + "\r\n");
         var2.write("frameRate=" + PerformanceSettings.getLockFPS() + "\r\n");
         var2.write("uncappedFPS=" + PerformanceSettings.instance.isFramerateUncapped() + "\r\n");
         var2.write("iso_cursor=" + this.isoCursorVisibility.getValueAsString() + "\r\n");
         var2.write("showCursorWhileAiming=" + this.OptionShowCursorWhileAiming.getValueAsString() + "\r\n");
         var2.write("water=" + PerformanceSettings.WaterQuality + "\r\n");
         var2.write("puddles=" + PerformanceSettings.PuddlesQuality + "\r\n");
         var2.write("lighting=" + PerformanceSettings.LightingFrameSkip + "\r\n");
         var2.write("lightFPS=" + PerformanceSettings.LightingFPS + "\r\n");
         var2.write("perfSkybox=" + this.iPerfSkybox_new.getValueAsString() + "\r\n");
         var2.write("perfPuddles=" + this.iPerfPuddles_new.getValueAsString() + "\r\n");
         var2.write("bPerfReflections=" + this.bPerfReflections_new.getValueAsString() + "\r\n");
         var2.write("vidMem=" + this.vidMem + "\r\n");
         var2.write("language=" + this.OptionLanguageName.getValueAsString() + "\r\n");
         var2.write("zoom=" + this.OptionZoom.getValueAsString() + "\r\n");
         var2.write("fontSize=" + this.OptionFontSize.getValueAsString() + "\r\n");
         var2.write("moodleSize=" + this.OptionMoodleSize.getValueAsString() + "\r\n");
         var2.write("contextMenuFont=" + this.OptionContextMenuFont.getValueAsString() + "\r\n");
         var2.write("inventoryFont=" + this.OptionInventoryFont.getValueAsString() + "\r\n");
         var2.write("inventoryContainerSize=" + this.OptionInventoryContainerSize.getValueAsString() + "\r\n");
         var2.write("tooltipFont=" + this.OptionTooltipFont.getValueAsString() + "\r\n");
         var2.write("clockFormat=" + this.OptionClockFormat.getValueAsString() + "\r\n");
         var2.write("clockSize=" + this.OptionClockSize.getValueAsString() + "\r\n");
         var2.write("clock24Hour=" + this.OptionClock24Hour.getValueAsString() + "\r\n");
         var2.write("measurementsFormat=" + this.OptionMeasurementFormat.getValueAsString() + "\r\n");
         var2.write("autozoom=" + this.bAutoZoom.getValueAsString() + "\r\n");
         var2.write("vsync=" + this.OptionVSync.getValueAsString() + "\r\n");
         var2.write("soundVolume=" + this.OptionSoundVolume.getValue() + "\r\n");
         var2.write("ambientVolume=" + this.OptionAmbientVolume.getValueAsString() + "\r\n");
         var2.write("musicVolume=" + this.OptionMusicVolume.getValueAsString() + "\r\n");
         var2.write("jumpScareVolume=" + this.OptionJumpScareVolume.getValueAsString() + "\r\n");
         var2.write("musicActionStyle=" + this.OptionMusicActionStyle.getValueAsString() + "\r\n");
         var2.write("musicLibrary=" + this.OptionMusicLibrary.getValueAsString() + "\r\n");
         var2.write("vehicleEngineVolume=" + this.OptionVehicleEngineVolume.getValueAsString() + "\r\n");
         var2.write("vehicleStreamerMode=" + this.OptionStreamerMode.getValueAsString() + "\r\n");
         var2.write("voiceEnable=" + this.OptionVoiceEnable.getValueAsString() + "\r\n");
         var2.write("voiceMode=" + this.OptionVoiceMode.getValueAsString() + "\r\n");
         var2.write("voiceVADMode=" + this.OptionVoiceVADMode.getValueAsString() + "\r\n");
         var2.write("voiceAGCMode=" + this.OptionVoiceAGCMode.getValueAsString() + "\r\n");
         var2.write("voiceVolumeMic=" + this.OptionVoiceVolumeMic.getValueAsString() + "\r\n");
         var2.write("voiceVolumePlayers=" + this.OptionVoiceVolumePlayers.getValueAsString() + "\r\n");
         var2.write("voiceRecordDeviceName=" + this.OptionVoiceRecordDeviceName.getValueAsString() + "\r\n");
         var2.write("reloadDifficulty=" + this.OptionReloadDifficulty.getValueAsString() + "\r\n");
         var2.write("rackProgress=" + this.OptionRackProgress.getValueAsString() + "\r\n");
         Iterator var3 = JoypadManager.instance.ActiveControllerGUIDs.iterator();

         while(var3.hasNext()) {
            String var4 = (String)var3.next();
            var2.write("controller=" + var4 + "\r\n");
         }

         var2.write("tutorialDone=" + this.isTutorialDone() + "\r\n");
         var2.write("vehiclesWarningShow=" + this.isVehiclesWarningShow() + "\r\n");
         var2.write("bloodDecals=" + this.OptionBloodDecals.getValueAsString() + "\r\n");
         var2.write("focusloss=" + this.OptionFocusloss.getValueAsString() + "\r\n");
         var2.write("borderless=" + this.OptionBorderlessWindow.getValueAsString() + "\r\n");
         var2.write("lockCursorToWindow=" + this.OptionLockCursorToWindow.getValueAsString() + "\r\n");
         var2.write("textureCompression=" + this.OptionTextureCompression.getValueAsString() + "\r\n");
         var2.write("modelTextureMipmaps=" + this.OptionModelTextureMipmaps.getValueAsString() + "\r\n");
         var2.write("texture2x=" + this.OptionTexture2x.getValueAsString() + "\r\n");
         var2.write("maxTextureSize=" + this.OptionMaxTextureSize.getValueAsString() + "\r\n");
         var2.write("maxVehicleTextureSize=" + this.OptionMaxVehicleTextureSize.getValueAsString() + "\r\n");
         var2.write("zoomLevels1x=" + this.OptionZoomLevels1x.getValueAsString() + "\r\n");
         var2.write("zoomLevels2x=" + this.OptionZoomLevels2x.getValueAsString() + "\r\n");
         var2.write("showChatTimestamp=" + this.OptionShowChatTimestamp.getValueAsString() + "\r\n");
         var2.write("showChatTitle=" + this.OptionShowChatTitle.getValueAsString() + "\r\n");
         var2.write("chatFontSize=" + this.OptionChatFontSize.getValueAsString() + "\r\n");
         var2.write("minChatOpaque=" + this.OptionMinChatOpaque.getValueAsString() + "\r\n");
         var2.write("maxChatOpaque=" + this.OptionMaxChatOpaque.getValueAsString() + "\r\n");
         var2.write("chatFadeTime=" + this.OptionChatFadeTime.getValueAsString() + "\r\n");
         var2.write("chatOpaqueOnFocus=" + this.OptionChatOpaqueOnFocus.getValueAsString() + "\r\n");
         var2.write("doneNewSaveFolder=" + this.OptionDoneNewSaveFolder.getValueAsString() + "\r\n");
         var2.write("contentTranslationsEnabled=" + this.OptionEnableContentTranslations.getValueAsString() + "\r\n");
         var2.write("showYourUsername=" + this.showYourUsername.getValueAsString() + "\r\n");
         var2.write("rosewoodSpawnDone=" + this.OptionRosewoodSpawnDone.getValueAsString() + "\r\n");
         if (this.mpTextColor != null) {
            var2.write("mpTextColor=" + this.OptionMPTextColor.getValueAsString() + "\r\n");
         }

         var2.write("objHighlightColor=" + this.OptionObjectHighlightColor.getValueAsString() + "\r\n");
         var2.write("workstationHighlightColor=" + this.OptionWorkstationHighlightColor.getValueAsString() + "\r\n");
         var2.write("seenNews=" + this.getSeenUpdateText() + "\r\n");
         var2.write("toggleToAim=" + this.toggleToAim.getValueAsString() + "\r\n");
         var2.write("toggleToRun=" + this.toggleToRun.getValueAsString() + "\r\n");
         var2.write("toggleToSprint=" + this.toggleToSprint.getValueAsString() + "\r\n");
         var2.write("celsius=" + this.celsius.getValueAsString() + "\r\n");
         var2.write("riversideDone=" + this.isRiversideDone() + "\r\n");
         var2.write("showFirstTimeSneakTutorial=" + this.isShowFirstTimeSneakTutorial() + "\r\n");
         var2.write("showFirstTimeSearchTutorial=" + this.isShowFirstTimeSearchTutorial() + "\r\n");
         var2.write("termsOfServiceVersion=" + this.OptionTermsOfServiceVersion.getValueAsString() + "\r\n");
         var2.write("uiRenderOffscreen=" + this.OptionUIFBO.getValueAsString() + "\r\n");
         var2.write("uiRenderFPS=" + this.OptionUIRenderFPS.getValueAsString() + "\r\n");
         var2.write("radialMenuKeyToggle=" + this.OptionRadialMenuKeyToggle.getValueAsString() + "\r\n");
         var2.write("reloadRadialInstant=" + this.OptionReloadRadialInstant.getValueAsString() + "\r\n");
         var2.write("panCameraWhileAiming=" + this.OptionPanCameraWhileAiming.getValueAsString() + "\r\n");
         var2.write("panCameraWhileDriving=" + this.OptionPanCameraWhileDriving.getValueAsString() + "\r\n");
         var2.write("temperatureDisplayCelsius=" + this.OptionTemperatureDisplayCelsius.getValueAsString() + "\r\n");
         var2.write("doVideoEffects=" + this.OptionDoVideoEffects.getValueAsString() + "\r\n");
         var2.write("doWindSpriteEffects=" + this.OptionDoWindSpriteEffects.getValueAsString() + "\r\n");
         var2.write("doDoorSpriteEffects=" + this.OptionDoDoorSpriteEffects.getValueAsString() + "\r\n");
         var2.write("updateSneakButton=" + this.OptionUpdateSneakButton.getValueAsString() + "\r\n");
         var2.write("dblTapJogToSprint=" + this.OptiondblTapJogToSprint.getValueAsString() + "\r\n");
         var2.write("gotNewBelt=" + this.OptionGotNewBelt.getValueAsString() + "\r\n");
         var2.write("meleeOutline=" + this.OptionMeleeOutline.getValueAsString() + "\r\n");
         var2.write("cycleContainerKey=" + this.OptionCycleContainerKey.getValueAsString() + "\r\n");
         var2.write("dropItemsOnSquareCenter=" + this.OptionDropItemsOnSquareCenter.getValueAsString() + "\r\n");
         var2.write("timedActionGameSpeedReset=" + this.OptionTimedActionGameSpeedReset.getValueAsString() + "\r\n");
         var2.write("shoulderButtonContainerSwitch=" + this.OptionShoulderButtonContainerSwitch.getValueAsString() + "\r\n");
         var2.write("controllerButtonStyle=" + this.OptionControllerButtonStyle.getValueAsString() + "\r\n");
         var2.write("singleContextMenu=" + this.OptionSingleContextMenu.getValueAsString() + "\r\n");
         var2.write("renderPrecipIndoors=" + this.OptionRenderPrecipIndoors.getValueAsString() + "\r\n");
         var2.write("precipitationSpeedMultiplier=" + this.OptionPrecipitationSpeedMultiplier.getValueAsString() + "\r\n");
         var2.write("autoProneAtk=" + this.OptionAutoProneAtk.getValueAsString() + "\r\n");
         var2.write("3DGroundItem=" + this.Option3DGroundItem.getValueAsString() + "\r\n");
         var2.write("tieredZombieUpdates=" + this.getOptionTieredZombieUpdates() + "\r\n");
         var2.write("progressBar=" + this.isOptionProgressBar() + "\r\n");
         var2.write("corpseShadows=" + this.getOptionCorpseShadows() + "\r\n");
         var2.write("simpleClothingTextures=" + this.getOptionSimpleClothingTextures() + "\r\n");
         var2.write("simpleWeaponTextures=" + this.getOptionSimpleWeaponTextures() + "\r\n");
         var2.write("autoDrink=" + this.getOptionAutoDrink() + "\r\n");
         var2.write("leaveKeyInIgnition=" + this.getOptionLeaveKeyInIgnition() + "\r\n");
         var2.write("autoWalkContainer=" + this.getOptionAutoWalkContainer() + "\r\n");
         var2.write("searchModeOverlayEffect=" + this.getOptionSearchModeOverlayEffect() + "\r\n");
         var2.write("ignoreProneZombieRange=" + this.getOptionIgnoreProneZombieRange() + "\r\n");
         var2.write("fogQuality=" + PerformanceSettings.FogQuality + "\r\n");
         var2.write("renderPrecipitation=" + this.OptionRenderPrecipitation.getValueAsString() + "\r\n");
         var2.write("showItemModInfo=" + this.OptionShowItemModInfo.getValueAsString() + "\r\n");
         var2.write("showSurvivalGuide=" + this.OptionShowSurvivalGuide.getValueAsString() + "\r\n");
         var2.write("showFirstAnimalZoneInfo=" + this.OptionShowFirstAnimalZoneInfo.getValueAsString() + "\r\n");
         var2.write("enableLeftJoystickRadialMenu=" + this.OptionEnableLeftJoystickRadialMenu.getValueAsString() + "\r\n");
         var2.write("doContainerOutline=" + this.OptionDoContainerOutline.getValueAsString() + "\r\n");
         var2.write("goodHighlightColor=" + this.OptionGoodHighlightColor.getValueAsString() + "\r\n");
         var2.write("badHighlightColor=" + this.OptionBadHighlightColor.getValueAsString() + "\r\n");
         var2.write("enableDyslexicFont=" + this.OptionEnableDyslexicFont.getValueAsString() + "\r\n");
      } catch (Exception var8) {
         var8.printStackTrace();
      } finally {
         var2.close();
      }

   }

   public void saveOptions() throws IOException {
      ConfigFile var1 = new ConfigFile();
      var1.setVersionString("version");
      var1.setWriteTooltips(false);
      ArrayList var2 = new ArrayList(this.options);
      this.addFakeOptionsForWriting(var2);
      var2.sort((var0, var1x) -> {
         return String.CASE_INSENSITIVE_ORDER.compare(var0.getName(), var1x.getName());
      });
      var1.write(ZomboidFileSystem.instance.getCacheDirSub("options.ini"), this.fileversion, var2);
   }

   private void addFakeOptionsForWriting(ArrayList<ConfigOption> var1) {
      this.OptionFogQuality.setValue(PerformanceSettings.FogQuality);
      this.OptionLightingFPS.setValue(PerformanceSettings.LightingFPS);
      this.OptionLightingFrameSkip.setValue(PerformanceSettings.LightingFrameSkip);
      this.OptionLockFPS.setValue(PerformanceSettings.getLockFPS());
      this.OptionPuddlesQuality.setValue(PerformanceSettings.PuddlesQuality);
      this.OptionScreenHeight.setValue(this.getScreenHeight());
      this.OptionScreenWidth.setValue(this.getScreenWidth());
      this.OptionTieredZombieUpdates.setValue(this.getOptionTieredZombieUpdates());
      this.OptionUncappedFPS.setValue(PerformanceSettings.instance.isFramerateUncapped());
      this.OptionWaterQuality.setValue(PerformanceSettings.WaterQuality);
      this.OptionActiveControllerGUIDs.clear();
      Iterator var2 = JoypadManager.instance.ActiveControllerGUIDs.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         this.OptionActiveControllerGUIDs.parse(var3);
      }

      var1.addAll(this.fakeOptions);
   }

   public void setWindowed(boolean var1) {
      if (!isUseGameViewport()) {
         RenderThread.invokeOnRenderContext(() -> {
            if (var1 != this.fullScreen.getValue()) {
               setDisplayMode(this.getScreenWidth(), this.getScreenHeight(), var1);
            }

            this.fullScreen.setValue(var1);
            if (fakefullscreen) {
               Display.setResizable(false);
            } else {
               Display.setResizable(!var1);
            }

            try {
               this.saveOptions();
            } catch (IOException var3) {
               var3.printStackTrace();
            }

         });
      }
   }

   public boolean isFullScreen() {
      return this.fullScreen.getValue();
   }

   public KahluaTable getScreenModes() {
      ArrayList var1 = new ArrayList();
      KahluaTable var2 = LuaManager.platform.newTable();
      String var10002 = LuaManager.getLuaCacheDir();
      File var3 = new File(var10002 + File.separator + "screenresolution.ini");
      int var4 = 1;

      try {
         Integer var6;
         if (!var3.exists()) {
            var3.createNewFile();
            FileWriter var5 = new FileWriter(var3);
            var6 = 0;
            Integer var7 = 0;
            DisplayMode[] var8 = Display.getAvailableDisplayModes();

            for(int var9 = 0; var9 < var8.length; ++var9) {
               var6 = var8[var9].getWidth();
               var7 = var8[var9].getHeight();
               if (!var1.contains("" + var6 + " x " + var7)) {
                  var2.rawset(var4, "" + var6 + " x " + var7);
                  var5.write("" + var6 + " x " + var7 + " \r\n");
                  var1.add("" + var6 + " x " + var7);
                  ++var4;
               }
            }

            var5.close();
         } else {
            BufferedReader var11 = new BufferedReader(new FileReader(var3));

            String var12;
            for(var6 = null; (var12 = var11.readLine()) != null; ++var4) {
               var2.rawset(var4, var12.trim());
            }

            var11.close();
         }
      } catch (Exception var10) {
         var10.printStackTrace();
      }

      return var2;
   }

   public static void setDisplayMode(int var0, int var1, boolean var2) {
      RenderThread.invokeOnRenderContext(() -> {
         boolean var3 = getInstance().getOptionBorderlessWindow();
         if (Display.getWidth() != var0 || Display.getHeight() != var1 || Display.isFullscreen() != var2 || Display.isBorderlessWindow() != var3) {
            getInstance().fullScreen.setValue(var2);

            try {
               DisplayMode var4 = null;
               if (!var2) {
                  if (var3) {
                     if (Display.getWindow() != 0L && Display.isFullscreen()) {
                        Display.setFullscreen(false);
                     }

                     long var13 = GLFW.glfwGetPrimaryMonitor();
                     GLFWVidMode var14 = GLFW.glfwGetVideoMode(var13);
                     var4 = new DisplayMode(var14.width(), var14.height());
                  } else {
                     var4 = new DisplayMode(var0, var1);
                  }
               } else {
                  DisplayMode[] var5 = Display.getAvailableDisplayModes();
                  int var6 = 0;
                  DisplayMode var7 = null;
                  DisplayMode[] var8 = var5;
                  int var9 = var5.length;

                  for(int var10 = 0; var10 < var9; ++var10) {
                     DisplayMode var11 = var8[var10];
                     if (var11.getWidth() == var0 && var11.getHeight() == var1 && var11.isFullscreenCapable()) {
                        if ((var4 == null || var11.getFrequency() >= var6) && (var4 == null || var11.getBitsPerPixel() > var4.getBitsPerPixel())) {
                           var4 = var11;
                           var6 = var11.getFrequency();
                        }

                        if (var11.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel() && var11.getFrequency() == Display.getDesktopDisplayMode().getFrequency()) {
                           var4 = var11;
                           break;
                        }
                     }

                     if (var11.isFullscreenCapable() && (var7 == null || Math.abs(var11.getWidth() - var0) < Math.abs(var7.getWidth() - var0) || var11.getWidth() == var7.getWidth() && var11.getFrequency() > var6)) {
                        var7 = var11;
                        var6 = var11.getFrequency();
                        PrintStream var10000 = System.out;
                        int var10001 = var11.getWidth();
                        var10000.println("closest width=" + var10001 + " freq=" + var11.getFrequency());
                     }
                  }

                  if (var4 == null && var7 != null) {
                     var4 = var7;
                  }
               }

               if (var4 == null) {
                  DebugLog.log("Failed to find value mode: " + var0 + "x" + var1 + " fs=" + var2);
                  return;
               }

               Display.setBorderlessWindow(var3);
               if (var2) {
                  Display.setDisplayModeAndFullscreen(var4);
               } else {
                  Display.setDisplayMode(var4);
                  Display.setFullscreen(false);
               }

               if (!var2 && var3) {
                  Display.setResizable(false);
               } else if (!var2 && !fakefullscreen) {
                  Display.setResizable(false);
                  Display.setResizable(true);
               }

               if (Display.isCreated()) {
                  int var15 = Display.getWidth();
                  DebugLog.log("Display mode changed to " + var15 + "x" + Display.getHeight() + " freq=" + Display.getDisplayMode().getFrequency() + " fullScreen=" + Display.isFullscreen());
               }
            } catch (LWJGLException var12) {
               DebugLog.log("Unable to setup mode " + var0 + "x" + var1 + " fullscreen=" + var2 + var12);
            }

         }
      });
   }

   private boolean isFunctionKey(int var1) {
      return var1 >= 59 && var1 <= 68 || var1 >= 87 && var1 <= 105 || var1 == 113;
   }

   public boolean isDoingTextEntry() {
      if (CurrentTextEntryBox == null) {
         return false;
      } else if (!CurrentTextEntryBox.isEditable()) {
         return false;
      } else {
         return CurrentTextEntryBox.isDoingTextEntry();
      }
   }

   private void updateKeyboardAux(UITextEntryInterface var1, int var2) {
      boolean var3 = Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157);
      if (var2 != 28 && var2 != 156) {
         if (var2 == 1) {
            var1.onOtherKey(1);
            GameKeyboard.eatKeyPress(1);
         } else if (var2 == 15) {
            var1.onOtherKey(15);
            LuaEventManager.triggerEvent("SwitchChatStream");
         } else if (var2 != 58) {
            if (var2 == 199) {
               var1.onKeyHome();
            } else if (var2 == 207) {
               var1.onKeyEnd();
            } else if (var2 == 200) {
               var1.onKeyUp();
            } else if (var2 == 208) {
               var1.onKeyDown();
            } else if (var2 != 29) {
               if (var2 != 157) {
                  if (var2 != 42) {
                     if (var2 != 54) {
                        if (var2 != 56) {
                           if (var2 != 184) {
                              if (var2 == 203) {
                                 var1.onKeyLeft();
                              } else if (var2 == 205) {
                                 var1.onKeyRight();
                              } else if (!this.isFunctionKey(var2)) {
                                 if (var2 == 211) {
                                    var1.onKeyDelete();
                                 } else if (var2 == 14) {
                                    var1.onKeyBack();
                                 } else if (var3 && var2 == 47) {
                                    var1.pasteFromClipboard();
                                 } else if (var3 && var2 == 46) {
                                    var1.copyToClipboard();
                                 } else if (var3 && var2 == 45) {
                                    var1.cutToClipboard();
                                 } else if (var3 && var2 == 30) {
                                    var1.selectAll();
                                 } else if (!var1.isIgnoreFirst()) {
                                    if (!var1.isTextLimit()) {
                                       char var4 = Keyboard.getEventCharacter();
                                       if (var4 != 0) {
                                          if (var1.isOnlyNumbers() && var4 != '.' && var4 != '-') {
                                             try {
                                                Double.parseDouble(String.valueOf(var4));
                                             } catch (Exception var6) {
                                                return;
                                             }
                                          }

                                          if (!var1.isOnlyText() || var4 >= 'A' && var4 <= 'Z' || var4 >= 'a' && var4 <= 'z') {
                                             var1.putCharacter(var4);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         var1.onKeyEnter();
      }
   }

   public void updateKeyboard() {
      if (this.isDoingTextEntry()) {
         while(Keyboard.next()) {
            if (this.isDoingTextEntry() && Keyboard.getEventKeyState()) {
               int var1 = Keyboard.getEventKey();
               this.updateKeyboardAux(CurrentTextEntryBox, var1);
            }
         }

         if (CurrentTextEntryBox != null && CurrentTextEntryBox.isIgnoreFirst()) {
            CurrentTextEntryBox.setIgnoreFirst(false);
         }

      }
   }

   public void quit() {
      DebugType.ExitDebug.debugln("Core.quit 1");
      if (IsoPlayer.getInstance() != null) {
         DebugType.ExitDebug.debugln("Core.quit 2");
         bExiting = true;
      } else {
         DebugType.ExitDebug.debugln("Core.quit 3");

         try {
            this.saveOptions();
         } catch (IOException var2) {
            var2.printStackTrace();
         }

         GameClient.instance.Shutdown();
         SteamUtils.shutdown();
         DebugType.ExitDebug.debugln("Core.quit 4");
         System.exit(0);
      }

   }

   public void exitToMenu() {
      DebugType.ExitDebug.debugln("Core.exitToMenu");
      bExiting = true;
   }

   public void quitToDesktop() {
      DebugType.ExitDebug.debugln("Core.quitToDesktop");
      GameWindow.closeRequested = true;
   }

   public boolean supportRes(int var1, int var2) throws LWJGLException {
      DisplayMode[] var3 = Display.getAvailableDisplayModes();
      boolean var4 = false;

      for(int var5 = 0; var5 < var3.length; ++var5) {
         if (var3[var5].getWidth() == var1 && var3[var5].getHeight() == var2 && var3[var5].isFullscreenCapable()) {
            return true;
         }
      }

      return false;
   }

   public void init(int var1, int var2) throws LWJGLException {
      System.setProperty("org.lwjgl.opengl.Window.undecorated", this.getOptionBorderlessWindow() ? "true" : "false");
      if (!System.getProperty("os.name").contains("OS X") && !System.getProperty("os.name").startsWith("Win")) {
         DebugLog.log("Creating display. If this fails, you may need to install xrandr.");
      }

      setDisplayMode(var1, var2, this.fullScreen.getValue());

      try {
         Display.create(new PixelFormat(32, 0, 24, 8, 0));
      } catch (LWJGLException var4) {
         Display.destroy();
         Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
         Display.create(new PixelFormat(32, 0, 24, 8, 0));
      }

      if (bDebug && "1".equalsIgnoreCase(System.getProperty("zomboid.opengl.debugcontext"))) {
         PZGLUtil.InitGLDebugging();
      }

      this.fullScreen.setValue(Display.isFullscreen());
      String var10000 = GL11.glGetString(7936);
      DebugLog.log("GraphicsCard: " + var10000 + " " + GL11.glGetString(7937));
      var10000 = GL11.glGetString(7938);
      DebugLog.log("OpenGL version: " + var10000);
      int var5 = Display.getDesktopDisplayMode().getWidth();
      DebugLog.log("Desktop resolution " + var5 + "x" + Display.getDesktopDisplayMode().getHeight());
      var5 = width;
      DebugLog.log("Initial resolution " + var5 + "x" + height + " fullScreen=" + this.fullScreen.getValueAsString());
      GLVertexBufferObject.init();
      DebugLog.General.println("VSync: %s", this.getOptionVSync() ? "ON" : "OFF");
      Display.setVSyncEnabled(this.getOptionVSync());
      GL11.glEnable(3553);
      IndieGL.glBlendFuncA(770, 771);
      GL32.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
   }

   private boolean setupMultiFBO() {
      try {
         if (!this.OffscreenBuffer.test()) {
            return false;
         } else {
            this.OffscreenBuffer.setZoomLevelsFromOption(TileScale == 2 ? this.OptionZoomLevels2x.getValueAsString() : this.OptionZoomLevels1x.getValueAsString());
            this.OffscreenBuffer.create(Display.getWidth(), Display.getHeight());
            return true;
         }
      } catch (Exception var2) {
         var2.printStackTrace();
         return false;
      }
   }

   public static void setInitialSize() {
      initialHeight = (float)height;
      initialWidth = (float)width;
   }

   public void setScreenSize(int var1, int var2) {
      if (width != var1 || var2 != height) {
         int var3 = width;
         int var4 = height;
         DebugLog.log("Screen resolution changed from " + var3 + "x" + var4 + " to " + var1 + "x" + var2 + " fullScreen=" + this.fullScreen.getValueAsString());
         width = var1;
         height = var2;
         if (bDebug) {
            DebugContext.instance.destroy();
            DebugContext.instance.initRenderTarget();
         }

         if (this.OffscreenBuffer != null && this.OffscreenBuffer.Current != null) {
            this.OffscreenBuffer.destroy();

            try {
               this.OffscreenBuffer.setZoomLevelsFromOption(TileScale == 2 ? this.OptionZoomLevels2x.getValueAsString() : this.OptionZoomLevels1x.getValueAsString());
               this.OffscreenBuffer.create(var1, var2);
            } catch (Exception var8) {
               var8.printStackTrace();
            }
         }

         try {
            LuaEventManager.triggerEvent("OnResolutionChange", var3, var4, var1, var2);
         } catch (Exception var7) {
            var7.printStackTrace();
         }

         for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
            IsoPlayer var6 = IsoPlayer.players[var5];
            if (var6 != null) {
               var6.dirtyRecalcGridStackTime = 2.0F;
            }
         }
      }

   }

   public static boolean supportCompressedTextures() {
      return GL.getCapabilities().GL_EXT_texture_compression_latc;
   }

   public void StartFrame() {
      if (LuaManager.thread == null || !LuaManager.thread.bStep) {
         if (SceneShaderStore.WeatherShader != null && this.OffscreenBuffer.Current != null) {
            SceneShaderStore.WeatherShader.setTexture(this.OffscreenBuffer.getTexture(0));
         }

         SpriteRenderer.instance.prePopulating();
         IndieGL.glAlphaFunc(516, 0.0F);
         IndieGL.enableBlend();
         UIManager.resize();
         boolean var1 = false;
         Texture.BindCount = 0;
         if (!var1) {
            SpriteRenderer.instance.glClearDepth(0.0F);
            IndieGL.glClear(18176);
            if (DebugOptions.instance.Terrain.RenderTiles.HighContrastBg.getValue()) {
               SpriteRenderer.instance.glClearColor(255, 0, 255, 255);
               SpriteRenderer.instance.glClear(16384);
            }

            SpriteRenderer.instance.glClearDepth(1.0F);
         }

         if (this.OffscreenBuffer.Current != null) {
            SpriteRenderer.instance.glBuffer(1, 0);
         }

         IndieGL.glDoStartFrame(this.getScreenWidth(), this.getScreenWidth(), this.getCurrentPlayerZoom(), 0);
         IndieGL.StartShader(SceneShaderStore.DefaultShaderID);
         this.frameStage = 1;
      }
   }

   public void StartFrame(int var1, boolean var2) {
      if (!LuaManager.thread.bStep) {
         this.OffscreenBuffer.update();
         IsoGameCharacter var3 = IsoCamera.getCameraCharacter();
         if (var3 != null) {
            PlayerCamera var4 = IsoCamera.cameras[var1];
            var4.calculateModelViewProjection(var3.getX(), var3.getY(), var3.getZ());
            var4.calculateFixForJigglyModels(var3.getX(), var3.getY(), var3.getZ());
         }

         if (SceneShaderStore.WeatherShader != null && this.OffscreenBuffer.Current != null) {
            SceneShaderStore.WeatherShader.setTexture(this.OffscreenBuffer.getTexture(var1));
         }

         if (var2) {
            SpriteRenderer.instance.prePopulating();
         }

         if (!var2) {
            SpriteRenderer.instance.initFromIsoCamera(var1);
         }

         Texture.BindCount = 0;
         if (this.OffscreenBuffer.Current != null) {
            SpriteRenderer.instance.glBuffer(1, var1);
         }

         IndieGL.glDepthMask(true);
         IndieGL.glDoStartFrame(this.getScreenWidth(), this.getScreenHeight(), this.getZoom(var1), var1);
         IndieGL.glClear(17664);
         if (DebugOptions.instance.Terrain.RenderTiles.HighContrastBg.getValue()) {
            SpriteRenderer.instance.glClearColor(255, 0, 255, 255);
            SpriteRenderer.instance.glClear(16384);
         }

         IndieGL.enableBlend();
         this.frameStage = 1;
      }
   }

   public TextureFBO getOffscreenBuffer() {
      return this.OffscreenBuffer.getCurrent(0);
   }

   public TextureFBO getOffscreenBuffer(int var1) {
      return this.OffscreenBuffer.getCurrent(var1);
   }

   public void setLastRenderedFBO(TextureFBO var1) {
      this.OffscreenBuffer.FBOrendered = var1;
   }

   public void DoStartFrameStuff(int var1, int var2, float var3, int var4) {
      this.DoStartFrameStuff(var1, var2, var3, var4, false);
   }

   public void DoStartFrameStuff(int var1, int var2, float var3, int var4, boolean var5) {
      this.DoStartFrameStuffInternal(var1, var2, var3, var4, var5, false, false);
   }

   public void DoEndFrameStuffFx(int var1, int var2, int var3) {
      GL11.glPopAttrib();
      --this.stack;
      this.projectionMatrixStack.pop();
      this.modelViewMatrixStack.pop();
      --this.stack;
   }

   public void DoStartFrameStuffSmartTextureFx(int var1, int var2, int var3) {
      this.DoStartFrameStuffInternal(var1, var2, 1.0F, var3, false, true, true);
   }

   private void DoStartFrameStuffInternal(int var1, int var2, float var3, int var4, boolean var5, boolean var6, boolean var7) {
      GL32.glEnable(3042);
      GL32.glDepthFunc(519);
      int var8 = this.getScreenWidth();
      int var9 = this.getScreenHeight();
      if (!var7 && !var6) {
         var1 = var8;
      }

      if (!var7 && !var6) {
         var2 = var9;
      }

      if (!var7 && var4 != -1) {
         var1 /= IsoPlayer.numPlayers > 1 ? 2 : 1;
         var2 /= IsoPlayer.numPlayers > 2 ? 2 : 1;
      }

      if (!var6) {
         while(this.stack > 0) {
            try {
               GL11.glPopAttrib();
               this.stack -= 2;
            } catch (Throwable var18) {
               int var11 = GL11.glGetInteger(2992);

               while(var11-- > 0) {
                  GL11.glPopAttrib();
               }

               this.stack = 0;
            }
         }
      }

      GL11.glAlphaFunc(516, 0.0F);
      GL11.glPushAttrib(2048);
      ++this.stack;
      ++this.stack;
      Matrix4f var10 = this.projectionMatrixStack.alloc();
      if (!var7 && !var5) {
         var10.setOrtho2D(0.0F, (float)var1 * var3, (float)var2 * var3, 0.0F);
      } else {
         var10.setOrtho2D(0.0F, (float)var1, (float)var2, 0.0F);
      }

      this.projectionMatrixStack.push(var10);
      Matrix4f var19 = this.modelViewMatrixStack.alloc();
      var19.identity();
      this.modelViewMatrixStack.push(var19);
      if (var4 != -1) {
         int var14 = var1;
         int var15 = var2;
         int var12;
         int var13;
         if (var5) {
            var12 = var1;
            var13 = var2;
         } else {
            var12 = var8;
            var13 = var9;
            if (IsoPlayer.numPlayers > 1) {
               var12 = var8 / 2;
            }

            if (IsoPlayer.numPlayers > 2) {
               var13 = var9 / 2;
            }
         }

         if (var6) {
            var14 = var12;
            var15 = var13;
         }

         float var16 = 0.0F;
         float var17 = (float)(var12 * (var4 % 2));
         if (var4 >= 2) {
            var16 += (float)var13;
         }

         if (var5) {
            var16 = (float)(getInstance().getScreenHeight() - var15) - var16;
         }

         GL11.glViewport((int)var17, (int)var16, var14, var15);
         GL11.glEnable(3089);
         GL11.glScissor((int)var17, (int)var16, var14, var15);
         SpriteRenderer.instance.setRenderingPlayerIndex(var4);
      } else {
         GL11.glViewport(0, 0, var1, var2);
      }

   }

   public void ChangeWorldViewport(int var1, int var2, int var3) {
      this.DoStartFrameNoZoom(var1, var2, 1.0F, var3, false, false, false);
   }

   public void StartFrameFlipY(int var1, int var2, float var3, int var4) {
      this.DoStartFrameFlipY(var1, var2, var3, var4, false, false, false);
   }

   private void DoStartFrameFlipY(int var1, int var2, float var3, int var4, boolean var5, boolean var6, boolean var7) {
      GL32.glEnable(3042);
      GL32.glDepthFunc(519);
      GL11.glAlphaFunc(516, 0.0F);
      GL14.glBlendFuncSeparate(1, 771, 773, 1);
      GL11.glPushAttrib(2048);
      ++this.stack;
      ++this.stack;
      Matrix4f var8 = this.projectionMatrixStack.alloc();
      if (!var7 && !var5) {
         if (var3 == 1.0F) {
            var8.setOrtho2D((float)var1 / 4.0F, (float)(var1 * 3) / 4.0F, 0.0F, (float)var2 / 2.0F);
         } else {
            var8.setOrtho2D(0.0F, (float)var1, 0.0F, (float)var2);
         }
      } else {
         var8.setOrtho2D(0.0F, (float)var1, (float)var2, 0.0F);
      }

      this.projectionMatrixStack.push(var8);
      Matrix4f var9 = this.modelViewMatrixStack.alloc();
      var9.identity();
      this.modelViewMatrixStack.push(var9);
      GL11.glViewport(0, 0, var1, var2);
   }

   public void DoStartFrameNoZoom(int var1, int var2, float var3, int var4, boolean var5, boolean var6, boolean var7) {
      GL32.glEnable(3042);
      GL32.glDepthFunc(519);
      int var8 = this.getScreenWidth();
      int var9 = this.getScreenHeight();
      if (!DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue()) {
         if (!var7 && !var6) {
            var1 = var8;
         }

         if (!var7 && !var6) {
            var2 = var9;
         }

         if (!var7 && var4 != -1) {
            var1 /= IsoPlayer.numPlayers > 1 ? 2 : 1;
            var2 /= IsoPlayer.numPlayers > 2 ? 2 : 1;
         }
      }

      if (!var6) {
         while(this.stack > 0) {
            try {
               GL11.glPopAttrib();
               this.stack -= 2;
            } catch (OpenGLException var18) {
               int var11 = GL11.glGetInteger(2992);

               while(var11-- > 0) {
                  GL11.glPopAttrib();
               }

               this.stack = 0;
            }
         }
      }

      GL11.glAlphaFunc(516, 0.0F);
      GL11.glPushAttrib(2048);
      ++this.stack;
      ++this.stack;
      Matrix4f var10 = this.projectionMatrixStack.alloc();
      if (!var7 && !var5) {
         var10.setOrtho2D(0.0F, (float)var1, (float)var2, 0.0F);
      } else {
         var10.setOrtho2D(0.0F, (float)var1, (float)var2, 0.0F);
      }

      this.projectionMatrixStack.push(var10);
      Matrix4f var19 = this.modelViewMatrixStack.alloc();
      var19.identity();
      this.modelViewMatrixStack.push(var19);
      if (var4 != -1) {
         float var16 = (float)IsoCamera.getScreenLeft(var4);
         float var17 = (float)IsoCamera.getScreenTop(var4);
         GL11.glViewport((int)var16, (int)var17, var1, var2);
         GL11.glEnable(3089);
         GL11.glScissor((int)var16, (int)var17, var1, var2);
      } else {
         GL11.glViewport(0, 0, var1, var2);
      }

   }

   public void DoPushIsoStuff(float var1, float var2, float var3, float var4, boolean var5) {
      float var6 = (Float)getInstance().FloatParamMap.get(0);
      float var7 = (Float)getInstance().FloatParamMap.get(1);
      float var8 = (Float)getInstance().FloatParamMap.get(2);
      double var9 = (double)var6;
      double var11 = (double)var7;
      double var13 = (double)var8;
      SpriteRenderState var15 = SpriteRenderer.instance.getRenderingState();
      int var16 = var15.playerIndex;
      PlayerCamera var17 = var15.playerCamera[var16];
      float var18 = var17.RightClickX;
      float var19 = var17.RightClickY;
      float var20 = var17.getTOffX();
      float var21 = var17.getTOffY();
      float var22 = var17.DeferedX;
      float var23 = var17.DeferedY;
      var9 -= (double)var17.XToIso(-var20 - var18, -var21 - var19, 0.0F);
      var11 -= (double)var17.YToIso(-var20 - var18, -var21 - var19, 0.0F);
      var9 += (double)var22;
      var11 += (double)var23;
      double var24 = (double)((float)var17.OffscreenWidth / 1920.0F);
      double var26 = (double)((float)var17.OffscreenHeight / 1920.0F);
      Matrix4f var28 = this.projectionMatrixStack.alloc();
      var28.setOrtho(-((float)var24) / 2.0F, (float)var24 / 2.0F, -((float)var26) / 2.0F, (float)var26 / 2.0F, -10.0F, 10.0F);
      this.projectionMatrixStack.push(var28);
      Matrix4f var29 = this.modelViewMatrixStack.alloc();
      float var30 = (float)(2.0 / Math.sqrt(2048.0));
      var29.scaling(scale);
      var29.scale((float)TileScale / 2.0F);
      var29.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
      var29.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
      double var31 = (double)var1 - var9;
      double var33 = (double)var2 - var11;
      var29.translate(-((float)var31), (float)((double)var3 - var13) * 2.44949F, -((float)var33));
      if (var5) {
         var29.scale(-1.0F, 1.0F, 1.0F);
      } else {
         var29.scale(-1.5F, 1.5F, 1.5F);
      }

      var29.rotate(var4 + 3.1415927F, 0.0F, 1.0F, 0.0F);
      if (!var5) {
         var29.translate(0.0F, -0.48F, 0.0F);
      }

      this.modelViewMatrixStack.push(var29);
      GL11.glDepthRange(-10.0, 10.0);
   }

   public void DoPushIsoStuff2D(float var1, float var2, float var3, float var4, boolean var5) {
      float var6 = (Float)getInstance().FloatParamMap.get(0);
      float var7 = (Float)getInstance().FloatParamMap.get(1);
      float var8 = (Float)getInstance().FloatParamMap.get(2);
      double var9 = (double)var6;
      double var11 = (double)var7;
      double var13 = (double)var8;
      SpriteRenderState var15 = SpriteRenderer.instance.getRenderingState();
      int var16 = var15.playerIndex;
      PlayerCamera var17 = var15.playerCamera[var16];
      float var18 = var17.RightClickX;
      float var19 = var17.RightClickY;
      float var20 = var17.getTOffX();
      float var21 = var17.getTOffY();
      float var22 = var17.DeferedX;
      float var23 = var17.DeferedY;
      var9 -= (double)var17.XToIso(-var20 - var18, -var21 - var19, 0.0F);
      var11 -= (double)var17.YToIso(-var20 - var18, -var21 - var19, 0.0F);
      var9 += (double)var22;
      var11 += (double)var23;
      double var24 = (double)((float)var17.OffscreenWidth / 1920.0F);
      double var26 = (double)((float)var17.OffscreenHeight / 1920.0F);
      Matrix4f var28 = this.projectionMatrixStack.alloc();
      var28.setOrtho(-((float)var24) / 2.0F, (float)var24 / 2.0F, -((float)var26) / 2.0F, (float)var26 / 2.0F, -10.0F, 10.0F);
      this.projectionMatrixStack.push(var28);
      Matrix4f var29 = this.modelViewMatrixStack.alloc();
      float var30 = (float)(2.0 / Math.sqrt(2048.0));
      var29.scaling(scale);
      var29.scale((float)TileScale / 2.0F);
      var29.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
      var29.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
      double var31 = (double)var1 - var9;
      double var33 = (double)var2 - var11;
      var29.translate(-((float)var31), (float)((double)var3 - var13) * 2.5F, -((float)var33));
      if (var5) {
         var29.scale(-1.0F, 1.0F, 1.0F);
      } else {
         var29.scale(-1.5F, 1.5F, 1.5F);
      }

      var29.rotate(var4 + 3.1415927F, 0.0F, 1.0F, 0.0F);
      if (!var5) {
         var29.translate(0.0F, -0.48F, 0.0F);
      }

      var29.rotate(-1.5707964F, 1.0F, 0.0F, 0.0F);
      this.modelViewMatrixStack.push(var29);
      GL11.glDepthRange(0.0, 1.0);
   }

   public void DoPushIsoParticleStuff(float var1, float var2, float var3) {
      Matrix4f var4 = this.projectionMatrixStack.alloc();
      float var5 = (Float)getInstance().FloatParamMap.get(0);
      float var6 = (Float)getInstance().FloatParamMap.get(1);
      float var7 = (Float)getInstance().FloatParamMap.get(2);
      float var11 = (float)Math.abs(getInstance().getOffscreenWidth(0)) / 1920.0F;
      float var12 = (float)Math.abs(getInstance().getOffscreenHeight(0)) / 1080.0F;
      var4.setOrtho(-var11 / 2.0F, var11 / 2.0F, -var12 / 2.0F, var12 / 2.0F, -10.0F, 10.0F);
      this.projectionMatrixStack.push(var4);
      Matrix4f var13 = this.modelViewMatrixStack.alloc();
      var13.scaling(scale, scale, scale);
      var13.rotate(62.65607F, 1.0F, 0.0F, 0.0F);
      var13.translate(0.0F, -2.72F, 0.0F);
      var13.rotate(135.0F, 0.0F, 1.0F, 0.0F);
      var13.scale(1.7099999F, 14.193F, 1.7099999F);
      var13.scale(0.59F, 0.59F, 0.59F);
      var13.translate(-(var1 - var5), var3 - var7, -(var2 - var6));
      this.modelViewMatrixStack.push(var13);
      GL11.glDepthRange(0.0, 1.0);
   }

   public void DoPopIsoStuff() {
      GL11.glDepthRange(0.0, 1.0);
      GL11.glEnable(3008);
      GL11.glDepthFunc(519);
      GL11.glDepthMask(false);
      GLStateRenderThread.AlphaTest.restore();
      GLStateRenderThread.DepthFunc.restore();
      GLStateRenderThread.DepthMask.restore();
      this.projectionMatrixStack.pop();
      this.modelViewMatrixStack.pop();
   }

   public void DoEndFrameStuff(int var1, int var2) {
      try {
         GL11.glPopAttrib();
         --this.stack;
         this.projectionMatrixStack.pop();
         this.modelViewMatrixStack.pop();
         --this.stack;
      } catch (Throwable var5) {
         int var4 = GL11.glGetInteger(2992);

         while(var4-- > 0) {
            GL11.glPopAttrib();
         }

         this.stack = 0;
      }

      GL11.glDisable(3089);
   }

   public void RenderOffScreenBuffer() {
      if (LuaManager.thread == null || !LuaManager.thread.bStep) {
         if (this.OffscreenBuffer.Current != null) {
            IndieGL.disableStencilTest();
            IndieGL.glDoStartFrame(width, height, 1.0F, -1);
            IndieGL.disableBlend();
            this.OffscreenBuffer.render();
            IndieGL.glDoEndFrame();
            IndieGL.enableBlend();
         }
      }
   }

   public void StartFrameText(int var1) {
      if (LuaManager.thread == null || !LuaManager.thread.bStep) {
         IndieGL.glDoStartFrame(IsoCamera.getScreenWidth(var1), IsoCamera.getScreenHeight(var1), 1.0F, var1, true);
         this.frameStage = 2;
      }
   }

   public boolean StartFrameUI() {
      if (LuaManager.thread != null && LuaManager.thread.bStep) {
         return false;
      } else {
         boolean var1 = true;
         if (UIManager.useUIFBO) {
            if (UIManager.defaultthread == LuaManager.debugthread) {
               this.UIRenderThisFrame = true;
            } else {
               this.UIRenderAccumulator += GameTime.getInstance().getThirtyFPSMultiplier();
               this.UIRenderThisFrame = this.UIRenderAccumulator >= 30.0F / (float)getInstance().getOptionUIRenderFPS();
            }

            if (this.UIRenderThisFrame) {
               SpriteRenderer.instance.startOffscreenUI();
               SpriteRenderer.instance.glBuffer(2, 0);
            } else {
               var1 = false;
            }
         } else {
            UIManager.UITextureContentsValid = false;
         }

         IndieGL.glDoStartFrame(width, height, 1.0F, -1);
         IndieGL.glClear(1024);
         UIManager.resize();
         this.frameStage = 3;
         return var1;
      }
   }

   public Map<String, Integer> getKeyMaps() {
      return this.keyMaps;
   }

   public Map<String, Integer> getAltKeyMaps() {
      return this.altKeyMaps;
   }

   public void setKeyMaps(Map<String, Integer> var1) {
      this.keyMaps = var1;
   }

   public String getBindForKey(int var1) {
      Iterator var2 = this.keyMaps.keySet().iterator();

      String var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (String)var2.next();
      } while((Integer)this.keyMaps.get(var3) != var1 && (Integer)this.altKeyMaps.get(var3) != var1);

      return var3;
   }

   public void reinitKeyMaps() {
      this.keyMaps = new HashMap();
      this.altKeyMaps = new HashMap();
   }

   public boolean isKey(String var1, Integer var2) {
      return this.getKey(var1) == var2 || this.getAltKey(var1) == var2;
   }

   public int getKey(String var1) {
      return this.keyMaps == null ? 0 : (Integer)this.keyMaps.getOrDefault(var1, 0);
   }

   public void addKeyBinding(String var1, Integer var2) {
      if (this.keyMaps == null) {
         this.keyMaps = new HashMap();
      }

      this.keyMaps.put(var1, var2);
   }

   public int getAltKey(String var1) {
      if (this.altKeyMaps == null) {
         return 0;
      } else {
         return this.altKeyMaps.get(var1) != null ? (Integer)this.altKeyMaps.get(var1) : 0;
      }
   }

   public void addAltKeyBinding(String var1, Integer var2) {
      if (this.altKeyMaps == null) {
         this.altKeyMaps = new HashMap();
      }

      this.altKeyMaps.put(var1, var2);
      if (this.getKey(var1) == 0) {
         this.addKeyBinding(var1, var2);
      }

   }

   public static boolean isLastStand() {
      return bLastStand;
   }

   public String getVersion() {
      GameVersion var10000 = gameVersion;
      return "" + var10000 + ".0 " + getSVNRevisionString();
   }

   public static String getSVNRevisionString() {
      return " rev:25057 2024-12-17 17:38:44 (ZB)";
   }

   public GameVersion getGameVersion() {
      return gameVersion;
   }

   public GameVersion getBreakModGameVersion() {
      return new GameVersion(42, 0, "");
   }

   public String getSteamServerVersion() {
      return this.steamServerVersion;
   }

   public void DoFrameReady() {
      this.updateKeyboard();
   }

   public float getCurrentPlayerZoom() {
      int var1 = IsoCamera.frameState.playerIndex;
      return this.getZoom(var1);
   }

   public float getZoom(int var1) {
      return this.OffscreenBuffer != null ? this.OffscreenBuffer.getDisplayZoom(var1) * ((float)TileScale / 2.0F) : 1.0F;
   }

   public float getNextZoom(int var1, int var2) {
      return this.OffscreenBuffer != null ? this.OffscreenBuffer.getNextZoom(var1, var2) : 1.0F;
   }

   public float getMinZoom() {
      return this.OffscreenBuffer != null ? this.OffscreenBuffer.getMinZoom() * ((float)TileScale / 2.0F) : 1.0F;
   }

   public float getMaxZoom() {
      return this.OffscreenBuffer != null ? this.OffscreenBuffer.getMaxZoom() * ((float)TileScale / 2.0F) : 1.0F;
   }

   public void doZoomScroll(int var1, int var2) {
      if (this.OffscreenBuffer != null) {
         this.OffscreenBuffer.doZoomScroll(var1, var2);
      }

   }

   public String getSaveFolder() {
      return this.saveFolder;
   }

   public boolean getOptionZoom() {
      return this.OptionZoom.getValue();
   }

   public void setOptionZoom(boolean var1) {
      this.OptionZoom.setValue(var1);
   }

   public void zoomOptionChanged(boolean var1) {
      if (var1) {
         RenderThread.invokeOnRenderContext(() -> {
            if (this.getOptionZoom() && !SafeModeForced) {
               SafeMode = false;
               this.bSupportsFBO = true;
               this.OffscreenBuffer.bZoomEnabled = true;
               this.supportsFBO();
            } else {
               this.OffscreenBuffer.destroy();
               SafeMode = true;
               this.bSupportsFBO = false;
               this.OffscreenBuffer.bZoomEnabled = false;
            }

         });
         DebugLog.log("SafeMode is " + (SafeMode ? "on" : "off"));
      } else {
         SafeMode = SafeModeForced;
         this.OffscreenBuffer.bZoomEnabled = this.getOptionZoom() && !SafeModeForced;
      }
   }

   public void zoomLevelsChanged() {
      if (this.OffscreenBuffer.Current != null) {
         RenderThread.invokeOnRenderContext(() -> {
            this.OffscreenBuffer.destroy();
            this.zoomOptionChanged(true);
         });
      }

   }

   public boolean isZoomEnabled() {
      return this.OffscreenBuffer.bZoomEnabled;
   }

   public void setZoomEnalbed(boolean var1) {
      this.OffscreenBuffer.bZoomEnabled = var1;
   }

   public void initFBOs() {
      if (this.getOptionZoom() && !SafeModeForced) {
         RenderThread.invokeOnRenderContext(this::supportsFBO);
      } else {
         SafeMode = true;
         this.OffscreenBuffer.bZoomEnabled = false;
      }

      DebugLog.log("SafeMode is " + (SafeMode ? "on" : "off"));
   }

   public boolean getAutoZoom(int var1) {
      return ((BooleanConfigOption)this.bAutoZoom.getElement(var1)).getValue();
   }

   public void setAutoZoom(int var1, boolean var2) {
      ((BooleanConfigOption)this.bAutoZoom.getElement(var1)).setValue(var2);
      this.OffscreenBuffer.bAutoZoom[var1] = var2;
   }

   public boolean getOptionVSync() {
      return this.OptionVSync.getValue();
   }

   public void setOptionVSync(boolean var1) {
      this.OptionVSync.setValue(var1);
      RenderThread.invokeOnRenderContext(() -> {
         Display.setVSyncEnabled(var1);
      });
   }

   public int getOptionSoundVolume() {
      return this.OptionSoundVolume.getValue();
   }

   public float getRealOptionSoundVolume() {
      return (float)this.OptionSoundVolume.getValue() / 10.0F;
   }

   public void setOptionSoundVolume(int var1) {
      this.OptionSoundVolume.setValue(var1);
      if (SoundManager.instance != null) {
         SoundManager.instance.setSoundVolume((float)this.getOptionSoundVolume() / 10.0F);
      }

   }

   public int getOptionMusicVolume() {
      return this.OptionMusicVolume.getValue();
   }

   public void setOptionMusicVolume(int var1) {
      this.OptionMusicVolume.setValue(var1);
      if (SoundManager.instance != null) {
         SoundManager.instance.setMusicVolume((float)this.getOptionMusicVolume() / 10.0F);
      }

   }

   public int getOptionAmbientVolume() {
      return this.OptionAmbientVolume.getValue();
   }

   public void setOptionAmbientVolume(int var1) {
      this.OptionAmbientVolume.setValue(var1);
      if (SoundManager.instance != null) {
         SoundManager.instance.setAmbientVolume((float)this.getOptionAmbientVolume() / 10.0F);
      }

   }

   public int getOptionJumpScareVolume() {
      return this.OptionJumpScareVolume.getValue();
   }

   public void setOptionJumpScareVolume(int var1) {
      this.OptionJumpScareVolume.setValue(var1);
   }

   public int getOptionMusicActionStyle() {
      return this.OptionMusicActionStyle.getValue();
   }

   public void setOptionMusicActionStyle(int var1) {
      this.OptionMusicActionStyle.setValue(var1);
   }

   public int getOptionMusicLibrary() {
      return this.OptionMusicLibrary.getValue();
   }

   public void setOptionMusicLibrary(int var1) {
      this.OptionMusicLibrary.setValue(var1);
   }

   public int getOptionVehicleEngineVolume() {
      return this.OptionVehicleEngineVolume.getValue();
   }

   public void setOptionVehicleEngineVolume(int var1) {
      this.OptionVehicleEngineVolume.setValue(var1);
      if (SoundManager.instance != null) {
         SoundManager.instance.setVehicleEngineVolume((float)this.getOptionVehicleEngineVolume() / 10.0F);
      }

   }

   public boolean getOptionStreamerMode() {
      return this.OptionStreamerMode.getValue();
   }

   public void setOptionStreamerMode(boolean var1) {
      this.OptionStreamerMode.setValue(var1);
   }

   public boolean getOptionVoiceEnable() {
      return this.OptionVoiceEnable.getValue();
   }

   public void setOptionVoiceEnable(boolean var1) {
      this.setOptionVoiceEnable(var1, true);
   }

   public void setOptionVoiceEnable(boolean var1, boolean var2) {
      if (this.OptionVoiceEnable.getValue() != var1) {
         this.OptionVoiceEnable.setValue(var1);
         if (var2) {
            VoiceManager.instance.VoiceRestartClient(var1);
         }
      }

   }

   public int getOptionVoiceMode() {
      return this.OptionVoiceMode.getValue();
   }

   public void setOptionVoiceMode(int var1) {
      this.OptionVoiceMode.setValue(var1);
      VoiceManager.instance.setMode(this.getOptionVoiceMode());
   }

   public int getOptionVoiceVADMode() {
      return this.OptionVoiceVADMode.getValue();
   }

   public void setOptionVoiceVADMode(int var1) {
      this.OptionVoiceVADMode.setValue(var1);
      VoiceManager.instance.setVADMode(this.getOptionVoiceVADMode());
   }

   public int getOptionVoiceAGCMode() {
      return this.OptionVoiceAGCMode.getValue();
   }

   public void setOptionVoiceAGCMode(int var1) {
      this.OptionVoiceAGCMode.setValue(var1);
      VoiceManager.instance.setAGCMode(this.getOptionVoiceAGCMode());
   }

   public int getOptionVoiceVolumeMic() {
      return this.OptionVoiceVolumeMic.getValue();
   }

   public void setOptionVoiceVolumeMic(int var1) {
      this.OptionVoiceVolumeMic.setValue(var1);
      VoiceManager.instance.setVolumeMic(this.getOptionVoiceVolumeMic());
   }

   public int getOptionVoiceVolumePlayers() {
      return this.OptionVoiceVolumePlayers.getValue();
   }

   public void setOptionVoiceVolumePlayers(int var1) {
      this.OptionVoiceVolumePlayers.setValue(var1);
      VoiceManager.instance.setVolumePlayers(var1);
   }

   public String getOptionVoiceRecordDeviceName() {
      return this.OptionVoiceRecordDeviceName.getValue();
   }

   public void setOptionVoiceRecordDeviceName(String var1) {
      this.OptionVoiceRecordDeviceName.setValue(var1);
      VoiceManager.instance.UpdateRecordDevice();
   }

   public int getOptionVoiceRecordDevice() {
      if (!SoundDisabled && !VoiceManager.VoipDisabled) {
         int var1 = javafmod.FMOD_System_GetRecordNumDrivers();

         for(int var2 = 0; var2 < var1; ++var2) {
            FMOD_DriverInfo var3 = new FMOD_DriverInfo();
            javafmod.FMOD_System_GetRecordDriverInfo(var2, var3);
            if (var3.name.equals(this.getOptionVoiceRecordDeviceName())) {
               return var2 + 1;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public void setOptionVoiceRecordDevice(int var1) {
      if (!SoundDisabled && !VoiceManager.VoipDisabled) {
         if (var1 >= 1) {
            FMOD_DriverInfo var2 = new FMOD_DriverInfo();
            javafmod.FMOD_System_GetRecordDriverInfo(var1 - 1, var2);
            this.OptionVoiceRecordDeviceName.setValue(var2.name);
            VoiceManager.instance.UpdateRecordDevice();
         }
      }
   }

   public int getMicVolumeIndicator() {
      return VoiceManager.instance.getMicVolumeIndicator();
   }

   public boolean getMicVolumeError() {
      return VoiceManager.instance.getMicVolumeError();
   }

   public boolean getServerVOIPEnable() {
      return VoiceManager.instance.getServerVOIPEnable();
   }

   public void setTestingMicrophone(boolean var1) {
      VoiceManager.instance.setTestingMicrophone(var1);
   }

   public int getOptionReloadDifficulty() {
      return 2;
   }

   public void setOptionReloadDifficulty(int var1) {
      this.OptionReloadDifficulty.setValue(var1);
   }

   public boolean getOptionRackProgress() {
      return this.OptionRackProgress.getValue();
   }

   public void setOptionRackProgress(boolean var1) {
      this.OptionRackProgress.setValue(var1);
   }

   public int getOptionFontSize() {
      return this.OptionFontSize.getValue();
   }

   public void setOptionFontSize(int var1) {
      this.OptionFontSize.setValue(var1);
   }

   public int getOptionFontSizeReal() {
      int var1 = this.getOptionFontSize();
      if (var1 == 6) {
         short var2 = 1080;
         short var3 = 2160;
         int var4 = (var3 - var2) / 10;
         int var5 = (int)PZMath.floor((float)(getInstance().getScreenHeight() - var2) / (float)var4) + 1;
         int var6 = PZMath.clamp(var5, 0, 10);
         switch (var6) {
            case 0:
               var1 = 1;
               break;
            case 1:
               var1 = 2;
               break;
            case 2:
            case 3:
               var1 = 3;
               break;
            case 4:
            case 5:
            case 6:
               var1 = 4;
               break;
            case 7:
            case 8:
            case 9:
            case 10:
               var1 = 5;
         }
      }

      return var1;
   }

   public int getOptionMoodleSize() {
      return this.OptionMoodleSize.getValue();
   }

   public void setOptionMoodleSize(int var1) {
      this.OptionMoodleSize.setValue(var1);
   }

   public int getOptionActionProgressBarSize() {
      return this.OptionActionProgressBarSize.getValue();
   }

   public void setOptionActionProgressBarSize(int var1) {
      this.OptionActionProgressBarSize.setValue(var1);
   }

   public String getOptionContextMenuFont() {
      return this.OptionContextMenuFont.getValue();
   }

   public void setOptionContextMenuFont(String var1) {
      this.OptionContextMenuFont.setValue(var1);
   }

   public String getOptionInventoryFont() {
      return this.OptionInventoryFont.getValue();
   }

   public void setOptionInventoryFont(String var1) {
      this.OptionInventoryFont.setValue(var1);
   }

   public int getOptionInventoryContainerSize() {
      return this.OptionInventoryContainerSize.getValue();
   }

   public void setOptionInventoryContainerSize(int var1) {
      this.OptionInventoryContainerSize.setValue(var1);
   }

   public String getOptionTooltipFont() {
      return this.OptionTooltipFont.getValue();
   }

   public void setOptionTooltipFont(String var1) {
      this.OptionTooltipFont.setValue(var1);
      ObjectTooltip.checkFont();
   }

   public String getOptionMeasurementFormat() {
      return this.OptionMeasurementFormat.getValue();
   }

   public void setOptionMeasurementFormat(String var1) {
      this.OptionMeasurementFormat.setValue(var1);
   }

   public int getOptionClockFormat() {
      return this.OptionClockFormat.getValue();
   }

   public int getOptionClockSize() {
      return this.OptionClockSize.getValue();
   }

   public void setOptionClockFormat(int var1) {
      this.OptionClockFormat.setValue(var1);
   }

   public void setOptionClockSize(int var1) {
      this.OptionClockSize.setValue(var1);
   }

   public boolean getOptionClock24Hour() {
      return this.OptionClock24Hour.getValue();
   }

   public void setOptionClock24Hour(boolean var1) {
      this.OptionClock24Hour.setValue(var1);
   }

   public boolean getOptionModsEnabled() {
      return OptionModsEnabled;
   }

   public void setOptionModsEnabled(boolean var1) {
      OptionModsEnabled = var1;
   }

   public int getOptionBloodDecals() {
      return this.OptionBloodDecals.getValue();
   }

   public void setOptionBloodDecals(int var1) {
      this.OptionBloodDecals.setValue(var1);
   }

   public boolean getOptionFocusloss() {
      return this.OptionFocusloss.getValue();
   }

   public void setOptionFocusloss(boolean var1) {
      this.OptionFocusloss.setValue(var1);
   }

   public boolean getOptionBorderlessWindow() {
      return this.OptionBorderlessWindow.getValue();
   }

   public void setOptionBorderlessWindow(boolean var1) {
      this.OptionBorderlessWindow.setValue(var1);
   }

   public boolean getOptionLockCursorToWindow() {
      return this.OptionLockCursorToWindow.getValue();
   }

   public void setOptionLockCursorToWindow(boolean var1) {
      this.OptionLockCursorToWindow.setValue(var1);
   }

   public boolean getOptionTextureCompression() {
      return this.OptionTextureCompression.getValue();
   }

   public void setOptionTextureCompression(boolean var1) {
      this.OptionTextureCompression.setValue(var1);
   }

   public boolean getOptionTexture2x() {
      return true;
   }

   public void setOptionTexture2x(boolean var1) {
      this.OptionTexture2x.setValue(var1);
      DebugLog.General.warn("1x textures are disabled.");
   }

   public boolean getOptionHighResPlacedItems() {
      return this.OptionHighResPlacedItems.getValue();
   }

   public void setOptionHighResPlacedItems(boolean var1) {
      this.OptionHighResPlacedItems.setValue(var1);
   }

   public int getOptionMaxTextureSize() {
      return this.OptionMaxTextureSize.getValue();
   }

   public void setOptionMaxTextureSize(int var1) {
      this.OptionMaxTextureSize.setValue(var1);
   }

   public int getOptionMaxVehicleTextureSize() {
      return this.OptionMaxVehicleTextureSize.getValue();
   }

   public void setOptionMaxVehicleTextureSize(int var1) {
      this.OptionMaxVehicleTextureSize.setValue(var1);
   }

   public int getMaxTextureSizeFromFlags(int var1) {
      if ((var1 & 128) != 0) {
         return this.getMaxTextureSize();
      } else {
         return (var1 & 256) != 0 ? this.getMaxVehicleTextureSize() : '耀';
      }
   }

   public int getMaxTextureSizeFromOption(int var1) {
      short var10000;
      switch (var1) {
         case 1:
            var10000 = 256;
            break;
         case 2:
            var10000 = 512;
            break;
         case 3:
            var10000 = 1024;
            break;
         case 4:
            var10000 = 2048;
            break;
         default:
            throw new IllegalStateException("Unexpected value: " + var1);
      }

      return var10000;
   }

   public int getMaxTextureSize() {
      return this.getMaxTextureSizeFromOption(this.OptionMaxTextureSize.getValue());
   }

   public int getMaxVehicleTextureSize() {
      return this.getMaxTextureSizeFromOption(this.OptionMaxVehicleTextureSize.getValue());
   }

   public boolean getOptionModelTextureMipmaps() {
      return this.OptionModelTextureMipmaps.getValue();
   }

   public void setOptionModelTextureMipmaps(boolean var1) {
      this.OptionModelTextureMipmaps.setValue(var1);
   }

   public String getOptionZoomLevels1x() {
      return this.OptionZoomLevels1x.getValueAsString();
   }

   public void setOptionZoomLevels1x(String var1) {
      this.OptionZoomLevels1x.parse(var1 == null ? "" : var1);
   }

   public String getOptionZoomLevels2x() {
      return this.OptionZoomLevels2x.getValueAsString();
   }

   public void setOptionZoomLevels2x(String var1) {
      this.OptionZoomLevels2x.parse(var1 == null ? "" : var1);
   }

   public ArrayList<Integer> getDefaultZoomLevels() {
      return this.OffscreenBuffer.getDefaultZoomLevels();
   }

   public void setOptionActiveController(int var1, boolean var2) {
      if (var1 >= 0 && var1 < GameWindow.GameInput.getControllerCount()) {
         Controller var3 = GameWindow.GameInput.getController(var1);
         if (var3 != null) {
            JoypadManager.instance.setControllerActive(var3.getGUID(), var2);
         }

      }
   }

   public boolean getOptionActiveController(String var1) {
      return JoypadManager.instance.ActiveControllerGUIDs.contains(var1);
   }

   public boolean isOptionShowChatTimestamp() {
      return this.OptionShowChatTimestamp.getValue();
   }

   public void setOptionShowChatTimestamp(boolean var1) {
      this.OptionShowChatTimestamp.setValue(var1);
   }

   public boolean isOptionShowChatTitle() {
      return this.OptionShowChatTitle.getValue();
   }

   public String getOptionChatFontSize() {
      return this.OptionChatFontSize.getValue();
   }

   public void setOptionChatFontSize(String var1) {
      this.OptionChatFontSize.setValue(var1);
   }

   public void setOptionShowChatTitle(boolean var1) {
      this.OptionShowChatTitle.setValue(var1);
   }

   public float getOptionMinChatOpaque() {
      return (float)this.OptionMinChatOpaque.getValue();
   }

   public void setOptionMinChatOpaque(float var1) {
      this.OptionMinChatOpaque.setValue((double)var1);
   }

   public float getOptionMaxChatOpaque() {
      return (float)this.OptionMaxChatOpaque.getValue();
   }

   public void setOptionMaxChatOpaque(float var1) {
      this.OptionMaxChatOpaque.setValue((double)var1);
   }

   public float getOptionChatFadeTime() {
      return (float)this.OptionChatFadeTime.getValue();
   }

   public void setOptionChatFadeTime(float var1) {
      this.OptionChatFadeTime.setValue((double)var1);
   }

   public boolean getOptionChatOpaqueOnFocus() {
      return this.OptionChatOpaqueOnFocus.getValue();
   }

   public void setOptionChatOpaqueOnFocus(boolean var1) {
      this.OptionChatOpaqueOnFocus.setValue(var1);
   }

   public boolean getOptionTemperatureDisplayCelsius() {
      return this.OptionTemperatureDisplayCelsius.getValue();
   }

   public boolean getOptionUIFBO() {
      return this.OptionUIFBO.getValue();
   }

   public void setOptionUIFBO(boolean var1) {
      this.OptionUIFBO.setValue(var1);
      if (GameWindow.states.current == IngameState.instance) {
         UIManager.useUIFBO = getInstance().supportsFBO() && this.getOptionUIFBO();
      }

   }

   public boolean getOptionMeleeOutline() {
      return this.OptionMeleeOutline.getValue();
   }

   public void setOptionMeleeOutline(boolean var1) {
      this.OptionMeleeOutline.setValue(var1);
   }

   public int getOptionUIRenderFPS() {
      return this.OptionUIRenderFPS.getValue();
   }

   public void setOptionUIRenderFPS(int var1) {
      this.OptionUIRenderFPS.setValue(var1);
   }

   public void setOptionRadialMenuKeyToggle(boolean var1) {
      this.OptionRadialMenuKeyToggle.setValue(var1);
   }

   public boolean getOptionRadialMenuKeyToggle() {
      return this.OptionRadialMenuKeyToggle.getValue();
   }

   public void setOptionReloadRadialInstant(boolean var1) {
      this.OptionReloadRadialInstant.setValue(var1);
   }

   public boolean getOptionReloadRadialInstant() {
      return this.OptionReloadRadialInstant.getValue();
   }

   public void setOptionPanCameraWhileAiming(boolean var1) {
      this.OptionPanCameraWhileAiming.setValue(var1);
   }

   public boolean getOptionPanCameraWhileAiming() {
      return this.OptionPanCameraWhileAiming.getValue();
   }

   public void setOptionPanCameraWhileDriving(boolean var1) {
      this.OptionPanCameraWhileDriving.setValue(var1);
   }

   public boolean getOptionPanCameraWhileDriving() {
      return this.OptionPanCameraWhileDriving.getValue();
   }

   public String getOptionCycleContainerKey() {
      return this.OptionCycleContainerKey.getValue();
   }

   public void setOptionCycleContainerKey(String var1) {
      this.OptionCycleContainerKey.setValue(var1);
   }

   public boolean getOptionDropItemsOnSquareCenter() {
      return this.OptionDropItemsOnSquareCenter.getValue();
   }

   public void setOptionDropItemsOnSquareCenter(boolean var1) {
      this.OptionDropItemsOnSquareCenter.setValue(var1);
   }

   public boolean getOptionTimedActionGameSpeedReset() {
      return this.OptionTimedActionGameSpeedReset.getValue();
   }

   public void setOptionTimedActionGameSpeedReset(boolean var1) {
      this.OptionTimedActionGameSpeedReset.setValue(var1);
   }

   public int getOptionShoulderButtonContainerSwitch() {
      return this.OptionShoulderButtonContainerSwitch.getValue();
   }

   public void setOptionShoulderButtonContainerSwitch(int var1) {
      this.OptionShoulderButtonContainerSwitch.setValue(var1);
   }

   public int getOptionControllerButtonStyle() {
      return this.OptionControllerButtonStyle.getValue();
   }

   public void setOptionControllerButtonStyle(int var1) {
      this.OptionControllerButtonStyle.setValue(var1);
   }

   public boolean getOptionSingleContextMenu(int var1) {
      return ((BooleanConfigOption)this.OptionSingleContextMenu.getElement(var1)).getValue();
   }

   public void setOptionSingleContextMenu(int var1, boolean var2) {
      ((BooleanConfigOption)this.OptionSingleContextMenu.getElement(var1)).setValue(var2);
   }

   public boolean getOptionAutoDrink() {
      return this.OptionAutoDrink.getValue();
   }

   public void setOptionAutoDrink(boolean var1) {
      this.OptionAutoDrink.setValue(var1);
   }

   public boolean getOptionAutoWalkContainer() {
      return this.OptionAutoWalkContainer.getValue();
   }

   public void setOptionAutoWalkContainer(boolean var1) {
      this.OptionAutoWalkContainer.setValue(var1);
   }

   public boolean getOptionCorpseShadows() {
      return this.OptionCorpseShadows.getValue();
   }

   public void setOptionCorpseShadows(boolean var1) {
      this.OptionCorpseShadows.setValue(var1);
   }

   public boolean getOptionLeaveKeyInIgnition() {
      return this.OptionLeaveKeyInIgnition.getValue();
   }

   public void setOptionLeaveKeyInIgnition(boolean var1) {
      this.OptionLeaveKeyInIgnition.setValue(var1);
   }

   public int getOptionSearchModeOverlayEffect() {
      return this.OptionSearchModeOverlayEffect.getValue();
   }

   public void setOptionSearchModeOverlayEffect(int var1) {
      this.OptionSearchModeOverlayEffect.setValue(var1);
   }

   public int getOptionSimpleClothingTextures() {
      return this.OptionSimpleClothingTextures.getValue();
   }

   public void setOptionSimpleClothingTextures(int var1) {
      this.OptionSimpleClothingTextures.setValue(var1);
   }

   public boolean isOptionSimpleClothingTextures(boolean var1) {
      switch (this.getOptionSimpleClothingTextures()) {
         case 1:
            return false;
         case 2:
            return var1;
         default:
            return true;
      }
   }

   public boolean getOptionSimpleWeaponTextures() {
      return this.OptionSimpleWeaponTextures.getValue();
   }

   public void setOptionSimpleWeaponTextures(boolean var1) {
      this.OptionSimpleWeaponTextures.setValue(var1);
   }

   public int getOptionIgnoreProneZombieRange() {
      return this.OptionIgnoreProneZombieRange.getValue();
   }

   public void setOptionIgnoreProneZombieRange(int var1) {
      this.OptionIgnoreProneZombieRange.setValue(var1);
   }

   public float getIgnoreProneZombieRange() {
      switch (this.OptionIgnoreProneZombieRange.getValue()) {
         case 1:
            return -1.0F;
         case 2:
            return 1.5F;
         case 3:
            return 2.0F;
         case 4:
            return 2.5F;
         case 5:
            return 3.0F;
         default:
            return -1.0F;
      }
   }

   private void readPerPlayerBoolean(String var1, boolean[] var2) {
      Arrays.fill(var2, false);
      String[] var3 = var1.split(",");

      for(int var4 = 0; var4 < var3.length && var4 != 4; ++var4) {
         var2[var4] = StringUtils.tryParseBoolean(var3[var4]);
      }

   }

   private String getPerPlayerBooleanString(boolean[] var1) {
      return String.format("%b,%b,%b,%b", var1[0], var1[1], var1[2], var1[3]);
   }

   /** @deprecated */
   @Deprecated
   public void ResetLua(boolean var1, String var2) throws IOException {
      this.ResetLua("default", var2);
   }

   public void ResetLua(String var1, String var2) throws IOException {
      if (SpriteRenderer.instance != null) {
         GameWindow.DrawReloadingLua = true;
         GameWindow.render();
         GameWindow.DrawReloadingLua = false;
      }

      RenderThread.setWaitForRenderState(false);
      SpriteRenderer.instance.notifyRenderStateQueue();
      ScriptManager.instance.Reset();
      ClothingDecals.Reset();
      BeardStyles.Reset();
      HairStyles.Reset();
      OutfitManager.Reset();
      AnimationSet.Reset();
      GameSounds.Reset();
      VehicleType.Reset();
      LuaEventManager.Reset();
      MapObjects.Reset();
      UIManager.init();
      SurvivorFactory.Reset();
      ProfessionFactory.Reset();
      TraitFactory.Reset();
      ChooseGameInfo.Reset();
      AttachedLocations.Reset();
      BodyLocations.Reset();
      ContainerOverlays.instance.Reset();
      BentFences.getInstance().Reset();
      BrokenFences.getInstance().Reset();
      TileOverlays.instance.Reset();
      LuaHookManager.Reset();
      CustomPerks.Reset();
      PerkFactory.Reset();
      CustomSandboxOptions.Reset();
      SandboxOptions.Reset();
      WorldMap.Reset();
      AnimalDefinitions.Reset();
      AnimalZones.Reset();
      MigrationGroupDefinitions.Reset();
      LuaManager.init();
      JoypadManager.instance.Reset();
      GameKeyboard.doLuaKeyPressed = true;
      Texture.nullTextures.clear();
      SpriteModelManager.getInstance().Reset();
      TileGeometryManager.getInstance().Reset();
      TileDepthTextureManager.getInstance().Reset();
      SeamManager.getInstance().Reset();
      SeatingManager.getInstance().Reset();
      ZomboidFileSystem.instance.Reset();
      ZomboidFileSystem.instance.init();
      ZomboidFileSystem.instance.loadMods(var1);
      ZomboidFileSystem.instance.loadModPackFiles();
      Languages.instance.init();
      Translator.loadFiles();
      CustomPerks.instance.init();
      CustomPerks.instance.initLua();
      CustomSandboxOptions.instance.init();
      CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
      ScriptManager.instance.Load();
      SpriteModelManager.getInstance().init();
      ModelManager.instance.initAnimationMeshes(true);
      ModelManager.instance.loadModAnimations();
      ClothingDecals.init();
      BeardStyles.init();
      HairStyles.init();
      OutfitManager.init();
      VoiceStyles.init();
      TileGeometryManager.getInstance().init();
      TileDepthTextureAssignmentManager.getInstance().init();
      TileDepthTextureManager.getInstance().init();
      SeamManager.getInstance().init();
      SeatingManager.getInstance().init();

      try {
         TextManager.instance.Init();
         LuaManager.LoadDirBase();
      } catch (Exception var6) {
         ExceptionLogger.logException(var6);
         GameWindow.DoLoadingText("Reloading Lua - ERRORS!");

         try {
            Thread.sleep(2000L);
         } catch (InterruptedException var5) {
         }
      }

      ZomboidGlobals.Load();
      RenderThread.setWaitForRenderState(true);
      LuaEventManager.triggerEvent("OnGameBoot");
      LuaEventManager.triggerEvent("OnMainMenuEnter");
      LuaEventManager.triggerEvent("OnResetLua", var2);
   }

   public void DelayResetLua(String var1, String var2) {
      this.m_delayResetLua_activeMods = var1;
      this.m_delayResetLua_reason = var2;
   }

   public void CheckDelayResetLua() throws IOException {
      if (this.m_delayResetLua_activeMods != null) {
         String var1 = this.m_delayResetLua_activeMods;
         String var2 = this.m_delayResetLua_reason;
         this.m_delayResetLua_activeMods = null;
         this.m_delayResetLua_reason = null;
         this.ResetLua(var1, var2);
      }

   }

   public boolean isShowPing() {
      return this.showPing;
   }

   public void setShowPing(boolean var1) {
      this.showPing = var1;
   }

   public boolean isForceSnow() {
      return this.forceSnow;
   }

   public void setForceSnow(boolean var1) {
      this.forceSnow = var1;
   }

   public boolean isZombieGroupSound() {
      return this.zombieGroupSound;
   }

   public void setZombieGroupSound(boolean var1) {
      this.zombieGroupSound = var1;
   }

   public String getBlinkingMoodle() {
      return this.blinkingMoodle;
   }

   public void setBlinkingMoodle(String var1) {
      this.blinkingMoodle = var1;
   }

   public boolean isTutorialDone() {
      return this.OptionTutorialDone.getValue();
   }

   public void setTutorialDone(boolean var1) {
      this.OptionTutorialDone.setValue(var1);
   }

   public boolean isVehiclesWarningShow() {
      return this.OptionVehiclesWarningShow.getValue();
   }

   public void setVehiclesWarningShow(boolean var1) {
      this.OptionVehiclesWarningShow.setValue(var1);
   }

   public void initPoisonousBerry() {
      ArrayList var1 = new ArrayList();
      var1.add("Base.BerryGeneric1");
      var1.add("Base.BerryGeneric2");
      var1.add("Base.BerryGeneric3");
      var1.add("Base.BerryGeneric4");
      var1.add("Base.BerryGeneric5");
      var1.add("Base.BerryPoisonIvy");
      this.setPoisonousBerry((String)var1.get(Rand.Next(0, var1.size() - 1)));
   }

   public void initPoisonousMushroom() {
      ArrayList var1 = new ArrayList();
      var1.add("Base.MushroomGeneric1");
      var1.add("Base.MushroomGeneric2");
      var1.add("Base.MushroomGeneric3");
      var1.add("Base.MushroomGeneric4");
      var1.add("Base.MushroomGeneric5");
      var1.add("Base.MushroomGeneric6");
      var1.add("Base.MushroomGeneric7");
      this.setPoisonousMushroom((String)var1.get(Rand.Next(0, var1.size() - 1)));
   }

   public String getPoisonousBerry() {
      return this.poisonousBerry;
   }

   public void setPoisonousBerry(String var1) {
      this.poisonousBerry = var1;
   }

   public String getPoisonousMushroom() {
      return this.poisonousMushroom;
   }

   public void setPoisonousMushroom(String var1) {
      this.poisonousMushroom = var1;
   }

   public static String getDifficulty() {
      return difficulty;
   }

   public static void setDifficulty(String var0) {
      difficulty = var0;
   }

   public boolean isDoneNewSaveFolder() {
      return this.OptionDoneNewSaveFolder.getValue();
   }

   public void setDoneNewSaveFolder(boolean var1) {
      this.OptionDoneNewSaveFolder.setValue(var1);
   }

   public static int getTileScale() {
      return TileScale;
   }

   public boolean isSelectingAll() {
      return this.isSelectingAll;
   }

   public void setIsSelectingAll(boolean var1) {
      this.isSelectingAll = var1;
   }

   public boolean getContentTranslationsEnabled() {
      return this.OptionEnableContentTranslations.getValue();
   }

   public void setContentTranslationsEnabled(boolean var1) {
      this.OptionEnableContentTranslations.setValue(var1);
   }

   public boolean isShowYourUsername() {
      return this.showYourUsername.getValue();
   }

   public void setShowYourUsername(boolean var1) {
      this.showYourUsername.setValue(var1);
   }

   public ColorInfo getMpTextColor() {
      if (this.mpTextColor == null) {
         this.mpTextColor = new ColorInfo((float)(Rand.Next(135) + 120) / 255.0F, (float)(Rand.Next(135) + 120) / 255.0F, (float)(Rand.Next(135) + 120) / 255.0F, 1.0F);
         this.OptionMPTextColor.setValueVarArgs((double)this.mpTextColor.r, (double)this.mpTextColor.g, (double)this.mpTextColor.b);
      }

      return this.OptionMPTextColor.getValueAsColorInfo(this.mpTextColor);
   }

   public void setMpTextColor(ColorInfo var1) {
      this.mpTextColor = var1;
      this.OptionMPTextColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
   }

   public boolean isAzerty() {
      return this.isAzerty;
   }

   public void setAzerty(boolean var1) {
      this.isAzerty = var1;
   }

   public ColorInfo getObjectHighlitedColor() {
      return this.OptionObjectHighlightColor.getValueAsColorInfo(this.objectHighlitedColor);
   }

   public void setObjectHighlitedColor(ColorInfo var1) {
      this.OptionObjectHighlightColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
      this.objectHighlitedColor.set(var1);
   }

   public ColorInfo getWorkstationHighlitedColor() {
      return this.OptionWorkstationHighlightColor.getValueAsColorInfo(this.workstationHighlitedColor);
   }

   public void setWorkstationHighlitedColor(ColorInfo var1) {
      this.OptionWorkstationHighlightColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
      this.workstationHighlitedColor.set(var1);
   }

   public ColorInfo getGoodHighlitedColor() {
      return this.OptionGoodHighlightColor.getValueAsColorInfo(this.goodHighlitedColor);
   }

   public void setGoodHighlitedColor(ColorInfo var1) {
      this.OptionGoodHighlightColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
      this.goodHighlitedColor.set(var1);
   }

   public ColorInfo getBadHighlitedColor() {
      return this.OptionBadHighlightColor.getValueAsColorInfo(this.badHighlitedColor);
   }

   public void setBadHighlitedColor(ColorInfo var1) {
      this.OptionBadHighlightColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
      this.badHighlitedColor.set(var1);
   }

   public boolean getOptionColorblindPatterns() {
      return this.OptionColorblindPatterns.getValue();
   }

   public void setOptionColorblindPatterns(boolean var1) {
      this.OptionColorblindPatterns.setValue(var1);
   }

   public boolean getOptionEnableDyslexicFont() {
      return this.OptionEnableDyslexicFont.getValue();
   }

   public void setOptionEnableDyslexicFont(boolean var1) {
      this.OptionEnableDyslexicFont.setValue(var1);
   }

   public boolean getOptionLightSensitivity() {
      return this.OptionLightSensitivity.getValue();
   }

   public void setOptionLightSensitivity(boolean var1) {
      this.OptionLightSensitivity.setValue(var1);
   }

   public String getSeenUpdateText() {
      return this.seenUpdateText.getValue();
   }

   public void setSeenUpdateText(String var1) {
      this.seenUpdateText.setValue(var1);
   }

   public boolean isToggleToAim() {
      return this.toggleToAim.getValue();
   }

   public void setToggleToAim(boolean var1) {
      this.toggleToAim.setValue(var1);
   }

   public boolean isToggleToRun() {
      return this.toggleToRun.getValue();
   }

   public void setToggleToRun(boolean var1) {
      this.toggleToRun.setValue(var1);
   }

   public int getXAngle(int var1, float var2) {
      double var3 = Math.toRadians((double)(225.0F + var2));
      int var5 = Long.valueOf(Math.round((Math.sqrt(2.0) * Math.cos(var3) + 1.0) * (double)(var1 / 2))).intValue();
      return var5;
   }

   public int getYAngle(int var1, float var2) {
      double var3 = Math.toRadians((double)(225.0F + var2));
      int var5 = Long.valueOf(Math.round((Math.sqrt(2.0) * Math.sin(var3) + 1.0) * (double)(var1 / 2))).intValue();
      return var5;
   }

   public boolean isCelsius() {
      return this.celsius.getValue();
   }

   public void setCelsius(boolean var1) {
      this.celsius.setValue(var1);
   }

   public boolean isInDebug() {
      return bDebug;
   }

   public boolean isRiversideDone() {
      return this.OptionRiversideDone.getValue();
   }

   public void setRiversideDone(boolean var1) {
      this.OptionRiversideDone.setValue(var1);
   }

   public boolean isNoSave() {
      return this.noSave;
   }

   public void setNoSave(boolean var1) {
      this.noSave = var1;
   }

   public boolean isShowFirstTimeVehicleTutorial() {
      return this.showFirstTimeVehicleTutorial;
   }

   public void setShowFirstTimeVehicleTutorial(boolean var1) {
      this.showFirstTimeVehicleTutorial = var1;
   }

   public boolean getOptionDisplayAsCelsius() {
      return this.OptionTemperatureDisplayCelsius.getValue();
   }

   public void setOptionDisplayAsCelsius(boolean var1) {
      this.OptionTemperatureDisplayCelsius.setValue(var1);
   }

   public boolean isShowFirstTimeWeatherTutorial() {
      return this.showFirstTimeWeatherTutorial;
   }

   public void setShowFirstTimeWeatherTutorial(boolean var1) {
      this.showFirstTimeWeatherTutorial = var1;
   }

   public boolean getOptionDoVideoEffects() {
      return this.OptionDoVideoEffects.getValue();
   }

   public void setOptionDoVideoEffects(boolean var1) {
      this.OptionDoVideoEffects.setValue(var1);
   }

   public boolean getOptionDoWindSpriteEffects() {
      return this.OptionDoWindSpriteEffects.getValue();
   }

   public void setOptionDoWindSpriteEffects(boolean var1) {
      this.OptionDoWindSpriteEffects.setValue(var1);
   }

   public boolean getOptionDoDoorSpriteEffects() {
      return this.OptionDoDoorSpriteEffects.getValue();
   }

   public void setOptionDoDoorSpriteEffects(boolean var1) {
      this.OptionDoDoorSpriteEffects.setValue(var1);
   }

   public boolean getOptionDoContainerOutline() {
      return this.OptionDoContainerOutline.getValue();
   }

   public void setOptionDoContainerOutline(boolean var1) {
      this.OptionDoContainerOutline.setValue(var1);
   }

   public void setOptionUpdateSneakButton(boolean var1) {
      this.OptionUpdateSneakButton.setValue(var1);
   }

   public boolean getOptionUpdateSneakButton() {
      return this.OptionUpdateSneakButton.getValue();
   }

   public boolean isShowFirstTimeSneakTutorial() {
      return this.OptionShowFirstTimeSneakTutorial.getValue();
   }

   public void setShowFirstTimeSneakTutorial(boolean var1) {
      this.OptionShowFirstTimeSneakTutorial.setValue(var1);
   }

   public boolean isShowFirstTimeSearchTutorial() {
      return this.OptionShowFirstTimeSearchTutorial.getValue();
   }

   public void setShowFirstTimeSearchTutorial(boolean var1) {
      this.OptionShowFirstTimeSearchTutorial.setValue(var1);
   }

   public int getTermsOfServiceVersion() {
      return this.OptionTermsOfServiceVersion.getValue();
   }

   public void setTermsOfServiceVersion(int var1) {
      this.OptionTermsOfServiceVersion.setValue(var1);
   }

   public void setOptiondblTapJogToSprint(boolean var1) {
      this.OptiondblTapJogToSprint.setValue(var1);
   }

   public boolean isOptiondblTapJogToSprint() {
      return this.OptiondblTapJogToSprint.getValue();
   }

   public boolean isToggleToSprint() {
      return this.toggleToSprint.getValue();
   }

   public void setToggleToSprint(boolean var1) {
      this.toggleToSprint.setValue(var1);
   }

   public int getIsoCursorVisibility() {
      return this.isoCursorVisibility.getValue();
   }

   public void setIsoCursorVisibility(int var1) {
      this.isoCursorVisibility.setValue(var1);
   }

   public boolean getOptionShowCursorWhileAiming() {
      return this.OptionShowCursorWhileAiming.getValue();
   }

   public void setOptionShowCursorWhileAiming(boolean var1) {
      this.OptionShowCursorWhileAiming.setValue(var1);
   }

   public boolean gotNewBelt() {
      return this.OptionGotNewBelt.getValue();
   }

   public void setGotNewBelt(boolean var1) {
      this.OptionGotNewBelt.setValue(var1);
   }

   public void setAnimPopupDone(boolean var1) {
      this.bAnimPopupDone = var1;
   }

   public boolean isAnimPopupDone() {
      return this.bAnimPopupDone;
   }

   public void setModsPopupDone(boolean var1) {
      this.bModsPopupDone = var1;
   }

   public boolean isModsPopupDone() {
      return this.bModsPopupDone;
   }

   public boolean isRenderPrecipIndoors() {
      return this.OptionRenderPrecipIndoors.getValue();
   }

   public void setRenderPrecipIndoors(boolean var1) {
      this.OptionRenderPrecipIndoors.setValue(var1);
   }

   public float getOptionPrecipitationSpeedMultiplier() {
      return (float)this.OptionPrecipitationSpeedMultiplier.getValue();
   }

   public void setOptionPrecipitationSpeedMultiplier(float var1) {
      this.OptionPrecipitationSpeedMultiplier.setValue((double)var1);
   }

   public boolean isCollideZombies() {
      return this.collideZombies;
   }

   public void setCollideZombies(boolean var1) {
      this.collideZombies = var1;
   }

   public boolean isFlashIsoCursor() {
      return this.flashIsoCursor;
   }

   public void setFlashIsoCursor(boolean var1) {
      this.flashIsoCursor = var1;
   }

   public boolean isOptionProgressBar() {
      return true;
   }

   public void setOptionProgressBar(boolean var1) {
      this.OptionProgressBar.setValue(var1);
   }

   public void setOptionLanguageName(String var1) {
      this.OptionLanguageName.setValue(var1);
   }

   public String getOptionLanguageName() {
      return this.OptionLanguageName.getValue();
   }

   public int getOptionRenderPrecipitation() {
      return this.OptionRenderPrecipitation.getValue();
   }

   public void setOptionRenderPrecipitation(int var1) {
      this.OptionRenderPrecipitation.setValue(var1);
   }

   public void setOptionAutoProneAtk(boolean var1) {
      this.OptionAutoProneAtk.setValue(var1);
   }

   public boolean isOptionAutoProneAtk() {
      return this.OptionAutoProneAtk.getValue();
   }

   public void setOption3DGroundItem(boolean var1) {
      this.Option3DGroundItem.setValue(var1);
   }

   public boolean isOption3DGroundItem() {
      return this.Option3DGroundItem.getValue();
   }

   public Object getOptionOnStartup(String var1) {
      return optionsOnStartup.get(var1);
   }

   public void setOptionOnStartup(String var1, Object var2) {
      optionsOnStartup.put(var1, var2);
   }

   public void countMissing3DItems() {
      ArrayList var1 = ScriptManager.instance.getAllItems();
      int var2 = 0;
      Iterator var3 = var1.iterator();

      while(var3.hasNext()) {
         Item var4 = (Item)var3.next();
         if (var4.type != Item.Type.Weapon && var4.type != Item.Type.Moveable && !var4.name.contains("ZedDmg") && !var4.name.contains("Wound") && !var4.name.contains("MakeUp") && !var4.name.contains("Bandage") && !var4.name.contains("Hat") && !var4.getObsolete() && StringUtils.isNullOrEmpty(var4.worldObjectSprite) && StringUtils.isNullOrEmpty(var4.worldStaticModel)) {
            System.out.println("Missing: " + var4.name);
            ++var2;
         }
      }

      System.out.println("total missing: " + var2 + "/" + var1.size());
   }

   public boolean getOptionShowItemModInfo() {
      return this.OptionShowItemModInfo.getValue();
   }

   public void setOptionShowItemModInfo(boolean var1) {
      this.OptionShowItemModInfo.setValue(var1);
   }

   public boolean getOptionShowSurvivalGuide() {
      return this.OptionShowSurvivalGuide.getValue();
   }

   public void setOptionShowSurvivalGuide(boolean var1) {
      this.OptionShowSurvivalGuide.setValue(var1);
   }

   public boolean getOptionShowFirstAnimalZoneInfo() {
      return this.OptionShowFirstAnimalZoneInfo.getValue();
   }

   public void setOptionShowFirstAnimalZoneInfo(boolean var1) {
      this.OptionShowFirstAnimalZoneInfo.setValue(var1);
   }

   public boolean getOptionEnableLeftJoystickRadialMenu() {
      return this.OptionEnableLeftJoystickRadialMenu.getValue();
   }

   public void setOptionEnableLeftJoystickRadialMenu(boolean var1) {
      this.OptionEnableLeftJoystickRadialMenu.setValue(var1);
   }

   public String getVersionNumber() {
      return gameVersion.toString();
   }

   public void setAnimalCheat(boolean var1) {
      this.animalCheat = var1;
   }

   public void setDisplayPlayerModel(boolean var1) {
      this.displayPlayerModel = var1;
   }

   public boolean isDisplayPlayerModel() {
      return this.displayPlayerModel;
   }

   public void setDisplayCursor(boolean var1) {
      this.displayCursor = var1;
   }

   public boolean isDisplayCursor() {
      return this.displayCursor;
   }

   public boolean getOptionShowAimTexture() {
      return this.OptionShowAimTexture.getValue();
   }

   public void setOptionShowAimTexture(boolean var1) {
      this.OptionShowAimTexture.setValue(var1);
   }

   public boolean getOptionShowReticleTexture() {
      return this.OptionShowReticleTexture.getValue();
   }

   public void setOptionShowReticleTexture(boolean var1) {
      this.OptionShowReticleTexture.setValue(var1);
   }

   public boolean getOptionShowValidTargetReticleTexture() {
      return this.OptionShowValidTargetReticleTexture.getValue();
   }

   public void setOptionShowValidTargetReticleTexture(boolean var1) {
      this.OptionShowValidTargetReticleTexture.setValue(var1);
   }

   public int getOptionReticleMode() {
      return this.OptionReticleMode.getValue();
   }

   public void setOptionReticleMode(int var1) {
      this.OptionReticleMode.setValue(var1);
   }

   public void setOptionAimTextureIndex(int var1) {
      this.OptionAimTextureIndex.setValue(var1);
   }

   public int getOptionAimTextureIndex() {
      return this.OptionAimTextureIndex.getValue();
   }

   public void setOptionReticleTextureIndex(int var1) {
      this.OptionReticleTextureIndex.setValue(var1);
   }

   public int getOptionReticleTextureIndex() {
      return this.OptionReticleTextureIndex.getValue();
   }

   public void setOptionValidTargetReticleTextureIndex(int var1) {
      this.OptionValidTargetReticleTextureIndex.setValue(var1);
   }

   public int getOptionValidTargetReticleTextureIndex() {
      return this.OptionValidTargetReticleTextureIndex.getValue();
   }

   public void setOptionCrosshairTextureIndex(int var1) {
      this.OptionCrosshairTextureIndex.setValue(var1);
   }

   public int getOptionCrosshairTextureIndex() {
      return this.OptionCrosshairTextureIndex.getValue();
   }

   public ColorInfo getTargetColor() {
      return this.OptionTargetColor.getValueAsColorInfo(this.targetColor);
   }

   public void setTargetColor(ColorInfo var1) {
      this.OptionTargetColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
      this.targetColor.set(var1);
   }

   public ColorInfo getNoTargetColor() {
      return this.OptionNoTargetColor.getValueAsColorInfo(this.noTargetColor);
   }

   public void setNoTargetColor(ColorInfo var1) {
      this.OptionNoTargetColor.setValueVarArgs((double)var1.r, (double)var1.g, (double)var1.b);
      this.noTargetColor.set(var1);
   }

   public int getOptionMaxCrosshairOffset() {
      return this.OptionMaxCrosshairOffset.getValue();
   }

   public void setOptionMaxCrosshairOffset(int var1) {
      this.OptionMaxCrosshairOffset.setValue(var1);
   }

   public boolean getOptionReticleCameraZoom() {
      return this.OptionReticleCameraZoom.getValue();
   }

   public void setOptionReticleCameraZoom(boolean var1) {
      this.OptionReticleCameraZoom.setValue(var1);
   }

   public float getIsoCursorAlpha() {
      float var1 = 0.05F;
      switch (this.isoCursorVisibility.getValue()) {
         case 0:
            var1 = 0.0F;
            break;
         case 1:
            var1 = 0.05F;
            break;
         case 2:
            var1 = 0.1F;
            break;
         case 3:
            var1 = 0.15F;
            break;
         case 4:
            var1 = 0.3F;
            break;
         case 5:
            var1 = 0.5F;
            break;
         case 6:
            var1 = 0.75F;
      }

      return var1;
   }

   public String debugOutputMissingItemSpawn() throws Exception {
      return this.debugOutputMissingSpawn("../workdir/media/scripts/items/", "item");
   }

   public String debugOutputMissingCLothingSpawn() throws Exception {
      return this.debugOutputMissingSpawn("../workdir/media/scripts/clothing/", "clothing");
   }

   public String debugOutputMissingSpawn(String var1, String var2) throws Exception {
      StringBuilder var3 = new StringBuilder(var2 + " items that don't spawn:\r\n");
      File var4 = new File(var1);
      if (!var4.isDirectory()) {
         var3.append("Couldn't find " + var2 + " dir.");
         return var3.toString();
      } else if (((File[])Objects.requireNonNull(var4.listFiles())).length == 0) {
         var3.append("No " + var2 + " script found.");
         return var3.toString();
      } else {
         ArrayList var5 = getClothingStrings(var4);
         var3.append("Found ").append(var5.size()).append(" " + var2 + "s ").append("\r\n");
         File var6 = new File("../workdir/media/lua/server/Items/");
         if (!var6.isDirectory()) {
            var3.append("Couldn't find Items dir.");
            return var3.toString();
         } else if (((File[])Objects.requireNonNull(var6.listFiles())).length == 0) {
            var3.append("No spawn script found.");
            return var3.toString();
         } else {
            ArrayList var7 = getClothingSpawnString(var6);
            ArrayList var8 = new ArrayList();

            for(int var9 = 0; var9 < var5.size(); ++var9) {
               String var10 = (String)var5.get(var9);
               if (!var7.contains(var10)) {
                  var8.add(var10);
               }
            }

            File var13 = new File("../workdir/media/lua/server/Vehicles");
            if (!var13.isDirectory()) {
               var3.append("Couldn't find vehicle distribution dir.");
               return var3.toString();
            } else if (((File[])Objects.requireNonNull(var13.listFiles())).length == 0) {
               var3.append("No vehicle distribution script found.");
               return var3.toString();
            } else {
               ArrayList var14 = getClothingSpawnString(var13);

               for(int var11 = 0; var11 < var8.size(); ++var11) {
                  String var12 = (String)var8.get(var11);
                  if (var14.contains(var12)) {
                     var3.append(var12).append(" only spawn in vehicle ").append("\r\n");
                  } else {
                     var3.append(var12).append(" dont spawn ").append("\r\n");
                  }
               }

               System.out.println(var3);
               File var15 = new File(var2 + "ItemsNotSpawning.txt");
               var15.createNewFile();
               FileWriter var16 = new FileWriter(var15);
               var16.write(var3.toString());
               var16.flush();
               var16.close();
               return var3.toString();
            }
         }
      }
   }

   private static ArrayList<String> getClothingSpawnString(File var0) throws IOException {
      ArrayList var1 = new ArrayList();
      int var2 = 500;
      File[] var3 = (File[])Objects.requireNonNull(var0.listFiles());
      int var4 = var3.length;

      label40:
      for(int var5 = 0; var5 < var4; ++var5) {
         File var6 = var3[var5];
         boolean var7 = false;
         BufferedReader var8 = new BufferedReader(new FileReader(var6));

         while(true) {
            String var9;
            do {
               if ((var9 = var8.readLine()) == null) {
                  continue label40;
               }

               if (!var7) {
                  --var2;
                  if (var2 == 0) {
                     continue label40;
                  }
               }
            } while(!var9.trim().startsWith("items") && !var7 && !var6.getName().contains("Junk"));

            var7 = true;
            var2 = 500;
            if (var9.trim().startsWith("\"") && var9.trim().contains(",")) {
               var1.add(var9.split("\"")[1]);
            }
         }
      }

      return var1;
   }

   private static ArrayList<String> getClothingStrings(File var0) throws IOException {
      ArrayList var1 = new ArrayList();
      File[] var2 = (File[])Objects.requireNonNull(var0.listFiles());
      int var3 = var2.length;

      label198:
      for(int var4 = 0; var4 < var3; ++var4) {
         File var5 = var2[var4];
         BufferedReader var6 = new BufferedReader(new FileReader(var5));

         while(true) {
            String var8;
            do {
               do {
                  do {
                     do {
                        do {
                           do {
                              do {
                                 do {
                                    do {
                                       do {
                                          do {
                                             do {
                                                do {
                                                   do {
                                                      do {
                                                         do {
                                                            do {
                                                               do {
                                                                  do {
                                                                     do {
                                                                        do {
                                                                           do {
                                                                              do {
                                                                                 do {
                                                                                    do {
                                                                                       do {
                                                                                          do {
                                                                                             do {
                                                                                                do {
                                                                                                   do {
                                                                                                      do {
                                                                                                         do {
                                                                                                            String var7;
                                                                                                            do {
                                                                                                               if ((var7 = var6.readLine()) == null) {
                                                                                                                  continue label198;
                                                                                                               }
                                                                                                            } while(!var7.trim().startsWith("item"));

                                                                                                            var8 = var7.trim().split("item ")[1];
                                                                                                         } while(var8.startsWith("Wound"));
                                                                                                      } while(var8.startsWith("Bandage"));
                                                                                                   } while(var8.startsWith("ZedDmg"));
                                                                                                } while(var8.startsWith("MakeUp"));
                                                                                             } while(var8.startsWith("Ring_Right"));
                                                                                          } while(var8.startsWith("Ring_Left_MiddleFinger_"));
                                                                                       } while(var8.startsWith("Animal_"));
                                                                                    } while(var8.startsWith("WristWatch_Right_"));
                                                                                 } while(var8.startsWith("Bracelet_Right"));
                                                                              } while(var8.startsWith("Berry"));
                                                                           } while(var8.startsWith("MushroomGeneric"));
                                                                        } while(var8.startsWith("Umbrella"));
                                                                     } while(var8.endsWith("DOWN"));
                                                                  } while(var8.endsWith("Reverse"));
                                                               } while(var8.endsWith("Back"));
                                                            } while(var8.endsWith("Right"));
                                                         } while(var8.endsWith("_R"));
                                                      } while(var8.endsWith("_nofilter"));
                                                   } while(var8.endsWith("_Stubble"));
                                                } while(var8.endsWith("Open"));
                                             } while(var8.endsWith("Wet"));
                                          } while(var8.endsWith("Lit"));
                                       } while(var8.endsWith("Set"));
                                    } while(var8.endsWith("BulletsMold"));
                                 } while(var8.endsWith("ShellsMold"));
                              } while(var8.contains("_R_"));
                           } while(var8.contains("Debug"));
                        } while(var8.contains("DEBUG"));
                     } while(var8.contains("Dev"));
                  } while(var8.contains("GloveBox"));
               } while(var8.contains("Trunk"));
            } while((var8.contains("Tent") || var8.contains("SleepingBag")) && !var8.contains("Packed"));

            var1.add(var8);
         }
      }

      return var1;
   }

   public String getSelectedMap() {
      return this.selectedMap;
   }

   public void setSelectedMap(String var1) {
      this.selectedMap = var1;
   }
}
