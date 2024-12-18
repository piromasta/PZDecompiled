package zombie.Lua;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import fmod.fmod.EmitterType;
import fmod.fmod.FMODAudio;
import fmod.fmod.FMODDebugEventPlayer;
import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundBank;
import fmod.fmod.FMODSoundEmitter;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Desktop.Action;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.luaj.kahluafork.compiler.FuncState;
import org.lwjglx.input.Controller;
import org.lwjglx.input.Controllers;
import org.lwjglx.input.KeyCodes;
import org.lwjglx.input.Keyboard;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.integration.LuaSuccess;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.integration.expose.LuaJavaClassExposer;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.Coroutine;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaThread;
import se.krka.kahlua.vm.KahluaUtil;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import zombie.AmbientStreamManager;
import zombie.BaseAmbientStreamManager;
import zombie.BaseSoundManager;
import zombie.DummySoundManager;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MapGroups;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.ZomboidFileSystem;
import zombie.ai.GameCharacterAIBrain;
import zombie.ai.MapKnowledge;
import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.ai.states.AttackState;
import zombie.ai.states.BurntToDeath;
import zombie.ai.states.ClimbDownSheetRopeState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CloseWindowState;
import zombie.ai.states.CrawlingZombieTurnState;
import zombie.ai.states.FakeDeadAttackState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.FitnessState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeState;
import zombie.ai.states.OpenWindowState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerActionsState;
import zombie.ai.states.PlayerAimState;
import zombie.ai.states.PlayerEmoteState;
import zombie.ai.states.PlayerExtState;
import zombie.ai.states.PlayerFallDownState;
import zombie.ai.states.PlayerFallingState;
import zombie.ai.states.PlayerGetUpState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.PlayerKnockedDown;
import zombie.ai.states.PlayerOnGroundState;
import zombie.ai.states.PlayerSitOnFurnitureState;
import zombie.ai.states.PlayerSitOnGroundState;
import zombie.ai.states.PlayerStrafeState;
import zombie.ai.states.SmashWindowState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieGetDownState;
import zombie.ai.states.ZombieGetUpState;
import zombie.ai.states.ZombieIdleState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.ZombieReanimateState;
import zombie.ai.states.ZombieSittingState;
import zombie.asset.Asset;
import zombie.asset.AssetPath;
import zombie.audio.BaseSoundBank;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundBank;
import zombie.audio.DummySoundEmitter;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.audio.MusicIntensityConfig;
import zombie.audio.MusicIntensityEvent;
import zombie.audio.MusicIntensityEvents;
import zombie.audio.MusicThreatConfig;
import zombie.audio.MusicThreatStatus;
import zombie.audio.MusicThreatStatuses;
import zombie.audio.parameters.ParameterRoomType;
import zombie.basements.Basements;
import zombie.basements.BasementsV1;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.Capability;
import zombie.characters.CharacterActionAnims;
import zombie.characters.CharacterSoundEmitter;
import zombie.characters.DummyCharacterSoundEmitter;
import zombie.characters.Faction;
import zombie.characters.HairOutfitDefinitions;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoDummyCameraCharacter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.NetworkUser;
import zombie.characters.NetworkUsers;
import zombie.characters.Position3D;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.characters.Safety;
import zombie.characters.SafetySystemManager;
import zombie.characters.Stats;
import zombie.characters.SurvivorDesc;
import zombie.characters.SurvivorFactory;
import zombie.characters.UnderwearDefinition;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocation;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.AttachedItems.AttachedWeaponDefinitions;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Fitness;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.characters.CharacterTimedActions.LuaTimedAction;
import zombie.characters.CharacterTimedActions.LuaTimedActionNew;
import zombie.characters.Moodles.Moodle;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.Moodles.Moodles;
import zombie.characters.WornItems.BodyLocation;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.AnimalChunk;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.AnimalGenomeDefinitions;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalPartsDefinitions;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.VirtualAnimal;
import zombie.characters.animals.behavior.BaseAnimalBehavior;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.animals.datas.AnimalData;
import zombie.characters.professions.ProfessionFactory;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraits;
import zombie.characters.traits.ObservationFactory;
import zombie.characters.traits.TraitCollection;
import zombie.characters.traits.TraitFactory;
import zombie.chat.ChatBase;
import zombie.chat.ChatManager;
import zombie.chat.ChatMessage;
import zombie.chat.ServerChatMessage;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.EnumConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.ActionManager;
import zombie.core.BoxedStaticValues;
import zombie.core.Clipboard;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.FishingAction;
import zombie.core.GameVersion;
import zombie.core.ImmutableColor;
import zombie.core.IndieFileLoader;
import zombie.core.Language;
import zombie.core.NetTimedAction;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.TransactionManager;
import zombie.core.Translator;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.input.Input;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.Bullet;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.physics.WorldSimulation;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAssetManager;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelAssetManager;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecalGroup;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.DefaultClothing;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyle;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.stash.Stash;
import zombie.core.stash.StashBuilding;
import zombie.core.stash.StashSystem;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureID;
import zombie.core.textures.VideoTexture;
import zombie.core.znet.GameServerDetails;
import zombie.core.znet.ISteamWorkshopCallback;
import zombie.core.znet.ServerBrowser;
import zombie.core.znet.SteamFriend;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamRemotePlay;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamUser;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.BooleanDebugOption;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.debug.objects.ObjectDebuggerLua;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityFactory;
import zombie.entity.GameEntityManager;
import zombie.entity.GameEntityType;
import zombie.entity.MetaEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeUtil;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.entity.components.attributes.EnumStringObj;
import zombie.entity.components.build.BuildLogic;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeComponent;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.FluidMatchMode;
import zombie.entity.components.crafting.FurnaceLogic;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.ItemApplyMode;
import zombie.entity.components.crafting.MashingLogic;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.TimeMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.CraftRecipeSort;
import zombie.entity.components.crafting.recipe.HandcraftLogic;
import zombie.entity.components.crafting.recipe.ItemDataList;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidProperties;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.fluids.FluidUtil;
import zombie.entity.components.fluids.PoisonEffect;
import zombie.entity.components.fluids.PoisonInfo;
import zombie.entity.components.fluids.SealedFluidProperties;
import zombie.entity.components.lua.LuaComponent;
import zombie.entity.components.parts.Parts;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFlag;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.signals.Signals;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.components.test.TestComponent;
import zombie.entity.components.ui.UiConfig;
import zombie.entity.debug.EntityDebugTest;
import zombie.entity.debug.EntityDebugTestType;
import zombie.entity.energy.Energy;
import zombie.entity.energy.EnergyType;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.ComponentEventType;
import zombie.entity.events.EntityEvent;
import zombie.entity.events.EntityEventType;
import zombie.entity.meta.MetaTagComponent;
import zombie.entity.util.Array;
import zombie.entity.util.BitSet;
import zombie.entity.util.GameEntityUtil;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.assoc.AssocArray;
import zombie.entity.util.assoc.AssocEnumArray;
import zombie.erosion.ErosionConfig;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.season.ErosionSeason;
import zombie.gameStates.AnimationViewerState;
import zombie.gameStates.AttachmentEditorState;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.ConnectToServerState;
import zombie.gameStates.DebugChunkState;
import zombie.gameStates.DebugGlobalObjectState;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.GameState;
import zombie.gameStates.IngameState;
import zombie.gameStates.LoadingQueueState;
import zombie.gameStates.MainScreenState;
import zombie.gameStates.SeamEditorState;
import zombie.gameStates.SpriteModelEditorState;
import zombie.gameStates.TermsOfServiceState;
import zombie.gameStates.TileGeometryState;
import zombie.globalObjects.CGlobalObject;
import zombie.globalObjects.CGlobalObjectSystem;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.SGlobalObject;
import zombie.globalObjects.SGlobalObjectSystem;
import zombie.globalObjects.SGlobalObjects;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.inventory.FixingManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.ItemType;
import zombie.inventory.RecipeManager;
import zombie.inventory.recipemanager.RecipeMonitor;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.ComboItem;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.KeyRing;
import zombie.inventory.types.Literature;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.Moveable;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponPart;
import zombie.inventory.types.WeaponType;
import zombie.iso.BentFences;
import zombie.iso.BrokenFences;
import zombie.iso.BuildingDef;
import zombie.iso.CellLoader;
import zombie.iso.ContainerOverlays;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoButcherHook;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirectionSet;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoLot;
import zombie.iso.IsoLuaMover;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWaterGeometry;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.MetaObject;
import zombie.iso.MultiStageBuilding;
import zombie.iso.NewMapBinaryFile;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;
import zombie.iso.SearchMode;
import zombie.iso.SliceY;
import zombie.iso.SpriteModel;
import zombie.iso.TileOverlays;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.WorldMarkers;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegionLogType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.IsoRegionsLogger;
import zombie.iso.areas.isoregion.IsoRegionsRenderer;
import zombie.iso.areas.isoregion.data.DataCell;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderAreaHighlights;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderTracerEffects;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStackedWasherDryer;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWheelieBin;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateColorInfo;
import zombie.iso.weather.ClimateForecaster;
import zombie.iso.weather.ClimateHistory;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.ClimateValues;
import zombie.iso.weather.Temperature;
import zombie.iso.weather.ThunderStorm;
import zombie.iso.weather.WeatherPeriod;
import zombie.iso.weather.WorldFlares;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.WGUtils;
import zombie.iso.zones.Trigger;
import zombie.iso.zones.VehicleZone;
import zombie.iso.zones.Zone;
import zombie.modding.ActiveMods;
import zombie.modding.ActiveModsFile;
import zombie.modding.ModUtilsJava;
import zombie.network.ConnectionManager;
import zombie.network.CoopMaster;
import zombie.network.CustomizationManager;
import zombie.network.DBResult;
import zombie.network.DBTicket;
import zombie.network.DesktopBrowser;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistic;
import zombie.network.MPStatistics;
import zombie.network.NetChecksum;
import zombie.network.NetworkAIParams;
import zombie.network.PVPLogTool;
import zombie.network.PZNetKahluaTableImpl;
import zombie.network.PacketTypes;
import zombie.network.Server;
import zombie.network.ServerOptions;
import zombie.network.ServerSettings;
import zombie.network.ServerSettingsManager;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.WarManager;
import zombie.network.anticheats.AntiCheatCapability;
import zombie.network.chat.ChatServer;
import zombie.network.chat.ChatType;
import zombie.network.fields.ContainerID;
import zombie.network.packets.BodyPartSyncPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.ItemTransactionPacket;
import zombie.network.packets.NetTimedActionPacket;
import zombie.network.packets.SyncPlayerStatsPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.network.packets.character.AnimalCommandPacket;
import zombie.network.server.AnimEventEmulator;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PathFindState2;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.ZombiePopulationRenderer;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.profanity.ProfanityFilter;
import zombie.radio.ChannelCategory;
import zombie.radio.RadioAPI;
import zombie.radio.RadioData;
import zombie.radio.ZomboidRadio;
import zombie.radio.StorySounds.DataPoint;
import zombie.radio.StorySounds.EventSound;
import zombie.radio.StorySounds.SLSoundManager;
import zombie.radio.StorySounds.StorySound;
import zombie.radio.StorySounds.StorySoundEvent;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.DevicePresets;
import zombie.radio.devices.PresetEntry;
import zombie.radio.media.MediaData;
import zombie.radio.media.RecordedMedia;
import zombie.radio.scripting.DynamicRadioChannel;
import zombie.radio.scripting.RadioBroadCast;
import zombie.radio.scripting.RadioChannel;
import zombie.radio.scripting.RadioLine;
import zombie.radio.scripting.RadioScript;
import zombie.radio.scripting.RadioScriptManager;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedBuilding.RBBar;
import zombie.randomizedWorld.randomizedBuilding.RBBarn;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RBBurnt;
import zombie.randomizedWorld.randomizedBuilding.RBBurntCorpse;
import zombie.randomizedWorld.randomizedBuilding.RBBurntFireman;
import zombie.randomizedWorld.randomizedBuilding.RBCafe;
import zombie.randomizedWorld.randomizedBuilding.RBClinic;
import zombie.randomizedWorld.randomizedBuilding.RBDorm;
import zombie.randomizedWorld.randomizedBuilding.RBGunstoreSiege;
import zombie.randomizedWorld.randomizedBuilding.RBHairSalon;
import zombie.randomizedWorld.randomizedBuilding.RBHeatBreakAfternoon;
import zombie.randomizedWorld.randomizedBuilding.RBJackieJaye;
import zombie.randomizedWorld.randomizedBuilding.RBJoanHartford;
import zombie.randomizedWorld.randomizedBuilding.RBJudge;
import zombie.randomizedWorld.randomizedBuilding.RBKateAndBaldspot;
import zombie.randomizedWorld.randomizedBuilding.RBLooted;
import zombie.randomizedWorld.randomizedBuilding.RBMayorWestPoint;
import zombie.randomizedWorld.randomizedBuilding.RBNolans;
import zombie.randomizedWorld.randomizedBuilding.RBOffice;
import zombie.randomizedWorld.randomizedBuilding.RBOther;
import zombie.randomizedWorld.randomizedBuilding.RBPileOCrepe;
import zombie.randomizedWorld.randomizedBuilding.RBPizzaWhirled;
import zombie.randomizedWorld.randomizedBuilding.RBPoliceSiege;
import zombie.randomizedWorld.randomizedBuilding.RBReverend;
import zombie.randomizedWorld.randomizedBuilding.RBSafehouse;
import zombie.randomizedWorld.randomizedBuilding.RBSchool;
import zombie.randomizedWorld.randomizedBuilding.RBShopLooted;
import zombie.randomizedWorld.randomizedBuilding.RBSpiffo;
import zombie.randomizedWorld.randomizedBuilding.RBStripclub;
import zombie.randomizedWorld.randomizedBuilding.RBTrashed;
import zombie.randomizedWorld.randomizedBuilding.RBTwiggy;
import zombie.randomizedWorld.randomizedBuilding.RBWoodcraft;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBandPractice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBanditRaid;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBathroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBedroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBleach;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSCorpsePsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDeadDrunk;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDevouredByRats;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSFootballNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGrouchos;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunmanInBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunslinger;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHenDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHockeyPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHouseParty;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPokerNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPoliceAtHouse;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscape;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscapeWithPolice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRPGNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatInfested;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatKing;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatWar;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSResourceGarage;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSkeletonPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSpecificProfession;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStagDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStudentNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSuicidePact;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSTinFoilHat;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombieLockedBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombiesEating;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAmbulanceCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAnimalOnRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAnimalTrailerOnRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSBanditRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSBurntCar;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrashCorpse;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrashDeer;
import zombie.randomizedWorld.randomizedVehicleStory.RVSChangingTire;
import zombie.randomizedWorld.randomizedVehicleStory.RVSConstructionSite;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCrashHorde;
import zombie.randomizedWorld.randomizedVehicleStory.RVSDeadEnd;
import zombie.randomizedWorld.randomizedVehicleStory.RVSFlippedCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSHerdOnRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPlonkies;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPoliceBlockade;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPoliceBlockadeShooting;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRegionalProfessionVehicle;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRichJerk;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRoadKill;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRoadKillSmall;
import zombie.randomizedWorld.randomizedVehicleStory.RVSTrailerCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSUtilityVehicle;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedZoneStory.RZJackieJaye;
import zombie.randomizedWorld.randomizedZoneStory.RZSAttachedAnimal;
import zombie.randomizedWorld.randomizedZoneStory.RZSBBQParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSBaseball;
import zombie.randomizedWorld.randomizedZoneStory.RZSBeachParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSBurntWreck;
import zombie.randomizedWorld.randomizedZoneStory.RZSBuryingCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSCampsite;
import zombie.randomizedWorld.randomizedZoneStory.RZSCharcoalBurner;
import zombie.randomizedWorld.randomizedZoneStory.RZSDean;
import zombie.randomizedWorld.randomizedZoneStory.RZSDuke;
import zombie.randomizedWorld.randomizedZoneStory.RZSEscapedAnimal;
import zombie.randomizedWorld.randomizedZoneStory.RZSEscapedHerd;
import zombie.randomizedWorld.randomizedZoneStory.RZSFishingTrip;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCampEaten;
import zombie.randomizedWorld.randomizedZoneStory.RZSFrankHemingway;
import zombie.randomizedWorld.randomizedZoneStory.RZSHermitCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSHillbillyHoedown;
import zombie.randomizedWorld.randomizedZoneStory.RZSHogWild;
import zombie.randomizedWorld.randomizedZoneStory.RZSHunterCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSKirstyKormick;
import zombie.randomizedWorld.randomizedZoneStory.RZSMurderScene;
import zombie.randomizedWorld.randomizedZoneStory.RZSMusicFest;
import zombie.randomizedWorld.randomizedZoneStory.RZSMusicFestStage;
import zombie.randomizedWorld.randomizedZoneStory.RZSNastyMattress;
import zombie.randomizedWorld.randomizedZoneStory.RZSOccultActivity;
import zombie.randomizedWorld.randomizedZoneStory.RZSOldFirepit;
import zombie.randomizedWorld.randomizedZoneStory.RZSOldShelter;
import zombie.randomizedWorld.randomizedZoneStory.RZSOrphanedFawn;
import zombie.randomizedWorld.randomizedZoneStory.RZSRangerSmith;
import zombie.randomizedWorld.randomizedZoneStory.RZSRockerParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSSadCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSSexyTime;
import zombie.randomizedWorld.randomizedZoneStory.RZSSirTwiggy;
import zombie.randomizedWorld.randomizedZoneStory.RZSSurvivalistCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSTragicPicnic;
import zombie.randomizedWorld.randomizedZoneStory.RZSTrapperCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSVanCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSWasteDump;
import zombie.randomizedWorld.randomizedZoneStory.RZSWaterPump;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDBHelper;
import zombie.savefile.SavefileNaming;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.attributes.AttributesScript;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftLogicScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.FurnaceLogicScript;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.MashingLogicScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.entity.components.lua.LuaComponentScript;
import zombie.scripting.entity.components.parts.PartsScript;
import zombie.scripting.entity.components.signals.SignalsScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.entity.components.test.TestComponentScript;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.EnergyDefinitionScript;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Fixing;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.scripting.objects.GameSoundScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemFilterScript;
import zombie.scripting.objects.ItemRecipe;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.MovableRecipe;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.scripting.objects.SoundTimelineScript;
import zombie.scripting.objects.StringListScript;
import zombie.scripting.objects.TimedActionScript;
import zombie.scripting.objects.UniqueRecipe;
import zombie.scripting.objects.VehiclePartModel;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.VehicleTemplate;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;
import zombie.scripting.ui.TextAlign;
import zombie.scripting.ui.VectorPosAlign;
import zombie.scripting.ui.XuiAutoApply;
import zombie.scripting.ui.XuiLuaStyle;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiReference;
import zombie.scripting.ui.XuiScript;
import zombie.scripting.ui.XuiScriptType;
import zombie.scripting.ui.XuiSkin;
import zombie.scripting.ui.XuiTableScript;
import zombie.scripting.ui.XuiVarType;
import zombie.seams.SeamManager;
import zombie.seating.SeatingManager;
import zombie.spnetwork.SinglePlayerClient;
import zombie.spriteModel.SpriteModelManager;
import zombie.text.templating.ReplaceProviderCharacter;
import zombie.text.templating.TemplateText;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileDepthTextures;
import zombie.tileDepth.TileGeometryManager;
import zombie.tileDepth.TilesetDepthTexture;
import zombie.ui.ActionProgressBar;
import zombie.ui.AtomUI;
import zombie.ui.AtomUIMap;
import zombie.ui.AtomUIText;
import zombie.ui.AtomUITextEntry;
import zombie.ui.AtomUITexture;
import zombie.ui.Clock;
import zombie.ui.ModalDialog;
import zombie.ui.MoodlesUI;
import zombie.ui.NewHealthPanel;
import zombie.ui.ObjectTooltip;
import zombie.ui.RadarPanel;
import zombie.ui.RadialMenu;
import zombie.ui.RadialProgressBar;
import zombie.ui.SpeedControls;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.UI3DModel;
import zombie.ui.UIDebugConsole;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.ui.UITextBox2;
import zombie.ui.UITransition;
import zombie.ui.VehicleGauge;
import zombie.util.AddCoopPlayer;
import zombie.util.PZCalendar;
import zombie.util.PublicServerUtil;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.util.list.PZUnmodifiableList;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.EditVehicleState;
import zombie.vehicles.UI3DScene;
import zombie.vehicles.VehicleDoor;
import zombie.vehicles.VehicleEngineRPM;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehicleWindow;
import zombie.vehicles.VehiclesDB2;
import zombie.world.moddata.ModData;
import zombie.worldMap.UIWorldMap;

public final class LuaManager {
   public static KahluaConverterManager converterManager = new KahluaConverterManager();
   public static J2SEPlatform platform = new J2SEPlatform();
   public static KahluaTable env;
   public static KahluaThread thread;
   public static KahluaThread debugthread;
   public static LuaCaller caller;
   public static LuaCaller debugcaller;
   public static Exposer exposer;
   public static ArrayList<String> loaded;
   private static final HashSet<String> loading;
   public static HashMap<String, Object> loadedReturn;
   public static boolean checksumDone;
   public static ArrayList<String> loadList;
   static ArrayList<String> paths;
   private static final HashMap<String, Object> luaFunctionMap;
   private static final HashMap<String, Object> luaTableMap;
   private static HashMap<String, VideoTexture> videoTextures;
   private static final HashSet<KahluaTable> s_wiping;

   public LuaManager() {
   }

   public static void outputTable(KahluaTable var0, int var1) {
   }

   private static void wipeRecurse(KahluaTable var0) {
      if (!var0.isEmpty()) {
         if (!s_wiping.contains(var0)) {
            s_wiping.add(var0);
            KahluaTableIterator var1 = var0.iterator();

            while(var1.advance()) {
               KahluaTable var2 = (KahluaTable)Type.tryCastTo(var1.getValue(), KahluaTable.class);
               if (var2 != null) {
                  wipeRecurse(var2);
               }
            }

            s_wiping.remove(var0);
            var0.wipe();
         }
      }
   }

   public static void init() {
      loaded.clear();
      loading.clear();
      loadedReturn.clear();
      paths.clear();
      luaFunctionMap.clear();
      luaTableMap.clear();
      platform = new J2SEPlatform();
      if (env != null) {
         s_wiping.clear();
         wipeRecurse(env);
      }

      env = platform.newEnvironment();
      converterManager = new KahluaConverterManager();
      if (thread != null) {
         thread.bReset = true;
      }

      thread = new KahluaThread(platform, env);
      debugthread = new KahluaThread(platform, env);
      thread.debugOwnerThread = Thread.currentThread();
      debugthread.debugOwnerThread = Thread.currentThread();
      UIManager.defaultthread = thread;
      caller = new LuaCaller(converterManager);
      debugcaller = new LuaCaller(converterManager);
      if (exposer != null) {
         exposer.destroy();
      }

      exposer = new Exposer(converterManager, platform, env);
      loaded = new ArrayList();
      checksumDone = false;
      GameClient.checksum = "";
      GameClient.checksumValid = false;
      KahluaNumberConverter.install(converterManager);
      LuaEventManager.register(platform, env);
      LuaHookManager.register(platform, env);
      if (CoopMaster.instance != null) {
         CoopMaster.instance.register(platform, env);
      }

      if (VoiceManager.instance != null) {
         VoiceManager.instance.LuaRegister(platform, env);
      }

      KahluaTable var0 = env;
      exposer.exposeAll();
      exposer.TypeMap.put("function", LuaClosure.class);
      exposer.TypeMap.put("table", KahluaTable.class);
      outputTable(env, 0);
   }

   public static void LoadDir(String var0) throws URISyntaxException {
   }

   public static void LoadDirBase(String var0) throws Exception {
      LoadDirBase(var0, false);
   }

   public static void LoadDirBase(String var0, boolean var1) throws Exception {
      String var2 = "media/lua/" + var0 + "/";
      File var3 = ZomboidFileSystem.instance.getMediaFile("lua" + File.separator + var0);
      if (!paths.contains(var2)) {
         paths.add(var2);
      }

      try {
         searchFolders(ZomboidFileSystem.instance.base.lowercaseURI, var3);
      } catch (IOException var17) {
         ExceptionLogger.logException(var17);
      }

      ArrayList var18 = loadList;
      loadList = new ArrayList();
      ArrayList var19 = ZomboidFileSystem.instance.getModIDs();

      String var6;
      for(int var4 = 0; var4 < var19.size(); ++var4) {
         ChooseGameInfo.Mod var5 = ChooseGameInfo.getAvailableModDetails((String)var19.get(var4));
         if (var5 != null) {
            var6 = var5.getCommonDir();
            File var7;
            URI var8;
            URI var9;
            File var10;
            File var11;
            File var12;
            File var13;
            if (var6 != null) {
               var7 = new File(var6);
               var8 = var7.getCanonicalFile().toURI();
               var9 = (new File(var7.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH))).toURI();
               var10 = ZomboidFileSystem.instance.getCanonicalFile(var7, "media");
               var11 = ZomboidFileSystem.instance.getCanonicalFile(var10, "lua");
               var12 = ZomboidFileSystem.instance.getCanonicalFile(var11, var0);
               var13 = var12;

               try {
                  searchFolders(var9, var13);
               } catch (IOException var16) {
                  ExceptionLogger.logException(var16);
               }
            }

            var6 = var5.getVersionDir();
            if (var6 != null) {
               var7 = new File(var6);
               var8 = var7.getCanonicalFile().toURI();
               var9 = (new File(var7.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH))).toURI();
               var10 = ZomboidFileSystem.instance.getCanonicalFile(var7, "media");
               var11 = ZomboidFileSystem.instance.getCanonicalFile(var10, "lua");
               var12 = ZomboidFileSystem.instance.getCanonicalFile(var11, var0);
               var13 = var12;

               try {
                  searchFolders(var9, var13);
               } catch (IOException var15) {
                  ExceptionLogger.logException(var15);
               }
            }
         }
      }

      Collections.sort(var18);
      var18.addAll(loadList);
      loadList.clear();
      loadList = var18;
      HashSet var20 = new HashSet();
      Iterator var21 = loadList.iterator();

      while(true) {
         do {
            if (!var21.hasNext()) {
               loadList.clear();
               return;
            }

            var6 = (String)var21.next();
         } while(var20.contains(var6));

         var20.add(var6);
         String var22 = ZomboidFileSystem.instance.getAbsolutePath(var6);
         if (var22 == null) {
            throw new IllegalStateException("couldn't find \"" + var6 + "\"");
         }

         if (!var1) {
            RunLua(var22);
         }

         if (!checksumDone && !var6.contains("SandboxVars.lua") && (GameServer.bServer || GameClient.bClient)) {
            NetChecksum.checksummer.addFile(var6, var22);
         }

         if (CoopMaster.instance != null) {
            CoopMaster.instance.update();
         }
      }
   }

   public static void initChecksum() throws Exception {
      if (!checksumDone) {
         if (GameClient.bClient || GameServer.bServer) {
            NetChecksum.checksummer.reset(false);
         }

      }
   }

   public static void finishChecksum() {
      if (GameServer.bServer) {
         GameServer.checksum = NetChecksum.checksummer.checksumToString();
         DebugLog.Lua.println("luaChecksum: " + GameServer.checksum);
      } else {
         if (!GameClient.bClient) {
            return;
         }

         GameClient.checksum = NetChecksum.checksummer.checksumToString();
      }

      NetChecksum.GroupOfFiles.finishChecksum();
      checksumDone = true;
   }

   public static void LoadDirBase() throws Exception {
      initChecksum();
      LoadDirBase("shared");
      LoadDirBase("client");
   }

   public static void searchFolders(URI var0, File var1) throws IOException {
      if (var1.isDirectory()) {
         String[] var2 = var1.list();

         for(int var3 = 0; var3 < var2.length; ++var3) {
            String var10003 = var1.getCanonicalFile().getAbsolutePath();
            searchFolders(var0, new File(var10003 + File.separator + var2[var3]));
         }
      } else if (var1.getAbsolutePath().toLowerCase().endsWith(".lua")) {
         String var4 = ZomboidFileSystem.instance.getRelativeFile(var0, var1.getAbsolutePath());
         var4 = var4.toLowerCase(Locale.ENGLISH);
         loadList.add(var4);
      }

   }

   public static String getLuaCacheDir() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var0 = var10000 + File.separator + "Lua";
      File var1 = new File(var0);
      if (!var1.exists()) {
         var1.mkdir();
      }

      return var0;
   }

   public static String getSandboxCacheDir() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var0 = var10000 + File.separator + "Sandbox Presets";
      File var1 = new File(var0);
      if (!var1.exists()) {
         var1.mkdir();
      }

      return var0;
   }

   public static void fillContainer(ItemContainer var0, IsoPlayer var1) {
      ItemPickerJava.fillContainer(var0, var1);
   }

   public static void updateOverlaySprite(IsoObject var0) {
      ItemPickerJava.updateOverlaySprite(var0);
   }

   public static LuaClosure getDotDelimitedClosure(String var0) {
      String[] var1 = var0.split("\\.");
      KahluaTable var2 = env;

      for(int var3 = 0; var3 < var1.length - 1; ++var3) {
         var2 = (KahluaTable)env.rawget(var1[var3]);
      }

      return (LuaClosure)var2.rawget(var1[var1.length - 1]);
   }

   public static IsoGridSquare AdjacentFreeTileFinder(IsoGridSquare var0, IsoPlayer var1) {
      KahluaTable var2 = (KahluaTable)env.rawget("AdjacentFreeTileFinder");
      LuaClosure var3 = (LuaClosure)var2.rawget("Find");
      return (IsoGridSquare)caller.pcall(thread, var3, new Object[]{var0, var1})[1];
   }

   public static Object RunLua(String var0) {
      return RunLua(var0, false);
   }

   public static Object RunLua(String var0, boolean var1) {
      String var2 = var0.replace("\\", "/");
      if (loading.contains(var2)) {
         DebugLog.Lua.warn("recursive require(): %s", var2);
         return null;
      } else {
         loading.add(var2);

         Object var3;
         try {
            var3 = RunLuaInternal(var0, var1);
         } finally {
            loading.remove(var2);
         }

         return var3;
      }
   }

   private static Object RunLuaInternal(String var0, boolean var1) {
      var0 = var0.replace("\\", "/");
      if (loaded.contains(var0)) {
         return loadedReturn.get(var0);
      } else {
         FuncState.currentFile = var0.substring(var0.lastIndexOf(47) + 1);
         FuncState.currentfullFile = var0;
         String var2 = var0;
         var0 = ZomboidFileSystem.instance.getString(var0.replace("\\", "/"));

         InputStreamReader var3;
         try {
            var3 = IndieFileLoader.getStreamReader(var0);
         } catch (FileNotFoundException var11) {
            ExceptionLogger.logException(var11);
            return null;
         }

         LuaCompiler.rewriteEvents = var1;

         LuaClosure var4;
         try {
            BufferedReader var5 = new BufferedReader(var3);

            try {
               var4 = LuaCompiler.loadis(var5, var0.substring(var0.lastIndexOf(47) + 1), env);
            } catch (Throwable var9) {
               try {
                  var5.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var5.close();
         } catch (Exception var10) {
            Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, "Error found in LUA file: " + var0, (Object)null);
            ExceptionLogger.logException(var10);
            thread.debugException(var10);
            return null;
         }

         luaFunctionMap.clear();
         luaTableMap.clear();
         CraftRecipe.onLuaFileReloaded();
         AttachedWeaponDefinitions.instance.m_dirty = true;
         DefaultClothing.instance.m_dirty = true;
         HairOutfitDefinitions.instance.m_dirty = true;
         UnderwearDefinition.instance.m_dirty = true;
         ZombiesZoneDefinition.bDirty = true;
         LuaReturn var12 = caller.protectedCall(thread, var4, new Object[0]);
         if (!var12.isSuccess()) {
            Logger.getLogger(IsoWorld.class.getName()).log(Level.SEVERE, var12.getErrorString(), (Object)null);
            if (var12.getJavaException() != null) {
               Logger.getLogger(IsoWorld.class.getName()).log(Level.SEVERE, var12.getJavaException().toString(), (Object)null);
            }

            Logger.getLogger(IsoWorld.class.getName()).log(Level.SEVERE, var12.getLuaStackTrace(), (Object)null);
         }

         loaded.add(var2);
         Object var6 = var12.isSuccess() && var12.size() > 0 ? var12.getFirst() : null;
         if (var6 != null) {
            loadedReturn.put(var2, var6);
         } else {
            loadedReturn.remove(var2);
         }

         LuaCompiler.rewriteEvents = false;
         return var6;
      }
   }

   public static Object getFunctionObject(String var0) {
      return getFunctionObject(var0, DebugLog.General);
   }

   public static Object getFunctionObject(String var0, DebugLogStream var1) {
      if (var0 != null && !var0.isEmpty()) {
         Object var2 = luaFunctionMap.get(var0);
         if (var2 != null) {
            return var2;
         } else {
            KahluaTable var3 = env;
            if (var0.contains(".")) {
               String[] var4 = var0.split("\\.");

               for(int var5 = 0; var5 < var4.length - 1; ++var5) {
                  KahluaTable var6 = (KahluaTable)Type.tryCastTo(var3.rawget(var4[var5]), KahluaTable.class);
                  if (var6 == null) {
                     if (var1 != null) {
                        var1.error("no such function \"%s\"", var0);
                     }

                     return null;
                  }

                  var3 = var6;
               }

               var2 = var3.rawget(var4[var4.length - 1]);
            } else {
               var2 = var3.rawget(var0);
            }

            if (!(var2 instanceof JavaFunction) && !(var2 instanceof LuaClosure)) {
               if (var1 != null) {
                  var1.error("no such function \"%s\"", var0);
               }

               return null;
            } else {
               luaFunctionMap.put(var0, var2);
               return var2;
            }
         }
      } else {
         return null;
      }
   }

   public static Object getTableObject(String var0) {
      return getTableObject(var0, DebugLog.General);
   }

   public static Object getTableObject(String var0, DebugLogStream var1) {
      if (var0 != null && !var0.isEmpty()) {
         Object var2 = luaTableMap.get(var0);
         if (var2 != null) {
            return var2;
         } else {
            KahluaTable var3 = env;
            if (var0.contains(".")) {
               String[] var4 = var0.split("\\.");

               for(int var5 = 0; var5 < var4.length - 1; ++var5) {
                  KahluaTable var6 = (KahluaTable)Type.tryCastTo(var3.rawget(var4[var5]), KahluaTable.class);
                  if (var6 == null) {
                     if (var1 != null) {
                        var1.error("no such table \"%s\"", var0);
                     }

                     return null;
                  }

                  var3 = var6;
               }

               var2 = var3.rawget(var4[var4.length - 1]);
            } else {
               var2 = var3.rawget(var0);
            }

            if (var2 instanceof KahluaTable) {
               luaTableMap.put(var0, var2);
               return var2;
            } else {
               if (var1 != null) {
                  var1.error("no such table \"%s\"", var0);
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static void Test() throws IOException {
   }

   public static Object get(Object var0) {
      return env.rawget(var0);
   }

   public static void call(String var0, Object var1) {
      caller.pcall(thread, env.rawget(var0), var1);
   }

   private static void exposeKeyboardKeys(KahluaTable var0) {
      Object var1 = var0.rawget("Keyboard");
      if (var1 instanceof KahluaTable var2) {
         Field[] var3 = Keyboard.class.getFields();

         try {
            Field[] var4 = var3;
            int var5 = var3.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               Field var7 = var4[var6];
               if (Modifier.isStatic(var7.getModifiers()) && Modifier.isPublic(var7.getModifiers()) && Modifier.isFinal(var7.getModifiers()) && var7.getType().equals(Integer.TYPE) && var7.getName().startsWith("KEY_") && !var7.getName().endsWith("WIN")) {
                  var2.rawset(var7.getName(), (double)var7.getInt((Object)null));
               }
            }
         } catch (Exception var8) {
         }

      }
   }

   private static void exposeMouseButtons(KahluaTable var0) {
      Object var1 = var0.rawget("Mouse");
      if (var1 instanceof KahluaTable var2) {
         Field[] var3 = Mouse.class.getFields();

         try {
            Field[] var4 = var3;
            int var5 = var3.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               Field var7 = var4[var6];
               if ((var7.getName().startsWith("BTN_") || var7.getName().equals("LMB") || var7.getName().equals("RMB") || var7.getName().equals("MMB")) && Modifier.isStatic(var7.getModifiers()) && Modifier.isPublic(var7.getModifiers()) && Modifier.isFinal(var7.getModifiers()) && var7.getType().equals(Integer.TYPE)) {
                  var2.rawset(var7.getName(), (double)var7.getInt((Object)null));
               }
            }
         } catch (Exception var8) {
         }

      }
   }

   private static void exposeLuaCalendar() {
      KahluaTable var0 = (KahluaTable)env.rawget("PZCalendar");
      if (var0 != null) {
         Field[] var1 = Calendar.class.getFields();

         try {
            Field[] var2 = var1;
            int var3 = var1.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               Field var5 = var2[var4];
               if (Modifier.isStatic(var5.getModifiers()) && Modifier.isPublic(var5.getModifiers()) && Modifier.isFinal(var5.getModifiers()) && var5.getType().equals(Integer.TYPE)) {
                  var0.rawset(var5.getName(), BoxedStaticValues.toDouble((double)var5.getInt((Object)null)));
               }
            }
         } catch (Exception var6) {
         }

         env.rawset("Calendar", var0);
      }
   }

   public static String getHourMinuteJava() {
      Calendar var10000 = Calendar.getInstance();
      String var0 = "" + var10000.get(12);
      if (Calendar.getInstance().get(12) < 10) {
         var0 = "0" + var0;
      }

      int var1 = Calendar.getInstance().get(11);
      return "" + var1 + ":" + var0;
   }

   public static void releaseAllVideoTextures() {
      Iterator var0 = videoTextures.values().iterator();

      while(var0.hasNext()) {
         VideoTexture var1 = (VideoTexture)var0.next();
         var1.Close();
         Objects.requireNonNull(var1);
         RenderThread.queueInvokeOnRenderContext(var1::destroy);
      }

      videoTextures.clear();
   }

   public static KahluaTable copyTable(KahluaTable var0) {
      return copyTable((KahluaTable)null, var0);
   }

   public static KahluaTable copyTable(KahluaTable var0, KahluaTable var1) {
      if (var0 == null) {
         var0 = platform.newTable();
      } else {
         var0.wipe();
      }

      if (var1 != null && !var1.isEmpty()) {
         KahluaTableIterator var2 = var1.iterator();

         while(var2.advance()) {
            Object var3 = var2.getKey();
            Object var4 = var2.getValue();
            if (var4 instanceof KahluaTable) {
               var0.rawset(var3, copyTable((KahluaTable)null, (KahluaTable)var4));
            } else {
               var0.rawset(var3, var4);
            }
         }

         return var0;
      } else {
         return var0;
      }
   }

   static {
      caller = new LuaCaller(converterManager);
      debugcaller = new LuaCaller(converterManager);
      loaded = new ArrayList();
      loading = new HashSet();
      loadedReturn = new HashMap();
      checksumDone = false;
      loadList = new ArrayList();
      paths = new ArrayList();
      luaFunctionMap = new HashMap();
      luaTableMap = new HashMap();
      videoTextures = new HashMap();
      s_wiping = new HashSet();
   }

   public static final class Exposer extends LuaJavaClassExposer {
      private final HashSet<Class<?>> exposed = new HashSet();

      public Exposer(KahluaConverterManager var1, Platform var2, KahluaTable var3) {
         super(var1, var2, var3);
      }

      public void exposeAll() {
         this.setExposed(BufferedReader.class);
         this.setExposed(BufferedWriter.class);
         this.setExposed(DataInputStream.class);
         this.setExposed(DataOutputStream.class);
         this.setExposed(Double.class);
         this.setExposed(Long.class);
         this.setExposed(Float.class);
         this.setExposed(Integer.class);
         this.setExposed(Math.class);
         this.setExposed(Void.class);
         this.setExposed(SimpleDateFormat.class);
         this.setExposed(ArrayList.class);
         this.setExposed(EnumMap.class);
         this.setExposed(HashMap.class);
         this.setExposed(LinkedHashMap.class);
         this.setExposed(LinkedList.class);
         this.setExposed(Stack.class);
         this.setExposed(Vector.class);
         this.setExposed(Iterator.class);
         this.setExposed(EmitterType.class);
         this.setExposed(FMODAudio.class);
         this.setExposed(FMODDebugEventPlayer.class);
         this.setExposed(FMODSoundBank.class);
         this.setExposed(FMODSoundEmitter.class);
         this.setExposed(FMODDebugEventPlayer.class);
         this.setExposed(Vector2f.class);
         this.setExposed(Vector3f.class);
         this.setExposed(Position3D.class);
         this.setExposed(KahluaUtil.class);
         this.setExposed(DummySoundBank.class);
         this.setExposed(DummySoundEmitter.class);
         this.setExposed(BaseSoundEmitter.class);
         this.setExposed(GameSound.class);
         this.setExposed(GameSoundClip.class);
         this.setExposed(MusicIntensityConfig.class);
         this.setExposed(MusicIntensityEvent.class);
         this.setExposed(MusicIntensityEvents.class);
         this.setExposed(MusicThreatConfig.class);
         this.setExposed(MusicThreatStatus.class);
         this.setExposed(MusicThreatStatuses.class);
         this.setExposed(AttackState.class);
         this.setExposed(BurntToDeath.class);
         this.setExposed(ClimbDownSheetRopeState.class);
         this.setExposed(ClimbOverFenceState.class);
         this.setExposed(ClimbOverWallState.class);
         this.setExposed(ClimbSheetRopeState.class);
         this.setExposed(ClimbThroughWindowState.class);
         this.setExposed(CloseWindowState.class);
         this.setExposed(CrawlingZombieTurnState.class);
         this.setExposed(FakeDeadAttackState.class);
         this.setExposed(FakeDeadZombieState.class);
         this.setExposed(FishingState.class);
         this.setExposed(FitnessState.class);
         this.setExposed(IdleState.class);
         this.setExposed(LungeState.class);
         this.setExposed(OpenWindowState.class);
         this.setExposed(PathFindState.class);
         this.setExposed(PlayerActionsState.class);
         this.setExposed(PlayerAimState.class);
         this.setExposed(PlayerEmoteState.class);
         this.setExposed(PlayerExtState.class);
         this.setExposed(PlayerFallDownState.class);
         this.setExposed(PlayerFallingState.class);
         this.setExposed(PlayerGetUpState.class);
         this.setExposed(PlayerHitReactionPVPState.class);
         this.setExposed(PlayerHitReactionState.class);
         this.setExposed(PlayerKnockedDown.class);
         this.setExposed(PlayerOnGroundState.class);
         this.setExposed(PlayerSitOnFurnitureState.class);
         this.setExposed(PlayerSitOnGroundState.class);
         this.setExposed(PlayerStrafeState.class);
         this.setExposed(SmashWindowState.class);
         this.setExposed(StaggerBackState.class);
         this.setExposed(SwipeStatePlayer.class);
         this.setExposed(ThumpState.class);
         this.setExposed(WalkTowardState.class);
         this.setExposed(ZombieFallDownState.class);
         this.setExposed(ZombieGetDownState.class);
         this.setExposed(ZombieGetUpState.class);
         this.setExposed(ZombieIdleState.class);
         this.setExposed(ZombieOnGroundState.class);
         this.setExposed(ZombieReanimateState.class);
         this.setExposed(ZombieSittingState.class);
         this.setExposed(GameCharacterAIBrain.class);
         this.setExposed(MapKnowledge.class);
         this.setExposed(Basements.class);
         this.setExposed(BasementsV1.class);
         this.setExposed(BodyPartType.class);
         this.setExposed(BodyPart.class);
         this.setExposed(BodyDamage.class);
         this.setExposed(Thermoregulator.class);
         this.setExposed(Thermoregulator.ThermalNode.class);
         this.setExposed(Metabolics.class);
         this.setExposed(Fitness.class);
         this.setExposed(GameKeyboard.class);
         this.setExposed(LuaTimedAction.class);
         this.setExposed(LuaTimedActionNew.class);
         this.setExposed(Moodle.class);
         this.setExposed(Moodles.class);
         this.setExposed(MoodleType.class);
         this.setExposed(ProfessionFactory.class);
         this.setExposed(ProfessionFactory.Profession.class);
         this.setExposed(PerkFactory.class);
         this.setExposed(PerkFactory.Perk.class);
         this.setExposed(PerkFactory.Perks.class);
         this.setExposed(ObservationFactory.class);
         this.setExposed(ObservationFactory.Observation.class);
         this.setExposed(TraitFactory.class);
         this.setExposed(TraitFactory.Trait.class);
         this.setExposed(IsoDummyCameraCharacter.class);
         this.setExposed(Stats.class);
         this.setExposed(SurvivorDesc.class);
         this.setExposed(SurvivorFactory.class);
         this.setExposed(SurvivorFactory.SurvivorType.class);
         this.setExposed(IsoGameCharacter.class);
         this.setExposed(AnimalPartsDefinitions.class);
         this.setExposed(IsoAnimal.class);
         this.setExposed(AnimalData.class);
         this.setExposed(AnimalBreed.class);
         this.setExposed(BaseAnimalBehavior.class);
         this.setExposed(AnimalAllele.class);
         this.setExposed(AnimalGene.class);
         this.setExposed(AnimalGenomeDefinitions.class);
         this.setExposed(AnimalDefinitions.class);
         this.setExposed(IsoGameCharacter.Location.class);
         this.setExposed(IsoGameCharacter.PerkInfo.class);
         this.setExposed(IsoGameCharacter.XP.class);
         this.setExposed(CharacterTraits.class);
         this.setExposed(TraitCollection.TraitSlot.class);
         this.setExposed(TraitCollection.class);
         this.setExposed(IsoPlayer.class);
         this.setExposed(IsoSurvivor.class);
         this.setExposed(IsoZombie.class);
         this.setExposed(CharacterActionAnims.class);
         this.setExposed(HaloTextHelper.class);
         this.setExposed(HaloTextHelper.ColorRGB.class);
         this.setExposed(MoveDeltaModifiers.class);
         this.setExposed(BloodBodyPartType.class);
         this.setExposed(Clipboard.class);
         this.setExposed(AngelCodeFont.class);
         this.setExposed(ZLogger.class);
         this.setExposed(PropertyContainer.class);
         this.setExposed(ClothingItem.class);
         this.setExposed(AnimatorDebugMonitor.class);
         this.setExposed(RuntimeAnimationScript.class);
         this.setExposed(ColorInfo.class);
         this.setExposed(Texture.class);
         this.setExposed(VideoTexture.class);
         this.setExposed(SteamFriend.class);
         this.setExposed(SteamUGCDetails.class);
         this.setExposed(SteamWorkshopItem.class);
         this.setExposed(Color.class);
         this.setExposed(Colors.class);
         this.setExposed(Colors.ColNfo.class);
         this.setExposed(Colors.ColorSet.class);
         this.setExposed(Core.class);
         this.setExposed(GameVersion.class);
         this.setExposed(ImmutableColor.class);
         this.setExposed(Language.class);
         this.setExposed(PerformanceSettings.class);
         this.setExposed(SpriteRenderer.class);
         this.setExposed(Translator.class);
         this.setExposed(PZMath.class);
         this.setExposed(DebugLog.class);
         this.setExposed(DebugOptions.class);
         this.setExposed(BooleanDebugOption.class);
         this.setExposed(DebugType.class);
         this.setExposed(LogSeverity.class);
         this.setExposed(ObjectDebuggerLua.class);
         this.setExposed(Component.class);
         this.setExposed(ComponentType.class);
         this.setExposed(EntityBucket.class);
         this.setExposed(Family.class);
         this.setExposed(GameEntity.class);
         this.setExposed(GameEntityFactory.class);
         this.setExposed(GameEntityType.class);
         this.setExposed(MetaEntity.class);
         this.setExposed(Attribute.class);
         this.setExposed(AttributeContainer.class);
         this.setExposed(AttributeInstance.class);
         this.setExposed(AttributeInstance.Bool.class);
         this.setExposed(AttributeInstance.String.class);
         this.setExposed(AttributeInstance.Numeric.class);
         this.setExposed(AttributeInstance.Float.class);
         this.setExposed(AttributeInstance.Double.class);
         this.setExposed(AttributeInstance.Byte.class);
         this.setExposed(AttributeInstance.Short.class);
         this.setExposed(AttributeInstance.Int.class);
         this.setExposed(AttributeInstance.Long.class);
         this.setExposed(AttributeInstance.Enum.class);
         this.setExposed(AttributeInstance.EnumSet.class);
         this.setExposed(AttributeInstance.EnumStringSet.class);
         this.setExposed(AttributeType.class);
         this.setExposed(AttributeType.Bool.class);
         this.setExposed(AttributeType.String.class);
         this.setExposed(AttributeType.Numeric.class);
         this.setExposed(AttributeType.Float.class);
         this.setExposed(AttributeType.Double.class);
         this.setExposed(AttributeType.Byte.class);
         this.setExposed(AttributeType.Short.class);
         this.setExposed(AttributeType.Int.class);
         this.setExposed(AttributeType.Long.class);
         this.setExposed(AttributeType.Enum.class);
         this.setExposed(AttributeType.EnumSet.class);
         this.setExposed(AttributeType.EnumStringSet.class);
         this.setExposed(AttributeValueType.class);
         this.setExposed(AttributeUtil.class);
         this.setExposed(EnumStringObj.class);
         this.setExposed(CraftRecipeData.class);
         this.setExposed(CraftRecipeData.CacheData.class);
         this.setExposed(CraftRecipeData.InputScriptData.class);
         this.setExposed(CraftRecipeData.OutputScriptData.class);
         this.setExposed(CraftRecipeManager.class);
         this.setExposed(CraftRecipeSort.class);
         this.setExposed(HandcraftLogic.class);
         this.setExposed(HandcraftLogic.CachedRecipeInfo.class);
         this.setExposed(HandcraftLogic.InputItemNode.class);
         this.setExposed(ItemDataList.class);
         this.setExposed(OutputMapper.class);
         this.setExposed(CraftBench.class);
         this.setExposed(CraftLogic.class);
         this.setExposed(FurnaceLogic.class);
         this.setExposed(CraftMode.class);
         this.setExposed(CraftRecipeMonitor.class);
         this.setExposed(CraftUtil.class);
         this.setExposed(FluidMatchMode.class);
         this.setExposed(InputFlag.class);
         this.setExposed(ItemApplyMode.class);
         this.setExposed(MashingLogic.class);
         this.setExposed(OutputFlag.class);
         this.setExposed(StartMode.class);
         this.setExposed(TimeMode.class);
         this.setExposed(BaseCraftingLogic.class);
         this.setExposed(BuildLogic.class);
         this.setExposed(BaseCraftingLogic.CachedRecipeInfo.class);
         this.setExposed(CraftRecipeComponent.class);
         this.setExposed(CraftRecipeComponentScript.class);
         this.setExposed(Fluid.class);
         this.setExposed(FluidType.class);
         this.setExposed(FluidCategory.class);
         this.setExposed(FluidFilter.class);
         this.setExposed(FluidFilter.FilterType.class);
         this.setExposed(FluidConsume.class);
         this.setExposed(FluidContainer.class);
         this.setExposed(FluidProperties.class);
         this.setExposed(FluidSample.class);
         this.setExposed(FluidUtil.class);
         this.setExposed(PoisonEffect.class);
         this.setExposed(PoisonInfo.class);
         this.setExposed(SealedFluidProperties.class);
         this.setExposed(LuaComponent.class);
         this.setExposed(Parts.class);
         this.setExposed(Resource.class);
         this.setExposed(ResourceBlueprint.class);
         this.setExposed(ResourceChannel.class);
         this.setExposed(ResourceEnergy.class);
         this.setExposed(ResourceFlag.class);
         this.setExposed(ResourceFluid.class);
         this.setExposed(ResourceIO.class);
         this.setExposed(ResourceItem.class);
         this.setExposed(Resources.class);
         this.setExposed(ResourceType.class);
         this.setExposed(EntityScriptInfo.class);
         this.setExposed(Signals.class);
         this.setExposed(SpriteConfig.class);
         this.setExposed(SpriteConfigManager.class);
         this.setExposed(SpriteConfigManager.FaceInfo.class);
         this.setExposed(SpriteConfigManager.ObjectInfo.class);
         this.setExposed(SpriteConfigManager.TileInfo.class);
         this.setExposed(TestComponent.class);
         this.setExposed(UiConfig.class);
         this.setExposed(Energy.class);
         this.setExposed(EnergyType.class);
         this.setExposed(ComponentEvent.class);
         this.setExposed(ComponentEventType.class);
         this.setExposed(EntityEvent.class);
         this.setExposed(EntityEventType.class);
         this.setExposed(MetaTagComponent.class);
         this.setExposed(EntityDebugTest.class);
         this.setExposed(EntityDebugTestType.class);
         this.setExposed(AssocArray.class);
         this.setExposed(AssocEnumArray.class);
         this.setExposed(Array.class);
         this.setExposed(BitSet.class);
         this.setExposed(GameEntityUtil.class);
         this.setExposed(ImmutableArray.class);
         this.setExposed(ErosionConfig.class);
         this.setExposed(ErosionConfig.Debug.class);
         this.setExposed(ErosionConfig.Season.class);
         this.setExposed(ErosionConfig.Seeds.class);
         this.setExposed(ErosionConfig.Time.class);
         this.setExposed(ErosionMain.class);
         this.setExposed(ErosionSeason.class);
         this.setExposed(AnimationViewerState.class);
         this.setExposed(AnimationViewerState.BooleanDebugOption.class);
         this.setExposed(AttachmentEditorState.class);
         this.setExposed(ChooseGameInfo.Mod.class);
         this.setExposed(DebugChunkState.class);
         this.setExposed(DebugChunkState.BooleanDebugOption.class);
         this.setExposed(DebugGlobalObjectState.class);
         this.setExposed(GameLoadingState.class);
         this.setExposed(LoadingQueueState.class);
         this.setExposed(MainScreenState.class);
         this.setExposed(SeamEditorState.class);
         this.setExposed(SeamEditorState.BooleanDebugOption.class);
         this.setExposed(SpriteModelEditorState.class);
         this.setExposed(SpriteModelEditorState.BooleanDebugOption.class);
         this.setExposed(TermsOfServiceState.class);
         this.setExposed(TileGeometryState.class);
         this.setExposed(TileGeometryState.BooleanDebugOption.class);
         this.setExposed(CGlobalObject.class);
         this.setExposed(CGlobalObjects.class);
         this.setExposed(CGlobalObjectSystem.class);
         this.setExposed(SGlobalObject.class);
         this.setExposed(SGlobalObjects.class);
         this.setExposed(SGlobalObjectSystem.class);
         this.setExposed(Mouse.class);
         this.setExposed(RecipeMonitor.class);
         this.setExposed(AnimalInventoryItem.class);
         this.setExposed(AlarmClock.class);
         this.setExposed(AlarmClockClothing.class);
         this.setExposed(Clothing.class);
         this.setExposed(Clothing.ClothingPatch.class);
         this.setExposed(Clothing.ClothingPatchFabricType.class);
         this.setExposed(ComboItem.class);
         this.setExposed(Drainable.class);
         this.setExposed(DrainableComboItem.class);
         this.setExposed(Food.class);
         this.setExposed(HandWeapon.class);
         this.setExposed(InventoryContainer.class);
         this.setExposed(Key.class);
         this.setExposed(KeyRing.class);
         this.setExposed(Literature.class);
         this.setExposed(MapItem.class);
         this.setExposed(Moveable.class);
         this.setExposed(Radio.class);
         this.setExposed(WeaponPart.class);
         this.setExposed(ItemContainer.class);
         this.setExposed(ItemPickerJava.class);
         this.setExposed(ItemPickerJava.KeyNamer.class);
         this.setExposed(ItemSpawner.class);
         this.setExposed(InventoryItem.class);
         this.setExposed(FixingManager.class);
         this.setExposed(RecipeManager.class);
         this.setExposed(IsoRegions.class);
         this.setExposed(IsoRegionsLogger.class);
         this.setExposed(IsoRegionsLogger.IsoRegionLog.class);
         this.setExposed(IsoRegionLogType.class);
         this.setExposed(DataCell.class);
         this.setExposed(DataChunk.class);
         this.setExposed(IsoChunkRegion.class);
         this.setExposed(IsoWorldRegion.class);
         this.setExposed(IsoRegionsRenderer.class);
         this.setExposed(IsoRegionsRenderer.BooleanDebugOption.class);
         this.setExposed(IsoBuilding.class);
         this.setExposed(IsoRoom.class);
         this.setExposed(SafeHouse.class);
         this.setExposed(IsoButcherHook.class);
         this.setExposed(FBORenderTracerEffects.class);
         this.setExposed(FBORenderChunk.class);
         this.setExposed(BarricadeAble.class);
         this.setExposed(IsoBarbecue.class);
         this.setExposed(IsoBarricade.class);
         this.setExposed(IsoBrokenGlass.class);
         this.setExposed(IsoClothingDryer.class);
         this.setExposed(IsoClothingWasher.class);
         this.setExposed(IsoCombinationWasherDryer.class);
         this.setExposed(IsoStackedWasherDryer.class);
         this.setExposed(IsoCurtain.class);
         this.setExposed(IsoCarBatteryCharger.class);
         this.setExposed(IsoDeadBody.class);
         this.setExposed(IsoDoor.class);
         this.setExposed(IsoFire.class);
         this.setExposed(IsoFireManager.class);
         this.setExposed(IsoFireplace.class);
         this.setExposed(IsoFeedingTrough.class);
         this.setExposed(IsoHutch.class);
         this.setExposed(IsoHutch.NestBox.class);
         this.setExposed(IsoGenerator.class);
         this.setExposed(IsoJukebox.class);
         this.setExposed(IsoLightSwitch.class);
         this.setExposed(IsoMannequin.class);
         this.setExposed(IsoMolotovCocktail.class);
         this.setExposed(IsoWaveSignal.class);
         this.setExposed(IsoRadio.class);
         this.setExposed(IsoTelevision.class);
         this.setExposed(IsoStackedWasherDryer.class);
         this.setExposed(IsoStove.class);
         this.setExposed(IsoThumpable.class);
         this.setExposed(IsoTrap.class);
         this.setExposed(IsoTree.class);
         this.setExposed(IsoWheelieBin.class);
         this.setExposed(IsoWindow.class);
         this.setExposed(IsoWindowFrame.class);
         this.setExposed(IsoWorldInventoryObject.class);
         this.setExposed(IsoZombieGiblets.class);
         this.setExposed(RainManager.class);
         this.setExposed(ObjectRenderEffects.class);
         this.setExposed(HumanVisual.class);
         this.setExposed(AnimalVisual.class);
         this.setExposed(ItemVisual.class);
         this.setExposed(ItemVisuals.class);
         this.setExposed(IsoSprite.class);
         this.setExposed(IsoSpriteInstance.class);
         this.setExposed(IsoSpriteManager.class);
         this.setExposed(IsoSpriteGrid.class);
         this.setExposed(IsoFlagType.class);
         this.setExposed(IsoObjectType.class);
         this.setExposed(ClimateManager.class);
         this.setExposed(ClimateManager.DayInfo.class);
         this.setExposed(ClimateManager.ClimateFloat.class);
         this.setExposed(ClimateManager.ClimateColor.class);
         this.setExposed(ClimateManager.ClimateBool.class);
         this.setExposed(WeatherPeriod.class);
         this.setExposed(WeatherPeriod.WeatherStage.class);
         this.setExposed(WeatherPeriod.StrLerpVal.class);
         this.setExposed(ClimateManager.AirFront.class);
         this.setExposed(ThunderStorm.class);
         this.setExposed(ThunderStorm.ThunderCloud.class);
         this.setExposed(IsoWeatherFX.class);
         this.setExposed(Temperature.class);
         this.setExposed(ClimateColorInfo.class);
         this.setExposed(ClimateValues.class);
         this.setExposed(ClimateForecaster.class);
         this.setExposed(ClimateForecaster.DayForecast.class);
         this.setExposed(ClimateForecaster.ForecastValue.class);
         this.setExposed(ClimateHistory.class);
         this.setExposed(WorldFlares.class);
         this.setExposed(WorldFlares.Flare.class);
         this.setExposed(ImprovedFog.class);
         this.setExposed(ClimateMoon.class);
         this.setExposed(RagdollSettingsManager.class);
         this.setExposed(RagdollSettingsManager.RagdollSetting.class);
         this.setExposed(RagdollSettingsManager.HitReactionSetting.class);
         this.setExposed(RagdollSettingsManager.ForceHitReactionLocation.class);
         this.setExposed(IsoPuddles.class);
         this.setExposed(IsoPuddles.PuddlesFloat.class);
         this.setExposed(BentFences.class);
         this.setExposed(BrokenFences.class);
         this.setExposed(ContainerOverlays.class);
         this.setExposed(IsoChunk.class);
         this.setExposed(BuildingDef.class);
         this.setExposed(IsoCamera.class);
         this.setExposed(IsoCell.class);
         this.setExposed(IsoChunkMap.class);
         this.setExposed(IsoDirections.class);
         this.setExposed(IsoDirectionSet.class);
         this.setExposed(IsoGridSquare.class);
         this.setExposed(IsoHeatSource.class);
         this.setExposed(IsoLightSource.class);
         this.setExposed(IsoLot.class);
         this.setExposed(IsoLuaMover.class);
         this.setExposed(IsoMetaChunk.class);
         this.setExposed(IsoMetaCell.class);
         this.setExposed(IsoMetaGrid.class);
         this.setExposed(Trigger.class);
         this.setExposed(VehicleZone.class);
         this.setExposed(Zone.class);
         this.setExposed(IsoMovingObject.class);
         this.setExposed(IsoObject.class);
         this.setExposed(IsoObjectPicker.class);
         this.setExposed(IsoPushableObject.class);
         this.setExposed(IsoUtils.class);
         this.setExposed(IsoWorld.class);
         this.setExposed(LosUtil.class);
         this.setExposed(MetaObject.class);
         this.setExposed(RoomDef.class);
         this.setExposed(SpriteModel.class);
         this.setExposed(SliceY.class);
         this.setExposed(TileOverlays.class);
         this.setExposed(Vector2.class);
         this.setExposed(Vector3.class);
         this.setExposed(WorldMarkers.class);
         this.setExposed(WorldMarkers.DirectionArrow.class);
         this.setExposed(WorldMarkers.GridSquareMarker.class);
         this.setExposed(WorldMarkers.PlayerHomingPoint.class);
         this.setExposed(SearchMode.class);
         this.setExposed(SearchMode.PlayerSearchMode.class);
         this.setExposed(SearchMode.SearchModeFloat.class);
         this.setExposed(IsoMarkers.class);
         this.setExposed(IsoMarkers.IsoMarker.class);
         this.setExposed(IsoMarkers.CircleIsoMarker.class);
         this.setExposed(FishSchoolManager.class);
         this.setExposed(WGUtils.class);
         this.setExposed(WGParams.class);
         this.setExposed(LuaEventManager.class);
         this.setExposed(MapObjects.class);
         this.setExposed(ActiveMods.class);
         this.setExposed(PVPLogTool.class);
         this.setExposed(PVPLogTool.PVPEvent.class);
         this.setExposed(NetworkAIParams.class);
         this.setExposed(Server.class);
         this.setExposed(ServerOptions.class);
         this.setExposed(ServerOptions.BooleanServerOption.class);
         this.setExposed(ServerOptions.DoubleServerOption.class);
         this.setExposed(ServerOptions.IntegerServerOption.class);
         this.setExposed(ServerOptions.StringServerOption.class);
         this.setExposed(ServerOptions.TextServerOption.class);
         this.setExposed(ServerOptions.EnumServerOption.class);
         this.setExposed(ServerSettings.class);
         this.setExposed(ServerSettingsManager.class);
         this.setExposed(ContainerID.class);
         this.setExposed(ContainerID.ContainerType.class);
         this.setExposed(WarManager.class);
         this.setExposed(WarManager.War.class);
         this.setExposed(WarManager.State.class);
         this.setExposed(ZombiePopulationRenderer.class);
         this.setExposed(ZombiePopulationRenderer.BooleanDebugOption.class);
         this.setExposed(RadioAPI.class);
         this.setExposed(DeviceData.class);
         this.setExposed(DevicePresets.class);
         this.setExposed(PresetEntry.class);
         this.setExposed(ZomboidRadio.class);
         this.setExposed(RadioData.class);
         this.setExposed(RadioScriptManager.class);
         this.setExposed(DynamicRadioChannel.class);
         this.setExposed(RadioChannel.class);
         this.setExposed(RadioBroadCast.class);
         this.setExposed(RadioLine.class);
         this.setExposed(RadioScript.class);
         this.setExposed(RadioScript.ExitOption.class);
         this.setExposed(ChannelCategory.class);
         this.setExposed(SLSoundManager.class);
         this.setExposed(StorySound.class);
         this.setExposed(StorySoundEvent.class);
         this.setExposed(EventSound.class);
         this.setExposed(DataPoint.class);
         this.setExposed(RecordedMedia.class);
         this.setExposed(MediaData.class);
         this.setExposed(MediaData.MediaLineData.class);
         this.setExposed(GameEntityScript.class);
         this.setExposed(GameEntityTemplate.class);
         this.setExposed(ComponentScript.class);
         this.setExposed(AttributesScript.class);
         this.setExposed(CraftBenchScript.class);
         this.setExposed(CraftLogicScript.class);
         this.setExposed(CraftRecipe.class);
         this.setExposed(CraftRecipe.RequiredSkill.class);
         this.setExposed(InputScript.class);
         this.setExposed(MashingLogicScript.class);
         this.setExposed(FurnaceLogicScript.class);
         this.setExposed(OutputScript.class);
         this.setExposed(FluidContainerScript.class);
         this.setExposed(FluidContainerScript.FluidScript.class);
         this.setExposed(LuaComponentScript.class);
         this.setExposed(PartsScript.class);
         this.setExposed(SignalsScript.class);
         this.setExposed(SpriteConfigScript.class);
         this.setExposed(SpriteConfigScript.FaceScript.class);
         this.setExposed(SpriteConfigScript.TileScript.class);
         this.setExposed(SpriteConfigScript.XRow.class);
         this.setExposed(SpriteConfigScript.ZLayer.class);
         this.setExposed(TestComponentScript.class);
         this.setExposed(UiConfigScript.class);
         this.setExposed(ItemConfig.class);
         this.setExposed(AnimationsMesh.class);
         this.setExposed(BaseScriptObject.class);
         this.setExposed(EnergyDefinitionScript.class);
         this.setExposed(EvolvedRecipe.class);
         this.setExposed(Fixing.class);
         this.setExposed(Fixing.Fixer.class);
         this.setExposed(Fixing.FixerSkill.class);
         this.setExposed(FluidDefinitionScript.class);
         this.setExposed(FluidFilterScript.class);
         this.setExposed(GameSoundScript.class);
         this.setExposed(Item.class);
         this.setExposed(Item.Type.class);
         this.setExposed(ItemRecipe.class);
         this.setExposed(ItemFilterScript.class);
         this.setExposed(MannequinScript.class);
         this.setExposed(ModelAttachment.class);
         this.setExposed(ModelScript.class);
         this.setExposed(MovableRecipe.class);
         this.setExposed(PhysicsShapeScript.class);
         this.setExposed(Recipe.class);
         this.setExposed(Recipe.RequiredSkill.class);
         this.setExposed(Recipe.Result.class);
         this.setExposed(Recipe.Source.class);
         this.setExposed(ScriptModule.class);
         this.setExposed(SoundTimelineScript.class);
         this.setExposed(StringListScript.class);
         this.setExposed(TimedActionScript.class);
         this.setExposed(UniqueRecipe.class);
         this.setExposed(VehiclePartModel.class);
         this.setExposed(VehicleScript.class);
         this.setExposed(VehicleScript.Area.class);
         this.setExposed(VehicleScript.Model.class);
         this.setExposed(VehicleScript.Part.class);
         this.setExposed(VehicleScript.Passenger.class);
         this.setExposed(VehicleScript.PhysicsShape.class);
         this.setExposed(VehicleScript.Position.class);
         this.setExposed(VehicleScript.Wheel.class);
         this.setExposed(VehicleTemplate.class);
         this.setExposed(XuiColorsScript.class);
         this.setExposed(XuiConfigScript.class);
         this.setExposed(XuiLayoutScript.class);
         this.setExposed(XuiSkinScript.class);
         this.setExposed(VectorPosAlign.class);
         this.setExposed(TextAlign.class);
         this.setExposed(XuiAutoApply.class);
         this.setExposed(XuiLuaStyle.class);
         this.setExposed(XuiLuaStyle.XuiVar.class);
         this.setExposed(XuiLuaStyle.XuiBoolean.class);
         this.setExposed(XuiLuaStyle.XuiColor.class);
         this.setExposed(XuiLuaStyle.XuiDouble.class);
         this.setExposed(XuiLuaStyle.XuiFontType.class);
         this.setExposed(XuiLuaStyle.XuiString.class);
         this.setExposed(XuiLuaStyle.XuiStringList.class);
         this.setExposed(XuiLuaStyle.XuiTexture.class);
         this.setExposed(XuiLuaStyle.XuiTranslateString.class);
         this.setExposed(XuiManager.class);
         this.setExposed(XuiReference.class);
         this.setExposed(XuiScript.class);
         this.setExposed(XuiTableScript.class);
         this.setExposed(XuiTableScript.XuiTableColumnScript.class);
         this.setExposed(XuiTableScript.XuiTableRowScript.class);
         this.setExposed(XuiTableScript.XuiTableCellScript.class);
         this.setExposed(XuiScript.XuiVar.class);
         this.setExposed(XuiScript.XuiBoolean.class);
         this.setExposed(XuiScript.XuiColor.class);
         this.setExposed(XuiScript.XuiDouble.class);
         this.setExposed(XuiScript.XuiFloat.class);
         this.setExposed(XuiScript.XuiFontType.class);
         this.setExposed(XuiScript.XuiFunction.class);
         this.setExposed(XuiScript.XuiInteger.class);
         this.setExposed(XuiScript.XuiSpacing.class);
         this.setExposed(XuiScript.XuiString.class);
         this.setExposed(XuiScript.XuiStringList.class);
         this.setExposed(XuiScript.XuiTexture.class);
         this.setExposed(XuiScript.XuiTextAlign.class);
         this.setExposed(XuiScript.XuiTranslateString.class);
         this.setExposed(XuiScript.XuiUnit.class);
         this.setExposed(XuiScript.XuiVector.class);
         this.setExposed(XuiScript.XuiVectorPosAlign.class);
         this.setExposed(XuiScriptType.class);
         this.setExposed(XuiSkin.class);
         this.setExposed(XuiSkin.EntityUiStyle.class);
         this.setExposed(XuiSkin.ComponentUiStyle.class);
         this.setExposed(XuiVarType.class);
         this.setExposed(ScriptManager.class);
         this.setExposed(ScriptType.class);
         this.setExposed(SeamManager.class);
         this.setExposed(SeatingManager.class);
         this.setExposed(SpriteModelManager.class);
         this.setExposed(TemplateText.class);
         this.setExposed(ReplaceProviderCharacter.class);
         this.setExposed(TileDepthTexture.class);
         this.setExposed(TileDepthTextureAssignmentManager.class);
         this.setExposed(TileDepthTextureManager.class);
         this.setExposed(TileDepthTextures.class);
         this.setExposed(TileGeometryManager.class);
         this.setExposed(TilesetDepthTexture.class);
         this.setExposed(ActionProgressBar.class);
         this.setExposed(Clock.class);
         this.setExposed(UIDebugConsole.class);
         this.setExposed(ModalDialog.class);
         this.setExposed(MoodlesUI.class);
         this.setExposed(NewHealthPanel.class);
         this.setExposed(ObjectTooltip.class);
         this.setExposed(ObjectTooltip.Layout.class);
         this.setExposed(ObjectTooltip.LayoutItem.class);
         this.setExposed(RadarPanel.class);
         this.setExposed(RadialMenu.class);
         this.setExposed(RadialProgressBar.class);
         this.setExposed(SpeedControls.class);
         this.setExposed(TextManager.class);
         this.setExposed(UI3DModel.class);
         this.setExposed(UIElement.class);
         this.setExposed(AtomUI.class);
         this.setExposed(AtomUIText.class);
         this.setExposed(AtomUITexture.class);
         this.setExposed(AtomUITextEntry.class);
         this.setExposed(AtomUIMap.class);
         this.setExposed(UIFont.class);
         this.setExposed(UITransition.class);
         this.setExposed(UIManager.class);
         this.setExposed(UITextBox2.class);
         this.setExposed(VehicleGauge.class);
         this.setExposed(TextDrawObject.class);
         this.setExposed(PZArrayList.class);
         this.setExposed(PZUnmodifiableList.class);
         this.setExposed(PZCalendar.class);
         this.setExposed(BaseVehicle.class);
         this.setExposed(EditVehicleState.class);
         this.setExposed(PathFindBehavior2.BehaviorResult.class);
         this.setExposed(PathFindBehavior2.class);
         this.setExposed(PathFindState2.class);
         this.setExposed(UI3DScene.class);
         this.setExposed(VehicleDoor.class);
         this.setExposed(VehicleEngineRPM.class);
         this.setExposed(VehicleLight.class);
         this.setExposed(VehiclePart.class);
         this.setExposed(VehicleType.class);
         this.setExposed(VehicleWindow.class);
         this.setExposed(AttachedItem.class);
         this.setExposed(AttachedItems.class);
         this.setExposed(AttachedLocation.class);
         this.setExposed(AttachedLocationGroup.class);
         this.setExposed(AttachedLocations.class);
         this.setExposed(WornItems.class);
         this.setExposed(WornItem.class);
         this.setExposed(BodyLocation.class);
         this.setExposed(BodyLocationGroup.class);
         this.setExposed(BodyLocations.class);
         this.setExposed(Role.class);
         this.setExposed(Capability.class);
         this.setExposed(NetworkUser.class);
         this.setExposed(DummySoundManager.class);
         this.setExposed(GameSounds.class);
         this.setExposed(GameTime.class);
         this.setExposed(GameWindow.class);
         this.setExposed(SandboxOptions.class);
         this.setExposed(SandboxOptions.BooleanSandboxOption.class);
         this.setExposed(SandboxOptions.DoubleSandboxOption.class);
         this.setExposed(SandboxOptions.StringSandboxOption.class);
         this.setExposed(SandboxOptions.EnumSandboxOption.class);
         this.setExposed(SandboxOptions.IntegerSandboxOption.class);
         this.setExposed(SoundManager.class);
         this.setExposed(SystemDisabler.class);
         this.setExposed(VirtualZombieManager.class);
         this.setExposed(WorldSoundManager.class);
         this.setExposed(WorldSoundManager.WorldSound.class);
         this.setExposed(DummyCharacterSoundEmitter.class);
         this.setExposed(CharacterSoundEmitter.class);
         this.setExposed(SoundManager.AmbientSoundEffect.class);
         this.setExposed(BaseAmbientStreamManager.class);
         this.setExposed(AmbientStreamManager.class);
         this.setExposed(Nutrition.class);
         this.setExposed(MultiStageBuilding.class);
         this.setExposed(MultiStageBuilding.Stage.class);
         this.setExposed(SleepingEvent.class);
         this.setExposed(IsoCompost.class);
         this.setExposed(Userlog.class);
         this.setExposed(Userlog.UserlogType.class);
         this.setExposed(ConfigOption.class);
         this.setExposed(BooleanConfigOption.class);
         this.setExposed(DoubleConfigOption.class);
         this.setExposed(EnumConfigOption.class);
         this.setExposed(IntegerConfigOption.class);
         this.setExposed(StringConfigOption.class);
         this.setExposed(Faction.class);
         this.setExposed(GlobalObject.LuaFileWriter.class);
         this.setExposed(Keyboard.class);
         this.setExposed(DBResult.class);
         this.setExposed(NonPvpZone.class);
         this.setExposed(DesignationZoneAnimal.class);
         this.setExposed(AnimalTracks.class);
         this.setExposed(IsoAnimalTrack.class);
         this.setExposed(AnimalChunk.class);
         this.setExposed(VirtualAnimal.class);
         this.setExposed(DesignationZone.class);
         this.setExposed(DBTicket.class);
         this.setExposed(StashSystem.class);
         this.setExposed(StashBuilding.class);
         this.setExposed(Stash.class);
         this.setExposed(ItemType.class);
         this.setExposed(RandomizedWorldBase.class);
         this.setExposed(RandomizedBuildingBase.class);
         this.setExposed(RBBurntFireman.class);
         this.setExposed(RBBasic.class);
         this.setExposed(RBBurnt.class);
         this.setExposed(RBOther.class);
         this.setExposed(RBStripclub.class);
         this.setExposed(RBSchool.class);
         this.setExposed(RBSpiffo.class);
         this.setExposed(RBPizzaWhirled.class);
         this.setExposed(RBOffice.class);
         this.setExposed(RBHairSalon.class);
         this.setExposed(RBClinic.class);
         this.setExposed(RBPileOCrepe.class);
         this.setExposed(RBCafe.class);
         this.setExposed(RBBar.class);
         this.setExposed(RBLooted.class);
         this.setExposed(RBSafehouse.class);
         this.setExposed(RBBurntCorpse.class);
         this.setExposed(RBShopLooted.class);
         this.setExposed(RBKateAndBaldspot.class);
         this.setExposed(RBGunstoreSiege.class);
         this.setExposed(RBPoliceSiege.class);
         this.setExposed(RBHeatBreakAfternoon.class);
         this.setExposed(RBTrashed.class);
         this.setExposed(RBBarn.class);
         this.setExposed(RBDorm.class);
         this.setExposed(RBNolans.class);
         this.setExposed(RBJackieJaye.class);
         this.setExposed(RBReverend.class);
         this.setExposed(RBTwiggy.class);
         this.setExposed(RBWoodcraft.class);
         this.setExposed(RBJoanHartford.class);
         this.setExposed(RBJudge.class);
         this.setExposed(RBMayorWestPoint.class);
         this.setExposed(RandomizedDeadSurvivorBase.class);
         this.setExposed(RDSZombiesEating.class);
         this.setExposed(RDSBleach.class);
         this.setExposed(RDSDeadDrunk.class);
         this.setExposed(RDSGunmanInBathroom.class);
         this.setExposed(RDSGunslinger.class);
         this.setExposed(RDSZombieLockedBathroom.class);
         this.setExposed(RDSBanditRaid.class);
         this.setExposed(RDSBandPractice.class);
         this.setExposed(RDSBathroomZed.class);
         this.setExposed(RDSBedroomZed.class);
         this.setExposed(RDSFootballNight.class);
         this.setExposed(RDSHenDo.class);
         this.setExposed(RDSStagDo.class);
         this.setExposed(RDSStudentNight.class);
         this.setExposed(RDSPokerNight.class);
         this.setExposed(RDSSuicidePact.class);
         this.setExposed(RDSPrisonEscape.class);
         this.setExposed(RDSPrisonEscapeWithPolice.class);
         this.setExposed(RDSSkeletonPsycho.class);
         this.setExposed(RDSCorpsePsycho.class);
         this.setExposed(RDSSpecificProfession.class);
         this.setExposed(RDSPoliceAtHouse.class);
         this.setExposed(RDSHouseParty.class);
         this.setExposed(RDSTinFoilHat.class);
         this.setExposed(RDSHockeyPsycho.class);
         this.setExposed(RDSDevouredByRats.class);
         this.setExposed(RDSRPGNight.class);
         this.setExposed(RDSRatInfested.class);
         this.setExposed(RDSRatKing.class);
         this.setExposed(RDSRatWar.class);
         this.setExposed(RDSResourceGarage.class);
         this.setExposed(RDSGrouchos.class);
         this.setExposed(RandomizedVehicleStoryBase.class);
         this.setExposed(RVSCarCrash.class);
         this.setExposed(RVSBanditRoad.class);
         this.setExposed(RVSAmbulanceCrash.class);
         this.setExposed(RVSCrashHorde.class);
         this.setExposed(RVSCarCrashCorpse.class);
         this.setExposed(RVSPoliceBlockade.class);
         this.setExposed(RVSPoliceBlockadeShooting.class);
         this.setExposed(RVSBurntCar.class);
         this.setExposed(RVSConstructionSite.class);
         this.setExposed(RVSUtilityVehicle.class);
         this.setExposed(RVSChangingTire.class);
         this.setExposed(RVSFlippedCrash.class);
         this.setExposed(RVSTrailerCrash.class);
         this.setExposed(RVSCarCrashDeer.class);
         this.setExposed(RVSDeadEnd.class);
         this.setExposed(RVSRegionalProfessionVehicle.class);
         this.setExposed(RVSRoadKill.class);
         this.setExposed(RVSRoadKillSmall.class);
         this.setExposed(RVSAnimalOnRoad.class);
         this.setExposed(RVSHerdOnRoad.class);
         this.setExposed(RVSAnimalTrailerOnRoad.class);
         this.setExposed(RVSRichJerk.class);
         this.setExposed(RVSPlonkies.class);
         this.setExposed(RandomizedZoneStoryBase.class);
         this.setExposed(RZSForestCamp.class);
         this.setExposed(RZSForestCampEaten.class);
         this.setExposed(RZSBuryingCamp.class);
         this.setExposed(RZSBeachParty.class);
         this.setExposed(RZSFishingTrip.class);
         this.setExposed(RZSBBQParty.class);
         this.setExposed(RZSHunterCamp.class);
         this.setExposed(RZSSexyTime.class);
         this.setExposed(RZSTrapperCamp.class);
         this.setExposed(RZSBaseball.class);
         this.setExposed(RZSMusicFestStage.class);
         this.setExposed(RZSMusicFest.class);
         this.setExposed(RZSBurntWreck.class);
         this.setExposed(RZSHermitCamp.class);
         this.setExposed(RZSHillbillyHoedown.class);
         this.setExposed(RZSHogWild.class);
         this.setExposed(RZSRockerParty.class);
         this.setExposed(RZSSadCamp.class);
         this.setExposed(RZSSurvivalistCamp.class);
         this.setExposed(RZSVanCamp.class);
         this.setExposed(RZSEscapedAnimal.class);
         this.setExposed(RZSEscapedHerd.class);
         this.setExposed(RZSAttachedAnimal.class);
         this.setExposed(RZSOrphanedFawn.class);
         this.setExposed(RZSNastyMattress.class);
         this.setExposed(RZSWasteDump.class);
         this.setExposed(RZSMurderScene.class);
         this.setExposed(RZSTragicPicnic.class);
         this.setExposed(RZSRangerSmith.class);
         this.setExposed(RZSOccultActivity.class);
         this.setExposed(RZSWaterPump.class);
         this.setExposed(RZSOldFirepit.class);
         this.setExposed(RZSOldShelter.class);
         this.setExposed(RZSCampsite.class);
         this.setExposed(RZSCharcoalBurner.class);
         this.setExposed(RZSDean.class);
         this.setExposed(RZSDuke.class);
         this.setExposed(RZSFrankHemingway.class);
         this.setExposed(RZSKirstyKormick.class);
         this.setExposed(RZSSirTwiggy.class);
         this.setExposed(RZJackieJaye.class);
         this.setExposed(MapGroups.class);
         this.setExposed(BeardStyles.class);
         this.setExposed(BeardStyle.class);
         this.setExposed(HairStyles.class);
         this.setExposed(HairStyle.class);
         this.setExposed(VoiceStyles.class);
         this.setExposed(VoiceStyle.class);
         this.setExposed(BloodClothingType.class);
         this.setExposed(WeaponType.class);
         this.setExposed(IsoWaterGeometry.class);
         this.setExposed(ModData.class);
         this.setExposed(WorldMarkers.class);
         this.setExposed(SyncPlayerStatsPacket.class);
         this.setExposed(BodyPartSyncPacket.class);
         this.setExposed(ChatMessage.class);
         this.setExposed(ChatBase.class);
         this.setExposed(ServerChatMessage.class);
         this.setExposed(Safety.class);
         this.setExposed(NetTimedAction.class);
         this.setExposed(NetTimedActionPacket.class);
         if (Core.bDebug) {
            this.setExposed(Field.class);
            this.setExposed(Method.class);
            this.setExposed(Coroutine.class);
         }

         UIWorldMap.setExposed(this);
         if (Core.bDebug) {
            try {
               this.exposeMethod(Class.class, Class.class.getMethod("getName"), LuaManager.env);
               this.exposeMethod(Class.class, Class.class.getMethod("getSimpleName"), LuaManager.env);
            } catch (NoSuchMethodException var3) {
               var3.printStackTrace();
            }
         }

         Iterator var1 = this.exposed.iterator();

         while(var1.hasNext()) {
            Class var2 = (Class)var1.next();
            this.exposeLikeJavaRecursively(var2, LuaManager.env);
         }

         this.exposeGlobalFunctions(new GlobalObject());
         LuaManager.exposeKeyboardKeys(LuaManager.env);
         LuaManager.exposeMouseButtons(LuaManager.env);
         LuaManager.exposeLuaCalendar();
      }

      public void setExposed(Class<?> var1) {
         this.exposed.add(var1);
      }

      public boolean shouldExpose(Class<?> var1) {
         return var1 == null ? false : this.exposed.contains(var1);
      }
   }

   public static class GlobalObject {
      static FileOutputStream outStream;
      static FileInputStream inStream;
      static FileReader inFileReader = null;
      static BufferedReader inBufferedReader = null;
      static long timeLastRefresh = 0L;
      private static final TimSortComparator timSortComparator = new TimSortComparator();

      public GlobalObject() {
      }

      @LuaMethod(
         name = "loadVehicleModel",
         global = true
      )
      public static Model loadVehicleModel(String var0, String var1, String var2) {
         return loadZomboidModel(var0, var1, var2, "vehicle", true);
      }

      @LuaMethod(
         name = "loadStaticZomboidModel",
         global = true
      )
      public static Model loadStaticZomboidModel(String var0, String var1, String var2) {
         return loadZomboidModel(var0, var1, var2, (String)null, true);
      }

      @LuaMethod(
         name = "loadSkinnedZomboidModel",
         global = true
      )
      public static Model loadSkinnedZomboidModel(String var0, String var1, String var2) {
         return loadZomboidModel(var0, var1, var2, (String)null, false);
      }

      @LuaMethod(
         name = "loadZomboidModel",
         global = true
      )
      public static Model loadZomboidModel(String var0, String var1, String var2, String var3, boolean var4) {
         try {
            if (var1.startsWith("/")) {
               var1 = var1.substring(1);
            }

            if (var2.startsWith("/")) {
               var2 = var2.substring(1);
            }

            if (StringUtils.isNullOrWhitespace(var3)) {
               var3 = "basicEffect";
            }

            if ("vehicle".equals(var3) && !Core.getInstance().getPerfReflectionsOnLoad()) {
               var3 = var3 + "_noreflect";
            }

            Model var5 = ModelManager.instance.tryGetLoadedModel(var1, var2, var4, var3, false);
            if (var5 != null) {
               return var5;
            } else {
               ModelManager.instance.setModelMetaData(var0, var1, var2, var3, var4);
               Model.ModelAssetParams var6 = new Model.ModelAssetParams();
               var6.bStatic = var4;
               var6.meshName = var1;
               var6.shaderName = var3;
               var6.textureName = var2;
               var6.textureFlags = ModelManager.instance.getTextureFlags();
               var5 = (Model)ModelAssetManager.instance.load(new AssetPath(var0), var6);
               if (var5 != null) {
                  ModelManager.instance.putLoadedModel(var1, var2, var4, var3, var5);
               }

               return var5;
            }
         } catch (Exception var7) {
            DebugLog.Lua.error("LuaManager.loadZomboidModel> Exception thrown loading model: " + var0 + " mesh:" + var1 + " tex:" + var2 + " shader:" + var3 + " isStatic:" + var4);
            var7.printStackTrace();
            return null;
         }
      }

      @LuaMethod(
         name = "setModelMetaData",
         global = true
      )
      public static void setModelMetaData(String var0, String var1, String var2, String var3, boolean var4) {
         if (var1.startsWith("/")) {
            var1 = var1.substring(1);
         }

         if (var2.startsWith("/")) {
            var2 = var2.substring(1);
         }

         ModelManager.instance.setModelMetaData(var0, var1, var2, var3, var4);
      }

      @LuaMethod(
         name = "reloadModelsMatching",
         global = true
      )
      public static void reloadModelsMatching(String var0) {
         ModelManager.instance.reloadModelsMatching(var0);
      }

      @LuaMethod(
         name = "getSLSoundManager",
         global = true
      )
      public static SLSoundManager getSLSoundManager() {
         return null;
      }

      @LuaMethod(
         name = "getRadioAPI",
         global = true
      )
      public static RadioAPI getRadioAPI() {
         return RadioAPI.hasInstance() ? RadioAPI.getInstance() : null;
      }

      @LuaMethod(
         name = "getRadioTranslators",
         global = true
      )
      public static ArrayList<String> getRadioTranslators(Language var0) {
         return RadioData.getTranslatorNames(var0);
      }

      @LuaMethod(
         name = "getTranslatorCredits",
         global = true
      )
      public static ArrayList<String> getTranslatorCredits(Language var0) {
         File var1 = new File(ZomboidFileSystem.instance.getString("media/lua/shared/Translate/" + var0.name() + "/credits.txt"));

         try {
            FileReader var2 = new FileReader(var1, Charset.forName(var0.charset()));

            ArrayList var6;
            try {
               BufferedReader var3 = new BufferedReader(var2);

               try {
                  ArrayList var4 = new ArrayList();

                  String var5;
                  while((var5 = var3.readLine()) != null) {
                     if (!StringUtils.isNullOrWhitespace(var5)) {
                        var4.add(var5.trim());
                     }
                  }

                  var6 = var4;
               } catch (Throwable var9) {
                  try {
                     var3.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               var3.close();
            } catch (Throwable var10) {
               try {
                  var2.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }

               throw var10;
            }

            var2.close();
            return var6;
         } catch (FileNotFoundException var11) {
            return null;
         } catch (Exception var12) {
            ExceptionLogger.logException(var12);
            return null;
         }
      }

      @LuaMethod(
         name = "getBehaviourDebugPlayer",
         global = true
      )
      public static IsoGameCharacter getBehaviourDebugPlayer() {
         return null;
      }

      @LuaMethod(
         name = "setBehaviorStep",
         global = true
      )
      public static void setBehaviorStep(boolean var0) {
      }

      @LuaMethod(
         name = "getPuddlesManager",
         global = true
      )
      public static IsoPuddles getPuddlesManager() {
         return IsoPuddles.getInstance();
      }

      @LuaMethod(
         name = "getAllAnimalsDefinitions",
         global = true
      )
      public static ArrayList<AnimalDefinitions> getAllAnimalsDefinitions() {
         return AnimalDefinitions.getAnimalDefsArray();
      }

      @LuaMethod(
         name = "setPuddles",
         global = true
      )
      public static void setPuddles(float var0) {
         IsoPuddles.PuddlesFloat var1 = IsoPuddles.getInstance().getPuddlesFloat(3);
         var1.setEnableAdmin(true);
         var1.setAdminValue(var0);
         var1 = IsoPuddles.getInstance().getPuddlesFloat(1);
         var1.setEnableAdmin(true);
         var1.setAdminValue(PZMath.clamp_01(var0 * 1.2F));
      }

      @LuaMethod(
         name = "fastfloor",
         global = true
      )
      public static float fastfloor(float var0) {
         return (float)PZMath.fastfloor(var0);
      }

      @LuaMethod(
         name = "getZomboidRadio",
         global = true
      )
      public static ZomboidRadio getZomboidRadio() {
         return ZomboidRadio.hasInstance() ? ZomboidRadio.getInstance() : null;
      }

      @LuaMethod(
         name = "getRandomUUID",
         global = true
      )
      public static String getRandomUUID() {
         return ModUtilsJava.getRandomUUID();
      }

      @LuaMethod(
         name = "sendItemListNet",
         global = true
      )
      public static boolean sendItemListNet(IsoPlayer var0, ArrayList<InventoryItem> var1, IsoPlayer var2, String var3, String var4) {
         return ModUtilsJava.sendItemListNet(var0, var1, var2, var3, var4);
      }

      @LuaMethod(
         name = "convertToPZNetTable",
         global = true
      )
      public static KahluaTable convertToPZNetTable(KahluaTable var0) {
         PZNetKahluaTableImpl var1 = new PZNetKahluaTableImpl(new LinkedHashMap());
         KahluaTableIterator var2 = var0.iterator();

         while(var2.advance()) {
            var1.rawset(var2.getKey(), var2.getValue());
         }

         return var1;
      }

      @LuaMethod(
         name = "instanceof",
         global = true
      )
      public static boolean instof(Object var0, String var1) {
         if ("PZKey".equals(var1)) {
            boolean var2 = false;
         }

         if (var0 == null) {
            return false;
         } else if (LuaManager.exposer.TypeMap.containsKey(var1)) {
            Class var3 = (Class)LuaManager.exposer.TypeMap.get(var1);
            return var3.isInstance(var0);
         } else if (var1.equals("LuaClosure") && var0 instanceof LuaClosure) {
            return true;
         } else {
            return var1.equals("KahluaTableImpl") && var0 instanceof KahluaTableImpl;
         }
      }

      @LuaMethod(
         name = "serverConnect",
         global = true
      )
      public static void serverConnect(String var0, String var1, String var2, String var3, String var4, String var5, String var6, boolean var7, boolean var8, int var9, String var10) {
         ConnectionManager.getInstance().serverConnect(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
      }

      @LuaMethod(
         name = "serverConnectCoop",
         global = true
      )
      public static void serverConnectCoop(String var0) {
         ConnectionManager.getInstance().serverConnectCoop(var0);
      }

      @LuaMethod(
         name = "sendPing",
         global = true
      )
      public static void sendPing() {
         if (GameClient.bClient) {
            ByteBufferWriter var0 = GameClient.connection.startPingPacket();
            PacketTypes.doPingPacket(var0);
            var0.putLong(System.currentTimeMillis());
            GameClient.connection.endPingPacket();
         }

      }

      @LuaMethod(
         name = "connectionManagerLog",
         global = true
      )
      public static void connectionManagerLog(String var0, String var1) {
         ConnectionManager.log(var0, var1, GameClient.connection);
      }

      @LuaMethod(
         name = "forceDisconnect",
         global = true
      )
      public static void forceDisconnect() {
         if (GameClient.connection != null) {
            GameClient.connection.forceDisconnect("lua-force-disconnect");
         }

      }

      @LuaMethod(
         name = "checkPermissions",
         global = true
      )
      public static boolean checkPermissions(IsoPlayer var0, Capability var1) {
         if (GameServer.bServer && var0 != null && var1 != null) {
            UdpConnection var2 = GameServer.getConnectionFromPlayer(var0);
            if (var2 != null) {
               return AntiCheatCapability.validate(var2, var1);
            }
         }

         return true;
      }

      @LuaMethod(
         name = "backToSinglePlayer",
         global = true
      )
      public static void backToSinglePlayer() {
         if (GameClient.bClient) {
            GameClient.instance.doDisconnect("going back to single-player");
            GameClient.bClient = false;
            timeLastRefresh = 0L;
         }

      }

      @LuaMethod(
         name = "isIngameState",
         global = true
      )
      public static boolean isIngameState() {
         return GameWindow.states.current == IngameState.instance;
      }

      @LuaMethod(
         name = "requestPacketCounts",
         global = true
      )
      public static void requestPacketCounts() {
         if (GameClient.bClient) {
            GameClient.instance.requestPacketCounts();
         }

      }

      @LuaMethod(
         name = "canConnect",
         global = true
      )
      public static boolean canConnect() {
         return GameClient.instance.canConnect();
      }

      @LuaMethod(
         name = "getReconnectCountdownTimer",
         global = true
      )
      public static String getReconnectCountdownTimer() {
         return GameClient.instance.getReconnectCountdownTimer();
      }

      @LuaMethod(
         name = "getPacketCounts",
         global = true
      )
      public static KahluaTable getPacketCounts(int var0) {
         return GameClient.bClient ? PacketTypes.getPacketCounts(var0) : null;
      }

      @LuaMethod(
         name = "sendEvent",
         global = true
      )
      public static void sendEvent(IsoPlayer var0, String var1) {
         if (GameClient.bClient) {
            GameClient.sendEvent(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendAnimalGenome",
         global = true
      )
      public static void sendAnimalGenome(IsoAnimal var0) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.AnimalCommand, AnimalCommandPacket.Type.UpdateGenome, var0);
         }

      }

      @LuaMethod(
         name = "addAnimal",
         global = true
      )
      public static IsoAnimal addAnimal(IsoCell var0, int var1, int var2, int var3, String var4, AnimalBreed var5, boolean var6) {
         return new IsoAnimal(var0, var1, var2, var3, var4, var5, var6);
      }

      @LuaMethod(
         name = "addAnimal",
         global = true
      )
      public static IsoAnimal addAnimal(IsoCell var0, int var1, int var2, int var3, String var4, AnimalBreed var5) {
         return new IsoAnimal(var0, var1, var2, var3, var4, var5);
      }

      @LuaMethod(
         name = "removeAnimal",
         global = true
      )
      public static void removeAnimal(int var0) {
         IsoAnimal var1 = getAnimal(var0);
         if (var1 != null) {
            var1.remove();
         } else {
            AnimalSynchronizationManager.getInstance().delete((short)var0);
         }

      }

      @LuaMethod(
         name = "getFakeAttacker",
         global = true
      )
      public static IsoGameCharacter getFakeAttacker() {
         return IsoWorld.instance.CurrentCell.getFakeZombieForHit();
      }

      @LuaMethod(
         name = "sendHitPlayer",
         global = true
      )
      public static void sendHitPlayer(IsoPlayer var0, String var1, String var2, String var3, boolean var4, boolean var5, boolean var6, boolean var7) {
         if (GameClient.bClient) {
            Object var8;
            if (var1 == null && IsoPlayer.getInstance().getPrimaryHandItem() != null) {
               var8 = IsoPlayer.getInstance().getPrimaryHandItem();
            } else {
               if (var1 == null) {
                  var1 = "Base.BareHands";
               }

               switch (var1) {
                  case "Base.Katana":
                  case "Base.Pistol":
                     var8 = InventoryItemFactory.CreateItem(var1);
                     break;
                  case "Base.BareHands":
                  default:
                     var8 = IsoPlayer.getInstance().bareHands;
               }
            }

            GameClient.sendPlayerHit(IsoPlayer.getInstance(), var0, (HandWeapon)var8, Float.parseFloat(var2), var5, Float.parseFloat(var3), var4, var6, var7);
         }

      }

      @LuaMethod(
         name = "sendHitVehicle",
         global = true
      )
      public static void sendHitVehicle(IsoGameCharacter var0, String var1, boolean var2, String var3, String var4, boolean var5) {
         if (GameClient.bClient) {
            BaseVehicle var6 = IsoPlayer.getInstance().getNearVehicle();
            if (var6 != null) {
               GameClient.sendVehicleHit(IsoPlayer.getInstance(), var0, var6, Float.parseFloat(var1), var2, Integer.parseInt(var3), Float.parseFloat(var4), var5);
            }
         }

      }

      @LuaMethod(
         name = "requestUsers",
         global = true
      )
      public static void requestUsers() {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.RequestNetworkUsers);
         }

      }

      @LuaMethod(
         name = "requestPVPEvents",
         global = true
      )
      public static void requestPVPEvents() {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.PVPEvents, false);
         }

      }

      @LuaMethod(
         name = "clearPVPEvents",
         global = true
      )
      public static void clearPVPEvents() {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.PVPEvents, true);
         }

      }

      @LuaMethod(
         name = "getUsers",
         global = true
      )
      public static ArrayList<NetworkUser> getUsers() {
         return NetworkUsers.instance.getUsers();
      }

      @LuaMethod(
         name = "networkUserAction",
         global = true
      )
      public static void networkUserAction(String var0, String var1, String var2) {
         INetworkPacket.send(PacketTypes.PacketType.NetworkUserAction, var0, var1, var2);
      }

      @LuaMethod(
         name = "requestRoles",
         global = true
      )
      public static void requestRoles() {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.RequestRoles);
         }

      }

      @LuaMethod(
         name = "getRoles",
         global = true
      )
      public static ArrayList<Role> getRoles() {
         return Roles.getRoles();
      }

      @LuaMethod(
         name = "getCapabilities",
         global = true
      )
      public static ArrayList<Capability> getCapabilities() {
         ArrayList var0 = new ArrayList();
         Capability[] var1 = Capability.values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Capability var4 = var1[var3];
            var0.add(var4);
         }

         return var0;
      }

      @LuaMethod(
         name = "addRole",
         global = true
      )
      public static void addRole(String var0) {
         Roles.addRole(var0);
      }

      @LuaMethod(
         name = "setupRole",
         global = true
      )
      public static void setupRole(Role var0, String var1, Color var2, KahluaTable var3) {
         ArrayList var4 = new ArrayList();
         KahluaTableIterator var5 = var3.iterator();

         while(var5.advance()) {
            if ((Boolean)var5.getValue()) {
               var4.add((Capability)var5.getKey());
            }
         }

         Roles.setupRole(var0.getName(), var1, var2, var4);
      }

      @LuaMethod(
         name = "deleteRole",
         global = true
      )
      public static void deleteRole(String var0) {
         Roles.deleteRole(var0, IsoPlayer.getInstance().getUsername());
      }

      @LuaMethod(
         name = "setDefaultRoleFor",
         global = true
      )
      public static void setDefaultRoleFor(String var0, String var1) {
         Roles.setDefaultRoleFor(var0, var1);
      }

      @LuaMethod(
         name = "getWarNearest",
         global = true
      )
      public static WarManager.War getWarNearest() {
         return WarManager.getWarNearest(IsoPlayer.getInstance());
      }

      @LuaMethod(
         name = "getWars",
         global = true
      )
      public static ArrayList<WarManager.War> getWars() {
         return WarManager.getWarRelevent(IsoPlayer.getInstance());
      }

      @LuaMethod(
         name = "getHutch",
         global = true
      )
      public static IsoHutch getHutch(int var0, int var1, int var2) {
         return IsoHutch.getHutch(var0, var1, var2);
      }

      @LuaMethod(
         name = "getAnimal",
         global = true
      )
      public static IsoAnimal getAnimal(int var0) {
         return AnimalInstanceManager.getInstance().get((short)var0);
      }

      @LuaMethod(
         name = "sendAddAnimalFromHandsInTrailer",
         global = true
      )
      public static void sendAddAnimalFromHandsInTrailer(IsoAnimal var0, IsoPlayer var1, BaseVehicle var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.AddAnimalFromHandsInTrailer, var0, var1, var2, null);
         }

      }

      @LuaMethod(
         name = "sendAddAnimalFromHandsInTrailer",
         global = true
      )
      public static void sendAddAnimalFromHandsInTrailer(IsoDeadBody var0, IsoPlayer var1, BaseVehicle var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.AddAnimalFromHandsInTrailer, var0, var1, var2, null);
         }

      }

      @LuaMethod(
         name = "sendAddAnimalInTrailer",
         global = true
      )
      public static void sendAddAnimalInTrailer(IsoAnimal var0, IsoPlayer var1, BaseVehicle var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.AddAnimalInTrailer, var0, var1, var2, null);
         }

      }

      @LuaMethod(
         name = "sendAddAnimalInTrailer",
         global = true
      )
      public static void sendAddAnimalInTrailer(IsoDeadBody var0, IsoPlayer var1, BaseVehicle var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.AddAnimalInTrailer, var0, var1, var2, null);
         }

      }

      @LuaMethod(
         name = "sendRemoveAnimalFromTrailer",
         global = true
      )
      public static void sendRemoveAnimalFromTrailer(IsoAnimal var0, IsoPlayer var1, BaseVehicle var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.RemoveAnimalFromTrailer, var0, var1, var2, null);
         }

      }

      @LuaMethod(
         name = "sendRemoveAndGrabAnimalFromTrailer",
         global = true
      )
      public static void sendRemoveAndGrabAnimalFromTrailer(IsoAnimal var0, IsoPlayer var1, BaseVehicle var2, AnimalInventoryItem var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.RemoveAndGrabAnimalFromTrailer, var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendRemoveAndGrabAnimalFromTrailer",
         global = true
      )
      public static void sendRemoveAndGrabAnimalFromTrailer(IsoDeadBody var0, IsoPlayer var1, BaseVehicle var2, AnimalInventoryItem var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.RemoveAndGrabAnimalFromTrailer, var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendAttachAnimalToPlayer",
         global = true
      )
      public static void sendAttachAnimalToPlayer(IsoAnimal var0, IsoPlayer var1, IsoObject var2, boolean var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var1.getX(), var1.getY(), AnimalCommandPacket.Type.AttachAnimalToPlayer, var0, var1, var0, var3);
         }

      }

      @LuaMethod(
         name = "sendAttachAnimalToTree",
         global = true
      )
      public static void sendAttachAnimalToTree(IsoAnimal var0, IsoPlayer var1, IsoObject var2, boolean var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var2.getX(), var2.getY(), AnimalCommandPacket.Type.AttachAnimalToTree, var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendPickupAnimal",
         global = true
      )
      public static void sendPickupAnimal(IsoAnimal var0, IsoPlayer var1, AnimalInventoryItem var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var1.getX(), var1.getY(), AnimalCommandPacket.Type.PickupAnimal, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendButcherAnimal",
         global = true
      )
      public static void sendButcherAnimal(IsoDeadBody var0, IsoPlayer var1) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var0.getX(), var0.getY(), AnimalCommandPacket.Type.ButcherAnimal, var0, var1);
         }

      }

      @LuaMethod(
         name = "sendFeedAnimalFromHand",
         global = true
      )
      public static void sendFeedAnimalFromHand(IsoAnimal var0, IsoPlayer var1, InventoryItem var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var1.getX(), var1.getY(), AnimalCommandPacket.Type.FeedAnimalFromHand, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendHutchGrabAnimal",
         global = true
      )
      public static void sendHutchGrabAnimal(IsoAnimal var0, IsoPlayer var1, IsoObject var2, InventoryItem var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var1.getX(), var1.getY(), AnimalCommandPacket.Type.HutchGrabAnimal, var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendHutchGrabCorpseAction",
         global = true
      )
      public static void sendHutchGrabCorpseAction(IsoAnimal var0, IsoPlayer var1, IsoObject var2, InventoryItem var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var1.getX(), var1.getY(), AnimalCommandPacket.Type.HutchGrabCorpseAction, var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendHutchRemoveAnimalAction",
         global = true
      )
      public static void sendHutchRemoveAnimalAction(IsoAnimal var0, IsoPlayer var1, IsoObject var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, var1.getX(), var1.getY(), AnimalCommandPacket.Type.HutchRemoveAnimal, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "getAllItems",
         global = true
      )
      public static ArrayList<Item> getAllItems() {
         return ScriptManager.instance.getAllItems();
      }

      @LuaMethod(
         name = "scoreboardUpdate",
         global = true
      )
      public static void scoreboardUpdate() {
         GameClient.instance.scoreboardUpdate();
      }

      @LuaMethod(
         name = "save",
         global = true
      )
      public static void save(boolean var0) {
         try {
            GameWindow.save(var0);
         } catch (Throwable var2) {
            ExceptionLogger.logException(var2);
         }

      }

      @LuaMethod(
         name = "saveGame",
         global = true
      )
      public static void saveGame() {
         save(true);
      }

      @LuaMethod(
         name = "getAllRecipes",
         global = true
      )
      public static ArrayList<Recipe> getAllRecipes() {
         return new ArrayList(ScriptManager.instance.getAllRecipes());
      }

      @LuaMethod(
         name = "requestUserlog",
         global = true
      )
      public static void requestUserlog(String var0) {
         if (GameClient.bClient) {
            GameClient.instance.requestUserlog(var0);
         }

      }

      @LuaMethod(
         name = "addUserlog",
         global = true
      )
      public static void addUserlog(String var0, String var1, String var2) {
         if (GameClient.bClient) {
            GameClient.instance.addUserlog(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "removeUserlog",
         global = true
      )
      public static void removeUserlog(String var0, String var1, String var2) {
         if (GameClient.bClient) {
            GameClient.instance.removeUserlog(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "tabToX",
         global = true
      )
      public static String tabToX(String var0, int var1) {
         while(var0.length() < var1) {
            var0 = var0 + " ";
         }

         return var0;
      }

      @LuaMethod(
         name = "istype",
         global = true
      )
      public static boolean isType(Object var0, String var1) {
         if (LuaManager.exposer.TypeMap.containsKey(var1)) {
            Class var2 = (Class)LuaManager.exposer.TypeMap.get(var1);
            return var2.equals(var0.getClass());
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "isoToScreenX",
         global = true
      )
      public static float isoToScreenX(int var0, float var1, float var2, float var3) {
         PlayerCamera var4 = IsoCamera.cameras[var0];
         float var5 = var4.fixJigglyModelsSquareX;
         float var6 = var4.fixJigglyModelsSquareY;
         float var7 = IsoUtils.XToScreen(var1 + var5, var2 + var6, var3, 0) - var4.getOffX();
         var7 /= var4.zoom;
         return (float)IsoCamera.getScreenLeft(var0) + var7;
      }

      @LuaMethod(
         name = "isoToScreenY",
         global = true
      )
      public static float isoToScreenY(int var0, float var1, float var2, float var3) {
         PlayerCamera var4 = IsoCamera.cameras[var0];
         float var5 = var4.fixJigglyModelsSquareX;
         float var6 = var4.fixJigglyModelsSquareY;
         float var7 = IsoUtils.YToScreen(var1 + var5, var2 + var6, var3, 0) - var4.getOffY();
         var7 /= var4.zoom;
         return (float)IsoCamera.getScreenTop(var0) + var7;
      }

      @LuaMethod(
         name = "screenToIsoX",
         global = true
      )
      public static float screenToIsoX(int var0, float var1, float var2, float var3) {
         float var4 = Core.getInstance().getZoom(var0);
         var1 -= (float)IsoCamera.getScreenLeft(var0);
         var2 -= (float)IsoCamera.getScreenTop(var0);
         return IsoCamera.cameras[var0].XToIso(var1 * var4, var2 * var4, var3);
      }

      @LuaMethod(
         name = "screenToIsoY",
         global = true
      )
      public static float screenToIsoY(int var0, float var1, float var2, float var3) {
         float var4 = Core.getInstance().getZoom(var0);
         var1 -= (float)IsoCamera.getScreenLeft(var0);
         var2 -= (float)IsoCamera.getScreenTop(var0);
         return IsoCamera.cameras[var0].YToIso(var1 * var4, var2 * var4, var3);
      }

      @LuaMethod(
         name = "getAmbientStreamManager",
         global = true
      )
      public static BaseAmbientStreamManager getAmbientStreamManager() {
         return AmbientStreamManager.instance;
      }

      @LuaMethod(
         name = "getSleepingEvent",
         global = true
      )
      public static SleepingEvent getSleepingEvent() {
         return SleepingEvent.instance;
      }

      @LuaMethod(
         name = "setPlayerMovementActive",
         global = true
      )
      public static void setPlayerMovementActive(int var0, boolean var1) {
         IsoPlayer.players[var0].bJoypadMovementActive = var1;
      }

      @LuaMethod(
         name = "setActivePlayer",
         global = true
      )
      public static void setActivePlayer(int var0) {
         if (!GameClient.bClient) {
            IsoPlayer.setInstance(IsoPlayer.players[var0]);
            IsoCamera.setCameraCharacter(IsoPlayer.getInstance());
         }
      }

      @LuaMethod(
         name = "getPlayer",
         global = true
      )
      public static IsoPlayer getPlayer() {
         return IsoPlayer.getInstance();
      }

      @LuaMethod(
         name = "getNumActivePlayers",
         global = true
      )
      public static int getNumActivePlayers() {
         return IsoPlayer.numPlayers;
      }

      @LuaMethod(
         name = "playServerSound",
         global = true
      )
      public static void playServerSound(String var0, IsoGridSquare var1) {
         GameServer.PlayWorldSoundServer(var0, false, var1, 0.2F, 5.0F, 1.1F, true);
      }

      @LuaMethod(
         name = "getMaxActivePlayers",
         global = true
      )
      public static int getMaxActivePlayers() {
         return 4;
      }

      @LuaMethod(
         name = "getPlayerScreenLeft",
         global = true
      )
      public static int getPlayerScreenLeft(int var0) {
         return IsoCamera.getScreenLeft(var0);
      }

      @LuaMethod(
         name = "getPlayerScreenTop",
         global = true
      )
      public static int getPlayerScreenTop(int var0) {
         return IsoCamera.getScreenTop(var0);
      }

      @LuaMethod(
         name = "getPlayerScreenWidth",
         global = true
      )
      public static int getPlayerScreenWidth(int var0) {
         return IsoCamera.getScreenWidth(var0);
      }

      @LuaMethod(
         name = "getPlayerScreenHeight",
         global = true
      )
      public static int getPlayerScreenHeight(int var0) {
         return IsoCamera.getScreenHeight(var0);
      }

      @LuaMethod(
         name = "getPlayerByOnlineID",
         global = true
      )
      public static IsoPlayer getPlayerByOnlineID(int var0) {
         if (GameServer.bServer) {
            return (IsoPlayer)GameServer.IDToPlayerMap.get((short)var0);
         } else {
            return GameClient.bClient ? (IsoPlayer)GameClient.IDToPlayerMap.get((short)var0) : null;
         }
      }

      @LuaMethod(
         name = "initUISystem",
         global = true
      )
      public static void initUISystem() {
         UIManager.init();
         LuaEventManager.triggerEvent("OnCreatePlayer", 0, IsoPlayer.players[0]);
      }

      @LuaMethod(
         name = "getPerformance",
         global = true
      )
      public static PerformanceSettings getPerformance() {
         return PerformanceSettings.instance;
      }

      @LuaMethod(
         name = "getDBSchema",
         global = true
      )
      public static void getDBSchema() {
         GameClient.instance.getDBSchema();
      }

      @LuaMethod(
         name = "getTableResult",
         global = true
      )
      public static void getTableResult(String var0, int var1) {
         GameClient.instance.getTableResult(var0, var1);
      }

      @LuaMethod(
         name = "getWorldSoundManager",
         global = true
      )
      public static WorldSoundManager getWorldSoundManager() {
         return WorldSoundManager.instance;
      }

      @LuaMethod(
         name = "getAnimalChunk",
         global = true
      )
      public static AnimalChunk getAnimalChunk(int var0, int var1) {
         return AnimalManagerWorker.getInstance().getAnimalChunk((float)var0, (float)var1);
      }

      @LuaMethod(
         name = "AddWorldSound",
         global = true
      )
      public static void AddWorldSound(IsoPlayer var0, int var1, int var2) {
         WorldSoundManager.instance.addSound((Object)null, PZMath.fastfloor(var0.getX()), PZMath.fastfloor(var0.getY()), PZMath.fastfloor(var0.getZ()), var1, var2, false);
      }

      @LuaMethod(
         name = "AddNoiseToken",
         global = true
      )
      public static void AddNoiseToken(IsoGridSquare var0, int var1) {
      }

      @LuaMethod(
         name = "pauseSoundAndMusic",
         global = true
      )
      public static void pauseSoundAndMusic() {
         DebugType.ExitDebug.debugln("pauseSoundAndMusic 1");
         SoundManager.instance.pauseSoundAndMusic();
         DebugType.ExitDebug.debugln("pauseSoundAndMusic 2");
      }

      @LuaMethod(
         name = "resumeSoundAndMusic",
         global = true
      )
      public static void resumeSoundAndMusic() {
         SoundManager.instance.resumeSoundAndMusic();
      }

      @LuaMethod(
         name = "isDemo",
         global = true
      )
      public static boolean isDemo() {
         Core.getInstance();
         return false;
      }

      @LuaMethod(
         name = "getTimeInMillis",
         global = true
      )
      public static long getTimeInMillis() {
         return System.currentTimeMillis();
      }

      @LuaMethod(
         name = "getCurrentCoroutine",
         global = true
      )
      public static Coroutine getCurrentCoroutine() {
         return LuaManager.thread.getCurrentCoroutine();
      }

      @LuaMethod(
         name = "reloadLuaFile",
         global = true
      )
      public static void reloadLuaFile(String var0) {
         LuaManager.loaded.remove(var0);
         LuaManager.RunLua(var0, true);
      }

      @LuaMethod(
         name = "reloadServerLuaFile",
         global = true
      )
      public static void reloadServerLuaFile(String var0) {
         if (GameServer.bServer) {
            String var10000 = ZomboidFileSystem.instance.getCacheDir();
            var0 = var10000 + File.separator + "Server" + File.separator + var0;
            LuaManager.loaded.remove(var0);
            LuaManager.RunLua(var0, true);
         }
      }

      @LuaMethod(
         name = "setSpawnRegion",
         global = true
      )
      public static void setSpawnRegion(String var0) {
         if (GameClient.bClient) {
            IsoWorld.instance.setSpawnRegion(var0);
         }

      }

      @LuaMethod(
         name = "getServerSpawnRegions",
         global = true
      )
      public static KahluaTable getServerSpawnRegions() {
         return !GameClient.bClient ? null : GameClient.instance.getServerSpawnRegions();
      }

      @LuaMethod(
         name = "getServerOptions",
         global = true
      )
      public static ServerOptions getServerOptions() {
         return ServerOptions.instance;
      }

      @LuaMethod(
         name = "getServerName",
         global = true
      )
      public static String getServerName() {
         if (GameServer.bServer) {
            return GameServer.ServerName;
         } else {
            return GameClient.bClient ? GameClient.ServerName : "";
         }
      }

      @LuaMethod(
         name = "getServerIP",
         global = true
      )
      public static String getServerIP() {
         if (GameServer.bServer) {
            return GameServer.IPCommandline == null ? GameServer.ip : GameServer.IPCommandline;
         } else {
            return GameClient.bClient ? GameClient.ip : "";
         }
      }

      @LuaMethod(
         name = "getServerPort",
         global = true
      )
      public static String getServerPort() {
         if (GameServer.bServer) {
            return String.valueOf(GameServer.DEFAULT_PORT);
         } else {
            return GameClient.bClient ? String.valueOf(GameClient.port) : "";
         }
      }

      @LuaMethod(
         name = "isShowConnectionInfo",
         global = true
      )
      public static boolean isShowConnectionInfo() {
         return NetworkAIParams.isShowConnectionInfo();
      }

      @LuaMethod(
         name = "setShowConnectionInfo",
         global = true
      )
      public static void setShowConnectionInfo(boolean var0) {
         NetworkAIParams.setShowConnectionInfo(var0);
      }

      @LuaMethod(
         name = "isShowServerInfo",
         global = true
      )
      public static boolean isShowServerInfo() {
         return NetworkAIParams.isShowServerInfo();
      }

      @LuaMethod(
         name = "setShowServerInfo",
         global = true
      )
      public static void setShowServerInfo(boolean var0) {
         NetworkAIParams.setShowServerInfo(var0);
      }

      @LuaMethod(
         name = "getSpecificPlayer",
         global = true
      )
      public static IsoPlayer getSpecificPlayer(int var0) {
         return IsoPlayer.players[var0];
      }

      @LuaMethod(
         name = "getCameraOffX",
         global = true
      )
      public static float getCameraOffX() {
         return IsoCamera.getOffX();
      }

      @LuaMethod(
         name = "getLatestSave",
         global = true
      )
      public static KahluaTable getLatestSave() {
         KahluaTable var0 = LuaManager.platform.newTable();
         BufferedReader var1 = null;

         try {
            String var10006 = ZomboidFileSystem.instance.getCacheDir();
            var1 = new BufferedReader(new FileReader(new File(var10006 + File.separator + "latestSave.ini")));
         } catch (FileNotFoundException var4) {
            return var0;
         }

         try {
            String var2 = null;

            for(int var3 = 1; (var2 = var1.readLine()) != null; ++var3) {
               var0.rawset(var3, var2);
            }

            var1.close();
            return var0;
         } catch (Exception var5) {
            return var0;
         }
      }

      @LuaMethod(
         name = "isCurrentExecutionPoint",
         global = true
      )
      public static boolean isCurrentExecutionPoint(String var0, int var1) {
         int var2 = LuaManager.thread.currentCoroutine.getCallframeTop() - 1;
         if (var2 < 0) {
            var2 = 0;
         }

         LuaCallFrame var3 = LuaManager.thread.currentCoroutine.getCallFrame(var2);
         if (var3.closure == null) {
            return false;
         } else {
            return var3.closure.prototype.lines[var3.pc] == var1 && var0.equals(var3.closure.prototype.filename);
         }
      }

      @LuaMethod(
         name = "toggleBreakOnChange",
         global = true
      )
      public static void toggleBreakOnChange(KahluaTable var0, Object var1) {
         if (Core.bDebug) {
            LuaManager.thread.toggleBreakOnChange(var0, var1);
         }

      }

      @LuaMethod(
         name = "isDebugEnabled",
         global = true
      )
      public static boolean isDebugEnabled() {
         return Core.bDebug;
      }

      @LuaMethod(
         name = "toggleBreakOnRead",
         global = true
      )
      public static void toggleBreakOnRead(KahluaTable var0, Object var1) {
         if (Core.bDebug) {
            LuaManager.thread.toggleBreakOnRead(var0, var1);
         }

      }

      @LuaMethod(
         name = "toggleBreakpoint",
         global = true
      )
      public static void toggleBreakpoint(String var0, int var1) {
         var0 = var0.replace("\\", "/");
         if (Core.bDebug) {
            LuaManager.thread.breakpointToggle(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendVisual",
         global = true
      )
      public static void sendVisual(IsoPlayer var0) {
         if (GameClient.bClient) {
            GameClient.instance.sendVisual(var0);
         }

      }

      @LuaMethod(
         name = "sendSyncPlayerFields",
         global = true
      )
      public static void sendSyncPlayerFields(IsoPlayer var0, byte var1) {
         if (GameServer.bServer) {
            GameServer.sendSyncPlayerFields(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendClothing",
         global = true
      )
      public static void sendClothing(IsoPlayer var0, String var1, InventoryItem var2) {
         if (GameServer.bServer) {
            GameServer.sendSyncClothing(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "syncVisuals",
         global = true
      )
      public static void syncVisuals(IsoPlayer var0) {
         if (GameServer.bServer) {
            GameServer.syncVisuals(var0);
         }

      }

      @LuaMethod(
         name = "sendEquip",
         global = true
      )
      public static void sendEquip(IsoPlayer var0) {
         if (GameServer.bServer) {
            GameServer.updateHandEquips(GameServer.getConnectionFromPlayer(var0), var0);
         }

      }

      @LuaMethod(
         name = "sendDamage",
         global = true
      )
      public static void sendDamage(IsoPlayer var0) {
         if (GameServer.bServer) {
            INetworkPacket.send(var0, PacketTypes.PacketType.PlayerDamage, var0);
         }

      }

      @LuaMethod(
         name = "sendPlayerEffects",
         global = true
      )
      public static void sendPlayerEffects(IsoPlayer var0) {
         if (GameServer.bServer) {
            INetworkPacket.send(var0, PacketTypes.PacketType.PlayerEffectsSync, var0);
         }

      }

      @LuaMethod(
         name = "sendItemStats",
         global = true
      )
      public static void sendItemStats(InventoryItem var0) {
         if (GameServer.bServer) {
            GameServer.sendItemStats(var0);
         }

      }

      @LuaMethod(
         name = "hasDataReadBreakpoint",
         global = true
      )
      public static boolean hasDataReadBreakpoint(KahluaTable var0, Object var1) {
         return LuaManager.thread.hasReadDataBreakpoint(var0, var1);
      }

      @LuaMethod(
         name = "hasDataBreakpoint",
         global = true
      )
      public static boolean hasDataBreakpoint(KahluaTable var0, Object var1) {
         return LuaManager.thread.hasDataBreakpoint(var0, var1);
      }

      @LuaMethod(
         name = "hasBreakpoint",
         global = true
      )
      public static boolean hasBreakpoint(String var0, int var1) {
         return LuaManager.thread.hasBreakpoint(var0, var1);
      }

      @LuaMethod(
         name = "getLoadedLuaCount",
         global = true
      )
      public static int getLoadedLuaCount() {
         return LuaManager.loaded.size();
      }

      @LuaMethod(
         name = "getLoadedLua",
         global = true
      )
      public static String getLoadedLua(int var0) {
         return (String)LuaManager.loaded.get(var0);
      }

      @LuaMethod(
         name = "isServer",
         global = true
      )
      public static boolean isServer() {
         return GameServer.bServer;
      }

      @LuaMethod(
         name = "isServerSoftReset",
         global = true
      )
      public static boolean isServerSoftReset() {
         return GameServer.bServer && GameServer.bSoftReset;
      }

      @LuaMethod(
         name = "isClient",
         global = true
      )
      public static boolean isClient() {
         return GameClient.bClient;
      }

      @LuaMethod(
         name = "isMultiplayer",
         global = true
      )
      public static boolean isMultiplayer() {
         return GameClient.bClient || GameServer.bServer;
      }

      @LuaMethod(
         name = "executeQuery",
         global = true
      )
      public static void executeQuery(String var0, KahluaTable var1) {
         GameClient.instance.executeQuery(var0, var1);
      }

      @LuaMethod(
         name = "canSeePlayerStats",
         global = true
      )
      public static boolean canSeePlayerStats() {
         return GameClient.canSeePlayerStats();
      }

      /** @deprecated */
      @Deprecated
      @LuaMethod(
         name = "getAccessLevel",
         global = true
      )
      public static String getAccessLevel() {
         return GameClient.connection.role.getName();
      }

      /** @deprecated */
      @Deprecated
      @LuaMethod(
         name = "haveAccess",
         global = true
      )
      public static boolean haveAccess(String var0) {
         try {
            Capability var1 = Capability.valueOf(var0);
            return GameClient.connection.role.haveCapability(var1);
         } catch (Exception var2) {
            DebugLog.General.printException(var2, "access=" + var0, LogSeverity.Error);
            return false;
         }
      }

      @LuaMethod(
         name = "getOnlinePlayers",
         global = true
      )
      public static ArrayList<IsoPlayer> getOnlinePlayers() {
         if (GameServer.bServer) {
            return GameServer.getPlayers();
         } else {
            return GameClient.bClient ? GameClient.instance.getPlayers() : new ArrayList();
         }
      }

      @LuaMethod(
         name = "getDebug",
         global = true
      )
      public static boolean getDebug() {
         return Core.bDebug || GameServer.bServer && GameServer.bDebug;
      }

      @LuaMethod(
         name = "getCameraOffY",
         global = true
      )
      public static float getCameraOffY() {
         return IsoCamera.getOffY();
      }

      @LuaMethod(
         name = "createRegionFile",
         global = true
      )
      public static KahluaTable createRegionFile() {
         KahluaTable var0 = LuaManager.platform.newTable();
         String var1 = IsoWorld.instance.getMap();
         if (var1.equals("DEFAULT")) {
            MapGroups var2 = new MapGroups();
            var2.createGroups();
            if (var2.getNumberOfGroups() != 1) {
               throw new RuntimeException("GameMap is DEFAULT but there are multiple worlds to choose from");
            }

            var2.setWorld(0);
            var1 = IsoWorld.instance.getMap();
         }

         String[] var10 = var1.split(";");
         int var3 = 1;
         String[] var4 = var10;
         int var5 = var10.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            String var7 = var4[var6];
            var7 = var7.trim();
            if (!var7.isEmpty()) {
               File var8 = new File(ZomboidFileSystem.instance.getString("media/maps/" + var7 + "/spawnpoints.lua"));
               if (var8.exists()) {
                  KahluaTable var9 = LuaManager.platform.newTable();
                  var9.rawset("name", var7);
                  var9.rawset("file", "media/maps/" + var7 + "/spawnpoints.lua");
                  var0.rawset(var3, var9);
                  ++var3;
               }
            }
         }

         return var0;
      }

      @LuaMethod(
         name = "getMapDirectoryTable",
         global = true
      )
      public static KahluaTable getMapDirectoryTable() {
         KahluaTable var0 = LuaManager.platform.newTable();
         File var1 = ZomboidFileSystem.instance.getMediaFile("maps");
         String[] var2 = var1.list();
         if (var2 == null) {
            return var0;
         } else {
            int var3 = 1;

            String var5;
            for(int var4 = 0; var4 < var2.length; ++var4) {
               var5 = var2[var4];
               if (!var5.equals("challengemaps")) {
                  var0.rawset(var3, var5);
                  ++var3;
               }
            }

            Iterator var11 = ZomboidFileSystem.instance.getModIDs().iterator();

            while(var11.hasNext()) {
               var5 = (String)var11.next();
               ChooseGameInfo.Mod var6 = null;

               try {
                  var6 = ChooseGameInfo.getAvailableModDetails(var5);
               } catch (Exception var10) {
               }

               if (var6 != null) {
                  var1 = new File(var6.getCommonDir() + "/media/maps/");
                  int var7;
                  String var8;
                  ChooseGameInfo.Map var9;
                  if (var1.exists()) {
                     var2 = var1.list();
                     if (var2 != null) {
                        for(var7 = 0; var7 < var2.length; ++var7) {
                           var8 = var2[var7];
                           var9 = ChooseGameInfo.getMapDetails(var8);
                           if (var9.getLotDirectories() != null && !var9.getLotDirectories().isEmpty() && !var8.equals("challengemaps")) {
                              var0.rawset(var3, var8);
                              ++var3;
                           }
                        }
                     }
                  }

                  var1 = new File(var6.getVersionDir() + "/media/maps/");
                  if (var1.exists()) {
                     var2 = var1.list();
                     if (var2 != null) {
                        for(var7 = 0; var7 < var2.length; ++var7) {
                           var8 = var2[var7];
                           var9 = ChooseGameInfo.getMapDetails(var8);
                           if (var9.getLotDirectories() != null && !var9.getLotDirectories().isEmpty() && !var8.equals("challengemaps")) {
                              var0.rawset(var3, var8);
                              ++var3;
                           }
                        }
                     }
                  }
               }
            }

            return var0;
         }
      }

      @LuaMethod(
         name = "deleteSave",
         global = true
      )
      public static void deleteSave(String var0) {
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var1 = new File(var10002 + File.separator + var0);
         String[] var2 = var1.list();
         if (var2 != null) {
            for(int var3 = 0; var3 < var2.length; ++var3) {
               File var4 = new File(ZomboidFileSystem.instance.getSaveDir() + File.separator + var0 + File.separator + var2[var3]);
               if (var4.isDirectory()) {
                  deleteSave(var0 + File.separator + var4.getName());
               }

               var4.delete();
            }

            var1.delete();
         }
      }

      @LuaMethod(
         name = "sendPlayerExtraInfo",
         global = true
      )
      public static void sendPlayerExtraInfo(IsoPlayer var0) {
         GameClient.sendPlayerExtraInfo(var0);
      }

      @LuaMethod(
         name = "getServerAddressFromArgs",
         global = true
      )
      public static String getServerAddressFromArgs() {
         if (System.getProperty("args.server.connect") != null) {
            String var0 = System.getProperty("args.server.connect");
            System.clearProperty("args.server.connect");
            return var0;
         } else {
            return null;
         }
      }

      @LuaMethod(
         name = "getServerPasswordFromArgs",
         global = true
      )
      public static String getServerPasswordFromArgs() {
         if (System.getProperty("args.server.password") != null) {
            String var0 = System.getProperty("args.server.password");
            System.clearProperty("args.server.password");
            return var0;
         } else {
            return null;
         }
      }

      @LuaMethod(
         name = "getServerListFile",
         global = true
      )
      public static String getServerListFile() {
         return SteamUtils.isSteamModeEnabled() ? "ServerListSteam.txt" : "ServerList.txt";
      }

      @LuaMethod(
         name = "getServerList",
         global = true
      )
      public static KahluaTable getServerList() {
         ArrayList var0 = new ArrayList();
         KahluaTable var1 = LuaManager.platform.newTable();
         BufferedReader var2 = null;

         try {
            String var10002 = LuaManager.getLuaCacheDir();
            File var3 = new File(var10002 + File.separator + getServerListFile());
            if (!var3.exists()) {
               var3.createNewFile();
            }

            var2 = new BufferedReader(new FileReader(var3, StandardCharsets.UTF_8));
            String var4 = null;
            Server var5 = null;
            boolean var6 = true;

            while((var4 = var2.readLine()) != null) {
               if (var4.startsWith("name=")) {
                  var5 = new Server();
                  var0.add(var5);
                  var5.setName(var4.replaceFirst("name=", ""));
                  var6 = true;
                  var10002 = LuaManager.getLuaCacheDir();
                  File var7 = new File(var10002 + File.separator + var5.getName() + "_icon.jpg");
                  if (var7.exists()) {
                     var5.setServerIcon(new Texture(var7.getAbsolutePath()));
                  } else {
                     var5.setServerIcon(Texture.getSharedTexture("media/ui/zomboidIcon64.png"));
                  }

                  var10002 = LuaManager.getLuaCacheDir();
                  var7 = new File(var10002 + File.separator + var5.getName() + "_loadingScreen.jpg");
                  if (var7.exists()) {
                     var5.setServerLoadingScreen(new Texture(var7.getAbsolutePath()));
                  }

                  var10002 = LuaManager.getLuaCacheDir();
                  var7 = new File(var10002 + File.separator + var5.getName() + "_loginScreen.jpg");
                  if (var7.exists()) {
                     var5.setServerLoginScreen(new Texture(var7.getAbsolutePath()));
                  }
               } else if (var4.startsWith("serverCustomizationLastUpdate=")) {
                  var5.setServerCustomizationLastUpdate(Integer.valueOf(var4.replaceFirst("serverCustomizationLastUpdate=", "")));
               } else if (var4.startsWith("ip=")) {
                  var5.setIp(var4.replaceFirst("ip=", ""));
               } else if (var4.startsWith("localip=")) {
                  var5.setLocalIP(var4.replaceFirst("localip=", ""));
               } else if (var4.startsWith("description=")) {
                  var5.setDescription(var4.replaceFirst("description=", ""));
               } else if (var4.startsWith("port=")) {
                  var5.setPort(var4.replaceFirst("port=", ""));
               } else if (var4.startsWith("user=")) {
                  var5.setUserName(var4.replaceFirst("user=", ""));
               } else if (var4.startsWith("remember=")) {
                  var5.setSavePwd(Boolean.parseBoolean(var4.replaceFirst("remember=", "")));
                  var6 = false;
               } else if (var4.startsWith("authType=")) {
                  var5.setAuthType(Integer.parseInt(var4.replaceFirst("authType=", "")));
               } else if (var4.startsWith("loginScreenId=")) {
                  var5.setLoginScreenId(Integer.parseInt(var4.replaceFirst("loginScreenId=", "")));
               } else if (var4.startsWith("password=")) {
                  if (var6) {
                     var5.setNeedSave(true);
                  }

                  var5.setPwd(var4.replaceFirst("password=", ""), var6);
               } else if (var4.startsWith("serverpassword=")) {
                  var5.setServerPassword(var4.replaceFirst("serverpassword=", ""));
               } else if (var4.startsWith("usesteamrelay=")) {
                  var5.setUseSteamRelay(Boolean.parseBoolean(var4.replaceFirst("usesteamrelay=", "")));
               }
            }

            int var21 = 1;

            for(int var8 = 0; var8 < var0.size(); ++var8) {
               Server var9 = (Server)var0.get(var8);
               Double var10 = (double)var21;
               var1.rawset(var10, var9);
               ++var21;
            }
         } catch (Exception var19) {
            var19.printStackTrace();
         } finally {
            try {
               var2.close();
            } catch (Exception var18) {
            }

         }

         return var1;
      }

      @LuaMethod(
         name = "ping",
         global = true
      )
      public static void ping(String var0, String var1, String var2, String var3, boolean var4) {
         ConnectionManager.getInstance().ping(var0, var1, var2, var3, var4);
      }

      @LuaMethod(
         name = "getCustomizationData",
         global = true
      )
      public static void getCustomizationData(String var0, String var1, String var2, String var3, String var4, String var5, boolean var6) {
         ConnectionManager.getInstance().getCustomizationData(var0, var1, var2, var3, var4, var5, var6);
      }

      @LuaMethod(
         name = "getClientLoadingScreen",
         global = true
      )
      public static Texture getClientLoadingScreen(int var0) {
         return CustomizationManager.getInstance().getClientCustomBackground(var0);
      }

      @LuaMethod(
         name = "stopPing",
         global = true
      )
      public static void stopPing() {
         ConnectionManager.getInstance().stopPing();
      }

      @LuaMethod(
         name = "transformIntoKahluaTable",
         global = true
      )
      public static KahluaTable transformIntoKahluaTable(HashMap<Object, Object> var0) {
         KahluaTable var1 = LuaManager.platform.newTable();
         Iterator var2 = var0.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            var1.rawset(var3.getKey(), var3.getValue());
         }

         return var1;
      }

      @LuaMethod(
         name = "getSaveDirectory",
         global = true
      )
      public static ArrayList<File> getSaveDirectory(String var0) {
         File var1 = new File(var0 + File.separator);
         if (!var1.exists() && !Core.getInstance().isNoSave()) {
            var1.mkdir();
         }

         String[] var2 = var1.list();
         if (var2 == null) {
            return null;
         } else {
            ArrayList var3 = new ArrayList();

            for(int var4 = 0; var4 < var2.length; ++var4) {
               File var5 = new File(var0 + File.separator + var2[var4]);
               if (var5.isDirectory()) {
                  var3.add(var5);
               }
            }

            return var3;
         }
      }

      @LuaMethod(
         name = "getFullSaveDirectoryTable",
         global = true
      )
      public static KahluaTable getFullSaveDirectoryTable() {
         KahluaTable var0 = LuaManager.platform.newTable();
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var1 = new File(var10002 + File.separator);
         if (!var1.exists()) {
            var1.mkdir();
         }

         String[] var2 = var1.list();
         if (var2 == null) {
            return var0;
         } else {
            ArrayList var3 = new ArrayList();

            int var4;
            for(var4 = 0; var4 < var2.length; ++var4) {
               var10002 = ZomboidFileSystem.instance.getSaveDir();
               File var5 = new File(var10002 + File.separator + var2[var4]);
               if (var5.isDirectory() && !"Multiplayer".equals(var2[var4])) {
                  String var10000 = ZomboidFileSystem.instance.getSaveDir();
                  ArrayList var6 = getSaveDirectory(var10000 + File.separator + var2[var4]);
                  var3.addAll(var6);
               }
            }

            Collections.sort(var3, new Comparator<File>() {
               public int compare(File var1, File var2) {
                  return Long.valueOf(var2.lastModified()).compareTo(var1.lastModified());
               }
            });
            var4 = 1;

            for(int var9 = 0; var9 < var3.size(); ++var9) {
               File var10 = (File)var3.get(var9);
               String var7 = getSaveName(var10);
               Double var8 = (double)var4;
               var0.rawset(var8, var7);
               ++var4;
            }

            return var0;
         }
      }

      public static String getSaveName(File var0) {
         String[] var1 = var0.getAbsolutePath().split("\\" + File.separator);
         return var1[var1.length - 2] + File.separator + var0.getName();
      }

      @LuaMethod(
         name = "getSaveDirectoryTable",
         global = true
      )
      public static KahluaTable getSaveDirectoryTable() {
         return LuaManager.platform.newTable();
      }

      @LuaMethod(
         name = "getCurrentSaveName",
         global = true
      )
      public static String getCurrentSaveName() {
         return ZomboidFileSystem.instance.getCurrentSaveDir();
      }

      public static List<String> getMods() {
         ArrayList var0 = new ArrayList();
         ZomboidFileSystem.instance.getAllModFolders(var0);
         return var0;
      }

      @LuaMethod(
         name = "doChallenge",
         global = true
      )
      public static void doChallenge(KahluaTable var0) {
         Core.GameMode = var0.rawget("gameMode").toString();
         Core.ChallengeID = var0.rawget("id").toString();
         Core.bLastStand = Core.GameMode.equals("LastStand");
         Core.getInstance().setChallenge(true);
         getWorld().setMap(var0.getString("world"));
         Integer var1 = Rand.Next(100000000);
         IsoWorld.instance.setWorld(var1.toString());
         getWorld().bDoChunkMapUpdate = false;
      }

      @LuaMethod(
         name = "doTutorial",
         global = true
      )
      public static void doTutorial(KahluaTable var0) {
         Core.GameMode = "Tutorial";
         Core.bLastStand = false;
         Core.ChallengeID = null;
         Core.getInstance().setChallenge(false);
         Core.bTutorial = true;
         getWorld().setMap(var0.getString("world"));
         getWorld().bDoChunkMapUpdate = false;
      }

      @LuaMethod(
         name = "deleteAllGameModeSaves",
         global = true
      )
      public static void deleteAllGameModeSaves(String var0) {
         String var1 = Core.GameMode;
         Core.GameMode = var0;
         Path var2 = Paths.get(ZomboidFileSystem.instance.getGameModeCacheDir());
         if (!Files.exists(var2, new LinkOption[0])) {
            Core.GameMode = var1;
         } else {
            try {
               Files.walkFileTree(var2, new FileVisitor<Path>() {
                  public FileVisitResult preVisitDirectory(Path var1, BasicFileAttributes var2) throws IOException {
                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult visitFile(Path var1, BasicFileAttributes var2) throws IOException {
                     Files.delete(var1);
                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult visitFileFailed(Path var1, IOException var2) throws IOException {
                     var2.printStackTrace();
                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult postVisitDirectory(Path var1, IOException var2) throws IOException {
                     Files.delete(var1);
                     return FileVisitResult.CONTINUE;
                  }
               });
            } catch (IOException var4) {
               var4.printStackTrace();
            }

            Core.GameMode = var1;
         }
      }

      @LuaMethod(
         name = "sledgeDestroy",
         global = true
      )
      public static void sledgeDestroy(IsoObject var0) {
         if (GameClient.bClient) {
            GameClient.destroy(var0);
         }

      }

      @LuaMethod(
         name = "getTickets",
         global = true
      )
      public static void getTickets(String var0) {
         if (GameClient.bClient) {
            GameClient.getTickets(var0);
         }

      }

      @LuaMethod(
         name = "addTicket",
         global = true
      )
      public static void addTicket(String var0, String var1, int var2) {
         if (GameClient.bClient) {
            GameClient.addTicket(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "removeTicket",
         global = true
      )
      public static void removeTicket(int var0) {
         if (GameClient.bClient) {
            GameClient.removeTicket(var0);
         }

      }

      @LuaMethod(
         name = "sendFactionInvite",
         global = true
      )
      public static void sendFactionInvite(Faction var0, IsoPlayer var1, String var2) {
         if (GameClient.bClient) {
            GameClient.sendFactionInvite(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "acceptFactionInvite",
         global = true
      )
      public static void acceptFactionInvite(Faction var0, String var1) {
         if (GameClient.bClient) {
            GameClient.acceptFactionInvite(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendSafehouseInvite",
         global = true
      )
      public static void sendSafehouseInvite(SafeHouse var0, IsoPlayer var1, String var2) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseInvite, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "acceptSafehouseInvite",
         global = true
      )
      public static void acceptSafehouseInvite(SafeHouse var0, String var1, IsoPlayer var2) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseAccept, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendSafehouseChangeMember",
         global = true
      )
      public static void sendSafehouseChangeMember(SafeHouse var0, String var1) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseChangeMember, var0, var1);
         }

      }

      @LuaMethod(
         name = "sendSafehouseChangeOwner",
         global = true
      )
      public static void sendSafehouseChangeOwner(SafeHouse var0, String var1) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseChangeOwner, var0, var1);
         }

      }

      @LuaMethod(
         name = "sendSafehouseChangeRespawn",
         global = true
      )
      public static void sendSafehouseChangeRespawn(SafeHouse var0, String var1, boolean var2) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseChangeRespawn, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendSafehouseChangeTitle",
         global = true
      )
      public static void sendSafehouseChangeTitle(SafeHouse var0, String var1) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseChangeTitle, var0, var1);
         }

      }

      @LuaMethod(
         name = "sendSafezoneClaim",
         global = true
      )
      public static void sendSafezoneClaim(String var0, int var1, int var2, int var3, int var4, String var5) {
         if (GameClient.bClient) {
            IsoPlayer var6 = GameClient.instance.getPlayerFromUsername(var0);
            INetworkPacket.send(PacketTypes.PacketType.SafezoneClaim, var6, var1, var2, var3, var4, var5);
         }

      }

      @LuaMethod(
         name = "sendSafehouseClaim",
         global = true
      )
      public static void sendSafehouseClaim(IsoGridSquare var0, IsoPlayer var1, String var2) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseClaim, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendSafehouseRelease",
         global = true
      )
      public static void sendSafehouseRelease(SafeHouse var0) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SafehouseRelease, var0);
         }

      }

      @LuaMethod(
         name = "createHordeFromTo",
         global = true
      )
      public static void createHordeFromTo(float var0, float var1, float var2, float var3, int var4) {
         ZombiePopulationManager.instance.createHordeFromTo(PZMath.fastfloor(var0), PZMath.fastfloor(var1), PZMath.fastfloor(var2), PZMath.fastfloor(var3), var4);
      }

      @LuaMethod(
         name = "createHordeInAreaTo",
         global = true
      )
      public static void createHordeInAreaTo(int var0, int var1, int var2, int var3, int var4, int var5, int var6) {
         ZombiePopulationManager.instance.createHordeInAreaTo(var0, var1, var2, var3, var4, var5, var6);
      }

      @LuaMethod(
         name = "spawnHorde",
         global = true
      )
      public static void spawnHorde(float var0, float var1, float var2, float var3, float var4, int var5) {
         for(int var6 = 0; var6 < var5; ++var6) {
            VirtualZombieManager.instance.choices.clear();
            IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)Rand.Next(var0, var2), (double)Rand.Next(var1, var3), (double)var4);
            if (var7 != null) {
               VirtualZombieManager.instance.choices.add(var7);
               IsoZombie var8 = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.fromIndex(Rand.Next(IsoDirections.Max.index())).index(), false);
               var8.dressInRandomOutfit();
               ZombieSpawnRecorder.instance.record(var8, "LuaManager.spawnHorde");
            }
         }

      }

      @LuaMethod(
         name = "createZombie",
         global = true
      )
      public static IsoZombie createZombie(float var0, float var1, float var2, SurvivorDesc var3, int var4, IsoDirections var5) {
         VirtualZombieManager.instance.choices.clear();
         IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare((double)var0, (double)var1, (double)var2);
         VirtualZombieManager.instance.choices.add(var6);
         IsoZombie var7 = VirtualZombieManager.instance.createRealZombieAlways(var5.index(), false);
         ZombieSpawnRecorder.instance.record(var7, "LuaManager.createZombie");
         return var7;
      }

      @LuaMethod(
         name = "triggerEvent",
         global = true
      )
      public static void triggerEvent(String var0) {
         LuaEventManager.triggerEvent(var0);
      }

      @LuaMethod(
         name = "triggerEvent",
         global = true
      )
      public static void triggerEvent(String var0, Object var1) {
         LuaEventManager.triggerEventGarbage(var0, var1);
      }

      @LuaMethod(
         name = "triggerEvent",
         global = true
      )
      public static void triggerEvent(String var0, Object var1, Object var2) {
         LuaEventManager.triggerEventGarbage(var0, var1, var2);
      }

      @LuaMethod(
         name = "triggerEvent",
         global = true
      )
      public static void triggerEvent(String var0, Object var1, Object var2, Object var3) {
         LuaEventManager.triggerEventGarbage(var0, var1, var2, var3);
      }

      @LuaMethod(
         name = "triggerEvent",
         global = true
      )
      public static void triggerEvent(String var0, Object var1, Object var2, Object var3, Object var4) {
         LuaEventManager.triggerEventGarbage(var0, var1, var2, var3, var4);
      }

      @LuaMethod(
         name = "debugLuaTable",
         global = true
      )
      public static void debugLuaTable(Object var0, int var1) {
         if (var1 <= 1) {
            if (var0 instanceof KahluaTable) {
               KahluaTable var2 = (KahluaTable)var0;
               KahluaTableIterator var3 = var2.iterator();
               String var4 = "";

               for(int var5 = 0; var5 < var1; ++var5) {
                  var4 = var4 + "\t";
               }

               do {
                  Object var7 = var3.getKey();
                  Object var6 = var3.getValue();
                  if (var7 != null) {
                     if (var6 != null) {
                        DebugLog.Lua.debugln(var4 + var7 + " : " + var6.toString());
                     }

                     if (var6 instanceof KahluaTable) {
                        debugLuaTable(var6, var1 + 1);
                     }
                  }
               } while(var3.advance());

               if (var2.getMetatable() != null) {
                  debugLuaTable(var2.getMetatable(), var1);
               }
            }

         }
      }

      @LuaMethod(
         name = "debugLuaTable",
         global = true
      )
      public static void debugLuaTable(Object var0) {
         debugLuaTable(var0, 0);
      }

      @LuaMethod(
         name = "sendItemsInContainer",
         global = true
      )
      public static void sendItemsInContainer(IsoObject var0, ItemContainer var1) {
         GameServer.sendItemsInContainer(var0, var1 == null ? var0.getContainer() : var1);
      }

      @LuaMethod(
         name = "getModDirectoryTable",
         global = true
      )
      public static KahluaTable getModDirectoryTable() {
         KahluaTable var0 = LuaManager.platform.newTable();
         List var1 = getMods();
         int var2 = 1;

         for(int var3 = 0; var3 < var1.size(); ++var3) {
            String var4 = (String)var1.get(var3);
            Double var5 = (double)var2;
            var0.rawset(var5, var4);
            ++var2;
         }

         return var0;
      }

      @LuaMethod(
         name = "getModInfoByID",
         global = true
      )
      public static ChooseGameInfo.Mod getModInfoByID(String var0) {
         try {
            return ChooseGameInfo.getModDetails(var0);
         } catch (Exception var2) {
            var2.printStackTrace();
            return null;
         }
      }

      @LuaMethod(
         name = "getModInfo",
         global = true
      )
      public static ChooseGameInfo.Mod getModInfo(String var0) {
         try {
            return ChooseGameInfo.readModInfo(var0);
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
            return null;
         }
      }

      @LuaMethod(
         name = "getMapFoldersForMod",
         global = true
      )
      public static ArrayList<String> getMapFoldersForMod(String var0) {
         try {
            ChooseGameInfo.Mod var1 = ChooseGameInfo.getModDetails(var0);
            if (var1 == null) {
               return null;
            } else {
               ArrayList var2 = null;
               String var10000 = var1.getCommonDir();
               String var3 = var10000 + File.separator + "media" + File.separator + "maps";
               File var4 = new File(var3);
               DirectoryStream var5;
               Iterator var6;
               Path var7;
               if (var4.exists() && var4.isDirectory()) {
                  var5 = Files.newDirectoryStream(var4.toPath());

                  try {
                     var6 = var5.iterator();

                     while(var6.hasNext()) {
                        var7 = (Path)var6.next();
                        if (Files.isDirectory(var7, new LinkOption[0])) {
                           var4 = new File(var3 + File.separator + var7.getFileName().toString() + File.separator + "map.info");
                           if (var4.exists()) {
                              if (var2 == null) {
                                 var2 = new ArrayList();
                              }

                              var2.add(var7.getFileName().toString());
                           }
                        }
                     }
                  } catch (Throwable var11) {
                     if (var5 != null) {
                        try {
                           var5.close();
                        } catch (Throwable var8) {
                           var11.addSuppressed(var8);
                        }
                     }

                     throw var11;
                  }

                  if (var5 != null) {
                     var5.close();
                  }
               }

               var10000 = var1.getVersionDir();
               var3 = var10000 + File.separator + "media" + File.separator + "maps";
               var4 = new File(var3);
               if (var4.exists() && var4.isDirectory()) {
                  var5 = Files.newDirectoryStream(var4.toPath());

                  try {
                     var6 = var5.iterator();

                     while(var6.hasNext()) {
                        var7 = (Path)var6.next();
                        if (Files.isDirectory(var7, new LinkOption[0])) {
                           var4 = new File(var3 + File.separator + var7.getFileName().toString() + File.separator + "map.info");
                           if (var4.exists()) {
                              if (var2 == null) {
                                 var2 = new ArrayList();
                              }

                              var2.add(var7.getFileName().toString());
                           }
                        }
                     }
                  } catch (Throwable var10) {
                     if (var5 != null) {
                        try {
                           var5.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (var5 != null) {
                     var5.close();
                  }
               }

               return var2;
            }
         } catch (Exception var12) {
            var12.printStackTrace();
            return null;
         }
      }

      @LuaMethod(
         name = "spawnpointsExistsForMod",
         global = true
      )
      public static boolean spawnpointsExistsForMod(String var0, String var1) {
         try {
            ChooseGameInfo.Mod var2 = ChooseGameInfo.getModDetails(var0);
            if (var2 == null) {
               return false;
            } else {
               String var10000 = var2.getCommonDir();
               String var3 = var10000 + File.separator + "media" + File.separator + "maps" + File.separator + var1 + File.separator + "spawnpoints.lua";
               var10000 = var2.getVersionDir();
               String var4 = var10000 + File.separator + "media" + File.separator + "maps" + File.separator + var1 + File.separator + "spawnpoints.lua";
               File var5 = new File(var3);
               File var6 = new File(var4);
               return var5.exists() || var6.exists();
            }
         } catch (Exception var7) {
            var7.printStackTrace();
            return false;
         }
      }

      @LuaMethod(
         name = "getFileSeparator",
         global = true
      )
      public static String getFileSeparator() {
         return File.separator;
      }

      @LuaMethod(
         name = "getScriptManager",
         global = true
      )
      public static ScriptManager getScriptManager() {
         return ScriptManager.instance;
      }

      @LuaMethod(
         name = "checkSaveFolderExists",
         global = true
      )
      public static boolean checkSaveFolderExists(String var0) {
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var1 = new File(var10002 + File.separator + var0);
         return var1.exists();
      }

      @LuaMethod(
         name = "getAbsoluteSaveFolderName",
         global = true
      )
      public static String getAbsoluteSaveFolderName(String var0) {
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var1 = new File(var10002 + File.separator + var0);
         return var1.getAbsolutePath();
      }

      @LuaMethod(
         name = "checkSaveFileExists",
         global = true
      )
      public static boolean checkSaveFileExists(String var0) {
         File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave(var0));
         return var1.exists();
      }

      @LuaMethod(
         name = "checkSavePlayerExists",
         global = true
      )
      public static boolean checkSavePlayerExists() {
         if (!GameClient.bClient) {
            return PlayerDBHelper.isPlayerAlive(ZomboidFileSystem.instance.getCurrentSaveDir(), 1);
         } else if (ClientPlayerDB.getInstance() == null) {
            return false;
         } else {
            return ClientPlayerDB.getInstance().clientLoadNetworkPlayer() && ClientPlayerDB.getInstance().isAliveMainNetworkPlayer();
         }
      }

      @LuaMethod(
         name = "cacheFileExists",
         global = true
      )
      public static boolean cacheFileExists(String var0) {
         String var1 = var0.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         String var10002 = ZomboidFileSystem.instance.getCacheDir();
         File var2 = new File(var10002 + File.separator + "Lua" + File.separator + var1);
         return var2.exists();
      }

      @LuaMethod(
         name = "fileExists",
         global = true
      )
      public static boolean fileExists(String var0) {
         String var1 = var0.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         File var2 = new File(ZomboidFileSystem.instance.getString(var1));
         return var2.exists();
      }

      @LuaMethod(
         name = "serverFileExists",
         global = true
      )
      public static boolean serverFileExists(String var0) {
         String var1 = var0.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         String var10002 = ZomboidFileSystem.instance.getCacheDir();
         File var2 = new File(var10002 + File.separator + "Server" + File.separator + var1);
         return var2.exists();
      }

      @LuaMethod(
         name = "takeScreenshot",
         global = true
      )
      public static void takeScreenshot() {
         Core.getInstance().TakeFullScreenshot((String)null);
      }

      @LuaMethod(
         name = "takeScreenshot",
         global = true
      )
      public static void takeScreenshot(String var0) {
         Core.getInstance().TakeFullScreenshot(var0);
      }

      @LuaMethod(
         name = "checkStringPattern",
         global = true
      )
      public static boolean checkStringPattern(String var0) {
         return !var0.contains("[");
      }

      @LuaMethod(
         name = "instanceItem",
         global = true
      )
      public static InventoryItem instanceItem(Item var0) {
         return InventoryItemFactory.CreateItem(var0.moduleDotType);
      }

      @LuaMethod(
         name = "instanceItem",
         global = true
      )
      public static InventoryItem instanceItem(String var0) {
         return InventoryItemFactory.CreateItem(var0);
      }

      @LuaMethod(
         name = "instanceItem",
         global = true
      )
      public static InventoryItem instanceItem(String var0, float var1) {
         return InventoryItemFactory.CreateItem(var0, var1);
      }

      @LuaMethod(
         name = "createNewScriptItem",
         global = true
      )
      public static Item createNewScriptItem(String var0, String var1, String var2, String var3, String var4) {
         Item var5 = new Item();
         var5.setModule(ScriptManager.instance.getModule(var0));
         var5.getModule().items.getScriptMap().put(var1, var5);
         var5.Icon = "Item_" + var4;
         var5.DisplayName = var2;
         var5.name = var1;
         var5.moduleDotType = var5.getModule().name + "." + var1;

         try {
            var5.type = Item.Type.valueOf(var3);
         } catch (Exception var7) {
         }

         return var5;
      }

      @LuaMethod(
         name = "cloneItemType",
         global = true
      )
      public static Item cloneItemType(String var0, String var1) {
         Item var2 = ScriptManager.instance.FindItem(var1);
         Item var3 = new Item();
         var3.setModule(var2.getModule());
         var3.getModule().items.getScriptMap().put(var0, var3);
         return var3;
      }

      @LuaMethod(
         name = "moduleDotType",
         global = true
      )
      public static String moduleDotType(String var0, String var1) {
         return StringUtils.moduleDotType(var0, var1);
      }

      @LuaMethod(
         name = "require",
         global = true
      )
      public static Object require(String var0) {
         String var1 = var0;
         if (!var0.endsWith(".lua")) {
            var1 = var0 + ".lua";
         }

         for(int var2 = 0; var2 < LuaManager.paths.size(); ++var2) {
            String var3 = (String)LuaManager.paths.get(var2);
            String var4 = ZomboidFileSystem.instance.getAbsolutePath(var3 + var1);
            if (var4 != null) {
               return LuaManager.RunLua(ZomboidFileSystem.instance.getString(var4));
            }
         }

         DebugLog.Lua.warn("require(\"" + var0 + "\") failed");
         return null;
      }

      @LuaMethod(
         name = "getRenderer",
         global = true
      )
      public static SpriteRenderer getRenderer() {
         return SpriteRenderer.instance;
      }

      @LuaMethod(
         name = "getGameTime",
         global = true
      )
      public static GameTime getGameTime() {
         return GameTime.instance;
      }

      @LuaMethod(
         name = "getMPStatistics",
         global = true
      )
      public static KahluaTable getStatistics() {
         return MPStatistics.getLuaStatistics();
      }

      @LuaMethod(
         name = "getMPStatus",
         global = true
      )
      public static KahluaTable getMPStatus() {
         return MPStatistics.getLuaStatus();
      }

      @LuaMethod(
         name = "getMaxPlayers",
         global = true
      )
      public static Double getMaxPlayers() {
         return (double)GameClient.connection.maxPlayers;
      }

      @LuaMethod(
         name = "callLua",
         global = true
      )
      public static void callLua(String var0, Object var1) {
         LuaManager.caller.pcall(LuaManager.thread, LuaManager.env.rawget(var0), var1);
      }

      @LuaMethod(
         name = "callLuaReturn",
         global = true
      )
      public static ArrayList<Object> callLuaReturn(String var0, ArrayList<Object> var1) {
         if (var1 == null) {
            var1 = new ArrayList();
         }

         ArrayList var2 = new ArrayList();
         LuaReturn var3 = LuaManager.caller.protectedCall(LuaManager.thread, LuaManager.env.rawget(var0), new Object[]{var1});
         if (var3 instanceof LuaSuccess var4) {
            var2.addAll(var4);
         }

         return var2;
      }

      @LuaMethod(
         name = "callLuaBool",
         global = true
      )
      public static Boolean callLuaBool(String var0, Object var1) {
         return LuaManager.caller.pcallBoolean(LuaManager.thread, LuaManager.env.rawget(var0), var1, (Object)null);
      }

      @LuaMethod(
         name = "getWorld",
         global = true
      )
      public static IsoWorld getWorld() {
         return IsoWorld.instance;
      }

      @LuaMethod(
         name = "getCell",
         global = true
      )
      public static IsoCell getCell() {
         return IsoWorld.instance.getCell();
      }

      @LuaMethod(
         name = "getCellSizeInChunks",
         global = true
      )
      public static Double getCellSizeInChunks() {
         return BoxedStaticValues.toDouble((double)IsoCell.CellSizeInChunks);
      }

      @LuaMethod(
         name = "getCellSizeInSquares",
         global = true
      )
      public static Double getCellSizeInSquares() {
         return BoxedStaticValues.toDouble((double)IsoCell.CellSizeInSquares);
      }

      @LuaMethod(
         name = "getChunkSizeInSquares",
         global = true
      )
      public static Double getChunkSizeInSquares() {
         return BoxedStaticValues.toDouble(8.0);
      }

      @LuaMethod(
         name = "getMinimumWorldLevel",
         global = true
      )
      public static Double getMinimumWorldLevel() {
         return BoxedStaticValues.toDouble(-32.0);
      }

      @LuaMethod(
         name = "getMaximumWorldLevel",
         global = true
      )
      public static Double getMaximumWorldLevel() {
         return BoxedStaticValues.toDouble(31.0);
      }

      @LuaMethod(
         name = "getSandboxOptions",
         global = true
      )
      public static SandboxOptions getSandboxOptions() {
         return SandboxOptions.instance;
      }

      @LuaMethod(
         name = "getFileOutput",
         global = true
      )
      public static DataOutputStream getFileOutput(String var0) {
         if (StringUtils.containsDoubleDot(var0)) {
            DebugLog.Lua.warn("relative paths not allowed");
            return null;
         } else {
            String var10000 = LuaManager.getLuaCacheDir();
            String var1 = var10000 + File.separator + var0;
            var1 = var1.replace("/", File.separator);
            var1 = var1.replace("\\", File.separator);
            String var2 = var1.substring(0, var1.lastIndexOf(File.separator));
            var2 = var2.replace("\\", "/");
            File var3 = new File(var2);
            if (!var3.exists()) {
               var3.mkdirs();
            }

            File var4 = new File(var1);

            try {
               outStream = new FileOutputStream(var4);
            } catch (FileNotFoundException var6) {
               Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var6);
            }

            DataOutputStream var5 = new DataOutputStream(outStream);
            return var5;
         }
      }

      @LuaMethod(
         name = "getLastStandPlayersDirectory",
         global = true
      )
      public static String getLastStandPlayersDirectory() {
         return "LastStand";
      }

      @LuaMethod(
         name = "getLastStandPlayerFileNames",
         global = true
      )
      public static List<String> getLastStandPlayerFileNames() throws IOException {
         ArrayList var0 = new ArrayList();
         String var10000 = LuaManager.getLuaCacheDir();
         String var1 = var10000 + File.separator + getLastStandPlayersDirectory();
         var1 = var1.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         File var2 = new File(var1);
         if (!var2.exists()) {
            var2.mkdir();
         }

         File[] var3 = var2.listFiles();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File var6 = var3[var5];
            if (!var6.isDirectory() && var6.getName().endsWith(".txt")) {
               String var10001 = getLastStandPlayersDirectory();
               var0.add(var10001 + File.separator + var6.getName());
            }
         }

         return var0;
      }

      /** @deprecated */
      @Deprecated
      @LuaMethod(
         name = "getAllSavedPlayers",
         global = true
      )
      public static List<BufferedReader> getAllSavedPlayers() throws IOException {
         ArrayList var0 = new ArrayList();
         String var10000 = LuaManager.getLuaCacheDir();
         String var1 = var10000 + File.separator + getLastStandPlayersDirectory();
         var1 = var1.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         File var2 = new File(var1);
         if (!var2.exists()) {
            var2.mkdir();
         }

         File[] var3 = var2.listFiles();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File var6 = var3[var5];
            var0.add(new BufferedReader(new FileReader(var6)));
         }

         return var0;
      }

      @LuaMethod(
         name = "getSandboxPresets",
         global = true
      )
      public static List<String> getSandboxPresets() throws IOException {
         ArrayList var0 = new ArrayList();
         String var1 = LuaManager.getSandboxCacheDir();
         File var2 = new File(var1);
         if (!var2.exists()) {
            var2.mkdir();
         }

         File[] var3 = var2.listFiles();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File var6 = var3[var5];
            if (var6.getName().endsWith(".cfg")) {
               var0.add(var6.getName().replace(".cfg", ""));
            }
         }

         Collections.sort(var0);
         return var0;
      }

      @LuaMethod(
         name = "deleteSandboxPreset",
         global = true
      )
      public static void deleteSandboxPreset(String var0) {
         if (StringUtils.containsDoubleDot(var0)) {
            DebugLog.Lua.warn("relative paths not allowed");
         } else {
            String var10000 = LuaManager.getSandboxCacheDir();
            String var1 = var10000 + File.separator + var0 + ".cfg";
            File var2 = new File(var1);
            if (var2.exists()) {
               var2.delete();
            }

         }
      }

      @LuaMethod(
         name = "getFileReader",
         global = true
      )
      public static BufferedReader getFileReader(String var0, boolean var1) throws IOException {
         if (StringUtils.containsDoubleDot(var0)) {
            DebugLog.Lua.warn("relative paths not allowed");
            return null;
         } else {
            String var10000 = LuaManager.getLuaCacheDir();
            String var2 = var10000 + File.separator + var0;
            var2 = var2.replace("/", File.separator);
            var2 = var2.replace("\\", File.separator);
            File var3 = new File(var2);
            if (!var3.exists() && var1) {
               var3.createNewFile();
            }

            if (var3.exists()) {
               BufferedReader var4 = null;

               try {
                  FileInputStream var5 = new FileInputStream(var3);
                  InputStreamReader var6 = new InputStreamReader(var5, StandardCharsets.UTF_8);
                  var4 = new BufferedReader(var6);
               } catch (IOException var7) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var7);
               }

               return var4;
            } else {
               return null;
            }
         }
      }

      @LuaMethod(
         name = "getModFileReader",
         global = true
      )
      public static BufferedReader getModFileReader(String var0, String var1, boolean var2) throws IOException {
         if (!var1.isEmpty() && !StringUtils.containsDoubleDot(var1) && !(new File(var1)).isAbsolute()) {
            String var10000 = ZomboidFileSystem.instance.getCacheDir();
            String var3 = var10000 + File.separator + "mods" + File.separator + var1;
            if (var0 != null) {
               ChooseGameInfo.Mod var4 = ChooseGameInfo.getModDetails(var0);
               if (var4 == null) {
                  return null;
               }

               var10000 = var4.getCommonDir();
               var3 = var10000 + File.separator + var1;
            }

            var3 = var3.replace("/", File.separator);
            var3 = var3.replace("\\", File.separator);
            File var9 = new File(var3);
            if (!var9.exists() && var2) {
               String var5 = var3.substring(0, var3.lastIndexOf(File.separator));
               File var6 = new File(var5);
               if (!var6.exists()) {
                  var6.mkdirs();
               }

               var9.createNewFile();
            }

            if (var9.exists()) {
               BufferedReader var10 = null;

               try {
                  FileInputStream var11 = new FileInputStream(var9);
                  InputStreamReader var7 = new InputStreamReader(var11, StandardCharsets.UTF_8);
                  var10 = new BufferedReader(var7);
               } catch (IOException var8) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var8);
               }

               return var10;
            } else {
               return null;
            }
         } else {
            return null;
         }
      }

      public static void refreshAnimSets(boolean var0) {
         try {
            Iterator var1;
            if (var0) {
               AnimationSet.Reset();
               var1 = AnimNodeAssetManager.instance.getAssetTable().values().iterator();

               while(var1.hasNext()) {
                  Asset var2 = (Asset)var1.next();
                  AnimNodeAssetManager.instance.reload(var2);
               }
            }

            AnimationSet.GetAnimationSet("player", true);
            AnimationSet.GetAnimationSet("player-vehicle", true);
            AnimationSet.GetAnimationSet("zombie", true);
            AnimationSet.GetAnimationSet("zombie-crawler", true);

            for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
               IsoPlayer var6 = IsoPlayer.players[var5];
               if (var6 != null) {
                  var6.advancedAnimator.OnAnimDataChanged(var0);
               }
            }

            if (IsoWorld.instance.CurrentCell != null) {
               var1 = IsoWorld.instance.CurrentCell.getZombieList().iterator();

               while(var1.hasNext()) {
                  IsoZombie var7 = (IsoZombie)var1.next();
                  var7.advancedAnimator.OnAnimDataChanged(var0);
               }
            }

            var1 = IsoWorld.instance.CurrentCell.getObjectList().iterator();

            while(var1.hasNext()) {
               IsoMovingObject var8 = (IsoMovingObject)var1.next();
               IsoAnimal var3 = (IsoAnimal)Type.tryCastTo(var8, IsoAnimal.class);
               if (var3 != null) {
                  var3.advancedAnimator.OnAnimDataChanged(var0);
               }
            }
         } catch (Exception var4) {
            ExceptionLogger.logException(var4);
         }

      }

      public static void reloadActionGroups() {
         try {
            ActionGroup.reloadAll();
         } catch (Exception var1) {
         }

      }

      @LuaMethod(
         name = "getModFileWriter",
         global = true
      )
      public static LuaFileWriter getModFileWriter(String var0, String var1, boolean var2, boolean var3) {
         if (!var1.isEmpty() && !StringUtils.containsDoubleDot(var1) && !(new File(var1)).isAbsolute()) {
            ChooseGameInfo.Mod var4 = ChooseGameInfo.getModDetails(var0);
            if (var4 == null) {
               return null;
            } else {
               String var10000 = var4.getCommonDir();
               String var5 = var10000 + File.separator + var1;
               var5 = var5.replace("/", File.separator);
               var5 = var5.replace("\\", File.separator);
               String var6 = var5.substring(0, var5.lastIndexOf(File.separator));
               File var7 = new File(var6);
               if (!var7.exists()) {
                  var7.mkdirs();
               }

               File var8 = new File(var5);
               if (!var8.exists() && var2) {
                  try {
                     var8.createNewFile();
                  } catch (IOException var13) {
                     Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var13);
                  }
               }

               PrintWriter var9 = null;

               try {
                  FileOutputStream var10 = new FileOutputStream(var8, var3);
                  OutputStreamWriter var11 = new OutputStreamWriter(var10, StandardCharsets.UTF_8);
                  var9 = new PrintWriter(var11);
               } catch (IOException var12) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var12);
               }

               return new LuaFileWriter(var9);
            }
         } else {
            return null;
         }
      }

      @LuaMethod(
         name = "updateFire",
         global = true
      )
      public static void updateFire() {
         IsoFireManager.Update();
      }

      @LuaMethod(
         name = "deletePlayerFromDatabase",
         global = true
      )
      public static void deletePlayerFromDatabase(String var0, String var1, String var2) {
         try {
            ServerWorldDatabase.instance.connect();
            ServerWorldDatabase.instance.removeUser(var1, var2);
            ServerWorldDatabase.instance.close();
            PlayerDBHelper.removePlayer(ZomboidFileSystem.instance.getSaveDir() + File.separator + var0, var1, var2);
         } catch (SQLException var4) {
            var4.printStackTrace();
         }

      }

      @LuaMethod(
         name = "checkPlayerExistsInDatabase",
         global = true
      )
      public static boolean checkPlayerExistsInDatabase(String var0, String var1, String var2) {
         boolean var3 = false;

         try {
            ServerWorldDatabase.instance.connect();
            var3 = ServerWorldDatabase.instance.containsUser(var1, var2);
            ServerWorldDatabase.instance.close();
            var3 |= PlayerDBHelper.containsNetworkPlayer(ZomboidFileSystem.instance.getSaveDir() + File.separator + var0, var1, var2);
         } catch (SQLException var5) {
            var5.printStackTrace();
         }

         return var3;
      }

      @LuaMethod(
         name = "deletePlayerSave",
         global = true
      )
      public static void deletePlayerSave(String var0) {
         String var10000 = LuaManager.getLuaCacheDir();
         String var1 = var10000 + File.separator + "Players" + File.separator + "player" + var0 + ".txt";
         var1 = var1.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         File var2 = new File(var1);
         var2.delete();
      }

      @LuaMethod(
         name = "getControllerCount",
         global = true
      )
      public static int getControllerCount() {
         return GameWindow.GameInput.getControllerCount();
      }

      @LuaMethod(
         name = "isControllerConnected",
         global = true
      )
      public static boolean isControllerConnected(int var0) {
         if (var0 >= 0 && var0 <= GameWindow.GameInput.getControllerCount()) {
            return GameWindow.GameInput.getController(var0) != null;
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "getControllerGUID",
         global = true
      )
      public static String getControllerGUID(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 != null ? var1.getGUID() : "???";
         } else {
            return "???";
         }
      }

      @LuaMethod(
         name = "getControllerName",
         global = true
      )
      public static String getControllerName(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 != null ? var1.getGamepadName() : "???";
         } else {
            return "???";
         }
      }

      @LuaMethod(
         name = "getControllerAxisCount",
         global = true
      )
      public static int getControllerAxisCount(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 == null ? 0 : var1.getAxisCount();
         } else {
            return 0;
         }
      }

      @LuaMethod(
         name = "getControllerAxisValue",
         global = true
      )
      public static float getControllerAxisValue(int var0, int var1) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var2 = GameWindow.GameInput.getController(var0);
            if (var2 == null) {
               return 0.0F;
            } else {
               return var1 >= 0 && var1 < var2.getAxisCount() ? var2.getAxisValue(var1) : 0.0F;
            }
         } else {
            return 0.0F;
         }
      }

      @LuaMethod(
         name = "getControllerDeadZone",
         global = true
      )
      public static float getControllerDeadZone(int var0, int var1) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            return var1 >= 0 && var1 < GameWindow.GameInput.getAxisCount(var0) ? JoypadManager.instance.getDeadZone(var0, var1) : 0.0F;
         } else {
            return 0.0F;
         }
      }

      @LuaMethod(
         name = "setControllerDeadZone",
         global = true
      )
      public static void setControllerDeadZone(int var0, int var1, float var2) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            if (var1 >= 0 && var1 < GameWindow.GameInput.getAxisCount(var0)) {
               JoypadManager.instance.setDeadZone(var0, var1, var2);
            }
         }
      }

      @LuaMethod(
         name = "saveControllerSettings",
         global = true
      )
      public static void saveControllerSettings(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            JoypadManager.instance.saveControllerSettings(var0);
         }
      }

      @LuaMethod(
         name = "getControllerButtonCount",
         global = true
      )
      public static int getControllerButtonCount(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 == null ? 0 : var1.getButtonCount();
         } else {
            return 0;
         }
      }

      @LuaMethod(
         name = "getControllerPovX",
         global = true
      )
      public static float getControllerPovX(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 == null ? 0.0F : var1.getPovX();
         } else {
            return 0.0F;
         }
      }

      @LuaMethod(
         name = "getControllerPovY",
         global = true
      )
      public static float getControllerPovY(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 == null ? 0.0F : var1.getPovY();
         } else {
            return 0.0F;
         }
      }

      @LuaMethod(
         name = "reloadControllerConfigFiles",
         global = true
      )
      public static void reloadControllerConfigFiles() {
         JoypadManager.instance.reloadControllerFiles();
      }

      @LuaMethod(
         name = "isJoypadPressed",
         global = true
      )
      public static boolean isJoypadPressed(int var0, int var1) {
         return GameWindow.GameInput.isButtonPressedD(var1, var0);
      }

      @LuaMethod(
         name = "isJoypadDown",
         global = true
      )
      public static boolean isJoypadDown(int var0) {
         return JoypadManager.instance.isDownPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadLTPressed",
         global = true
      )
      public static boolean isJoypadLTPressed(int var0) {
         return JoypadManager.instance.isLTPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadRTPressed",
         global = true
      )
      public static boolean isJoypadRTPressed(int var0) {
         return JoypadManager.instance.isRTPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadLeftStickButtonPressed",
         global = true
      )
      public static boolean isJoypadLeftStickButtonPressed(int var0) {
         return JoypadManager.instance.isL3Pressed(var0);
      }

      @LuaMethod(
         name = "isJoypadRightStickButtonPressed",
         global = true
      )
      public static boolean isJoypadRightStickButtonPressed(int var0) {
         return JoypadManager.instance.isR3Pressed(var0);
      }

      @LuaMethod(
         name = "getJoypadAimingAxisX",
         global = true
      )
      public static float getJoypadAimingAxisX(int var0) {
         return JoypadManager.instance.getAimingAxisX(var0);
      }

      @LuaMethod(
         name = "getJoypadAimingAxisY",
         global = true
      )
      public static float getJoypadAimingAxisY(int var0) {
         return JoypadManager.instance.getAimingAxisY(var0);
      }

      @LuaMethod(
         name = "getJoypadMovementAxisX",
         global = true
      )
      public static float getJoypadMovementAxisX(int var0) {
         return JoypadManager.instance.getMovementAxisX(var0);
      }

      @LuaMethod(
         name = "getJoypadMovementAxisY",
         global = true
      )
      public static float getJoypadMovementAxisY(int var0) {
         return JoypadManager.instance.getMovementAxisY(var0);
      }

      @LuaMethod(
         name = "getJoypadAButton",
         global = true
      )
      public static int getJoypadAButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getAButton() : -1;
      }

      @LuaMethod(
         name = "getJoypadBButton",
         global = true
      )
      public static int getJoypadBButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getBButton() : -1;
      }

      @LuaMethod(
         name = "getJoypadXButton",
         global = true
      )
      public static int getJoypadXButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getXButton() : -1;
      }

      @LuaMethod(
         name = "getJoypadYButton",
         global = true
      )
      public static int getJoypadYButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getYButton() : -1;
      }

      @LuaMethod(
         name = "getJoypadLBumper",
         global = true
      )
      public static int getJoypadLBumper(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getLBumper() : -1;
      }

      @LuaMethod(
         name = "getJoypadRBumper",
         global = true
      )
      public static int getJoypadRBumper(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getRBumper() : -1;
      }

      @LuaMethod(
         name = "getJoypadBackButton",
         global = true
      )
      public static int getJoypadBackButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getBackButton() : -1;
      }

      @LuaMethod(
         name = "getJoypadStartButton",
         global = true
      )
      public static int getJoypadStartButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getStartButton() : -1;
      }

      @LuaMethod(
         name = "getJoypadLeftStickButton",
         global = true
      )
      public static int getJoypadLeftStickButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getL3() : -1;
      }

      @LuaMethod(
         name = "getJoypadRightStickButton",
         global = true
      )
      public static int getJoypadRightStickButton(int var0) {
         JoypadManager.Joypad var1 = JoypadManager.instance.getFromControllerID(var0);
         return var1 != null ? var1.getR3() : -1;
      }

      @LuaMethod(
         name = "wasMouseActiveMoreRecentlyThanJoypad",
         global = true
      )
      public static boolean wasMouseActiveMoreRecentlyThanJoypad() {
         if (IsoPlayer.players[0] == null) {
            JoypadManager.Joypad var1 = GameWindow.ActivatedJoyPad;
            if (var1 != null && !var1.isDisabled()) {
               return JoypadManager.instance.getLastActivity(var1.getID()) < Mouse.lastActivity;
            } else {
               return true;
            }
         } else {
            int var0 = IsoPlayer.players[0].getJoypadBind();
            if (var0 == -1) {
               return true;
            } else {
               return JoypadManager.instance.getLastActivity(var0) < Mouse.lastActivity;
            }
         }
      }

      @LuaMethod(
         name = "activateJoypadOnSteamDeck",
         global = true
      )
      public static void activateJoypadOnSteamDeck() {
         if (GameWindow.ActivatedJoyPad == null) {
            JoypadManager.instance.isAPressed(0);
            if (JoypadManager.instance.JoypadList.isEmpty()) {
               return;
            }

            JoypadManager.Joypad var0 = (JoypadManager.Joypad)JoypadManager.instance.JoypadList.get(0);
            GameWindow.ActivatedJoyPad = var0;
         }

         if (IsoPlayer.getInstance() != null) {
            LuaEventManager.triggerEvent("OnJoypadActivate", GameWindow.ActivatedJoyPad.getID());
         } else {
            LuaEventManager.triggerEvent("OnJoypadActivateUI", GameWindow.ActivatedJoyPad.getID());
         }

      }

      @LuaMethod(
         name = "reactivateJoypadAfterResetLua",
         global = true
      )
      public static boolean reactivateJoypadAfterResetLua() {
         if (GameWindow.ActivatedJoyPad != null) {
            LuaEventManager.triggerEvent("OnJoypadActivateUI", GameWindow.ActivatedJoyPad.getID());
            return true;
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "isJoypadConnected",
         global = true
      )
      public static boolean isJoypadConnected(int var0) {
         return JoypadManager.instance.isJoypadConnected(var0);
      }

      private static void addPlayerToWorld(int var0, IsoPlayer var1, boolean var2) {
         if (IsoPlayer.players[var0] != null) {
            IsoPlayer.players[var0].getEmitter().stopAll();
            IsoPlayer.players[var0].getEmitter().unregister();
            IsoPlayer.players[var0].updateUsername();
            IsoPlayer.players[var0].setSceneCulled(true);
            IsoPlayer.players[var0] = null;
         }

         var1.PlayerIndex = var0;
         if (GameClient.bClient && var0 != 0 && var1.serverPlayerIndex != 1) {
            ClientPlayerDB.getInstance().forgetPlayer(var1.serverPlayerIndex);
         }

         if (GameClient.bClient && var0 != 0 && var1.serverPlayerIndex == 1) {
            var1.serverPlayerIndex = ClientPlayerDB.getInstance().getNextServerPlayerIndex();
         }

         if (var0 == 0) {
            var1.sqlID = 1;
         }

         if (var2) {
            var1.applyTraits(IsoWorld.instance.getLuaTraits());
            ProfessionFactory.Profession var3 = ProfessionFactory.getProfession(var1.getDescriptor().getProfession());
            Iterator var4;
            String var5;
            if (var3 != null && !var3.getFreeRecipes().isEmpty()) {
               var4 = var3.getFreeRecipes().iterator();

               while(var4.hasNext()) {
                  var5 = (String)var4.next();
                  var1.getKnownRecipes().add(var5);
               }
            }

            var4 = IsoWorld.instance.getLuaTraits().iterator();

            label58:
            while(true) {
               TraitFactory.Trait var6;
               do {
                  do {
                     if (!var4.hasNext()) {
                        var1.setDir(IsoDirections.SE);
                        LuaEventManager.triggerEvent("OnNewGame", var1, var1.getCurrentSquare());
                        break label58;
                     }

                     var5 = (String)var4.next();
                     var6 = TraitFactory.getTrait(var5);
                  } while(var6 == null);
               } while(var6.getFreeRecipes().isEmpty());

               Iterator var7 = var6.getFreeRecipes().iterator();

               while(var7.hasNext()) {
                  String var8 = (String)var7.next();
                  var1.getKnownRecipes().add(var8);
               }
            }
         }

         IsoPlayer.numPlayers = Math.max(IsoPlayer.numPlayers, var0 + 1);
         IsoWorld.instance.AddCoopPlayers.add(new AddCoopPlayer(var1, var2));
         if (var0 == 0) {
            IsoPlayer.setInstance(var1);
         }

      }

      @LuaMethod(
         name = "toInt",
         global = true
      )
      public static int toInt(double var0) {
         return PZMath.fastfloor(var0);
      }

      @LuaMethod(
         name = "getClientUsername",
         global = true
      )
      public static String getClientUsername() {
         return GameClient.bClient ? GameClient.username : null;
      }

      @LuaMethod(
         name = "setPlayerJoypad",
         global = true
      )
      public static void setPlayerJoypad(int var0, int var1, IsoPlayer var2, String var3) {
         if (IsoPlayer.players[var0] == null || IsoPlayer.players[var0].isDead()) {
            boolean var4 = var2 == null;
            if (var2 == null) {
               IsoPlayer var5 = IsoPlayer.getInstance();
               IsoWorld var6 = IsoWorld.instance;
               int var7 = var6.getLuaPosX();
               int var8 = var6.getLuaPosY();
               int var9 = var6.getLuaPosZ();
               DebugLog.Lua.debugln("coop player spawning at " + var7 + "," + var8 + "," + var9);
               var2 = new IsoPlayer(var6.CurrentCell, GameClient.bClient ? null : var6.getLuaPlayerDesc(), var7, var8, var9);
               IsoPlayer.setInstance(var5);
               var6.CurrentCell.getAddList().remove(var2);
               var6.CurrentCell.getObjectList().remove(var2);
               var2.SaveFileName = IsoPlayer.getUniqueFileName();
            }

            if (GameClient.bClient) {
               if (var3 != null) {
                  assert var0 != 0;

                  var2.username = var3;
                  var2.getModData().rawset("username", var3);
               } else {
                  assert var0 == 0;

                  var2.username = GameClient.username;
               }
            }

            addPlayerToWorld(var0, var2, var4);
         }

         var2.JoypadBind = var1;
         JoypadManager.instance.assignJoypad(var1, var0);
      }

      @LuaMethod(
         name = "setPlayerMouse",
         global = true
      )
      public static void setPlayerMouse(IsoPlayer var0) {
         byte var1 = 0;
         boolean var2 = var0 == null;
         if (var0 == null) {
            IsoPlayer var3 = IsoPlayer.getInstance();
            IsoWorld var4 = IsoWorld.instance;
            int var5 = var4.getLuaPosX();
            int var6 = var4.getLuaPosY();
            int var7 = var4.getLuaPosZ();
            DebugLog.Lua.debugln("coop player spawning at " + var5 + "," + var6 + "," + var7);
            var0 = new IsoPlayer(var4.CurrentCell, GameClient.bClient ? null : var4.getLuaPlayerDesc(), var5, var6, var7);
            IsoPlayer.setInstance(var3);
            var4.CurrentCell.getAddList().remove(var0);
            var4.CurrentCell.getObjectList().remove(var0);
            var0.SaveFileName = null;
         }

         if (GameClient.bClient) {
            var0.username = GameClient.username;
         }

         addPlayerToWorld(var1, var0, var2);
      }

      @LuaMethod(
         name = "revertToKeyboardAndMouse",
         global = true
      )
      public static void revertToKeyboardAndMouse() {
         JoypadManager.instance.revertToKeyboardAndMouse();
      }

      @LuaMethod(
         name = "isJoypadUp",
         global = true
      )
      public static boolean isJoypadUp(int var0) {
         return JoypadManager.instance.isUpPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadLeft",
         global = true
      )
      public static boolean isJoypadLeft(int var0) {
         return JoypadManager.instance.isLeftPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadRight",
         global = true
      )
      public static boolean isJoypadRight(int var0) {
         return JoypadManager.instance.isRightPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadLBPressed",
         global = true
      )
      public static boolean isJoypadLBPressed(int var0) {
         return JoypadManager.instance.isLBPressed(var0);
      }

      @LuaMethod(
         name = "isJoypadRBPressed",
         global = true
      )
      public static boolean isJoypadRBPressed(int var0) {
         return JoypadManager.instance.isRBPressed(var0);
      }

      @LuaMethod(
         name = "getButtonCount",
         global = true
      )
      public static int getButtonCount(int var0) {
         if (var0 >= 0 && var0 < GameWindow.GameInput.getControllerCount()) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            return var1 == null ? 0 : var1.getButtonCount();
         } else {
            return 0;
         }
      }

      @LuaMethod(
         name = "setDebugToggleControllerPluggedIn",
         global = true
      )
      public static void setDebugToggleControllerPluggedIn(int var0) {
         Controllers.setDebugToggleControllerPluggedIn(var0);
      }

      @LuaMethod(
         name = "getFileWriter",
         global = true
      )
      public static LuaFileWriter getFileWriter(String var0, boolean var1, boolean var2) {
         if (StringUtils.containsDoubleDot(var0)) {
            DebugLog.Lua.warn("relative paths not allowed");
            return null;
         } else {
            String var10000 = LuaManager.getLuaCacheDir();
            String var3 = var10000 + File.separator + var0;
            var3 = var3.replace("/", File.separator);
            var3 = var3.replace("\\", File.separator);
            String var4 = var3.substring(0, var3.lastIndexOf(File.separator));
            var4 = var4.replace("\\", "/");
            File var5 = new File(var4);
            if (!var5.exists()) {
               var5.mkdirs();
            }

            File var6 = new File(var3);
            if (!var6.exists() && var1) {
               try {
                  var6.createNewFile();
               } catch (IOException var11) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var11);
               }
            }

            PrintWriter var7 = null;

            try {
               FileOutputStream var8 = new FileOutputStream(var6, var2);
               OutputStreamWriter var9 = new OutputStreamWriter(var8, StandardCharsets.UTF_8);
               var7 = new PrintWriter(var9);
            } catch (IOException var10) {
               Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var10);
            }

            return new LuaFileWriter(var7);
         }
      }

      @LuaMethod(
         name = "getSandboxFileWriter",
         global = true
      )
      public static LuaFileWriter getSandboxFileWriter(String var0, boolean var1, boolean var2) {
         if (StringUtils.containsDoubleDot(var0)) {
            DebugLog.Lua.warn("relative paths not allowed");
            return null;
         } else {
            String var10000 = LuaManager.getSandboxCacheDir();
            String var3 = var10000 + File.separator + var0;
            var3 = var3.replace("/", File.separator);
            var3 = var3.replace("\\", File.separator);
            String var4 = var3.substring(0, var3.lastIndexOf(File.separator));
            var4 = var4.replace("\\", "/");
            File var5 = new File(var4);
            if (!var5.exists()) {
               var5.mkdirs();
            }

            File var6 = new File(var3);
            if (!var6.exists() && var1) {
               try {
                  var6.createNewFile();
               } catch (IOException var11) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var11);
               }
            }

            PrintWriter var7 = null;

            try {
               FileOutputStream var8 = new FileOutputStream(var6, var2);
               OutputStreamWriter var9 = new OutputStreamWriter(var8, StandardCharsets.UTF_8);
               var7 = new PrintWriter(var9);
            } catch (IOException var10) {
               Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var10);
            }

            return new LuaFileWriter(var7);
         }
      }

      @LuaMethod(
         name = "createStory",
         global = true
      )
      public static void createStory(String var0) {
         Core.GameMode = var0;
         String var1 = ZomboidFileSystem.instance.getGameModeCacheDir();
         var1 = var1.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         int var2 = 1;
         File var3 = null;
         boolean var4 = false;

         while(!var4) {
            var3 = new File(var1 + File.separator + "Game" + var2);
            if (!var3.exists()) {
               var4 = true;
            } else {
               ++var2;
            }
         }

         Core.GameSaveWorld = "newstory";
      }

      @LuaMethod(
         name = "createWorld",
         global = true
      )
      public static void createWorld(String var0) {
         if (var0 == null || var0.isEmpty()) {
            var0 = "blah";
         }

         var0 = sanitizeWorldName(var0);
         String var10000 = ZomboidFileSystem.instance.getGameModeCacheDir();
         String var1 = var10000 + File.separator + var0 + File.separator;
         var1 = var1.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         String var2 = var1.substring(0, var1.lastIndexOf(File.separator));
         var2 = var2.replace("\\", "/");
         File var3 = new File(var2);
         if (!var3.exists() && !Core.getInstance().isNoSave()) {
            var3.mkdirs();
         }

         if (!Core.getInstance().isNoSave()) {
            SavefileNaming.ensureSubdirectoriesExist(var2);
         }

         Core.GameSaveWorld = var0;
      }

      @LuaMethod(
         name = "sanitizeWorldName",
         global = true
      )
      public static String sanitizeWorldName(String var0) {
         return var0.replace(" ", "_").replace("/", "").replace("\\", "").replace("?", "").replace("*", "").replace("<", "").replace(">", "").replace(":", "").replace("|", "").trim();
      }

      @LuaMethod(
         name = "forceChangeState",
         global = true
      )
      public static void forceChangeState(GameState var0) {
         GameWindow.states.forceNextState(var0);
      }

      @LuaMethod(
         name = "endFileOutput",
         global = true
      )
      public static void endFileOutput() {
         if (outStream != null) {
            try {
               outStream.close();
            } catch (IOException var1) {
               Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var1);
            }
         }

         outStream = null;
      }

      @LuaMethod(
         name = "getFileInput",
         global = true
      )
      public static DataInputStream getFileInput(String var0) throws IOException {
         if (StringUtils.containsDoubleDot(var0)) {
            DebugLog.Lua.warn("relative paths not allowed");
            return null;
         } else {
            String var10000 = LuaManager.getLuaCacheDir();
            String var1 = var10000 + File.separator + var0;
            var1 = var1.replace("/", File.separator);
            var1 = var1.replace("\\", File.separator);
            File var2 = new File(var1);
            if (var2.exists()) {
               try {
                  inStream = new FileInputStream(var2);
               } catch (FileNotFoundException var4) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var4);
               }

               DataInputStream var3 = new DataInputStream(inStream);
               return var3;
            } else {
               return null;
            }
         }
      }

      @LuaMethod(
         name = "getGameFilesInput",
         global = true
      )
      public static DataInputStream getGameFilesInput(String var0) {
         String var1 = var0.replace("/", File.separator);
         var1 = var1.replace("\\", File.separator);
         if (!ZomboidFileSystem.instance.isKnownFile(var1)) {
            return null;
         } else {
            File var2 = new File(ZomboidFileSystem.instance.getString(var1));
            if (var2.exists()) {
               try {
                  inStream = new FileInputStream(var2);
               } catch (FileNotFoundException var4) {
                  Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var4);
               }

               DataInputStream var3 = new DataInputStream(inStream);
               return var3;
            } else {
               return null;
            }
         }
      }

      @LuaMethod(
         name = "getGameFilesTextInput",
         global = true
      )
      public static BufferedReader getGameFilesTextInput(String var0) {
         if (!Core.getInstance().getDebug()) {
            return null;
         } else {
            String var1 = var0.replace("/", File.separator);
            var1 = var1.replace("\\", File.separator);
            if (!ZomboidFileSystem.instance.isKnownFile(var1)) {
               return null;
            } else {
               File var2 = new File(ZomboidFileSystem.instance.getString(var1));
               if (var2.exists()) {
                  try {
                     inFileReader = new FileReader(var0);
                     inBufferedReader = new BufferedReader(inFileReader);
                     return inBufferedReader;
                  } catch (FileNotFoundException var4) {
                     Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var4);
                  }
               }

               return null;
            }
         }
      }

      @LuaMethod(
         name = "endTextFileInput",
         global = true
      )
      public static void endTextFileInput() {
         if (inBufferedReader != null) {
            try {
               inBufferedReader.close();
               inFileReader.close();
            } catch (IOException var1) {
               Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var1);
            }
         }

         inBufferedReader = null;
         inFileReader = null;
      }

      @LuaMethod(
         name = "endFileInput",
         global = true
      )
      public static void endFileInput() {
         if (inStream != null) {
            try {
               inStream.close();
            } catch (IOException var1) {
               Logger.getLogger(LuaManager.class.getName()).log(Level.SEVERE, (String)null, var1);
            }
         }

         inStream = null;
      }

      @LuaMethod(
         name = "getLineNumber",
         global = true
      )
      public static int getLineNumber(LuaCallFrame var0) {
         if (var0.closure == null) {
            return 0;
         } else {
            int var1 = var0.pc;
            if (var1 < 0) {
               var1 = 0;
            }

            if (var1 >= var0.closure.prototype.lines.length) {
               var1 = var0.closure.prototype.lines.length - 1;
            }

            return var0.closure.prototype.lines[var1];
         }
      }

      @LuaMethod(
         name = "ZombRand",
         global = true
      )
      public static double ZombRand(double var0) {
         if (var0 == 0.0) {
            return 0.0;
         } else {
            return var0 < 0.0 ? (double)(-RandLua.INSTANCE.Next(-((long)var0))) : (double)RandLua.INSTANCE.Next((long)var0);
         }
      }

      @LuaMethod(
         name = "ZombRandBetween",
         global = true
      )
      public static double ZombRandBetween(double var0, double var2) {
         return (double)RandLua.INSTANCE.Next((long)var0, (long)var2);
      }

      @LuaMethod(
         name = "ZombRand",
         global = true
      )
      public static double ZombRand(double var0, double var2) {
         return (double)RandLua.INSTANCE.Next((int)var0, (int)var2);
      }

      @LuaMethod(
         name = "ZombRandFloat",
         global = true
      )
      public static float ZombRandFloat(float var0, float var1) {
         return RandLua.INSTANCE.Next(var0, var1);
      }

      @LuaMethod(
         name = "getShortenedFilename",
         global = true
      )
      public static String getShortenedFilename(String var0) {
         return var0.substring(var0.indexOf("lua/") + 4);
      }

      @LuaMethod(
         name = "isKeyDown",
         global = true
      )
      public static boolean isKeyDown(int var0) {
         return GameKeyboard.isKeyDown(var0);
      }

      @LuaMethod(
         name = "isKeyDown",
         global = true
      )
      public static boolean isKeyDown(String var0) {
         return GameKeyboard.isKeyDown(var0);
      }

      @LuaMethod(
         name = "wasKeyDown",
         global = true
      )
      public static boolean wasKeyDown(int var0) {
         return GameKeyboard.wasKeyDown(var0);
      }

      @LuaMethod(
         name = "wasKeyDown",
         global = true
      )
      public static boolean wasKeyDown(String var0) {
         return GameKeyboard.wasKeyDown(var0);
      }

      @LuaMethod(
         name = "isKeyPressed",
         global = true
      )
      public static boolean isKeyPressed(int var0) {
         return GameKeyboard.isKeyPressed(var0);
      }

      @LuaMethod(
         name = "isKeyPressed",
         global = true
      )
      public static boolean isKeyPressed(String var0) {
         return GameKeyboard.isKeyPressed(var0);
      }

      @LuaMethod(
         name = "getBaseSoundBank",
         global = true
      )
      public static BaseSoundBank getBaseSoundBank() {
         return BaseSoundBank.instance;
      }

      @LuaMethod(
         name = "getFMODSoundBank",
         global = true
      )
      public static BaseSoundBank getFMODSoundBank() {
         return FMODSoundBank.instance;
      }

      @LuaMethod(
         name = "isSoundPlaying",
         global = true
      )
      public static boolean isSoundPlaying(Object var0) {
         return var0 instanceof Double ? FMODManager.instance.isPlaying(((Double)var0).longValue()) : false;
      }

      @LuaMethod(
         name = "stopSound",
         global = true
      )
      public static void stopSound(long var0) {
         FMODManager.instance.stopSound(var0);
      }

      @LuaMethod(
         name = "isShiftKeyDown",
         global = true
      )
      public static boolean isShiftKeyDown() {
         return GameKeyboard.isKeyDown(42) || GameKeyboard.isKeyDown(54);
      }

      @LuaMethod(
         name = "isCtrlKeyDown",
         global = true
      )
      public static boolean isCtrlKeyDown() {
         return GameKeyboard.isKeyDown(29) || GameKeyboard.isKeyDown(157);
      }

      @LuaMethod(
         name = "isAltKeyDown",
         global = true
      )
      public static boolean isAltKeyDown() {
         return GameKeyboard.isKeyDown(56) || GameKeyboard.isKeyDown(184);
      }

      @LuaMethod(
         name = "setZoomLevels",
         global = true
      )
      public static void setZoomLevels(Double... var0) {
         Core.getInstance().OffscreenBuffer.setZoomLevels(var0);
      }

      @LuaMethod(
         name = "getCore",
         global = true
      )
      public static Core getCore() {
         return Core.getInstance();
      }

      @LuaMethod(
         name = "getGameVersion",
         global = true
      )
      public static String getGameVersion() {
         return Core.getInstance().getGameVersion().toString();
      }

      @LuaMethod(
         name = "getBreakModGameVersion",
         global = true
      )
      public static GameVersion getBreakModGameVersion() {
         return Core.getInstance().getBreakModGameVersion();
      }

      @LuaMethod(
         name = "getSquare",
         global = true
      )
      public static IsoGridSquare getSquare(double var0, double var2, double var4) {
         return IsoCell.getInstance().getGridSquare(var0, var2, var4);
      }

      @LuaMethod(
         name = "getDebugOptions",
         global = true
      )
      public static DebugOptions getDebugOptions() {
         return DebugOptions.instance;
      }

      @LuaMethod(
         name = "setShowPausedMessage",
         global = true
      )
      public static void setShowPausedMessage(boolean var0) {
         DebugType.ExitDebug.debugln("setShowPausedMessage 1");
         UIManager.setShowPausedMessage(var0);
         DebugType.ExitDebug.debugln("setShowPausedMessage 2");
      }

      @LuaMethod(
         name = "getFilenameOfCallframe",
         global = true
      )
      public static String getFilenameOfCallframe(LuaCallFrame var0) {
         return var0.closure == null ? null : var0.closure.prototype.filename;
      }

      @LuaMethod(
         name = "getFilenameOfClosure",
         global = true
      )
      public static String getFilenameOfClosure(LuaClosure var0) {
         return var0 == null ? null : var0.prototype.filename;
      }

      @LuaMethod(
         name = "getFirstLineOfClosure",
         global = true
      )
      public static int getFirstLineOfClosure(LuaClosure var0) {
         return var0 == null ? 0 : var0.prototype.lines[0];
      }

      @LuaMethod(
         name = "getLocalVarCount",
         global = true
      )
      public static int getLocalVarCount(Coroutine var0) {
         LuaCallFrame var1 = var0.currentCallFrame();
         return var1 == null ? 0 : var1.getLocalVarCount();
      }

      @LuaMethod(
         name = "getLocalVarCount",
         global = true
      )
      public static int getLocalVarCount(LuaCallFrame var0) {
         return var0.getLocalVarCount();
      }

      @LuaMethod(
         name = "isSystemLinux",
         global = true
      )
      public static boolean isSystemLinux() {
         return !isSystemMacOS() && !isSystemWindows();
      }

      @LuaMethod(
         name = "isSystemMacOS",
         global = true
      )
      public static boolean isSystemMacOS() {
         return System.getProperty("os.name").contains("OS X");
      }

      @LuaMethod(
         name = "isSystemWindows",
         global = true
      )
      public static boolean isSystemWindows() {
         return System.getProperty("os.name").startsWith("Win");
      }

      @LuaMethod(
         name = "isModActive",
         global = true
      )
      public static boolean isModActive(ChooseGameInfo.Mod var0) {
         String var1 = var0.getDir();
         if (!StringUtils.isNullOrWhitespace(var0.getId())) {
            var1 = var0.getId();
         }

         return ZomboidFileSystem.instance.getModIDs().contains(var1);
      }

      @LuaMethod(
         name = "openUrl",
         global = true
      )
      public static void openURl(String var0) {
         if (var0.startsWith("https://steamcommunity.com") || var0.startsWith("https://projectzomboid.com") || var0.startsWith("https://theindiestone.com")) {
            Desktop var1 = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (var1 != null && var1.isSupported(Action.BROWSE)) {
               try {
                  URI var2 = new URI(var0);
                  var1.browse(var2);
               } catch (Exception var3) {
                  ExceptionLogger.logException(var3);
               }

            } else {
               DesktopBrowser.openURL(var0);
            }
         }
      }

      @LuaMethod(
         name = "isDesktopOpenSupported",
         global = true
      )
      public static boolean isDesktopOpenSupported() {
         return !Desktop.isDesktopSupported() ? false : Desktop.getDesktop().isSupported(Action.OPEN);
      }

      @LuaMethod(
         name = "showFolderInDesktop",
         global = true
      )
      public static void showFolderInDesktop(String var0) {
         File var1 = new File(var0);
         if (var1.exists() && var1.isDirectory()) {
            Desktop var2 = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (var2 != null && var2.isSupported(Action.OPEN)) {
               try {
                  var2.open(var1);
               } catch (Exception var4) {
                  ExceptionLogger.logException(var4);
               }
            }

         }
      }

      @LuaMethod(
         name = "getActivatedMods",
         global = true
      )
      public static ArrayList<String> getActivatedMods() {
         return ZomboidFileSystem.instance.getModIDs();
      }

      @LuaMethod(
         name = "toggleModActive",
         global = true
      )
      public static void toggleModActive(ChooseGameInfo.Mod var0, boolean var1) {
         String var2 = var0.getDir();
         if (!StringUtils.isNullOrWhitespace(var0.getId())) {
            var2 = var0.getId();
         }

         ActiveMods.getById("default").setModActive(var2, var1);
      }

      @LuaMethod(
         name = "saveModsFile",
         global = true
      )
      public static void saveModsFile() {
         ZomboidFileSystem.instance.saveModsFile();
      }

      private static void deleteSavefileFilesMatching(File var0, String var1) {
         DirectoryStream.Filter var2 = (var1x) -> {
            return var1x.getFileName().toString().matches(var1);
         };

         try {
            DirectoryStream var3 = Files.newDirectoryStream(var0.toPath(), var2);

            try {
               Iterator var4 = var3.iterator();

               while(var4.hasNext()) {
                  Path var5 = (Path)var4.next();
                  System.out.println("DELETE " + var5);
                  Files.deleteIfExists(var5);
               }
            } catch (Throwable var7) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var3 != null) {
               var3.close();
            }
         } catch (Exception var8) {
            ExceptionLogger.logException(var8);
         }

      }

      @LuaMethod(
         name = "manipulateSavefile",
         global = true
      )
      public static void manipulateSavefile(String var0, String var1) {
         if (!StringUtils.isNullOrWhitespace(var0)) {
            if (!StringUtils.containsDoubleDot(var0)) {
               String var10000 = ZomboidFileSystem.instance.getSaveDir();
               String var2 = var10000 + File.separator + var0;
               File var3 = new File(var2);
               if (var3.exists() && var3.isDirectory()) {
                  switch (var1) {
                     case "DeleteAPopXYBin":
                        deleteSavefileFilesMatching(new File(var3, "apop"), "apop_-?[0-9]+_-?[0-9]+\\.bin");
                        break;
                     case "DeleteChunkDataXYBin":
                        deleteSavefileFilesMatching(new File(var3, "chunkdata"), "chunkdata_-?[0-9]+_-?[0-9]+\\.bin");
                        break;
                     case "DeleteEntityDataBin":
                        deleteSavefileFilesMatching(var3, "entity_data.bin");
                        break;
                     case "DeleteMapXYBin":
                        deleteSavefileFilesMatching(new File(var3, "map"), "map_-?[0-9]+_-?[0-9]+\\.bin");
                        break;
                     case "DeleteMapAnimalsBin":
                        deleteSavefileFilesMatching(var3, "map_animals\\.bin");
                        break;
                     case "DeleteMapBasementsBin":
                        deleteSavefileFilesMatching(var3, "map_basements\\.bin");
                        break;
                     case "DeleteMapMetaBin":
                        deleteSavefileFilesMatching(var3, "map_meta\\.bin");
                        break;
                     case "DeleteMapTBin":
                        deleteSavefileFilesMatching(var3, "map_t\\.bin");
                        break;
                     case "DeleteMapZoneBin":
                        deleteSavefileFilesMatching(var3, "map_zone\\.bin");
                        break;
                     case "DeletePlayersDB":
                        deleteSavefileFilesMatching(var3, "players\\.db");
                        break;
                     case "DeleteReanimatedBin":
                        deleteSavefileFilesMatching(var3, "reanimated\\.bin");
                        break;
                     case "DeleteVehiclesDB":
                        deleteSavefileFilesMatching(var3, "vehicles\\.db");
                        break;
                     case "DeleteZOutfitsBin":
                        deleteSavefileFilesMatching(var3, "z_outfits\\.bin");
                        break;
                     case "DeleteZPopVirtualBin":
                        deleteSavefileFilesMatching(new File(var3, "zpop"), "zpop_virtual\\.bin");
                        break;
                     case "DeleteZPopXYBin":
                        deleteSavefileFilesMatching(new File(var3, "zpop"), "zpop_[0-9]+_[0-9]+\\.bin");
                        break;
                     case "WriteModsDotTxt":
                        ActiveMods var6 = ActiveMods.getById("currentGame");
                        ActiveModsFile var7 = new ActiveModsFile();
                        var7.write(var2 + File.separator + "mods.txt", var6);
                        break;
                     default:
                        throw new IllegalArgumentException("unknown action \"" + var1 + "\"");
                  }

               }
            }
         }
      }

      @LuaMethod(
         name = "getLocalVarName",
         global = true
      )
      public static String getLocalVarName(Coroutine var0, int var1) {
         LuaCallFrame var2 = var0.currentCallFrame();
         return var2.getLocalVarName(var1);
      }

      @LuaMethod(
         name = "getLocalVarName",
         global = true
      )
      public static String getLocalVarName(LuaCallFrame var0, int var1) {
         return var0.getLocalVarName(var1);
      }

      @LuaMethod(
         name = "getLocalVarStack",
         global = true
      )
      public static int getLocalVarStack(Coroutine var0, int var1) {
         LuaCallFrame var2 = var0.currentCallFrame();
         return var2.getLocalVarStackIndex(var1);
      }

      @LuaMethod(
         name = "getLocalVarStackIndex",
         global = true
      )
      public static int getLocalVarStackIndex(LuaCallFrame var0, int var1) {
         return var0.getLocalVarStackIndex(var1);
      }

      @LuaMethod(
         name = "getCallframeTop",
         global = true
      )
      public static int getCallframeTop(Coroutine var0) {
         return var0.getCallframeTop();
      }

      @LuaMethod(
         name = "getCoroutineTop",
         global = true
      )
      public static int getCoroutineTop(Coroutine var0) {
         return var0.getTop();
      }

      @LuaMethod(
         name = "getCoroutineObjStack",
         global = true
      )
      public static Object getCoroutineObjStack(Coroutine var0, int var1) {
         return var0.getObjectFromStack(var1);
      }

      @LuaMethod(
         name = "getCoroutineObjStackWithBase",
         global = true
      )
      public static Object getCoroutineObjStackWithBase(Coroutine var0, int var1) {
         return var0.getObjectFromStack(var1 - var0.currentCallFrame().localBase);
      }

      @LuaMethod(
         name = "localVarName",
         global = true
      )
      public static String localVarName(Coroutine var0, int var1) {
         int var2 = var0.getCallframeTop() - 1;
         if (var2 < 0) {
            boolean var3 = false;
         }

         return "";
      }

      @LuaMethod(
         name = "getCoroutineCallframeStack",
         global = true
      )
      public static LuaCallFrame getCoroutineCallframeStack(Coroutine var0, int var1) {
         return var0.getCallFrame(var1);
      }

      @LuaMethod(
         name = "getLuaStackTrace",
         global = true
      )
      public static ArrayList<String> getLuaStackTrace() {
         ArrayList var0 = new ArrayList();
         Coroutine var1 = LuaManager.thread.getCurrentCoroutine();
         if (var1 == null) {
            return var0;
         } else {
            int var2 = var1.getCallframeTop();

            for(int var3 = var2 - 1; var3 >= 0; --var3) {
               LuaCallFrame var4 = var1.getCallFrame(var3);
               String var5 = KahluaUtil.rawTostring2(var4);
               if (var5 != null) {
                  var0.add(var5);
               }
            }

            return var0;
         }
      }

      @LuaMethod(
         name = "createTile",
         global = true
      )
      public static void createTile(String var0, IsoGridSquare var1) {
         synchronized(IsoSpriteManager.instance.NamedMap) {
            IsoSprite var3 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var0);
            if (var3 != null) {
               int var4 = 0;
               int var5 = 0;
               int var6 = 0;
               if (var1 != null) {
                  var4 = var1.getX();
                  var5 = var1.getY();
                  var6 = var1.getZ();
               }

               CellLoader.DoTileObjectCreation(var3, var3.getType(), var1, IsoWorld.instance.CurrentCell, var4, var5, var6, var0);
            }
         }
      }

      @LuaMethod(
         name = "getNumClassFunctions",
         global = true
      )
      public static int getNumClassFunctions(Object var0) {
         return var0.getClass().getDeclaredMethods().length;
      }

      @LuaMethod(
         name = "getClassFunction",
         global = true
      )
      public static Method getClassFunction(Object var0, int var1) {
         Method var2 = var0.getClass().getDeclaredMethods()[var1];
         return var2;
      }

      @LuaMethod(
         name = "getNumClassFields",
         global = true
      )
      public static int getNumClassFields(Object var0) {
         return var0.getClass().getDeclaredFields().length;
      }

      @LuaMethod(
         name = "getClassField",
         global = true
      )
      public static Field getClassField(Object var0, int var1) {
         Field var2 = var0.getClass().getDeclaredFields()[var1];
         var2.setAccessible(true);
         return var2;
      }

      @LuaMethod(
         name = "getDirectionTo",
         global = true
      )
      public static IsoDirections getDirectionTo(IsoGameCharacter var0, IsoObject var1) {
         Vector2 var2 = new Vector2(var1.getX(), var1.getY());
         var2.x -= var0.getX();
         var2.y -= var0.getY();
         return IsoDirections.fromAngle(var2);
      }

      @LuaMethod(
         name = "translatePointXInOverheadMapToWindow",
         global = true
      )
      public static float translatePointXInOverheadMapToWindow(float var0, UIElement var1, float var2, float var3) {
         IngameState.draww = (float)var1.getWidth().intValue();
         return IngameState.translatePointX(var0, var3, var2, 0.0F);
      }

      @LuaMethod(
         name = "translatePointYInOverheadMapToWindow",
         global = true
      )
      public static float translatePointYInOverheadMapToWindow(float var0, UIElement var1, float var2, float var3) {
         IngameState.drawh = (float)var1.getHeight().intValue();
         return IngameState.translatePointY(var0, var3, var2, 0.0F);
      }

      @LuaMethod(
         name = "translatePointXInOverheadMapToWorld",
         global = true
      )
      public static float translatePointXInOverheadMapToWorld(float var0, UIElement var1, float var2, float var3) {
         IngameState.draww = (float)var1.getWidth().intValue();
         return IngameState.invTranslatePointX(var0, var3, var2, 0.0F);
      }

      @LuaMethod(
         name = "translatePointYInOverheadMapToWorld",
         global = true
      )
      public static float translatePointYInOverheadMapToWorld(float var0, UIElement var1, float var2, float var3) {
         IngameState.drawh = (float)var1.getHeight().intValue();
         return IngameState.invTranslatePointY(var0, var3, var2, 0.0F);
      }

      @LuaMethod(
         name = "drawOverheadMap",
         global = true
      )
      public static void drawOverheadMap(UIElement var0, int var1, float var2, float var3, float var4) {
         IngameState.renderDebugOverhead2(getCell(), var1, var2, var0.getAbsoluteX().intValue(), var0.getAbsoluteY().intValue(), var3, var4, var0.getWidth().intValue(), var0.getHeight().intValue());
      }

      @LuaMethod(
         name = "assaultPlayer",
         global = true
      )
      public static void assaultPlayer() {
         assert false;

      }

      @LuaMethod(
         name = "isoRegionsRenderer",
         global = true
      )
      public static IsoRegionsRenderer isoRegionsRenderer() {
         return new IsoRegionsRenderer();
      }

      @LuaMethod(
         name = "zpopNewRenderer",
         global = true
      )
      public static ZombiePopulationRenderer zpopNewRenderer() {
         return new ZombiePopulationRenderer();
      }

      @LuaMethod(
         name = "zpopSpawnTimeToZero",
         global = true
      )
      public static void zpopSpawnTimeToZero(int var0, int var1) {
         ZombiePopulationManager.instance.dbgSpawnTimeToZero(var0, var1);
      }

      @LuaMethod(
         name = "zpopClearZombies",
         global = true
      )
      public static void zpopClearZombies(int var0, int var1) {
         ZombiePopulationManager.instance.dbgClearZombies(var0, var1);
      }

      @LuaMethod(
         name = "zpopSpawnNow",
         global = true
      )
      public static void zpopSpawnNow(int var0, int var1) {
         ZombiePopulationManager.instance.dbgSpawnNow(var0, var1);
      }

      @LuaMethod(
         name = "addVirtualZombie",
         global = true
      )
      public static void addVirtualZombie(int var0, int var1) {
      }

      @LuaMethod(
         name = "luaDebug",
         global = true
      )
      public static void luaDebug() {
         try {
            throw new Exception("LuaDebug");
         } catch (Exception var1) {
            var1.printStackTrace();
         }
      }

      @LuaMethod(
         name = "setAggroTarget",
         global = true
      )
      public static void setAggroTarget(int var0, int var1, int var2) {
         ZombiePopulationManager.instance.setAggroTarget(var0, var1, var2);
      }

      @LuaMethod(
         name = "debugFullyStreamedIn",
         global = true
      )
      public static void debugFullyStreamedIn(int var0, int var1) {
         IngameState.instance.debugFullyStreamedIn(var0, var1);
      }

      @LuaMethod(
         name = "getClassFieldVal",
         global = true
      )
      public static Object getClassFieldVal(Object var0, Field var1) {
         try {
            return var1.get(var0);
         } catch (Exception var3) {
            return "<private>";
         }
      }

      @LuaMethod(
         name = "getMethodParameter",
         global = true
      )
      public static String getMethodParameter(Method var0, int var1) {
         return var0.getParameterTypes()[var1].getSimpleName();
      }

      @LuaMethod(
         name = "getMethodParameterCount",
         global = true
      )
      public static int getMethodParameterCount(Method var0) {
         return var0.getParameterTypes().length;
      }

      @LuaMethod(
         name = "breakpoint",
         global = true
      )
      public static void breakpoint() {
         boolean var0 = false;
      }

      @LuaMethod(
         name = "getLuaDebuggerErrorCount",
         global = true
      )
      public static int getLuaDebuggerErrorCount() {
         KahluaThread var10000 = LuaManager.thread;
         return KahluaThread.m_error_count;
      }

      @LuaMethod(
         name = "getLuaDebuggerErrors",
         global = true
      )
      public static ArrayList<String> getLuaDebuggerErrors() {
         KahluaThread var10002 = LuaManager.thread;
         ArrayList var0 = new ArrayList(KahluaThread.m_errors_list);
         return var0;
      }

      @LuaMethod(
         name = "doLuaDebuggerAction",
         global = true
      )
      public static void doLuaDebuggerAction(String var0) {
         UIManager.luaDebuggerAction = var0;
      }

      @LuaMethod(
         name = "isQuitCooldown",
         global = true
      )
      public static boolean isQuitCooldown() {
         return SafetySystemManager.getCooldown(GameClient.connection) > 0.0F;
      }

      @LuaMethod(
         name = "getGameSpeed",
         global = true
      )
      public static int getGameSpeed() {
         return UIManager.getSpeedControls() != null ? UIManager.getSpeedControls().getCurrentGameSpeed() : 0;
      }

      @LuaMethod(
         name = "setGameSpeed",
         global = true
      )
      public static void setGameSpeed(int var0) {
         DebugType.ExitDebug.debugln("setGameSpeed 1");
         if (UIManager.getSpeedControls() == null) {
            DebugType.ExitDebug.debugln("setGameSpeed 2");
         } else {
            UIManager.getSpeedControls().SetCurrentGameSpeed(var0);
            DebugType.ExitDebug.debugln("setGameSpeed 3");
         }
      }

      @LuaMethod(
         name = "stepForward",
         global = true
      )
      public static void stepForward() {
         if (UIManager.getSpeedControls() != null) {
            UIManager.getSpeedControls().stepForward();
         }
      }

      @LuaMethod(
         name = "isGamePaused",
         global = true
      )
      public static boolean isGamePaused() {
         return GameTime.isGamePaused();
      }

      @LuaMethod(
         name = "getMouseXScaled",
         global = true
      )
      public static int getMouseXScaled() {
         return Mouse.getX();
      }

      @LuaMethod(
         name = "getMouseYScaled",
         global = true
      )
      public static int getMouseYScaled() {
         return Mouse.getY();
      }

      @LuaMethod(
         name = "getMouseX",
         global = true
      )
      public static int getMouseX() {
         return Mouse.getXA();
      }

      @LuaMethod(
         name = "setMouseXY",
         global = true
      )
      public static void setMouseXY(int var0, int var1) {
         Mouse.setXY(var0, var1);
      }

      @LuaMethod(
         name = "isMouseButtonDown",
         global = true
      )
      public static boolean isMouseButtonDown(int var0) {
         return Mouse.isButtonDown(var0);
      }

      @LuaMethod(
         name = "isMouseButtonPressed",
         global = true
      )
      public static boolean isMouseButtonPressed(int var0) {
         return Mouse.isButtonPressed(var0);
      }

      @LuaMethod(
         name = "getMouseY",
         global = true
      )
      public static int getMouseY() {
         return Mouse.getYA();
      }

      @LuaMethod(
         name = "getSoundManager",
         global = true
      )
      public static BaseSoundManager getSoundManager() {
         return SoundManager.instance;
      }

      @LuaMethod(
         name = "getLastPlayedDate",
         global = true
      )
      public static String getLastPlayedDate(String var0) {
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var1 = new File(var10002 + File.separator + var0);
         if (!var1.exists()) {
            return Translator.getText("UI_LastPlayed") + "???";
         } else {
            Date var2 = new Date(var1.lastModified());
            SimpleDateFormat var3 = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String var4 = var3.format(var2);
            String var10000 = Translator.getText("UI_LastPlayed");
            return var10000 + var4;
         }
      }

      @LuaMethod(
         name = "getTextureFromSaveDir",
         global = true
      )
      public static Texture getTextureFromSaveDir(String var0, String var1) {
         TextureID.UseFiltering = true;
         String var2 = ZomboidFileSystem.instance.getSaveDir() + File.separator + var1 + File.separator + var0;
         Texture var3 = Texture.getSharedTexture(var2);
         TextureID.UseFiltering = false;
         return var3;
      }

      @LuaMethod(
         name = "getSaveInfo",
         global = true
      )
      public static KahluaTable getSaveInfo(String var0) {
         String var10000;
         if (!var0.contains(File.separator)) {
            var10000 = IsoWorld.instance.getGameMode();
            var0 = var10000 + File.separator + var0;
         }

         KahluaTable var1 = LuaManager.platform.newTable();
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var2 = new File(var10002 + File.separator + var0);
         if (var2.exists()) {
            Date var3 = new Date(var2.lastModified());
            SimpleDateFormat var4 = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String var5 = var4.format(var3);
            var1.rawset("lastPlayed", var5);
            String[] var6 = var0.split("\\" + File.separator);
            var1.rawset("saveDir", var0);
            var1.rawset("saveName", var2.getName());
            var1.rawset("gameMode", var6[var6.length - 2]);
         }

         var10002 = ZomboidFileSystem.instance.getSaveDir();
         var2 = new File(var10002 + File.separator + var0 + File.separator + "map_ver.bin");
         String var28;
         if (var2.exists()) {
            try {
               FileInputStream var22 = new FileInputStream(var2);

               try {
                  DataInputStream var24 = new DataInputStream(var22);

                  try {
                     int var26 = var24.readInt();
                     var1.rawset("worldVersion", (double)var26);

                     try {
                        var28 = GameWindow.ReadString(var24);
                        if (var28.equals("DEFAULT")) {
                           var28 = "Muldraugh, KY";
                        }

                        var1.rawset("mapName", var28);
                     } catch (Exception var17) {
                     }

                     try {
                        var28 = GameWindow.ReadString(var24);
                        var1.rawset("difficulty", var28);
                     } catch (Exception var16) {
                     }
                  } catch (Throwable var18) {
                     try {
                        var24.close();
                     } catch (Throwable var15) {
                        var18.addSuppressed(var15);
                     }

                     throw var18;
                  }

                  var24.close();
               } catch (Throwable var19) {
                  try {
                     var22.close();
                  } catch (Throwable var14) {
                     var19.addSuppressed(var14);
                  }

                  throw var19;
               }

               var22.close();
            } catch (Exception var20) {
               ExceptionLogger.logException(var20);
            }
         }

         var10000 = ZomboidFileSystem.instance.getSaveDir();
         String var23 = var10000 + File.separator + var0 + File.separator + "mods.txt";
         ActiveMods var25 = new ActiveMods(var0);
         ActiveModsFile var27 = new ActiveModsFile();
         if (var27.read(var23, var25)) {
            var1.rawset("activeMods", var25);
         }

         var10000 = ZomboidFileSystem.instance.getSaveDir();
         var28 = var10000 + File.separator + var0;
         var1.rawset("playerAlive", PlayerDBHelper.isPlayerAlive(var28, 1));
         KahluaTable var7 = LuaManager.platform.newTable();

         try {
            ArrayList var8 = PlayerDBHelper.getPlayers(var28);

            for(int var9 = 0; var9 < var8.size(); var9 += 3) {
               Double var10 = (Double)var8.get(var9);
               String var11 = (String)var8.get(var9 + 1);
               Boolean var12 = (Boolean)var8.get(var9 + 2);
               KahluaTable var13 = LuaManager.platform.newTable();
               var13.rawset("sqlID", var10);
               var13.rawset("name", var11);
               var13.rawset("isDead", var12);
               var7.rawset(var9 / 3 + 1, var13);
            }
         } catch (Exception var21) {
            ExceptionLogger.logException(var21);
         }

         var1.rawset("players", var7);
         return var1;
      }

      @LuaMethod(
         name = "renameSavefile",
         global = true
      )
      public static boolean renameSaveFile(String var0, String var1, String var2) {
         if (var0 != null && !var0.contains("/") && !var0.contains("\\") && !var0.contains(File.separator) && !StringUtils.containsDoubleDot(var0)) {
            if (var1 != null && !var1.contains("/") && !var1.contains("\\") && !var1.contains(File.separator) && !StringUtils.containsDoubleDot(var1)) {
               if (var2 != null && !var2.contains("/") && !var2.contains("\\") && !var2.contains(File.separator) && !StringUtils.containsDoubleDot(var2)) {
                  String var3 = sanitizeWorldName(var2);
                  if (var3.equals(var2) && !var3.startsWith(".") && !var3.endsWith(".")) {
                     if (!(new File(ZomboidFileSystem.instance.getSaveDirSub(var0))).exists()) {
                        return false;
                     } else {
                        Path var4 = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getSaveDirSub(var0 + File.separator + var1));
                        Path var5 = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getSaveDirSub(var0 + File.separator + var3));

                        try {
                           Files.move(var4, var5);
                           return true;
                        } catch (IOException var7) {
                           return false;
                        }
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "setSavefilePlayer1",
         global = true
      )
      public static void setSavefilePlayer1(String var0, String var1, int var2) {
         String var3 = ZomboidFileSystem.instance.getSaveDirSub(var0 + File.separator + var1);

         try {
            PlayerDBHelper.setPlayer1(var3, var2);
         } catch (Exception var5) {
            ExceptionLogger.logException(var5);
         }

      }

      @LuaMethod(
         name = "getServerSavedWorldVersion",
         global = true
      )
      public static int getServerSavedWorldVersion(String var0) {
         String var10002 = ZomboidFileSystem.instance.getSaveDir();
         File var1 = new File(var10002 + File.separator + var0 + File.separator + "map_t.bin");
         if (var1.exists()) {
            try {
               FileInputStream var2 = new FileInputStream(var1);

               int var8;
               label64: {
                  byte var14;
                  try {
                     DataInputStream var3 = new DataInputStream(var2);

                     label60: {
                        try {
                           byte var4 = var3.readByte();
                           byte var5 = var3.readByte();
                           byte var6 = var3.readByte();
                           byte var7 = var3.readByte();
                           if (var4 != 71 || var5 != 77 || var6 != 84 || var7 != 77) {
                              var14 = 1;
                              break label60;
                           }

                           var8 = var3.readInt();
                        } catch (Throwable var11) {
                           try {
                              var3.close();
                           } catch (Throwable var10) {
                              var11.addSuppressed(var10);
                           }

                           throw var11;
                        }

                        var3.close();
                        break label64;
                     }

                     var3.close();
                  } catch (Throwable var12) {
                     try {
                        var2.close();
                     } catch (Throwable var9) {
                        var12.addSuppressed(var9);
                     }

                     throw var12;
                  }

                  var2.close();
                  return var14;
               }

               var2.close();
               return var8;
            } catch (Exception var13) {
               var13.printStackTrace();
            }
         }

         return 0;
      }

      @LuaMethod(
         name = "getZombieInfo",
         global = true
      )
      public static KahluaTable getZombieInfo(IsoZombie var0) {
         KahluaTable var1 = LuaManager.platform.newTable();
         if (var0 == null) {
            return var1;
         } else {
            var1.rawset("OnlineID", var0.OnlineID);
            var1.rawset("RealX", var0.realx);
            var1.rawset("RealY", var0.realy);
            var1.rawset("X", var0.getX());
            var1.rawset("Y", var0.getY());
            var1.rawset("TargetX", var0.networkAI.targetX);
            var1.rawset("TargetY", var0.networkAI.targetY);
            var1.rawset("PathLength", var0.getPathFindBehavior2().getPathLength());
            var1.rawset("TargetLength", Math.sqrt((double)((var0.getX() - var0.getPathFindBehavior2().getTargetX()) * (var0.getX() - var0.getPathFindBehavior2().getTargetX()) + (var0.getY() - var0.getPathFindBehavior2().getTargetY()) * (var0.getY() - var0.getPathFindBehavior2().getTargetY()))));
            var1.rawset("clientActionState", var0.getActionStateName());
            var1.rawset("clientAnimationState", var0.getAnimationStateName());
            var1.rawset("finderProgress", var0.getFinder().progress.name());
            var1.rawset("usePathFind", Boolean.toString(var0.networkAI.usePathFind));
            var1.rawset("owner", var0.authOwner.username);
            var0.networkAI.DebugInterfaceActive = true;
            return var1;
         }
      }

      @LuaMethod(
         name = "getPlayerInfo",
         global = true
      )
      public static KahluaTable getPlayerInfo(IsoPlayer var0) {
         KahluaTable var1 = LuaManager.platform.newTable();
         if (var0 == null) {
            return var1;
         } else {
            long var2 = GameTime.getServerTime() / 1000000L;
            var1.rawset("OnlineID", var0.OnlineID);
            var1.rawset("RealX", var0.realx);
            var1.rawset("RealY", var0.realy);
            var1.rawset("X", var0.getX());
            var1.rawset("Y", var0.getY());
            var1.rawset("TargetX", var0.networkAI.targetX);
            var1.rawset("TargetY", var0.networkAI.targetY);
            var1.rawset("TargetT", var0.networkAI.targetZ);
            var1.rawset("ServerT", var2);
            var1.rawset("PathLength", var0.getPathFindBehavior2().getPathLength());
            var1.rawset("TargetLength", Math.sqrt((double)((var0.getX() - var0.getPathFindBehavior2().getTargetX()) * (var0.getX() - var0.getPathFindBehavior2().getTargetX()) + (var0.getY() - var0.getPathFindBehavior2().getTargetY()) * (var0.getY() - var0.getPathFindBehavior2().getTargetY()))));
            var1.rawset("clientActionState", var0.getActionStateName());
            var1.rawset("clientAnimationState", var0.getAnimationStateName());
            var1.rawset("finderProgress", var0.getFinder().progress.name());
            var1.rawset("usePathFind", Boolean.toString(var0.networkAI.usePathFind));
            return var1;
         }
      }

      @LuaMethod(
         name = "getMapInfo",
         global = true
      )
      public static KahluaTable getMapInfo(String var0) {
         if (var0.contains(";")) {
            var0 = var0.split(";")[0];
         }

         ChooseGameInfo.Map var1 = ChooseGameInfo.getMapDetails(var0);
         if (var1 == null) {
            return null;
         } else {
            KahluaTable var2 = LuaManager.platform.newTable();
            var2.rawset("description", var1.getDescription());
            var2.rawset("dir", var1.getDirectory());
            KahluaTable var3 = LuaManager.platform.newTable();
            byte var4 = 1;
            Iterator var5 = var1.getLotDirectories().iterator();

            while(var5.hasNext()) {
               String var6 = (String)var5.next();
               var3.rawset((double)var4, var6);
            }

            var2.rawset("lots", var3);
            var2.rawset("thumb", var1.getThumbnail());
            var2.rawset("title", var1.getTitle());
            var2.rawset("worldmap", var1.getWorldmap());
            var2.rawset("spawnSelectImagePyramid", var1.getSpawnSelectImagePyramid());
            var2.rawset("zoomX", BoxedStaticValues.toDouble((double)var1.getZoomX()));
            var2.rawset("zoomY", BoxedStaticValues.toDouble((double)var1.getZoomY()));
            var2.rawset("zoomS", BoxedStaticValues.toDouble((double)var1.getZoomS()));
            var2.rawset("demoVideo", var1.getDemoVideo());
            return var2;
         }
      }

      @LuaMethod(
         name = "getVehicleInfo",
         global = true
      )
      public static KahluaTable getVehicleInfo(BaseVehicle var0) {
         if (var0 == null) {
            return null;
         } else {
            KahluaTable var1 = LuaManager.platform.newTable();
            var1.rawset("name", var0.getScript().getName());
            var1.rawset("weight", var0.getMass());
            var1.rawset("speed", var0.getMaxSpeed());
            var1.rawset("frontEndDurability", Integer.toString(var0.frontEndDurability));
            var1.rawset("rearEndDurability", Integer.toString(var0.rearEndDurability));
            var1.rawset("currentFrontEndDurability", Integer.toString(var0.currentFrontEndDurability));
            var1.rawset("currentRearEndDurability", Integer.toString(var0.currentRearEndDurability));
            var1.rawset("engine_running", var0.isEngineRunning());
            var1.rawset("engine_started", var0.isEngineStarted());
            var1.rawset("engine_quality", var0.getEngineQuality());
            var1.rawset("engine_loudness", var0.getEngineLoudness());
            var1.rawset("engine_power", var0.getEnginePower());
            var1.rawset("battery_isset", var0.getBattery() != null);
            var1.rawset("battery_charge", var0.getBatteryCharge());
            var1.rawset("gas_amount", var0.getPartById("GasTank").getContainerContentAmount());
            var1.rawset("gas_capacity", var0.getPartById("GasTank").getContainerCapacity());
            VehiclePart var2 = var0.getPartById("DoorFrontLeft");
            var1.rawset("doorleft_exist", var2 != null);
            if (var2 != null) {
               var1.rawset("doorleft_open", var2.getDoor().isOpen());
               var1.rawset("doorleft_locked", var2.getDoor().isLocked());
               var1.rawset("doorleft_lockbroken", var2.getDoor().isLockBroken());
               VehicleWindow var3 = var2.findWindow();
               var1.rawset("windowleft_exist", var3 != null);
               if (var3 != null) {
                  var1.rawset("windowleft_open", var3.isOpen());
                  var1.rawset("windowleft_health", var3.getHealth());
               }
            }

            VehiclePart var5 = var0.getPartById("DoorFrontRight");
            var1.rawset("doorright_exist", var5 != null);
            if (var2 != null) {
               var1.rawset("doorright_open", var5.getDoor().isOpen());
               var1.rawset("doorright_locked", var5.getDoor().isLocked());
               var1.rawset("doorright_lockbroken", var5.getDoor().isLockBroken());
               VehicleWindow var4 = var5.findWindow();
               var1.rawset("windowright_exist", var4 != null);
               if (var4 != null) {
                  var1.rawset("windowright_open", var4.isOpen());
                  var1.rawset("windowright_health", var4.getHealth());
               }
            }

            var1.rawset("headlights_set", var0.hasHeadlights());
            var1.rawset("headlights_on", var0.getHeadlightsOn());
            if (var0.getPartById("Heater") != null) {
               var1.rawset("heater_isset", true);
               Object var6 = var0.getPartById("Heater").getModData().rawget("active");
               if (var6 == null) {
                  var1.rawset("heater_on", false);
               } else {
                  var1.rawset("heater_on", var6 == Boolean.TRUE);
               }
            } else {
               var1.rawset("heater_isset", false);
            }

            return var1;
         }
      }

      @LuaMethod(
         name = "getLotDirectories",
         global = true
      )
      public static ArrayList<String> getLotDirectories() {
         return IsoWorld.instance.MetaGrid != null ? IsoWorld.instance.MetaGrid.getLotDirectories() : null;
      }

      @LuaMethod(
         name = "useTextureFiltering",
         global = true
      )
      public static void useTextureFiltering(boolean var0) {
         TextureID.UseFiltering = var0;
      }

      @LuaMethod(
         name = "getTexture",
         global = true
      )
      public static Texture getTexture(String var0) {
         return Texture.getSharedTexture(var0);
      }

      @LuaMethod(
         name = "tryGetTexture",
         global = true
      )
      public static Texture tryGetTexture(String var0) {
         return Texture.trygetTexture(var0);
      }

      @LuaMethod(
         name = "sendSecretKey",
         global = true
      )
      public static void sendSecretKey(String var0, String var1, String var2, String var3, boolean var4, int var5, String var6) {
         ConnectionManager.getInstance().sendSecretKey(var0, var1, var2, var3, var4, var5, var6);
      }

      @LuaMethod(
         name = "stopSendSecretKey",
         global = true
      )
      public static void stopSendSecretKey() {
         GameClient.sendQR = false;
      }

      @LuaMethod(
         name = "generateSecretKey",
         global = true
      )
      public static String generateSecretKey(String var0) {
         return GameClient.instance.generateSecretKey(var0);
      }

      @LuaMethod(
         name = "sendGoogleAuth",
         global = true
      )
      public static void sendGoogleAuth(String var0, String var1) {
         INetworkPacket.send(PacketTypes.PacketType.GoogleAuth, var0, var1);
      }

      @LuaMethod(
         name = "createQRCodeTex",
         global = true
      )
      public static Texture createQRCodeTex(String var0, String var1) throws WriterException, IOException {
         String var2 = GameClient.instance.getQR(var0, var1);
         short var3 = 180;
         short var4 = 180;
         BitMatrix var5 = (new MultiFormatWriter()).encode(var2, BarcodeFormat.QR_CODE, var4, var3);
         BufferedImage var6 = MatrixToImageWriter.toBufferedImage(var5);
         BufferedImage var7 = new BufferedImage(var4, var3, 1);
         Graphics2D var8 = (Graphics2D)var7.getGraphics();
         var8.drawImage(var6, 0, 0, (ImageObserver)null);
         ByteArrayOutputStream var9 = new ByteArrayOutputStream();
         ImageIO.write(var7, "PNG", var9);
         byte[] var10 = var9.toByteArray();
         ByteArrayInputStream var11 = new ByteArrayInputStream(var10);
         Texture var12 = null;

         try {
            BufferedInputStream var13 = new BufferedInputStream(var11, var10.length);
            var12 = new Texture("QRCode", var13, false);
            return var12;
         } catch (Exception var14) {
            DebugLog.General.println("Texture creation failed!");
            return null;
         }
      }

      @LuaMethod(
         name = "getVideo",
         global = true
      )
      public static VideoTexture getVideo(String var0, int var1, int var2) {
         if (LuaManager.videoTextures.containsKey(var0)) {
            return (VideoTexture)LuaManager.videoTextures.get(var0);
         } else {
            VideoTexture var3 = new VideoTexture(var0, var1, var2, false);
            var3.LoadVideoFile();
            LuaManager.videoTextures.put(var0, var3);
            return (VideoTexture)LuaManager.videoTextures.get(var0);
         }
      }

      @LuaMethod(
         name = "getTextManager",
         global = true
      )
      public static TextManager getTextManager() {
         return TextManager.instance;
      }

      @LuaMethod(
         name = "setProgressBarValue",
         global = true
      )
      public static void setProgressBarValue(IsoPlayer var0, int var1) {
         if (var0.isLocalPlayer()) {
            UIManager.getProgressBar((double)var0.getPlayerNum()).setValue((float)var1);
         }

      }

      @LuaMethod(
         name = "getText",
         global = true
      )
      public static String getText(String var0) {
         return Translator.getText(var0);
      }

      @LuaMethod(
         name = "getText",
         global = true
      )
      public static String getText(String var0, Object var1) {
         return Translator.getText(var0, var1);
      }

      @LuaMethod(
         name = "getText",
         global = true
      )
      public static String getText(String var0, Object var1, Object var2) {
         return Translator.getText(var0, var1, var2);
      }

      @LuaMethod(
         name = "getText",
         global = true
      )
      public static String getText(String var0, Object var1, Object var2, Object var3) {
         return Translator.getText(var0, var1, var2, var3);
      }

      @LuaMethod(
         name = "getText",
         global = true
      )
      public static String getText(String var0, Object var1, Object var2, Object var3, Object var4) {
         return Translator.getText(var0, var1, var2, var3, var4);
      }

      @LuaMethod(
         name = "getTextOrNull",
         global = true
      )
      public static String getTextOrNull(String var0) {
         return Translator.getTextOrNull(var0);
      }

      @LuaMethod(
         name = "getTextOrNull",
         global = true
      )
      public static String getTextOrNull(String var0, Object var1) {
         return Translator.getTextOrNull(var0, var1);
      }

      @LuaMethod(
         name = "getTextOrNull",
         global = true
      )
      public static String getTextOrNull(String var0, Object var1, Object var2) {
         return Translator.getTextOrNull(var0, var1, var2);
      }

      @LuaMethod(
         name = "getTextOrNull",
         global = true
      )
      public static String getTextOrNull(String var0, Object var1, Object var2, Object var3) {
         return Translator.getTextOrNull(var0, var1, var2, var3);
      }

      @LuaMethod(
         name = "getTextOrNull",
         global = true
      )
      public static String getTextOrNull(String var0, Object var1, Object var2, Object var3, Object var4) {
         return Translator.getTextOrNull(var0, var1, var2, var3, var4);
      }

      @LuaMethod(
         name = "getItemText",
         global = true
      )
      public static String getItemText(String var0) {
         return Translator.getDisplayItemName(var0);
      }

      @LuaMethod(
         name = "getRadioText",
         global = true
      )
      public static String getRadioText(String var0) {
         return Translator.getRadioText(var0);
      }

      @LuaMethod(
         name = "getTextMediaEN",
         global = true
      )
      public static String getTextMediaEN(String var0) {
         return Translator.getTextMediaEN(var0);
      }

      @LuaMethod(
         name = "getItemNameFromFullType",
         global = true
      )
      public static String getItemNameFromFullType(String var0) {
         return DebugOptions.instance.Asset.CheckItemTexAndNames.getValue() ? "ItemNameFromFullType" : Translator.getItemNameFromFullType(var0);
      }

      @LuaMethod(
         name = "getItem",
         global = true
      )
      public static Item getItem(String var0) {
         return InventoryItemFactory.getItem(var0, true);
      }

      @LuaMethod(
         name = "getItemStaticModel",
         global = true
      )
      public static String getItemStaticModel(String var0) {
         Item var1 = getItem(var0);
         return var1 == null ? null : var1.getStaticModel();
      }

      @LuaMethod(
         name = "isItemFood",
         global = true
      )
      public static boolean isItemFood(String var0) {
         Item var1 = getItem(var0);
         return var1 != null && Item.Type.Food.equals(var1.type);
      }

      @LuaMethod(
         name = "getItemFoodType",
         global = true
      )
      public static String getItemFoodType(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return "ItemFoodType";
         } else {
            Item var1 = getItem(var0);
            return var1 != null ? var1.FoodType : null;
         }
      }

      @LuaMethod(
         name = "isItemFresh",
         global = true
      )
      public static boolean isItemFresh(String var0, float var1) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return false;
         } else {
            Item var2 = getItem(var0);
            if (var2 != null) {
               return var1 < (float)var2.DaysFresh;
            } else {
               return false;
            }
         }
      }

      @LuaMethod(
         name = "getItemCount",
         global = true
      )
      public static int getItemCount(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return 101;
         } else {
            Item var1 = getItem(var0);
            return var1 != null ? var1.getCount() : 0;
         }
      }

      @LuaMethod(
         name = "getItemWeight",
         global = true
      )
      public static float getItemWeight(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return 101.0F;
         } else {
            Item var1 = getItem(var0);
            if (var1 != null) {
               return Item.Type.Weapon.equals(var1.type) ? var1.getWeaponWeight() : var1.getActualWeight();
            } else {
               return 0.0F;
            }
         }
      }

      @LuaMethod(
         name = "getItemActualWeight",
         global = true
      )
      public static float getItemActualWeight(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return 101.0F;
         } else {
            Item var1 = getItem(var0);
            return var1 != null ? var1.getActualWeight() : 0.0F;
         }
      }

      @LuaMethod(
         name = "getItemConditionMax",
         global = true
      )
      public static int getItemConditionMax(String var0) {
         Item var1 = getItem(var0);
         return var1 != null ? var1.getConditionMax() : 0;
      }

      @LuaMethod(
         name = "getItemEvolvedRecipeName",
         global = true
      )
      public static String getItemEvolvedRecipeName(String var0) {
         Item var1 = getItem(var0);
         return var1 != null ? var1.evolvedRecipeName : null;
      }

      @LuaMethod(
         name = "hasItemTag",
         global = true
      )
      public static boolean hasItemTag(String var0, String var1) {
         Item var2 = getItem(var0);
         if (var2 != null) {
            Stream var10000 = var2.Tags.stream();
            Objects.requireNonNull(var1);
            return var10000.anyMatch(var1::equalsIgnoreCase);
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "getItemDisplayName",
         global = true
      )
      public static String getItemDisplayName(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return "ItemDisplayName";
         } else {
            Item var1 = getItem(var0);
            return var1 != null ? var1.getDisplayName() : var0;
         }
      }

      @LuaMethod(
         name = "getItemName",
         global = true
      )
      public static String getItemName(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return "ItemName";
         } else {
            Item var1 = getItem(var0);
            if (var1 != null) {
               String var2 = var1.getDisplayName();
               return var1.vehicleType > 0 ? Translator.getText("IGUI_ItemNameMechanicalType", var2, Translator.getText("IGUI_VehicleType_" + var1.vehicleType)) : var2;
            } else {
               return var0;
            }
         }
      }

      @LuaMethod(
         name = "getItemTextureName",
         global = true
      )
      public static String getItemTextureName(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return "ItemTextureName";
         } else {
            Texture var1 = getItemTex(var0);
            return var1 != null ? var1.getName() : null;
         }
      }

      private static String getItemTextureColor(Item var0, String var1) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return "ItemTextureColor";
         } else {
            String var2 = "";
            String var3 = null;
            if (!var0.getPaletteChoices().isEmpty() || var1 != null) {
               var3 = (String)var0.getPaletteChoices().get(Rand.Next(var0.getPaletteChoices().size()));
               if (var1 != null) {
                  var3 = var1;
               }

               String var10000 = var3.replace(var0.getPalettesStart(), "");
               var2 = "_" + var10000;
            }

            return var2;
         }
      }

      @LuaMethod(
         name = "getAndFindNearestTracks",
         global = true
      )
      public static ArrayList<AnimalTracks> getAndFindNearestTracks(IsoGameCharacter var0) {
         if (GameClient.bClient) {
            GameClient.instance.sendGetAnimalTracks(var0);
         } else if (!GameServer.bServer) {
            return AnimalTracks.getAndFindNearestTracks(var0);
         }

         return null;
      }

      @LuaMethod(
         name = "getItemTex",
         global = true
      )
      public static Texture getItemTex(String var0) {
         if (DebugOptions.instance.Asset.CheckItemTexAndNames.getValue()) {
            return Texture.trygetTexture("media/textures/Foraging/question_mark.png");
         } else {
            Item var1 = getItem(var0);
            if (var1 != null) {
               Texture var2 = null;
               switch (var1.getType()) {
                  case AlarmClock:
                  case Animal:
                  case Drainable:
                  case Food:
                  case Literature:
                  case Map:
                  case Moveable:
                  case Normal:
                  case Weapon:
                     var2 = var1.NormalTexture;
                     break;
                  case AlarmClockClothing:
                  case Clothing:
                     String var10000 = var1.getIcon().replace(".png", "");
                     var2 = Texture.trygetTexture("Item_" + var10000 + getItemTextureColor(var1, (String)null));
                     break;
                  case Container:
                  case Key:
                  case Radio:
                  case WeaponPart:
                     var2 = Texture.trygetTexture("Item_" + var1.getIcon());
                  case KeyRing:
               }

               return var2;
            } else {
               return null;
            }
         }
      }

      @LuaMethod(
         name = "getRecipeDisplayName",
         global = true
      )
      public static String getRecipeDisplayName(String var0) {
         return Translator.getRecipeName(var0);
      }

      @LuaMethod(
         name = "getMyDocumentFolder",
         global = true
      )
      public static String getMyDocumentFolder() {
         return Core.getMyDocumentFolder();
      }

      @LuaMethod(
         name = "getSpriteManager",
         global = true
      )
      public static IsoSpriteManager getSpriteManager(String var0) {
         return IsoSpriteManager.instance;
      }

      @LuaMethod(
         name = "getSprite",
         global = true
      )
      public static IsoSprite getSprite(String var0) {
         return IsoSpriteManager.instance.getSprite(var0);
      }

      @LuaMethod(
         name = "getServerModData",
         global = true
      )
      public static void getServerModData() {
         INetworkPacket.send(PacketTypes.PacketType.GetModData);
      }

      @LuaMethod(
         name = "isXBOXController",
         global = true
      )
      public static boolean isXBOXController() {
         for(int var0 = 0; var0 < GameWindow.GameInput.getControllerCount(); ++var0) {
            Controller var1 = GameWindow.GameInput.getController(var0);
            if (var1 != null && var1.getGamepadName().contains("XBOX 360")) {
               return true;
            }
         }

         return false;
      }

      @LuaMethod(
         name = "isPlaystationController",
         global = true
      )
      public static boolean isPlaystationController(int var0) {
         Controller var1 = GameWindow.GameInput.getController(var0);
         if (var1 == null) {
            return false;
         } else {
            return var1.getJoystickName().contains("Playstation") || var1.getJoystickName().contains("Dualshock");
         }
      }

      @LuaMethod(
         name = "sendClientCommand",
         global = true
      )
      public static void sendClientCommand(String var0, String var1, KahluaTable var2) {
         if (GameClient.bClient && GameClient.bIngame) {
            GameClient.instance.sendClientCommand((IsoPlayer)null, var0, var1, var2);
         } else {
            if (GameServer.bServer) {
               throw new IllegalStateException("can't call this function on the server");
            }

            SinglePlayerClient.sendClientCommand((IsoPlayer)null, var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendClientCommand",
         global = true
      )
      public static void sendClientCommand(IsoPlayer var0, String var1, String var2, KahluaTable var3) {
         if (GameServer.bServer) {
            LuaEventManager.triggerEvent("OnClientCommand", var1, var2, var0, var3);
         } else if (var0 != null && var0.isLocalPlayer()) {
            if (GameClient.bClient && GameClient.bIngame) {
               GameClient.instance.sendClientCommand(var0, var1, var2, var3);
            } else {
               if (GameServer.bServer) {
                  throw new IllegalStateException("can't call this function on the server");
               }

               SinglePlayerClient.sendClientCommand(var0, var1, var2, var3);
            }

         }
      }

      @LuaMethod(
         name = "sendServerCommand",
         global = true
      )
      public static void sendServerCommand(String var0, String var1, KahluaTable var2) {
         if (GameServer.bServer) {
            GameServer.sendServerCommand(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendServerCommand",
         global = true
      )
      public static void sendServerCommand(IsoPlayer var0, String var1, String var2, KahluaTable var3) {
         if (GameServer.bServer) {
            GameServer.sendServerCommand(var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendServerCommandV",
         global = true
      )
      public void sendServerCommandV(String var1, String var2, Object... var3) {
         if (GameServer.bServer) {
            GameServer.sendServerCommandV(var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendClientCommandV",
         global = true
      )
      public void sendClientCommandV(IsoPlayer var1, String var2, String var3, Object... var4) {
         if (GameClient.bClient) {
            GameClient.instance.sendClientCommandV(var1, var2, var3, var4);
         }

      }

      @LuaMethod(
         name = "addVariableToSyncList",
         global = true
      )
      public static void addVariableToSyncList(String var0) {
         VariableSyncPacket.syncedVariables.add(var0);
      }

      @LuaMethod(
         name = "getOnlineUsername",
         global = true
      )
      public static String getOnlineUsername() {
         return IsoPlayer.getInstance().getDisplayName();
      }

      @LuaMethod(
         name = "isValidUserName",
         global = true
      )
      public static boolean isValidUserName(String var0) {
         return ServerWorldDatabase.isValidUserName(var0);
      }

      @LuaMethod(
         name = "getHourMinute",
         global = true
      )
      public static String getHourMinute() {
         return LuaManager.getHourMinuteJava();
      }

      @LuaMethod(
         name = "SendCommandToServer",
         global = true
      )
      public static void SendCommandToServer(String var0) {
         GameClient.SendCommandToServer(var0);
      }

      @LuaMethod(
         name = "isAdmin",
         global = true
      )
      public static boolean isAdmin() {
         return GameClient.bClient && GameClient.connection.role == Roles.getDefaultForAdmin();
      }

      /** @deprecated */
      @Deprecated
      @LuaMethod(
         name = "canModifyPlayerScoreboard",
         global = true
      )
      public static boolean canModifyPlayerScoreboard() {
         return GameClient.bClient && GameClient.connection.role.haveCapability(Capability.CanModifyPlayerStatsInThePlayerStatsUI);
      }

      /** @deprecated */
      @Deprecated
      @LuaMethod(
         name = "isAccessLevel",
         global = true
      )
      public static boolean isAccessLevel(String var0) {
         return GameClient.bClient ? GameClient.connection.role.getName().equals(var0) : false;
      }

      @LuaMethod(
         name = "sendHumanVisual",
         global = true
      )
      public static void sendHumanVisual(IsoPlayer var0) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.HumanVisual, (float)var0.square.x, (float)var0.square.y, var0);
         }

      }

      @LuaMethod(
         name = "stopFire",
         global = true
      )
      public static void stopFire(Object var0) {
         if (GameServer.bServer) {
            INetworkPacket.processPacketOnServer(PacketTypes.PacketType.StopFire, (UdpConnection)null, var0);
         }

      }

      @LuaMethod(
         name = "getGameClient",
         global = true
      )
      public static GameClient getGameClient() {
         return GameClient.instance;
      }

      @LuaMethod(
         name = "sendRequestInventory",
         global = true
      )
      public static void sendRequestInventory(int var0, String var1) {
         INetworkPacket.send(PacketTypes.PacketType.PlayerInventory, (short)var0, var1);
      }

      @LuaMethod(
         name = "InvMngGetItem",
         global = true
      )
      public static void InvMngGetItem(long var0, String var2, int var3, String var4) {
         GameClient.invMngRequestItem((int)var0, var2, (short)var3, var4);
      }

      @LuaMethod(
         name = "InvMngRemoveItem",
         global = true
      )
      public static void InvMngRemoveItem(long var0, int var2, String var3) {
         GameClient.invMngRequestRemoveItem((int)var0, (short)var2, var3);
      }

      @LuaMethod(
         name = "getConnectedPlayers",
         global = true
      )
      public static ArrayList<IsoPlayer> getConnectedPlayers() {
         return GameClient.instance.getConnectedPlayers();
      }

      @LuaMethod(
         name = "getPlayerFromUsername",
         global = true
      )
      public static IsoPlayer getPlayerFromUsername(String var0) {
         return GameClient.instance.getPlayerFromUsername(var0);
      }

      @LuaMethod(
         name = "isCoopHost",
         global = true
      )
      public static boolean isCoopHost() {
         return GameClient.connection != null && GameClient.connection.isCoopHost;
      }

      @LuaMethod(
         name = "setAdmin",
         global = true
      )
      public static void setAdmin() {
         if (CoopMaster.instance.isRunning()) {
            String var0 = Roles.getDefaultForAdmin().getName();
            if (GameClient.connection.role == Roles.getDefaultForAdmin()) {
               var0 = "";
            }

            GameClient.connection.role = Roles.getDefaultForAdmin();
            IsoPlayer.getInstance().setRole(GameClient.connection.role);
            GameClient.SendCommandToServer("/setaccesslevel \"" + IsoPlayer.getInstance().username + "\" \"" + (var0.equals("") ? "none" : var0) + "\"");
            if (var0.equals("") && IsoPlayer.getInstance().isInvisible() || var0.equals("admin") && !IsoPlayer.getInstance().isInvisible()) {
               GameClient.SendCommandToServer("/invisible");
            }

         }
      }

      @LuaMethod(
         name = "addWarningPoint",
         global = true
      )
      public static void addWarningPoint(String var0, String var1, int var2) {
         if (GameClient.bClient) {
            GameClient.instance.addWarningPoint(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "disconnect",
         global = true
      )
      public static void disconnect() {
         GameClient.connection.forceDisconnect("lua-disconnect");
      }

      @LuaMethod(
         name = "writeLog",
         global = true
      )
      public static void writeLog(String var0, String var1) {
         LoggerManager.getLogger(var0).write(var1);
      }

      @LuaMethod(
         name = "doKeyPress",
         global = true
      )
      public static void doKeyPress(boolean var0) {
         GameKeyboard.doLuaKeyPressed = var0;
      }

      @LuaMethod(
         name = "getEvolvedRecipes",
         global = true
      )
      public static Stack<EvolvedRecipe> getEvolvedRecipes() {
         return ScriptManager.instance.getAllEvolvedRecipes();
      }

      @LuaMethod(
         name = "getZone",
         global = true
      )
      public static Zone getZone(int var0, int var1, int var2) {
         return IsoWorld.instance.MetaGrid.getZoneAt(var0, var1, var2);
      }

      @LuaMethod(
         name = "getZones",
         global = true
      )
      public static ArrayList<Zone> getZones(int var0, int var1, int var2) {
         return IsoWorld.instance.MetaGrid.getZonesAt(var0, var1, var2);
      }

      @LuaMethod(
         name = "getVehicleZoneAt",
         global = true
      )
      public static VehicleZone getVehicleZoneAt(int var0, int var1, int var2) {
         return IsoWorld.instance.MetaGrid.getVehicleZoneAt(var0, var1, var2);
      }

      @LuaMethod(
         name = "getCellMinX",
         global = true
      )
      public static int getCellMinX() {
         return IsoWorld.instance.MetaGrid.getMinX();
      }

      @LuaMethod(
         name = "getCellMaxX",
         global = true
      )
      public static int getCellMaxX() {
         return IsoWorld.instance.MetaGrid.getMaxX();
      }

      @LuaMethod(
         name = "getCellMinY",
         global = true
      )
      public static int getCellMinY() {
         return IsoWorld.instance.MetaGrid.getMinY();
      }

      @LuaMethod(
         name = "getCellMaxY",
         global = true
      )
      public static int getCellMaxY() {
         return IsoWorld.instance.MetaGrid.getMaxY();
      }

      @LuaMethod(
         name = "replaceWith",
         global = true
      )
      public static String replaceWith(String var0, String var1, String var2) {
         return var0.replaceFirst(var1, var2);
      }

      @LuaMethod(
         name = "getTimestamp",
         global = true
      )
      public static long getTimestamp() {
         return System.currentTimeMillis() / 1000L;
      }

      @LuaMethod(
         name = "getTimestampMs",
         global = true
      )
      public static long getTimestampMs() {
         return System.currentTimeMillis();
      }

      @LuaMethod(
         name = "forceSnowCheck",
         global = true
      )
      public static void forceSnowCheck() {
         ErosionMain.getInstance().snowCheck();
      }

      @LuaMethod(
         name = "getGametimeTimestamp",
         global = true
      )
      public static long getGametimeTimestamp() {
         return GameTime.instance.getCalender().getTimeInMillis() / 1000L;
      }

      @LuaMethod(
         name = "canInviteFriends",
         global = true
      )
      public static boolean canInviteFriends() {
         if (GameClient.bClient && SteamUtils.isSteamModeEnabled()) {
            return CoopMaster.instance.isRunning() || !GameClient.bCoopInvite;
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "inviteFriend",
         global = true
      )
      public static void inviteFriend(String var0) {
         if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
            CoopMaster.instance.sendMessage("invite-add", var0);
         }

         SteamFriends.InviteUserToGame(SteamUtils.convertStringToSteamID(var0), "+connect " + GameClient.ip + ":" + GameClient.port);
      }

      @LuaMethod(
         name = "getFriendsList",
         global = true
      )
      public static KahluaTable getFriendsList() {
         KahluaTable var0 = LuaManager.platform.newTable();
         if (!getSteamModeActive()) {
            return var0;
         } else {
            List var1 = SteamFriends.GetFriendList();
            int var2 = 1;

            for(int var3 = 0; var3 < var1.size(); ++var3) {
               SteamFriend var4 = (SteamFriend)var1.get(var3);
               Double var5 = (double)var2;
               var0.rawset(var5, var4);
               ++var2;
            }

            return var0;
         }
      }

      @LuaMethod(
         name = "getSteamModeActive",
         global = true
      )
      public static Boolean getSteamModeActive() {
         return SteamUtils.isSteamModeEnabled();
      }

      @LuaMethod(
         name = "getStreamModeActive",
         global = true
      )
      public static Boolean getStreamModeActive() {
         return SteamUtils.isStreamModeEnabled();
      }

      @LuaMethod(
         name = "getRemotePlayModeActive",
         global = true
      )
      public static Boolean getRemotePlayModeActive() {
         return SteamRemotePlay.GetSessionCount() > 0;
      }

      @LuaMethod(
         name = "isValidSteamID",
         global = true
      )
      public static boolean isValidSteamID(String var0) {
         return var0 != null && !var0.isEmpty() ? SteamUtils.isValidSteamID(var0) : false;
      }

      @LuaMethod(
         name = "getCurrentUserSteamID",
         global = true
      )
      public static String getCurrentUserSteamID() {
         return SteamUtils.isSteamModeEnabled() && !GameServer.bServer ? SteamUser.GetSteamIDString() : null;
      }

      @LuaMethod(
         name = "getCurrentUserProfileName",
         global = true
      )
      public static String getCurrentUserProfileName() {
         return SteamUtils.isSteamModeEnabled() && !GameServer.bServer ? SteamFriends.GetFriendPersonaName(SteamUser.GetSteamID()) : null;
      }

      @LuaMethod(
         name = "getSteamScoreboard",
         global = true
      )
      public static boolean getSteamScoreboard() {
         if (SteamUtils.isSteamModeEnabled() && GameClient.bClient) {
            String var0 = ServerOptions.instance.SteamScoreboard.getValue();
            return "true".equals(var0) || GameClient.connection.role.haveCapability(Capability.GetSteamScoreboard) && "admin".equals(var0);
         } else {
            return false;
         }
      }

      @LuaMethod(
         name = "isSteamOverlayEnabled",
         global = true
      )
      public static boolean isSteamOverlayEnabled() {
         return SteamUtils.isOverlayEnabled();
      }

      @LuaMethod(
         name = "activateSteamOverlayToWorkshop",
         global = true
      )
      public static void activateSteamOverlayToWorkshop() {
         if (SteamUtils.isOverlayEnabled()) {
            SteamFriends.ActivateGameOverlayToWebPage("steam://url/SteamWorkshopPage/108600");
         }

      }

      @LuaMethod(
         name = "activateSteamOverlayToWorkshopUser",
         global = true
      )
      public static void activateSteamOverlayToWorkshopUser() {
         if (SteamUtils.isOverlayEnabled()) {
            SteamFriends.ActivateGameOverlayToWebPage("steam://url/SteamIDCommunityFilesPage/" + SteamUser.GetSteamIDString() + "/108600");
         }

      }

      @LuaMethod(
         name = "activateSteamOverlayToWorkshopItem",
         global = true
      )
      public static void activateSteamOverlayToWorkshopItem(String var0) {
         if (SteamUtils.isOverlayEnabled() && SteamUtils.isValidSteamID(var0)) {
            SteamFriends.ActivateGameOverlayToWebPage("steam://url/CommunityFilePage/" + var0);
         }

      }

      @LuaMethod(
         name = "activateSteamOverlayToWebPage",
         global = true
      )
      public static void activateSteamOverlayToWebPage(String var0) {
         if (var0.startsWith("https://steamcommunity.com") || var0.startsWith("https://projectzomboid.com") || var0.startsWith("https://theindiestone.com")) {
            if (SteamUtils.isOverlayEnabled()) {
               SteamFriends.ActivateGameOverlayToWebPage(var0);
            }

         }
      }

      @LuaMethod(
         name = "getSteamProfileNameFromSteamID",
         global = true
      )
      public static String getSteamProfileNameFromSteamID(String var0) {
         if (SteamUtils.isSteamModeEnabled() && GameClient.bClient) {
            long var1 = SteamUtils.convertStringToSteamID(var0);
            if (var1 != -1L) {
               return SteamFriends.GetFriendPersonaName(var1);
            }
         }

         return null;
      }

      @LuaMethod(
         name = "getSteamAvatarFromSteamID",
         global = true
      )
      public static Texture getSteamAvatarFromSteamID(String var0) {
         if (SteamUtils.isSteamModeEnabled() && GameClient.bClient) {
            long var1 = SteamUtils.convertStringToSteamID(var0);
            if (var1 != -1L) {
               return Texture.getSteamAvatar(var1);
            }
         }

         return null;
      }

      @LuaMethod(
         name = "getSteamIDFromUsername",
         global = true
      )
      public static String getSteamIDFromUsername(String var0) {
         if (SteamUtils.isSteamModeEnabled() && GameClient.bClient) {
            IsoPlayer var1 = GameClient.instance.getPlayerFromUsername(var0);
            if (var1 != null) {
               return SteamUtils.convertSteamIDToString(var1.getSteamID());
            }
         }

         return null;
      }

      @LuaMethod(
         name = "resetRegionFile",
         global = true
      )
      public static void resetRegionFile() {
         ServerOptions.getInstance().resetRegionFile();
      }

      @LuaMethod(
         name = "getSteamProfileNameFromUsername",
         global = true
      )
      public static String getSteamProfileNameFromUsername(String var0) {
         if (SteamUtils.isSteamModeEnabled() && GameClient.bClient) {
            IsoPlayer var1 = GameClient.instance.getPlayerFromUsername(var0);
            if (var1 != null) {
               return SteamFriends.GetFriendPersonaName(var1.getSteamID());
            }
         }

         return null;
      }

      @LuaMethod(
         name = "getSteamAvatarFromUsername",
         global = true
      )
      public static Texture getSteamAvatarFromUsername(String var0) {
         if (SteamUtils.isSteamModeEnabled() && GameClient.bClient) {
            IsoPlayer var1 = GameClient.instance.getPlayerFromUsername(var0);
            if (var1 != null) {
               return Texture.getSteamAvatar(var1.getSteamID());
            }
         }

         return null;
      }

      @LuaMethod(
         name = "getSteamWorkshopStagedItems",
         global = true
      )
      public static ArrayList<SteamWorkshopItem> getSteamWorkshopStagedItems() {
         return SteamUtils.isSteamModeEnabled() ? SteamWorkshop.instance.loadStagedItems() : null;
      }

      @LuaMethod(
         name = "getSteamWorkshopItemIDs",
         global = true
      )
      public static ArrayList<String> getSteamWorkshopItemIDs() {
         if (SteamUtils.isSteamModeEnabled()) {
            ArrayList var0 = new ArrayList();
            String[] var1 = SteamWorkshop.instance.GetInstalledItemFolders();
            if (var1 == null) {
               return var0;
            } else {
               for(int var2 = 0; var2 < var1.length; ++var2) {
                  String var3 = SteamWorkshop.instance.getIDFromItemInstallFolder(var1[var2]);
                  if (var3 != null) {
                     var0.add(var3);
                  }
               }

               return var0;
            }
         } else {
            return null;
         }
      }

      @LuaMethod(
         name = "getSteamWorkshopItemMods",
         global = true
      )
      public static ArrayList<ChooseGameInfo.Mod> getSteamWorkshopItemMods(String var0) {
         if (SteamUtils.isSteamModeEnabled()) {
            long var1 = SteamUtils.convertStringToSteamID(var0);
            if (var1 > 0L) {
               return ZomboidFileSystem.instance.getWorkshopItemMods(var1);
            }
         }

         return null;
      }

      @LuaMethod(
         name = "isSteamRunningOnSteamDeck",
         global = true
      )
      public static boolean isSteamRunningOnSteamDeck() {
         return SteamUtils.isSteamModeEnabled() ? SteamUtils.isRunningOnSteamDeck() : false;
      }

      @LuaMethod(
         name = "showSteamGamepadTextInput",
         global = true
      )
      public static boolean showSteamGamepadTextInput(boolean var0, boolean var1, String var2, int var3, String var4) {
         return SteamUtils.isSteamModeEnabled() ? SteamUtils.showGamepadTextInput(var0, var1, var2, var3, var4) : false;
      }

      @LuaMethod(
         name = "showSteamFloatingGamepadTextInput",
         global = true
      )
      public static boolean showSteamFloatingGamepadTextInput(boolean var0, int var1, int var2, int var3, int var4) {
         return SteamUtils.isSteamModeEnabled() ? SteamUtils.showFloatingGamepadTextInput(var0, var1, var2, var3, var4) : false;
      }

      @LuaMethod(
         name = "isFloatingGamepadTextInputVisible",
         global = true
      )
      public static boolean isFloatingGamepadTextInputVisible() {
         return SteamUtils.isSteamModeEnabled() ? SteamUtils.isFloatingGamepadTextInputVisible() : false;
      }

      @LuaMethod(
         name = "sendPlayerStatsChange",
         global = true
      )
      public static void sendPlayerStatsChange(IsoPlayer var0) {
         if (GameClient.bClient) {
            GameClient.instance.sendChangedPlayerStats(var0);
         }

      }

      @LuaMethod(
         name = "sendPersonalColor",
         global = true
      )
      public static void sendPersonalColor(IsoPlayer var0) {
         if (GameClient.bClient) {
            GameClient.instance.sendPersonalColor(var0);
         }

      }

      @LuaMethod(
         name = "requestTrading",
         global = true
      )
      public static void requestTrading(IsoPlayer var0, IsoPlayer var1) {
         GameClient.instance.requestTrading(var0, var1);
      }

      @LuaMethod(
         name = "acceptTrading",
         global = true
      )
      public static void acceptTrading(IsoPlayer var0, IsoPlayer var1, boolean var2) {
         GameClient.instance.acceptTrading(var0, var1, var2);
      }

      @LuaMethod(
         name = "tradingUISendAddItem",
         global = true
      )
      public static void tradingUISendAddItem(IsoPlayer var0, IsoPlayer var1, InventoryItem var2) {
         GameClient.instance.tradingUISendAddItem(var0, var1, var2);
      }

      @LuaMethod(
         name = "tradingUISendRemoveItem",
         global = true
      )
      public static void tradingUISendRemoveItem(IsoPlayer var0, IsoPlayer var1, InventoryItem var2) {
         GameClient.instance.tradingUISendRemoveItem(var0, var1, var2);
      }

      @LuaMethod(
         name = "tradingUISendUpdateState",
         global = true
      )
      public static void tradingUISendUpdateState(IsoPlayer var0, IsoPlayer var1, int var2) {
         GameClient.instance.tradingUISendUpdateState(var0, var1, var2);
      }

      @LuaMethod(
         name = "sendWarManagerUpdate",
         global = true
      )
      public static void sendWarManagerUpdate(int var0, String var1, WarManager.State var2) {
         INetworkPacket.send(PacketTypes.PacketType.WarStateSync, var0, var1, var2);
      }

      @LuaMethod(
         name = "querySteamWorkshopItemDetails",
         global = true
      )
      public static void querySteamWorkshopItemDetails(ArrayList<String> var0, LuaClosure var1, Object var2) {
         if (var0 != null && var1 != null) {
            if (var0.isEmpty()) {
               if (var2 == null) {
                  LuaManager.caller.pcall(LuaManager.thread, var1, new Object[]{"Completed", new ArrayList()});
               } else {
                  LuaManager.caller.pcall(LuaManager.thread, var1, new Object[]{var2, "Completed", new ArrayList()});
               }

            } else {
               new ItemQuery(var0, var1, var2);
            }
         } else {
            throw new NullPointerException();
         }
      }

      @LuaMethod(
         name = "connectToServerStateCallback",
         global = true
      )
      public static void connectToServerStateCallback(String var0) {
         if (ConnectToServerState.instance != null) {
            ConnectToServerState.instance.FromLua(var0);
         }

      }

      @LuaMethod(
         name = "getPublicServersList",
         global = true
      )
      public static KahluaTable getPublicServersList() {
         KahluaTable var0 = LuaManager.platform.newTable();
         if (!SteamUtils.isSteamModeEnabled() && !PublicServerUtil.isEnabled()) {
            return var0;
         } else if (System.currentTimeMillis() - timeLastRefresh < 60000L) {
            return var0;
         } else {
            ArrayList var1 = new ArrayList();

            try {
               Server var5;
               if (getSteamModeActive()) {
                  ServerBrowser.RefreshInternetServers();
                  List var2 = ServerBrowser.GetServerList();
                  Iterator var3 = var2.iterator();

                  while(var3.hasNext()) {
                     GameServerDetails var4 = (GameServerDetails)var3.next();
                     var5 = new Server();
                     var5.setName(var4.name);
                     var5.setDescription(var4.gameDescription);
                     var5.setSteamId(Long.toString(var4.steamId));
                     var5.setPing(Integer.toString(var4.ping));
                     var5.setPlayers(Integer.toString(var4.numPlayers));
                     var5.setMaxPlayers(Integer.toString(var4.maxPlayers));
                     var5.setOpen(true);
                     var5.setIp(var4.address);
                     var5.setPort(Integer.toString(var4.port));
                     var5.setMods(var4.tags);
                     var5.setVersion(Core.getInstance().getVersionNumber());
                     var5.setLastUpdate(1);
                     var1.add(var5);
                  }

                  System.out.printf("%d servers\n", var2.size());
               } else {
                  URL var18 = new URL(PublicServerUtil.webSite + "servers.xml");
                  InputStreamReader var20 = new InputStreamReader(var18.openStream());
                  BufferedReader var22 = new BufferedReader(var20);
                  var5 = null;
                  StringBuffer var6 = new StringBuffer();

                  String var24;
                  while((var24 = var22.readLine()) != null) {
                     var6.append(var24).append('\n');
                  }

                  var22.close();
                  DocumentBuilderFactory var7 = DocumentBuilderFactory.newInstance();
                  DocumentBuilder var8 = var7.newDocumentBuilder();
                  Document var9 = var8.parse(new InputSource(new StringReader(var6.toString())));
                  var9.getDocumentElement().normalize();
                  NodeList var10 = var9.getElementsByTagName("server");

                  for(int var11 = 0; var11 < var10.getLength(); ++var11) {
                     Node var12 = var10.item(var11);
                     if (var12.getNodeType() == 1) {
                        Element var13 = (Element)var12;
                        Server var14 = new Server();
                        var14.setName(var13.getElementsByTagName("name").item(0).getTextContent());
                        if (var13.getElementsByTagName("desc").item(0) != null && !"".equals(var13.getElementsByTagName("desc").item(0).getTextContent())) {
                           var14.setDescription(var13.getElementsByTagName("desc").item(0).getTextContent());
                        }

                        var14.setIp(var13.getElementsByTagName("ip").item(0).getTextContent());
                        var14.setPort(var13.getElementsByTagName("port").item(0).getTextContent());
                        var14.setPlayers(var13.getElementsByTagName("players").item(0).getTextContent());
                        var14.setMaxPlayers(var13.getElementsByTagName("maxPlayers").item(0).getTextContent());
                        if (var13.getElementsByTagName("version") != null && var13.getElementsByTagName("version").item(0) != null) {
                           var14.setVersion(var13.getElementsByTagName("version").item(0).getTextContent());
                        }

                        var14.setOpen(var13.getElementsByTagName("open").item(0).getTextContent().equals("1"));
                        Integer var15 = Integer.parseInt(var13.getElementsByTagName("lastUpdate").item(0).getTextContent());
                        if (var13.getElementsByTagName("mods").item(0) != null && !"".equals(var13.getElementsByTagName("mods").item(0).getTextContent())) {
                           var14.setMods(var13.getElementsByTagName("mods").item(0).getTextContent());
                        }

                        var14.setLastUpdate(PZMath.fastfloor((float)((getTimestamp() - (long)var15) / 60L)));
                        NodeList var16 = var13.getElementsByTagName("password");
                        var14.setPasswordProtected(var16 != null && var16.getLength() != 0 && var16.item(0).getTextContent().equals("1"));
                        var1.add(var14);
                     }
                  }
               }

               int var19 = 1;

               for(int var21 = 0; var21 < var1.size(); ++var21) {
                  Server var23 = (Server)var1.get(var21);
                  Double var25 = (double)var19;
                  var0.rawset(var25, var23);
                  ++var19;
               }

               timeLastRefresh = Calendar.getInstance().getTimeInMillis();
               return var0;
            } catch (Exception var17) {
               var17.printStackTrace();
               return null;
            }
         }
      }

      @LuaMethod(
         name = "steamRequestInternetServersList",
         global = true
      )
      public static void steamRequestInternetServersList() {
         ServerBrowser.RefreshInternetServers();
      }

      @LuaMethod(
         name = "steamReleaseInternetServersRequest",
         global = true
      )
      public static void steamReleaseInternetServersRequest() {
         ServerBrowser.Release();
      }

      @LuaMethod(
         name = "steamGetInternetServersCount",
         global = true
      )
      public static int steamRequestInternetServersCount() {
         return ServerBrowser.GetServerCount();
      }

      @LuaMethod(
         name = "steamGetInternetServerDetails",
         global = true
      )
      public static Server steamGetInternetServerDetails(int var0) {
         if (!ServerBrowser.IsRefreshing()) {
            return null;
         } else {
            GameServerDetails var1 = ServerBrowser.GetServerDetails(var0);
            if (var1 == null) {
               return null;
            } else if (!var1.tags.contains("hidden") && !var1.tags.contains("hosted")) {
               if (!var1.tags.contains("hidden") && !var1.tags.contains("hosted")) {
                  Server var2 = new Server();
                  var2.setName(var1.name);
                  var2.setDescription("");
                  var2.setSteamId(Long.toString(var1.steamId));
                  var2.setPing(Integer.toString(var1.ping));
                  var2.setPlayers(Integer.toString(var1.numPlayers));
                  var2.setMaxPlayers(Integer.toString(var1.maxPlayers));
                  var2.setOpen(true);
                  var2.setPublic(true);
                  if (var1.tags.contains("hidden")) {
                     var2.setOpen(false);
                     var2.setPublic(false);
                  }

                  var2.setIp(var1.address);
                  var2.setPort(Integer.toString(var1.port));
                  var2.setMods("");
                  if (!var1.tags.replace("hidden", "").replace("hosted", "").replace(";", "").isEmpty()) {
                     var2.setMods(var1.tags.replace(";hosted", "").replace("hidden", ""));
                  }

                  var2.setHosted(var1.tags.contains("hosted"));
                  var2.setVersion("");
                  var2.setLastUpdate(1);
                  var2.setPasswordProtected(var1.passwordProtected);
                  return var2;
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      }

      @LuaMethod(
         name = "steamRequestServerRules",
         global = true
      )
      public static boolean steamRequestServerRules(String var0, int var1) {
         return ServerBrowser.RequestServerRules(var0, var1);
      }

      @LuaMethod(
         name = "steamRequestServerDetails",
         global = true
      )
      public static boolean steamRequestServerDetails(String var0, int var1) {
         return ServerBrowser.QueryServer(var0, var1);
      }

      @LuaMethod(
         name = "isPublicServerListAllowed",
         global = true
      )
      public static boolean isPublicServerListAllowed() {
         return SteamUtils.isSteamModeEnabled() ? true : PublicServerUtil.isEnabled();
      }

      @LuaMethod(
         name = "is64bit",
         global = true
      )
      public static boolean is64bit() {
         return "64".equals(System.getProperty("sun.arch.data.model"));
      }

      @LuaMethod(
         name = "testSound",
         global = true
      )
      public static void testSound() {
         float var0 = (float)Mouse.getX();
         float var1 = (float)Mouse.getY();
         int var2 = (int)IsoPlayer.getInstance().getZ();
         int var3 = (int)IsoUtils.XToIso(var0, var1, (float)var2);
         int var4 = (int)IsoUtils.YToIso(var0, var1, (float)var2);
         float var5 = 50.0F;
         float var6 = 1.0F;
         AmbientStreamManager.Ambient var7 = new AmbientStreamManager.Ambient("Meta/House Alarm", (float)var3, (float)var4, var5, var6);
         var7.trackMouse = true;
         ((AmbientStreamManager)AmbientStreamManager.instance).ambient.add(var7);
      }

      @LuaMethod(
         name = "getFMODEventPathList",
         global = true
      )
      public static ArrayList<String> getFMODEventPathList() {
         return FMODManager.instance.getEventPathList();
      }

      @LuaMethod(
         name = "debugSetRoomType",
         global = true
      )
      public static void debugSetRoomType(Double var0) {
         ParameterRoomType.setRoomType(var0.intValue());
      }

      @LuaMethod(
         name = "copyTable",
         global = true
      )
      public static KahluaTable copyTable(KahluaTable var0) {
         return LuaManager.copyTable(var0);
      }

      @LuaMethod(
         name = "copyTable",
         global = true
      )
      public static KahluaTable copyTable(KahluaTable var0, KahluaTable var1) {
         return LuaManager.copyTable(var0, var1);
      }

      @LuaMethod(
         name = "renderIsoCircle",
         global = true
      )
      public static void renderIsoCircle(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
         double var9 = 0.3490658503988659;

         for(double var11 = 0.0; var11 < 6.283185307179586; var11 += var9) {
            float var13 = var0 + var3 * (float)Math.cos(var11);
            float var14 = var1 + var3 * (float)Math.sin(var11);
            float var15 = var0 + var3 * (float)Math.cos(var11 + var9);
            float var16 = var1 + var3 * (float)Math.sin(var11 + var9);
            float var17 = IsoUtils.XToScreenExact(var13, var14, var2, 0);
            float var18 = IsoUtils.YToScreenExact(var13, var14, var2, 0);
            float var19 = IsoUtils.XToScreenExact(var15, var16, var2, 0);
            float var20 = IsoUtils.YToScreenExact(var15, var16, var2, 0);
            LineDrawer.drawLine(var17, var18, var19, var20, var4, var5, var6, var7, var8);
         }

      }

      @LuaMethod(
         name = "renderIsoRect",
         global = true
      )
      public static void renderIsoRect(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
         float var9 = IsoUtils.XToScreenExact(var0 - 1.0F, var1 - 1.0F, var2, 0);
         float var10 = IsoUtils.YToScreenExact(var0 - 1.0F, var1 - 1.0F, var2, 0);
         float var11 = IsoUtils.XToScreenExact(var0 - 1.0F + var3, var1 - 1.0F, var2, 0);
         float var12 = IsoUtils.YToScreenExact(var0 - 1.0F, var1 - 1.0F + var3, var2, 0);
         LineDrawer.drawLine(var9, var10, var11, var12, var4, var5, var6, var7, var8);
         var9 = IsoUtils.XToScreenExact(var0 - 1.0F, var1 - 1.0F, var2, 0);
         var10 = IsoUtils.YToScreenExact(var0 - 1.0F, var1 - 1.0F, var2, 0);
         var11 = IsoUtils.XToScreenExact(var0 - 1.0F - var3, var1 - 1.0F, var2, 0);
         var12 = IsoUtils.YToScreenExact(var0 - 1.0F, var1 - 1.0F + var3, var2, 0);
         LineDrawer.drawLine(var9, var10, var11, var12, var4, var5, var6, var7, var8);
         var9 = IsoUtils.XToScreenExact(var0 - 1.0F + var3, var1 - 1.0F + var3, var2, 0);
         var10 = IsoUtils.YToScreenExact(var0 - 1.0F + var3, var1 - 1.0F + var3, var2, 0);
         var11 = IsoUtils.XToScreenExact(var0 - 1.0F - var3, var1 - 1.0F, var2, 0);
         var12 = IsoUtils.YToScreenExact(var0 - 1.0F, var1 - 1.0F + var3, var2, 0);
         LineDrawer.drawLine(var9, var10, var11, var12, var4, var5, var6, var7, var8);
         var9 = IsoUtils.XToScreenExact(var0 - 1.0F + var3, var1 - 1.0F + var3, var2, 0);
         var10 = IsoUtils.YToScreenExact(var0 - 1.0F + var3, var1 - 1.0F + var3, var2, 0);
         var11 = IsoUtils.XToScreenExact(var0 - 1.0F + var3, var1 - 1.0F, var2, 0);
         var12 = IsoUtils.YToScreenExact(var0 - 1.0F, var1 - 1.0F + var3, var2, 0);
         LineDrawer.drawLine(var9, var10, var11, var12, var4, var5, var6, var7, var8);
      }

      @LuaMethod(
         name = "renderLine",
         global = true
      )
      public static void renderLine(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
         LineDrawer.addLine(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9);
      }

      @LuaMethod(
         name = "configureLighting",
         global = true
      )
      public static void configureLighting(float var0) {
         if (LightingJNI.init) {
            LightingJNI.configure(var0);
         }

      }

      @LuaMethod(
         name = "invalidateLighting",
         global = true
      )
      public static void invalidateLighting() {
         for(int var0 = 0; var0 < IsoPlayer.numPlayers; ++var0) {
            LosUtil.cachecleared[var0] = true;
         }

         IsoGridSquare.setRecalcLightTime(-1.0F);
         GameTime.getInstance().lightSourceUpdate = 100.0F;
      }

      @LuaMethod(
         name = "testHelicopter",
         global = true
      )
      public static void testHelicopter() {
         if (GameClient.bClient) {
            GameClient.SendCommandToServer("/chopper start");
         } else {
            IsoWorld.instance.helicopter.pickRandomTarget();
         }

      }

      @LuaMethod(
         name = "endHelicopter",
         global = true
      )
      public static void endHelicopter() {
         if (GameClient.bClient) {
            GameClient.SendCommandToServer("/chopper stop");
         } else {
            IsoWorld.instance.helicopter.deactivate();
         }

      }

      @LuaMethod(
         name = "getServerSettingsManager",
         global = true
      )
      public static ServerSettingsManager getServerSettingsManager() {
         return ServerSettingsManager.instance;
      }

      @LuaMethod(
         name = "rainConfig",
         global = true
      )
      public static void rainConfig(String var0, int var1) {
         if ("alpha".equals(var0)) {
            IsoWorld.instance.CurrentCell.setRainAlpha(var1);
         }

         if ("intensity".equals(var0)) {
            IsoWorld.instance.CurrentCell.setRainIntensity(var1);
         }

         if ("speed".equals(var0)) {
            IsoWorld.instance.CurrentCell.setRainSpeed(var1);
         }

         if ("reloadTextures".equals(var0)) {
            IsoWorld.instance.CurrentCell.reloadRainTextures();
         }

      }

      @LuaMethod(
         name = "sendSwitchSeat",
         global = true
      )
      public static void sendSwitchSeat(BaseVehicle var0, IsoGameCharacter var1, int var2, int var3) {
         if (GameClient.bClient) {
            VehicleManager.instance.sendSwitchSeat(GameClient.connection, var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "getVehicleById",
         global = true
      )
      public static BaseVehicle getVehicleById(int var0) {
         return VehicleManager.instance.getVehicleByID((short)var0);
      }

      @LuaMethod(
         name = "addBloodSplat",
         global = true
      )
      public void addBloodSplat(IsoGridSquare var1, int var2) {
         for(int var3 = 0; var3 < var2; ++var3) {
            var1.getChunk().addBloodSplat((float)var1.x + Rand.Next(-0.5F, 0.5F), (float)var1.y + Rand.Next(-0.5F, 0.5F), (float)var1.z, Rand.Next(8));
         }

      }

      @LuaMethod(
         name = "addBloodSplat",
         global = true
      )
      public void addBloodSplat(IsoGridSquare var1, int var2, float var3, float var4) {
         for(int var5 = 0; var5 < var2; ++var5) {
            var1.getChunk().addBloodSplat((float)var1.x + var3, (float)var1.y + var4, (float)var1.z, Rand.Next(20));
         }

      }

      @LuaMethod(
         name = "addCarCrash",
         global = true
      )
      public static void addCarCrash() {
         IsoGridSquare var0 = IsoPlayer.getInstance().getCurrentSquare();
         if (var0 != null) {
            IsoChunk var1 = var0.getChunk();
            if (var1 != null) {
               Zone var2 = var0.getZone();
               if (var2 != null) {
                  if (var1.canAddRandomCarCrash(var2, true)) {
                     var0.chunk.addRandomCarCrash(var2, true);
                  }
               }
            }
         }
      }

      @LuaMethod(
         name = "createRandomDeadBody",
         global = true
      )
      public static IsoDeadBody createRandomDeadBody(IsoGridSquare var0, int var1) {
         if (var0 == null) {
            return null;
         } else {
            ItemPickerJava.ItemPickerRoom var2 = (ItemPickerJava.ItemPickerRoom)ItemPickerJava.rooms.get("all");
            RandomizedBuildingBase.HumanCorpse var3 = new RandomizedBuildingBase.HumanCorpse(IsoWorld.instance.getCell(), (float)var0.x, (float)var0.y, (float)var0.z);
            var3.setDir(IsoDirections.getRandom());
            var3.setDescriptor(SurvivorFactory.CreateSurvivor());
            var3.setFemale(var3.getDescriptor().isFemale());
            var3.initWornItems("Human");
            var3.initAttachedItems("Human");
            Outfit var4 = var3.getRandomDefaultOutfit();
            var3.dressInNamedOutfit(var4.m_Name);
            var3.initSpritePartsEmpty();
            var3.Dressup(var3.getDescriptor());

            for(int var5 = 0; var5 < var1; ++var5) {
               var3.addBlood((BloodBodyPartType)null, false, true, false);
            }

            IsoDeadBody var6 = new IsoDeadBody(var3, true);
            ItemPickerJava.fillContainerType(var2, var6.getContainer(), var3.isFemale() ? "inventoryfemale" : "inventorymale", (IsoGameCharacter)null);
            LuaEventManager.triggerEvent("OnFillContainer", "Random Dead Body", var6.getContainer().getType(), var6.getContainer());
            return var6;
         }
      }

      @LuaMethod(
         name = "addZombieSitting",
         global = true
      )
      public void addZombieSitting(int var1, int var2, int var3) {
         IsoGridSquare var4 = IsoCell.getInstance().getGridSquare(var1, var2, var3);
         if (var4 != null) {
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(var4);
            IsoZombie var5 = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom().index(), false);
            var5.bDressInRandomOutfit = true;
            ZombiePopulationManager.instance.sitAgainstWall(var5, var4);
         }
      }

      @LuaMethod(
         name = "addZombiesEating",
         global = true
      )
      public void addZombiesEating(int var1, int var2, int var3, int var4, boolean var5) {
         IsoGridSquare var6 = IsoCell.getInstance().getGridSquare(var1, var2, var3);
         if (var6 != null) {
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(var6);
            IsoZombie var7 = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
            var7.setX((float)var6.x);
            var7.setY((float)var6.y);
            var7.setFakeDead(false);
            var7.setHealth(0.0F);
            var7.upKillCount = false;
            if (!var5) {
               var7.dressInRandomOutfit();

               for(int var8 = 0; var8 < 10; ++var8) {
                  var7.addHole((BloodBodyPartType)null);
                  var7.addBlood((BloodBodyPartType)null, false, true, false);
               }

               var7.DoZombieInventory();
            }

            var7.setSkeleton(var5);
            if (var5) {
               var7.getHumanVisual().setSkinTextureIndex(2);
            }

            IsoDeadBody var9 = new IsoDeadBody(var7, true);
            VirtualZombieManager.instance.createEatingZombies(var9, var4);
         }
      }

      @LuaMethod(
         name = "addZombiesInOutfitArea",
         global = true
      )
      public ArrayList<IsoZombie> addZombiesInOutfitArea(int var1, int var2, int var3, int var4, int var5, int var6, String var7, Integer var8) {
         ArrayList var9 = new ArrayList();

         for(int var10 = 0; var10 < var6; ++var10) {
            var9.addAll(addZombiesInOutfit(Rand.Next(var1, var3), Rand.Next(var2, var4), var5, 1, var7, var8));
         }

         return var9;
      }

      @LuaMethod(
         name = "addZombiesInOutfit",
         global = true
      )
      public static ArrayList<IsoZombie> addZombiesInOutfit(int var0, int var1, int var2, int var3, String var4, Integer var5) {
         return addZombiesInOutfit(var0, var1, var2, var3, var4, var5, false, false, false, false, false, false, 1.0F);
      }

      @LuaMethod(
         name = "addZombiesInOutfit",
         global = true
      )
      public static ArrayList<IsoZombie> addZombiesInOutfit(int var0, int var1, int var2, int var3, String var4, Integer var5, boolean var6, boolean var7, boolean var8, boolean var9, boolean var10, boolean var11, float var12) {
         ArrayList var13 = new ArrayList();
         if (IsoWorld.getZombiesDisabled() && !Core.getInstance().getDebug()) {
            return var13;
         } else {
            IsoGridSquare var14 = IsoCell.getInstance().getGridSquare(var0, var1, var2);
            if (var14 == null) {
               return var13;
            } else {
               for(int var15 = 0; var15 < var3; ++var15) {
                  if (var12 <= 0.0F) {
                     var14.getChunk().AddCorpses(var0 / 8, var1 / 8);
                  } else {
                     VirtualZombieManager.instance.choices.clear();
                     VirtualZombieManager.instance.choices.add(var14);
                     IsoZombie var16 = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom().index(), false);
                     if (var16 != null) {
                        if (var5 != null) {
                           var16.setFemaleEtc(Rand.Next(100) < var5);
                        }

                        if (var4 != null) {
                           var16.dressInPersistentOutfit(var4);
                           var16.bDressInRandomOutfit = false;
                        } else {
                           var16.bDressInRandomOutfit = true;
                        }

                        var16.bLunger = true;
                        var16.setKnockedDown(var9);
                        if (var6) {
                           var16.setCrawler(true);
                           var16.setCanWalk(false);
                           var16.setOnFloor(true);
                           var16.setKnockedDown(true);
                           var16.setCrawlerType(1);
                           var16.DoZombieStats();
                        }

                        var16.setFakeDead(var8);
                        var16.setFallOnFront(var7);
                        var16.setInvulnerable(var10);
                        var16.setHealth(var12);
                        if (var11) {
                           var16.setSitAgainstWall(true);
                        }

                        var13.add(var16);
                     }
                  }
               }

               ZombieSpawnRecorder.instance.record(var13, GlobalObject.class.getSimpleName());
               return var13;
            }
         }
      }

      @LuaMethod(
         name = "addZombiesInBuilding",
         global = true
      )
      public ArrayList<IsoZombie> addZombiesInBuilding(BuildingDef var1, int var2, String var3, RoomDef var4, Integer var5) {
         boolean var6 = var4 == null;
         ArrayList var7 = new ArrayList();
         if (IsoWorld.getZombiesDisabled()) {
            return var7;
         } else {
            if (var4 == null) {
               var4 = var1.getRandomRoom(6);
            }

            int var8 = 2;
            int var9 = var4.area / 2;
            if (var2 == 0) {
               if (SandboxOptions.instance.Zombies.getValue() == 1) {
                  var9 += 4;
               } else if (SandboxOptions.instance.Zombies.getValue() == 2) {
                  var9 += 3;
               } else if (SandboxOptions.instance.Zombies.getValue() == 3) {
                  var9 += 2;
               } else if (SandboxOptions.instance.Zombies.getValue() == 5) {
                  var9 -= 4;
               }

               if (var9 > 8) {
                  var9 = 8;
               }

               if (var9 < var8) {
                  var9 = var8 + 1;
               }
            } else {
               var8 = var2;
               var9 = var2;
            }

            int var10 = Rand.Next(var8, var9);

            for(int var11 = 0; var11 < var10; ++var11) {
               IsoGridSquare var12 = RandomizedBuildingBase.getRandomSpawnSquare(var4);
               if (var12 == null) {
                  break;
               }

               VirtualZombieManager.instance.choices.clear();
               VirtualZombieManager.instance.choices.add(var12);
               IsoZombie var13 = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom().index(), false);
               if (var13 != null) {
                  if (var5 != null) {
                     var13.setFemaleEtc(Rand.Next(100) < var5);
                  }

                  if (var3 != null) {
                     var13.dressInPersistentOutfit(var3);
                     var13.bDressInRandomOutfit = false;
                  } else {
                     var13.bDressInRandomOutfit = true;
                  }

                  var7.add(var13);
                  if (var6) {
                     var4 = var1.getRandomRoom(6);
                  }
               }
            }

            ZombieSpawnRecorder.instance.record(var7, this.getClass().getSimpleName());
            return var7;
         }
      }

      @LuaMethod(
         name = "addVehicleDebug",
         global = true
      )
      public static BaseVehicle addVehicleDebug(String var0, IsoDirections var1, Integer var2, IsoGridSquare var3) {
         if (var1 == null) {
            var1 = IsoDirections.getRandom();
         }

         BaseVehicle var4 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         if (!StringUtils.isNullOrEmpty(var0)) {
            var4.setScriptName(var0);
            var4.setScript();
            if (var2 != null) {
               var4.setSkinIndex(var2);
            }
         }

         var4.setDir(var1);

         float var5;
         for(var5 = var1.toAngle() + 3.1415927F + Rand.Next(-0.2F, 0.2F); (double)var5 > 6.283185307179586; var5 = (float)((double)var5 - 6.283185307179586)) {
         }

         var4.savedRot.setAngleAxis(var5, 0.0F, 1.0F, 0.0F);
         var4.jniTransform.setRotation(var4.savedRot);
         var4.setX((float)var3.x);
         var4.setY((float)var3.y);
         var4.setZ((float)var3.z);
         if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var4)) {
            var4.setSquare(var3);
            var3.chunk.vehicles.add(var4);
            var4.chunk = var3.chunk;
            var4.addToWorld();
            VehiclesDB2.instance.addVehicle(var4);
         }

         var4.setGeneralPartCondition(1.3F, 10.0F);
         var4.rust = 0.0F;
         return var4;
      }

      @LuaMethod(
         name = "addVehicle",
         global = true
      )
      public static BaseVehicle addVehicle(String var0, int var1, int var2, int var3) {
         if (!StringUtils.isNullOrWhitespace(var0) && ScriptManager.instance.getVehicle(var0) == null) {
            DebugLog.Lua.warn("No such vehicle script \"" + var0 + "\"");
            return null;
         } else {
            ArrayList var4 = ScriptManager.instance.getAllVehicleScripts();
            if (var4.isEmpty()) {
               DebugLog.Lua.warn("No vehicle scripts defined");
               return null;
            } else {
               WorldSimulation.instance.create();
               BaseVehicle var5 = new BaseVehicle(IsoWorld.instance.CurrentCell);
               if (StringUtils.isNullOrWhitespace(var0)) {
                  VehicleScript var6 = (VehicleScript)PZArrayUtil.pickRandom((List)var4);
                  var0 = var6.getFullName();
               }

               var5.setScriptName(var0);
               if (var1 != 0 && var2 != 0) {
                  var5.setX((float)var1);
                  var5.setY((float)var2);
                  var5.setZ((float)var3);
               } else {
                  var5.setX(IsoPlayer.getInstance().getX());
                  var5.setY(IsoPlayer.getInstance().getY());
                  var5.setZ(0.0F);
               }

               if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var5)) {
                  var5.setSquare(IsoPlayer.getInstance().getSquare());
                  var5.square.chunk.vehicles.add(var5);
                  var5.chunk = var5.square.chunk;
                  var5.addToWorld();
                  VehiclesDB2.instance.addVehicle(var5);
               } else {
                  DebugLog.Lua.error("ERROR: I can not spawn the vehicle. Invalid position. Try to change position.");
               }

               return var5;
            }
         }
      }

      @LuaMethod(
         name = "attachTrailerToPlayerVehicle",
         global = true
      )
      public static void attachTrailerToPlayerVehicle(int var0) {
         IsoPlayer var1 = IsoPlayer.players[var0];
         IsoGridSquare var2 = var1.getCurrentSquare();
         BaseVehicle var3 = var1.getVehicle();
         if (var3 == null) {
            var3 = addVehicleDebug("Base.OffRoad", IsoDirections.N, 0, var2);
            var3.repair();
            var1.getInventory().AddItem(var3.createVehicleKey());
         }

         var2 = IsoWorld.instance.CurrentCell.getGridSquare(var2.x, var2.y + 5, var2.z);
         BaseVehicle var4 = addVehicleDebug("Base.Trailer", IsoDirections.N, 0, var2);
         var4.repair();
         var3.addPointConstraint(var1, var4, "trailer", "trailer");
      }

      @LuaMethod(
         name = "getKeyName",
         global = true
      )
      public static String getKeyName(int var0) {
         return Input.getKeyName(var0);
      }

      @LuaMethod(
         name = "getKeyCode",
         global = true
      )
      public static int getKeyCode(String var0) {
         return Input.getKeyCode(var0);
      }

      @LuaMethod(
         name = "queueCharEvent",
         global = true
      )
      public static void queueCharEvent(String var0) {
         RenderThread.queueInvokeOnRenderContext(() -> {
            GameKeyboard.getEventQueuePolling().addCharEvent(var0.charAt(0));
         });
      }

      @LuaMethod(
         name = "queueKeyEvent",
         global = true
      )
      public static void queueKeyEvent(int var0) {
         RenderThread.queueInvokeOnRenderContext(() -> {
            int var1 = KeyCodes.toGlfwKey(var0);
            GameKeyboard.getEventQueuePolling().addKeyEvent(var1, 1);
            GameKeyboard.getEventQueuePolling().addKeyEvent(var1, 0);
         });
      }

      @LuaMethod(
         name = "addAllVehicles",
         global = true
      )
      public static void addAllVehicles() {
         addAllVehicles((var0) -> {
            return !var0.getName().contains("Smashed") && !var0.getName().contains("Burnt");
         });
      }

      @LuaMethod(
         name = "addAllBurntVehicles",
         global = true
      )
      public static void addAllBurntVehicles() {
         addAllVehicles((var0) -> {
            return var0.getName().contains("Burnt");
         });
      }

      @LuaMethod(
         name = "addAllSmashedVehicles",
         global = true
      )
      public static void addAllSmashedVehicles() {
         addAllVehicles((var0) -> {
            return var0.getName().contains("Smashed");
         });
      }

      public static void addAllVehicles(Predicate<VehicleScript> var0) {
         ArrayList var1 = ScriptManager.instance.getAllVehicleScripts();
         Collections.sort(var1, Comparator.comparing(VehicleScript::getName));
         float var2 = (float)(IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMinTiles() + 5);
         float var3 = IsoPlayer.getInstance().getY();
         float var4 = 0.0F;

         for(int var5 = 0; var5 < var1.size(); ++var5) {
            VehicleScript var6 = (VehicleScript)var1.get(var5);
            if (var6.getModel() != null && var0.test(var6) && IsoWorld.instance.CurrentCell.getGridSquare((double)var2, (double)var3, (double)var4) != null) {
               WorldSimulation.instance.create();
               BaseVehicle var7 = new BaseVehicle(IsoWorld.instance.CurrentCell);
               var7.setScriptName(var6.getFullName());
               var7.setX(var2);
               var7.setY(var3);
               var7.setZ(var4);
               if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var7)) {
                  var7.setSquare(IsoPlayer.getInstance().getSquare());
                  var7.square.chunk.vehicles.add(var7);
                  var7.chunk = var7.square.chunk;
                  var7.addToWorld();
                  VehiclesDB2.instance.addVehicle(var7);
                  IsoChunk.addFromCheckedVehicles(var7);
               } else {
                  DebugLog.Lua.warn(var6.getName() + " not spawned, position invalid");
               }

               var2 += 4.0F;
               if (var2 > (float)(IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMaxTiles() - 5)) {
                  var2 = (float)(IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMinTiles() + 5);
                  var3 += 8.0F;
               }
            }
         }

      }

      @LuaMethod(
         name = "addPhysicsObject",
         global = true
      )
      public static BaseVehicle addPhysicsObject() {
         MPStatistic.getInstance().Bullet.Start();
         int var0 = Bullet.addPhysicsObject(getPlayer().getX(), getPlayer().getY());
         MPStatistic.getInstance().Bullet.End();
         IsoPushableObject var1 = new IsoPushableObject(IsoWorld.instance.getCell(), IsoPlayer.getInstance().getCurrentSquare(), IsoSpriteManager.instance.getSprite("trashcontainers_01_16"));
         WorldSimulation.instance.physicsObjectMap.put(var0, var1);
         return null;
      }

      @LuaMethod(
         name = "toggleVehicleRenderToTexture",
         global = true
      )
      public static void toggleVehicleRenderToTexture() {
         BaseVehicle.RENDER_TO_TEXTURE = !BaseVehicle.RENDER_TO_TEXTURE;
      }

      @LuaMethod(
         name = "reloadSoundFiles",
         global = true
      )
      public static void reloadSoundFiles() {
         ScriptManager.instance.ReloadScripts(EnumSet.of(ScriptType.Sound));
      }

      @LuaMethod(
         name = "getAnimationViewerState",
         global = true
      )
      public static AnimationViewerState getAnimationViewerState() {
         return AnimationViewerState.instance;
      }

      @LuaMethod(
         name = "getAttachmentEditorState",
         global = true
      )
      public static AttachmentEditorState getAttachmentEditorState() {
         return AttachmentEditorState.instance;
      }

      @LuaMethod(
         name = "getEditVehicleState",
         global = true
      )
      public static EditVehicleState getEditVehicleState() {
         return EditVehicleState.instance;
      }

      @LuaMethod(
         name = "getSpriteModelEditorState",
         global = true
      )
      public static SpriteModelEditorState getSpriteModelEditorState() {
         return SpriteModelEditorState.instance;
      }

      @LuaMethod(
         name = "showAnimationViewer",
         global = true
      )
      public static void showAnimationViewer() {
         IngameState.instance.showAnimationViewer = true;
      }

      @LuaMethod(
         name = "showAttachmentEditor",
         global = true
      )
      public static void showAttachmentEditor() {
         IngameState.instance.showAttachmentEditor = true;
      }

      @LuaMethod(
         name = "showChunkDebugger",
         global = true
      )
      public static void showChunkDebugger() {
         IngameState.instance.showChunkDebugger = true;
      }

      @LuaMethod(
         name = "getTileGeometryState",
         global = true
      )
      public static TileGeometryState getTileGeometryState() {
         return TileGeometryState.instance;
      }

      @LuaMethod(
         name = "showGlobalObjectDebugger",
         global = true
      )
      public static void showGlobalObjectDebugger() {
         IngameState.instance.showGlobalObjectDebugger = true;
      }

      @LuaMethod(
         name = "showSeamEditor",
         global = true
      )
      public static void showSeamEditor() {
         IngameState.instance.showSeamEditor = true;
      }

      @LuaMethod(
         name = "getSeamEditorState",
         global = true
      )
      public static SeamEditorState getSeamEditorState() {
         return SeamEditorState.instance;
      }

      @LuaMethod(
         name = "showSpriteModelEditor",
         global = true
      )
      public static void showSpriteModelEditor() {
         IngameState.instance.showSpriteModelEditor = true;
      }

      @LuaMethod(
         name = "showVehicleEditor",
         global = true
      )
      public static void showVehicleEditor(String var0) {
         IngameState.instance.showVehicleEditor = StringUtils.isNullOrWhitespace(var0) ? "" : var0;
      }

      @LuaMethod(
         name = "showWorldMapEditor",
         global = true
      )
      public static void showWorldMapEditor(String var0) {
         IngameState.instance.showWorldMapEditor = StringUtils.isNullOrWhitespace(var0) ? "" : var0;
      }

      @LuaMethod(
         name = "reloadVehicles",
         global = true
      )
      public static void reloadVehicles() {
         try {
            ScriptManager.instance.ReloadScripts(EnumSet.of(ScriptType.Vehicle, ScriptType.VehicleTemplate));
            BaseVehicle.LoadAllVehicleTextures();
            Iterator var0 = IsoWorld.instance.CurrentCell.vehicles.iterator();

            while(var0.hasNext()) {
               BaseVehicle var1 = (BaseVehicle)var0.next();
               var1.scriptReloaded();
            }
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }

      }

      @LuaMethod(
         name = "reloadEngineRPM",
         global = true
      )
      public static void reloadEngineRPM() {
         try {
            ScriptManager.instance.ReloadScripts(EnumSet.of(ScriptType.VehicleEngineRPM));
         } catch (Exception var1) {
            ExceptionLogger.logException(var1);
         }

      }

      @LuaMethod(
         name = "reloadXui",
         global = true
      )
      public static void reloadXui() {
         try {
            ScriptManager.instance.ReloadScripts(XuiManager.XuiScriptTypes);
         } catch (Exception var1) {
            ExceptionLogger.logException(var1);
         }

      }

      @LuaMethod(
         name = "reloadScripts",
         global = true
      )
      public static void reloadScripts(ScriptType var0) {
         try {
            if (XuiManager.XuiScriptTypes.contains(var0)) {
               reloadXui();
               return;
            }

            if (var0 == ScriptType.Vehicle || var0 == ScriptType.VehicleTemplate) {
               reloadVehicles();
               return;
            }

            if (var0 == ScriptType.Entity || var0 == ScriptType.EntityTemplate) {
               ScriptManager.instance.ReloadScripts(EnumSet.of(ScriptType.Entity, ScriptType.EntityTemplate));
               return;
            }

            ScriptManager.instance.ReloadScripts(var0);
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }

      }

      @LuaMethod(
         name = "reloadEntityScripts",
         global = true
      )
      public static void reloadEntityScripts() {
         try {
            EnumSet var0 = EnumSet.noneOf(ScriptType.class);
            var0.addAll(XuiManager.XuiScriptTypes);
            var0.add(ScriptType.EntityTemplate);
            var0.add(ScriptType.Entity);
            var0.add(ScriptType.ItemConfig);
            var0.add(ScriptType.ItemFilter);
            var0.add(ScriptType.CraftRecipe);
            var0.add(ScriptType.FluidFilter);
            var0.add(ScriptType.StringList);
            var0.add(ScriptType.EnergyDefinition);
            var0.add(ScriptType.FluidDefinition);
            var0.add(ScriptType.Item);
            DebugLog.General.println("Reloading entity related scripts: " + var0);
            ScriptManager.instance.ReloadScripts(var0);
         } catch (Exception var1) {
            ExceptionLogger.logException(var1);
         }

      }

      @LuaMethod(
         name = "reloadEntitiesDebug",
         global = true
      )
      public static void reloadEntitiesDebug() {
         try {
            GameEntityManager.reloadDebug();
         } catch (Exception var1) {
            ExceptionLogger.logException(var1);
         }

      }

      @LuaMethod(
         name = "reloadEntityDebug",
         global = true
      )
      public static void reloadEntityDebug(GameEntity var0) {
         try {
            GameEntityManager.reloadDebugEntity(var0);
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }

      }

      @LuaMethod(
         name = "reloadEntityFromScriptDebug",
         global = true
      )
      public static void reloadEntityFromScriptDebug(GameEntity var0) {
         try {
            GameEntityManager.reloadEntityFromScriptDebug(var0);
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }

      }

      @LuaMethod(
         name = "getIsoEntitiesDebug",
         global = true
      )
      public static ArrayList<GameEntity> getIsoEntitiesDebug() {
         try {
            return GameEntityManager.getIsoEntitiesDebug();
         } catch (Exception var1) {
            var1.printStackTrace();
            return null;
         }
      }

      @LuaMethod(
         name = "proceedPM",
         global = true
      )
      public static String proceedPM(String var0) {
         var0 = var0.trim();
         String var1 = null;
         String var2 = null;
         Matcher var3 = Pattern.compile("(\"[^\"]*\\s+[^\"]*\"|[^\"]\\S*)\\s(.+)").matcher(var0);
         if (var3.matches()) {
            var1 = var3.group(1);
            var2 = var3.group(2);
            var1 = var1.replaceAll("\"", "");
            ChatManager.getInstance().sendWhisperMessage(var1, var2);
            return var1;
         } else {
            ChatManager.getInstance().addMessage("Error", getText("IGUI_Commands_Whisper"));
            return "";
         }
      }

      @LuaMethod(
         name = "processSayMessage",
         global = true
      )
      public static void processSayMessage(String var0) {
         if (var0 != null && !var0.isEmpty()) {
            var0 = var0.trim();
            ChatManager.getInstance().sendMessageToChat(ChatType.say, var0);
         }
      }

      @LuaMethod(
         name = "processGeneralMessage",
         global = true
      )
      public static void processGeneralMessage(String var0) {
         if (var0 != null && !var0.isEmpty()) {
            var0 = var0.trim();
            ChatManager.getInstance().sendMessageToChat(ChatType.general, var0);
         }
      }

      @LuaMethod(
         name = "processShoutMessage",
         global = true
      )
      public static void processShoutMessage(String var0) {
         if (var0 != null && !var0.isEmpty()) {
            var0 = var0.trim();
            ChatManager.getInstance().sendMessageToChat(ChatType.shout, var0);
         }
      }

      @LuaMethod(
         name = "proceedFactionMessage",
         global = true
      )
      public static void ProceedFactionMessage(String var0) {
         if (var0 != null && !var0.isEmpty()) {
            var0 = var0.trim();
            ChatManager.getInstance().sendMessageToChat(ChatType.faction, var0);
         }
      }

      @LuaMethod(
         name = "processSafehouseMessage",
         global = true
      )
      public static void ProcessSafehouseMessage(String var0) {
         if (var0 != null && !var0.isEmpty()) {
            var0 = var0.trim();
            ChatManager.getInstance().sendMessageToChat(ChatType.safehouse, var0);
         }
      }

      @LuaMethod(
         name = "processAdminChatMessage",
         global = true
      )
      public static void ProcessAdminChatMessage(String var0) {
         if (var0 != null && !var0.isEmpty()) {
            var0 = var0.trim();
            ChatManager.getInstance().sendMessageToChat(ChatType.admin, var0);
         }
      }

      @LuaMethod(
         name = "showWrongChatTabMessage",
         global = true
      )
      public static void showWrongChatTabMessage(int var0, int var1, String var2) {
         String var3 = ChatManager.getInstance().getTabName((short)var0);
         String var4 = ChatManager.getInstance().getTabName((short)var1);
         String var5 = Translator.getText("UI_chat_wrong_tab", var3, var4, var2);
         ChatManager.getInstance().showServerChatMessage(var5);
      }

      @LuaMethod(
         name = "focusOnTab",
         global = true
      )
      public static void focusOnTab(Short var0) {
         ChatManager.getInstance().focusOnTab(var0);
      }

      @LuaMethod(
         name = "updateChatSettings",
         global = true
      )
      public static void updateChatSettings(String var0, boolean var1, boolean var2) {
         ChatManager.getInstance().updateChatSettings(var0, var1, var2);
      }

      @LuaMethod(
         name = "checkPlayerCanUseChat",
         global = true
      )
      public static Boolean checkPlayerCanUseChat(String var0) {
         ChatType var1;
         switch (var0.trim()) {
            case "/all":
               var1 = ChatType.general;
               break;
            case "/a":
            case "/admin":
               var1 = ChatType.admin;
               break;
            case "/s":
            case "/say":
               var1 = ChatType.say;
               break;
            case "/y":
            case "/yell":
               var1 = ChatType.shout;
               break;
            case "/f":
            case "/faction":
               var1 = ChatType.faction;
               break;
            case "/sh":
            case "/safehouse":
               var1 = ChatType.safehouse;
               break;
            case "/w":
            case "/whisper":
               var1 = ChatType.whisper;
               break;
            case "/radio":
            case "/r":
               var1 = ChatType.radio;
               break;
            default:
               var1 = ChatType.notDefined;
               DebugLog.Lua.warn("Chat command not found");
         }

         return ChatManager.getInstance().isPlayerCanUseChat(var1);
      }

      @LuaMethod(
         name = "reloadVehicleTextures",
         global = true
      )
      public static void reloadVehicleTextures(String var0) {
         VehicleScript var1 = ScriptManager.instance.getVehicle(var0);
         if (var1 == null) {
            DebugLog.Lua.warn("no such vehicle script");
         } else {
            for(int var2 = 0; var2 < var1.getSkinCount(); ++var2) {
               VehicleScript.Skin var3 = var1.getSkin(var2);
               if (var3.texture != null) {
                  Texture.reload("media/textures/" + var3.texture + ".png");
               }

               if (var3.textureRust != null) {
                  Texture.reload("media/textures/" + var3.textureRust + ".png");
               }

               if (var3.textureMask != null) {
                  Texture.reload("media/textures/" + var3.textureMask + ".png");
               }

               if (var3.textureLights != null) {
                  Texture.reload("media/textures/" + var3.textureLights + ".png");
               }

               if (var3.textureDamage1Overlay != null) {
                  Texture.reload("media/textures/" + var3.textureDamage1Overlay + ".png");
               }

               if (var3.textureDamage1Shell != null) {
                  Texture.reload("media/textures/" + var3.textureDamage1Shell + ".png");
               }

               if (var3.textureDamage2Overlay != null) {
                  Texture.reload("media/textures/" + var3.textureDamage2Overlay + ".png");
               }

               if (var3.textureDamage2Shell != null) {
                  Texture.reload("media/textures/" + var3.textureDamage2Shell + ".png");
               }

               if (var3.textureShadow != null) {
                  Texture.reload("media/textures/" + var3.textureShadow + ".png");
               }
            }

         }
      }

      @LuaMethod(
         name = "useStaticErosionRand",
         global = true
      )
      public static void useStaticErosionRand(boolean var0) {
         ErosionData.staticRand = var0;
      }

      @LuaMethod(
         name = "getClimateManager",
         global = true
      )
      public static ClimateManager getClimateManager() {
         return ClimateManager.getInstance();
      }

      @LuaMethod(
         name = "getRagdollSettingsManager",
         global = true
      )
      public static RagdollSettingsManager getRagdollSettingsManager() {
         return RagdollSettingsManager.getInstance();
      }

      @LuaMethod(
         name = "getClimateMoon",
         global = true
      )
      public static ClimateMoon getClimateMoon() {
         return ClimateMoon.getInstance();
      }

      @LuaMethod(
         name = "getWorldMarkers",
         global = true
      )
      public static WorldMarkers getWorldMarkers() {
         return WorldMarkers.instance;
      }

      @LuaMethod(
         name = "getIsoMarkers",
         global = true
      )
      public static IsoMarkers getIsoMarkers() {
         return IsoMarkers.instance;
      }

      @LuaMethod(
         name = "getErosion",
         global = true
      )
      public static ErosionMain getErosion() {
         return ErosionMain.getInstance();
      }

      @LuaMethod(
         name = "getAllOutfits",
         global = true
      )
      public static ArrayList<String> getAllOutfits(boolean var0) {
         ArrayList var1 = new ArrayList();
         ModelManager.instance.create();
         if (OutfitManager.instance == null) {
            return var1;
         } else {
            ArrayList var2 = var0 ? OutfitManager.instance.m_FemaleOutfits : OutfitManager.instance.m_MaleOutfits;
            Iterator var3 = var2.iterator();

            while(var3.hasNext()) {
               Outfit var4 = (Outfit)var3.next();
               var1.add(var4.m_Name);
            }

            Collections.sort(var1);
            return var1;
         }
      }

      @LuaMethod(
         name = "getAllVehicles",
         global = true
      )
      public static ArrayList<String> getAllVehicles() {
         return (ArrayList)ScriptManager.instance.getAllVehicleScripts().stream().map(VehicleScript::getFullName).sorted().collect(Collectors.toCollection(ArrayList::new));
      }

      @LuaMethod(
         name = "getAllHairStyles",
         global = true
      )
      public static ArrayList<String> getAllHairStyles(boolean var0) {
         ArrayList var1 = new ArrayList();
         if (HairStyles.instance == null) {
            return var1;
         } else {
            ArrayList var2 = new ArrayList(var0 ? HairStyles.instance.m_FemaleStyles : HairStyles.instance.m_MaleStyles);
            var2.sort((var0x, var1x) -> {
               if (var0x.name.isEmpty()) {
                  return -1;
               } else if (var1x.name.isEmpty()) {
                  return 1;
               } else {
                  String var2 = getText("IGUI_Hair_" + var0x.name);
                  String var3 = getText("IGUI_Hair_" + var1x.name);
                  return var2.compareTo(var3);
               }
            });
            Iterator var3 = var2.iterator();

            while(var3.hasNext()) {
               HairStyle var4 = (HairStyle)var3.next();
               var1.add(var4.name);
            }

            return var1;
         }
      }

      @LuaMethod(
         name = "getHairStylesInstance",
         global = true
      )
      public static HairStyles getHairStylesInstance() {
         return HairStyles.instance;
      }

      @LuaMethod(
         name = "getBeardStylesInstance",
         global = true
      )
      public static BeardStyles getBeardStylesInstance() {
         return BeardStyles.instance;
      }

      @LuaMethod(
         name = "getAllBeardStyles",
         global = true
      )
      public static ArrayList<String> getAllBeardStyles() {
         ArrayList var0 = new ArrayList();
         if (BeardStyles.instance == null) {
            return var0;
         } else {
            ArrayList var1 = new ArrayList(BeardStyles.instance.m_Styles);
            var1.sort((var0x, var1x) -> {
               if (var0x.name.isEmpty()) {
                  return -1;
               } else if (var1x.name.isEmpty()) {
                  return 1;
               } else {
                  String var2 = getText("IGUI_Beard_" + var0x.name);
                  String var3 = getText("IGUI_Beard_" + var1x.name);
                  return var2.compareTo(var3);
               }
            });
            Iterator var2 = var1.iterator();

            while(var2.hasNext()) {
               BeardStyle var3 = (BeardStyle)var2.next();
               var0.add(var3.name);
            }

            return var0;
         }
      }

      @LuaMethod(
         name = "getVoiceStylesInstance",
         global = true
      )
      public static VoiceStyles getVoiceStylesInstance() {
         return VoiceStyles.instance;
      }

      @LuaMethod(
         name = "getAllVoiceStyles",
         global = true
      )
      public static ArrayList<VoiceStyle> getAllVoiceStyles() {
         ArrayList var0 = new ArrayList();
         if (VoiceStyles.instance == null) {
            return var0;
         } else {
            ArrayList var1 = new ArrayList(VoiceStyles.instance.m_Styles);
            var1.sort((var0x, var1x) -> {
               if (var0x.name.isEmpty()) {
                  return -1;
               } else if (var1x.name.isEmpty()) {
                  return 1;
               } else {
                  String var2 = getText("IGUI_Voice_" + var0x.name);
                  String var3 = getText("IGUI_Voice_" + var1x.name);
                  return var2.compareTo(var3);
               }
            });
            Iterator var2 = var1.iterator();

            while(var2.hasNext()) {
               VoiceStyle var3 = (VoiceStyle)var2.next();
               if (!var3.name.isEmpty()) {
                  var0.add(var3);
               }
            }

            return var0;
         }
      }

      @LuaMethod(
         name = "getAllItemsForBodyLocation",
         global = true
      )
      public static KahluaTable getAllItemsForBodyLocation(String var0) {
         KahluaTable var1 = LuaManager.platform.newTable();
         if (StringUtils.isNullOrWhitespace(var0)) {
            return var1;
         } else {
            int var2 = 1;
            ArrayList var3 = ScriptManager.instance.getAllItems();
            Iterator var4 = var3.iterator();

            while(true) {
               Item var5;
               do {
                  do {
                     if (!var4.hasNext()) {
                        return var1;
                     }

                     var5 = (Item)var4.next();
                  } while(StringUtils.isNullOrWhitespace(var5.getClothingItem()));
               } while(!var0.equals(var5.getBodyLocation()) && !var0.equals(var5.CanBeEquipped));

               var1.rawset(var2++, var5.getFullName());
            }
         }
      }

      @LuaMethod(
         name = "getAllDecalNamesForItem",
         global = true
      )
      public static ArrayList<String> getAllDecalNamesForItem(InventoryItem var0) {
         ArrayList var1 = new ArrayList();
         if (var0 != null && ClothingDecals.instance != null) {
            ClothingItem var2 = var0.getClothingItem();
            if (var2 == null) {
               return var1;
            } else {
               String var3 = var2.getDecalGroup();
               if (StringUtils.isNullOrWhitespace(var3)) {
                  return var1;
               } else {
                  ClothingDecalGroup var4 = ClothingDecals.instance.FindGroup(var3);
                  if (var4 == null) {
                     return var1;
                  } else {
                     var4.getDecals(var1);
                     return var1;
                  }
               }
            }
         } else {
            return var1;
         }
      }

      @LuaMethod(
         name = "screenZoomIn",
         global = true
      )
      public void screenZoomIn() {
      }

      @LuaMethod(
         name = "screenZoomOut",
         global = true
      )
      public void screenZoomOut() {
      }

      @LuaMethod(
         name = "addSound",
         global = true
      )
      public void addSound(IsoObject var1, int var2, int var3, int var4, int var5, int var6) {
         WorldSoundManager.instance.addSound(var1, var2, var3, var4, var5, var6);
      }

      @LuaMethod(
         name = "sendPlaySound",
         global = true
      )
      public void sendPlaySound(String var1, boolean var2, IsoMovingObject var3) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, (float)((int)var3.getX()), (float)((int)var3.getY()), var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "sendAddXp",
         global = true
      )
      public void sendAddXp(IsoPlayer var1, PerkFactory.Perk var2, float var3, boolean var4) {
         if (GameClient.bClient && var1.isExistInTheWorld()) {
            GameClient.instance.sendAddXp(var1, var2, var3, var4);
         } else if (!GameServer.bServer) {
            var1.getXp().AddXP(var2, var3, var4);
         }

      }

      @LuaMethod(
         name = "sendIconFound",
         global = true
      )
      public void sendIconFound(IsoPlayer var1, String var2, float var3) {
         if (GameClient.bClient) {
            GameClient.sendForageItemFound(var1, var2, var3);
         } else {
            LuaEventManager.triggerEvent("OnItemFound", var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "addXpNoMultiplier",
         global = true
      )
      public void addXpNoMultiplier(IsoPlayer var1, PerkFactory.Perk var2, float var3) {
         if (var1.isExistInTheWorld()) {
            if (GameServer.bServer) {
               GameServer.addXp(var1, var2, var3, true);
            } else if (!GameClient.bClient) {
               var1.getXp().AddXP(var2, var3, true, false, false);
            }

         }
      }

      @LuaMethod(
         name = "addXp",
         global = true
      )
      public void addXp(IsoPlayer var1, PerkFactory.Perk var2, float var3) {
         if (var1.isExistInTheWorld()) {
            if (GameServer.bServer) {
               GameServer.addXp(var1, var2, var3);
            } else if (!GameClient.bClient) {
               var1.getXp().AddXP(var2, var3);
            }

         }
      }

      @LuaMethod(
         name = "addXpMultiplier",
         global = true
      )
      public void addXpMultiplier(IsoPlayer var1, PerkFactory.Perk var2, float var3, int var4, int var5) {
         if (var1.isExistInTheWorld()) {
            if (GameServer.bServer) {
               GameServer.addXpMultiplier(var1, var2, var3, var4, var5);
            } else if (!GameClient.bClient) {
               var1.getXp().addXpMultiplier(var2, var3, var4, var5);
            }

         }
      }

      @LuaMethod(
         name = "syncBodyPart",
         global = true
      )
      public void syncBodyPart(BodyPart var1, long var2) {
         if (GameServer.bServer && var1.getParentChar() instanceof IsoPlayer) {
            IsoPlayer var4 = (IsoPlayer)var1.getParentChar();
            INetworkPacket.send(var4, PacketTypes.PacketType.BodyPartSync, var1, var2);
         }

      }

      @LuaMethod(
         name = "syncPlayerStats",
         global = true
      )
      public void syncPlayerStats(IsoPlayer var1, int var2) {
         if (GameServer.bServer && var1.isExistInTheWorld()) {
            INetworkPacket.send(var1, PacketTypes.PacketType.SyncPlayerStats, var1, var2);
         }

      }

      @LuaMethod(
         name = "SyncXp",
         global = true
      )
      public void SyncXp(IsoPlayer var1) {
         if (GameClient.bClient) {
            GameClient.instance.sendSyncXp(var1);
         }

      }

      @LuaMethod(
         name = "checkServerName",
         global = true
      )
      public String checkServerName(String var1) {
         String var2 = ProfanityFilter.getInstance().validateString(var1, true, true, true);
         return !StringUtils.isNullOrEmpty(var2) ? Translator.getText("UI_BadWordCheck", var2) : null;
      }

      @LuaMethod(
         name = "Render3DItem",
         global = true
      )
      public void Render3DItem(InventoryItem var1, IsoGridSquare var2, float var3, float var4, float var5, float var6) {
         if (var1 != null && var2 != null) {
            ItemModelRenderer.RenderStatus var7 = WorldItemModelDrawer.renderMain(var1, var2, var2, var3, var4, var5, 0.0F, var6, true);
            if (var7 != ItemModelRenderer.RenderStatus.Loading && var7 != ItemModelRenderer.RenderStatus.Ready) {
               String var8 = var1.getTex().getName();
               if (var1.isUseWorldItem()) {
                  var8 = var1.getWorldTexture();
               }

               Texture var9;
               try {
                  var9 = Texture.getSharedTexture(var8);
                  if (var9 == null) {
                     var8 = var1.getTex().getName();
                  }
               } catch (Exception var19) {
                  var8 = "media/inventory/world/WItem_Sack.png";
               }

               var9 = Texture.getSharedTexture(var8);
               if (var9 != null) {
                  float var10 = 1.0F;
                  float var11 = 1.0F;
                  float var12;
                  float var13;
                  float var14;
                  float var15;
                  if (var1.getScriptItem() == null) {
                     var12 = (float)var9.getWidthOrig();
                     var13 = (float)var9.getHeightOrig();
                     var14 = (float)(16 * Core.TileScale);
                     var15 = (float)(16 * Core.TileScale);
                     if (var12 > 0.0F && var13 > 0.0F && var14 > 0.0F && var15 > 0.0F) {
                        float var16 = var15 * var12 / var13;
                        float var17 = var14 * var13 / var12;
                        boolean var18 = var16 <= var14;
                        if (var18) {
                           var14 = var16;
                        } else {
                           var15 = var17;
                        }

                        var10 = var14 / var12;
                        var11 = var15 / var13;
                     }
                  } else {
                     float var10001 = (float)Core.TileScale;
                     var10 = var11 = var1.getScriptItem().ScaleWorldIcon * (var10001 / 2.0F);
                  }

                  var12 = IsoUtils.XToScreen(var3, var4, var5, 0) - IsoCamera.frameState.OffX;
                  var13 = IsoUtils.YToScreen(var3, var4, var5, 0) - IsoCamera.frameState.OffY;
                  var14 = (float)var9.getWidthOrig() * var10 / 2.0F;
                  var15 = (float)var9.getHeightOrig() * var11 * 3.0F / 4.0F;
                  if (PerformanceSettings.FBORenderChunk) {
                     SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
                     IndieGL.glDepthMask(false);
                     IndieGL.enableDepthTest();
                     IndieGL.glDepthFunc(515);
                     TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var3 + 0.25F, var4 + 0.25F, var5).depthStart * 2.0F - 1.0F;
                  }

                  var9.render(var12 - var14, var13 - var15, (float)var9.getWidth(), (float)var9.getHeight(), 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
               }
            }
         }
      }

      @LuaMethod(
         name = "getContainerOverlays",
         global = true
      )
      public ContainerOverlays getContainerOverlays() {
         return ContainerOverlays.instance;
      }

      @LuaMethod(
         name = "getTileOverlays",
         global = true
      )
      public TileOverlays getTileOverlays() {
         return TileOverlays.instance;
      }

      @LuaMethod(
         name = "NewMapBinaryFile",
         global = true
      )
      public void NewMapBinaryFile(String var1) throws IOException {
         switch (var1) {
            case "TEST":
               NewMapBinaryFile.SpawnBasement("basement1", PZMath.fastfloor(IsoPlayer.getInstance().getX()), PZMath.fastfloor(IsoPlayer.getInstance().getY()));
            default:
         }
      }

      @LuaMethod(
         name = "getAverageFPS",
         global = true
      )
      public Double getAverageFSP() {
         float var1 = GameWindow.averageFPS;
         if (!PerformanceSettings.instance.isFramerateUncapped()) {
            var1 = Math.min(var1, (float)PerformanceSettings.getLockFPS());
         }

         return BoxedStaticValues.toDouble((double)PZMath.fastfloor(var1));
      }

      @LuaMethod(
         name = "getCPUTime",
         global = true
      )
      public long getCPUTime() {
         return GameWindow.getUpdateTime() / 1000000L;
      }

      @LuaMethod(
         name = "getGPUTime",
         global = true
      )
      public long getGPUTime() {
         return RenderThread.getRenderTime() / 1000000L;
      }

      @LuaMethod(
         name = "getCPUWait",
         global = true
      )
      public long getCPUWait() {
         return SpriteRenderer.getWaitTime() / 1000000L;
      }

      @LuaMethod(
         name = "getGPUWait",
         global = true
      )
      public long getGPUWait() {
         return RenderThread.getWaitTime() / 1000000L;
      }

      @LuaMethod(
         name = "getServerFPS",
         global = true
      )
      public int getServerFPS() {
         return 10;
      }

      @LuaMethod(
         name = "createItemTransaction",
         global = true
      )
      public static byte createItemTransaction(IsoPlayer var0, InventoryItem var1, ItemContainer var2, ItemContainer var3) {
         return GameClient.bClient ? TransactionManager.createItemTransaction(var0, var1, var2, var3) : 0;
      }

      /** @deprecated */
      @Deprecated
      @LuaMethod(
         name = "createItemTransactionWithPosData",
         global = true
      )
      public static byte createItemTransactionWithPosData(IsoPlayer var0, InventoryItem var1, ItemContainer var2, ItemContainer var3, String var4, float var5, float var6, float var7) {
         IsoDirections var8 = var4 == null ? IsoDirections.N : IsoDirections.valueOf(var4);
         return GameClient.bClient ? TransactionManager.createItemTransaction(var0, var1, var2, var3, var8, var5, var6, var7) : 0;
      }

      @LuaMethod(
         name = "changeItemTypeTransaction",
         global = true
      )
      public static byte changeItemTypeTransaction(IsoPlayer var0, InventoryItem var1, String var2) {
         return GameClient.bClient && var1 != null && !StringUtils.isNullOrEmpty(var2) ? TransactionManager.changeItemTypeTransaction(var0, var1, var1.getContainer(), var1.getContainer(), var2) : 0;
      }

      @LuaMethod(
         name = "removeItemTransaction",
         global = true
      )
      public static void removeItemTransaction(byte var0, boolean var1) {
         if (GameClient.bClient) {
            TransactionManager.removeItemTransaction(var0, var1);
         }

      }

      @LuaMethod(
         name = "isItemTransactionConsistent",
         global = true
      )
      public static boolean isItemTransactionConsistent(InventoryItem var0, ItemContainer var1, ItemContainer var2, String var3) {
         if (GameClient.bClient) {
            int var4 = -1;
            if (var0 != null) {
               var4 = var0.id;
            }

            if (var1.getType().equals("floor") && var0.getWorldItem() != null) {
               var4 = -1;
            }

            return TransactionManager.isConsistent(var4, var1, var2, var3, (ItemTransactionPacket)null) == 0;
         } else {
            return true;
         }
      }

      @LuaMethod(
         name = "isItemTransactionDone",
         global = true
      )
      public static boolean isItemTransactionDone(byte var0) {
         return GameClient.bClient && var0 != 0 ? TransactionManager.isDone(var0) : true;
      }

      @LuaMethod(
         name = "isItemTransactionRejected",
         global = true
      )
      public static boolean isItemTransactionRejected(byte var0) {
         return GameClient.bClient && var0 != 0 ? TransactionManager.isRejected(var0) : true;
      }

      @LuaMethod(
         name = "getItemTransactionDuration",
         global = true
      )
      public static int getItemTransactionDuration(byte var0) {
         return GameClient.bClient && var0 != 0 ? TransactionManager.getDuration(var0) / 20 : -1;
      }

      @LuaMethod(
         name = "isActionDone",
         global = true
      )
      public static boolean isActionDone(byte var0) {
         return GameClient.bClient && var0 != 0 ? ActionManager.isDone(var0) : true;
      }

      @LuaMethod(
         name = "isActionRejected",
         global = true
      )
      public static boolean isActionRejected(byte var0) {
         return GameClient.bClient && var0 != 0 ? ActionManager.isRejected(var0) : true;
      }

      @LuaMethod(
         name = "getActionDuration",
         global = true
      )
      public static int getActionDuration(byte var0) {
         return GameClient.bClient && var0 != 0 ? ActionManager.getDuration(var0) / 20 : -1;
      }

      @LuaMethod(
         name = "removeAction",
         global = true
      )
      public static void removeAction(byte var0, boolean var1) {
         if (GameClient.bClient) {
            ActionManager.remove(var0, var1);
         }

      }

      @LuaMethod(
         name = "emulateAnimEvent",
         global = true
      )
      public static void emulateAnimEvent(NetTimedAction var0, long var1, String var3, String var4) {
         if (GameServer.bServer) {
            AnimEventEmulator.getInstance().create(var0, var1, false, var3, var4);
         }

      }

      @LuaMethod(
         name = "emulateAnimEventOnce",
         global = true
      )
      public static void emulateAnimEventOnce(NetTimedAction var0, long var1, String var3, String var4) {
         if (GameServer.bServer) {
            AnimEventEmulator.getInstance().create(var0, var1, true, var3, var4);
         }

      }

      @LuaMethod(
         name = "showDebugInfoInChat",
         global = true
      )
      public static void showDebugInfoInChat(String var0) {
         if (GameClient.bClient && DebugLog.isLogEnabled(DebugType.Action, LogSeverity.Trace)) {
            ChatManager.getInstance().showServerChatMessage(var0);
         }

      }

      @LuaMethod(
         name = "createBuildAction",
         global = true
      )
      public static byte createBuildAction(IsoPlayer var0, float var1, float var2, float var3, boolean var4, String var5, KahluaTable var6) {
         if (GameClient.bClient) {
            String var7 = var6.getMetatable().getString("Type");
            if (GameClient.bClient && DebugLog.isLogEnabled(DebugType.Action, LogSeverity.Trace)) {
               ChatManager.getInstance().showServerChatMessage(" BUILD createBuildAction objectType:" + var7 + " spriteName:" + var5 + " north:" + (var4 ? "true" : "false"));
            }

            return ActionManager.getInstance().createBuildAction(var0, var1, var2, var3, var4, var5, var6);
         } else {
            return 0;
         }
      }

      @LuaMethod(
         name = "startFishingAction",
         global = true
      )
      public static byte startFishingAction(IsoPlayer var0, InventoryItem var1, IsoGridSquare var2, KahluaTable var3) {
         return GameClient.bClient ? ActionManager.getInstance().createFishingAction(var0, var1, var2, var3) : 0;
      }

      /** @deprecated */
      @LuaMethod(
         name = "syncInventory",
         global = true
      )
      @Deprecated
      public void syncInventory(IsoPlayer var1) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SyncInventory, var1);
         }

      }

      @LuaMethod(
         name = "syncItemModData",
         global = true
      )
      public void syncItemModData(IsoPlayer var1, InventoryItem var2) {
         if (GameServer.bServer) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncItemModData, (float)((int)var1.getX()), (float)((int)var1.getY()), var2);
         }

      }

      @LuaMethod(
         name = "syncItemFields",
         global = true
      )
      public void syncItemFields(IsoPlayer var1, InventoryItem var2) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, var1, var2);
         } else if (GameServer.bServer) {
            INetworkPacket.send(var1, PacketTypes.PacketType.SyncItemFields, var1, var2);
         }

      }

      @LuaMethod(
         name = "syncHandWeaponFields",
         global = true
      )
      public void syncHandWeaponFields(IsoPlayer var1, HandWeapon var2) {
         if (GameServer.bServer) {
            INetworkPacket.send(var1, PacketTypes.PacketType.SyncHandWeaponFields, var1, var2);
         }

      }

      @LuaMethod(
         name = "getPickedUpFish",
         global = true
      )
      public InventoryItem getPickedUpFish(IsoPlayer var1) {
         return GameServer.bServer ? FishingAction.getPickedUpFish(var1) : null;
      }

      @LuaMethod(
         name = "sendAddItemToContainer",
         global = true
      )
      public static void sendAddItemToContainer(ItemContainer var0, InventoryItem var1) {
         if (GameServer.bServer) {
            GameServer.sendAddItemToContainer(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendAddItemsToContainer",
         global = true
      )
      public static void sendAddItemsToContainer(ItemContainer var0, ArrayList<InventoryItem> var1) {
         if (GameServer.bServer) {
            GameServer.sendAddItemsToContainer(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendReplaceItemInContainer",
         global = true
      )
      public static void sendReplaceItemInContainer(ItemContainer var0, InventoryItem var1, InventoryItem var2) {
         if (GameServer.bServer) {
            GameServer.sendReplaceItemInContainer(var0, var1, var2);
         }

      }

      @LuaMethod(
         name = "sendRemoveItemFromContainer",
         global = true
      )
      public static void sendRemoveItemFromContainer(ItemContainer var0, InventoryItem var1) {
         if (GameServer.bServer) {
            GameServer.sendRemoveItemFromContainer(var0, var1);
         }

      }

      @LuaMethod(
         name = "sendRemoveItemsFromContainer",
         global = true
      )
      public static void sendRemoveItemsFromContainer(ItemContainer var0, ArrayList<InventoryItem> var1) {
         if (GameServer.bServer) {
            GameServer.sendRemoveItemsFromContainer(var0, var1);
         }

      }

      @LuaMethod(
         name = "replaceItemInContainer",
         global = true
      )
      public static void replaceItemInContainer(ItemContainer var0, InventoryItem var1, InventoryItem var2) {
         if (!GameServer.bServer && !GameClient.bClient) {
            IsoPlayer var3 = (IsoPlayer)var0.getParent();
            if (var3 != null) {
               ActionManager.getInstance().replaceObjectInQueuedActions(var3, var1, var2);
            }
         }

      }

      @LuaMethod(
         name = "getServerStatistic",
         global = true
      )
      public static KahluaTable getServerStatistic() {
         return MPStatistic.getInstance().getStatisticTableForLua();
      }

      @LuaMethod(
         name = "log",
         global = true
      )
      public static void log(DebugType var0, String var1) {
         DebugLog.getOrCreateDebugLogStream(var0).debugln(var1);
      }

      @LuaMethod(
         name = "setServerStatisticEnable",
         global = true
      )
      public static void setServerStatisticEnable(boolean var0) {
         if (GameClient.bClient) {
            GameClient.setServerStatisticEnable(var0);
         }

      }

      @LuaMethod(
         name = "getServerStatisticEnable",
         global = true
      )
      public static boolean getServerStatisticEnable() {
         return GameClient.bClient ? GameClient.getServerStatisticEnable() : false;
      }

      @LuaMethod(
         name = "checkModsNeedUpdate",
         global = true
      )
      public static void checkModsNeedUpdate(UdpConnection var0) {
         DebugLog.Mod.println("CheckModsNeedUpdate: Checking...");
         if (SteamUtils.isSteamModeEnabled() && isServer()) {
            ArrayList var1 = getSteamWorkshopItemIDs();
            new ItemQueryJava(var1, var0);
         }

      }

      @LuaMethod(
         name = "getSearchMode",
         global = true
      )
      public static SearchMode getSearchMode() {
         return SearchMode.getInstance();
      }

      @LuaMethod(
         name = "transmitBigWaterSplash",
         global = true
      )
      public static void transmitBigWaterSplash(int var0, int var1, float var2, float var3) {
         if (GameClient.bClient) {
            GameClient.sendBigWaterSplash(var0, var1, var2, var3);
         }

         if (GameServer.bServer) {
            GameServer.transmitBigWaterSplash(var0, var1, var2, var3);
         }

      }

      @LuaMethod(
         name = "addAreaHighlight",
         global = true
      )
      public static void addAreaHighlight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, float var8) {
         FBORenderAreaHighlights.getInstance().addHighlight(var0, var1, var2, var3, var4, var5, var6, var7, var8);
      }

      @LuaMethod(
         name = "timSort",
         global = true
      )
      public static void timSort(KahluaTable var0, Object var1) {
         KahluaTableImpl var2 = (KahluaTableImpl)Type.tryCastTo(var0, KahluaTableImpl.class);
         if (var2 != null && var2.len() >= 2 && var1 != null) {
            timSortComparator.comp = var1;
            Object[] var3 = var2.delegate.values().toArray();
            Arrays.sort(var3, timSortComparator);

            for(int var4 = 0; var4 < var3.length; ++var4) {
               var2.rawset(var4 + 1, var3[var4]);
               var3[var4] = null;
            }

         }
      }

      @LuaMethod(
         name = "javaListRemoveAt",
         global = true
      )
      public static Object javaListRemoveAt(List<?> var0, int var1) {
         return var0 == null ? null : var0.remove(var1);
      }

      @LuaMethod(
         name = "sendDebugStory",
         global = true
      )
      public static void sendDebugStory(IsoGridSquare var0, int var1, String var2) {
         INetworkPacket.send(PacketTypes.PacketType.DebugStory, var0, var1, var2);
      }

      public static final class LuaFileWriter {
         private final PrintWriter writer;

         public LuaFileWriter(PrintWriter var1) {
            this.writer = var1;
         }

         public void write(String var1) throws IOException {
            this.writer.write(var1);
         }

         public void writeln(String var1) throws IOException {
            this.writer.write(var1);
            this.writer.write(System.lineSeparator());
         }

         public void close() throws IOException {
            this.writer.close();
         }
      }

      private static final class ItemQuery implements ISteamWorkshopCallback {
         private LuaClosure functionObj;
         private Object arg1;
         private long handle;

         public ItemQuery(ArrayList<String> var1, LuaClosure var2, Object var3) {
            this.functionObj = var2;
            this.arg1 = var3;
            long[] var4 = new long[var1.size()];
            int var5 = 0;

            for(int var6 = 0; var6 < var1.size(); ++var6) {
               long var7 = SteamUtils.convertStringToSteamID((String)var1.get(var6));
               if (var7 != -1L) {
                  var4[var5++] = var7;
               }
            }

            this.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(var4, this);
            if (this.handle == 0L) {
               SteamWorkshop.instance.RemoveCallback(this);
               if (var3 == null) {
                  LuaManager.caller.pcall(LuaManager.thread, var2, "NotCompleted");
               } else {
                  LuaManager.caller.pcall(LuaManager.thread, var2, new Object[]{var3, "NotCompleted"});
               }
            }

         }

         public void onItemCreated(long var1, boolean var3) {
         }

         public void onItemNotCreated(int var1) {
         }

         public void onItemUpdated(boolean var1) {
         }

         public void onItemNotUpdated(int var1) {
         }

         public void onItemSubscribed(long var1) {
         }

         public void onItemNotSubscribed(long var1, int var3) {
         }

         public void onItemDownloaded(long var1) {
         }

         public void onItemNotDownloaded(long var1, int var3) {
         }

         public void onItemQueryCompleted(long var1, int var3) {
            if (var1 == this.handle) {
               SteamWorkshop.instance.RemoveCallback(this);
               ArrayList var4 = new ArrayList();

               for(int var5 = 0; var5 < var3; ++var5) {
                  SteamUGCDetails var6 = SteamWorkshop.instance.GetQueryUGCResult(var1, var5);
                  if (var6 != null) {
                     var4.add(var6);
                  }
               }

               SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
               if (this.arg1 == null) {
                  LuaManager.caller.pcall(LuaManager.thread, this.functionObj, new Object[]{"Completed", var4});
               } else {
                  LuaManager.caller.pcall(LuaManager.thread, this.functionObj, new Object[]{this.arg1, "Completed", var4});
               }

            }
         }

         public void onItemQueryNotCompleted(long var1, int var3) {
            if (var1 == this.handle) {
               SteamWorkshop.instance.RemoveCallback(this);
               SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
               if (this.arg1 == null) {
                  LuaManager.caller.pcall(LuaManager.thread, this.functionObj, "NotCompleted");
               } else {
                  LuaManager.caller.pcall(LuaManager.thread, this.functionObj, new Object[]{this.arg1, "NotCompleted"});
               }

            }
         }
      }

      private static final class ItemQueryJava implements ISteamWorkshopCallback {
         private long handle;
         private UdpConnection connection;

         public ItemQueryJava(ArrayList<String> var1, UdpConnection var2) {
            this.connection = var2;
            long[] var3 = new long[var1.size()];
            int var4 = 0;

            for(int var5 = 0; var5 < var1.size(); ++var5) {
               long var6 = SteamUtils.convertStringToSteamID((String)var1.get(var5));
               if (var6 != -1L) {
                  var3[var4++] = var6;
               }
            }

            this.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(var3, this);
            if (this.handle == 0L) {
               SteamWorkshop.instance.RemoveCallback(this);
               this.inform("CheckModsNeedUpdate: Check not completed");
            }

         }

         private void inform(String var1) {
            if (this.connection != null) {
               ChatServer.getInstance().sendMessageToServerChat(this.connection, var1);
            }

            DebugLog.Mod.println(var1);
         }

         public void onItemCreated(long var1, boolean var3) {
         }

         public void onItemNotCreated(int var1) {
         }

         public void onItemUpdated(boolean var1) {
         }

         public void onItemNotUpdated(int var1) {
         }

         public void onItemSubscribed(long var1) {
         }

         public void onItemNotSubscribed(long var1, int var3) {
         }

         public void onItemDownloaded(long var1) {
         }

         public void onItemNotDownloaded(long var1, int var3) {
         }

         public void onItemQueryCompleted(long var1, int var3) {
            if (var1 == this.handle) {
               SteamWorkshop.instance.RemoveCallback(this);

               for(int var4 = 0; var4 < var3; ++var4) {
                  SteamUGCDetails var5 = SteamWorkshop.instance.GetQueryUGCResult(var1, var4);
                  if (var5 != null) {
                     long var6 = var5.getID();
                     long var8 = SteamWorkshop.instance.GetItemState(var6);
                     if (SteamWorkshopItem.ItemState.Installed.and(var8) && SteamWorkshopItem.ItemState.NeedsUpdate.not(var8) && var5.getTimeCreated() != 0L && var5.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(var6)) {
                        var8 |= (long)SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
                     }

                     if (SteamWorkshopItem.ItemState.NeedsUpdate.and(var8)) {
                        this.inform("CheckModsNeedUpdate: Mods need update");
                        SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
                        return;
                     }
                  }
               }

               this.inform("CheckModsNeedUpdate: Mods updated");
               SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
            }
         }

         public void onItemQueryNotCompleted(long var1, int var3) {
            if (var1 == this.handle) {
               SteamWorkshop.instance.RemoveCallback(this);
               SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
               this.inform("CheckModsNeedUpdate: Check not completed");
            }
         }
      }

      private static final class TimSortComparator implements Comparator<Object> {
         Object comp;

         private TimSortComparator() {
         }

         public int compare(Object var1, Object var2) {
            if (Objects.equals(var1, var2)) {
               return 0;
            } else {
               Boolean var3 = LuaManager.thread.pcallBoolean(this.comp, var1, var2);
               return var3 == Boolean.TRUE ? -1 : 1;
            }
         }
      }
   }
}
