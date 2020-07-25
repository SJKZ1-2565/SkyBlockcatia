package com.stevekung.skyblockcatia.event.handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.stevekung.indicatia.hud.InfoUtils;
import com.stevekung.skyblockcatia.config.SBExtendedConfig;
import com.stevekung.skyblockcatia.config.SkyBlockcatiaConfig;
import com.stevekung.skyblockcatia.core.SkyBlockcatiaMod;
import com.stevekung.skyblockcatia.gui.SignSelectionList;
import com.stevekung.skyblockcatia.gui.screen.SkyBlockProfileViewerScreen;
import com.stevekung.skyblockcatia.gui.toasts.*;
import com.stevekung.skyblockcatia.gui.toasts.ToastUtils.ToastType;
import com.stevekung.skyblockcatia.handler.KeyBindingHandler;
import com.stevekung.skyblockcatia.integration.IndicatiaIntegration;
import com.stevekung.skyblockcatia.utils.TimeUtils;
import com.stevekung.skyblockcatia.utils.ToastLog;
import com.stevekung.skyblockcatia.utils.ToastMode;
import com.stevekung.skyblockcatia.utils.skyblock.*;
import com.stevekung.skyblockcatia.utils.skyblock.api.BazaarData;
import com.stevekung.skyblockcatia.utils.skyblock.api.DragonType;
import com.stevekung.stevekungslib.utils.*;
import com.stevekung.stevekungslib.utils.client.ClientUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkyBlockEventHandler
{
    private static final Pattern CUSTOM_FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-Z]");
    private static final Pattern LETTERS_NUMBERS = Pattern.compile("[^a-z A-Z:0-9/']");
    private static final Pattern VISIT_ISLAND_PATTERN = Pattern.compile("(?:\\[SkyBlock\\]|\\[SkyBlock\\] (?:\\[VIP?\\u002B{0,1}\\]|\\[MVP?\\u002B{0,2}\\]|\\[YOUTUBE\\])) (?<name>\\w+) is visiting Your Island!");
    public static final String UUID_PATTERN_STRING = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static final Pattern UUID_PATTERN = Pattern.compile("Your new API key is (?<uuid>" + SkyBlockEventHandler.UUID_PATTERN_STRING + ")");
    private static final String RANKED_PATTERN = "(?:(?:\\w)|(?:\\[VIP?\\u002B{0,1}\\]|\\[MVP?\\u002B{0,2}\\]|\\[YOUTUBE\\]) \\w)+";
    private static final Pattern CHAT_PATTERN = Pattern.compile("(?:(\\w+)|(?:\\[VIP?\\u002B{0,1}\\]|\\[MVP?\\u002B{0,2}\\]|\\[YOUTUBE\\]) (\\w+))+: (?:.)+");
    private static final Pattern PET_CARE_PATTERN = Pattern.compile("I'm currently taking care of your [\\w ]+! You can pick it up in (?:(?<day>[\\d]+) day(?:s){0,1} ){0,1}(?:(?<hour>[\\d]+) hour(?:s){0,1} ){0,1}(?:(?<minute>[\\d]+) minute(?:s){0,1} ){0,1}(?:(?<second>[\\d]+) second(?:s){0,1}).");
    private static final Pattern DRAGON_DOWN_PATTERN = Pattern.compile("\\u00A7r +\\u00A7r\\u00A76\\u00A7l(?<dragon>SUPERIOR|STRONG|YOUNG|OLD|PROTECTOR|UNSTABLE|WISE) DRAGON DOWN!\\u00a7r");
    private static final Pattern DRAGON_SPAWNED_PATTERN = Pattern.compile("\\u00A75\\u262C \\u00A7r\\u00A7d\\u00A7lThe \\u00A7r\\u00A75\\u00A7c\\u00A7l(?<dragon>Superior|Strong|Young|Unstable|Wise|Old|Protector) Dragon\\u00A7r\\u00A7d\\u00A7l has spawned!\\u00A7r");

    // Item Drop Stuff
    private static final String ITEM_PATTERN = "[\\w\\'\\u25C6\\[\\] -]+";
    private static final String DROP_PATTERN = "(?<item>(?:\\u00a7r\\u00a7[0-9a-fk-or]){0,1}(?:\\u00a7[0-9a-fk-or]){0,1}" + ITEM_PATTERN + "(?:[\\(][^\\)]" + ITEM_PATTERN + "[\\)]){0,1})";
    private static final Pattern RARE_DROP_PATTERN = Pattern.compile("\\u00a7r\\u00a76\\u00a7lRARE DROP! " + DROP_PATTERN + "(?:\\b\\u00a7r\\b){0,1} ?(?:\\u00a7r\\u00a7b\\(\\+(?<mf>[0-9]+)% Magic Find!\\)\\u00a7r){0,1}");
    private static final Pattern RARE_DROP_2_SPACE_PATTERN = Pattern.compile("\\u00a7r\\u00a7b\\u00a7lRARE DROP! \\u00a7r\\u00a77\\(" + DROP_PATTERN + "\\u00a7r\\u00a77\\)(?:\\b\\u00a7r\\b){0,1} ?(?:\\u00a7r\\u00a7b\\(\\+(?<mf>[0-9]+)% Magic Find!\\)\\u00a7r){0,1}");
    private static final Pattern RARE_DROP_WITH_BRACKET_PATTERN = Pattern.compile("(?<type>\\u00a7r\\u00a79\\u00a7lVERY RARE|\\u00a7r\\u00a75\\u00a7lVERY RARE|\\u00a7r\\u00a7d\\u00a7lCRAZY RARE) DROP!  \\u00a7r\\u00a77\\(" + DROP_PATTERN + "\\u00a7r\\u00a77\\)(?:\\b\\u00a7r\\b){0,1} ?(?:\\u00a7r\\u00a7b\\(\\+(?<mf>[0-9]+)% Magic Find!\\)\\u00a7r){0,1}");
    private static final Pattern BOSS_DROP_PATTERN = Pattern.compile("(?:(?:" + GameProfileUtils.getUsername() + ")|(?:\\[VIP?\\u002B{0,1}\\]|\\[MVP?\\u002B{0,2}\\]|\\[YOUTUBE\\]) " + GameProfileUtils.getUsername() + ") has obtained " + DROP_PATTERN + "!");

    // Dungeons
    private static final Pattern DUNGEON_QUALITY_DROP_PATTERN = Pattern.compile("You found a Top Quality Item! " + DROP_PATTERN);

    // Fish catch stuff
    private static final Pattern FISH_CATCH_PATTERN = Pattern.compile("(?<type>GOOD|GREAT) CATCH! You found a " + DROP_PATTERN + ".");
    private static final Pattern COINS_CATCH_PATTERN = Pattern.compile("(?<type>GOOD|GREAT) CATCH! You found (?<coin>[0-9,]+) Coins.");

    // Winter island stuff
    private static final Pattern COINS_GIFT_PATTERN = Pattern.compile("(?<type>COMMON|SWEET|RARE)! \\u002B(?<coin>[0-9,]+) coins gift with " + RANKED_PATTERN + "!");
    private static final Pattern SKILL_EXP_GIFT_PATTERN = Pattern.compile("(?<type>COMMON|SWEET|RARE)! \\u002B(?<exp>[0-9,]+) (?<skill>Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy)+ XP gift with " + RANKED_PATTERN + "!");
    private static final Pattern ITEM_DROP_GIFT_PATTERN = Pattern.compile("(?<type>COMMON|SWEET|RARE)! " + DROP_PATTERN + " gift with " + RANKED_PATTERN + "!");
    private static final Pattern SANTA_TIER_PATTERN = Pattern.compile("SANTA TIER! " + DROP_PATTERN + " gift with " + RANKED_PATTERN + "!");

    // Pet
    private static final Pattern PET_LEVEL_UP_PATTERN = Pattern.compile("\\u00a7r\\u00a7aYour (?<name>\\u00a7r\\u00a7[0-9a-fk-or][\\w ]+) \\u00a7r\\u00a7alevelled up to level \\u00a7r\\u00a79(?<level>\\d+)\\u00a7r\\u00a7a!\\u00a7r");
    private static final Pattern PET_DROP_PATTERN = Pattern.compile("PET DROP! " + DROP_PATTERN + " ?(?:\\(\\+(?<mf>[0-9]+)% Magic Find!\\)){0,1}");

    private static final List<String> LEFT_PARTY_MESSAGE = new ArrayList<>(Arrays.asList("You are not in a party and have been moved to the ALL channel!", "has disbanded the party!", "The party was disbanded because all invites have expired and all members have left."));
    private static final Map<String, String> RENAMED_DROP = ImmutableMap.<String, String>builder().put("\u25C6 Ice Rune", "\u25C6 Ice Rune I").build();
    public static boolean isSkyBlock = false;
    public static boolean foundSkyBlockPack;
    public static String skyBlockPackResolution = "16";
    public static SBLocation SKY_BLOCK_LOCATION = SBLocation.YOUR_ISLAND;
    public static String SKYBLOCK_AMPM = "";
    public static float dragonHealth;
    private static final List<ToastUtils.ItemDropCheck> ITEM_DROP_CHECK_LIST = new ArrayList<>();
    private List<ItemStack> previousInventory;
    private DragonType dragonType;
    private final Minecraft mc;

    public SkyBlockEventHandler()
    {
        this.mc = Minecraft.getInstance();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (this.mc.player != null)
        {
            if (event.phase == TickEvent.Phase.START)
            {
                if (this.mc.player.ticksExisted % 5 == 0)
                {
                    this.getInventoryDifference(this.mc.player.inventory.mainInventory);
                }
                if (this.mc.world != null)
                {
                    boolean found = false;
                    ScoreObjective scoreObj = this.mc.world.getScoreboard().getObjectiveInDisplaySlot(1);
                    Scoreboard scoreboard = this.mc.world.getScoreboard();
                    Collection<Score> collection = scoreboard.getSortedScores(scoreObj);
                    List<Score> list = Lists.newArrayList(collection.stream().filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#")).collect(Collectors.toList()));

                    if (list.size() > 15)
                    {
                        collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
                    }
                    else
                    {
                        collection = list;
                    }

                    for (Score score1 : collection)
                    {
                        ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(score1.getPlayerName());
                        String scoreText = CUSTOM_FORMATTING_CODE_PATTERN.matcher(ScorePlayerTeam.formatMemberName(scorePlayerTeam, new StringTextComponent(score1.getPlayerName())).getFormattedText()).replaceAll("");

                        if (scoreText.startsWith("Dragon HP: "))
                        {
                            try
                            {
                                SkyBlockEventHandler.dragonHealth = Float.valueOf(scoreText.replaceAll("[^\\d]", ""));

                                if (this.dragonType != null)
                                {
                                    //SkyBlockBossBar.healthScale = HypixelEventHandler.dragonHealth / this.dragonType.getMaxHealth();
                                }
                                break;
                            }
                            catch (Exception e) {}
                        }
                    }

                    for (Score score1 : collection)
                    {
                        ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(score1.getPlayerName());
                        String scoreText = CUSTOM_FORMATTING_CODE_PATTERN.matcher(ScorePlayerTeam.formatMemberName(scorePlayerTeam, new StringTextComponent(score1.getPlayerName())).getFormattedText()).replaceAll("");

                        if (scoreText.endsWith("am"))
                        {
                            SkyBlockEventHandler.SKYBLOCK_AMPM = " AM";
                        }
                        else if (scoreText.endsWith("pm"))
                        {
                            SkyBlockEventHandler.SKYBLOCK_AMPM = " PM";
                        }

                        for (SBLocation location : SBLocation.VALUES)
                        {
                            if (scoreText.endsWith(location.getLocation()))
                            {
                                SkyBlockEventHandler.SKY_BLOCK_LOCATION = location;
                                found = true;
                                break;
                            }
                        }
                    }

                    if (scoreObj != null)
                    {
                        SkyBlockEventHandler.isSkyBlock = CUSTOM_FORMATTING_CODE_PATTERN.matcher(scoreObj.getDisplayName().getFormattedText()).replaceAll("").contains("SKYBLOCK");
                    }
                    else
                    {
                        SkyBlockEventHandler.isSkyBlock = false;
                    }

                    if (!found)
                    {
                        SkyBlockEventHandler.SKY_BLOCK_LOCATION = SBLocation.NONE;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseClick(InputEvent.MouseInputEvent event)
    {
        if (event.getButton() == GLFW.GLFW_PRESS && event.getAction() == GLFW.GLFW_MOUSE_BUTTON_2 && this.mc.pointedEntity != null && this.mc.pointedEntity instanceof RemoteClientPlayerEntity && this.mc.player.isSneaking() && InfoUtils.INSTANCE.isHypixel() && SBExtendedConfig.INSTANCE.sneakToTradeOtherPlayerIsland)
        {
            RemoteClientPlayerEntity player = (RemoteClientPlayerEntity)this.mc.pointedEntity;
            ScoreObjective scoreObj = this.mc.world.getScoreboard().getObjectiveInDisplaySlot(1);
            Scoreboard scoreboard = this.mc.world.getScoreboard();
            Collection<Score> collection = scoreboard.getSortedScores(scoreObj);
            List<Score> list = Lists.newArrayList(collection.stream().filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#")).collect(Collectors.toList()));

            if (list.size() > 15)
            {
                collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
            }
            else
            {
                collection = list;
            }

            for (Score score1 : collection)
            {
                ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(score1.getPlayerName());
                String scoreText = CUSTOM_FORMATTING_CODE_PATTERN.matcher(ScorePlayerTeam.formatMemberName(scorePlayerTeam, new StringTextComponent(score1.getPlayerName())).getFormattedText()).replaceAll("");

                if (scoreText.endsWith("'s Island"))
                {
                    this.mc.player.sendChatMessage("/trade " + player.getName().getUnformattedComponentText());
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event)
    {
        if (event.getMessage() == null)
        {
            return;
        }

        String formattedMessage = event.getMessage().getFormattedText();
        String message = event.getMessage().getString();
        boolean cancelMessage = false;

        if (this.isHypixel())
        {
            // Common matcher
            Matcher visitIslandMatcher = SkyBlockEventHandler.VISIT_ISLAND_PATTERN.matcher(message);
            Matcher uuidMatcher = SkyBlockEventHandler.UUID_PATTERN.matcher(message);
            Matcher chatMatcher = SkyBlockEventHandler.CHAT_PATTERN.matcher(message);
            Matcher petCareMatcher = SkyBlockEventHandler.PET_CARE_PATTERN.matcher(message);
            Matcher dragonDownMatcher = SkyBlockEventHandler.DRAGON_DOWN_PATTERN.matcher(formattedMessage);
            Matcher dragonSpawnedMatcher = SkyBlockEventHandler.DRAGON_SPAWNED_PATTERN.matcher(formattedMessage);

            // Item Drop matcher
            Matcher rareDropPattern = SkyBlockEventHandler.RARE_DROP_PATTERN.matcher(formattedMessage);
            Matcher bossDropPattern = SkyBlockEventHandler.BOSS_DROP_PATTERN.matcher(message);

            // Dungeons matcher
            Matcher dungeonQualityDropPattern = SkyBlockEventHandler.DUNGEON_QUALITY_DROP_PATTERN.matcher(message);

            // Fish catch matcher
            Matcher fishCatchPattern = SkyBlockEventHandler.FISH_CATCH_PATTERN.matcher(message);
            Matcher coinsCatchPattern = SkyBlockEventHandler.COINS_CATCH_PATTERN.matcher(message);

            // Slayer Drop matcher
            Matcher rareDropBracketPattern = SkyBlockEventHandler.RARE_DROP_WITH_BRACKET_PATTERN.matcher(formattedMessage);
            Matcher rareDrop2SpaceBracketPattern = SkyBlockEventHandler.RARE_DROP_2_SPACE_PATTERN.matcher(formattedMessage);

            // Gift matcher
            Matcher coinsGiftPattern = SkyBlockEventHandler.COINS_GIFT_PATTERN.matcher(message);
            Matcher skillExpGiftPattern = SkyBlockEventHandler.SKILL_EXP_GIFT_PATTERN.matcher(message);
            Matcher itemDropGiftPattern = SkyBlockEventHandler.ITEM_DROP_GIFT_PATTERN.matcher(message);
            Matcher santaTierPattern = SkyBlockEventHandler.SANTA_TIER_PATTERN.matcher(message);

            // Pet
            Matcher petLevelUpPattern = SkyBlockEventHandler.PET_LEVEL_UP_PATTERN.matcher(formattedMessage);
            Matcher petDropPattern = SkyBlockEventHandler.PET_DROP_PATTERN.matcher(message);

            if (event.getType() == ChatType.CHAT)
            {
                if (visitIslandMatcher.matches())
                {
                    String name = visitIslandMatcher.group("name");

                    if (SBExtendedConfig.INSTANCE.visitIslandDisplayMode == ToastMode.TOAST || SBExtendedConfig.INSTANCE.visitIslandDisplayMode == ToastMode.CHAT_AND_TOAST)
                    {
                        SkyBlockEventHandler.addVisitingToast(this.mc, name);
                        ToastLog.logToast(message);
                    }
                    cancelMessage = SBExtendedConfig.INSTANCE.visitIslandDisplayMode == ToastMode.TOAST || SBExtendedConfig.INSTANCE.visitIslandDisplayMode == ToastMode.DISABLED;
                }
                else if (uuidMatcher.matches())
                {
                    SBAPIUtils.setApiKeyFromServer(uuidMatcher.group("uuid"));
                    ClientUtils.printClientMessage("Setting a new API Key!", TextFormatting.GREEN);
                }
                else if (petCareMatcher.matches())
                {
                    int day = 0;
                    int hour = 0;
                    int minute = 0;
                    int second = 0;

                    if (petCareMatcher.group("day") != null)
                    {
                        day = Integer.parseInt(petCareMatcher.group("day"));
                    }
                    if (petCareMatcher.group("hour") != null)
                    {
                        hour = Integer.parseInt(petCareMatcher.group("hour"));
                    }
                    if (petCareMatcher.group("minute") != null)
                    {
                        minute = Integer.parseInt(petCareMatcher.group("minute"));
                    }
                    if (petCareMatcher.group("second") != null)
                    {
                        second = Integer.parseInt(petCareMatcher.group("second"));
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, day);
                    calendar.add(Calendar.HOUR, hour);
                    calendar.add(Calendar.MINUTE, minute);
                    calendar.add(Calendar.SECOND, second);
                    String date1 = new SimpleDateFormat("d MMMMM yyyy", Locale.ENGLISH).format(calendar.getTime());
                    String date2 = new SimpleDateFormat("h:mm:ss a", Locale.ENGLISH).format(calendar.getTime());
                    ClientUtils.printClientMessage(JsonUtils.create("Pet take care will be finished on " + date1 + " " + date2).applyTextStyle(TextFormatting.GREEN));
                }

                if (SkyBlockcatiaMod.isIndicatiaLoaded && SkyBlockEventHandler.LEFT_PARTY_MESSAGE.stream().anyMatch(pmess -> message.equals(pmess)))
                {
                    IndicatiaIntegration.savePartyChat();
                }
                if (SBExtendedConfig.INSTANCE.leavePartyWhenLastEyePlaced && message.contains(" Brace yourselves! (8/8)"))
                {
                    this.mc.player.sendChatMessage("/p leave");
                }
                if (SBExtendedConfig.INSTANCE.automaticOpenMaddox)
                {
                    for (ITextComponent component : event.getMessage().getSiblings())
                    {
                        if (message.contains("[OPEN MENU]") && component.getStyle().getClickEvent() != null)
                        {
                            this.mc.player.sendChatMessage(component.getStyle().getClickEvent().getValue());
                        }
                    }
                }

                if (SkyBlockEventHandler.isSkyBlock)
                {
                    if (SBExtendedConfig.INSTANCE.currentServerDay && message.startsWith("Sending to server"))
                    {
                        TimeUtils.schedule(() ->
                        {
                            long day = this.mc.world.getDayTime() / 24000L;
                            TextFormatting dayColor = day >= 29 ? TextFormatting.RED : TextFormatting.GREEN;

                            if (SkyBlockEventHandler.isSkyBlock)
                            {
                                ClientUtils.printClientMessage(JsonUtils.create("Current server day: ").applyTextStyles(TextFormatting.YELLOW, TextFormatting.BOLD).appendSibling(JsonUtils.create(String.valueOf(day)).applyTextStyles(TextFormatting.RESET, dayColor)));
                            }
                        }, 2500);
                    }
                    if (dragonDownMatcher.matches())
                    {
                        SkyBlockEventHandler.dragonHealth = 0;
                    }
                    if (dragonSpawnedMatcher.matches())
                    {
                        String dragon = dragonSpawnedMatcher.group("dragon");
                        DragonType type = DragonType.valueOf(dragon.toUpperCase());
                        this.dragonType = type;
                    }

                    if (chatMatcher.matches())
                    {
                        try
                        {
                            String name = "";

                            if (chatMatcher.group(1) != null)
                            {
                                name = chatMatcher.group(1);
                            }
                            if (chatMatcher.group(2) != null)
                            {
                                name = chatMatcher.group(2);
                            }

                            if (!name.isEmpty())
                            {
                                ITextComponent chat = event.getMessage().deepCopy();
                                chat.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/p " + name));
                                event.setMessage(chat);
                            }
                        }
                        catch (Exception e) {}
                    }

                    if (SBExtendedConfig.INSTANCE.fishCatchDisplayMode == ToastMode.TOAST || SBExtendedConfig.INSTANCE.fishCatchDisplayMode == ToastMode.CHAT_AND_TOAST)
                    {
                        if (fishCatchPattern.matches())
                        {
                            String dropType = fishCatchPattern.group("type");
                            String name = fishCatchPattern.group("item");
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, dropType.equals("GOOD") ? ToastUtils.DropType.GOOD_CATCH : ToastUtils.DropType.GREAT_CATCH, ToastType.DROP));
                            ToastLog.logToast(formattedMessage);
                            cancelMessage = SBExtendedConfig.INSTANCE.fishCatchDisplayMode == ToastMode.TOAST;
                        }
                        else if (coinsCatchPattern.matches())
                        {
                            String type = coinsCatchPattern.group("type");
                            String coin = coinsCatchPattern.group("coin");
                            CoinType coinType = type.equals("GOOD") ? CoinType.TYPE_1 : CoinType.TYPE_2;
                            ItemStack coinSkull = SBRenderUtils.getSkullItemStack(coinType.getId(), coinType.getValue());
                            NumericToast.addValueOrUpdate(this.mc.getToastGui(), type.equals("GOOD") ? ToastUtils.DropType.GOOD_CATCH_COINS : ToastUtils.DropType.GREAT_CATCH_COINS, Integer.valueOf(coin.replace(",", "")), coinSkull, "Coins");
                            ToastLog.logToast(formattedMessage);
                            cancelMessage = SBExtendedConfig.INSTANCE.fishCatchDisplayMode == ToastMode.TOAST;
                        }
                    }

                    if (SBExtendedConfig.INSTANCE.giftDisplayMode == ToastMode.TOAST || SBExtendedConfig.INSTANCE.giftDisplayMode == ToastMode.CHAT_AND_TOAST)
                    {
                        if (coinsGiftPattern.matches())
                        {
                            String type = coinsGiftPattern.group("type");
                            String coin = coinsGiftPattern.group("coin");
                            ToastUtils.DropType rarity = type.equals("RARE") ? ToastUtils.DropType.RARE_GIFT : type.equals("SWEET") ? ToastUtils.DropType.SWEET_GIFT : ToastUtils.DropType.COMMON_GIFT;
                            ItemStack coinSkull = SBRenderUtils.getSkullItemStack(CoinType.TYPE_1.getId(), CoinType.TYPE_1.getValue());
                            NumericToast.addValueOrUpdate(this.mc.getToastGui(), rarity, Integer.valueOf(coin.replace(",", "")), coinSkull, "Coins");
                            ToastLog.logToast(message);
                            cancelMessage = SBExtendedConfig.INSTANCE.giftDisplayMode == ToastMode.TOAST;
                        }
                        else if (skillExpGiftPattern.matches())
                        {
                            String type = skillExpGiftPattern.group("type");
                            String exp = skillExpGiftPattern.group("exp");
                            String skill = skillExpGiftPattern.group("skill");
                            ToastUtils.DropType rarity = type.equals("RARE") ? ToastUtils.DropType.RARE_GIFT : type.equals("SWEET") ? ToastUtils.DropType.SWEET_GIFT : ToastUtils.DropType.COMMON_GIFT;
                            NumericToast.addValueOrUpdate(this.mc.getToastGui(), rarity, Integer.valueOf(exp.replace(",", "")), null, skill);
                            ToastLog.logToast(message);
                            cancelMessage = SBExtendedConfig.INSTANCE.giftDisplayMode == ToastMode.TOAST;
                        }
                        else if (itemDropGiftPattern.matches())
                        {
                            String type = itemDropGiftPattern.group("type");
                            String name = itemDropGiftPattern.group("item");
                            ToastUtils.DropType rarity = type.equals("RARE") ? ToastUtils.DropType.RARE_GIFT : type.equals("SWEET") ? ToastUtils.DropType.SWEET_GIFT : ToastUtils.DropType.COMMON_GIFT;
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, rarity, ToastUtils.ToastType.GIFT));
                            ToastLog.logToast(message);
                            cancelMessage = SBExtendedConfig.INSTANCE.giftDisplayMode == ToastMode.TOAST;
                        }
                        else if (santaTierPattern.matches())
                        {
                            String name = santaTierPattern.group("item");
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, ToastUtils.DropType.SANTA_TIER, ToastUtils.ToastType.GIFT));
                            ToastLog.logToast(message);
                            cancelMessage = SBExtendedConfig.INSTANCE.giftDisplayMode == ToastMode.TOAST;
                        }
                    }

                    if (SBExtendedConfig.INSTANCE.itemLogDisplayMode == ToastMode.TOAST || SBExtendedConfig.INSTANCE.itemLogDisplayMode == ToastMode.CHAT_AND_TOAST)
                    {
                        boolean isToast = SBExtendedConfig.INSTANCE.itemLogDisplayMode == ToastMode.TOAST;

                        if (message.contains("You destroyed an Ender Crystal!"))
                        {
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck("Crystal Fragment", ToastUtils.DropType.DRAGON_CRYSTAL_FRAGMENT, ToastType.DROP));
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }

                        if (rareDropPattern.matches())
                        {
                            String name = rareDropPattern.group("item");
                            String magicFind = rareDropPattern.group("mf");
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, magicFind, ToastUtils.DropType.RARE_DROP, ToastType.DROP));
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }
                        else if (bossDropPattern.matches())
                        {
                            String name = bossDropPattern.group("item");
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, ToastUtils.DropType.BOSS_DROP, ToastType.DROP));
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }
                        else if (dungeonQualityDropPattern.matches())
                        {
                            String name = dungeonQualityDropPattern.group("item");
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, ToastUtils.DropType.DUNGEON_QUALITY_DROP, ToastType.DROP));
                            ToastLog.logToast(formattedMessage);
                            cancelMessage = isToast;
                        }
                        else if (rareDropBracketPattern.matches())
                        {
                            String type = rareDropBracketPattern.group("type");
                            String name = rareDropBracketPattern.group("item");
                            String magicFind = rareDropBracketPattern.group(3);
                            ToastUtils.DropType dropType = type.startsWith("\u00a7r\u00a79\u00a7lVERY RARE") ? ToastUtils.DropType.SLAYER_VERY_RARE_DROP_BLUE : type.startsWith("\u00a7r\u00a75\u00a7lVERY RARE") ? ToastUtils.DropType.SLAYER_VERY_RARE_DROP_PURPLE : ToastUtils.DropType.SLAYER_CRAZY_RARE_DROP;
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, magicFind, dropType, ToastType.DROP));
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }
                        else if (rareDrop2SpaceBracketPattern.matches())
                        {
                            String name = rareDrop2SpaceBracketPattern.group("item");
                            String magicFind = rareDrop2SpaceBracketPattern.group(2);
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, magicFind, ToastUtils.DropType.SLAYER_RARE_DROP, ToastType.DROP));
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }
                    }

                    if (SBExtendedConfig.INSTANCE.petDisplayMode == ToastMode.TOAST || SBExtendedConfig.INSTANCE.petDisplayMode == ToastMode.CHAT_AND_TOAST)
                    {
                        boolean isToast = SBExtendedConfig.INSTANCE.petDisplayMode == ToastMode.TOAST;

                        if (petLevelUpPattern.matches())
                        {
                            String name = petLevelUpPattern.group("name");
                            String level = petLevelUpPattern.group("level");
                            ItemStack itemStack = SBPets.Type.valueOf(TextFormatting.getTextWithoutFormattingCodes(name).replace(" ", "_").toUpperCase()).getPetItem();
                            itemStack.setDisplayName(JsonUtils.create(name));
                            NumericToast.addValueOrUpdate(this.mc.getToastGui(), ToastUtils.DropType.PET_LEVEL_UP, Integer.valueOf(level), itemStack, "Pet", true);
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }
                        else if (petDropPattern.matches())
                        {
                            String name = petDropPattern.group("item");
                            String magicFind = petDropPattern.group("mf");
                            SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.add(new ToastUtils.ItemDropCheck(name, magicFind, ToastUtils.DropType.PET_DROP, ToastType.DROP));
                            ToastLog.logToast(message);
                            cancelMessage = isToast;
                        }
                    }
                }
                event.setCanceled(cancelMessage);
            }
        }
    }

    @SubscribeEvent
    public void onPressKey(InputEvent.KeyInputEvent event)
    {
        if (SkyBlockEventHandler.isSkyBlock)
        {
            if (KeyBindingHandler.KEY_SB_ENDER_CHEST.isKeyDown())
            {
                this.mc.player.sendChatMessage("/enderchest");
            }
            else if (KeyBindingHandler.KEY_SB_CRAFTED_MINIONS.isKeyDown())
            {
                this.mc.player.sendChatMessage("/craftedgenerators");
            }
            else if (KeyBindingHandler.KEY_SB_CRAFTING_TABLE.isKeyDown())
            {
                this.mc.player.sendChatMessage("/craft");
            }
            else if (KeyBindingHandler.KEY_SB_MENU.isKeyDown())
            {
                this.mc.player.sendChatMessage("/sbmenu");
            }
            else if (KeyBindingHandler.KEY_SB_VIEW_RECIPE.isKeyDown() && this.mc.currentScreen == null)
            {
                this.mc.player.sendChatMessage("/recipes");
            }
        }

        if (KeyBindingHandler.KEY_SB_API_VIEWER.isKeyDown())
        {
            if (StringUtils.isNullOrEmpty(SkyBlockcatiaConfig.GENERAL.hypixelApiKey.get()))
            {
                ClientUtils.printClientMessage("Couldn't open API Viewer, Empty text in the Config!", TextFormatting.RED);
                ClientUtils.printClientMessage(JsonUtils.create("Make sure you're in the Hypixel!").applyTextStyle(TextFormatting.YELLOW).appendSibling(JsonUtils.create(" Click Here to create an API key").applyTextStyle(TextFormatting.GOLD).setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/api new")))));
                return;
            }
            if (!SkyBlockcatiaConfig.GENERAL.hypixelApiKey.get().matches(SkyBlockEventHandler.UUID_PATTERN_STRING))
            {
                ClientUtils.printClientMessage("Invalid UUID for Hypixel API Key!", TextFormatting.RED);
                ClientUtils.printClientMessage("Example UUID pattern: " + UUID.randomUUID(), TextFormatting.YELLOW);
                return;
            }
            if (this.mc.pointedEntity != null && this.mc.pointedEntity instanceof RemoteClientPlayerEntity)
            {
                RemoteClientPlayerEntity player = (RemoteClientPlayerEntity)this.mc.pointedEntity;

                if (this.mc.player.connection.getPlayerInfoMap().stream().anyMatch(info -> info.getGameProfile().getName().equals(player.getName().getString())))
                {
                    this.mc.displayGuiScreen(new SkyBlockProfileViewerScreen(SkyBlockProfileViewerScreen.GuiState.PLAYER, player.getDisplayName().getString(), "", ""));
                }
                else
                {
                    this.mc.displayGuiScreen(new SkyBlockProfileViewerScreen(SkyBlockProfileViewerScreen.GuiState.EMPTY));
                }
            }
            else
            {
                this.mc.displayGuiScreen(new SkyBlockProfileViewerScreen(SkyBlockProfileViewerScreen.GuiState.EMPTY));
            }
        }
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event)
    {
        String name = event.getName();

        if (this.mc.world != null)
        {
            if (name.equals("records.13") && SkyBlockEventHandler.SKY_BLOCK_LOCATION == SBLocation.BLAZING_FORTRESS)
            {
                this.mc.ingameGUI.displayTitle(JsonUtils.create("Preparing spawn...").applyTextStyle(TextFormatting.RED).getFormattedText(), JsonUtils.create("").getFormattedText(), 0, 1200, 20);
                this.mc.getSoundHandler().play(new SimpleSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP.getName(), SoundCategory.PLAYERS, 0.75F, 1.0F, false, 0, ISound.AttenuationType.NONE, (float)this.mc.player.getPosX() + 0.5F, (float)this.mc.player.getPosY() + 0.5F, (float)this.mc.player.getPosZ() + 0.5F, false));
            }
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent event)
    {
        if (event.getEntity() == this.mc.player)
        {
            this.previousInventory = null;
            SkyBlockEventHandler.dragonHealth = 0;
            ITEM_DROP_CHECK_LIST.clear();
        }
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(ClientPlayerNetworkEvent.LoggedOutEvent event)
    {
        SignSelectionList.clearAll();
        SkyBlockEventHandler.dragonHealth = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemTooltip(ItemTooltipEvent event)
    {
        if (!SkyBlockEventHandler.isSkyBlock)
        {
            return;
        }

        List<ITextComponent> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        try
        {
            if (event.getItemStack().hasTag())
            {
                CompoundNBT extraAttrib = event.getItemStack().getTag().getCompound("ExtraAttributes");
                int toAdd = this.mc.gameSettings.advancedItemTooltips ? 3 : 1;

                if (extraAttrib.contains("timestamp"))
                {
                    DateFormat parseFormat = new SimpleDateFormat("MM/dd/yy HH:mm a");
                    Date date = parseFormat.parse(extraAttrib.getString("timestamp"));
                    String formatted = new SimpleDateFormat("d MMMM yyyy").format(date);
                    event.getToolTip().add(event.getToolTip().size() - toAdd, JsonUtils.create("Obtained: " + TextFormatting.RESET + formatted).applyTextStyle(TextFormatting.GRAY));
                }
                if (SBExtendedConfig.INSTANCE.bazaarOnItemTooltip)
                {
                    for (Map.Entry<String, BazaarData> entry : MainEventHandler.BAZAAR_DATA.entrySet())
                    {
                        BazaarData.Product product = entry.getValue().getProduct();

                        if (extraAttrib.getString("id").equals(entry.getKey()))
                        {
                            if (ClientUtils.isShiftKeyDown())
                            {
                                double buyStack = 64 * product.getBuyPrice();
                                double sellStack = 64 * product.getSellPrice();
                                double buyCurrent = event.getItemStack().getCount() * product.getBuyPrice();
                                double sellCurrent = event.getItemStack().getCount() * product.getSellPrice();
                                event.getToolTip().add(event.getToolTip().size() - toAdd, JsonUtils.create("Buy/Sell (Stack): " + TextFormatting.GOLD + NumberUtils.NUMBER_FORMAT_WITH_DECIMAL.format(buyStack) + TextFormatting.YELLOW + "/" + TextFormatting.GOLD + NumberUtils.NUMBER_FORMAT_WITH_DECIMAL.format(sellStack) + " coins"));

                                if (event.getItemStack().getCount() > 1 && event.getItemStack().getCount() < 64)
                                {
                                    event.getToolTip().add(event.getToolTip().size() - toAdd, JsonUtils.create("Buy/Sell (Current): " + TextFormatting.GOLD + NumberUtils.NUMBER_FORMAT_WITH_DECIMAL.format(buyCurrent) + TextFormatting.YELLOW + "/" + TextFormatting.GOLD + NumberUtils.NUMBER_FORMAT_WITH_DECIMAL.format(sellCurrent) + " coins"));
                                }

                                event.getToolTip().add(event.getToolTip().size() - toAdd, JsonUtils.create("Buy/Sell (One): " + TextFormatting.GOLD + NumberUtils.NUMBER_FORMAT_WITH_DECIMAL.format(product.getBuyPrice()) + TextFormatting.YELLOW + "/" + TextFormatting.GOLD + NumberUtils.NUMBER_FORMAT_WITH_DECIMAL.format(product.getSellPrice()) + " coins"));
                                event.getToolTip().add(event.getToolTip().size() - toAdd, JsonUtils.create("Last Updated: " + TextFormatting.WHITE + TimeUtils.getRelativeTime(entry.getValue().getLastUpdated())));
                            }
                            else
                            {
                                event.getToolTip().add(event.getToolTip().size() - toAdd, JsonUtils.create("Press <SHIFT> to view Bazaar Buy/Sell").applyTextStyle(TextFormatting.GRAY));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {}

        try
        {
            for (ITextComponent tooltip : event.getToolTip())
            {
                String lore = ITextComponent.Serializer.fromJson(ITextComponent.Serializer.toJson(tooltip)).getString();

                SkyBlockEventHandler.replaceEventEstimateTime(lore, calendar, event.getToolTip(), dates, "Starts in: ");
                SkyBlockEventHandler.replaceEventEstimateTime(lore, calendar, event.getToolTip(), dates, "Starting in: ");

                SkyBlockEventHandler.replaceBankInterestTime(lore, calendar, event.getToolTip(), dates, "Interest in: ");
                SkyBlockEventHandler.replaceBankInterestTime(lore, calendar, event.getToolTip(), dates, "Until interest: ");

                SkyBlockEventHandler.replaceAuctionTime(lore, calendar, event.getToolTip(), dates, "Ends in: ");
            }
        }
        catch (Exception e) {}
    }

    @SubscribeEvent // TODO Remove later
    public void onRenderNameplate(RenderNameplateEvent event)
    {
        if (event.getEntity() instanceof ArmorStandEntity && event.getEntity().hasCustomName())
        {
            String name = event.getEntity().getCustomName().getFormattedText();

            if (name.contains("Sven Pup"))
            {
                event.setResult(Result.DENY);
            }
        }
    }

    /**
     * Credit to codes.biscuit.skyblockaddons.utils.InventoryUtils
     */
    private void getInventoryDifference(NonNullList<ItemStack> currentInventory)
    {
        List<ItemStack> newInventory = this.copyInventory(currentInventory);
        Map<String, ItemDropDiff> previousInventoryMap = new HashMap<>();
        Map<String, ItemDropDiff> newInventoryMap = new HashMap<>();
        SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.removeIf(drop -> this.removeUndisplayedToast(drop));

        if (this.previousInventory != null)
        {
            for (int i = 0; i < newInventory.size(); i++)
            {
                ItemStack previousItem = this.previousInventory.get(i);
                ItemStack newItem = newInventory.get(i);

                if (previousItem != null)
                {
                    int amount = previousInventoryMap.getOrDefault(previousItem.getDisplayName(), new ItemDropDiff(previousItem, 0)).count + previousItem.getCount();
                    previousInventoryMap.put(previousItem.getDisplayName().getFormattedText(), new ItemDropDiff(previousItem, amount));
                }
                if (newItem != null)
                {
                    int amount = newInventoryMap.getOrDefault(newItem.getDisplayName(), new ItemDropDiff(newItem, 0)).count + newItem.getCount();
                    newInventoryMap.put(newItem.getDisplayName().getFormattedText(), new ItemDropDiff(newItem, amount));
                }
            }

            Set<String> keySet = new HashSet<>(previousInventoryMap.keySet());
            keySet.addAll(newInventoryMap.keySet());

            keySet.forEach(key ->
            {
                ItemDropDiff previousDiff = previousInventoryMap.getOrDefault(key, new ItemDropDiff(null, 0));
                ItemDropDiff newDiff = newInventoryMap.getOrDefault(key, new ItemDropDiff(null, 0));
                int diff = newDiff.count - previousDiff.count;

                if (diff != 0)
                {
                    ItemStack newItem = newDiff.itemStack;

                    if (newItem != null)
                    {
                        for (Iterator<ToastUtils.ItemDropCheck> iterator = SkyBlockEventHandler.ITEM_DROP_CHECK_LIST.iterator(); iterator.hasNext();)
                        {
                            ToastUtils.ItemDropCheck drop = iterator.next();
                            String dropName = drop.getName();

                            if (drop.getType() == ToastUtils.DropType.PET_DROP)
                            {
                                if (("[Lvl 1] " + dropName).equals(key))
                                {
                                    newItem.setCount(diff);

                                    if (this.mc.getToastGui().toastsQueue.add(new ItemDropsToast(newItem, drop.getType(), drop.getMagicFind())))
                                    {
                                        iterator.remove();
                                    }
                                }
                            }
                            else
                            {
                                dropName = RENAMED_DROP.getOrDefault(dropName, dropName);

                                if (dropName.equals(key) || !drop.getType().hasFormat() && dropName.equals(TextFormatting.getTextWithoutFormattingCodes(key)))
                                {
                                    newItem.setCount(diff);

                                    if (drop.getToastType() == ToastType.DROP)
                                    {
                                        if (this.mc.getToastGui().toastsQueue.add(new ItemDropsToast(newItem, drop.getType(), drop.getMagicFind())))
                                        {
                                            iterator.remove();
                                        }
                                    }
                                    else
                                    {
                                        if (this.mc.getToastGui().toastsQueue.add(new GiftToast(newItem, drop.getType(), drop.getType() == ToastUtils.DropType.SANTA_TIER)))
                                        {
                                            iterator.remove();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
        this.previousInventory = newInventory;
    }

    private boolean removeUndisplayedToast(ToastUtils.ItemDropCheck drop)
    {
        if (System.currentTimeMillis() > drop.getTimestamp() + 10000L)
        {
            ToastLog.logToast("You got " + drop.getName() + " but it doesn't show on toast!");
            return true;
        }
        return false;
    }

    public static ItemStack getSkillItemStack(String exp, SBSkills.Type skill)
    {
        ItemStack itemStack = skill.getItemStack();
        itemStack.setDisplayName(JsonUtils.create(ColorUtils.stringToRGB("255,255,85").toColoredFont() + exp + " " + skill.getName() + " XP"));
        return itemStack;
    }

    private List<ItemStack> copyInventory(NonNullList<ItemStack> inventory)
    {
        List<ItemStack> copy = new ArrayList<>(inventory.size());

        for (ItemStack item : inventory)
        {
            copy.add(item.copy());
        }
        return copy;
    }

    private boolean isHypixel()
    {
        ServerData server = this.mc.getCurrentServerData();
        return server != null && server.serverIP.contains("hypixel");
    }

    private static void addVisitingToast(Minecraft mc, String name)
    {
        CommonUtils.runAsync(() -> mc.getToastGui().add(new VisitIslandToast(name)));
    }

    private static void replaceEventEstimateTime(String lore, Calendar calendar, List<ITextComponent> tooltip, List<ITextComponent> dates, String replacedText)
    {
        if (lore.startsWith(replacedText))
        {
            lore = lore.replace(replacedText, "");
            String[] timeEstimate = Arrays.stream(lore.split(" ")).map(time -> time.replaceAll("[^0-9]+", "")).toArray(size -> new String[size]);
            int dayF = Integer.valueOf(timeEstimate[0]);
            int hourF = Integer.valueOf(timeEstimate[1]);
            int minuteF = Integer.valueOf(timeEstimate[2]);
            int secondF = Integer.valueOf(timeEstimate[3]);
            calendar.add(Calendar.DATE, dayF);
            calendar.add(Calendar.HOUR, hourF);
            calendar.add(Calendar.MINUTE, minuteF);
            calendar.add(Calendar.SECOND, secondF);
            String date1 = new SimpleDateFormat("EEEE h:mm:ss a", Locale.ENGLISH).format(calendar.getTime());
            String date2 = new SimpleDateFormat("d MMMMM yyyy", Locale.ENGLISH).format(calendar.getTime());
            dates.add(JsonUtils.create("Event starts at: ").applyTextStyle(TextFormatting.GRAY));
            dates.add(JsonUtils.create(TextFormatting.YELLOW + date1));
            dates.add(JsonUtils.create(TextFormatting.YELLOW + date2));

            int indexToRemove = 0;

            for (int i = 0; i < tooltip.size(); i++)
            {
                if (tooltip.get(i).getString().contains(replacedText))
                {
                    indexToRemove = i;
                }
            }
            if (!ClientUtils.isShiftKeyDown())
            {
                tooltip.add(indexToRemove + 1, JsonUtils.create("Press <SHIFT> to view exact time").applyTextStyle(TextFormatting.GRAY));
            }
            else
            {
                tooltip.remove(indexToRemove);
                tooltip.addAll(indexToRemove, dates);
            }
        }
    }

    private static void replaceBankInterestTime(String lore, Calendar calendar, List<ITextComponent> tooltip, List<ITextComponent> dates, String replacedText)
    {
        if (lore.startsWith(replacedText))
        {
            lore = lore.replace(replacedText, "").replaceAll("[^0-9]+", " ");
            String[] timeEstimate = Arrays.stream(lore.split(" ")).map(time -> time.replaceAll("[^0-9]+", "")).toArray(size -> new String[size]);
            int hourF = 0;
            int minuteF = 0;
            int secondF = 0;

            if (timeEstimate.length == 1)
            {
                hourF = Integer.valueOf(timeEstimate[0]);
            }
            else if (timeEstimate.length == 2)
            {
                minuteF = Integer.valueOf(timeEstimate[0]);
                secondF = Integer.valueOf(timeEstimate[1]);
            }
            else
            {
                hourF = Integer.valueOf(timeEstimate[0]);
                minuteF = Integer.valueOf(timeEstimate[1]);
                secondF = Integer.valueOf(timeEstimate[2]);
            }

            calendar.add(Calendar.HOUR, hourF);
            calendar.add(Calendar.MINUTE, minuteF);
            calendar.add(Calendar.SECOND, secondF);
            String date1 = new SimpleDateFormat("EEEE h:mm:ss a", Locale.ENGLISH).format(calendar.getTime());

            if (timeEstimate.length == 1)
            {
                date1 = new SimpleDateFormat("EEEE h:00 a", Locale.ENGLISH).format(calendar.getTime());
            }

            String date2 = new SimpleDateFormat("d MMMMM yyyy", Locale.ENGLISH).format(calendar.getTime());
            dates.add(JsonUtils.create("Interest receive at: ").applyTextStyle(TextFormatting.GRAY));
            dates.add(JsonUtils.create(TextFormatting.YELLOW + date1));
            dates.add(JsonUtils.create(TextFormatting.YELLOW + date2));

            int indexToRemove = 0;

            for (int i = 0; i < tooltip.size(); i++)
            {
                if (tooltip.get(i).getString().contains(replacedText))
                {
                    indexToRemove = i;
                    break;
                }
            }
            if (!ClientUtils.isShiftKeyDown())
            {
                tooltip.add(indexToRemove + 1, JsonUtils.create("Press <SHIFT> to view exact time").applyTextStyle(TextFormatting.GRAY));
            }
            else
            {
                tooltip.remove(indexToRemove);
                tooltip.addAll(indexToRemove, dates);
            }
        }
    }

    private static void replaceAuctionTime(String lore, Calendar calendar, List<ITextComponent> tooltip, List<ITextComponent> dates, String replacedText)
    {
        Minecraft mc = Minecraft.getInstance();

        if (lore.startsWith(replacedText))
        {
            boolean isDay = lore.endsWith("d");
            lore = lore.replace(replacedText, "").replaceAll("[^0-9]+", " ");
            String[] timeEstimate = Arrays.stream(lore.split(" ")).map(time -> time.replaceAll("[^0-9]+", "")).toArray(size -> new String[size]);
            int dayF = 0;
            int hourF = 0;
            int minuteF = 0;
            int secondF = 0;

            if (timeEstimate.length == 1)
            {
                if (isDay)
                {
                    dayF = Integer.valueOf(timeEstimate[0]);
                }
                else
                {
                    hourF = Integer.valueOf(timeEstimate[0]);
                }
            }
            else if (timeEstimate.length == 2)
            {
                minuteF = Integer.valueOf(timeEstimate[0]);
                secondF = Integer.valueOf(timeEstimate[1]);
            }
            else
            {
                hourF = Integer.valueOf(timeEstimate[0]);
                minuteF = Integer.valueOf(timeEstimate[1]);
                secondF = Integer.valueOf(timeEstimate[2]);
            }

            calendar.add(Calendar.DATE, dayF);
            calendar.add(Calendar.HOUR, hourF);
            calendar.add(Calendar.MINUTE, minuteF);
            calendar.add(Calendar.SECOND, secondF);
            String date1 = new SimpleDateFormat("EEEE h:mm:ss a", Locale.ENGLISH).format(calendar.getTime());

            if (timeEstimate.length == 1)
            {
                date1 = new SimpleDateFormat("EEEE h:00 a", Locale.ENGLISH).format(calendar.getTime());
            }

            String date2 = new SimpleDateFormat("d MMMMM yyyy", Locale.ENGLISH).format(calendar.getTime());

            if (mc.currentScreen != null && mc.currentScreen instanceof ChestScreen)
            {
                ChestScreen chest = (ChestScreen)mc.currentScreen;
                String name = chest.getTitle().getUnformattedComponentText();

                if (name.equals("Auction View"))
                {
                    dates.add(JsonUtils.create("Ends at: " + TextFormatting.YELLOW + date1 + ", " + date2).applyTextStyle(TextFormatting.GRAY));
                }
                else
                {
                    dates.add(JsonUtils.create("Ends at: ").applyTextStyle(TextFormatting.GRAY));
                    dates.add(JsonUtils.create(TextFormatting.YELLOW + date1));
                    dates.add(JsonUtils.create(TextFormatting.YELLOW + date2));
                }
            }

            int indexToRemove = 0;

            for (int i = 0; i < tooltip.size(); i++)
            {
                if (tooltip.get(i).getString().contains(replacedText))
                {
                    indexToRemove = i;
                    break;
                }
            }
            if (!ClientUtils.isShiftKeyDown())
            {
                tooltip.add(indexToRemove + 1, JsonUtils.create("Press <SHIFT> to view exact time").applyTextStyle(TextFormatting.GRAY));
            }
            else
            {
                tooltip.remove(indexToRemove);
                tooltip.addAll(indexToRemove, dates);
            }
        }
    }

    private enum CoinType
    {
        TYPE_1("2070f6cb-f5db-367a-acd0-64d39a7e5d1b", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM4MDcxNzIxY2M1YjRjZDQwNmNlNDMxYTEzZjg2MDgzYTg5NzNlMTA2NGQyZjg4OTc4Njk5MzBlZTZlNTIzNyJ9fX0="),
        TYPE_2("8ce61ae1-7cb4-3bdd-b1be-448c6fabb355", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGZhMDg3ZWI3NmU3Njg3YTgxZTRlZjgxYTdlNjc3MjY0OTk5MGY2MTY3Y2ViMGY3NTBhNGM1ZGViNmM0ZmJhZCJ9fX0=");

        private final String id;
        private final String value;

        private CoinType(String id, String value)
        {
            this.id = id;
            this.value = value;
        }

        public String getId()
        {
            return this.id;
        }

        public String getValue()
        {
            return this.value;
        }
    }

    class ItemDropDiff
    {
        final ItemStack itemStack;
        final int count;

        public ItemDropDiff(ItemStack itemStack, int count)
        {
            this.itemStack = itemStack;
            this.count = count;
        }
    }
}